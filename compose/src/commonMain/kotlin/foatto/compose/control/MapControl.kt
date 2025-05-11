package foatto.compose.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.hsl
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.control.composable.map.MapToolBar
import foatto.compose.control.model.MouseRectData
import foatto.compose.control.model.map.DistancerLineData
import foatto.compose.control.model.map.DistancerTextData
import foatto.compose.control.model.map.MapWorkMode
import foatto.compose.control.model.xy.XyElementData
import foatto.compose.control.model.xy.XyElementDataType
import foatto.compose.invokeRequest
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.model.xy.XyViewCoord
import foatto.core.model.request.MapActionRequest
import foatto.core.model.response.MapActionResponse
import foatto.core.model.response.xy.XyBitmapType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.map.MapResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private enum class SelectOption { SET, ADD, REVERT, DELETE }

class MapControl(
    root: Root,
    appControl: AppControl,
    private val mapAction: AppAction,
    private val mapResponse: MapResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, mapResponse.elementConfigs, tabId) {

    companion object {
        private val COLOR_MAP_LINE: Color = hsl(180.0f, 1.0f, 0.5f)
        private val COLOR_MAP_LINE_WIDTH = 1.0f //!!! max(1.0, scaleKoef).roundToInt()
        private val COLOR_MAP_DISTANCER = hsl(30.0f, 1.0f, 0.5f)
        private const val mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта
        const val MAP_START_EXPAND_KOEF = 0.05f                 // расширение стартовой области, чтобы "отодвинуть" элементы от края карты
    }

    private var isToolBarsVisible by mutableStateOf(true)

    private var isPanButtonEnabled by mutableStateOf(false)
    private var isZoomButtonEnabled by mutableStateOf(true)
    private var isDistancerButtonEnabled by mutableStateOf(true)
    private var isSelectButtonEnabled by mutableStateOf(true)

    private var isZoomInButtonEnabled by mutableStateOf(true)
    private var isZoomOutButtonEnabled by mutableStateOf(true)

    private var isAddElementButtonVisible by mutableStateOf(false)
    private var isEditPointButtonVisible by mutableStateOf(false)
    private var isMoveElementsButtonVisible by mutableStateOf(false)

    private var isActionOkButtonVisible by mutableStateOf(false)
    private var isActionCancelButtonVisible by mutableStateOf(false)

    private val alDistancerLine = mutableStateListOf<DistancerLineData>()   // contains state-fields
    private val alDistancerDist = mutableStateListOf<Double>()
    private val alDistancerText = mutableStateListOf<DistancerTextData>()
    private val distancerSumTextVisible = mutableStateOf(false)             // contains state-fields
    private val distancerSumText = DistancerTextData()                      // contains state-fields

    private val mouseRect = MouseRectData()                                 // contains state-fields

    private var addElement = mutableStateOf<XyElementData?>(null)

    private var curMode = MapWorkMode.PAN
//    private var isMouseDown = false

    private val alAddEC = mutableListOf<XyElementConfig>()

//    private var editElement: XyElementData? = null
//    private var editPointIndex = -1
//
//    private val alMoveElement = mutableListOf<XyElementData>()
//    private var moveStartPoint: XyPoint? = null
//    private var moveEndPoint: XyPoint? = null

    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getHeader()

            MapToolBar(
                isWideScreen = root.isWideScreen,
                isToolBarsVisible = isToolBarsVisible,
                isPanButtonEnabled = isPanButtonEnabled,
                isZoomButtonEnabled = isZoomButtonEnabled,
                isDistancerButtonEnabled = isDistancerButtonEnabled,
                isSelectButtonEnabled = isSelectButtonEnabled,
                isZoomInButtonEnabled = isZoomInButtonEnabled,
                isZoomOutButtonEnabled = isZoomOutButtonEnabled,
                isAddElementButtonVisible = isAddElementButtonVisible,
                isEditPointButtonVisible = isEditPointButtonVisible,
                isMoveElementsButtonVisible = isMoveElementsButtonVisible,
                isActionOkButtonVisible = isActionOkButtonVisible,
                isActionCancelButtonVisible = isActionCancelButtonVisible,
                alAddEC = alAddEC,
                refreshInterval = refreshInterval,
                setMode = { mapWorkMode: MapWorkMode -> setMode(mapWorkMode) },
                zoomIn = { zoomIn() },
                zoomOut = { zoomOut() },
                startAdd = { elementConfig: XyElementConfig -> /*!!! startAdd(elementConfig)*/ },
                startEditPoint = { /*!!! startEditPoint()*/ },
                startMoveElements = { /*!!! startMoveElements()*/ },
                actionOk = { /*!!! actionOk()*/ },
                actionCancel = { /*!!! actionCancel()*/ },
            ) { interval: Int -> coroutineScope.launch { setInterval(interval) } }

            getXyElementTemplate(true)
        }

        coroutineScope.launch {
            while (refreshInterval > 0) {
                mapRefreshView(null, false)
                delay(refreshInterval * 1000L)
            }
        }
    }

    override fun addSpecificXy(drawScope: DrawScope) {
        super.addSpecificXy(drawScope)

        drawScope.apply {
            if (mouseRect.isVisible.value) {
                val x = min(mouseRect.x1.value, mouseRect.x2.value)
                val y = min(mouseRect.y1.value, mouseRect.y2.value)
                val width = abs(mouseRect.x2.value - mouseRect.x1.value)
                val height = abs(mouseRect.y2.value - mouseRect.y1.value)

                drawRectOnCanvas(
                    drawScope = this,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    fillColor = COLOR_MAP_LINE,
                    fillAlpha = 0.25f,
                    strokeColor = COLOR_MAP_LINE,
                    strokeAlpha = 1.0f,
                    strokeStyle = Stroke(width = COLOR_MAP_LINE_WIDTH),
                )
            }

            for (distancerLine in alDistancerLine) {
                drawLine(
                    start = Offset(distancerLine.x1.value, distancerLine.y1.value),
                    end = Offset(distancerLine.x2.value, distancerLine.y2.value),
                    color = COLOR_MAP_DISTANCER,
                    strokeWidth = 4.0f,    // 4.dp.toPx()
                    pathEffect = PathEffect.dashPathEffect(intervals = floatArrayOf(1.0f, 1.0f)),
                )
            }
//!!!            for (distancerText in alDistancerText) {
//                Div(
//                    attrs = {
//                        style {
//                            setDistancerTextStyle()
//                            distancerText.pos.value(this)
//                        }
//                    }
//                ) {
//                    Text(distancerText.text.value)
//                }
//            }
//
//            if (distancerSumTextVisible.value) {
//                Div(
//                    attrs = {
//                        style {
//                            setDistancerTextStyle()
//                            distancerSumText.pos.value(this)
//                        }
//                    }
//                ) {
//                    Text(distancerSumText.text.value)
//                }
//            }
            addElement.value?.let { element ->
                if (element.type == XyElementDataType.POLY) {
                    drawPathOnCanvas(
                        drawScope = this,
                        path = element.getPath(),
                        fillColor = element.fillColor,
                        fillAlpha = element.alpha,
                        strokeColor = if (element.isSelected) {
                            COLOR_XY_POLYGON_BORDER
                        } else {
                            element.strokeColor
                        },
                        strokeAlpha = 1.0f,
                        strokeStyle = getElementStrokeStyle(element),
                    )
                }
            }
        }
    }

    override suspend fun start() {
//    var mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта
//        //--- на текущий момент MapSurfer - наиболее правильная карта
//        val bitmapMapMode = appContainer.getUserProperty( iCoreAppContainer.UP_BITMAP_MAP_MODE )
//        mapBitmapTypeName = if( bitmapMapMode.isNullOrEmpty() ) XyBitmapType.MS else bitmapMapMode

        //--- подготовка данных для меню добавления
//!!!
//        alAddEC.addAll(
//            xyDocumentConfig.hmElementConfig.filterValues { value ->
//                value.descrForAction.isNotEmpty()
//            }.values
//        )

        root.setTabInfo(tabId, mapResponse.tabCaption)

        startMapBody(curScale = minXyScale)

        setMode(MapWorkMode.PAN)    //!!! может дефолтное значение подойдёт?
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun startMapBody(curScale: Int) {
        //--- вынесено сюда для использования во внешнем CompositeControl
        headerData = mapResponse.headerData

        root.setWait(true)
        invokeRequest(
            MapActionRequest(
                action = mapAction.copy(
                    type = ActionType.GET_COORDS,
                )
            )
        ) { xyActionResponse: MapActionResponse ->

            GlobalScope.launch {
                while (xyCanvasWidth == 0.0f) {
                    yield()
                }
                val newViewCoord = getXyCoordsDone(
                    startExpandKoef = MAP_START_EXPAND_KOEF,
                    isCentered = false,
                    curScale = curScale,
                    minCoord = xyActionResponse.minCoord!!,
                    maxCoord = xyActionResponse.maxCoord!!,
                )

                //--- именно до xyRefreshView, чтобы не сбросить сразу после включения
                root.setWait(false)
                mapRefreshView(newViewCoord, true)
            }
        }
    }

    suspend fun mapRefreshView(aView: XyViewCoord?, withWait: Boolean) {
        if (aView != null) {
            //--- вычисляем координату середины (безопасным от переполнения способом)
            //--- и выносим эту точку на середину экрана
            //--- и сохраненяем новое состояние в view
            val checkedView = getXyViewCoord(
                aScale = checkXyScale(
                    isScaleAlign = true,
                    curScale = xyViewCoord.scale,
                    newScale = aView.scale,
                    isAdaptive = false
                ),
                aCenterX = aView.x1 + (aView.x2 - aView.x1) / 2,
                aCenterY = aView.y1 + (aView.y2 - aView.y1) / 2,
            )
            xyViewCoord = checkedView
        }

        getMapElements(
            mapBitmapTypeName = mapBitmapTypeName,
            withWait = withWait,
        )

        //--- обновление в любом случае сбрасывает выделенность элементов и возможность соответствующих операций
        isEditPointButtonVisible = false
        isMoveElementsButtonVisible = false
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun getMapElements(
        mapBitmapTypeName: String,
        withWait: Boolean,
        doAdditionalWork: (xyActionResponse: MapActionResponse) -> Unit = { _: MapActionResponse -> },
    ) {
        if (withWait) {
            root.setWait(true)
        }
        invokeRequest(
            MapActionRequest(
                action = mapAction.copy(
                    type = ActionType.GET_ELEMENTS
                ),
                viewCoord = xyViewCoord,
                bitmapTypeName = mapBitmapTypeName,
            )
        ) { mapActionResponse: MapActionResponse ->

            GlobalScope.launch {
                //--- сбрасываем горизонтальный и вертикальный скроллинг/смещение
                screenOffsetX = 0.0f
                screenOffsetY = 0.0f

                readXyElements(mapActionResponse.elements)

                if (withWait) {
                    root.setWait(false)
                }

                doAdditionalWork(mapActionResponse)
            }
        }
    }

    private fun setMode(newMode: MapWorkMode) {
        when (curMode) {
            MapWorkMode.PAN -> {
                isPanButtonEnabled = true
            }

            MapWorkMode.ZOOM_BOX -> {
                isZoomButtonEnabled = true
            }

            MapWorkMode.DISTANCER -> {
                isDistancerButtonEnabled = true
                isActionCancelButtonVisible = false
                alDistancerLine.clear()
                alDistancerDist.clear()
                alDistancerText.clear()
                distancerSumText.text.value = ""
                distancerSumTextVisible.value = false
            }

            MapWorkMode.SELECT_FOR_ACTION -> {
                isSelectButtonEnabled = true

                isAddElementButtonVisible = false
                isEditPointButtonVisible = false
                isMoveElementsButtonVisible = false
            }

            MapWorkMode.ACTION_ADD,
            MapWorkMode.ACTION_EDIT_POINT,
            MapWorkMode.ACTION_MOVE -> {
                isToolBarsVisible = true
            }
        }

        when (newMode) {
            MapWorkMode.PAN -> {
                isPanButtonEnabled = false
                xyDeselectAll()
            }

            MapWorkMode.ZOOM_BOX -> {
                isZoomButtonEnabled = false
                xyDeselectAll()
            }

            MapWorkMode.DISTANCER -> {
                isDistancerButtonEnabled = false
                xyDeselectAll()
            }

            MapWorkMode.SELECT_FOR_ACTION -> {
                isSelectButtonEnabled = false
                isAddElementButtonVisible = true
                isEditPointButtonVisible = false
                isMoveElementsButtonVisible = false
                isActionOkButtonVisible = false
                isActionCancelButtonVisible = false
            }

            MapWorkMode.ACTION_ADD -> {
                isToolBarsVisible = false
                isActionOkButtonVisible = false
                isActionCancelButtonVisible = true
            }

            MapWorkMode.ACTION_EDIT_POINT -> {
                isToolBarsVisible = false
                isActionOkButtonVisible = true
                isActionCancelButtonVisible = true
            }

            MapWorkMode.ACTION_MOVE -> {
                isToolBarsVisible = false
                isActionOkButtonVisible = false
                isActionCancelButtonVisible = true
            }
        }
        curMode = newMode

        refreshInterval = 0
    }

    private fun zoomIn() {
//!!!
//        //--- проверить масштаб
//        val newScale = checkXyScale(
//            isScaleAlign = xyDocumentConfig.isScaleAlign,
//            curScale = xyViewCoord.scale,
//            newScale = xyViewCoord.scale / 2,
//            isAdaptive = false,
//        )
//
//        xyViewCoord.scale = newScale
//
//        xyRefreshView(xyViewCoord, true)
    }

    private fun zoomOut() {
//!!!
//        //--- проверить масштаб
//        val newScale = checkXyScale(
//            isScaleAlign = xyDocumentConfig.isScaleAlign,
//            curScale = xyViewCoord.scale,
//            newScale = xyViewCoord.scale * 2,
//            isAdaptive = false,
//        )
//
//        xyViewCoord.scale = newScale
//
//        xyRefreshView(xyViewCoord, true)
    }

    override fun onPointerDown(pointerInputChange: PointerInputChange) {
        when (curMode) {
            MapWorkMode.PAN -> {}

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {}

            MapWorkMode.DISTANCER -> {}

            MapWorkMode.ACTION_ADD -> {}

            MapWorkMode.ACTION_EDIT_POINT -> {}

            MapWorkMode.ACTION_MOVE -> {}
        }
    }

    override fun onDragStart(offset: Offset) {
        when (curMode) {
            MapWorkMode.PAN -> {}

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {
                mouseRect.apply {
                    isVisible.value = true
                    x1.value = offset.x
                    y1.value = offset.y
                    x2.value = offset.x
                    y2.value = offset.y
                }
            }

            MapWorkMode.DISTANCER -> {}

            MapWorkMode.ACTION_ADD -> {}

            MapWorkMode.ACTION_EDIT_POINT -> {
//                editElement?.let { editElement ->
//                    val clickRect = getXyClickRect(mouseX, mouseY)
//                    editPointIndex = -1
//                    //--- попытаемся найти вершину, на которую кликнули
//                    for (i in 0..editElement.alPoint!!.lastIndex)
//                        if (clickRect.isContains(editElement.alPoint[i])) {
//                            editPointIndex = i
//                            break
//                        }
//                    //--- если кликнутую вершину не нашли, попытаемся найти отрезок, на который кликнули
//                    if (editPointIndex == -1) {
//                        for (i in 0..editElement.alPoint.lastIndex)
//                            if (clickRect.isIntersects(
//                                    XyLine(
//                                        p1 = editElement.alPoint[i],
//                                        p2 = editElement.alPoint[if (i == editElement.alPoint.lastIndex) 0 else (i + 1)]
//                                    )
//                                )
//                            ) {
//                                //--- в месте клика на отрезке добавляем точку, которую будем двигать
//                                editPointIndex = i + 1
//                                editElement.insertPoint(editPointIndex, mouseX, mouseY)
//                                break
//                            }
//                    }
//                }
            }

            MapWorkMode.ACTION_MOVE -> {
//                moveStartPoint = XyPoint(mouseX, mouseY)
//                moveEndPoint = XyPoint(mouseX, mouseY)
            }

        }
//        isMouseDown = true
    }

    override fun onDrag(pointerInputChange: PointerInputChange, dragAmount: Offset) {
        val mouseX = pointerInputChange.position.x
        val mouseY = pointerInputChange.position.y

        when (curMode) {
            MapWorkMode.PAN -> {
                screenOffsetX += dragAmount.x
                screenOffsetY += dragAmount.y
            }

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {
                if (mouseRect.isVisible.value && mouseX >= 0 && mouseX <= xyCanvasWidth && mouseY >= 0 && mouseY <= xyCanvasHeight) {
                    mouseRect.x2.value = mouseX
                    mouseRect.y2.value = mouseY
                }
            }

            MapWorkMode.DISTANCER -> {}

            MapWorkMode.ACTION_ADD -> {}

            MapWorkMode.ACTION_EDIT_POINT -> {
//                if (editPointIndex != -1) {
//                    editElement?.setPoint(editPointIndex, mouseX, mouseY)
//                }
            }

            MapWorkMode.ACTION_MOVE -> {
//                moveEndPoint?.let { moveEndPoint ->
//                    for (element in alMoveElement) {
//                        element.moveRel(mouseX - moveEndPoint.x, mouseY - moveEndPoint.y)
//                    }
//                    moveEndPoint.set(mouseX, mouseY)
//                }
            }
        }
    }

    override suspend fun onDragEnd() {
        when (curMode) {
            MapWorkMode.PAN -> {
                //--- перезагружаем карту, только если был сдвиг
                if (abs(screenOffsetX) >= 1 || abs(screenOffsetY) >= 1) {
                    xyViewCoord.moveRel((-screenOffsetX * xyViewCoord.scale / root.scaleKoef).roundToInt(), (-screenOffsetY * xyViewCoord.scale / root.scaleKoef).roundToInt())
                    mapRefreshView(aView = xyViewCoord, withWait = true)
                }
            }

            MapWorkMode.ZOOM_BOX -> {
                if (mouseRect.isVisible.value) {
                    mouseRect.isVisible.value = false

                    val mouseWidth = abs(mouseRect.x2.value - mouseRect.x1.value)
                    val mouseHeight = abs(mouseRect.y2.value - mouseRect.y1.value)

                    //--- если размер прямоугольника меньше MIN_USER_RECT_SIZE pix, то это видимо ошибка - игнорируем
                    if (mouseWidth >= MIN_USER_RECT_SIZE * root.scaleKoef && mouseHeight >= MIN_USER_RECT_SIZE * root.scaleKoef) {

                        //--- установить показ этой области ( с наиболее близким и разрешенным масштабом )
                        //--- специально установлена работа с double-числами и округление в большую сторону
                        //--- из-за ошибок округления при масштабах, близких к 1
                        //--- (и scaleKoef здесь не нужен!)
                        val newScale = ceil(xyViewCoord.scale * max(1.0 * mouseWidth / xyCanvasWidth, 1.0 * mouseHeight / xyCanvasHeight)).toInt()
                        //--- переводим в мировые координаты
                        mapRefreshView(
                            aView = XyViewCoord(
                                newScale,
                                xyViewCoord.x1 + mouseToReal(root.scaleKoef, xyViewCoord.scale, min(mouseRect.x1.value, mouseRect.x2.value)),
                                xyViewCoord.y1 + mouseToReal(root.scaleKoef, xyViewCoord.scale, min(mouseRect.y1.value, mouseRect.y2.value)),
                                xyViewCoord.x1 + mouseToReal(root.scaleKoef, xyViewCoord.scale, max(mouseRect.x1.value, mouseRect.x2.value)),
                                xyViewCoord.y1 + mouseToReal(root.scaleKoef, xyViewCoord.scale, max(mouseRect.y1.value, mouseRect.y2.value)),
                            ),
                            withWait = true
                        )
                    }
                }
            }

            MapWorkMode.DISTANCER -> {
//                //--- при первом клике заводим сумму, отключаем тулбар и включаем кнопку отмены линейки
//                val (newX, newY) = getGraphixAndXyTooltipCoord(xySvgLeft + mouseX, xySvgTop + mouseY)
//                if (alDistancerLine.isEmpty()) {
//                    distancerSumText.apply {
//                        text.value = "0.0"
//                        pos.value = {
//                            left(newX.px)
//                            top(newY.px)
//                        }
//                    }
//                    isToolBarsVisible.value = false
//                    distancerSumTextVisible.value = true
//                    isActionCancelButtonVisible.value = true
//                }
//                alDistancerLine.add(
//                    DistancerLineData(
//                        x1 = mutableStateOf(mouseX),
//                        y1 = mutableStateOf(mouseY),
//                        x2 = mutableStateOf(mouseX),
//                        y2 = mutableStateOf(mouseY),
//                    )
//                )
//
//                alDistancerDist.add(0.0)
//
//                alDistancerText.add(
//                    DistancerTextData().apply {
//                        text.value = "0.0"
//                        pos.value = {
//                            left(newX.px)
//                            top(newY.px)
//                        }
//                    }
//                )
            }

            MapWorkMode.SELECT_FOR_ACTION -> {
//                if (mouseRect.isVisible.value) {
//                    mouseRect.isVisible.value = false
//
//                    //--- установим опцию выбора
//                    val selectOption =
//                        if (shiftKey) SelectOption.ADD
//                        else if (ctrlKey) SelectOption.REVERT
//                        else if (altKey) SelectOption.DELETE
//                        else SelectOption.SET
//
//                    //--- в обычном режиме ( т.е. без доп.клавиш ) предварительно развыберем остальные элементы
//                    if (selectOption == SelectOption.SET) {
//                        xyDeselectAll()
//                    }
//
//                    val mouseXyRect = XyRect(
//                        min(mouseRect.x1.value, mouseRect.x2.value), min(mouseRect.y1.value, mouseRect.y2.value),
//                        abs(mouseRect.x1.value - mouseRect.x2.value), abs(mouseRect.y1.value - mouseRect.y2.value)
//                    )
//                    var editableElementCount = 0
//                    isMoveElementsButtonVisible.value = false
//                    alMoveElement.clear()
//                    for (element in getXyElementList(mouseXyRect, true)) {
//                        element.isSelected = when (selectOption) {
//                            SelectOption.SET,
//                            SelectOption.ADD -> {
//                                true
//                            }
//
//                            SelectOption.REVERT -> {
//                                !element.isSelected
//                            }
//
//                            SelectOption.DELETE -> {
//                                false
//                            }
//                        }
//                        if (element.isSelected) {
//                            if (element.isEditablePoint) {
//                                editableElementCount++
//                                editElement = element
//                            }
//                            if (element.isMoveable) {
//                                //--- предварительная краткая проверка на наличие выбранных передвигабельных объектов
//                                isMoveElementsButtonVisible.value = true
//                                alMoveElement.add(element)
//                            }
//                        }
//                    }
//                    //--- проверка на возможность создания элементов при данном масштабе - пока не проверяем, т.к. геозоны можно создавать при любом масштабе
//                    //for( mi in hmAddMenuEC.keys ) {
//                    //    val tmpActionAddEC = hmAddMenuEC[ mi ]!!
//                    //    mi.isDisable = xyModel.viewCoord.scale < tmpActionAddEC.scaleMin || xyModel.viewCoord.scale > tmpActionAddEC.scaleMax
//                    //}
//                    isEditPointButtonVisible.value = editableElementCount == 1
//                }
            }

            MapWorkMode.ACTION_ADD -> {
//                val actionAddPointStatus = addElement.value?.addPoint(mouseX, mouseY)
//
//                if (actionAddPointStatus == AddPointStatus.COMPLETED) {
//                    actionOk()
//                } else {
//                    isActionOkButtonVisible.value = actionAddPointStatus == AddPointStatus.COMPLETEABLE
//                }
            }

            MapWorkMode.ACTION_EDIT_POINT -> {
//                if (editPointIndex != -1) {
//                    editOnePoint()
//                    editPointIndex = -1
//                }
            }

            MapWorkMode.ACTION_MOVE -> {
//                doMoveElements()
//                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }
        }
//        isMouseDown = false
    }

    override fun onDragCancel() {
        when (curMode) {
            MapWorkMode.PAN -> {}

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {}

            MapWorkMode.DISTANCER -> {}

            MapWorkMode.ACTION_ADD -> {}

            MapWorkMode.ACTION_EDIT_POINT -> {}

            MapWorkMode.ACTION_MOVE -> {}
        }
    }

    override fun onPointerUp(pointerInputChange: PointerInputChange) {
        when (curMode) {
            MapWorkMode.PAN -> {}

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {}

            MapWorkMode.DISTANCER -> {}

            MapWorkMode.ACTION_ADD -> {}

            MapWorkMode.ACTION_EDIT_POINT -> {}

            MapWorkMode.ACTION_MOVE -> {}
        }
    }

    //--- преобразование экранных координат в мировые
    private fun mouseToReal(scaleKoef: Float, scale: Int, screenCoord: Float): Int = (screenCoord * scale / scaleKoef).roundToInt()

    private suspend fun setInterval(sec: Int) {
        refreshInterval = sec

        if (sec == 0) {
            mapRefreshView(null, false)
        }
    }

}

/*

    private fun StyleScope.setDistancerTextStyle() {
        position(Position.Absolute)
        color(COLOR_MAIN_TEXT)
        backgroundColor(COLOR_XY_LABEL_BACK)
        textAlign("center")
        verticalAlign("baseline")
        setBorder(color = COLOR_XY_LABEL_BORDER, width = (1 * scaleKoef).px, radius = (2 * scaleKoef).px)
        setPaddings(arrStyleXyDistancerPadding)
        userSelect("none")
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun onXyMouseOver(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
        when (curMode) {
            MapWorkMode.PAN,
            MapWorkMode.ZOOM_BOX,
            MapWorkMode.SELECT_FOR_ACTION -> {
                super.onXyMouseOver(syntheticMouseEvent, xyElement)
            }

            else -> {}
        }
    }

    override fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {

        //--- mouse dragged
        if (isMouseDown) {
        }
        //--- mouse moved
        else {
            when (curMode) {
                MapWorkMode.DISTANCER -> {
                    alDistancerLine.lastOrNull()?.let { line ->
                        line.x2.value = mouseX
                        line.y2.value = mouseY

                        val dist = XyProjection.distancePrj(
                            XyPoint(
                                xyViewCoord.x1 + mouseToReal(scaleKoef, xyViewCoord.scale, line.x1.value),
                                xyViewCoord.y1 + mouseToReal(scaleKoef, xyViewCoord.scale, line.y1.value)
                            ),
                            XyPoint(
                                xyViewCoord.x1 + mouseToReal(scaleKoef, xyViewCoord.scale, line.x2.value),
                                xyViewCoord.y1 + mouseToReal(scaleKoef, xyViewCoord.scale, line.y2.value)
                            ),
                            xyViewCoord.scale
                        ) / 1000.0

                        alDistancerDist[alDistancerDist.lastIndex] = dist

                        val distancerSumDist = alDistancerDist.sum()

                        val text = alDistancerText.last()
                        val textX = (line.x1.value + line.x2.value) / 2
                        val textY = (line.y1.value + line.y2.value) / 2
                        text.text.value = getSplittedDouble(dist, 1, true, '.')
                        text.pos.value = {
                            left((xySvgLeft + textX).px)
                            top((xySvgTop + textY).px)
                        }

                        //--- иногда вышибает округлятор в getSplittedDouble
                        distancerSumText.text.value =
                            try {
                                getSplittedDouble(distancerSumDist, 1, true, '.')
                            } catch (t: Throwable) {
                                distancerSumText.text.value
                            }
                        val (newX, newY) = getGraphixAndXyTooltipCoord(xySvgLeft + mouseX, xySvgTop + mouseY)
                        distancerSumText.pos.value = {
                            left(newX.px)
                            top(newY.px)
                        }
                    }
                }

                MapWorkMode.ACTION_ADD -> {
                    addElement.value?.setLastPoint(mouseX, mouseY)
                }

                else -> {}
            }
        }
    }

    override fun onXyMouseWheel(syntheticWheelEvent: SyntheticWheelEvent) {
        val isCtrl = syntheticWheelEvent.ctrlKey
        val mouseX = syntheticWheelEvent.offsetX.toInt()
        val mouseY = syntheticWheelEvent.offsetY.toInt()
        val deltaY = syntheticWheelEvent.deltaY.toInt()

        if ((curMode == MapWorkMode.PAN && curMode == MapWorkMode.ZOOM_BOX) && isCtrl) {
            //--- вычисляем текущую координату середины
            //--- ( безопасным от переполнения способом )
            val curCenterX = xyViewCoord.x1 + (xyViewCoord.x2 - xyViewCoord.x1) / 2
            val curCenterY = xyViewCoord.y1 + (xyViewCoord.y2 - xyViewCoord.y1) / 2

            //--- сдвиг курсора мыши относительно середины в экранных координатах
            //--- ( не трогать здесь scaleKoef! )
            val sx = (1.0 * (mouseX - xySvgWidth.value / 2) / scaleKoef).roundToInt()
            val sy = (1.0 * (mouseY - xySvgHeight.value / 2) / scaleKoef).roundToInt()

            //--- то же самое в реальных координатах
            val curDX = sx * xyViewCoord.scale
            val curDY = sy * xyViewCoord.scale

            //--- новый сдвиг относительно центра для нового масштаба
            val newScale = checkXyScale(
                isScaleAlign = xyResponse.documentConfig.isScaleAlign,
                curScale = xyViewCoord.scale,
                newScale = if (deltaY < 0) {
                    xyViewCoord.scale / 2
                } else {
                    xyViewCoord.scale * 2
                },
                isAdaptive = false
            )

            val newDX = sx * newScale
            val newDY = sy * newScale

            //--- новые координаты середины для нового масштаба
            val newCenterX = curCenterX + curDX - newDX
            val newCenterY = curCenterY + curDY - newDY

            val newView = getXyViewCoord(newScale, newCenterX, newCenterY)
            xyRefreshView(newView, true)
        }
    }

    override fun onXyTextPressed(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {}
    override fun onXyTextPressed(syntheticTouchEvent: SyntheticTouchEvent, xyElement: XyElementData) {}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun startAdd(elementConfig: XyElementConfig) {
        addElement.value = getXyEmptyElementData(elementConfig)
        setMode(MapWorkMode.ACTION_ADD)
    }

    private fun startEditPoint() {
        //--- в старой версии мы предварительно прятали из модели текущую (адаптированную под текущий масштаб и координаты)
        //--- версию addElement, чтобы не мешала загрузке и работе с полной версией со всеми негенерализованными точками.
        //--- учитывая, что интерактив у нас сейчас идёт только с зонами, нарисованными вручную и точки там далеки друг от друга и не подвержены генерализации,
        //--- можно считать, что загрузка полной копии редактируемого элемента не нужна
        setMode(MapWorkMode.ACTION_EDIT_POINT)
    }

    private fun startMoveElements() {
        setMode(MapWorkMode.ACTION_MOVE)
    }

    private fun actionOk() {
        when (curMode) {
            MapWorkMode.ACTION_ADD -> {
                addElement.value?.doAddElement(root, this, xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, xyViewCoord)
                addElement.value = null
            }

            MapWorkMode.ACTION_EDIT_POINT -> {
                editElement?.doEditElementPoint(root, this, xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, xyViewCoord)
            }

            else -> {}
        }
        //that().xyRefreshView( null, null, true ) - делается внути методов doAdd/doEdit/doMove по завершении операций
        setMode(MapWorkMode.SELECT_FOR_ACTION)
    }

    private fun actionCancel() {
        when (curMode) {
            MapWorkMode.DISTANCER -> {
                //--- включить кнопки, но кнопку линейки выключить обратно
                isDistancerButtonEnabled.value = false
                alDistancerLine.clear()
                alDistancerDist.clear()
                alDistancerText.clear()
                distancerSumText.text.value = ""
                distancerSumTextVisible.value = false
                isActionCancelButtonVisible.value = false
                isToolBarsVisible.value = true
            }

            MapWorkMode.ACTION_ADD -> {
                addElement.value = null
                xyRefreshView(null, true)
                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }

            MapWorkMode.ACTION_EDIT_POINT -> {
                editElement = null
                xyRefreshView(null, true)
                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }

            else -> {}
        }
    }

    private fun editOnePoint() {
        //--- с крайними точками незамкнутой полилинии нечего доделывать
        //if( editElement.type != XyElementDataType.POLYGON && ( editPointIndex == 0 || editPointIndex == editElement.alPoint!!.lastIndex ) ) return

        //--- берем передвигаемую, предыдущую и последующую точки
        editElement?.let { editElement ->
            val p0 = editElement.alPoint!![editPointIndex]
            val p1 = editElement.alPoint[if (editPointIndex == 0) editElement.alPoint.lastIndex else editPointIndex - 1]
            val p2 = editElement.alPoint[if (editPointIndex == editElement.alPoint.lastIndex) 0 else editPointIndex + 1]

            //--- если рабочая точка достаточно близка к отрезку,
            //--- то считаем, что рабочая точка (почти :) лежит на отрезке,
            //--- соединяющем предыдущую и последущую точки, и ее можно удалить за ненадобностью
            val isRemovable = XyLine.distanceSeg(p1.x.toDouble(), p1.y.toDouble(), p2.x.toDouble(), p2.y.toDouble(), p0.x.toDouble(), p0.y.toDouble()) <= scaleKoef * 2

            //--- если точку можно удалить и элемент не является замкнутым или кол-во точек у него больше 3-х
            //--- ( т.е. даже если элемент замкнутый, то после удаления точки еще 3 точки у него останутся )
            //    if( isRemovable && ( !actionElement!!.element.itClosed || actionElement!!.element.alPoint.size > 3 ) )

            //--- сейчас работаем только с полигонами. Если сейчас больше трёх точек, значит после удаления останется как минимум 3 точки, что достаточно.
            if (isRemovable && editElement.alPoint.size > 3) {
                editElement.removePoint(editPointIndex)
            }
        }
    }

    private fun doMoveElements() {
        val xyActionRequest = XyActionRequest(
            documentTypeName = xyResponse.documentConfig.name,
            action = XyAction.MOVE_ELEMENTS,
            startParamId = xyResponse.startParamId,

            alActionElementIds = alMoveElement.map { it.elementId },
            dx = ((moveEndPoint!!.x - moveStartPoint!!.x) * xyViewCoord.scale / scaleKoef).roundToInt(),
            dy = ((moveEndPoint!!.y - moveStartPoint!!.y) * xyViewCoord.scale / scaleKoef).roundToInt()
        )

        root.setWait(true)
        invokeXy(
            xyActionRequest
        ) {
            root.setWait(false)
            xyRefreshView(null, true)
        }
    }
}

----------------

XyElementData:

    fun doAddElement(
        root: Root,
        xyControl: AbstractXyControl,
        documentTypeName: String,
        startParamId: String,
        scaleKoef: Double,
        viewCoord: XyViewCoord
    ) {
        lateinit var xyActionRequest: XyActionRequest

        when (type) {
            XyElementDataType.POLY -> {
                val xyElement = XyElement(
                    typeName = typeName!!,
                    elementId = 0,
                    objectId = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoints.map {
                    XyPoint(
                        viewCoord.x1 + (it.x * viewCoord.scale / scaleKoef).roundToInt(),
                        viewCoord.y1 + (it.y * viewCoord.scale / scaleKoef).roundToInt()
                    )
                }.dropLast(1)  // убираем последнюю служебную точку

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.ADD_ELEMENT,
                    startParamId = startParamId,
                    xyElement = xyElement
                )

                alAddInfo!!.forEach {
                    xyActionRequest.hmParam[it.first] = it.second()
                }
            }

            else -> {}
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        root.setWait(true)
        invokeRequest(xyActionRequest) { _: XyActionResponse ->
            root.setWait(false)
            xyControl.xyRefreshView(null, true)
        }
    }

    fun doEditElementPoint(
        root: Root,
        xyControl: AbstractXyControl,
        documentTypeName: String,
        startParamId: String,
        scaleKoef: Double,
        viewCoord: XyViewCoord
    ) {
        lateinit var xyActionRequest: XyActionRequest

        when (type) {
            XyElementDataType.POLY -> {
                val xyElement = XyElement(
                    typeName = "",      // неважно для редактирования точек
                    elementId = elementId,
                    objectId = 0
                )
                //--- переводим в мировые координаты
                xyElement.alPoint = alPoints.map {
                    XyPoint(
                        viewCoord.x1 + (it.x * viewCoord.scale / scaleKoef).roundToInt(),
                        viewCoord.y1 + (it.y * viewCoord.scale / scaleKoef).roundToInt()
                    )
                }

                xyActionRequest = XyActionRequest(
                    documentTypeName = documentTypeName,
                    action = XyAction.EDIT_ELEMENT_POINT,
                    startParamId = startParamId,
                    xyElement = xyElement
                )
            }

            else -> {}
//            //--- приводим в локальные/экранные размеры
//            actionElement!!.element.imageWidth *= xyModel.viewCoord.scale / scaleKoef
//            actionElement!!.element.imageHeight *= xyModel.viewCoord.scale / scaleKoef
        }

        root.setWait(true)
        invokeRequest(xyActionRequest) { _: XyActionResponse ->
            root.setWait(false)
            xyControl.xyRefreshView(null, true)
        }
    }


 */
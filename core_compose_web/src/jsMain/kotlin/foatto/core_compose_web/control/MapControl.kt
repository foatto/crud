package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyProjection
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.config.XyBitmapType
import foatto.core.app.xy.geom.XyLine
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyElementConfig
import foatto.core.link.XyResponse
import foatto.core.util.getSplittedDouble
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.control.composable.getRefreshSubToolbar
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose_web.control.model.AddPointStatus
import foatto.core_compose_web.control.model.MouseRectData
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.control.model.XyElementDataType
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.MIN_USER_RECT_SIZE
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.stroke
import org.jetbrains.compose.web.attributes.strokeDasharray
import org.jetbrains.compose.web.attributes.strokeWidth
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.userSelect
import org.jetbrains.compose.web.css.properties.verticalAlign
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.svg.*
import org.w3c.dom.svg.SVGElement
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private enum class MapWorkMode {
    PAN, ZOOM_BOX, SELECT_FOR_ACTION, DISTANCER, ACTION_ADD, ACTION_EDIT_POINT, ACTION_MOVE
}

//--- опции выбора
private enum class SelectOption { SET, ADD, REVERT, DELETE }

private val COLOR_MAP_LINE: CSSColorValue = hsl(180, 100, 50)
private val COLOR_MAP_LINE_WIDTH = max(1.0, scaleKoef).roundToInt()
private val COLOR_MAP_DISTANCER = hsl(30, 100, 50)

private const val mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта

private const val MAP_START_EXPAND_KOEF = 0.05

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class MapControl(
    root: Root,
    appControl: AppControl,
    xyResponse: XyResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, xyResponse, tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        containerPrefix = "map"
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val isPanButtonVisible = mutableStateOf(false)
    private val isZoomButtonVisible = mutableStateOf(true)
    private val isDistancerButtonVisible = mutableStateOf(true)
    private val isSelectButtonVisible = mutableStateOf(true)

    private val isZoomInButtonVisible = mutableStateOf(true)
    private val isZoomOutButtonVisible = mutableStateOf(true)

    private val isAddElementButtonVisible = mutableStateOf(false)
    private val isEditPointButtonVisible = mutableStateOf(false)
    private val isMoveElementsButtonVisible = mutableStateOf(false)

    private val isActionOkButtonVisible = mutableStateOf(false)
    private val isActionCancelButtonVisible = mutableStateOf(false)

    private val isRefreshButtonVisible = mutableStateOf(true)

    private val alDistancerLine = mutableStateListOf<DistancerLineData>()   // contains state-fields
    private val alDistancerDist = mutableStateListOf<Double>()
    private val alDistancerText = mutableStateListOf<DistancerTextData>()
    private val distancerSumTextVisible = mutableStateOf(false)             // contains state-fields
    private val distancerSumText = DistancerTextData()                      // contains state-fields

    private val mouseRect = MouseRectData()                                 // contains state-fields

    private var addElement = mutableStateOf<XyElementData?>(null)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var curMode = MapWorkMode.PAN
    private var isMouseDown = false
    private var panPointOldX = 0
    private var panPointOldY = 0
    private var panDX = 0
    private var panDY = 0

    private val alAddEC = mutableListOf<XyElementConfig>()

    private var editElement: XyElementData? = null
    private var editPointIndex = -1

    private val alMoveElement = mutableListOf<XyElementData>()
    private var moveStartPoint: XyPoint? = null
    private var moveEndPoint: XyPoint? = null

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- Map Header
            getGraphicAndXyHeader(containerPrefix)

            //--- Map Toolbar
            getGraphicAndXyToolbar(containerPrefix) {
                getToolBarSpan {
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isPanButtonVisible.value,
                        src = "/web/images/ic_open_with_black_48dp.png",
                        title = "Перемещение по карте"
                    ) {
                        setMode(MapWorkMode.PAN)
                    }
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isZoomButtonVisible.value,
                        src = "/web/images/ic_search_black_48dp.png",
                        title = "Выбор области для показа"
                    ) {
                        setMode(MapWorkMode.ZOOM_BOX)
                    }
                    getToolBarIconButton(
                        isVisible = !styleIsNarrowScreen && refreshInterval.value == 0 && isDistancerButtonVisible.value,
                        src = "/web/images/ic_linear_scale_black_48dp.png",
                        title = "Измерение расстояний"
                    ) {
                        setMode(MapWorkMode.DISTANCER)
                    }
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isSelectButtonVisible.value,
                        src = "/web/images/ic_touch_app_black_48dp.png",
                        title = "Работа с объектами"
                    ) {
                        setMode(MapWorkMode.SELECT_FOR_ACTION)
                    }
                }
                getToolBarSpan {
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isZoomInButtonVisible.value,
                        src = "/web/images/ic_zoom_in_black_48dp.png",
                        title = "Ближе"
                    ) {
                        zoomIn()
                    }
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isZoomOutButtonVisible.value,
                        src = "/web/images/ic_zoom_out_black_48dp.png",
                        title = "Дальше"
                    ) {
                        zoomOut()
                    }
                }
                getToolBarSpan {
                    if (isAddElementButtonVisible.value) {
                        if (alAddEC.isNotEmpty()) {
                            Span {
                                Text("Добавить:")
                            }
                        }
                        for (ec in alAddEC) {
                            Button(
                                attrs = {
                                    style {
                                        backgroundColor(getColorButtonBack())
                                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                        fontSize(styleCommonButtonFontSize)
                                        padding(styleTextButtonPadding)
                                        setMargins(arrStyleCommonMargin)
                                        cursor("pointer")
                                    }
                                    title("Добавить ${ec.descrForAction}")
                                    onClick {
                                        startAdd(ec)
                                    }
                                }
                            ) {
                                Text(ec.descrForAction)
                            }
                        }
                    }
                }
                getToolBarSpan {
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isEditPointButtonVisible.value,
                        src = "/web/images/ic_format_shapes_black_48dp.png",
                        title = "Редактирование точек"
                    ) {
                        startEditPoint()
                    }
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isMoveElementsButtonVisible.value,
                        src = "/web/images/ic_zoom_out_map_black_48dp.png",
                        title = "Перемещение объектов"
                    ) {
                        startMoveElements()
                    }
                }
                getToolBarSpan {
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isActionOkButtonVisible.value,
                        src = "/web/images/ic_save_black_48dp.png",
                        title = "Сохранить"
                    ) {
                        actionOk()
                    }
                    getToolBarIconButton(
                        isVisible = refreshInterval.value == 0 && isActionCancelButtonVisible.value,
                        src = "/web/images/ic_exit_to_app_black_48dp.png",
                        title = "Отменить"
                    ) {
                        actionCancel()
                    }
                }
                if (isRefreshButtonVisible.value) {
                    getRefreshSubToolbar(refreshInterval) { interval ->
                        setInterval(interval)
                    }
                }
            }

            getXyElementTemplate(true)

            for (distancerText in alDistancerText) {
                Div(
                    attrs = {
                        style {
                            setDistancerTextStyle()
                            distancerText.pos.value(this)
                        }
                    }
                ) {
                    Text(distancerText.text.value)
                }
            }

            if (distancerSumTextVisible.value) {
                Div(
                    attrs = {
                        style {
                            setDistancerTextStyle()
                            distancerSumText.pos.value(this)
                        }
                    }
                ) {
                    Text(distancerSumText.text.value)
                }
            }
        }
    }

    private fun StyleScope.setDistancerTextStyle() {
        position(Position.Absolute)
        color(COLOR_MAIN_TEXT)
        backgroundColor(COLOR_XY_LABEL_BACK)
        textAlign("center")
        verticalAlign("baseline")
        setBorder(color = COLOR_XY_LABEL_BORDER, width = (1 * scaleKoef).px, radius = (2 * scaleKoef).px)
        setPaddings(arrStyleXyDistancerPadding)
        userSelect(if (styleIsNarrowScreen) "none" else "auto")
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @OptIn(ExperimentalComposeWebSvgApi::class)
    @Composable
    override fun addSpecifigSvg(svg: ElementScope<SVGElement>) {
        super.addSpecifigSvg(svg)

        svg.apply {
            if (mouseRect.isVisible.value) {
                Rect(
                    x = min(mouseRect.x1.value, mouseRect.x2.value),
                    y = min(mouseRect.y1.value, mouseRect.y2.value),
                    width = abs(mouseRect.x2.value - mouseRect.x1.value),
                    height = abs(mouseRect.y2.value - mouseRect.y1.value),
                    attrs = {
                        fill(COLOR_MAP_LINE.toString())
                        style {
                            opacity(0.25)
                        }
                    }
                )
                Line(
                    x1 = mouseRect.x1.value,
                    y1 = mouseRect.y1.value,
                    x2 = mouseRect.x2.value,
                    y2 = mouseRect.y1.value,
                    attrs = {
                        stroke(COLOR_MAP_LINE)
                        strokeWidth(COLOR_MAP_LINE_WIDTH)
                    }
                )
                Line(
                    x1 = mouseRect.x2.value,
                    y1 = mouseRect.y1.value,
                    x2 = mouseRect.x2.value,
                    y2 = mouseRect.y2.value,
                    attrs = {
                        stroke(COLOR_MAP_LINE)
                        strokeWidth(COLOR_MAP_LINE_WIDTH)
                    }
                )
                Line(
                    x1 = mouseRect.x2.value,
                    y1 = mouseRect.y2.value,
                    x2 = mouseRect.x1.value,
                    y2 = mouseRect.y2.value,
                    attrs = {
                        stroke(COLOR_MAP_LINE)
                        strokeWidth(COLOR_MAP_LINE_WIDTH)
                    }
                )
                Line(
                    x1 = mouseRect.x1.value,
                    y1 = mouseRect.y2.value,
                    x2 = mouseRect.x1.value,
                    y2 = mouseRect.y1.value,
                    attrs = {
                        stroke(COLOR_MAP_LINE)
                        strokeWidth(COLOR_MAP_LINE_WIDTH)
                    }
                )
            }

            for (distancerLine in alDistancerLine) {
                Line(
                    x1 = distancerLine.x1.value,
                    y1 = distancerLine.y1.value,
                    x2 = distancerLine.x2.value,
                    y2 = distancerLine.y2.value,
                    attrs = {
                        stroke(COLOR_MAP_DISTANCER)
                        strokeWidth((4 * scaleKoef).roundToInt())
                        strokeDasharray("${scaleKoef * 4},${scaleKoef * 4}")
                    }
                )
            }

            addElement.value?.let { element ->
                if (element.type == XyElementDataType.POLYGON) {
                    Polygon(
                        points = element.arrPoints.value,
                        attrs = {
                            element.transform?.let {
                                transform(element.transform)
                            }
                            stroke(if (element.itSelected) COLOR_XY_POLYGON_BORDER else (element.stroke ?: COLOR_TRANSPARENT))
                            element.strokeWidth?.let {
                                strokeWidth(element.strokeWidth)
                            }
                            if (element.itSelected) {
                                strokeDasharray(element.strokeDash ?: "")
                            } else {
                                strokeDasharray("")
                            }
                            element.fill?.let {
                                fill(element.fill.toString())
                            }
                        }
                    )
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
//    var mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта
//        //--- на текущий момент MapSurfer - наиболее правильная карта
//        val bitmapMapMode = appContainer.getUserProperty( iCoreAppContainer.UP_BITMAP_MAP_MODE )
//        mapBitmapTypeName = if( bitmapMapMode.isNullOrEmpty() ) XyBitmapType.MS else bitmapMapMode

        //--- подготовка данных для меню добавления
        alAddEC.addAll(xyResponse.documentConfig.alElementConfig.filter { it.second.descrForAction.isNotEmpty() }.map { it.second })

        doXyMounted(
            startExpandKoef = MAP_START_EXPAND_KOEF,
            isCentered = false,
            curScale = xyResponse.documentConfig.alElementConfig.minBy { it.second.scaleMin }.second.scaleMin
        )

        setMode(MapWorkMode.PAN)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun xyRefreshView(aView: XyViewCoord?, withWait: Boolean) {
        if (aView != null) {
            //--- вычисляем координату середины (безопасным от переполнения способом)
            //--- и выносим эту точку на середину экрана
            //--- и сохраненяем новое состояние в view
            val checkedView = getXyViewCoord(
                aScale = checkXyScale(
                    minScale = xyResponse.documentConfig.alElementConfig.minByOrNull { it.second.scaleMin }!!.second.scaleMin,
                    maxScale = xyResponse.documentConfig.alElementConfig.maxByOrNull { it.second.scaleMax }!!.second.scaleMax,
                    isScaleAlign = xyResponse.documentConfig.itScaleAlign,
                    curScale = xyViewCoord.scale,
                    newScale = aView.scale,
                    isAdaptive = false
                ),
                aCenterX = aView.x1 + (aView.x2 - aView.x1) / 2,
                aCenterY = aView.y1 + (aView.y2 - aView.y1) / 2,
            )
            xyViewCoord = checkedView
        }

        getXyElements(
            mapBitmapTypeName = mapBitmapTypeName,
            withWait = withWait,
        )

        //--- обновление в любом случае сбрасывает выделенность элементов и возможность соответствующих операций
        isEditPointButtonVisible.value = false
        isMoveElementsButtonVisible.value = false
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun onXyMouseOver(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
        when (curMode.toString()) {
            MapWorkMode.PAN.toString(),
            MapWorkMode.ZOOM_BOX.toString(),
            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                super.onXyMouseOver(syntheticMouseEvent, xyElement)
            }
        }
    }

    override fun onXyMousePressed(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        if (isNeedOffsetCompensation) {
            mouseX -= xySvgLeft
            mouseY -= xySvgTop
        }

        when (curMode) {
            MapWorkMode.PAN -> {
                panPointOldX = mouseX
                panPointOldY = mouseY
                panDX = 0
                panDY = 0
            }

            MapWorkMode.ZOOM_BOX, MapWorkMode.SELECT_FOR_ACTION -> {
                mouseRect.apply {
                    isVisible.value = true
                    x1.value = mouseX
                    y1.value = mouseY
                    x2.value = mouseX
                    y2.value = mouseY
                }
            }

            MapWorkMode.ACTION_EDIT_POINT -> {
                editElement?.let { editElement ->
                    val clickRect = getXyClickRect(mouseX, mouseY)
                    editPointIndex = -1
                    //--- попытаемся найти вершину, на которую кликнули
                    for (i in 0..editElement.alPoint!!.lastIndex)
                        if (clickRect.isContains(editElement.alPoint[i])) {
                            editPointIndex = i
                            break
                        }
                    //--- если кликнутую вершину не нашли, попытаемся найти отрезок, на который кликнули
                    if (editPointIndex == -1) {
                        for (i in 0..editElement.alPoint.lastIndex)
                            if (clickRect.isIntersects(
                                    XyLine(
                                        p1 = editElement.alPoint[i],
                                        p2 = editElement.alPoint[if (i == editElement.alPoint.lastIndex) 0 else (i + 1)]
                                    )
                                )
                            ) {
                                //--- в месте клика на отрезке добавляем точку, которую будем двигать
                                editPointIndex = i + 1
                                editElement.insertPoint(editPointIndex, mouseX, mouseY)
                                break
                            }
                    }
                }
            }

            MapWorkMode.ACTION_MOVE -> {
                moveStartPoint = XyPoint(mouseX, mouseY)
                moveEndPoint = XyPoint(mouseX, mouseY)
            }

            else -> {}
        }
        isMouseDown = true
    }

    override fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        if (isNeedOffsetCompensation) {
            mouseX -= xySvgLeft
            mouseY -= xySvgTop
        }

        //--- mouse dragged
        if (isMouseDown) {
            when (curMode.toString()) {
                MapWorkMode.PAN.toString() -> {
                    val dx = mouseX - panPointOldX
                    val dy = mouseY - panPointOldY

                    val arrViewBoxBody = getXyViewBoxBody()

                    arrViewBoxBody[0] -= dx
                    arrViewBoxBody[1] -= dy

                    panPointOldX = mouseX
                    panPointOldY = mouseY
                    panDX += dx
                    panDY += dy

                    setXyViewBoxBody(arrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))

                    setXyTextOffset()
                }

                MapWorkMode.ZOOM_BOX.toString(), MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                    if (mouseRect.isVisible.value && mouseX >= 0 && mouseX <= xySvgWidth.value && mouseY >= 0 && mouseY <= xySvgHeight.value) {
                        mouseRect.x2.value = mouseX
                        mouseRect.y2.value = mouseY
                    }
                }

                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    if (editPointIndex != -1) {
                        editElement?.setPoint(editPointIndex, mouseX, mouseY)
                    }
                }

                MapWorkMode.ACTION_MOVE.toString() -> {
                    moveEndPoint?.let { moveEndPoint ->
                        for (element in alMoveElement) {
                            element.moveRel(mouseX - moveEndPoint.x, mouseY - moveEndPoint.y)
                        }
                        moveEndPoint.set(mouseX, mouseY)
                    }
                }

                else -> {}
            }
        }
        //--- mouse moved
        else {
            when (curMode.toString()) {
                MapWorkMode.DISTANCER.toString() -> {
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

                MapWorkMode.ACTION_ADD.toString() -> {
                    addElement.value?.setLastPoint(mouseX, mouseY)
                }

                else -> {}
            }
        }
    }

    override fun onXyMouseReleased(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        if (isNeedOffsetCompensation) {
            mouseX -= xySvgLeft
            mouseY -= xySvgTop
        }

        when (curMode.toString()) {
            MapWorkMode.PAN.toString() -> {
                //--- перезагружаем карту, только если был горизонтальный сдвиг
                if (abs(panDX) >= 1 || abs(panDY) >= 1) {
                    xyViewCoord.moveRel((-panDX * xyViewCoord.scale / scaleKoef).roundToInt(), (-panDY * xyViewCoord.scale / scaleKoef).roundToInt())
                    xyRefreshView(xyViewCoord, true)
                }
                panPointOldX = 0
                panPointOldY = 0
                panDX = 0
            }

            MapWorkMode.ZOOM_BOX.toString() -> {
                if (mouseRect.isVisible.value) {
                    mouseRect.isVisible.value = false

                    val mouseWidth = abs(mouseRect.x2.value - mouseRect.x1.value)
                    val mouseHeight = abs(mouseRect.y2.value - mouseRect.y1.value)

                    //--- если размер прямоугольника меньше 8 pix, то это видимо ошибка - игнорируем
                    if (mouseWidth >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt() && mouseHeight >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt()) {

                        //--- установить показ этой области ( с наиболее близким и разрешенным масштабом )
                        //--- специально установлена работа с double-числами и округление в большую сторону
                        //--- из-за ошибок округления при масштабах, близких к 1
                        //--- (и scaleKoef здесь не нужен!!!)
                        val newScale = ceil(xyViewCoord.scale * max(1.0 * mouseWidth / xySvgWidth.value, 1.0 * mouseHeight / xySvgHeight.value)).toInt()
                        //--- переводим в мировые координаты
                        xyRefreshView(
                            XyViewCoord(
                                newScale,
                                xyViewCoord.x1 + mouseToReal(scaleKoef, xyViewCoord.scale, min(mouseRect.x1.value, mouseRect.x2.value)),
                                xyViewCoord.y1 + mouseToReal(scaleKoef, xyViewCoord.scale, min(mouseRect.y1.value, mouseRect.y2.value)),
                                xyViewCoord.x1 + mouseToReal(scaleKoef, xyViewCoord.scale, max(mouseRect.x1.value, mouseRect.x2.value)),
                                xyViewCoord.y1 + mouseToReal(scaleKoef, xyViewCoord.scale, max(mouseRect.y1.value, mouseRect.y2.value)),
                            ),
                            true
                        )
                    }
                }
            }

            MapWorkMode.DISTANCER.toString() -> {
                //--- при первом клике заводим сумму, отключаем тулбар и включаем кнопку отмены линейки
                val (newX, newY) = getGraphixAndXyTooltipCoord(xySvgLeft + mouseX, xySvgTop + mouseY)
                if (alDistancerLine.isEmpty()) {
                    distancerSumText.apply {
                        text.value = "0.0"
                        pos.value = {
                            left(newX.px)
                            top(newY.px)
                        }
                    }
                    distancerSumTextVisible.value = true
                    disableToolbar()
                    isActionCancelButtonVisible.value = true
                }
                alDistancerLine.add(
                    DistancerLineData(
                        x1 = mutableStateOf(mouseX),
                        y1 = mutableStateOf(mouseY),
                        x2 = mutableStateOf(mouseX),
                        y2 = mutableStateOf(mouseY),
                    )
                )

                alDistancerDist.add(0.0)

                alDistancerText.add(
                    DistancerTextData().apply {
                        text.value = "0.0"
                        pos.value = {
                            left(newX.px)
                            top(newY.px)
                        }
                    }
                )
            }

            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                if (mouseRect.isVisible.value) {
                    mouseRect.isVisible.value = false

                    //--- установим опцию выбора
                    val selectOption =
                        if (shiftKey) SelectOption.ADD
                        else if (ctrlKey) SelectOption.REVERT
                        else if (altKey) SelectOption.DELETE
                        else SelectOption.SET

                    //--- в обычном режиме ( т.е. без доп.клавиш ) предварительно развыберем остальные элементы
                    if (selectOption == SelectOption.SET) {
                        xyDeselectAll()
                    }

                    val mouseXyRect = XyRect(
                        min(mouseRect.x1.value, mouseRect.x2.value), min(mouseRect.y1.value, mouseRect.y2.value),
                        abs(mouseRect.x1.value - mouseRect.x2.value), abs(mouseRect.y1.value - mouseRect.y2.value)
                    )
                    var editableElementCount = 0
                    isMoveElementsButtonVisible.value = false
                    alMoveElement.clear()
                    for (element in getXyElementList(mouseXyRect, true)) {
                        element.itSelected = when (selectOption) {
                            SelectOption.SET,
                            SelectOption.ADD -> {
                                true
                            }

                            SelectOption.REVERT -> {
                                !element.itSelected
                            }

                            SelectOption.DELETE -> {
                                false
                            }
                        }
                        if (element.itSelected) {
                            if (element.itEditablePoint) {
                                editableElementCount++
                                editElement = element
                            }
                            if (element.itMoveable) {
                                //--- предварительная краткая проверка на наличие выбранных передвигабельных объектов
                                isMoveElementsButtonVisible.value = true
                                alMoveElement.add(element)
                            }
                        }
                    }
                    //--- проверка на возможность создания элементов при данном масштабе - пока не проверяем, т.к. геозоны можно создавать при любом масштабе
                    //for( mi in hmAddMenuEC.keys ) {
                    //    val tmpActionAddEC = hmAddMenuEC[ mi ]!!
                    //    mi.isDisable = xyModel.viewCoord.scale < tmpActionAddEC.scaleMin || xyModel.viewCoord.scale > tmpActionAddEC.scaleMax
                    //}
                    isEditPointButtonVisible.value = editableElementCount == 1
                }
            }

            MapWorkMode.ACTION_ADD.toString() -> {
                val actionAddPointStatus = addElement.value?.addPoint(mouseX, mouseY)

                if (actionAddPointStatus == AddPointStatus.COMPLETED) {
                    actionOk()
                } else {
                    isActionOkButtonVisible.value = actionAddPointStatus == AddPointStatus.COMPLETEABLE
                }
            }

            MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                if (editPointIndex != -1) {
                    editOnePoint()
                    editPointIndex = -1
                }
            }

            MapWorkMode.ACTION_MOVE.toString() -> {
                doMoveElements()
                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }
        }
        isMouseDown = false
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
                minScale = xyResponse.documentConfig.alElementConfig.minByOrNull { it.second.scaleMin }!!.second.scaleMin,
                maxScale = xyResponse.documentConfig.alElementConfig.maxByOrNull { it.second.scaleMax }!!.second.scaleMax,
                isScaleAlign = xyResponse.documentConfig.itScaleAlign,
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

    //--- преобразование экранных координат в мировые
    private fun mouseToReal(scaleKoef: Double, scale: Int, screenCoord: Int): Int = (screenCoord * scale / scaleKoef).roundToInt()

    private fun setMode(newMode: MapWorkMode) {
        when (curMode.toString()) {
            MapWorkMode.PAN.toString() -> {
                isPanButtonVisible.value = true
            }

            MapWorkMode.ZOOM_BOX.toString() -> {
                isZoomButtonVisible.value = true
            }

            MapWorkMode.DISTANCER.toString() -> {
                isDistancerButtonVisible.value = true
                isActionCancelButtonVisible.value = false
                alDistancerLine.clear()
                alDistancerDist.clear()
                alDistancerText.clear()
                distancerSumText.text.value = ""
                distancerSumTextVisible.value = false
            }

            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                isSelectButtonVisible.value = true

                isAddElementButtonVisible.value = false
                isEditPointButtonVisible.value = false
                isMoveElementsButtonVisible.value = false
            }

            MapWorkMode.ACTION_ADD.toString(),
            MapWorkMode.ACTION_EDIT_POINT.toString(),
            MapWorkMode.ACTION_MOVE.toString() -> {
                enableToolbar()
            }
        }

        when (newMode.toString()) {
            MapWorkMode.PAN.toString() -> {
                isPanButtonVisible.value = false
                xyDeselectAll()
            }

            MapWorkMode.ZOOM_BOX.toString() -> {
                isZoomButtonVisible.value = false
                xyDeselectAll()
            }

            MapWorkMode.DISTANCER.toString() -> {
                isDistancerButtonVisible.value = false
                xyDeselectAll()
            }

            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                isSelectButtonVisible.value = false
                isAddElementButtonVisible.value = true
                isEditPointButtonVisible.value = false
                isMoveElementsButtonVisible.value = false
                isActionOkButtonVisible.value = false
                isActionCancelButtonVisible.value = false
            }

            MapWorkMode.ACTION_ADD.toString() -> {
                disableToolbar()
                isActionOkButtonVisible.value = false
                isActionCancelButtonVisible.value = true
            }

            MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                disableToolbar()
                isActionOkButtonVisible.value = true
                isActionCancelButtonVisible.value = true
            }

            MapWorkMode.ACTION_MOVE.toString() -> {
                disableToolbar()
                isActionOkButtonVisible.value = false
                isActionCancelButtonVisible.value = true
            }
        }
        //--- извращение для правильного сохранения enum в .data (а то в следующий раз в setMode не узнает)
        curMode = MapWorkMode.valueOf(newMode.toString())
    }

    private fun zoomIn() {
        //--- проверить масштаб
        val newScale = checkXyScale(
            minScale = xyResponse.documentConfig.alElementConfig.minByOrNull { it.second.scaleMin }!!.second.scaleMin,
            maxScale = xyResponse.documentConfig.alElementConfig.maxByOrNull { it.second.scaleMax }!!.second.scaleMax,
            isScaleAlign = xyResponse.documentConfig.itScaleAlign,
            curScale = xyViewCoord.scale,
            newScale = xyViewCoord.scale / 2,
            isAdaptive = false,
        )

        xyViewCoord.scale = newScale

        xyRefreshView(xyViewCoord, true)
    }

    private fun zoomOut() {
        //--- проверить масштаб
        val newScale = checkXyScale(
            minScale = xyResponse.documentConfig.alElementConfig.minByOrNull { it.second.scaleMin }!!.second.scaleMin,
            maxScale = xyResponse.documentConfig.alElementConfig.maxByOrNull { it.second.scaleMax }!!.second.scaleMax,
            isScaleAlign = xyResponse.documentConfig.itScaleAlign,
            curScale = xyViewCoord.scale,
            newScale = xyViewCoord.scale * 2,
            isAdaptive = false,
        )

        xyViewCoord.scale = newScale

        xyRefreshView(xyViewCoord, true)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun disableToolbar() {
        isPanButtonVisible.value = false
        isZoomButtonVisible.value = false
        isDistancerButtonVisible.value = false
        isSelectButtonVisible.value = false

        isZoomInButtonVisible.value = false
        isZoomOutButtonVisible.value = false

        isRefreshButtonVisible.value = false
    }

    private fun enableToolbar() {
        isPanButtonVisible.value = true
        isZoomButtonVisible.value = true
        isDistancerButtonVisible.value = true
        isSelectButtonVisible.value = true

        isZoomInButtonVisible.value = true
        isZoomOutButtonVisible.value = true

        isRefreshButtonVisible.value = true
    }

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
        when (curMode.toString()) {
            MapWorkMode.ACTION_ADD.toString() -> {
                addElement.value?.doAddElement(root, this, xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, xyViewCoord)
                addElement.value = null
            }

            MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                editElement?.doEditElementPoint(root, this, xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, xyViewCoord)
            }
        }
        //that().xyRefreshView( null, null, true ) - делается внути методов doAdd/doEdit/doMove по завершении операций
        setMode(MapWorkMode.SELECT_FOR_ACTION)
    }

    private fun actionCancel() {
        when (curMode.toString()) {
            MapWorkMode.DISTANCER.toString() -> {
                //--- включить кнопки, но кнопку линейки выключить обратно
                enableToolbar()
                isDistancerButtonVisible.value = false
                alDistancerLine.clear()
                alDistancerDist.clear()
                alDistancerText.clear()
                distancerSumText.text.value = ""
                distancerSumTextVisible.value = false
                isActionCancelButtonVisible.value = false
            }

            MapWorkMode.ACTION_ADD.toString() -> {
                addElement.value = null
                xyRefreshView(null, true)
                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }

            MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                editElement = null
                xyRefreshView(null, true)
                setMode(MapWorkMode.SELECT_FOR_ACTION)
            }
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

private class DistancerLineData(
    val x1: MutableState<Int>,
    val y1: MutableState<Int>,
    val x2: MutableState<Int>,
    val y2: MutableState<Int>,
)

private class DistancerTextData(
    val pos: MutableState<StyleScope.() -> Unit> = mutableStateOf({}),
    val text: MutableState<String> = mutableStateOf(""),
)

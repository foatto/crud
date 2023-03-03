package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.config.XyBitmapType
import foatto.core.link.XyResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.style.arrStyleCommonMargin
import foatto.core_compose_web.style.scaleKoef
import foatto.core_compose_web.style.setBorder
import foatto.core_compose_web.style.setMargins
import foatto.core_compose_web.style.styleCommonButtonFontSize
import foatto.core_compose_web.style.styleIconButtonPadding
import foatto.core_compose_web.style.styleIsNarrowScreen
import foatto.core_compose_web.util.MIN_USER_RECT_SIZE
import foatto.core_compose_web.util.MouseRectData
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Img
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

private val COLOR_XY_LINE: CSSColorValue = hsl(180, 100, 50)

private const val mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта

private const val START_EXPAND_KOEF = 0.05

private const val MAP_PREFIX = "map"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class MapControl(
    root: Root,
    appControl: AppControl,
    xyResponse: XyResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, xyResponse, tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val isPanButtonDisabled = mutableStateOf(true)
    private val isZoomButtonDisabled = mutableStateOf(false)
    private val isDistancerButtonDisabled = mutableStateOf(false)
    private val isSelectButtonDisabled = mutableStateOf(false)

    private val isZoomInButtonDisabled = mutableStateOf(false)
    private val isZoomOutButtonDisabled = mutableStateOf(false)

    //                "arrAddEC" to arrayOf<XyElementConfig>(),
//                "isAddElementButtonVisible" to false,
    private val isEditPointButtonVisible = mutableStateOf(false)
    private val isMoveElementsButtonVisible = mutableStateOf(false)

    private val isActionOkButtonVisible = mutableStateOf(false)
    private val isActionCancelButtonVisible = mutableStateOf(false)

    private val isRefreshButtonDisabled = mutableStateOf(false)

//    private val alDistancerLine = mutableStateListOf<XyElementData>()
//    private val alDistancerDist = mutableStateListOf<Double>()
//    private val alDistancerText = mutableStateListOf<XyElementData>()
//    private val distancerSumText = XyElementData(type = XyElementDataType.HTML_TEXT, elementId = -1, objectId = -1)

    private val mouseRect = MouseRectData(
        isVisible = mutableStateOf(false),
        x1 = mutableStateOf(0),
        y1 = mutableStateOf(0),
        x2 = mutableStateOf(0),
        y2 = mutableStateOf(0),
        lineWidth = mutableStateOf(1),
    )

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var curMode = MapWorkMode.PAN
    private var isMouseDown = false

    private var panPointOldX = 0
    private var panPointOldY = 0
    private var panDX = 0
    private var panDY = 0

//                "addElement" to null,
//                "editElement" to null,
//                "editPointIndex" to -1,
//
//                "arrMoveElement" to null,
//                "moveStartPoint" to null,
//                "moveEndPoint" to null

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- Map Header
            getGraphicAndXyHeader(MAP_PREFIX)

            //--- Map Toolbar
            getGraphicAndXyToolbar(MAP_PREFIX) {
                getToolBarSpan {
                    getToolBarIconButton("/web/images/ic_open_with_black_48dp.png", "Перемещение по карте", { setMode(MapWorkMode.PAN) })
                    getToolBarIconButton("/web/images/ic_search_black_48dp.png", "Выбор области для показа", { setMode(MapWorkMode.ZOOM_BOX) })
                    if (!styleIsNarrowScreen) {
                        getToolBarIconButton("/web/images/ic_linear_scale_black_48dp.png", "Измерение расстояний", { setMode(MapWorkMode.DISTANCER) })
                    }
                    getToolBarIconButton("/web/images/ic_touch_app_black_48dp.png", "Работа с объектами", { setMode(MapWorkMode.SELECT_FOR_ACTION) })
                }
                getToolBarSpan {
                    getToolBarIconButton("/web/images/ic_zoom_in_black_48dp.png", "Ближе", { zoomIn() })
                    getToolBarIconButton("/web/images/ic_zoom_out_black_48dp.png", "Дальше", { zoomOut() })
                }
                getToolBarSpan {
//                <template v-if="isAddElementButtonVisible">
//                    <span v-show="arrAddEC.length > 0">
//                        Добавить:
//                    </span>
//                    <button v-for="ec in arrAddEC"
//                            v-on:click="startAdd( ec )"
//                            v-bind:style="style_text_button"
//                            v-bind:title="'Добавить `' + ec.descrForAction + '`'"
//                    >
//                        {{ec.descrForAction}}
//                    </button>
//                </template>
//    "style_text_button" to json(
//        "background" to colorButtonBack(),
//        "border" to "1px solid ${colorButtonBorder()}",
//        "border-radius" to styleButtonBorderRadius,
//        "font-size" to styleCommonButtonFontSize(),
//        "padding" to styleTextButtonPadding(),//styleCommonEditorPadding(),
//        "margin" to styleCommonMargin(),
//        "cursor" to "pointer"
//    ),
                }
//                getToolBarSpan {
//                    if (isEditPointButtonVisible.value) {
//                        getToolBarIconButton("/web/images/ic_format_shapes_black_48dp.png", "Редактирование точек", { startEditPoint() })
//                    }
//                    if (isMoveElementsButtonVisible.value) {
//                        getToolBarIconButton("/web/images/ic_zoom_out_map_black_48dp.png", "Перемещение объектов", { startMoveElements() })
//                    }
//                }
//                getToolBarSpan {
//                    if (isActionOkButtonVisible.value) {
//                        getToolBarIconButton("/web/images/ic_save_black_48dp.png", "Сохранить", { actionOk() })
//                    }
//                    if (isActionCancelButtonVisible.value) {
//                        getToolBarIconButton("/web/images/ic_exit_to_app_black_48dp.png", "Отменить", { actionCancel() })
//                    }
//                }
                getToolBarSpan {
                    Img(
                        src = "/web/images/ic_sync_black_48dp.png",
                        attrs = {
                            style {
                                backgroundColor(getColorRefreshButtonBack())
                                setBorder(getStyleToolbarButtonBorder())
                                fontSize(styleCommonButtonFontSize)
                                padding(styleIconButtonPadding)
                                setMargins(arrStyleCommonMargin)
                                cursor("pointer")
                            }
                            title("Обновить")
                            onClick {
                                xyRefreshView(null, true)
                            }
                        }
                    )
                }
            }

            getXyElementTemplate(true)

//            for (distancerText in alDistancerText) {
//                Div(
//                    attrs = {
//                        style {
////             v-bind:style="[distancerText.pos, distancerText.style]"
//                        }
//                    }
//                ) {
//                    Text(distancerText.text.value)
//                }
//            }
//
//            if (distancerSumText.isVisible.value) {
//                Div(
//                    attrs = {
//                        style {
////             v-bind:style="[distancerSumText.pos, distancerSumText.style]"
//                        }
//                    }
//                ) {
//                    Text(distancerSumText.text.value)
//                }
//            }
        }
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
                        fill(COLOR_XY_LINE.toString())
//                        opacity(0.25)
                    }
                )
                Line(
                    x1 = mouseRect.x1.value,
                    y1 = mouseRect.y1.value,
                    x2 = mouseRect.x2.value,
                    y2 = mouseRect.y1.value,
                    attrs = {
                        attr("stroke", COLOR_XY_LINE.toString())
                        attr("stroke-width", mouseRect.lineWidth.toString())
                    }
                )
                Line(
                    x1 = mouseRect.x2.value,
                    y1 = mouseRect.y1.value,
                    x2 = mouseRect.x2.value,
                    y2 = mouseRect.y2.value,
                    attrs = {
                        attr("stroke", COLOR_XY_LINE.toString())
                        attr("stroke-width", mouseRect.lineWidth.toString())
                    }
                )
                Line(
                    x1 = mouseRect.x2.value,
                    y1 = mouseRect.y2.value,
                    x2 = mouseRect.x1.value,
                    y2 = mouseRect.y2.value,
                    attrs = {
                        attr("stroke", COLOR_XY_LINE.toString())
                        attr("stroke-width", mouseRect.lineWidth.toString())
                    }
                )
                Line(
                    x1 = mouseRect.x1.value,
                    y1 = mouseRect.y2.value,
                    x2 = mouseRect.x1.value,
                    y2 = mouseRect.y1.value,
                    attrs = {
                        attr("stroke", COLOR_XY_LINE.toString())
                        attr("stroke-width", mouseRect.lineWidth.toString())
                    }
                )
            }

//            for (distancerLine in alDistancerLine) {
//                Line(
//                    x1 = distancerLine.x1!!,
//                    y1 = distancerLine.y1!!,
//                    x2 = distancerLine.x2!!,
//                    y2 = distancerLine.y2!!,
//                    attrs = {
//                        attr("stroke", distancerLine.stroke!!)
//                        attr("stroke-width", distancerLine.strokeWidth.toString())
//                        attr("stroke-dasharray", distancerLine.strokeDash)
//                    }
//                )
//            }

//        <template v-if="addElement">
//            <polygon v-if="addElement.type == '${XyElementDataType.POLYGON}'"
//                     v-bind:points="addElement.points"
//                     v-bind:stroke="addElement.itSelected ? '$COLOR_XY_ZONE_BORDER' : addElement.stroke"
//                     v-bind:fill="addElement.fill"
//                     v-bind:stroke-width="addElement.strokeWidth"
//                     v-bind:stroke-dasharray="addElement.itSelected ? addElement.strokeDash : ''"
//                     v-bind:transform="addElement.transform"
//            />
//        </template>
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
//    var mapBitmapTypeName = XyBitmapType.MS   // на текущий момент MapSurfer - наиболее правильная карта
//        //--- на текущий момент MapSurfer - наиболее правильная карта
//        val bitmapMapMode = appContainer.getUserProperty( iCoreAppContainer.UP_BITMAP_MAP_MODE )
//        mapBitmapTypeName = if( bitmapMapMode.isNullOrEmpty() ) XyBitmapType.MS else bitmapMapMode

        //--- подготовка данных для меню добавления
//        arrAddEC = xyResponse.documentConfig.alElementConfig.filter { it.second.descrForAction.isNotEmpty() }.map { it.second }.toTypedArray()

        doXyMounted(
            elementPrefix = MAP_PREFIX,
            startExpandKoef = START_EXPAND_KOEF,
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
                    lineWidth.value = max(1.0, scaleKoef).roundToInt()
                }
            }
//            MapWorkMode.ACTION_EDIT_POINT -> {
//                val clickRect = getXyClickRect(mouseX, mouseY)
//                val editElement = that().editElement.unsafeCast<XyElementData>()
//                var editPointIndex = -1
//                //--- попытаемся найти вершину, на которую кликнули
//                for (i in 0..editElement.alPoint!!.lastIndex)
//                    if (clickRect.isContains(editElement.alPoint[i])) {
//                        editPointIndex = i
//                        break
//                    }
//                //--- если кликнутую вершину не нашли, попытаемся найти отрезок, на который кликнули
//                if (editPointIndex == -1) {
//                    for (i in 0..editElement.alPoint.lastIndex)
//                        if (clickRect.isIntersects(
//                                XyLine(
//                                    editElement.alPoint[i],
//                                    editElement.alPoint[if (i == editElement.alPoint.lastIndex) 0 else (i + 1)]
//                                )
//                            )
//                        ) {
//                            //--- в месте клика на отрезке добавляем точку, которую будем двигать
//                            editPointIndex = i + 1
//                            editElement.insertPoint(editPointIndex, mouseX, mouseY)
//                            break
//                        }
//                }
//                that().editPointIndex = editPointIndex
//            }
//            MapWorkMode.ACTION_MOVE -> {
//                that.moveStartPoint = XyPoint(mouseX, mouseY)
//                that.moveEndPoint = XyPoint(mouseX, mouseY)
//            }
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
//                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
//                    val editElement = that().editElement.unsafeCast<XyElementData>()
//                    val editPointIndex = that().editPointIndex.unsafeCast<Int>()
//                    if (editPointIndex != -1)
//                        editElement.setPoint(editPointIndex, mouseX, mouseY)
//                }
//                MapWorkMode.ACTION_MOVE.toString() -> {
//                    val arrMoveElement = that().arrMoveElement.unsafeCast<Array<XyElementData>>()
//                    val moveEndPoint = that().moveEndPoint.unsafeCast<XyPoint>()
//                    for (element in arrMoveElement)
//                        element.moveRel(mouseX - moveEndPoint.x, mouseY - moveEndPoint.y)
//                    moveEndPoint.set(mouseX, mouseY)
//                    that().moveEndPoint = moveEndPoint
//                }
                else -> {}
            }
        }
        //--- mouse moved
        else {
            when (curMode.toString()) {
                MapWorkMode.DISTANCER.toString() -> {
//                    val alDistancerLine = that().arrDistancerLine.unsafeCast<Array<XyElementData>>().toMutableList()
//                    val alDistancerDist = that().arrDistancerDist.unsafeCast<Array<Double>>().toMutableList()
//                    val alDistancerText = that().arrDistancerText.unsafeCast<Array<XyElementData>>().toMutableList()
//                    val distancerSumText = that().distancerSumText.unsafeCast<XyElementData>()
//
//                    if (alDistancerLine.isNotEmpty()) {
//                        val line = alDistancerLine.last()
//                        line.x2 = mouseX
//                        line.y2 = mouseY
//
//                        val dist = XyProjection.distancePrj(
//                            XyPoint(
//                                viewCoord.x1 + mouseToReal(scaleKoef, viewCoord.scale, line.x1!!),
//                                viewCoord.y1 + mouseToReal(scaleKoef, viewCoord.scale, line.y1!!)
//                            ),
//                            XyPoint(
//                                viewCoord.x1 + mouseToReal(scaleKoef, viewCoord.scale, line.x2!!),
//                                viewCoord.y1 + mouseToReal(scaleKoef, viewCoord.scale, line.y2!!)
//                            ),
//                            viewCoord.scale
//                        ) / 1000.0
//
//                        alDistancerDist[alDistancerDist.lastIndex] = dist
//
//                        val distancerSumDist = alDistancerDist.sum()
//
//                        val text = alDistancerText.last()
//                        text.x = (line.x1 + line.x2!!) / 2
//                        text.y = (line.y1 + line.y2!!) / 2
//                        text.text = getSplittedDouble(dist, 1, true, '.')
//                        text.pos = json(
//                            "left" to "${svgCoords.bodyLeft + text.x!!}px",
//                            "top" to "${svgCoords.bodyTop + text.y!!}px"
//                        )
//
//                        distancerSumText.x = mouseX
//                        distancerSumText.y = mouseY
//                        //--- иногда вышибает округлятор в getSplittedDouble
//                        distancerSumText.text =
//                            try {
//                                getSplittedDouble(distancerSumDist, 1, true, '.')
//                            } catch (t: Throwable) {
//                                distancerSumText.text
//                            }
//                        distancerSumText.pos = json(
//                            "left" to "${svgCoords.bodyLeft + mouseX + 16}px",
//                            "top" to "${svgCoords.bodyTop + mouseY + 16}px"
//                        )
//
//                        that().arrDistancerLine = alDistancerLine.toTypedArray()
//                        that().arrDistancerDist = alDistancerDist.toTypedArray()
//                        that().arrDistancerText = alDistancerText.toTypedArray()
//                        //that().distancerSumText = distancerSumText - излишне
//                    }
                }

                MapWorkMode.ACTION_ADD.toString() -> {
//                    val addElement = that().addElement.unsafeCast<XyElementData>()
//                    addElement.setLastPoint(mouseX, mouseY)
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

//            MapWorkMode.DISTANCER.toString() -> {
//                val alDistancerLine = that().arrDistancerLine.unsafeCast<Array<XyElementData>>().toMutableList()
//                val alDistancerDist = that().arrDistancerDist.unsafeCast<Array<Double>>().toMutableList()
//                val alDistancerText = that().arrDistancerText.unsafeCast<Array<XyElementData>>().toMutableList()
//
//                //--- при первом клике заводим сумму, отключаем тулбар и включаем кнопку отмены линейки
//                if (alDistancerLine.isEmpty()) {
//                    that().distancerSumText = XyElementData(
//                        type = XyElementDataType.HTML_TEXT,
//                        elementId = -getRandomInt(),
//                        objectId = 0,
//                        x = mouseX,
//                        y = mouseY,
//                        text = "0.0",
//                        pos = json(
//                            "left" to "${svgCoords.bodyLeft + mouseX + 16}px",
//                            "top" to "${svgCoords.bodyTop + mouseY + 16}px"
//                        ),
//                        style = json(
//                            "position" to "absolute",
//                            "color" to COLOR_MAIN_TEXT,
//                            "text-align" to "center",
//                            "vertical-align" to "baseline",
//                            "border-radius" to "${2 * scaleKoef}px",
//                            "border" to "${1 * scaleKoef}px solid $COLOR_XY_LABEL_BORDER",
//                            "background" to COLOR_XY_LABEL_BACK,
//                            "padding" to styleXyDistancerPadding(),
//                            "user-select" to if (styleIsNarrowScreen) "none" else "auto"
//                        )
//                    )
//                    disableToolbar(that())
//                    that().isActionCancelButtonVisible = true
//                }
//                alDistancerLine.add(
//                    XyElementData(
//                        type = XyElementDataType.LINE,
//                        elementId = -getRandomInt(),
//                        objectId = 0,
//                        x1 = mouseX,
//                        y1 = mouseY,
//                        x2 = mouseX,
//                        y2 = mouseY,
//                        stroke = COLOR_XY_DISTANCER,
//                        strokeWidth = (4 * scaleKoef).roundToInt(),
//                        strokeDash = "${scaleKoef * 4},${scaleKoef * 4}"
//                    )
//                )
//
//                alDistancerDist.add(0.0)
//
//                alDistancerText.add(
//                    XyElementData(
//                        type = XyElementDataType.HTML_TEXT,
//                        elementId = -getRandomInt(),
//                        objectId = 0,
//                        x = mouseX,
//                        y = mouseY,
//                        text = "0.0",
//                        pos = json(
//                            "left" to "${svgCoords.bodyLeft + mouseX}px",
//                            "top" to "${svgCoords.bodyTop + mouseY}px"
//                        ),
//                        style = json(
//                            "position" to "absolute",
//                            "color" to COLOR_MAIN_TEXT,
//                            "text-align" to "center",
//                            "vertical-align" to "baseline",
//                            "border-radius" to "${2 * scaleKoef}px",
//                            "border" to "${1 * scaleKoef}px solid $COLOR_XY_LABEL_BORDER",
//                            "background" to COLOR_XY_LABEL_BACK,
//                            "padding" to styleXyDistancerPadding(),
//                            "user-select" to if (styleIsNarrowScreen) "none" else "auto"
//                        )
//                    )
//                )
//
//                that().arrDistancerLine = alDistancerLine.toTypedArray()
//                that().arrDistancerDist = alDistancerDist.toTypedArray()
//                that().arrDistancerText = alDistancerText.toTypedArray()
//            }
//
//            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
//                val mouseRect = that().mouseRect.unsafeCast<MouseRectData>()
//
//                if (mouseRect.isVisible) {
//                    mouseRect.isVisible = false
//
//                    //--- установим опцию выбора
//                    val selectOption =
//                        if (shiftKey) SelectOption.ADD
//                        else if (ctrlKey) SelectOption.REVERT
//                        else if (altKey) SelectOption.DELETE
//                        else SelectOption.SET
//
//                    //--- в обычном режиме ( т.е. без доп.клавиш ) предварительно развыберем остальные элементы
//                    if (selectOption == SelectOption.SET) xyDeselectAll(that())
//
//                    val mouseXyRect = XyRect(
//                        min(mouseRect.x1, mouseRect.x2), min(mouseRect.y1, mouseRect.y2),
//                        abs(mouseRect.x1 - mouseRect.x2), abs(mouseRect.y1 - mouseRect.y2)
//                    )
//                    var editableElementCount = 0
//                    var editElement: XyElementData? = null
//                    var itMoveable = false
//                    val alMoveElement = mutableListOf<XyElementData>()
//                    for (element in getXyElementList(that(), mouseXyRect, true)) {
//                        element.itSelected = when (selectOption) {
//                            SelectOption.SET,
//                            SelectOption.ADD -> true
//                            SelectOption.REVERT -> !element.itSelected
//                            SelectOption.DELETE -> false
//                        }
//                        if (element.itSelected) {
//                            if (element.itEditablePoint) {
//                                editableElementCount++
//                                editElement = element
//                            }
//                            if (element.itMoveable) {
//                                itMoveable = true
//                                alMoveElement.add(element)
//                            }
//                        }
//                    }
//                    //--- проверка на возможность создания элементов при данном масштабе - пока не проверяем, т.к. геозоны можно создавать при любом масштабе
//                    //for( mi in hmAddMenuEC.keys ) {
//                    //    val tmpActionAddEC = hmAddMenuEC[ mi ]!!
//                    //    mi.isDisable = xyModel.viewCoord.scale < tmpActionAddEC.scaleMin || xyModel.viewCoord.scale > tmpActionAddEC.scaleMax
//                    //}
//                    that().isEditPointButtonVisible = editableElementCount == 1
//                    that().editElement = editElement
//                    //--- предварительная краткая проверка на наличие выбранных передвигабельных объектов
//                    that().isMoveElementsButtonVisible = itMoveable
//                    that().arrMoveElement = alMoveElement.toTypedArray()
//                }
//            }
//            MapWorkMode.ACTION_ADD.toString() -> {
//                val addElement = that().addElement.unsafeCast<XyElementData>()
//
//                val actionAddPointStatus = addElement.addPoint(mouseX, mouseY)
//
//                if (actionAddPointStatus == AddPointStatus.COMPLETED) that().doAddElement()
//                else that().isActionOkButtonVisible = actionAddPointStatus == AddPointStatus.COMPLETEABLE
//            }
//            MapWorkMode.ACTION_EDIT_POINT.toString() -> {
//                val editPointIndex = that().editPointIndex.unsafeCast<Int>()
//                if (editPointIndex != -1) {
//                    editOnePoint(that())
//                    that().editPointIndex = -1
//                }
//            }
//            MapWorkMode.ACTION_MOVE.toString() -> {
//                doMoveElements(that(), xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, viewCoord)
//                that().setMode(MapWorkMode.SELECT_FOR_ACTION)
//            }
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
                isPanButtonDisabled.value = false
            }

            MapWorkMode.ZOOM_BOX.toString() -> {
                isZoomButtonDisabled.value = false
            }

            MapWorkMode.DISTANCER.toString() -> {
                isDistancerButtonDisabled.value = false
                isActionCancelButtonVisible.value = false
//                arrDistancerLine = arrayOf<XyElementData>()
//                arrDistancerDist = arrayOf<Double>()
//                arrDistancerText = arrayOf<XyElementData>()
//                distancerSumText = null
            }

            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                isSelectButtonDisabled.value = false

//                isAddElementButtonVisible.value = false
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
                isPanButtonDisabled.value = true
//                    stackPane.cursor = Cursor.MOVE
                xyDeselectAll()
            }

            MapWorkMode.ZOOM_BOX.toString() -> {
                isZoomButtonDisabled.value = true
//                    stackPane.cursor = Cursor.CROSSHAIR
                xyDeselectAll()
            }

            MapWorkMode.DISTANCER.toString() -> {
                isDistancerButtonDisabled.value = true
//                    stackPane.cursor = Cursor.CROSSHAIR
                xyDeselectAll()
            }

            MapWorkMode.SELECT_FOR_ACTION.toString() -> {
                isSelectButtonDisabled.value = true
                //stackPane.cursor = Cursor.DEFAULT
//                isAddElementButtonVisible = true
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
        isPanButtonDisabled.value = true
        isZoomButtonDisabled.value = true
        isDistancerButtonDisabled.value = true
        isSelectButtonDisabled.value = true
    
        isZoomInButtonDisabled.value = true
        isZoomOutButtonDisabled.value = true
    
        isRefreshButtonDisabled.value = true
    }
    
    private fun enableToolbar() {
        isPanButtonDisabled.value = false
        isZoomButtonDisabled.value = false
        isDistancerButtonDisabled.value = false
        isSelectButtonDisabled.value = false
    
        isZoomInButtonDisabled.value = false
        isZoomOutButtonDisabled.value = false
    
        isRefreshButtonDisabled.value = false
    }

}

/*
    this.methods = json(
        "startAdd" to { elementConfig: XyElementConfig ->
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()

            that().addElement = getXyEmptyElementData(scaleKoef, elementConfig)
            that().setMode(MapWorkMode.ACTION_ADD)
        },
        "startEditPoint" to {
            //--- в старой версии мы предварительно прятали из модели текущую (адаптированную под текущий масштаб и координаты)
            //--- версию addElement, чтобы не мешала загрузке и работе с полной версией со всеми негенерализованными точками.
            //--- учитывая, что интерактив у нас сейчас идёт только с зонами, нарисованными вручную и точки там далеки друг от друга и не подвержены генерализации,
            //--- можно считать, что загрузка полной копии редактируемого элемента не нужна
            that().setMode(MapWorkMode.ACTION_EDIT_POINT)
        },
        "startMoveElements" to {
            that().setMode(MapWorkMode.ACTION_MOVE)
        },
        "actionOk" to {
            val scaleKoef = that().`$root`.scaleKoef.unsafeCast<Double>()
            val viewCoord = that().xyViewCoord.unsafeCast<XyViewCoord>()
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when (curMode.toString()) {
                MapWorkMode.ACTION_ADD.toString() -> {
                    val addElement = that().addElement.unsafeCast<XyElementData>()
                    addElement.doAddElement(that(), xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, viewCoord)
                    that().addElement = null
                }
                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    val editElement = that().editElement.unsafeCast<XyElementData>()
                    editElement.doEditElementPoint(that(), xyResponse.documentConfig.name, xyResponse.startParamId, scaleKoef, viewCoord)
                }
            }
            //that().xyRefreshView( null, null, true ) - делается внути методов doAdd/doEdit/doMove по завершении операций
            that().setMode(MapWorkMode.SELECT_FOR_ACTION)
        },
        "actionCancel" to {
            val curMode = that().curMode.unsafeCast<MapWorkMode>()

            when (curMode.toString()) {
                MapWorkMode.DISTANCER.toString() -> {
                    //--- включить кнопки, но кнопку линейки выключить обратно
                    enableToolbar(that())
                    that().isDistancerButtonDisabled = true
                    that().arrDistancerLine = arrayOf<XyElementData>()
                    that().arrDistancerDist = arrayOf<Double>()
                    that().arrDistancerText = arrayOf<XyElementData>()
                    that().distancerSumText = null
                    that().isActionCancelButtonVisible = false
                }
                MapWorkMode.ACTION_ADD.toString() -> {
                    that().addElement = null
                    that().xyRefreshView(null, null, true)
                    that().setMode(MapWorkMode.SELECT_FOR_ACTION)
                }
                MapWorkMode.ACTION_EDIT_POINT.toString() -> {
                    that().editElement = null
                    that().xyRefreshView(null, null, true)
                    that().setMode(MapWorkMode.SELECT_FOR_ACTION)
                }
            }
            null
        }
    )


}

private fun editOnePoint(that: dynamic) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    val editElement = that.editElement.unsafeCast<XyElementData>()
    val editPointIndex = that.editPointIndex.unsafeCast<Int>()

    //--- с крайними точками незамкнутой полилинии нечего доделывать
    //if( editElement.type != XyElementDataType.POLYGON && ( editPointIndex == 0 || editPointIndex == editElement.alPoint!!.lastIndex ) ) return

    //--- берем передвигаемую, предыдущую и последующую точки
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

private fun doMoveElements(that: dynamic, documentTypeName: String, startParamId: String, scaleKoef: Double, viewCoord: XyViewCoord) {
    val arrMoveElement = that.arrMoveElement.unsafeCast<Array<XyElementData>>()
    val moveStartPoint = that.moveStartPoint.unsafeCast<XyPoint>()
    val moveEndPoint = that.moveEndPoint.unsafeCast<XyPoint>()

    val xyActionRequest = XyActionRequest(
        documentTypeName = documentTypeName,
        action = XyAction.MOVE_ELEMENTS,
        startParamId = startParamId,

        alActionElementIds = arrMoveElement.map { it.elementId!! },
        dx = ((moveEndPoint.x - moveStartPoint.x) * viewCoord.scale / scaleKoef).roundToInt(),
        dy = ((moveEndPoint.y - moveStartPoint.y) * viewCoord.scale / scaleKoef).roundToInt()
    )

    that.`$root`.setWait(true)
    invokeXy(
        xyActionRequest,
        {
            that.`$root`.setWait(false)
            that.xyRefreshView(that, null, true)
        }
    )
} */
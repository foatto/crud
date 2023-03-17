package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.graphic.GraphicAction
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicElement
import foatto.core.app.graphic.GraphicViewCoord
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.GraphicResponse
import foatto.core.link.SaveUserPropertyRequest
import foatto.core.util.getSplittedDouble
import foatto.core_compose_web.*
import foatto.core_compose_web.control.composable.getRefreshSubToolbar
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose.model.MouseRectData
import foatto.core_compose.model.TitleData
import foatto.core_compose_web.link.invokeGraphic
import foatto.core_compose_web.link.invokeSaveUserProperty
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto
import org.jetbrains.compose.web.css.properties.userSelect
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.*
import org.jetbrains.compose.web.svg.*
import kotlin.js.Date
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private val COLOR_GRAPHIC_TIME_LINE = hsl(180, 100, 50)
private val COLOR_GRAPHIC_LABEL_BACK = hsl(60, 100, 50)
private val COLOR_GRAPHIC_LABEL_BORDER = hsl(60, 100, 25)
private val COLOR_GRAPHIC_AXIS_DEFAULT = hsl(0, 0, 50)
private val COLOR_GRAPHIC_DATA_BACK = hsla(60, 100, 50, 0.7)
private val COLOR_GRAPHIC_LINE_WIDTH = max(1.0, scaleKoef).roundToInt()

private val styleGraphicVisibilityTop = 10.5.cssRem
private val styleGraphicDataTop = 10.8.cssRem
private val arrStyleGraphicTimeLabelPadding: Array<CSSSize> = arrayOf(CONTROL_PADDING, CONTROL_LEFT_RIGHT_SIDE_PADDING, CONTROL_PADDING, CONTROL_LEFT_RIGHT_SIDE_PADDING)

private fun StyleScope.setGraphicVisibilityMaxWidth() {
    if (styleIsNarrowScreen) {
        maxWidth(85.percent)
    } else {
        maxWidth(20.cssRem)
    }
}

private fun StyleScope.setGraphicDataMaxWidth() {
    if (styleIsNarrowScreen) {
        maxWidth(85.percent)
    } else {
        maxWidth(30.cssRem)
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val MARGIN_LEFT = 100     // на каждую ось Y

//private const val MARGIN_RIGHT = 40 - вычисляется динамически по размеру шрифта
private const val MARGIN_TOP = 40
private const val MARGIN_BOTTOM = 60

const val GRAPHIC_MIN_HEIGHT: Int = 300

private const val MIN_GRID_STEP_X = 40  // минимальный шаг между линиями сетки в пикселях
private const val MIN_GRID_STEP_Y = 40  // минимальный шаг между линиями сетки в пикселях

private const val MIN_SCALE_X = 15 * 60     // минимальный разрешённый масштаб - диапазон не менее 15 мин
private const val MAX_SCALE_X = 32 * 86400  // максимальный разрешённый масштаб - диапазон не более 32 дней (чуть более месяца)

private const val GRAPHIC_TEXT_HEIGHT = 20              // высота текстового блока
private const val GRAPHIC_TEXT_MIN_VISIBLE_WIDTH = 4    // минимальная ширина видимого текстового блока

private const val LEGEND_TEXT_MARGIN = 4        // поля вокруг текста легенды справа графика

private val arrGridStepX = arrayOf(
    1, 5, 15,                           // 1 - 5 - 15 seconds
    1 * 60, 5 * 60, 15 * 60,            // 1 - 5 - 15 minutes
    1 * 3_600, 3 * 3_600, 6 * 3_600,    // 1 - 3 - 6 hours
    1 * 86_400, 3 * 86_400, 9 * 86_400, // 1 - 3 - 9 days
    27 * 86_400, 81 * 86_400            // 27 - 81 days
)

private val arrGridStepY = arrayOf(
    0.001, 0.002, 0.0025, 0.005,
    0.01, 0.02, 0.025, 0.05,
    0.1, 0.2, 0.25, 0.5,
    1.0, 2.0, 2.5, 5.0,
    10.0, 20.0, 25.0, 50.0,
    100.0, 200.0, 250.0, 500.0,
    1_000.0, 2_000.0, 2_500.0, 5_000.0,
    10_000.0, 20_000.0, 25_000.0, 50_000.0,
    100_000.0, 200_000.0, 250_000.0, 500_000.0,
    1_000_000.0, 2_000_000.0, 2_500_000.0, 5_000_000.0,
    10_000_000.0, 20_000_000.0, 25_000_000.0, 50_000_000.0,
)

private val arrPrecY = arrayOf(
    3, 3, 4, 3,
    2, 2, 3, 2,
    1, 1, 2, 1,
    0, 0, 1, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
    0, 0, 0, 0,
)

private enum class GraphicWorkMode {
    PAN, ZOOM_BOX/*, SELECT_FOR_PRINT*/
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class GraphicControl(
    private val root: Root,
    private val appControl: AppControl,
    private val graphicResponse: GraphicResponse,
    tabId: Int,
) : AbstractControl(tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    var containerPrefix: String = "graphic"
    var presetSvgHeight: Int? = null
    var arrAddHeights: Array<Int> = emptyArray()

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val isPanButtonEnabled = mutableStateOf(false)
    private val isZoomButtonEnabled = mutableStateOf(true)

    private val isShowGraphicVisible = mutableStateOf(false)
    private val isShowGraphicDataVisible = mutableStateOf(false)

    private val alGraphicVisibleData = mutableStateListOf<GraphicVisibleData>()
    private val alGraphicDataData = mutableStateListOf<String>()

    private val mouseRect = MouseRectData()     // contains state-fields

    private val alTimeLabel = mutableStateListOf(TimeLabelData(), TimeLabelData(), TimeLabelData())

    private val alGrLegend = mutableStateListOf<LegendData>()
    private val alElement = mutableStateListOf<Pair<String, GraphicElement>>()

    private val grSvgAxisWidth = mutableStateOf(0)
    private val grSvgBodyWidth = mutableStateOf(0)
    private val grSvgLegendWidth = mutableStateOf(0)
    private val grSvgHeight = mutableStateOf(0)

    private val grViewBoxAxis = mutableStateOf("0 0 1 1")
    private val grViewBoxBody = mutableStateOf("0 0 1 1")
    private val grViewBoxLegend = mutableStateOf("0 0 1 1")

    private val alGraphicElement = mutableStateListOf<GraphicElementData>()

    private val grTooltipVisible = mutableStateOf(false)
    private val grTooltipText = mutableStateOf("")
    private val grTooltipLeft = mutableStateOf(0.px)
    private val grTooltipTop = mutableStateOf(0.px)

    private val grTimeLine = LineData(
        isVisible = mutableStateOf(false),
        x1 = mutableStateOf(0),
        y1 = mutableStateOf(0),
        x2 = mutableStateOf(0),
        y2 = mutableStateOf(0),
        width = mutableStateOf(1),
    )

    //--- использовать для отображения кнопок переключения режимов
    private val grCurMode = mutableStateOf(GraphicWorkMode.PAN)

    private val refreshInterval = mutableStateOf(0)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var grViewCoord = GraphicViewCoord(0, 0)
    private val alYData = mutableListOf<YData>()
    private var pixStartY = 0
    private var isMouseDown = false
    private var panPointOldX = 0
    private var panPointOldY = 0
    private var panDX = 0
    private var grTooltipOffTime = 0.0
    private var refreshHandlerId = 0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- Graphic Header
            getGraphicAndXyHeader(containerPrefix)

            //--- Graphic Toolbar
            getGraphicAndXyToolbar(containerPrefix) {
                getToolBarSpan {
                    getToolBarIconButton(
                        isEnabled = refreshInterval.value == 0 && isPanButtonEnabled.value,
                        src = "/web/images/ic_open_with_black_48dp.png",
                        title = "Перемещение по графику"
                    ) {
                        setMode(GraphicWorkMode.PAN)
                    }
                    getToolBarIconButton(
                        isEnabled = refreshInterval.value == 0 && isZoomButtonEnabled.value,
                        src = "/web/images/ic_search_black_48dp.png",
                        title = "Выбор области для показа"
                    ) {
                        setMode(GraphicWorkMode.ZOOM_BOX)
                    }
                }
                getToolBarSpan {
                    getToolBarIconButton(refreshInterval.value == 0, src = "/web/images/ic_zoom_in_black_48dp.png", title = "Ближе") { zoomIn() }
                    getToolBarIconButton(refreshInterval.value == 0, src = "/web/images/ic_zoom_out_black_48dp.png", title = "Дальше") { zoomOut() }
                }
                getToolBarSpan {
                    getToolBarIconButton(
                        isEnabled = refreshInterval.value == 0,
                        src = "/web/images/ic_timeline_black_48dp.png",
                        title = "Включить/выключить отдельные графики"
                    ) {
                        isShowGraphicVisible.value = !isShowGraphicVisible.value
                    }
                    if (refreshInterval.value == 0 && isShowGraphicVisible.value) {
                        Div(
                            attrs = {
                                style {
                                    zIndex(Z_INDEX_GRAPHIC_VISIBILITY_LIST)     // popup menu must be above than table headers
                                    position(Position.Absolute)
                                    top(styleGraphicVisibilityTop)
                                    width(auto)
                                    setGraphicVisibilityMaxWidth()
                                    backgroundColor(getColorPopupMenuBack())
                                    setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                                    fontSize(arrStyleMenuFontSize[0])
                                    setPaddings(arrStyleMenuStartPadding)
                                    overflow("auto")
                                    cursor("pointer")
                                }
                            }
                        ) {
                            for (data in alGraphicVisibleData) {
                                Input(InputType.Checkbox) {
                                    style {
                                        setPaddings(getStyleMenuItemPadding(0))
                                    }
                                    checked(data.check.value)
                                    onChange { syntheticChangeEvent ->
                                        data.check.value = syntheticChangeEvent.value
                                    }
                                }
                                Text(data.descr)
                                Br()
                            }
                            Br()
                            Button(
                                attrs = {
                                    style {
                                        backgroundColor(getColorButtonBack())
                                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                        fontSize(styleCommonButtonFontSize)
                                        padding(styleFileNameButtonPadding)
                                        margin(styleFileNameButtonMargin)
                                        cursor("pointer")
                                    }
                                    title("Применить изменения")
                                    onClick {
                                        doChangeGraphicVisibility()
                                    }
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
                getToolBarSpan {
                    for (legend in alGrLegend) {
                        Button(
                            attrs = {
                                style {
                                    legend.style(this)
                                }
                                title(legend.text)
                            }
                        ) {
                            Text(legend.text)
                        }
                    }
                }
                getToolBarSpan {
                    getToolBarIconButton(
                        refreshInterval.value == 0,
                        src = "/web/images/ic_menu_black_48dp.png",
                        title = "Включить/выключить отдельный показ данных"
                    ) {
                        isShowGraphicDataVisible.value = !isShowGraphicDataVisible.value
                    }
                    if (refreshInterval.value == 0 && isShowGraphicDataVisible.value) {
                        Div(
                            attrs = {
                                style {
                                    zIndex(Z_INDEX_GRAPHIC_DATA_LIST)   // popup menu must be above than table headers
                                    position(Position.Absolute)
                                    top(styleGraphicDataTop)
                                    right(0.px)
                                    width(auto)
                                    setGraphicDataMaxWidth()
                                    backgroundColor(COLOR_GRAPHIC_DATA_BACK)
                                    setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                                    fontSize(arrStyleMenuFontSize[0])
                                    setPaddings(arrStyleMenuStartPadding)
                                    overflow("auto")
                                    cursor("pointer")
                                }
                            }
                        ) {
                            for (data in alGraphicDataData) {
                                Text(data)
                                Br()
                            }
                        }
                    }
                }
                getRefreshSubToolbar(refreshInterval) { interval ->
                    setInterval(interval)
                }
            }
            getGraphicElementTemplate(true)
        }
    }

    //--- предположительно, static метод
    @OptIn(ExperimentalComposeWebSvgApi::class)
    @Composable
    fun getGraphicElementTemplate(withInteractive: Boolean) {
        Div(
            attrs = {
                style {
                    display(DisplayStyle.Flex)
                }
                if (refreshInterval.value == 0 && withInteractive) {
                    onWheel { syntheticWheelEvent ->
                        onGrMouseWheel(syntheticWheelEvent)
                        syntheticWheelEvent.preventDefault()
                    }
                }
            }
        ) {

            //--- Y-Axis
            Svg(
                viewBox = grViewBoxAxis.value,
                attrs = {
                    style {
                        flexShrink(0)
                    }
                    width(grSvgAxisWidth.value)
                    height(grSvgHeight.value)
                }
            ) {
                for (element in alGraphicElement) {
                    for (axisLine in element.alAxisYLine) {
                        Line(
                            x1 = axisLine.x1,
                            y1 = axisLine.y1,
                            x2 = axisLine.x2,
                            y2 = axisLine.y2,
                            attrs = {
                                stroke(axisLine.stroke)
                                strokeWidth(axisLine.width)
                                strokeDasharray(axisLine.dash)
                            }
                        )
                    }
                    for (axisText in element.alAxisYText) {
                        SvgText(
                            x = axisText.x,
                            y = axisText.y,
                            text = axisText.text,
                            attrs = {
                                style {
                                    fontSize((1.0 * scaleKoef).cssRem)
                                }
                                fill(axisText.stroke.toString())
                                textAnchor(axisText.hAnchor)
                                dominantBaseline(axisText.vAnchor)
                                transform(axisText.transform)
                            }
                        )
                    }
                }
            }

            //--- Graphic/Chart Body
            Svg(
                viewBox = grViewBoxBody.value,
                attrs = {
                    width(grSvgBodyWidth.value)
                    height(grSvgHeight.value)
                    if (refreshInterval.value == 0 && withInteractive) {
                        onMouseDown { syntheticMouseEvent ->
                            onGrMousePressed(false, syntheticMouseEvent.offsetX, syntheticMouseEvent.offsetY)
                            syntheticMouseEvent.preventDefault()
                        }
                        onMouseMove { syntheticMouseEvent ->
                            onGrMouseMove(false, syntheticMouseEvent.offsetX, syntheticMouseEvent.offsetY)
                            syntheticMouseEvent.preventDefault()
                        }
                        onMouseUp { syntheticMouseEvent ->
                            onGrMouseReleased(
                                false,
                                syntheticMouseEvent.offsetX,
                                syntheticMouseEvent.offsetY,
                                syntheticMouseEvent.shiftKey,
                                syntheticMouseEvent.ctrlKey,
                                syntheticMouseEvent.altKey
                            )
                            syntheticMouseEvent.preventDefault()
                        }
                        onWheel { syntheticWheelEvent ->
                            onGrMouseWheel(syntheticWheelEvent)
                            syntheticWheelEvent.preventDefault()
                        }
                        onTouchStart { syntheticTouchEvent ->
                            syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                                onGrMousePressed(true, firstTouch.clientX.toDouble(), firstTouch.clientY.toDouble())
                            }
                            syntheticTouchEvent.preventDefault()
                        }
                        onTouchMove { syntheticTouchEvent ->
                            syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                                onGrMouseMove(true, firstTouch.clientX.toDouble(), firstTouch.clientY.toDouble())
                            }
                            syntheticTouchEvent.preventDefault()
                        }
                        onTouchEnd { syntheticTouchEvent ->
                            syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                                onGrMouseReleased(
                                    isNeedOffsetCompensation = true,
                                    aMouseX = firstTouch.clientX.toDouble(),
                                    aMouseY = firstTouch.clientY.toDouble(),
                                    shiftKey = syntheticTouchEvent.shiftKey,
                                    ctrlKey = syntheticTouchEvent.ctrlKey,
                                    altKey = syntheticTouchEvent.altKey
                                )
                            }
                            syntheticTouchEvent.preventDefault()
                        }
                    }
                }
            ) {
                for (element in alGraphicElement) {
                    SvgText(
                        x = element.title.x,
                        y = element.title.y,
                        text = element.title.text,
                        attrs = {
                            style {
                                fontSize((1.0 * scaleKoef).cssRem)
                            }
                            fill(element.title.stroke.toString())
                            textAnchor(element.title.hAnchor)
                            dominantBaseline(element.title.vAnchor)
                        }
                    )
                    for (graphicBack in element.alGraphicBack) {
                        Rect(
                            x = graphicBack.x,
                            y = graphicBack.y,
                            width = graphicBack.width,
                            height = graphicBack.height,
                            attrs = {
                                fill(graphicBack.fill)
                            }
                        )
                    }
                    for (axisLine in element.alAxisXLine) {
                        Line(
                            x1 = axisLine.x1,
                            y1 = axisLine.y1,
                            x2 = axisLine.x2,
                            y2 = axisLine.y2,
                            attrs = {
                                stroke(axisLine.stroke)
                                strokeWidth(axisLine.width)
                                strokeDasharray(axisLine.dash)
                            }
                        )
                    }
                    for (axisText in element.alAxisXText) {
                        SvgText(
                            x = axisText.x,
                            y = axisText.y,
                            text = axisText.text,
                            attrs = {
                                style {
                                    fontSize((1.0 * scaleKoef).cssRem)
                                }
                                fill(axisText.stroke.toString())
                                textAnchor(axisText.hAnchor)
                                dominantBaseline(axisText.vAnchor)
                            }
                        )
                    }
                    for (graphicPoint in element.alGraphicPoint) {
                        Circle(
                            cx = graphicPoint.cx,
                            cy = graphicPoint.cy,
                            r = graphicPoint.radius,
                            attrs = {
                                fill(graphicPoint.fill)
                                onMouseEnter { syntheticMouseEvent ->
                                    onGrMouseOver(syntheticMouseEvent, graphicPoint)
                                }
                                onMouseLeave {
                                    onGrMouseOut()
                                }
                            }
                        )
                    }
                    for (graphicLine in element.alGraphicLine) {
                        Line(
                            x1 = graphicLine.x1,
                            y1 = graphicLine.y1,
                            x2 = graphicLine.x2,
                            y2 = graphicLine.y2,
                            attrs = {
                                stroke(graphicLine.stroke)
                                strokeWidth(graphicLine.width)
                                strokeDasharray(graphicLine.dash)
                                onMouseEnter { syntheticMouseEvent ->
                                    onGrMouseOver(syntheticMouseEvent, graphicLine)
                                }
                                onMouseLeave {
                                    onGrMouseOut()
                                }
                            }
                        )
                    }
                }

                if (grTimeLine.isVisible.value) {
                    Line(
                        x1 = grTimeLine.x1.value,
                        y1 = grTimeLine.y1.value,
                        x2 = grTimeLine.x2.value,
                        y2 = grTimeLine.y2.value,
                        attrs = {
                            stroke(COLOR_GRAPHIC_TIME_LINE)
                            strokeWidth(grTimeLine.width.value)
                        }
                    )
                }

                if (refreshInterval.value == 0 && withInteractive && mouseRect.isVisible.value) {
                    Rect(
                        x = min(mouseRect.x1.value, mouseRect.x2.value),
                        y = min(mouseRect.y1.value, mouseRect.y2.value),
                        width = abs(mouseRect.x2.value - mouseRect.x1.value),
                        height = abs(mouseRect.y2.value - mouseRect.y1.value),
                        attrs = {
                            fill(COLOR_GRAPHIC_TIME_LINE.toString())
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
                            stroke(COLOR_GRAPHIC_TIME_LINE)
                            strokeWidth(COLOR_GRAPHIC_LINE_WIDTH)
                        }
                    )
                    Line(
                        x1 = mouseRect.x2.value,
                        y1 = mouseRect.y1.value,
                        x2 = mouseRect.x2.value,
                        y2 = mouseRect.y2.value,
                        attrs = {
                            stroke(COLOR_GRAPHIC_TIME_LINE)
                            strokeWidth(COLOR_GRAPHIC_LINE_WIDTH)
                        }
                    )
                    Line(
                        x1 = mouseRect.x2.value,
                        y1 = mouseRect.y2.value,
                        x2 = mouseRect.x1.value,
                        y2 = mouseRect.y2.value,
                        attrs = {
                            stroke(COLOR_GRAPHIC_TIME_LINE)
                            strokeWidth(COLOR_GRAPHIC_LINE_WIDTH)
                        }
                    )
                    Line(
                        x1 = mouseRect.x1.value,
                        y1 = mouseRect.y2.value,
                        x2 = mouseRect.x1.value,
                        y2 = mouseRect.y1.value,
                        attrs = {
                            stroke(COLOR_GRAPHIC_TIME_LINE)
                            strokeWidth(COLOR_GRAPHIC_LINE_WIDTH)
                        }
                    )
                }
            }

            //--- Legends
            Svg(
                viewBox = grViewBoxLegend.value,
                attrs = {
                    style {
                        flexShrink(0)
                    }
                    width(grSvgLegendWidth.value)
                    height(grSvgHeight.value)
                }
            ) {
                for (element in alGraphicElement) {
                    for (legendBack in element.alLegendBack) {
                        Rect(
                            x = legendBack.x,
                            y = legendBack.y,
                            width = legendBack.width,
                            height = legendBack.height,
                            attrs = {
                                stroke(legendBack.stroke)
                                fill(legendBack.fill)
                                rx(legendBack.rx)
                                ry(legendBack.ry)
                            }
                        )
                    }
                    for (legendText in element.alLegendText) {
                        SvgText(
                            x = legendText.x,
                            y = legendText.y,
                            text = legendText.text,
                            attrs = {
                                style {
                                    fontSize((1.0 * scaleKoef).cssRem)
                                }
                                fill(legendText.stroke.toString())
                                textAnchor(legendText.hAnchor)
                                dominantBaseline(legendText.vAnchor)
                                transform(legendText.transform)
                            }
                        )
                    }
                }
            }

            //--- Graphic/Chart Texts
            for (element in alGraphicElement) {
                for (graphicText in element.alGraphicText) {
                    if (graphicText.isVisible) {
                        Div(
                            attrs = {
                                style {
                                    graphicText.pos(this)
                                    graphicText.style(this)
                                }
                                onMouseEnter { syntheticMouseEvent ->
                                    onGrMouseOver(syntheticMouseEvent, graphicText)
                                }
                                onMouseLeave {
                                    onGrMouseOut()
                                }
                            }
                        ) {
                            Text(graphicText.text)
                        }
                    }
                }
            }

            if (refreshInterval.value == 0 && withInteractive) {
                //--- Time Labels
                for (element in alTimeLabel) {
                    if (element.isVisible.value) {
                        Div(
                            attrs = {
                                style {
                                    position(Position.Absolute)
                                    textAlign("center")
                                    color(COLOR_MAIN_TEXT)
                                    backgroundColor(COLOR_GRAPHIC_LABEL_BACK)
                                    setBorder(color = COLOR_GRAPHIC_LABEL_BORDER, radius = styleButtonBorderRadius)
                                    setPaddings(arrStyleGraphicTimeLabelPadding)
                                    userSelect("none")
                                    element.pos.value(this)
                                }
                            }
                        ) {
                            Text(element.text.value)
                        }
                    }
                }

                //--- Tooltip
                if (grTooltipVisible.value) {
                    Div(
                        attrs = {
                            style {
                                position(Position.Absolute)
                                color(COLOR_MAIN_TEXT)
                                backgroundColor(COLOR_GRAPHIC_LABEL_BACK)
                                setBorder(color = COLOR_GRAPHIC_LABEL_BORDER, radius = styleButtonBorderRadius)
                                setPaddings(arrStyleControlTooltipPadding)
                                userSelect("none")
                                left(grTooltipLeft.value)
                                top(grTooltipTop.value)
                            }
                        }
                    ) {
                        Text(grTooltipText.value)
                    }
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        root.setTabInfo(tabId, graphicResponse.shortTitle, graphicResponse.fullTitle)
        alTitleData += graphicResponse.fullTitle.split('\n').filter { it.isNotBlank() }.map { TitleData("", it) }

        doGraphicSpecificComponentMounted()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun doGraphicSpecificComponentMounted() {
        val newViewCoord = GraphicViewCoord(
            t1 = if (graphicResponse.rangeType == 0) {
                graphicResponse.begTime
            } else {
                (Date.now() / 1000 - graphicResponse.rangeType).toInt()
            },
            t2 = if (graphicResponse.rangeType == 0) {
                graphicResponse.endTime
            } else {
                (Date.now() / 1000).toInt()
            },
        )
        doGraphicRefresh(
            aView = newViewCoord,
            withWait = true,
        )
    }

    fun setInterval(sec: Int) {
        if (refreshHandlerId != 0) {
            window.clearInterval(refreshHandlerId)
        }

        if (sec == 0) {
            doGraphicRefresh(
                aView = null,
                withWait = true,
            )
        } else {
            refreshHandlerId = window.setInterval({
                doGraphicRefresh(
                    aView = null,
                    withWait = false,
                )
            }, sec * 1000)
        }

        refreshInterval.value = sec
    }

    private fun grRefreshView(aView: GraphicViewCoord?) {
        doGraphicRefresh(
            aView = aView,
            withWait = true,
        )
    }

    private fun doGraphicRefresh(
        aView: GraphicViewCoord?,
        withWait: Boolean,
    ) {
        aView?.let {
            grViewCoord = aView
        } ?: run {
            if (graphicResponse.rangeType != 0 && refreshInterval.value != 0) {
                grViewCoord = GraphicViewCoord(
                    t1 = (Date.now() / 1000 - graphicResponse.rangeType).toInt(),
                    t2 = (Date.now() / 1000).toInt(),
                )
            }
        }

        if (withWait) {
            root.setWait(true)
        }
        invokeGraphic(
            GraphicActionRequest(
                documentTypeName = graphicResponse.documentTypeName,
                action = GraphicAction.GET_ELEMENTS,
                startParamId = graphicResponse.startParamId,
                graphicCoords = Pair(grViewCoord.t1, grViewCoord.t2),
                //--- передавая полную ширину окна (без учёта margin по бокам/сверху/снизу для отрисовки шкалы/полей/заголовков),
                //--- мы делаем сглаживание/масштабирование чуть точнее, чем надо, но на момент запроса величины margin неизвестны :/,
                //--- а от "лишней" точности хуже не будет
                viewSize = Pair((window.innerWidth / scaleKoef).roundToInt(), (window.innerHeight / scaleKoef).roundToInt())
            )

        ) { graphicActionResponse: GraphicActionResponse ->

            //--- calcBodyLeftAndTop нельз вызывать слишком рано (в doGraphicSpecificComponentMounted),
            //--- т.к. асинхронный Body ещё не готов и возвращает null для элементов заголовка
            val svgBodyTop = calcBodyLeftAndTop().second
            grSvgHeight.value = presetSvgHeight ?: run {
                window.innerHeight - svgBodyTop
            }

            alElement.clear()
            alElement.addAll(graphicActionResponse.arrElement)

            //--- пары element-descr -> element-key, отсортированные по element-descr для определения ключа,
            //--- по которому будет управляться видимость графиков
            alGraphicVisibleData.clear()
            alGraphicVisibleData.addAll(
                graphicActionResponse.arrVisibleElement.map { (first, second, third) ->
                    GraphicVisibleData(
                        descr = first,
                        name = second,
                        check = mutableStateOf(third),
                    )
                }
            )

            val hmIndexColor = graphicActionResponse.arrIndexColor.associate { (colorIndex, intColor) ->
                colorIndex.toString() to getColorFromInt(intColor)
            }
            alGrLegend.clear()
            alGrLegend.addAll(graphicActionResponse.arrLegend.map { (color, isBack, text) ->
                LegendData(
                    text = text,
                    style = {
                        backgroundColor(
                            if (isBack) {
                                getColorFromInt(color)
                            } else {
                                getColorButtonBack()
                            }
                        )
                        color(
                            if (isBack) {
                                COLOR_MAIN_TEXT
                            } else {
                                getColorFromInt(color)
                            }
                        )
                        fontSize(styleCommonButtonFontSize)
                        padding(styleTextButtonPadding)
                        setMargins(arrStyleCommonMargin)
                        //"cursor" to "none",
                        setBorder(color = getColorFromInt(color), radius = 0.2.cssRem)
                    }
                )
            })

            var maxMarginLeft = 0
            var maxMarginRight = 0

            //--- определить hard/soft-высоты графиков (для распределения области окна между графиками)
            var sumHard = 0        // сумма жестко заданных высот
            var sumSoft = 0        // сумма мягко/относительно заданных высот
            alElement.forEach { pair ->
                val cge = pair.second
                //--- prerare data for Y-reversed charts
                cge.alAxisYData.forEach { axisYData ->
                    if (axisYData.isReversedY) {
                        //--- во избежание перекрёстных изменений
                        val minY = axisYData.min
                        val maxY = axisYData.max
                        axisYData.min = -maxY
                        axisYData.max = -minY
                    }
                }
                cge.alGDC.forEach { gdc ->
                    if (gdc.isReversedY) {
                        when (gdc.type) {
                            GraphicDataContainer.ElementType.LINE -> {
                                gdc.alGLD.forEach { gld ->
                                    gld.y = -gld.y
                                }
                            }
                            else -> {}
                        }
                    }
                }

                //--- переинициализировать значение левого поля
                maxMarginLeft = max(maxMarginLeft, (cge.alAxisYData.size * MARGIN_LEFT * scaleKoef).roundToInt())
                maxMarginRight = max(maxMarginRight, cge.alLegend.size * getLegendWidth(scaleKoef))

                val grHeight = cge.graphicHeight.toInt()
                if (grHeight > 0) {
                    //--- "положительная" высота - жестко заданная
                    sumHard += (grHeight * scaleKoef).roundToInt()
                } else {
                    //--- "отрицательная" высота - относительная (в долях от экрана)
                    sumSoft += -grHeight
                }
            }

            //--- установка динамической (зависящей от scaleKoef) ширины области с вертикальными осями
            grSvgAxisWidth.value = maxMarginLeft
            grSvgLegendWidth.value = maxMarginRight

            //--- сбрасываем горизонтальный скроллинг/смещение и устанавливаем размеры SVG-компонент
            val arrViewBoxAxis = getGraphicViewBoxAxis()
            setGraphicViewBoxAxis(arrayOf(0, arrViewBoxAxis[1], grSvgAxisWidth.value, grSvgHeight.value))

            val menuBarElement = document.getElementById(MENU_BAR_ID)
            val menuBarWidth = if (root.isShowMainMenu.value) {
                menuBarElement?.clientWidth ?: 0
            } else {
                0
            }
            val menuCloserElement = document.getElementById(MENU_CLOSER_BUTTON_ID)
            val menuCloserWidth = menuCloserElement?.clientWidth ?: 0
            grSvgBodyWidth.value = window.innerWidth - menuBarWidth - menuCloserWidth - (maxMarginLeft + maxMarginRight)

            val arrViewBoxBody = getGraphicViewBoxBody()
            setGraphicViewBoxBody(arrayOf(0, arrViewBoxBody[1], grSvgBodyWidth.value, grSvgHeight.value))

            val arrViewBoxLegend = getGraphicViewBoxBody()
            setGraphicViewBoxLegend(arrayOf(0, arrViewBoxLegend[1], grSvgLegendWidth.value, grSvgHeight.value))

            //--- реальная высота одной единицы относительной высоты
            val oneSoftHeight = if (sumSoft == 0) {
                0
            } else {
                (grSvgHeight.value - sumHard) / sumSoft
            }

            var localPixStartY = 0
            alGraphicElement.clear()
            alYData.clear()

            alElement.forEach { pair ->
                val element = pair.second

                val grHeight = element.graphicHeight.toInt()
                val pixRealHeight = if (grHeight > 0) {
                    (grHeight * scaleKoef).roundToInt()
                } else {
                    max((GRAPHIC_MIN_HEIGHT * scaleKoef).roundToInt(), -grHeight * oneSoftHeight)
                }
                outElement(
                    hmIndexColor = hmIndexColor,
                    t1 = grViewCoord.t1,
                    t2 = grViewCoord.t2,
                    element = element,
                    pixRealHeight = pixRealHeight,
                    pixStartY = localPixStartY,
                )
                localPixStartY += pixRealHeight
            }

//        //--- перезагрузка данных может быть связана с изменением показываемого временнОго диапазона,
//        //--- поэтому переотобразим его
//        val arrBegDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t1 )
//        arrTxtDateTime[ 2 ]!!.text = arrBegDT[ 0 ].toString()
//        arrTxtDateTime[ 1 ]!!.text = arrBegDT[ 1 ].toString()
//        arrTxtDateTime[ 0 ]!!.text = arrBegDT[ 2 ].toString()
//        arrTxtDateTime[ 3 ]!!.text = arrBegDT[ 3 ].toString()
//        arrTxtDateTime[ 4 ]!!.text = arrBegDT[ 4 ].toString()
//        val arrEndDT = DateTime_Arr( appContainer.timeZone, grModel.viewCoord.t2 )
//        arrTxtDateTime[ 7 ]!!.text = arrEndDT[ 0 ].toString()
//        arrTxtDateTime[ 6 ]!!.text = arrEndDT[ 1 ].toString()
//        arrTxtDateTime[ 5 ]!!.text = arrEndDT[ 2 ].toString()
//        arrTxtDateTime[ 8 ]!!.text = arrEndDT[ 3 ].toString()
//        arrTxtDateTime[ 9 ]!!.text = arrEndDT[ 4 ].toString()
//
//        onRequestFocus()

            pixStartY = localPixStartY

            //--- правильное значение svgBodyLeft известно только в конце загрузки
            val svgBodyLeft = calcBodyLeftAndTop().first
            setGraphicTextOffset(svgBodyLeft, svgBodyTop)

            if (withWait) {
                root.setWait(false)
            }
        }
    }

    private fun calcBodyLeftAndTop(): Pair<Int, Int> {
        val menuBarElement = document.getElementById(MENU_BAR_ID)
        val menuCloserElement = document.getElementById(MENU_CLOSER_BUTTON_ID)

        val topBar = document.getElementById(TOP_BAR_ID)
        val svgTabPanel = document.getElementById("tab_panel")!!
        val svgGraphicTitle = document.getElementById("${containerPrefix}_title_$tabId")!!
        val svgGraphicToolbar = document.getElementById("${containerPrefix}_toolbar_$tabId")!!

        val menuBarWidth = if (root.isShowMainMenu.value) {
            menuBarElement?.clientWidth ?: 0
        } else {
            0
        }

        return Pair(
            //svgBodyElement.clientLeft - BUG: всегда даёт 0
            menuBarWidth +
                (menuCloserElement?.clientWidth ?: 0) +
                grSvgAxisWidth.value,

            (topBar?.clientHeight ?: 0) +
                svgTabPanel.clientHeight +
                svgGraphicTitle.clientHeight +
                svgGraphicToolbar.clientHeight +
                arrAddHeights.sum()
        )
    }

    private fun getLegendWidth(scaleKoef: Double) = (iCoreAppContainer.BASE_FONT_SIZE * scaleKoef + 2 * LEGEND_TEXT_MARGIN * scaleKoef).toInt()

    private fun getGraphicViewBoxAxis(): Array<Int> = grViewBoxAxis.value.split(' ').map { it.toInt() }.toTypedArray()

    private fun setGraphicViewBoxAxis(arrViewBox: Array<Int>) {
        grViewBoxAxis.value = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
    }

    private fun getGraphicViewBoxBody(): Array<Int> = grViewBoxBody.value.split(' ').map { it.toInt() }.toTypedArray()

    private fun setGraphicViewBoxBody(arrViewBox: Array<Int>) {
        grViewBoxBody.value = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
    }

    private fun getGraphicViewBoxLegend(): Array<Int> = grViewBoxLegend.value.split(' ').map { it.toInt() }.toTypedArray()

    private fun setGraphicViewBoxLegend(arrViewBox: Array<Int>) {
        grViewBoxLegend.value = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
    }

    private fun setGraphicTextOffset(svgBodyLeft: Int, svgBodyTop: Int) {
        val arrViewBoxBody = getGraphicViewBoxBody()

        for (grElement in alGraphicElement) {
            for (grTextData in grElement.alGraphicText) {
                val newX = grTextData.x - arrViewBoxBody[0]
                val newY = grTextData.y - arrViewBoxBody[1]

                grTextData.isVisible = newX >= 0 && newY >= 0 && newX < arrViewBoxBody[2] && newY < arrViewBoxBody[3]

                grTextData.pos = {
                    left((svgBodyLeft + newX).px)
                    top((svgBodyTop + newY).px)
                }
            }
        }
    }

    private fun outElement(
        hmIndexColor: Map<String, CSSColorValue>,
        t1: Int,
        t2: Int,
        element: GraphicElement,
        pixRealHeight: Int,
        pixStartY: Int,
    ) {
        //--- maxMarginLeft уходит на левую панель, к оси Y
        val pixDrawHeight = pixRealHeight - ((MARGIN_TOP + MARGIN_BOTTOM) * scaleKoef).roundToInt()
        val pixDrawY0 = pixStartY + pixRealHeight - (MARGIN_BOTTOM * scaleKoef).roundToInt()   // "нулевая" ось Y
        val pixDrawTopY = pixStartY + (MARGIN_TOP * scaleKoef).roundToInt()  // верхний край графика

        val alAxisYLine = mutableListOf<SvgLineData>()
        val alAxisYText = mutableListOf<SvgTextData>()
        val alAxisXLine = mutableListOf<SvgLineData>()
        val alAxisXText = mutableListOf<SvgTextData>()
        val alGraphicBack = mutableListOf<SvgRectData>()
        val alGraphicLine = mutableListOf<SvgLineData>()
        val alGraphicPoint = mutableListOf<SvgCircleData>()
        val alGraphicText = mutableListOf<GraphicTextData>()
        val alLegendBack = mutableListOf<SvgRectData>()
        val alLegendText = mutableListOf<SvgTextData>()

        //--- заголовок

        val titleData = SvgTextData(
            x = (MIN_GRID_STEP_X * scaleKoef).roundToInt(),
            y = (pixDrawTopY - 4 * scaleKoef).roundToInt(),
            text = element.graphicTitle,
            stroke = COLOR_MAIN_TEXT,
            hAnchor = "start",
            vAnchor = "text-bottom"
        )

        //--- ось X ---

        drawTimePane(
            t1 = t1,
            t2 = t2,
            pixWidth = grSvgBodyWidth.value,
            pixDrawY0 = pixDrawY0,
            pixDrawTopY = pixDrawTopY,
            alAxisLine = alAxisXLine,
            alAxisText = alAxisXText
        )

        //--- оси Y

        val alAxisYDataIndex = mutableListOf<Int>()
        for (i in element.alAxisYData.indices) {
            val ayd = element.alAxisYData[i]

            val precY = drawAxisY(
                scaleKoef = scaleKoef,
                hmIndexColor = hmIndexColor,
                element = element,
                axisIndex = i,
                pixDrawWidth = grSvgAxisWidth.value,
                pixDrawHeight = pixDrawHeight,
                pixDrawY0 = pixDrawY0,
                pixDrawTopY = pixDrawTopY,
                pixBodyWidth = grSvgBodyWidth.value,
                alAxisLine = alAxisYLine,
                alAxisText = alAxisYText,
                alBodyLine = alAxisXLine    // для горизонтальной линии на самом графике
            )

            ayd.prec = precY

            val axisYDataIndex = alYData.size
            alAxisYDataIndex.add(axisYDataIndex)

            alYData.add(
                YData(
                    y1 = pixDrawY0,
                    y2 = pixDrawTopY,
                    value1 = ayd.min,
                    value2 = ayd.max,
                    prec = precY,
                    isReversedY = ayd.isReversedY,
                )
            )
        }

        //--- легенда ---

        for (i in element.alLegend.indices) {
            drawLegend(
                scaleKoef = scaleKoef,
                element = element,
                legendIndex = i,
                pixDrawHeight = pixDrawHeight,
                pixDrawY0 = pixDrawY0,
                pixDrawTopY = pixDrawTopY,
                alLegendBack = alLegendBack,
                alLegendText = alLegendText,
            )
        }

        //--- графики ---

        val alPrevTextBounds = mutableListOf<XyRect>()

        //--- для преодоления целочисленного переполнения
        val svgBodyWidthDouble = grSvgBodyWidth.value.toDouble()

        for (cagdc in element.alGDC) {
            val axisYIndex = cagdc.axisYIndex

            when (cagdc.type) {
                GraphicDataContainer.ElementType.BACK -> {
                    for (grd in cagdc.alGBD) {
                        val drawX1 = (svgBodyWidthDouble * (grd.x1 - t1) / (t2 - t1)).toInt()
                        val drawX2 = (svgBodyWidthDouble * (grd.x2 - t1) / (t2 - t1)).toInt()

                        alGraphicBack.add(
                            SvgRectData(
                                x = drawX1,
                                y = pixDrawTopY,
                                width = drawX2 - drawX1,
                                height = pixDrawY0 - pixDrawTopY,
                                fill = getColorFromInt(grd.color).toString(),
                            )
                        )
                    }
                }

                GraphicDataContainer.ElementType.LINE -> {
                    var prevDrawX = -1
                    var prevDrawY = -1.0
                    var prevDrawColorIndex: GraphicColorIndex? = null
                    val ayd = element.alAxisYData[axisYIndex]
                    val graphicHeight = ayd.max - ayd.min

                    for (gld in cagdc.alGLD) {
                        val drawX = (svgBodyWidthDouble * (gld.x - t1) / (t2 - t1)).toInt()
                        val drawY = pixDrawY0 - pixDrawHeight * (gld.y - ayd.min) / graphicHeight
                        prevDrawColorIndex?.let {
                            alGraphicLine.add(
                                SvgLineData(
                                    x1 = prevDrawX,
                                    y1 = prevDrawY.toInt(),
                                    x2 = drawX,
                                    y2 = drawY.toInt(),
                                    stroke = hmIndexColor[gld.colorIndex.toString()]!!,
                                    width = (cagdc.lineWidth * scaleKoef).roundToInt(),
                                    tooltip = alAxisYDataIndex[axisYIndex].toString()
                                )
                            )
                        }
                        prevDrawX = drawX
                        prevDrawY = drawY
                        prevDrawColorIndex = gld.colorIndex
                    }
                }

                GraphicDataContainer.ElementType.TEXT -> {
                    for (gtd in cagdc.alGTD) {
                        val drawX1 = grSvgBodyWidth.value * (gtd.textX1 - t1) / (t2 - t1)
                        val drawX2 = grSvgBodyWidth.value * (gtd.textX2 - t1) / (t2 - t1)
                        val drawWidth = drawX2 - drawX1
                        val drawHeight = (GRAPHIC_TEXT_HEIGHT * scaleKoef).roundToInt()

                        //--- смысла нет показывать коротенькие блоки
                        if (drawWidth <= (GRAPHIC_TEXT_MIN_VISIBLE_WIDTH * scaleKoef).roundToInt()) {
                            continue
                        }

                        val rect = XyRect(drawX1, pixDrawTopY, drawWidth, drawHeight)

                        //--- обеспечим отсутствие накладок текстов/прямоугольников
                        //--- ( многопроходной алгоритм, учитывающий "смену обстановки" после очередного сдвига )
                        while (true) {
                            var crossNotFound = true
                            for (otherRect in alPrevTextBounds) {
                                //System.out.println(  "Bounds = " + b  );
                                //--- если блок текста пересекается с кем-то предыдущим, опустимся ниже его
                                if (rect.isIntersects(otherRect)) {
                                    rect.y += rect.height
                                    crossNotFound = false
                                    break
                                }
                            }
                            if (crossNotFound) break
                        }
                        //--- для следующих текстов
                        alPrevTextBounds.add(rect)
                        alGraphicText.add(
                            GraphicTextData(
                                isVisible = false,
                                x = rect.x,
                                y = rect.y,
                                pos = {
                                    left(0.px)
                                    top(0.px)
                                },
                                style = {
                                    position(Position.Absolute)
                                    paddingTop(0.cssRem)                        // иначе прямоугольники текста налезают друг на друга по вертикали
                                    paddingRight(0.cssRem)
                                    paddingBottom(0.cssRem)
                                    paddingLeft(0.cssRem)                       // иначе прямоугольники текста налезают друг на друга по горизонтали
                                    overflow("hidden")
                                    width((rect.width - 2 * scaleKoef).px)      // чтобы избежать некрасивого перекрытия прямоугольников
                                    height((rect.height - 2 * scaleKoef).px)
                                    setBorder(
                                        color = hmIndexColor[gtd.borderColorIndex.toString()] ?: COLOR_MAIN_TEXT,
                                        width = (1 * scaleKoef).px,
                                        radius = (2 * scaleKoef).px,
                                    )
                                    color(hmIndexColor[gtd.textColorIndex.toString()] ?: COLOR_MAIN_TEXT)
                                    backgroundColor(hmIndexColor[gtd.fillColorIndex.toString()] ?: COLOR_MAIN_BACK_0)
                                    fontSize((1.0 * scaleKoef).cssRem)
                                    userSelect("none")
                                },
                                text = gtd.text,
                                tooltip = gtd.text
                            )
                        )
                    }
                }
            }
        }

        alGraphicElement.add(
            GraphicElementData(
                title = titleData,
                alAxisYLine = alAxisYLine,
                alAxisYText = alAxisYText,
                alAxisXLine = alAxisXLine,
                alAxisXText = alAxisXText,
                alGraphicBack = alGraphicBack,
                alGraphicLine = alGraphicLine,
                alGraphicPoint = alGraphicPoint,
                alGraphicText = alGraphicText,
                alLegendBack = alLegendBack,
                alLegendText = alLegendText,
            )
        )
    }

    private fun drawTimePane(
        t1: Int,
        t2: Int,

        pixWidth: Int,

        pixDrawY0: Int,
        pixDrawTopY: Int,

        alAxisLine: MutableList<SvgLineData>,
        alAxisText: MutableList<SvgTextData>
    ) {
        val timeOffset = root.timeOffset

        val timeWidth = t2 - t1

        //--- сетка, насечки, надписи по оси X
        val minStepX: Int = (timeWidth * MIN_GRID_STEP_X * scaleKoef / pixWidth).roundToInt()
        var notchStepX = 0   // шаг насечек
        var labelStepX = 0   // шаг подписей под насечками
        for (i in arrGridStepX.indices) {
            if (arrGridStepX[i] >= minStepX) {
                notchStepX = arrGridStepX[i]
                //--- подписи по шкале X делаются реже, чем насечки
                labelStepX = arrGridStepX[if (i == arrGridStepX.size - 1) i else i + 1]
                break
            }
        }
        //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
        if (notchStepX == 0) {
            notchStepX = arrGridStepX[arrGridStepX.size - 1]
            labelStepX = arrGridStepX[arrGridStepX.size - 1]
        }

        var notchX = (t1 + timeOffset) / notchStepX * notchStepX - timeOffset
        while (notchX <= t2) {

            if (notchX < t1) {
                notchX += notchStepX
                continue
            }
            //--- в double и обратно из-за ошибок округления
            val pixDrawX = (1.0 * pixWidth * (notchX - t1) / timeWidth).roundToInt()
            //--- вертикальная линия сетки, переходящая в насечку
            val line = SvgLineData(
                x1 = pixDrawX,
                y1 = pixDrawTopY,
                x2 = pixDrawX,
                y2 = pixDrawY0 + (2 * scaleKoef).roundToInt(),
                stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                width = max(1, scaleKoef.roundToInt()),
                //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
                    "${scaleKoef * 2},${scaleKoef * 2}"
                } else {
                    ""
                }
            )
            alAxisLine.add(line)

            //--- текст метки по оси X
            if ((notchX + timeOffset) % labelStepX == 0) {
                val alTextLine = DateTime_DMYHMS(timeOffset, notchX).split(' ')
                for (i in alTextLine.indices) {
                    alAxisText.add(
                        SvgTextData(
                            x = pixDrawX,
                            y = (pixDrawY0 + (2 + (if (i == 0) 0.2 else 1.2) * 16) * scaleKoef).toInt(),
                            text = alTextLine[i].trim(),
                            stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                            hAnchor = "middle",
                            vAnchor = "hanging",
                        )
                    )
                }
            }
            notchX += notchStepX
        }
        //--- первую две метки (дата и время) перевыровнять к началу, последние две (дата и время) - к концу
        alAxisText[0].hAnchor = "start"
        alAxisText[1].hAnchor = "start"
        alAxisText[alAxisText.lastIndex - 1].hAnchor = "end"
        alAxisText[alAxisText.lastIndex].hAnchor = "end"

        //--- ось X
        val line = SvgLineData(
            x1 = 0,
            y1 = pixDrawY0,
            x2 = pixWidth,
            y2 = pixDrawY0,
            stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
            width = max(1, scaleKoef.roundToInt()),
            //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
            dash = if (pixDrawTopY < pixDrawY0 && (notchX + timeOffset) % labelStepX != 0) {
                "${scaleKoef * 2},${scaleKoef * 2}"
            } else {
                ""
            }
        )
        alAxisLine.add(line)
    }

    private fun drawAxisY(
        scaleKoef: Double,
        hmIndexColor: Map<String, CSSColorValue>,
        element: GraphicElement,
        axisIndex: Int,
        pixDrawWidth: Int,
        pixDrawHeight: Int,
        pixDrawY0: Int,
        pixDrawTopY: Int,
        pixBodyWidth: Int,      // ширина основного графика
        alAxisLine: MutableList<SvgLineData>,
        alAxisText: MutableList<SvgTextData>,
        alBodyLine: MutableList<SvgLineData>
    ): Int {

        val ayd = element.alAxisYData[axisIndex]
        val grHeight = ayd.max - ayd.min
        val axisX = (pixDrawWidth - axisIndex * MARGIN_LEFT * scaleKoef).roundToInt()

        //--- сетка, насечки, надписи по оси Y
        val minGraphicStepY = grHeight * MIN_GRID_STEP_Y * scaleKoef / pixDrawHeight
        var notchGraphicStepY = 0.0   // шаг насечек
        var labelGraphicStepY = 0.0   // шаг подписей под насечками
        var precY = 0
        for (i in arrGridStepY.indices) {
            val gridStepY = arrGridStepY[i]
            if (gridStepY >= minGraphicStepY) {
                notchGraphicStepY = gridStepY
                //--- подписи по шкале Y делаются на каждой насечке
                labelGraphicStepY = gridStepY  //element.getGridStepX(  i == element.getGridStepXCount() - 1 ? i : i + 1  );
                //--- сразу же определим precY
                precY = arrPrecY[i]
                break
            }
        }
        //--- если подходящий шаг насечек не нашелся, берем максимальный (хотя такой ситуации не должно быть)
        if (notchGraphicStepY <= 0.0) {
            notchGraphicStepY = arrGridStepY.last()
            labelGraphicStepY = arrGridStepY.last()
            //--- сразу же определим precY
            precY = arrPrecY.last()
        }
        //--- для последующего корректного вычисления остатка от деления дробных чисел будем приводить их к целым числам путём умножения на квадрат минимального шага
        val mult = round(1.0 / arrGridStepY[0] / arrGridStepY[0])

        var notchY = floor(ayd.min / notchGraphicStepY) * notchGraphicStepY
        while (notchY <= ayd.max) {

            if (notchY < ayd.min) {
                notchY += notchGraphicStepY
                continue
            }
            val drawY = pixDrawY0 - pixDrawHeight * (notchY - ayd.min) / grHeight

            //--- горизонтальная линия сетки, переходящая в насечку
            val line = SvgLineData(
                x1 = axisX - (MARGIN_LEFT * scaleKoef / 2).roundToInt(),
                y1 = drawY.toInt(),
                x2 = /*if( axisIndex == 0 ) pixDrawWidth else*/ axisX,
                y2 = drawY.toInt(),
                stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                width = max(1, scaleKoef.roundToInt()),
                //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                dash = "${scaleKoef * 2},${scaleKoef * 2}"
            )
            alAxisLine.add(line)

            //--- горизонтальная линия сетки на основном графике
            if (axisIndex == 0) {
                alBodyLine.add(
                    SvgLineData(
                        x1 = 0,
                        y1 = drawY.toInt(),
                        x2 = pixBodyWidth,
                        y2 = drawY.toInt(),
                        stroke = COLOR_GRAPHIC_AXIS_DEFAULT,
                        width = max(1, scaleKoef.roundToInt()),
                        //--- если насечка переходит в линию сетки, то возможно меняется стиль линии
                        dash = "${scaleKoef * 2},${scaleKoef * 2}"
                    )
                )
            }

            //--- текст метки по оси Y
            if (round(notchY * mult).toLong() % round(labelGraphicStepY * mult).toLong() == 0L) {
                val value = if (ayd.isReversedY) {
                    -notchY
                } else {
                    notchY
                }
                val axisText = SvgTextData(
                    x = axisX - (2 * scaleKoef).roundToInt(),
                    y = drawY.toInt() - (2 * scaleKoef).roundToInt(),
                    text = getSplittedDouble(value, precY, true, '.'),
                    stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
                    hAnchor = "end",
                    vAnchor = "text-bottom"
                )
                alAxisText.add(axisText)
            }
            notchY += notchGraphicStepY
        }
        //--- ось Y
        val line = SvgLineData(
            x1 = axisX,
            y1 = pixDrawY0,
            x2 = axisX,
            y2 = pixDrawTopY,
            stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
            width = max(1, scaleKoef.roundToInt())
        )
        alAxisLine.add(line)

        //--- подпись оси Y - подпись отодвинем подальше от цифр, чтобы не перекрывались
        val axisTextX = axisX - (MARGIN_LEFT * scaleKoef * 5 / 6).roundToInt()
        val axisTextY = pixDrawY0 - pixDrawHeight / 2
        val axisText = SvgTextData(
            x = axisTextX,
            y = axisTextY,
            text = ayd.title,
            stroke = hmIndexColor[ayd.colorIndex.toString()]!!,
            hAnchor = "middle",
            vAnchor = "hanging",
            transform = "rotate(-90 $axisTextX $axisTextY)"
        )

        alAxisText.add(axisText)

        return precY
    }

    private fun drawLegend(
        scaleKoef: Double,
        element: GraphicElement,
        legendIndex: Int,
        pixDrawHeight: Int,
        pixDrawY0: Int,
        pixDrawTopY: Int,
        alLegendBack: MutableList<SvgRectData>,
        alLegendText: MutableList<SvgTextData>,
    ) {
        val triple = element.alLegend[legendIndex]
        val color = triple.first
        val isBack = triple.second
        val text = triple.third

        val width = getLegendWidth(scaleKoef)

        val x1 = width * legendIndex
        val y1 = pixDrawTopY
        val x2 = x1 + width
        val y2 = pixDrawY0

        val legendBack = SvgRectData(
            x = x1,
            y = y1,
            width = width,
            height = pixDrawHeight,
            stroke = getColorFromInt(color),
            fill = if (isBack) {
                getColorFromInt(color).toString()
            } else {
                "none"
            },
            rx = 2,
            ry = 2,
            //tooltip: String = "" - на случай, если будем выводить только прямоугольники без текста (для мобильной версии, например)
        )
        alLegendBack += legendBack

        val textX = x1 + (LEGEND_TEXT_MARGIN * scaleKoef).toInt()
        val textY = y2 - (LEGEND_TEXT_MARGIN * scaleKoef).toInt()
        val legendText = SvgTextData(
            x = textX,
            y = textY,
            text = text,
            stroke = if (isBack) COLOR_MAIN_TEXT else getColorFromInt(color),
            hAnchor = "start",
            vAnchor = "hanging",
            transform = "rotate(-90 $textX $textY)"
        )
        alLegendText += legendText
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun setMode(newMode: GraphicWorkMode) {
        when (newMode) {
            GraphicWorkMode.PAN -> {
                isPanButtonEnabled.value = false
                isZoomButtonEnabled.value = true
            }

            GraphicWorkMode.ZOOM_BOX -> {
                isPanButtonEnabled.value = true
                isZoomButtonEnabled.value = false
            }
        }
        grCurMode.value = newMode
    }

    private fun zoomIn() {
        val t1 = grViewCoord.t1
        val t2 = grViewCoord.t2
        val grWidth = grViewCoord.width

        val newT1 = t1 + grWidth / 4
        val newT2 = t2 - grWidth / 4

        if (newT2 - newT1 >= MIN_SCALE_X) {
            grRefreshView(GraphicViewCoord(newT1, newT2))
        }

    }

    private fun zoomOut() {
        val t1 = grViewCoord.t1
        val t2 = grViewCoord.t2
        val grWidth = grViewCoord.width

        val newT1 = t1 - grWidth / 2
        val newT2 = t2 + grWidth / 2
        if (newT2 - newT1 <= MAX_SCALE_X) {
            grRefreshView(GraphicViewCoord(newT1, newT2))
        }
    }

    private fun doChangeGraphicVisibility() {
        isShowGraphicVisible.value = false
        alGraphicVisibleData.forEach { graphicVisibleData ->
            invokeSaveUserProperty(
                SaveUserPropertyRequest(
                    name = graphicVisibleData.name,
                    value = graphicVisibleData.check.value.toString(),
                )
            )
        }
        grRefreshView(null)
    }

    private fun onGrMouseOver(syntheticMouseEvent: SyntheticMouseEvent, graphicElement: SvgElementData) {
        val mouseOffsetY = syntheticMouseEvent.offsetY.toInt()
        val mouseClientX = syntheticMouseEvent.clientX
        val mouseClientY = syntheticMouseEvent.clientY

        val arrViewBoxBody = getGraphicViewBoxBody()

        if (graphicElement is SvgLineData) {
            val yData = alYData[graphicElement.tooltip.toInt()]
            //--- именно в таком порядке, чтобы не нарваться на 0 при целочисленном делении
            //--- (yData.y1 - нижняя/большая координата, yData.y2 - верхняя/меньшая координата)
            var value = (yData.value2 - yData.value1) * (yData.y1 - (mouseOffsetY + arrViewBoxBody[1])) / (yData.y1 - yData.y2) + yData.value1
            if (yData.isReversedY) {
                value = -value
            }
            val tooltipValue = getSplittedDouble(value, yData.prec, true, '.')

            val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(mouseClientX, mouseClientY)

            grTooltipVisible.value = true
            grTooltipText.value = tooltipValue
            grTooltipLeft.value = tooltipX.px
            grTooltipTop.value = tooltipY.px
            grTooltipOffTime = Date.now() + 3000

        } else if (graphicElement.tooltip.isNotEmpty()) {
            val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(mouseClientX, mouseClientY)

            grTooltipVisible.value = true
            grTooltipText.value = graphicElement.tooltip.replace("\n", "<br>")
            grTooltipLeft.value = tooltipX.px
            grTooltipTop.value = tooltipY.px
            grTooltipOffTime = Date.now() + 3000
        } else {
            grTooltipVisible.value = false
        }
    }

    private fun onGrMouseOut() {
        //--- через 3 сек выключить тултип, если не было других активаций тултипов
        //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
        //--- причём после ухода с графика других mouseleave не вызывается.
        setGraphicAndXyTooltipOffTimeout(grTooltipOffTime, grTooltipVisible)
    }

    private fun onGrMousePressed(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        val (svgBodyLeft, svgBodyTop) = calcBodyLeftAndTop()

        if (isNeedOffsetCompensation) {
            mouseX -= svgBodyLeft
            mouseY -= svgBodyTop
        }

        //--- при нажатой кнопке мыши положение курсора не отслеживается
        disableCursorLinesAndLabels()

        when (grCurMode.value) {
            GraphicWorkMode.PAN -> {
                panPointOldX = mouseX
                panPointOldY = mouseY
                panDX = 0
            }

            GraphicWorkMode.ZOOM_BOX -> {
                //            case SELECT_FOR_PRINT:
                val arrViewBoxBody = getGraphicViewBoxBody()

                mouseRect.apply {
                    isVisible.value = true
                    x1.value = mouseX
                    y1.value = arrViewBoxBody[1]
                    x2.value = mouseX
                    y2.value = arrViewBoxBody[1] + arrViewBoxBody[3] - scaleKoef.roundToInt()
                }

                setTimeLabel(svgBodyLeft, grSvgBodyWidth.value, mouseX, alTimeLabel[1])
                setTimeLabel(svgBodyLeft, grSvgBodyWidth.value, mouseX, alTimeLabel[2])
            }
        }

        isMouseDown = true
    }

    private fun onGrMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        val (svgBodyLeft, svgBodyTop) = calcBodyLeftAndTop()

        if (isNeedOffsetCompensation) {
            mouseX -= svgBodyLeft
            mouseY -= svgBodyTop
        }

        //--- mouse dragged
        if (isMouseDown) {
            when (grCurMode.value) {
                GraphicWorkMode.PAN -> {
                    var dx = mouseX - panPointOldX
                    var dy = mouseY - panPointOldY

                    //--- чтобы убрать раздражающую диагональную прокрутку, нормализуем dx и dy - выбираем только один из них
                    if (abs(dx) >= abs(dy)) {
                        dy = 0
                    } else {
                        dx = 0
                    }

                    val arrViewBoxAxis = getGraphicViewBoxAxis()
                    val arrViewBoxBody = getGraphicViewBoxBody()
                    val arrViewBoxLegend = getGraphicViewBoxLegend()

                    arrViewBoxBody[0] -= dx

                    if (dy > 0) {
                        listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                            arr[1] -= dy
                            if (arr[1] < 0) {
                                arr[1] = 0
                            }
                        }
                    } else if (dy < 0 && pixStartY - arrViewBoxAxis[1] > grSvgHeight.value) {
                        listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                            arr[1] -= dy
                        }
                    }

                    panPointOldX = mouseX
                    panPointOldY = mouseY
                    panDX += dx

                    setGraphicViewBoxAxis(arrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                    setGraphicViewBoxBody(arrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))
                    setGraphicViewBoxLegend(arrayOf(arrViewBoxLegend[0], arrViewBoxLegend[1], arrViewBoxLegend[2], arrViewBoxLegend[3]))

                    setGraphicTextOffset(svgBodyLeft, svgBodyTop)
                }

                GraphicWorkMode.ZOOM_BOX -> {
                    //            case SELECT_FOR_PRINT:

                    if (mouseRect.isVisible.value && mouseX >= 0 && mouseX <= grSvgBodyWidth.value) {
                        mouseRect.x2.value = mouseX
                        setTimeLabel(svgBodyLeft, grSvgBodyWidth.value, mouseX, alTimeLabel[2])
                    }
                }
            }
        }
        //--- mouse moved
        else {
            when (grCurMode.value) {
                GraphicWorkMode.PAN, GraphicWorkMode.ZOOM_BOX -> {
                    //                    case SELECT_FOR_PRINT:
                    if (mouseX in 0..grSvgBodyWidth.value) {
                        val arrViewBoxBody = getGraphicViewBoxBody()

                        grTimeLine.isVisible.value = true
                        grTimeLine.x1.value = mouseX
                        grTimeLine.y1.value = arrViewBoxBody[1]
                        grTimeLine.x2.value = mouseX
                        grTimeLine.y2.value = arrViewBoxBody[1] + arrViewBoxBody[3]

                        setTimeLabel(svgBodyLeft, grSvgBodyWidth.value, mouseX, alTimeLabel[0])

                        if (isShowGraphicDataVisible.value) {
                            fillGraphicData(mouseX)
                        }
                    } else {
                        disableCursorLinesAndLabels()
                    }
                }
            }
        }
    }

    private fun onGrMouseReleased(
        isNeedOffsetCompensation: Boolean,
        aMouseX: Double,
        aMouseY: Double,
        shiftKey: Boolean,
        ctrlKey: Boolean,
        altKey: Boolean
    ) {

        when (grCurMode.value) {
            GraphicWorkMode.PAN -> {
                //--- перезагружаем график, только если был горизонтальный сдвиг
                if (abs(panDX) >= 1) {
                    //--- именно в этом порядке операндов, чтобы:
                    //--- не было всегда 0 из-за целочисленного деления panDX / svgBodyWidth
                    //--- и не было возможного переполнения из-за умножения viewCoord.width * panDX
                    val deltaT = getTimeFromX(-panDX, grSvgBodyWidth.value, 0, grViewCoord.width)
                    grViewCoord.moveRel(deltaT)
                    grRefreshView(grViewCoord)
                }
                panPointOldX = 0
                panPointOldY = 0
                panDX = 0
            }

            GraphicWorkMode.ZOOM_BOX -> {
                //            case SELECT_FOR_PRINT:
                if (mouseRect.isVisible.value) {
                    mouseRect.isVisible.value = false
                    alTimeLabel[1].isVisible.value = false
                    alTimeLabel[2].isVisible.value = false

                    //--- если размер прямоугольника меньше 8 pix, то это видимо ошибка - игнорируем
                    if (abs(mouseRect.x2.value - mouseRect.x1.value) >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt() &&
                        abs(mouseRect.y2.value - mouseRect.y1.value) >= (MIN_USER_RECT_SIZE * scaleKoef).roundToInt()
                    ) {

                        //--- именно в этом порядке операндов, чтобы:
                        //--- не было всегда 0 из-за целочисленного деления min( mouseRect.x1, mouseRect.x2 ) / svgBodyWidth
                        //--- и не было возможного переполнения из-за умножения viewCoord.width * min( mouseRect.x1, mouseRect.x2 )
                        val newT1 = getTimeFromX(min(mouseRect.x1.value, mouseRect.x2.value), grSvgBodyWidth.value, grViewCoord.t1, grViewCoord.width)
                        val newT2 = getTimeFromX(max(mouseRect.x1.value, mouseRect.x2.value), grSvgBodyWidth.value, grViewCoord.t1, grViewCoord.width)
                        if (newT2 - newT1 >= MIN_SCALE_X) {
                            if (grCurMode.value == GraphicWorkMode.ZOOM_BOX) {
                                grRefreshView(GraphicViewCoord(newT1, newT2))
                            } else {
                                //!!! пока пусть будет сразу печать с текущими границами, без возможности их отдельного определения перед печатью (а оно надо ли ?)
                                //outRect = mouseRectangle.getBoundsReal(  null  );
                                //outViewStage1();
                            }
                        }
                    }
                }
            }
        }

        isMouseDown = false
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun disableCursorLinesAndLabels() {
        grTimeLine.isVisible.value = false
        alTimeLabel.forEach { timeLabelData ->
            timeLabelData.isVisible.value = false
        }
    }

    private fun setTimeLabel(svgBodyLeft: Int, svgBodyWidth: Int, x: Int, timeLabelData: TimeLabelData) {
        val timeOffset = root.timeOffset

        val cursorTime = getTimeFromX(x, svgBodyWidth, grViewCoord.t1, grViewCoord.width)

        timeLabelData.isVisible.value = true
        timeLabelData.text.value = DateTime_DMYHMS(timeOffset, cursorTime)    //!!!.replace(" ", "\n") не работают замена на <br> и \n
        timeLabelData.pos.value = {
            bottom(0.px)
            if (x > svgBodyWidth * 7 / 8) {
                right((svgBodyWidth - x).px)
            } else {
                left((svgBodyLeft + x).px)
            }
        }
    }

    //--- в double и обратно из-за ошибок округления
    private fun getTimeFromX(pixX: Int, pixWidth: Int, timeStart: Int, timeWidth: Int): Int = (1.0 * timeWidth * pixX / pixWidth + timeStart).roundToInt()

    private fun fillGraphicData(mouseX: Int) {
        val timeOffset = root.timeOffset
        val cursorTime = getTimeFromX(mouseX, grSvgBodyWidth.value, grViewCoord.t1, grViewCoord.width)

        alGraphicDataData.clear()
        alGraphicDataData += "Дата/время: " + DateTime_YMDHMS(timeOffset, cursorTime)

        alElement.forEach { (_, element) ->

            element.alGDC.map { gdc ->
                gdc to element.alAxisYData[gdc.axisYIndex]
            }.filter { (gdc, _) ->
                gdc.type == GraphicDataContainer.ElementType.LINE
            }.forEach { (gdc, yData) ->

                val index = gdc.alGLD.indexOfFirst { gld ->
                    gld.x >= cursorTime
                }
                val s = (if (index != -1) {
                    val gld = gdc.alGLD[index]
                    if (gld.x == cursorTime) {
                        gld.y
                    } else if (index > 0) {
                        val gldPrev = gdc.alGLD[index - 1]
                        1.0 * (cursorTime - gldPrev.x) / (gld.x - gldPrev.x) * (gld.y - gldPrev.y) + gldPrev.y
                    } else {
                        null
                    }
                } else {
                    null
                })?.let { yValue ->
                    yValue * if (gdc.isReversedY) {
                        -1
                    } else {
                        1
                    }
                }?.let { y ->
                    getSplittedDouble(y, yData.prec, true, '.')
                } ?: "-"
                alGraphicDataData += "${yData.title} = $s"
            }
        }
    }

    private fun onGrMouseWheel(syntheticWheelEvent: SyntheticWheelEvent) {
        val isCtrl = syntheticWheelEvent.ctrlKey
        val mouseX = syntheticWheelEvent.offsetX.toInt()
        val deltaY = syntheticWheelEvent.deltaY.toInt()

        val (svgBodyLeft, svgBodyTop) = calcBodyLeftAndTop()

        if (grCurMode.value == GraphicWorkMode.PAN && !isMouseDown || grCurMode.value == GraphicWorkMode.ZOOM_BOX && !isMouseDown) {
            //|| grControl.curMode == GraphicModel.WorkMode.SELECT_FOR_PRINT && grControl.selectorX1 < 0  ) {
            //--- масштабирование
            if (isCtrl) {
                val t1 = grViewCoord.t1
                val t2 = grViewCoord.t2
                //--- вычисляем текущую координату курсора в реальных координатах
                val curT = getTimeFromX(mouseX, grSvgBodyWidth.value, t1, grViewCoord.width)

                val newT1 = if (deltaY < 0) {
                    curT - (curT - t1) / 2
                } else {
                    curT - (curT - t1) * 2
                }
                val newT2 = if (deltaY < 0) {
                    curT + (t2 - curT) / 2
                } else {
                    curT + (t2 - curT) * 2
                }

                if (newT2 - newT1 in MIN_SCALE_X..MAX_SCALE_X) {
                    grRefreshView(GraphicViewCoord(newT1, newT2))
                }
            }
            //--- вертикальная прокрутка
            else {
                val arrViewBoxAxis = getGraphicViewBoxAxis()
                val arrViewBoxBody = getGraphicViewBoxBody()
                val arrViewBoxLegend = getGraphicViewBoxLegend()

                val dy = (deltaY * scaleKoef).roundToInt()

                if (dy < 0) {
                    listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                        arr[1] += dy
                        if (arr[1] < 0) {
                            arr[1] = 0
                        }
                    }
                } else if (dy > 0 && pixStartY - arrViewBoxAxis[1] > grSvgHeight.value) {
                    listOf(arrViewBoxAxis, arrViewBoxBody, arrViewBoxLegend).forEach { arr ->
                        arr[1] += dy
                    }
                }

                setGraphicViewBoxAxis(arrayOf(arrViewBoxAxis[0], arrViewBoxAxis[1], arrViewBoxAxis[2], arrViewBoxAxis[3]))
                setGraphicViewBoxBody(arrayOf(arrViewBoxBody[0], arrViewBoxBody[1], arrViewBoxBody[2], arrViewBoxBody[3]))
                setGraphicViewBoxLegend(arrayOf(arrViewBoxLegend[0], arrViewBoxLegend[1], arrViewBoxLegend[2], arrViewBoxLegend[3]))

                setGraphicTextOffset(svgBodyLeft, svgBodyTop)
            }
        }
    }

}

//    /*
//                    else if( comp == butShowForTime ) {
//                        try {
//                            val begTime = Arr_DateTime( appContainer.timeZone, arrayOf(
//                                Integer.parseInt( arrTxtDateTime[ 2 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 1 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 0 ]!!.text ),
//                                Integer.parseInt( arrTxtDateTime[ 3 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 4 ]!!.text ), 0 ) )
//                            val endTime = Arr_DateTime( appContainer.timeZone, arrayOf(
//                                Integer.parseInt( arrTxtDateTime[ 7 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 6 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 5 ]!!.text ),
//                                Integer.parseInt( arrTxtDateTime[ 8 ]!!.text ), Integer.parseInt( arrTxtDateTime[ 9 ]!!.text ), 0 ) )
//                            val graphicWidth = endTime - begTime
//
//                            if( graphicWidth < GraphicModel.MIN_SCALE_X ) showError( "Ошибка задания периода", "Слишком короткий период для показа графика" )
//                            else if( graphicWidth > GraphicModel.MAX_SCALE_X ) showError( "Ошибка задания периода", "Слишком большой период для показа графика" )
//                            else grRefreshView( GraphicViewCoord( begTime, endTime ), 0 )
//                        }
//                        catch( nfe: NumberFormatException ) {
//                            showError( "Ошибка задания периода", "Неправильно заданы дата/время" )
//                        }
//
//                    }
//
//     */
//        ).add(
//            getGraphicSpecificComponentData()
//        )
//    }
//}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private class GraphicVisibleData(
    val descr: String,
    val name: String,
    val check: MutableState<Boolean>,
)

private class LegendData(
    val text: String,
    val style: StyleScope.() -> Unit,
)

private class GraphicElementData(
    val title: SvgTextData,
    val alAxisYLine: MutableList<SvgLineData>,
    val alAxisYText: MutableList<SvgTextData>,
    val alAxisXLine: MutableList<SvgLineData>,
    val alAxisXText: MutableList<SvgTextData>,
    val alGraphicBack: MutableList<SvgRectData>,
    val alGraphicLine: MutableList<SvgLineData>,
    val alGraphicPoint: MutableList<SvgCircleData>,
    val alGraphicText: MutableList<GraphicTextData>,
    val alLegendBack: MutableList<SvgRectData>,
    val alLegendText: MutableList<SvgTextData>,
)

private class LineData(
    val isVisible: MutableState<Boolean>,
    val x1: MutableState<Int>,
    val y1: MutableState<Int>,
    val x2: MutableState<Int>,
    val y2: MutableState<Int>,
    var width: MutableState<Int>,
)

private class YData(
    val y1: Int,
    val y2: Int,
    val value1: Double,
    val value2: Double,
    val prec: Int,
    val isReversedY: Boolean
)

private class GraphicTextData(
    var isVisible: Boolean,
    val x: Int,
    val y: Int,
    var pos: StyleScope.() -> Unit,
    val style: StyleScope.() -> Unit,
    val text: String,
    tooltip: String
) : SvgElementData(tooltip)

private class TimeLabelData(
    val isVisible: MutableState<Boolean> = mutableStateOf(false),
    var pos: MutableState<StyleScope.() -> Unit> = mutableStateOf({}),
    var text: MutableState<String> = mutableStateOf(""),
)

/*
                    <!-- текст теперь выводится по-другому, но может ещё пригодиться для других прямоугольников
                        <rect v-for="graphicText in element.arrGraphicText"
                              v-bind:x="graphicText.x"
                              v-bind:y="graphicText.y"
                              v-bind:width="graphicText.width"
                              v-bind:height="graphicText.height"
                              v-bind:stroke="graphicText.stroke"
                              v-bind:fill="graphicText.fill"
                              v-bind:strokeWidth="graphicText.strokeWidth"
                              v-bind:rx="graphicText.rx"
                              v-bind:ry="graphicText.ry"
            """ +
        if (withInteractive) {
            """
                              v-on:mouseenter="onMouseOver( ${'$'}event, graphicText )"
                              v-on:mouseleave="onMouseOut()"
                    """
        } else {
            ""
        } +
        """
                        />
                    -->
*/

/*
    private val panTimeBar = HBox( iAppContainer.DEFAULT_SPACING )
    private val arrTxtDateTime = arrayOfNulls<TextField>( 10 )
    private lateinit var butShowForTime: Button

        //--- нижняя панель временнОго масштабирования
        for( i in arrTxtDateTime.indices ) {
            arrTxtDateTime[ i ] = TextField()
            arrTxtDateTime[ i ]!!.setPrefColumnCount( if( i == 2 || i == 7 ) 4 else 2 )
            arrTxtDateTime[ i ]!!.setAlignment( Pos.CENTER )
            arrTxtDateTime[ i ]!!.setFont( curControlFont )
            arrTxtDateTime[ i ]!!.setOnKeyPressed( this )
        }

        butShowForTime = Button( "Показать" )
        butShowForTime.tooltip = Tooltip( "Показать график на заданный период" )
        butShowForTime.setOnAction( this )

        panTimeBar.children.addAll( Label( "Начало:" ), arrTxtDateTime[ 0 ], Label( "." ), arrTxtDateTime[ 1 ], Label( "." ), arrTxtDateTime[ 2 ], Label( " " ), arrTxtDateTime[ 3 ], Label( ":" ), arrTxtDateTime[ 4 ], Label( "Окончание:" ), arrTxtDateTime[ 5 ], Label( "." ), arrTxtDateTime[ 6 ], Label( "." ), arrTxtDateTime[ 7 ], Label( " " ), arrTxtDateTime[ 8 ], Label( ":" ), arrTxtDateTime[ 9 ], butShowForTime )

 */


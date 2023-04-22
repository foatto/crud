package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.iCoreAppContainer
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyElement
import foatto.core.app.xy.XyViewCoord
import foatto.core.app.xy.geom.XyPoint
import foatto.core.app.xy.geom.XyRect
import foatto.core.link.XyElementClientType
import foatto.core.link.XyElementConfig
import foatto.core.link.XyResponse
import foatto.core.util.getRandomInt
import foatto.core_compose.model.TitleData
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.MENU_BAR_ID
import foatto.core_compose_web.MENU_CLOSER_BUTTON_ID
import foatto.core_compose_web.Root
import foatto.core_compose_web.TOP_BAR_ID
import foatto.core_compose_web.control.composable.getMultilineText
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.control.model.XyElementDataType
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.MIN_USER_RECT_SIZE
import foatto.core_compose_web.util.getColorFromInt
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.dominantBaseline
import org.jetbrains.compose.web.attributes.stroke
import org.jetbrains.compose.web.attributes.strokeDasharray
import org.jetbrains.compose.web.attributes.strokeWidth
import org.jetbrains.compose.web.attributes.textAnchor
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.userSelect
import org.jetbrains.compose.web.css.properties.verticalAlign
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.svg.*
import org.w3c.dom.svg.SVGElement
import kotlin.js.Date
import kotlin.math.max
import kotlin.math.roundToInt

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

val COLOR_XY_LABEL_BACK = hsl(60, 100, 50)
val COLOR_XY_LABEL_BORDER = hsl(60, 100, 25)

private val COLOR_XY_POLYGON_CASUAL = hsla(60, 100, 50, 0.25)  // полупрозрачный жёлтый
private val COLOR_XY_POLYGON_ACTUAL = hsla(30, 100, 50, 0.25)  // полупрозрачный оранжевый
val COLOR_XY_POLYGON_BORDER: CSSColorValue = hsl(0, 100, 50)          // красный

private val XY_PADDING = 0.2.cssRem
private val XY_SIDE_PADDING = 0.4.cssRem

val arrStyleXyDistancerPadding: Array<CSSSize> = arrayOf(
    XY_PADDING,
    XY_SIDE_PADDING,
    XY_PADDING,
    XY_SIDE_PADDING
)
private val arrStyleXyTextPadding: Array<CSSSize> = arrayOf(
    XY_PADDING,
    XY_SIDE_PADDING,
    XY_PADDING,
    XY_SIDE_PADDING
)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

abstract class AbstractXyControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val xyResponse: XyResponse,
    tabId: Int,
) : AbstractControl(tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    var containerPrefix: String = ""
    var presetSvgHeight: Int? = null
    var arrAddHeights: Array<Int> = emptyArray()

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected val xySvgWidth: MutableState<Int> = mutableStateOf(0)
    protected val xySvgHeight: MutableState<Int> = mutableStateOf(0)
    private val xyViewBoxBody = mutableStateOf("0 0 1 1")

    private val alXyElement = mutableStateListOf<List<XyElementData>>()

    private val xyTooltipVisible = mutableStateOf(false)
    private val xyTooltipText = mutableStateOf("")
    private val xyTooltipLeft = mutableStateOf(0.px)
    private val xyTooltipTop = mutableStateOf(0.px)

    protected val refreshInterval: MutableState<Int> = mutableStateOf(0)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val minXyScale: Int = xyResponse.documentConfig.hmElementConfig.minByOrNull { (_, value) -> value.scaleMin }!!.value.scaleMin
    private val maxXyScale: Int = xyResponse.documentConfig.hmElementConfig.maxByOrNull { (_, value) -> value.scaleMax }!!.value.scaleMax

    protected var xySvgLeft: Int = 0
    protected var xySvgTop: Int = 0

    protected var xyViewCoord: XyViewCoord = XyViewCoord(1, 0, 0, 1, 1)

    private var xyTooltipOffTime = 0.0

    private var refreshHandlerId = 0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @OptIn(ExperimentalComposeWebSvgApi::class)
    @Composable
    fun getXyElementTemplate(withInteractive: Boolean) {
        Svg(
            viewBox = xyViewBoxBody.value,
            attrs = {
                width(xySvgWidth.value)
                height(xySvgHeight.value)
                if (refreshInterval.value == 0 && withInteractive) {
                    onMouseDown { syntheticMouseEvent ->
                        onXyMousePressed(false, syntheticMouseEvent.offsetX, syntheticMouseEvent.offsetY)
                        syntheticMouseEvent.preventDefault()
                    }
                    onMouseMove { syntheticMouseEvent ->
                        onXyMouseMove(false, syntheticMouseEvent.offsetX, syntheticMouseEvent.offsetY)
                        syntheticMouseEvent.preventDefault()
                    }
                    onMouseUp { syntheticMouseEvent ->
                        onXyMouseReleased(
                            isNeedOffsetCompensation = false,
                            aMouseX = syntheticMouseEvent.offsetX,
                            aMouseY = syntheticMouseEvent.offsetY,
                            shiftKey = syntheticMouseEvent.shiftKey,
                            ctrlKey = syntheticMouseEvent.ctrlKey,
                            altKey = syntheticMouseEvent.altKey
                        )
                        syntheticMouseEvent.preventDefault()
                    }
                    onWheel { syntheticWheelEvent ->
                        onXyMouseWheel(syntheticWheelEvent)
                        syntheticWheelEvent.preventDefault()
                    }
                    onTouchStart { syntheticTouchEvent ->
                        syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                            onXyMousePressed(true, firstTouch.clientX.toDouble(), firstTouch.clientY.toDouble())
                        }
                        syntheticTouchEvent.preventDefault()
                    }
                    onTouchMove { syntheticTouchEvent ->
                        syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                            onXyMouseMove(true, firstTouch.clientX.toDouble(), firstTouch.clientY.toDouble())
                        }
                        syntheticTouchEvent.preventDefault()
                    }
                    onTouchEnd { syntheticTouchEvent ->
                        syntheticTouchEvent.changedTouches.item(0)?.let { firstTouch ->
                            onXyMouseReleased(
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
            for (alElement in alXyElement) {
                for (element in alElement) {
                    when (element.type) {
                        XyElementDataType.CIRCLE -> {
                            Circle(
                                cx = element.x,
                                cy = element.y,
                                r = element.radius,
                                attrs = {
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.ELLIPSE -> {
                            Ellipse(
                                cx = element.x,
                                cy = element.y,
                                rx = element.rx,
                                ry = element.ry,
                                attrs = {
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.IMAGE -> {
                            Image(
                                href = element.url,
                                attrs = {
                                    x(element.x)
                                    y(element.y)
                                    width(element.width)
                                    height(element.height)
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.LINE -> {
                            Line(
                                x1 = element.x1,
                                y1 = element.y1,
                                x2 = element.x2,
                                y2 = element.y2,
                                attrs = {
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )

                        }

                        XyElementDataType.PATH -> {
                            Path(
                                d = element.strPoints!!,
                                attrs = {
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.POLYLINE -> {
                            Polyline(
                                points = element.arrPoints.value,
                                attrs = {
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.POLYGON -> {
                            Polygon(
                                points = element.arrPoints.value,
                                attrs = {
                                    style {
                                        element.style(this)
                                    }
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    stroke(if (element.isSelected) COLOR_XY_POLYGON_BORDER else (element.stroke ?: COLOR_TRANSPARENT))
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.RECT -> {
                            Rect(
                                x = element.x,
                                y = element.y,
                                width = element.width,
                                height = element.height,
                                attrs = {
                                    rx(element.rx)
                                    ry(element.ry)
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.stroke?.let {
                                        stroke(element.stroke)
                                    }
                                    element.strokeWidth?.let {
                                        strokeWidth(element.strokeWidth)
                                    }
                                    if (element.isSelected) {
                                        strokeDasharray(element.strokeDash ?: "")
                                    } else {
                                        strokeDasharray("")
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                }
                            )
                        }

                        XyElementDataType.SVG_TEXT -> {
                            SvgText(
                                x = element.x,
                                y = element.y,
                                text = element.text,
                                attrs = {
                                    style {
                                        fontSize((1.0 * scaleKoef).cssRem)
                                        element.style(this)
                                    }
                                    element.transform?.let {
                                        transform(element.transform)
                                    }
                                    element.fill?.let {
                                        fill(element.fill.toString())
                                    }
                                    textAnchor(element.hAnchor)
                                    dominantBaseline(element.vAnchor)
                                    onMouseEnter { syntheticMouseEvent ->
                                        onXyMouseOver(syntheticMouseEvent, element)
                                    }
                                    onMouseLeave {
                                        onXyMouseOut()
                                    }
                                    //--- поскольку нажатия на тексты могут быть срочными/неотложными действиями операторов/диспетчеров,
                                    //--- то отрабатываем их независимо от режима обновления экрана и режима включенности интерактива
                                    //if (refreshInterval.value == 0 && withInteractive) {
                                    onMouseDown { syntheticMouseEvent ->
                                        if (!element.isReadOnly) {
                                            onXyTextPressed(syntheticMouseEvent, element)
                                        }
                                        syntheticMouseEvent.preventDefault()
                                    }
                                    onTouchStart { syntheticTouchEvent ->
                                        if (!element.isReadOnly) {
                                            onXyTextPressed(syntheticTouchEvent, element)
                                        }
                                        syntheticTouchEvent.preventDefault()
                                    }
                                }
                            )
                        }

                        else -> {
                        }
                    }
                }
            }
            //--- for adding specific SVG-elements
            addSpecifigSvg(this)
        }

        for (alElement in alXyElement) {
            for (element in alElement) {
                if (element.type == XyElementDataType.HTML_TEXT && element.isVisible) {
                    Div(
                        attrs = {
                            style {
                                element.style(this)
                                element.pos(this)
                            }
                            onMouseEnter { syntheticMouseEvent ->
                                onXyMouseOver(syntheticMouseEvent, element)
                            }
                            onMouseLeave {
                                onXyMouseOut()
                            }
                            //--- поскольку нажатия на тексты могут быть срочными/неотложными действиями операторв/диспетчеров,
                            //--- то отрабатываем их независимо от режима обновления экрана и режима включенности интерактива
                            //if (refreshInterval.value == 0 && withInteractive) {
                            onMouseDown { syntheticMouseEvent ->
                                if (!element.isReadOnly) {
                                    onXyTextPressed(syntheticMouseEvent, element)
                                }
                                syntheticMouseEvent.preventDefault()
                            }
                            onTouchStart { syntheticTouchEvent ->
                                if (!element.isReadOnly) {
                                    onXyTextPressed(syntheticTouchEvent, element)
                                }
                                syntheticTouchEvent.preventDefault()
                            }
                        }
                    ) {
                        getMultilineText(element.text)
                    }
                }
            }
        }

        if (xyTooltipVisible.value) {
            Div(
                attrs = {
                    style {
                        position(Position.Absolute)
                        left(xyTooltipLeft.value)
                        top(xyTooltipTop.value)
                        color(COLOR_MAIN_TEXT)
                        backgroundColor(COLOR_XY_LABEL_BACK)
                        setBorder(color = COLOR_XY_LABEL_BORDER, radius = styleButtonBorderRadius)
                        setPaddings(arrStyleControlTooltipPadding)
                        userSelect("none")
                    }
                }
            ) {
                getMultilineText(xyTooltipText.value)
            }

        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    protected open fun addSpecifigSvg(svg: ElementScope<SVGElement>) {
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun doXyMounted(
        startExpandKoef: Double,
        isCentered: Boolean,
        curScale: Int,
    ) {
        root.setTabInfo(tabId, xyResponse.shortTitle, xyResponse.fullTitle)
        alTitleData += xyResponse.fullTitle.split('\n').filter { it.isNotBlank() }.map { TitleData("", it) }

        doXySpecificComponentMounted(
            startExpandKoef = startExpandKoef,
            isCentered = isCentered,
            curScale = curScale,
        )
    }

    //--- public for Composite Control
    fun doXySpecificComponentMounted(
        startExpandKoef: Double,
        isCentered: Boolean,
        curScale: Int,
    ) {
        root.setWait(true)
        invokeXy(
            XyActionRequest(
                documentTypeName = xyResponse.documentConfig.name,
                action = XyAction.GET_COORDS,
                startParamId = xyResponse.startParamId
            )
        ) { xyActionResponse: XyActionResponse ->

            //--- принудительная установка полной высоты svg-элементов
            //--- (BUG: иначе высота либо равна 150px - если не указывать высоту,
            //--- либо равно width, если указать height="100%")
            calcSvgCoordAndSize()
            setXyViewBoxBody(arrayOf(0, 0, xySvgWidth.value, xySvgHeight.value))

            val newViewCoord = getXyCoordsDone(
                startExpandKoef = startExpandKoef,
                isCentered = isCentered,
                curScale = curScale,
                minCoord = xyActionResponse.minCoord!!,
                maxCoord = xyActionResponse.maxCoord!!,
            )

            //--- именно до xyRefreshView, чтобы не сбросить сразу после включения
            root.setWait(false)
            xyRefreshView(newViewCoord, true)
        }
    }

    private fun calcSvgCoordAndSize() {
        val menuBarElement = document.getElementById(MENU_BAR_ID)
        val menuCloserElement = document.getElementById(MENU_CLOSER_BUTTON_ID)

        val topBar = document.getElementById(TOP_BAR_ID)
        val svgTabPanel = document.getElementById("tab_panel")!!
        val svgXyTitle = document.getElementById("${containerPrefix}_title_$tabId")!!
        val svgXyToolbar = document.getElementById("${containerPrefix}_toolbar_$tabId")!!

        val menuBarWidth = if (root.isShowMainMenu.value) {
            menuBarElement?.clientWidth ?: 0
        } else {
            0
        }

        //svgBodyElement.clientLeft - BUG: всегда даёт 0
        xySvgLeft = menuBarWidth + (menuCloserElement?.clientWidth ?: 0)

        xySvgWidth.value = window.innerWidth - xySvgLeft

        //--- svgBodyElement.clientTop - BUG: всегда даёт 0
        xySvgTop = (topBar?.clientHeight ?: 0) +
            svgTabPanel.clientHeight +
            svgXyTitle.clientHeight +
            svgXyToolbar.clientHeight +
            arrAddHeights.sum()

        xySvgHeight.value = presetSvgHeight ?: (window.innerHeight - xySvgTop)
    }

    protected fun getXyViewBoxBody(): Array<Int> {
        return xyViewBoxBody.value.split(' ').map { it.toInt() }.toTypedArray()
    }

    protected fun setXyViewBoxBody(arrViewBox: Array<Int>) {
        xyViewBoxBody.value = "${arrViewBox[0]} ${arrViewBox[1]} ${arrViewBox[2]} ${arrViewBox[3]}"
    }

    private fun getXyCoordsDone(
        startExpandKoef: Double,
        isCentered: Boolean,
        curScale: Int,
        minCoord: XyPoint,
        maxCoord: XyPoint,
    ): XyViewCoord {

        var x1 = minCoord.x
        var y1 = minCoord.y
        var x2 = maxCoord.x
        var y2 = maxCoord.y

        val tmpW = x2 - x1
        val tmpH = y2 - y1
        //--- если пришли граничные координаты только одной точки,
        //--- то оставим текущий масштаб
        val scale = if (tmpW == 0 && tmpH == 0) {
            curScale
        } else {
            //--- прибавим по краям startExpandKoef, чтобы искомые элементы не тёрлись об края экрана
            x1 -= (tmpW * startExpandKoef).toInt()
            y1 -= (tmpH * startExpandKoef).toInt()
            x2 += (tmpW * startExpandKoef).toInt()
            y2 += (tmpH * startExpandKoef).toInt()
            //--- масштаб вычисляется исходя из размеров docView (аналогично zoomBox)
            calcXyScale(x1, y1, x2, y2)
        }

        if (isCentered) {
            val fullXyWidth = (xySvgWidth.value * scale / scaleKoef).toInt()
            val restXyWidth = fullXyWidth - (x2 - x1)
            x1 -= restXyWidth / 2
            x2 -= restXyWidth / 2

            val fullXyHeight = (xySvgHeight.value * scale / scaleKoef).toInt()
            val restXyHeight = fullXyHeight - (y2 - y1)
            y1 -= restXyHeight / 2
            y2 -= restXyHeight / 2
        }

        return XyViewCoord(scale, x1, y1, x2, y2)
    }

    protected fun calcXyScale(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int
    ): Int = max((x2 - x1) * scaleKoef / xySvgWidth.value, (y2 - y1) * scaleKoef / xySvgHeight.value).roundToInt()

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    abstract fun xyRefreshView(aView: XyViewCoord?, withWait: Boolean)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun getXyViewCoord(aScale: Int, aCenterX: Int, aCenterY: Int): XyViewCoord {
        val vc = XyViewCoord()

        vc.scale = aScale

        val rw = (xySvgWidth.value * aScale / scaleKoef).roundToInt()
        val rh = (xySvgHeight.value * aScale / scaleKoef).roundToInt()

        vc.x1 = aCenterX - rw / 2
        vc.y1 = aCenterY - rh / 2
        //--- чтобы избежать неточностей из-за целочисленного деления нечетных чисел пополам,
        //--- правую/нижнюю границу получим в виде ( t1 + rw ), а не в виде ( newCenterX + rw / 2 )
        vc.x2 = vc.x1 + rw
        vc.y2 = vc.y1 + rh

        return vc
    }

    //--- проверка масштаба на минимальный/максимальный и на кратность степени двойки
    protected fun checkXyScale(isScaleAlign: Boolean, curScale: Int, newScale: Int, isAdaptive: Boolean): Int {

        if (newScale < minXyScale) {
            return minXyScale
        }
        if (newScale > maxXyScale) {
            return maxXyScale
        }

        //--- нужно ли выравнивание масштаба к степени двойки?
        if (isScaleAlign) {
            //--- ПРОТЕСТИРОВАНО: нельзя допускать наличие масштабов, не являющихся степенью двойки,
            //--- иначе при приведении (растягивании/сжимании) произвольного масштаба к выровненному (степени 2)
            //--- получается битмап-карта отвратного качества

            //--- адаптивный алгоритм - "докручивает" масштаб до ожидаемого пользователем
            if (isAdaptive) {
                //--- если идёт процесс увеличения масштаба (удаление от пользователя),
                //--- то поможем ему - округлим масштаб в бОльшую сторону
                if (newScale >= curScale) {
                    var scale = minXyScale
                    while (scale <= maxXyScale) {
                        if (newScale <= scale) {
                            return scale
                        }
                        scale *= 2
                    }
                }
                //--- иначе (если идёт процесс уменьшения масштаба - приближение к пользователю),
                //--- то поможем ему - округлим масштаб в меньшую сторону
                else {
                    var scale = maxXyScale
                    while (scale >= minXyScale) {
                        if (newScale >= scale) {
                            return scale
                        }
                        scale /= 2
                    }
                }
            }
            //--- обычный алгоритм - просто даёт больший или равный масштаб, чтобы всё гарантированно уместилось
            else {
                var scale = minXyScale
                while (scale <= maxXyScale) {
                    if (newScale <= scale) {
                        return scale
                    }
                    scale *= 2
                }
            }
        } else {
            return newScale
        }
        //--- такого быть не должно, но всё-таки для проверки вернём 0, чтобы получить деление на 0
        return 0   //XyConfig.MAX_SCALE;
    }

    protected fun getXyElements(
        mapBitmapTypeName: String,
        withWait: Boolean,
        doAdditionalWork: (xyActionResponse: XyActionResponse) -> Unit = { _: XyActionResponse -> },
    ) {
        if (withWait) {
            root.setWait(true)
        }
        invokeXy(
            XyActionRequest(
                documentTypeName = xyResponse.documentConfig.name,
                action = XyAction.GET_ELEMENTS,
                startParamId = xyResponse.startParamId,
                viewCoord = xyViewCoord,
                bitmapTypeName = mapBitmapTypeName
            )
        ) { xyActionResponse: XyActionResponse ->

            //--- сбрасываем горизонтальный и вертикальный скроллинг/смещение
            val arrViewBoxBody = getXyViewBoxBody()
            setXyViewBoxBody(arrayOf(0, 0, arrViewBoxBody[2], arrViewBoxBody[3]))

            readXyElements(xyActionResponse.alElement)

            setXyTextOffset()

            if (withWait) {
                root.setWait(false)
            }

            doAdditionalWork(xyActionResponse)
        }
    }

    private fun readXyElements(alElement: List<XyElement>) {
        val hmLayer = mutableMapOf<Int, MutableList<XyElementData>>()
        alElement.forEach { element ->
            val elementConfig = xyResponse.documentConfig.hmElementConfig[element.typeName]!!
            val alLayer = hmLayer.getOrPut(elementConfig.layer) { mutableListOf() }

            readXyElementData(elementConfig, element, alLayer)
        }

        alXyElement.clear()
        alXyElement.addAll(hmLayer.toList().sortedBy { it.first }.map { it.second })
    }

    private fun readXyElementData(
        elementConfig: XyElementConfig,
        element: XyElement,
        alLayer: MutableList<XyElementData>,
    ) {

        val lineWidth = element.lineWidth
        val drawColor = if (element.drawColor == 0) {
            COLOR_TRANSPARENT
        } else {
            getColorFromInt(element.drawColor)
        }
        val fillColor = if (element.fillColor == 0) {
            COLOR_TRANSPARENT
        } else {
            getColorFromInt(element.fillColor)
        }

        when (elementConfig.clientType) {
            XyElementClientType.BITMAP -> {
                val p = element.alPoint.first()

                if (element.imageName.isBlank()) {
                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.RECT,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            width = (element.imageWidth / xyViewCoord.scale * scaleKoef).roundToInt(),
                            height = (element.imageHeight / xyViewCoord.scale * scaleKoef).roundToInt(),
                            stroke = hsl(0, 0, 50), // gray
                            strokeWidth = 1, //scaleKoef,
                            strokeDash = "${scaleKoef * 2},${scaleKoef * 2}",
                            tooltip = element.imageName,
                            isReadOnly = element.isReadOnly
                        )
                    )
                } else {
                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.IMAGE,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            width = (element.imageWidth / xyViewCoord.scale * scaleKoef).roundToInt(),
                            height = (element.imageHeight / xyViewCoord.scale * scaleKoef).roundToInt(),
                            url = element.imageName,
                            isReadOnly = element.isReadOnly
                        )
                    )
                }
            }

            XyElementClientType.ICON -> {
                val p = element.alPoint.first()

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.IMAGE,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = (((p.x - xyViewCoord.x1) / xyViewCoord.scale - element.calcAnchorXKoef() * element.imageWidth).toInt() * scaleKoef).roundToInt(),
                        y = (((p.y - xyViewCoord.y1) / xyViewCoord.scale - element.calcAnchorYKoef() * element.imageHeight).toInt() * scaleKoef).roundToInt(),
                        width = (element.imageWidth * scaleKoef).roundToInt(),
                        height = (element.imageHeight * scaleKoef).roundToInt(),
                        transform = "rotate(${element.rotateDegree} ${((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()} ${((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()})",
                        url = element.imageName,
                        tooltip = element.toolTipText,
                        isReadOnly = element.isReadOnly
                    )
                )
            }

            XyElementClientType.MARKER -> {
                val p = element.alPoint.first()
                val x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                val y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()
                val halfX = (element.markerSize * scaleKoef / 2).roundToInt()
                val halfY = (element.markerSize * scaleKoef / 2).roundToInt()

                when (element.markerType) {

                    XyElement.MarkerType.ARROW -> {
                        val points = arrayOf(
                            x - halfX / 2, y,
                            x - halfX, y - halfY,
                            x + halfX * 2, y,
                            x - halfX, y + halfY,
                        )
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLYGON,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                arrPoints = mutableStateOf(points),
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.CIRCLE -> {
                        if (element.markerSize2 == 0 || element.markerSize == element.markerSize2) {
                            alLayer.add(
                                XyElementData(
                                    type = XyElementDataType.CIRCLE,
                                    elementId = element.elementId,
                                    objectId = element.objectId,
                                    x = x,
                                    y = y,
                                    radius = halfX,
                                    fill = fillColor,
                                    stroke = drawColor,
                                    strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                    strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                    transform = "rotate(${element.rotateDegree} $x $y)",
                                    tooltip = element.toolTipText,
                                    isReadOnly = element.isReadOnly
                                )
                            )
                        } else {
                            alLayer.add(
                                XyElementData(
                                    type = XyElementDataType.ELLIPSE,
                                    elementId = element.elementId,
                                    objectId = element.objectId,
                                    x = x,
                                    y = y,
                                    rx = (element.markerSize * scaleKoef / 2).roundToInt(),
                                    ry = (element.markerSize2 * scaleKoef / 2).roundToInt(),
                                    stroke = drawColor,
                                    fill = fillColor,
                                    strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                    strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                    transform = "rotate(${element.rotateDegree} $x $y)",
                                    tooltip = element.toolTipText,
                                    isReadOnly = element.isReadOnly
                                )
                            )
                        }
                    }

                    XyElement.MarkerType.CROSS -> {
                        val path =
                            "M${x - halfX},${y - halfY}" +
                                "L${x + halfX},${y + halfY}" +
                                "M${x + halfX},${y - halfY}" +
                                "L${x - halfX},${y + halfY}"
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.PATH,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                strPoints = path,
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.DIAMOND -> {
                        val points = arrayOf(
                            x, y - halfY,
                            x + halfX, y,
                            x, y + halfY,
                            x - halfX, y,
                        )
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLYGON,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                arrPoints = mutableStateOf(points),
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.PLUS -> {
                        val path =
                            "M$x,${y - halfY}" +
                                "L$x,${y + halfY}" +
                                "M${x - halfX},$y" +
                                "L${x + halfX},$y"
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.PATH,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                strPoints = path,
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.SQUARE -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.RECT,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                x = x - halfX,
                                y = y - halfY,
                                width = 2 * halfX,
                                height = 2 * halfY,
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.TRIANGLE -> {
                        val points = arrayOf(
                            x, y - halfY,
                            x + halfX, y + halfY,
                            x - halfX, y + halfY,
                        )
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLYGON,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                arrPoints = mutableStateOf(points),
                                stroke = drawColor,
                                fill = fillColor,
                                strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                                strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                                transform = "rotate(${element.rotateDegree} $x $y)",
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }
                }
            }

            XyElementClientType.POLY -> {
                val points = mutableListOf<Int>()
                element.alPoint.forEach {
                    val x = ((it.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                    val y = ((it.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()
                    points += x
                    points += y
                }
                val style: StyleScope.() -> Unit = if (!element.isReadOnly && element.isClosed) {
                    {
                        cursor("pointer")
                    }
                } else {
                    {}
                }
                alLayer.add(
                    XyElementData(
                        type = if (element.isClosed) {
                            XyElementDataType.POLYGON
                        } else {
                            XyElementDataType.POLYLINE
                        },
                        elementId = element.elementId,
                        objectId = element.objectId,
                        arrPoints = mutableStateOf(points.toTypedArray()),
                        stroke = drawColor,
                        fill = fillColor,
                        strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                        strokeDash = "${scaleKoef * lineWidth * 2},${scaleKoef * lineWidth * 2}",
                        tooltip = element.toolTipText,
                        isReadOnly = element.isReadOnly,
                        style = style,
                        dialogQuestion = element.dialogQuestion,
                    )
                )
            }

            XyElementClientType.SVG_TEXT -> {
                val p = element.alPoint.first()

                val x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                val y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()

                val textColor = if (element.textColor == 0) {
                    COLOR_TRANSPARENT
                } else {
                    getColorFromInt(element.textColor)
                }

                val hAnchor = when (element.anchorX) {
                    XyElement.Anchor.LT -> "start"
                    XyElement.Anchor.RB -> "end"
                    else -> "middle"
                }

                val vAlign = when (element.anchorY) {
                    XyElement.Anchor.LT -> "hanging"
                    XyElement.Anchor.RB -> "text-bottom"
                    else -> "central"
                }

                //--- сбор неизменяемых val-переменных для передачи в лямбду
                val fontSize = COMMON_FONT_SIZE * element.fontSize / iCoreAppContainer.BASE_FONT_SIZE
                val fontWeight = if (element.isFontBold) {
                    "bold"
                } else {
                    "normal"
                }
                val isPointerCursor = !element.isReadOnly
                val style: StyleScope.() -> Unit = {
                    fontSize(fontSize)
                    fontWeight(fontWeight)
                    if (isPointerCursor) {
                        cursor("pointer")
                    }
                }

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.SVG_TEXT,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = x,
                        y = y,
                        hAnchor = hAnchor,
                        vAnchor = vAlign,
                        text = element.text,
                        stroke = textColor,
                        tooltip = element.toolTipText,
                        isReadOnly = element.isReadOnly,
                        isVisible = true,
                        style = style,
                        dialogQuestion = element.dialogQuestion,
                    )
                )
            }

            XyElementClientType.HTML_TEXT -> {
                val p = element.alPoint.first()

                var x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                var y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()

                val limitWidth = (element.limitWidth / xyViewCoord.scale * scaleKoef).roundToInt()
                val limitHeight = (element.limitHeight / xyViewCoord.scale * scaleKoef).roundToInt()

                val textColor = if (element.textColor == 0) {
                    COLOR_TRANSPARENT
                } else {
                    getColorFromInt(element.textColor)
                }

                //--- сбор неизменяемых val-переменных для передачи в лямбду
                val textWidth = if (limitWidth > 0) {
                    val dx = if (element.anchorX == XyElement.Anchor.LT) {
                        0
                    } else if (element.anchorX == XyElement.Anchor.CC) {
                        limitWidth / 2
                    } else {
                        /*anchorX == ANCHOR_RB ?*/
                        limitWidth
                    }
                    x -= dx
                    //--- чтобы избежать некрасивого перекрытия прямоугольников
                    limitWidth
                } else {
                    null
                }
                val textHeight = if (limitHeight > 0) {
                    val dy = if (element.anchorY == XyElement.Anchor.LT) {
                        0
                    } else if (element.anchorY == XyElement.Anchor.CC) {
                        limitHeight / 2
                    } else {
                        /*anchorY == ANCHOR_RB ?*/
                        limitHeight
                    }
                    y -= dy
                    limitHeight
                } else {
                    null
                }
                val textBorder = if (element.drawColor != 0) {
                    BorderData(
                        color = drawColor,
                        width = (1 * scaleKoef).px,
                        radius = (2 * scaleKoef).px,
                    )
                } else {
                    null
                }
                val textBack = if (element.fillColor != 0) fillColor else null
                val textOverflow = if (limitWidth > 0 || limitHeight > 0) "hidden" else null
                val fontSize = COMMON_FONT_SIZE * element.fontSize / iCoreAppContainer.BASE_FONT_SIZE
                val fontWeight = if (element.isFontBold) {
                    "bold"
                } else {
                    "normal"
                }
                val textAlign = when (element.alignX) {
                    XyElement.Align.LT -> "left"
                    XyElement.Align.RB -> "right"
                    else -> "center"
                }
                val verticalAlign = when (element.alignY) {
                    XyElement.Align.LT -> "text-top"
                    XyElement.Align.RB -> "text-bottom"
                    else -> "baseline"
                }
                val isPointerCursor = !element.isReadOnly

                val style: StyleScope.() -> Unit = {
                    position(Position.Absolute)
                    setPaddings(arrStyleXyTextPadding)
                    color(textColor)
                    textWidth?.let {
                        width(textWidth.px)
                    }
                    textHeight?.let {
                        height(textHeight.px)
                    }
                    textBorder?.let {
                        setBorder(textBorder)
                    }
                    textBack?.let {
                        backgroundColor(textBack)
                    }
                    textOverflow?.let {
                        overflow(textOverflow)
                    }

                    fontSize(fontSize)
                    fontWeight(fontWeight)
                    textAlign(textAlign)
                    verticalAlign(verticalAlign)

                    if (isPointerCursor) {
                        cursor("pointer")
                    }
                }

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.HTML_TEXT,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = x,
                        y = y,
                        text = element.text,
                        tooltip = element.toolTipText,
                        isReadOnly = element.isReadOnly,
                        isVisible = true,       //!!! д.б. false и потом отдельно включаться в зависимости от заэкранности положения
                        pos = {
                            left(x.px)
                            top(y.px)
                        },
                        style = style,
                        dialogQuestion = element.dialogQuestion,
                    )
                )
            }

            XyElementClientType.TRACE -> {
                for (i in 0 until element.alPoint.size - 1) {
                    val p1 = element.alPoint[i]
                    val p2 = element.alPoint[i + 1]

                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.LINE,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x1 = ((p1.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            y1 = ((p1.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            x2 = ((p2.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            y2 = ((p2.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt(),
                            stroke = getColorFromInt(element.alDrawColor[i]),
                            strokeWidth = max(1.0, lineWidth * scaleKoef).roundToInt(),
                            tooltip = element.alToolTip[i],
                            isReadOnly = element.isReadOnly,
                        )
                    )
                }
            }

            XyElementClientType.ZONE -> {
                val points = mutableListOf<Int>()
                val alPoint = mutableListOf<XyPoint>()
                element.alPoint.forEach {
                    val x = ((it.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                    val y = ((it.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()

                    points += x
                    points += y
                    alPoint.add(XyPoint(x, y))
                }
                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.POLYGON,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        arrPoints = mutableStateOf(points.toTypedArray()),
                        stroke = COLOR_XY_POLYGON_ACTUAL,
                        fill = COLOR_XY_POLYGON_ACTUAL,
                        strokeWidth = (2 * scaleKoef).roundToInt(),
                        strokeDash = "${scaleKoef * 2 /*lineWidth*/ * 2},${scaleKoef * 2 /*lineWidth*/ * 2}",
                        tooltip = element.toolTipText,
                        isReadOnly = element.isReadOnly,
                        alPoint = alPoint,
                        isEditablePoint = !element.isReadOnly,
                        isMoveable = !element.isReadOnly,
                    )
                )
            }
        }
    }

    protected fun getXyEmptyElementData(elementConfig: XyElementConfig): XyElementData? {
        var result: XyElementData? = null

        when (elementConfig.clientType) {
            XyElementClientType.BITMAP -> {
            }

            XyElementClientType.ICON -> {
            }

            XyElementClientType.MARKER -> {
            }

            XyElementClientType.POLY -> {
            }

            XyElementClientType.SVG_TEXT -> {
            }

            XyElementClientType.HTML_TEXT -> {
            }

            XyElementClientType.TRACE -> {
            }

            XyElementClientType.ZONE -> {
                result = XyElementData(
                    type = XyElementDataType.POLYGON,
                    elementId = -getRandomInt(),
                    objectId = 0,
                    arrPoints = mutableStateOf(emptyArray()),
                    stroke = COLOR_XY_POLYGON_ACTUAL,
                    fill = COLOR_XY_POLYGON_ACTUAL,
                    strokeWidth = (2 * scaleKoef).roundToInt(),
                    strokeDash = "${scaleKoef * 2 /*lineWidth*/ * 2},${scaleKoef * 2 /*lineWidth*/ * 2}",
                    tooltip = "",
                    isReadOnly = false,
                    alPoint = mutableListOf(),
                    isEditablePoint = true,
                    isMoveable = true,
                    //--- при добавлении сразу выбранный
                    isSelected = true,
                    //--- данные для добавления на сервере
                    typeName = "mms_zone",
                    alAddInfo = listOf(
                        Pair("zone_name") {
                            (window.prompt("Введите наименование геозоны")?.trim() ?: "").ifEmpty { "-" }
                        },
                        Pair("zone_descr") {
                            (window.prompt("Введите описание геозоны")?.trim() ?: "").ifEmpty { "-" }
                        }
                    )
                )
            }
        }

        return result
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun onXyMouseOver(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
        if (!xyElement.tooltip.isNullOrBlank()) {
            val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(syntheticMouseEvent.clientX, syntheticMouseEvent.clientY)

            xyTooltipVisible.value = true
            xyTooltipText.value = xyElement.tooltip
            xyTooltipLeft.value = tooltipX.px
            xyTooltipTop.value = tooltipY.px
            xyTooltipOffTime = Date.now() + 3000
        } else {
            xyTooltipVisible.value = false
        }
    }

    private fun onXyMouseOut() {
        //--- через 3 сек выключить тултип, если не было других активаций тултипов
        //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
        //--- причём после ухода с графика других mouseleave не вызывается.
        setGraphicAndXyTooltipOffTimeout(xyTooltipOffTime, xyTooltipVisible)
    }

    protected abstract fun onXyMousePressed(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double)
    protected abstract fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double)
    protected abstract fun onXyMouseReleased(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean)
    protected abstract fun onXyMouseWheel(syntheticWheelEvent: SyntheticWheelEvent)
    protected abstract fun onXyTextPressed(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData)
    protected abstract fun onXyTextPressed(syntheticTouchEvent: SyntheticTouchEvent, xyElement: XyElementData)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setInterval(sec: Int) {
        if (refreshHandlerId != 0) {
            window.clearInterval(refreshHandlerId)
        }

        if (sec == 0) {
            xyRefreshView(null, true)
        } else {
            refreshHandlerId = window.setInterval({
                xyRefreshView(null, false)
            }, sec * 1000)
        }

        refreshInterval.value = sec
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun setXyTextOffset() {
        val arrViewBoxBody = getXyViewBoxBody()

        for (arrLayer in alXyElement) {
            for (xyElement in arrLayer) {
                if (xyElement.type == XyElementDataType.HTML_TEXT) {
                    val newX = xyElement.x - arrViewBoxBody[0]
                    val newY = xyElement.y - arrViewBoxBody[1]

                    xyElement.isVisible = newX >= 0 && newY >= 0 && newX < arrViewBoxBody[2] && newY /*- xyOffsY*/ < arrViewBoxBody[3]

                    xyElement.pos = {
                        left((xySvgLeft + newX).px)
                        top((xySvgTop + newY).px)
                    }
                }
            }
        }
    }

    protected fun xyDeselectAll() {
        alXyElement.forEach { alElement ->
            alElement.forEach { xyElement ->
                xyElement.isSelected = false
            }
        }
    }

    protected fun getXyClickRect(mouseX: Int, mouseY: Int): XyRect = XyRect(mouseX - MIN_USER_RECT_SIZE / 2, mouseY - MIN_USER_RECT_SIZE / 2, MIN_USER_RECT_SIZE, MIN_USER_RECT_SIZE)

    fun getXyElementList(rect: XyRect, isCollectEditableOnly: Boolean): List<XyElementData> =
        alXyElement.flatten().filter { xyElement ->
            isCollectEditableOnly.xor(xyElement.isReadOnly) && xyElement.isIntersects(rect)
        }.asReversed()

}

/*

//--- Возвращает список выбранных элементов
//fun getXySelectedElementList( that: dynamic ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            if( xyElement.itSelected )
//                alResult.add( xyElement )
//        }
//    }
//    return alResult.asReversed()
//}

//fun getXyElementList( that: dynamic, x: Int, y: Int ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            //--- небольшой хак: список элементов нужен только для интерактива, поэтмоу прежде чем тратить время на проверки геометрии - проверяем, а надо ли вообще проверять
//            if( !xyElement.itReadOnly && xyElement.isContains( x, y ))
//                alResult.add( xyElement )
//        }
//    }
//
//    return alResult.asReversed()
//}

 */
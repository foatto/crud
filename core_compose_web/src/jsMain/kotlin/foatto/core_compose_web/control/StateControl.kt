package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.STATE_ELEMENT_MOVE_DESCR
import foatto.core.app.STATE_ELEMENT_MOVE_ENABLED
import foatto.core.app.STATE_ELEMENT_MOVE_ID
import foatto.core.app.STATE_ELEMENT_MOVE_X
import foatto.core.app.STATE_ELEMENT_MOVE_Y
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core_compose_web.*
import foatto.core_compose_web.control.composable.getRefreshSubToolbar
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose_web.control.composable.getXyServerActionSubToolbar
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.control.model.XyServerActionButtonData
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.*
import org.jetbrains.compose.web.ExperimentalComposeWebSvgApi
import org.jetbrains.compose.web.attributes.stroke
import org.jetbrains.compose.web.attributes.strokeWidth
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.ElementScope
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.jetbrains.compose.web.svg.Rect
import org.jetbrains.compose.web.svg.fill
import org.w3c.dom.svg.SVGElement
import kotlin.math.max
import kotlin.math.roundToInt

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private val styleElementListTop = 10.5.cssRem

private fun StyleScope.setElementListMaxWidth() {
    if (styleIsNarrowScreen) {
        maxWidth(85.percent)
    } else {
        maxWidth(30.cssRem)
    }
}

private val styleElementListItemPadding = 0.4.cssRem
private val styleElementListCloseButtonWidth = 99.percent

private val COLOR_STATE_LINE: CSSColorValue = hsl(180, 100, 100)
private val COLOR_STATE_LINE_WIDTH = max(1.0, scaleKoef).roundToInt()
private val COLOR_STATE_FILL: CSSColorValue = hsl(180, 100, 50)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val STATE_START_EXPAND_KOEF = 0.0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class StateControl(
    root: Root,
    appControl: AppControl,
    xyResponse: XyResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, xyResponse, tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        containerPrefix = "state"
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val isElementMoveEnabled = mutableStateOf(false)
    private val isShowElementList = mutableStateOf(false)
    private val alMoveableElementData = mutableStateListOf<ElementMoveData>()

    private val alXyServerButton = mutableStateListOf<XyServerActionButtonData>()

    private val isShowStateAlert = mutableStateOf(false)
    private val stateAlertMessage = mutableStateOf("")

    private val isMoveRectVisible = mutableStateOf(false)
    private val moveRectX = mutableStateOf(0)
    private val moveRectY = mutableStateOf(0)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var moveElementId = 0

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- State Header
            getGraphicAndXyHeader(containerPrefix)

            //--- State Toolbar
            getGraphicAndXyToolbar(containerPrefix) {
                getToolBarSpan {
                    // empty, for aligning refresh buttons to right side
                }
                getXyServerActionSubToolbar(alXyServerButton) { url ->
                    invokeServerButton(url)
                }
                getToolBarSpan {
                    if (isElementMoveEnabled.value) {
                        getToolBarIconButton(
                            isEnabled = refreshInterval.value == 0,
                            src = "/web/images/ic_touch_app_${getStyleIconNameSuffix()}.png",
                            title = "Перемещение датчиков по объекту"
                        ) {
                            isShowElementList.value = !isShowElementList.value
                        }
                        if (refreshInterval.value == 0 && isShowElementList.value) {
                            Div(
                                attrs = {
                                    style {
                                        zIndex(Z_INDEX_STATE_ELEMENT_LIST)     // popup menu must be above than table headers
                                        position(Position.Absolute)
                                        top(styleElementListTop)
                                        width(auto)
                                        setElementListMaxWidth()
                                        backgroundColor(getColorPopupMenuBack())
                                        color(colorMenuTextDefault)
                                        setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                                        fontSize(arrStyleMenuFontSize[0])
                                        padding(styleElementListItemPadding)
                                        overflow("auto")
                                        cursor("pointer")
                                    }
                                }
                            ) {
                                for (data in alMoveableElementData) {
                                    Button(
                                        attrs = {
                                            style {
                                                backgroundColor(getColorButtonBack())
                                                setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                                fontSize(styleCommonButtonFontSize)
                                                padding(styleElementListItemPadding)
                                                margin(styleFileNameButtonMargin)
                                                cursor("pointer")
                                            }
                                            title(data.descr)
                                            onClick {
                                                isShowElementList.value = false
                                                moveElementId = data.id
                                                isMoveRectVisible.value = true
                                                moveRectX.value = ((data.x - xyViewCoord.x1) / xyViewCoord.scale * scaleKoef).roundToInt()
                                                moveRectY.value = ((data.y - xyViewCoord.y1) / xyViewCoord.scale * scaleKoef).roundToInt()
                                            }
                                        }
                                    ) {
                                        Text(data.descr)
                                    }
                                    Br()
                                }
                                Br()
                                Br()
                                Button(
                                    attrs = {
                                        style {
                                            backgroundColor(getColorButtonBack())
                                            setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                            fontSize(styleCommonButtonFontSize)
                                            width(styleElementListCloseButtonWidth)
                                            padding(styleFileNameButtonPadding)
                                            margin(styleFileNameButtonMargin)
                                            cursor("pointer")
                                        }
                                        title("Закрыть")
                                        onClick {
                                            isShowElementList.value = false
                                        }
                                    }
                                ) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }
                getRefreshSubToolbar(refreshInterval) { interval ->
                    setInterval(interval)
                }
            }

            getXyElementTemplate(true)

            getStateAlertBody()
        }
    }

    @OptIn(ExperimentalComposeWebSvgApi::class)
    @Composable
    override fun addSpecifigSvg(svg: ElementScope<SVGElement>) {
        super.addSpecifigSvg(svg)

        svg.apply {
            if (isMoveRectVisible.value) {
                Rect(
                    x = moveRectX.value,
                    y = moveRectY.value,
                    width = 64,
                    height = 64,
                    attrs = {
                        stroke(COLOR_STATE_LINE)
                        strokeWidth(COLOR_STATE_LINE_WIDTH)
                        fill(COLOR_STATE_FILL.toString())
                        style {
                            opacity(0.50)
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun getStateAlertBody() {
        if (isShowStateAlert.value) {
            Div(
                attrs = {
                    style {
                        position(Position.Fixed)
                        top(20.percent)
                        left(0.px)
                        width(100.percent)
                        bottom(0.px)
                        zIndex(Z_INDEX_STATE_ALERT)
                        backgroundColor(colorDialogBack)
                        display(DisplayStyle.Grid)
                        gridTemplateRows("1fr auto 1fr")
                        gridTemplateColumns("1fr auto 1fr")
                    }
                }
            ) {
                Div(
                    attrs = {
                        style {
                            gridArea("1", "2", "2", "3")
                        }
                    }
                ) {
                    Br()
                }
                Div(
                    attrs = {
                        style {
                            gridArea("2", "2", "3", "3")
                            padding(styleDialogCellPadding)
                            setBorder(color = getColorDialogBorder(), radius = styleFormBorderRadius)
                            backgroundColor(getColorDialogBackCenter())
                            display(DisplayStyle.Flex)
                            flexDirection(FlexDirection.Column)
                            alignItems(AlignItems.Center)
                        }
                    }
                ) {
                    Div(
                        attrs = {
                            style {
                                alignSelf(AlignSelf.Center)
                                fontSize(styleDialogTextFontSize)
                                fontWeight("bold")
                                color(COLOR_MAIN_BACK_0)
                            }
                        }
                    ) {
                        Text(stateAlertMessage.value)
                    }
                }
                Div(
                    attrs = {
                        style {
                            gridArea("3", "2", "4", "3")
                        }
                    }
                ) {
                    Br()
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        XyServerActionButtonData.readXyServerActionButton(xyResponse.alServerActionButton, alXyServerButton)

        doXyMounted(
            startExpandKoef = STATE_START_EXPAND_KOEF,
            isCentered = true,
            curScale = 1,
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun xyRefreshView(aView: XyViewCoord?, withWait: Boolean) {
        doStateRefreshView(
            aView = aView,
            withWait = withWait,
            doAdditionalWork = { xyActionResponse: XyActionResponse ->
                xyActionResponse.hmParam[STATE_ALERT_MESSAGE]?.let { value ->
                    stateAlertMessage.value = value
                    isShowStateAlert.value = true
                }

                isElementMoveEnabled.value = (xyActionResponse.hmParam[STATE_ELEMENT_MOVE_ENABLED]?.toBooleanStrictOrNull() == true)

                val sElementMoveId = xyActionResponse.hmParam[STATE_ELEMENT_MOVE_ID]
                val sElementMoveDescr = xyActionResponse.hmParam[STATE_ELEMENT_MOVE_DESCR]
                val sElementMoveX = xyActionResponse.hmParam[STATE_ELEMENT_MOVE_X]
                val sElementMoveY = xyActionResponse.hmParam[STATE_ELEMENT_MOVE_Y]

                if (!sElementMoveId.isNullOrBlank() && !sElementMoveDescr.isNullOrEmpty() &&
                    !sElementMoveX.isNullOrBlank() && !sElementMoveY.isNullOrEmpty()
                ) {

                    val alElementMoveId = sElementMoveId.split('\n').filter { it.isNotBlank() }
                    val alElementMoveDescr = sElementMoveDescr.split('\n').filter { it.isNotBlank() }
                    val alElementMoveX = sElementMoveX.split('\n').filter { it.isNotBlank() }
                    val alElementMoveY = sElementMoveY.split('\n').filter { it.isNotBlank() }

                    alMoveableElementData.clear()
                    alElementMoveId.forEachIndexed { index, id ->
                        alMoveableElementData.add(
                            ElementMoveData(
                                id = id.toInt(),
                                descr = alElementMoveDescr[index],
                                x = alElementMoveX[index].toInt(),
                                y = alElementMoveY[index].toInt(),
                            )
                        )
                    }
                }
            },
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun doStateRefreshView(
        aView: XyViewCoord?,
        withWait: Boolean,
        doAdditionalWork: (xyActionResponse: XyActionResponse) -> Unit = { _: XyActionResponse -> },
    ) {
        aView?.let {
            //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
            aView.scale = calcXyScale(aView.x1, aView.y1, aView.x2, aView.y2)
            xyViewCoord = aView
        }

        getXyElements(
            mapBitmapTypeName = "",
            withWait = withWait,
            doAdditionalWork = doAdditionalWork,
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun onXyMousePressed(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {}

    override fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        if (isNeedOffsetCompensation) {
            mouseX -= xySvgLeft
            mouseY -= xySvgTop
        }

        if (isMoveRectVisible.value) {
            moveRectX.value = mouseX
            moveRectY.value = mouseY
        }
    }

    override fun onXyMouseReleased(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean) {
        var mouseX = aMouseX.toInt()
        var mouseY = aMouseY.toInt()

        if (isNeedOffsetCompensation) {
            mouseX -= xySvgLeft
            mouseY -= xySvgTop
        }

        if (isMoveRectVisible.value) {
            val xyActionRequest = XyActionRequest(
                documentTypeName = xyResponse.documentConfig.name,
                action = XyAction.MOVE_ELEMENTS,
                startParamId = xyResponse.startParamId,
                elementId = moveElementId,
                dx = xyViewCoord.x1 + (mouseX * xyViewCoord.scale / scaleKoef).roundToInt(),
                dy = xyViewCoord.y1 + (mouseY * xyViewCoord.scale / scaleKoef).roundToInt(),
            )

            root.setWait(true)
            invokeXy(xyActionRequest) {
                root.setWait(false)
                xyRefreshView(null, true)
            }

            isMoveRectVisible.value = false
        }
    }

    override fun onXyMouseWheel(syntheticWheelEvent: SyntheticWheelEvent) {}

    override fun onXyTextPressed(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
        doStateTextPressed(xyElement)
    }

    override fun onXyTextPressed(syntheticTouchEvent: SyntheticTouchEvent, xyElement: XyElementData) {
        doStateTextPressed(xyElement)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun invokeServerButton(url: String) {
        root.openTab(url)
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun doStateTextPressed(xyElement: XyElementData) {
        root.dialogActionFun = {
            val xyActionRequest = XyActionRequest(
                documentTypeName = xyResponse.documentConfig.name,
                action = XyAction.CLICK_ELEMENT,
                startParamId = xyResponse.startParamId,
                elementId = xyElement.elementId,
                objectId = xyElement.objectId
            )

            root.setWait(true)
            invokeXy(
                xyActionRequest
            ) {
                root.setWait(false)

                root.dialogActionFun = {}
                root.dialogQuestion.value = "Действие выполнено!"
                root.showDialogCancel.value = false
                root.showDialog.value = true
            }
        }
        root.dialogQuestion.value = xyElement.dialogQuestion!!
        root.showDialogCancel.value = true
        root.showDialog.value = true
    }

}

private class ElementMoveData(
    val id: Int,
    val descr: String,
    val x: Int,
    val y: Int,
)
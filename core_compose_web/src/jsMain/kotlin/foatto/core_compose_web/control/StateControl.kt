package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.colorDialogBack
import foatto.core_compose_web.control.composable.getRefreshSubToolbar
import foatto.core_compose_web.control.composable.getToolBarSpan
import foatto.core_compose_web.control.composable.getXyServerActionSubToolbar
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.control.model.XyServerActionButtonData
import foatto.core_compose_web.getColorDialogBackCenter
import foatto.core_compose_web.getColorDialogBorder
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.COLOR_MAIN_BACK_0
import foatto.core_compose_web.style.Z_INDEX_STATE_ALERT
import foatto.core_compose_web.style.setBorder
import foatto.core_compose_web.style.styleFormBorderRadius
import foatto.core_compose_web.styleDialogCellPadding
import foatto.core_compose_web.styleDialogTextFontSize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.events.SyntheticTouchEvent

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

    private val alXyServerButton = mutableStateListOf<XyServerActionButtonData>()

    private val showStateAlert = mutableStateOf(false)
    private val stateAlertMessage = mutableStateOf("")

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
                    // empty, for aligning refresh buttons to right side
                }
                getRefreshSubToolbar(refreshInterval) { interval ->
                    setInterval(interval)
                }
            }

            getXyElementTemplate(true)

            getStateAlertBody()
        }
    }

    @Composable
    fun getStateAlertBody() {
        if (showStateAlert.value) {
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
        XyServerActionButtonData.readXyServerActionButton(xyResponse.arrServerActionButton, alXyServerButton)

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
                xyActionResponse.arrParams?.firstOrNull { pair ->
                    pair.first == STATE_ALERT_MESSAGE
                }?.let { pair ->
                    stateAlertMessage.value = pair.second
                    showStateAlert.value = true
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
    override fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {}
    override fun onXyMouseReleased(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean) {}
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

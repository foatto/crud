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
import foatto.core.link.XyServerActionButton
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.colorDialogBack
import foatto.core_compose_web.control.TableControl.Companion.hmTableIcon
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.control.model.XyServerActionButtonData
import foatto.core_compose_web.getColorDialogBackCenter
import foatto.core_compose_web.getColorDialogBorder
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.*
import foatto.core_compose_web.styleDialogCellPadding
import foatto.core_compose_web.styleDialogTextFontSize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.events.SyntheticTouchEvent

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private const val START_EXPAND_KOEF = 0.0

private const val STATE_PREFIX = "state"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class StateControl(
    root: Root,
    appControl: AppControl,
    xyResponse: XyResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, xyResponse, tabId) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alXyServerButton = mutableStateListOf<XyServerActionButtonData>()

    private val showStateAlert = mutableStateOf(false)
    private val stateAlertMessage = mutableStateOf("")

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- State Header
            getGraphicAndXyHeader(STATE_PREFIX)

            //--- State Toolbar
            getGraphicAndXyToolbar(STATE_PREFIX) {
                getToolBarSpan {
                    // empty, for aligning refresh buttons to right side
                }
                getToolBarSpan {
                    for (serverButton in alXyServerButton) {
                        if (!styleIsNarrowScreen || !serverButton.isForWideScreenOnly) {
                            Button(
                                attrs = {
                                    style {
                                        backgroundColor(getColorToolbarButtonBack())
                                        setBorder(getStyleToolbarButtonBorder())
                                        padding(
                                            if (serverButton.icon.isNotEmpty()) {
                                                styleIconButtonPadding
                                            } else {
                                                getStyleStateServerButtonTextPadding()
                                            }
                                        )
                                        setMargins(arrStyleCommonMargin)
                                        fontSize(styleCommonButtonFontSize)
                                        fontWeight(styleStateServerButtonTextFontWeight)
                                        cursor("pointer")
                                    }
                                    title(serverButton.tooltip)
                                    onClick {
                                        invokeServerButton(serverButton.url)
                                    }
                                }
                            ) {
                                if (serverButton.icon.isNotEmpty()) {
                                    Img(src = serverButton.icon)
                                } else {
                                    Text(serverButton.caption)
                                }
                            }
                        }
                    }
                    getRefreshSubToolbar()
                }
            }

            getXyElementTemplate(true)

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
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        readXyServerActionButton(xyResponse.arrServerActionButton)

        doXyMounted(
            elementPrefix = STATE_PREFIX,
            startExpandKoef = START_EXPAND_KOEF,
            isCentered = true,
            curScale = 1,
        )

//        statePostMountFun()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun readXyServerActionButton(arrServerActionButton: Array<XyServerActionButton>) {
        var serverButtonId = 0
        alXyServerButton.clear()
        for (sab in arrServerActionButton) {
            val icon = hmTableIcon[sab.icon] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val caption = if (sab.icon.isNotBlank() && icon.isBlank()) {
                sab.icon
            } else {
                sab.caption //!!!.replace("\n", "<br>")
            }
            alXyServerButton.add(
                XyServerActionButtonData(
                    id = serverButtonId++,
                    caption = caption,
                    tooltip = sab.tooltip,
                    icon = icon,
                    url = sab.url,
                    isForWideScreenOnly = sab.isForWideScreenOnly,
                )
            )
        }
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

    private fun doStateRefreshView(
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

/*


var statePostMountFun: (that: dynamic) -> Unit = { _: dynamic -> }

 */
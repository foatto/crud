package foatto.core_compose_web.control

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.web.events.SyntheticMouseEvent
import androidx.compose.web.events.SyntheticWheelEvent
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core_compose_web.AppControl
import foatto.core_compose_web.Root
import foatto.core_compose_web.colorDialogBack
import foatto.core_compose_web.control.model.XyElementData
import foatto.core_compose_web.getColorDialogBackCenter
import foatto.core_compose_web.getColorDialogBorder
import foatto.core_compose_web.link.invokeXy
import foatto.core_compose_web.style.*
import foatto.core_compose_web.styleDialogCellPadding
import foatto.core_compose_web.styleDialogTextFontSize
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.events.SyntheticTouchEvent
import org.w3c.dom.Element

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

//    "arrXyServerButton" to arrayOf<XyServerActionButton_>(),

    private val showStateAlert = mutableStateOf(false)
    private val stateAlertMessage = mutableStateOf("")

//--------------------------------------------------------------------------------------------------------------------------------------------------------------


//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Composable
    override fun getBody() {
        getMainDiv {
            //--- State Header
            getGraphicAndXyHeader(STATE_PREFIX)

            //--- State Toolbar
            getGraphicAndXyToolbar(STATE_PREFIX) {
                getToolBarSpan {
//                        <button v-for="serverButton in arrXyServerButton"
//                                v-show="!${styleIsNarrowScreen} || !serverButton.isForWideScreenOnly"
//                                v-bind:key="'sb'+serverButton.id"
//                                v-on:click="invokeServerButton( serverButton.url )"
//                                v-bind:style="[
//                                    style_icon_button,
//                                    { 'padding' : ( serverButton.icon ? '${styleIconButtonPadding()}' : '${styleStateServerButtonTextPadding()}' ) },
//                                    { 'font-weight' : '${styleStateServerButtonTextFontWeight}' }
//                                ]"
//                                v-bind:title="serverButton.tooltip"
//                        >
//                            <img v-if="serverButton.icon" v-bind:src="serverButton.icon">
//                            <span v-else v-html="serverButton.caption">
//                            </span>
//                        </button>
//!!! сравнить со стилями в getToolBarIconButton !!!
//    "style_icon_button" to json(
//        "background" to colorToolbarButtonBack(),
//        "border" to styleToolbarButtonBorder(),
//        "border-radius" to styleButtonBorderRadius,
//        "font-size" to styleCommonButtonFontSize(),
//        "padding" to styleIconButtonPadding(),
//        "margin" to styleCommonMargin(),
//        "cursor" to "pointer"
//    ),
                }
                getToolBarSpan {
                    listOf(0, 1, 5, 10, 30).forEach { interval ->
                        Img(
                            src = "/web/images/ic_replay_${if (interval == 0) "" else "${interval}_"}black_48dp.png",
                            attrs = {
                                style {
                                    backgroundColor(getColorRefreshButtonBack())
                                    setBorder(getStyleToolbarButtonBorder())
                                    fontSize(styleCommonButtonFontSize)
                                    padding(styleIconButtonPadding)
                                    setMargins(arrStyleCommonMargin)
                                    cursor("pointer")
                                }
                                title(
                                    when (interval) {
                                        0 -> "Обновить сейчас"
                                        1 -> "Обновлять каждую секунду"
                                        else -> "Обновлять каждые $interval сек"
                                    }
                                )
                                onClick {
//                                    setInterval(interval)
                                }
                            }
                        )
                    }
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
//        readXyServerActionButton(that(), xyResponse.arrServerActionButton)

        doXyMounted(
            elementPrefix = STATE_PREFIX,
            startExpandKoef = START_EXPAND_KOEF,
            isCentered = true,
            curScale = 1,
        )

//        statePostMountFun()
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun xyRefreshView(aView: XyViewCoord?, withWait: Boolean) {
        doStateRefreshView(
            arrAddElements = emptyArray(),
            aView = aView,
            withWait = withWait,
//            doAdditionalWork = { aThat: dynamic, xyActionResponse: XyActionResponse ->
//                xyActionResponse.arrParams?.firstOrNull { pair ->
//                    pair.first == STATE_ALERT_MESSAGE
//                }?.let { pair ->
//                    aThat.showStateAlert = true
//                    aThat.stateAlertMessage = pair.second
//                }
//            },
        )
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun doStateRefreshView(
        arrAddElements: Array<Element>,
        aView: XyViewCoord?,
        withWait: Boolean,
//        doAdditionalWork: (aThat: dynamic, xyActionResponse: XyActionResponse) -> Unit = { _: dynamic, _: XyActionResponse -> },
    ) {
//        val svgCoords = defineXySvgCoords(that, tabId, elementPrefix, arrAddElements)

        aView?.let {
            //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
            aView.scale = calcXyScale(aView.x1, aView.y1, aView.x2, aView.y2)
            xyViewCoord = aView
        }

        getXyElements(
            mapBitmapTypeName = "",
            withWait = withWait,
//            doAdditionalWork = doAdditionalWork,
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

@Suppress("UnsafeCastFromDynamic")
fun stateControl(xyResponse: XyResponse, tabId: Int) = vueComponentOptions().apply {

    this.methods = json(
        "invokeServerButton" to { url: String ->
            that().`$root`.openTab(url)
        },
        "setInterval" to { sec: Int ->
            val that = that()

            val refreshHandlerId = that.refreshHandlerId.unsafeCast<Int>()
            if (refreshHandlerId != 0) {
                window.clearInterval(refreshHandlerId)
            }

            if (sec == 0) {
                that.xyRefreshView(that, null, true)
            } else {
                that.refreshHandlerId = window.setInterval({
                    that.xyRefreshView(that, null, false)
                }, sec * 1000)
            }

            that.refreshInterval = sec
        },
        "onXyTextPressed" to { event: Event, xyElement: XyElementData ->
        },
    )

}

 */
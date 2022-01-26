package foatto.core_web

import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core.util.prepareForHTML
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.js.json

private enum class StateWorkMode {
    PAN
}

private const val startExpandKoef = 0.0

@Suppress("UnsafeCastFromDynamic")
fun stateControl(xyResponse: XyResponse, tabId: Int) = vueComponentOptions().apply {

    this.template =
        """
            <div>
                <div id="state_title_$tabId" v-bind:style="[ style_toolbar, style_header ]">
                    <span v-bind:style="style_toolbar_block">
                    </span>
                    <span v-bind:style="[style_toolbar_block, style_title]">
                        {{fullTitle}}
                    </span>
                    <span v-bind:style="style_toolbar_block">
                    </span>
                </div>
                <div id="state_toolbar_$tabId" v-bind:style="style_toolbar">
                    <span v-bind:style="style_toolbar_block">
                    </span>
                    <span v-bind:style="style_toolbar_block">
                        <img src="/web/images/ic_replay_black_48dp.png"
                             v-bind:style="style_icon_button"
                             v-on:click="setInterval(0)"
                             title="Обновить сейчас"
                        >
                        <img src="/web/images/ic_replay_5_black_48dp.png"
                             v-if="refreshInterval != 5"
                             v-bind:style="style_icon_button"
                             v-on:click="setInterval(5)"
                             title="Обновлять каждые 5 сек"
                        >
                        <img src="/web/images/ic_replay_10_black_48dp.png"
                             v-if="refreshInterval != 10"
                             v-bind:style="style_icon_button"
                             v-on:click="setInterval(10)"
                             title="Обновлять каждые 10 сек"
                        >
                        <img src="/web/images/ic_replay_30_black_48dp.png"
                             v-if="refreshInterval != 30"
                             v-bind:style="style_icon_button"
                             v-on:click="setInterval(30)"
                             title="Обновлять каждые 30 сек"
                        >
                    </span>
                </div>

        """ +

            getXyElementTemplate(tabId, "") +

            getStateAlertTemplate() +

            """
            </div>
        """

    this.methods = json(
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
        "xyRefreshView" to { aThat: dynamic, aView: XyViewCoord?, withWait: Boolean ->
            val that = aThat ?: that()

            doStateRefreshView(
                that = that,
                xyResponse = xyResponse,
                tabId = tabId,
                elementPrefix = "state",
                arrAddElements = emptyArray(),
                aView = aView,
                withWait = withWait,
                doAdditionalWork = { aThat: dynamic, xyActionResponse: XyActionResponse ->
                    xyActionResponse.arrParams?.firstOrNull { pair ->
                        pair.first == STATE_ALERT_MESSAGE
                    }?.let { pair ->
                        aThat.showStateAlert = true
                        aThat.stateAlertMessage = prepareForHTML(pair.second)
                    }
                },
            )
        },
        "onXyMouseOver" to { event: Event, xyElement: XyElementData ->
            onXyMouseOver(that(), event as MouseEvent, xyElement)
        },
        "onXyMouseOut" to {
            onXyMouseOut(that())
        },
        "onXyMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
        },
        "onXyMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
        },
        "onXyMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
        },
        "onXyMouseWheel" to { event: Event ->
        },
        "onXyTextPressed" to { event: Event, xyElement: XyElementData ->
            doStateTextPressed(that(), xyResponse, xyElement)
        },
    )

    this.mounted = {
        doXyMounted(
            that = that(),
            xyResponse = xyResponse,
            tabId = tabId,
            elementPrefix = "state",
            startExpandKoef = startExpandKoef,
            isCentered = true,
            curScale = 1,
        )
//        cursor = Cursor.HAND
    }

    this.data = {
        getStateComponentData().add(
            getXyComponentData()
        )
    }

}

fun doStateRefreshView(
    that: dynamic,
    xyResponse: XyResponse,
    tabId: Int,
    elementPrefix: String,
    arrAddElements: Array<Element>,
    aView: XyViewCoord?,
    withWait: Boolean,
    doAdditionalWork: (aThat: dynamic, xyActionResponse: XyActionResponse) -> Unit = { _: dynamic, _: XyActionResponse -> },
) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    val svgCoords = defineXySvgCoords(tabId, elementPrefix, arrAddElements)

    val newView = aView?.let {
        //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
        aView.scale = calcXyScale(scaleKoef, svgCoords.bodyWidth, svgCoords.bodyHeight, aView.x1, aView.y1, aView.x2, aView.y2)
        that.xyViewCoord = aView
        aView
    } ?: run {
        that.xyViewCoord.unsafeCast<XyViewCoord>()
    }

    getXyElements(
        that = that,
        xyResponse = xyResponse,
        scaleKoef = scaleKoef,
        newView = newView,
        mapBitmapTypeName = "",
        svgBodyLeft = svgCoords.bodyLeft,
        svgBodyTop = svgCoords.bodyTop,
        withWait = withWait,
        doAdditionalWork = doAdditionalWork,
    )
}

fun doStateTextPressed(that: dynamic, xyResponse: XyResponse, xyElement: XyElementData) {
    val curMode = that.stateCurMode.unsafeCast<StateWorkMode>()

    when (curMode) {
        StateWorkMode.PAN -> {
            dialogActionFun = { that: dynamic ->
                val xyActionRequest = XyActionRequest(
                    documentTypeName = xyResponse.documentConfig.name,
                    action = XyAction.CLICK_ELEMENT,
                    startParamId = xyResponse.startParamId,
                    elementId = xyElement.elementId,
                    objectId = xyElement.objectId
                )

                that.`$root`.setWait(true)
                invokeXy(
                    xyActionRequest,
                    {
                        that.`$root`.setWait(false)

                        dialogActionFun = { that: dynamic -> }
                        that.`$root`.dialogQuestion = "Действие выполнено!"
                        that.`$root`.showDialogCancel = false
                        that.`$root`.showDialog = true
                    }
                )
            }
            that.`$root`.dialogQuestion = xyElement.tooltip
            that.`$root`.showDialogCancel = true
            that.`$root`.showDialog = true
        }
    }
}

fun getStateAlertTemplate() =
    """
        <div v-if="showStateAlert"
             v-bind:style="style_state_alert"
        >
            <div v-bind:style="style_state_alert_top_expander">
                &nbsp;
            </div>
            <div v-bind:style="style_state_alert_cell">
                <div v-bind:style="style_state_alert_text"
                     v-html="stateAlertMessage"
                >
                </div>
            </div>
            <div v-bind:style="style_state_alert_bottom_expander">
                &nbsp;
            </div>
        </div>
    """

fun getStateComponentData() = json(
    "stateCurMode" to StateWorkMode.PAN,
    "showStateAlert" to false,
    "stateAlertMessage" to "",

    "style_state_alert" to json(
        "position" to "fixed",
        "top" to "20%",
        "left" to 0,
        "width" to "100%",
        "bottom" to 0,
        "z-index" to "2000",
        "background" to COLOR_DIALOG_BACK,
        "display" to "grid",
        "grid-template-rows" to "1fr auto 1fr",
        "grid-template-columns" to "1fr auto 1fr",
    ),
    "style_state_alert_top_expander" to json(
        "grid-area" to "1 / 2 / 2 / 3",
    ),
    "style_state_alert_cell" to json(
        "grid-area" to "2 / 2 / 3 / 3",
        "padding" to styleDialogCellPadding(),
        "border" to "1px solid $colorDialogBorder",
        "border-radius" to BORDER_RADIUS,
        "background" to colorDialogBackCenter,
        "display" to "flex",
        "flex-direction" to "column",
        "align-items" to "center",
    ),
    "style_state_alert_bottom_expander" to json(
        "grid-area" to "3 / 2 / 4 / 3",
    ),
    "style_dialog_div" to json(
        "font-size" to styleControlTextFontSize(),
        "padding" to styleDialogControlPadding()
    ),
    "style_state_alert_text" to json(
        "align-self" to "center",
        "font-size" to styleControlTextFontSize(),
        "font-weight" to "bold",
        "color" to COLOR_TEXT,
    ),
)



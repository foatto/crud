package foatto.core_web

import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
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

            getXyElementTemplate(tabId, true, "") +

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
            )
        },
        "onMouseOver" to { event: Event, xyElement: XyElementData ->
            onXyMouseOver(that(), event as MouseEvent, xyElement)
        },
        "onMouseOut" to {
            onXyMouseOut(that())
        },
        "onMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
        },
        "onMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
        },
        "onMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
        },
        "onMouseWheel" to { event: Event ->
        },
        "onTextPressed" to { event: Event, xyElement: XyElementData ->
            val that = that()

            val curMode = that().stateCurMode.unsafeCast<StateWorkMode>()

            when (curMode) {
                StateWorkMode.PAN -> {
                    if (window.confirm(xyElement.tooltip ?: "(не определено)")) {
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
                                window.alert("Действие выполнено!")
                            }
                        )
                    }
                }
            }
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
        getXyComponentData().add(
            json(
                "stateCurMode" to StateWorkMode.PAN,
            )
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
) {
    val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
    val svgCoords = defineXySvgCoords(tabId, elementPrefix, arrAddElements)

    val newView = aView?.let {
        //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
        aView.scale = calcXyScale(scaleKoef, svgCoords.bodyWidth, svgCoords.bodyHeight, aView.x1, aView.y1, aView.x2, aView.y2)
        //--- обновляем, только если изменилось (оптимизируем цепочку реактивных изменений)
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
        withWait = withWait
    )
}


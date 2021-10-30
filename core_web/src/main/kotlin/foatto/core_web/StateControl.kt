package foatto.core_web

import foatto.core.app.xy.XyAction
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.window
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
                    <img src="/web/images/ic_sync_black_48dp.png"
                         v-bind:style="style_icon_button"
                         v-on:click="refreshView( null, null )"
                         title="Обновить"
                    >
                </span>
            </div>

""" +

            getXyElementTemplate(tabId, "") +

            """
        </div>
"""

    this.methods = json(
        //--- метод может вызываться из лямбд, поэтому возможен проброс ему "истинного" this
        "refreshView" to { aThat: dynamic, aView: XyViewCoord? ->
            val that = aThat ?: that()
            val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
            val svgCoords = defineXySvgCoords("state", tabId)

            val newView =
                if (aView != null) {
                    //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
                    aView.scale = calcXyScale(scaleKoef, svgCoords.bodyWidth, svgCoords.bodyHeight, aView.x1, aView.y1, aView.x2, aView.y2)
                    //--- обновляем, только если изменилось (оптимизируем цепочку реактивных изменений)
                    that.viewCoord = aView
                    aView
                } else {
                    that.viewCoord.unsafeCast<XyViewCoord>()
                }

            getXyElements(that, xyResponse, scaleKoef, newView, "", svgCoords.bodyLeft, svgCoords.bodyTop)
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

            val curMode = that().curMode.unsafeCast<StateWorkMode>()

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

        doXyMounted(that(), xyResponse, tabId, "state", startExpandKoef, 1)

//        cursor = Cursor.HAND
    }

    this.data = {
        getXyComponentData().add(
            json(
                "curMode" to StateWorkMode.PAN,
                )
        )
    }

}


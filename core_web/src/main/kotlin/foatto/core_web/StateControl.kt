package foatto.core_web

import foatto.core.app.xy.XyViewCoord
import foatto.core.link.XyResponse
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.js.json

//    companion object {
//        //--- константы цветов
//        val COLOR_LINE = Color.CYAN       // безусловная линия/прямоугольник
//
//        const private val USER_MOUSE_RECT = 4
//    }
//
//    //--- слоёный набор xy-элементов
//    @Volatile
//    var alMapLayer = mutableListOf<MutableSet<FXElement>>()   // каждый слой - набор элементов

private const val startExpandKoef = 0.0

@Suppress("UnsafeCastFromDynamic")
fun stateControl(xyResponse: XyResponse, tabIndex: Int) = vueComponentOptions().apply {

    this.template =
        """
        <div>
            <div id="state_title_$tabIndex" v-bind:style="[ style_toolbar, style_header ]">
                <span v-bind:style="style_toolbar_block">
                </span>
                <span v-bind:style="[style_toolbar_block, style_title]">
                    {{fullTitle}}
                </span>
                <span v-bind:style="style_toolbar_block">
                </span>
            </div>
            <div id="state_toolbar_$tabIndex" v-bind:style="style_toolbar">
                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_sync_black_48dp.png"
                         v-bind:style="style_icon_button"
                         v-on:click="refreshView( null, null )"
                         title="Обновить"
                    >
                </span>
            </div>

""" +

            getXyElementTemplate(tabIndex, "") +

            """
        </div>
"""

    this.methods = json(
        //--- метод может вызываться из лямбд, поэтому возможен проброс ему "истинного" this
        "refreshView" to { aThat: dynamic, aView: XyViewCoord? ->
            val that = aThat ?: that()
            val scaleKoef = that.`$root`.scaleKoef.unsafeCast<Double>()
            val svgCoords = defineXySvgCoords("state", tabIndex)

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
//            when( xyModel.curMode ) {
//                XyModel.WorkMode.PAN -> {
//                    val alElement = getElementList( event.x, event.y )
//                    for( fxElement in alElement ) {
//                        //--- пока используем флаг read-only как флаг возможности интерактива
//                        if( !fxElement.isReadOnly ) {
//                            if( showConfirmation( "Подтверждение", null, fxElement.element.toolTipText + " ?" ) ) {
//
//                                val xyActionRequest = XyActionRequest(
//                                    documentTypeName = xyModel.documentConfig.name,
//                                    action = XyAction.CLICK_ELEMENT,
//                                    startParamID = xyModel.startParamID,
//                                    elementID = fxElement.element.elementID,
//                                    objectID = fxElement.element.objectID
//                                )
//
//                                Thread( XyActionOperator( xyActionRequest ) ).start()
//                            }
//                            //--- работаем только с одним кликабельным элементом
//                            break
//                        }
//                    }
//                }
//                else -> super.handle( event )
//            }
        },
        "onMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
        },
        "onMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
        },
        "onMouseWheel" to { event: Event ->
        }
    )

    this.mounted = {

        doXyMounted(that(), xyResponse, tabIndex, "state", startExpandKoef, 1)

//        cursor = Cursor.HAND
    }

    this.data = {
        getXyComponentData().add(
            json(

            )
        )
    }

}


package foatto.ts_web

import foatto.core.app.STATE_ALERT_MESSAGE
import foatto.core.app.graphic.GraphicViewCoord
import foatto.core.app.xy.XyActionResponse
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.CompositeResponse
import foatto.core.util.prepareForHTML
import foatto.core_web.*
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import foatto.ts_core.app.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.js.json

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = TSIndex()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//--- фирменный тёмно-синий         #004271 = hsl(205,100%,22.2%)
private const val TS_FIRM_COLOR_1_H = 205
private const val TS_FIRM_COLOR_1_S = 100
private const val TS_FIRM_COLOR_1_L = 22

//--- офигенный серый металлический #C0C0D0 = hsl(240,14.5%,78.4%)
private const val TS_FIRM_COLOR_2_H = 240
private const val TS_FIRM_COLOR_2_S = 15
private const val TS_FIRM_COLOR_2_L = 78

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val XY_SVG_HEIGHT = 400

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class TSIndex : Index() {

    init {
        colorMainBack0 = getHSL(TS_FIRM_COLOR_1_H, 50, 95)
        colorMainBack1 = getHSL(TS_FIRM_COLOR_1_H, 50, 90)
        colorMainBack2 = getHSL(TS_FIRM_COLOR_1_H, 50, 85)
        colorMainBack3 = getHSL(TS_FIRM_COLOR_1_H, 50, 80)

        colorMainBorder = { getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, TS_FIRM_COLOR_2_L) }

        colorWaitBack = getHSLA(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95, 0.75)
        colorWaitLoader0 = getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 80)
        colorWaitLoader1 = getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 85)
        colorWaitLoader2 = getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 90)
        colorWaitLoader3 = getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95)

        colorDialogBack = getHSLA(TS_FIRM_COLOR_1_H, TS_FIRM_COLOR_1_S, TS_FIRM_COLOR_1_L, 0.75)

        styleStateServerButtonTextFontWeight = "bold"

        //--- иконки внутри строк, цвет и размер менять/кастомизировать не планируется
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_CONNECT] = "/web/images/ic_portable_wifi_off_black_24dp.png"
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_WARNING] = "/web/images/ic_warning_black_24dp.png"
        hmTableIcon[ICON_NAME_TROUBLE_TYPE_ERROR] = "/web/images/ic_error_outline_black_24dp.png"

        colorGroupBack0 = { getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 90) }
        colorGroupBack1 = { getHSL(TS_FIRM_COLOR_2_H, TS_FIRM_COLOR_2_S, 95) }

        colorTableRowBack1 = { colorMainBack0 }
    }

    override fun addBeforeMounted() {
        super.addBeforeMounted()

        compositeResponseCodeControlFun = { compositeResponse: CompositeResponse, tabId: Int ->
            vueComponentOptions().apply {

                this.template = """
                    <div v-bind:style="style_composite">
                        <div id="composite_title_$tabId" v-bind:style="[ style_toolbar, style_header ]">
                            <span v-bind:style="style_toolbar_block">
                            </span>
                            <span v-bind:style="[style_toolbar_block, style_title]">
                                <span v-for="(title, index) in arrTitle"
                                     v-bind:style="{ 'font-weight': ( index == 0 ? 'bold' : 'normal' ) }"
                                >
                                    {{title}}
                                </span>
                            </span>
                            <span v-bind:style="style_toolbar_block">
                            </span>
                        </div>
                        <div id="composite_toolbar_$tabId" v-bind:style="style_toolbar">
                            <span v-bind:style="style_toolbar_block">
                            </span>
                            <span v-bind:style="style_toolbar_block">
                                <button v-for="serverButton in arrXyServerButton"
                                        v-show="!${styleIsNarrowScreen} || !serverButton.isForWideScreenOnly" 
                                        v-bind:key="'sb'+serverButton.id"
                                        v-on:click="invokeServerButton( serverButton.url )"
                                        v-bind:style="[ 
                                            style_icon_button, 
                                            { 'padding' : ( serverButton.icon ? '${styleIconButtonPadding()}' : '${styleStateServerButtonTextPadding()}' ) },
                                            { 'font-weight' : '${styleStateServerButtonTextFontWeight}' }
                                        ]"
                                        v-bind:title="serverButton.tooltip"
                                >
                                    <img v-if="serverButton.icon" v-bind:src="serverButton.icon">
                                    <span v-else v-html="serverButton.caption">
                                    </span>
                                </button>
                            </span>
                            <span v-bind:style="style_toolbar_block">
                                <img src="/web/images/ic_replay_black_48dp.png"
                                     v-bind:style="style_icon_button"
                                     v-on:click="setInterval(0)"
                                     title="Обновить сейчас"
                                >
                                <img src="/web/images/ic_replay_1_black_48dp.png"
                                     v-if="refreshInterval != 1"
                                     v-bind:style="style_icon_button"
                                     v-on:click="setInterval(1)"
                                     title="Обновлять каждую секунду"
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

                        <div v-bind:style="style_composite_grid">
                            <div v-bind:style="[ style_comp, { 'grid-area': '1 / 1 / 2 / 2' } ]">

                    """ +

                    getXyElementTemplate(tabId, false, "") +

                    """
                            </div>                                                                    
                            <div v-bind:style="[ style_comp, { 'grid-area': '2 / 1 / 3 / 2' } ]">
                    """ +

                    getGraphicElementTemplate(tabId, false) +

                    """
                            </div>                                                                    
<!--
                            <div v-bind:style="[ style_comp, { 'grid-area': '3 / 1 / 4 / 2' } ]">
                                ...
                            </div>
-->                                                                                                
                        </div>
                    """ +

                    getStateAlertTemplate() +

                    """
                    </div>
                """

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
                            that.refreshView(true)
                        } else {
                            that.refreshHandlerId = window.setInterval({
                                that.refreshView(false)
                            }, sec * 1000)
                        }

                        that.refreshInterval = sec
                    },
                    "refreshView" to { withWait: Boolean ->
                        val that = that()

                        that.xyRefreshView(that, null, withWait)
                        that.grRefreshView(that, null, withWait)
                    },
                    "xyRefreshView" to { aThat: dynamic, aView: XyViewCoord?, withWait: Boolean ->
                        val that = aThat ?: that()

                        doStateRefreshView(
                            that = that,
                            xyResponse = compositeResponse.xyResponse,
                            tabId = tabId,
                            elementPrefix = "composite",
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
                    "grRefreshView" to { aThat: dynamic, aView: GraphicViewCoord?, withWait: Boolean ->
                        val that = aThat ?: that()

                        doGraphicRefresh(
                            that = that,
                            graphicResponse = compositeResponse.graphicResponse,
                            tabId = tabId,
                            elementPrefix = "composite",
                            arrAddElements = emptyArray(),
                            aView = aView,
                            withWait = withWait,
                        )
                    },
                    "onXyMousePressed" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
                    },
                    "onXyMouseMove" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double ->
                    },
                    "onXyMouseReleased" to { isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean ->
                    },
//                    "onXyMouseWheel" to { event: Event ->
//                    },
                    "onXyMouseOver" to { event: Event, xyElement: XyElementData ->
                        onXyMouseOver(that(), event as MouseEvent, xyElement)
                    },
                    "onXyMouseOut" to {
                        onXyMouseOut(that())
                    },
                    "onXyTextPressed" to { event: Event, xyElement: XyElementData ->
                        doStateTextPressed(that(), compositeResponse.xyResponse, xyElement)
                    },
                )

                this.mounted = {
                    val that = that()

                    val xyResponse = compositeResponse.xyResponse

                    //--- once only
                    that.`$root`.setTabInfo(tabId, xyResponse.shortTitle, xyResponse.fullTitle)

                    that.arrTitle = xyResponse.fullTitle.split('\n').filter { it.isNotBlank() }.toTypedArray()

                    readXyServerActionButton(that, xyResponse.arrServerActionButton)

                    doXySpecificComponentMounted(
                        that = that,
                        xyResponse = xyResponse,
                        tabId = tabId,
                        elementPrefix = "composite",
                        startExpandKoef = 0.0,
                        isCentered = true,
                        curScale = 1,
                        svgHeight = XY_SVG_HEIGHT,
                        arrAddElements = emptyArray(),  // верхний элемент, нет элементов выше его
                    )

                    val graphicResponse = compositeResponse.graphicResponse

                    doGraphicSpecificComponentMounted(
                        that = that,
                        graphicResponse = graphicResponse,
                        tabId = tabId,
                        elementPrefix = "composite",
                        svgHeight = GRAPHIC_MIN_HEIGHT * 2,
                        arrAddElements = arrayOf(document.getElementById("xy_svg_body_$tabId")!!),
                    )

                    that.style_composite_grid = json(
                        "height" to "100%",
                        "overflow" to "auto",
                        "display" to "grid",
                        "grid-template-rows" to "${XY_SVG_HEIGHT}px ${GRAPHIC_MIN_HEIGHT * 2}px",
                        "grid-template-columns" to "repeat(1,auto)",
                    )

                    Vue.nextTick {
                        that.setInterval(10) as Unit
                    }
                }

                this.data = {
                    json(
                        "arrTitle" to arrayOf<String>(),

                        "arrXyServerButton" to arrayOf<XyServerActionButton_>(),

                        "refreshInterval" to 0,
                        "refreshHandlerId" to 0,

                        "style_composite" to json(
                            "flex-grow" to 1,
                            "flex-shrink" to 1,
                            "display" to "flex",
                            "flex-direction" to "column",
                            "height" to "100%"
                        ),
                        "style_header" to json(
                            "border-top" to if (!styleIsNarrowScreen) {
                                "none"
                            } else {
                                "1px solid ${colorMainBorder()}"
                            }
                        ),
                        "style_toolbar" to json(
                            "display" to "flex",
                            "flex-direction" to "row",
                            "flex-wrap" to "wrap",
                            "justify-content" to "space-between",
                            "align-items" to "center",        // "baseline" ?
                            "padding" to styleControlPadding(),
                            "background" to colorMainBack1
                        ),
                        "style_toolbar_block" to json(
                            "display" to "flex",
                            "flex-direction" to "row",
                            "flex-wrap" to "nowrap",
                            "justify-content" to "center",
                            "align-items" to "center"        // "baseline" ?
                        ),
                        "style_title" to json(
                            "font-size" to styleControlTitleTextFontSize(),
                            "padding" to styleControlTitlePadding(),
                            "display" to "flex",
                            "flex-direction" to "column",
                        ),
                        "style_icon_button" to json(
                            "background" to colorButtonBack(),
                            "border" to "1px solid ${colorButtonBorder()}",
                            "border-radius" to styleButtonBorderRadius,
                            "font-size" to styleCommonButtonFontSize(),
                            "padding" to styleIconButtonPadding(),
                            "margin" to styleCommonMargin(),
                            "cursor" to "pointer"
                        ),
                        "style_composite_grid" to null,
                        "style_comp" to json(
                            "justify-self" to "stretch", // horizontal align
                            "align-self" to "stretch",   // vertical align
                        ),
                    ).add(
                        getXySpecificComponentData()
                    ).add(
                        getGraphicSpecificComponentData()
                    ).add(
                        getStateComponentData()
                    )
                }
            }
        }

        styleStateServerButtonTextPadding = {
            "0.3rem 2.0rem"
        }

        statePostMountFun = { that: dynamic ->
            Vue.nextTick {
                that.setInterval(10) as Unit
            }
        }
    }
}

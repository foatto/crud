package foatto.ts_web

import foatto.core.app.graphic.GraphicViewCoord
import foatto.core.app.xy.XyViewCoord
import foatto.core.link.CustomResponse
import foatto.core_web.*
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.WheelEvent
import kotlin.js.json
import kotlin.math.roundToInt

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = TSIndex()
        index.init()
    }
}

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val FIRM_COLOR_DARK = "#004271"   // фирменный тёмно-синий
const val FIRM_COLOR_LIGHT = "#c0c0d0"  // серый металлический

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

const val XY_SVG_HEIGHT = 400

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

private class TSIndex : Index() {

    init {
        //--- constantly showed menu bar
//        styleIsHiddenMenuBar = localStorage.getItem(IS_HIDDEN_MENU_BAR)?.toBooleanStrictOrNull() ?: false
    }

    override fun addBeforeMounted() {
        super.addBeforeMounted()

        colorLogonBackAround = FIRM_COLOR_DARK
        colorLogonBackCenter = FIRM_COLOR_DARK
        colorLogonBorder = FIRM_COLOR_LIGHT
        colorLogonCheckBoxText = FIRM_COLOR_LIGHT
        colorLogonButtonBack = FIRM_COLOR_LIGHT
        colorLogonButtonBorder = FIRM_COLOR_LIGHT

        colorGroupBack0 = "#c0eeee"
        colorGroupBack1 = "#c0ffff"

        customResponseCodeControlFun = { customResponse: CustomResponse, tabId: Int ->
            vueComponentOptions().apply {

                this.template = """
                    <div v-bind:style="style_custom">
                        <div id="custom_title_$tabId" v-bind:style="[ style_toolbar, style_header ]">
                            <span v-bind:style="style_toolbar_block">
                            </span>
                            <span v-bind:style="[style_toolbar_block, style_title]">
                                {{fullTitle}}
                            </span>
                            <span v-bind:style="style_toolbar_block">
                            </span>
                        </div>
                        <div id="custom_toolbar_$tabId" v-bind:style="style_toolbar">
                            <span v-bind:style="style_toolbar_block">
                                <img src="/web/images/ic_sync_black_48dp.png"
                                     v-bind:style="style_icon_button"
                                     v-on:click="refreshView()"
                                     title="Обновить"
                                >
                            </span>
                        </div>

                        <div v-bind:style="style_custom_grid">
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
                    </div>
                """

                this.methods = json(
                    "refreshView" to {
                        val that = that()

                        that.xyRefreshView(that, null)
                        that.grRefreshView(that, null)
                    },
                    "xyRefreshView" to { aThat: dynamic, aView: XyViewCoord? ->
                        val that = aThat ?: that()

                        doStateRefreshView(
                            that = that,
                            xyResponse = customResponse.xyResponse,
                            tabId = tabId,
                            elementPrefix = "custom",
                            arrAddElements = emptyArray(),
                            aView = aView,
                        )
                    },
                    "grRefreshView" to { aThat: dynamic, aView: GraphicViewCoord? ->
                        val that = aThat ?: that()

                        doGraphicRefresh(
                            that = that,
                            graphicResponse = customResponse.graphicResponse,
                            tabId = tabId,
                            elementPrefix = "custom",
                            arrAddElements = emptyArray(),
                            aView = aView,
                        )
                    },
                )

                this.mounted = {
                    val that = that()

                    val xyResponse = customResponse.xyResponse

                    //--- once only
                    that.`$root`.setTabInfo(tabId, xyResponse.shortTitle, xyResponse.fullTitle)
                    that.fullTitle = xyResponse.fullTitle

                    doXySpecificComponentMounted(
                        that = that,
                        xyResponse = xyResponse,
                        tabId = tabId,
                        elementPrefix = "custom",
                        startExpandKoef = 0.0,
                        isCentered = true,
                        curScale = 1,
                        svgHeight = XY_SVG_HEIGHT,
                        arrAddElements = emptyArray(),  // верхний элемент, нет элементов выше его
                    )

                    val graphicResponse = customResponse.graphicResponse

                    doGraphicSpecificComponentMounted(
                        that = that,
                        graphicResponse = graphicResponse,
                        tabId = tabId,
                        elementPrefix = "custom",
                        svgHeight = GRAPHIC_MIN_HEIGHT * 2,
                        arrAddElements = arrayOf(document.getElementById("xy_svg_body_$tabId")!!),
                    )

                    that.style_custom_grid = json(
                        "height" to "100%",
                        "overflow" to "auto",
                        "display" to "grid",
                        "grid-template-rows" to "${XY_SVG_HEIGHT}px ${GRAPHIC_MIN_HEIGHT * 2}px",
                        "grid-template-columns" to "repeat(1,auto)",
                    )
                }

                this.data = {
                    json(
                        "fullTitle" to "",

                        "style_custom" to json(
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
                                "1px solid $COLOR_BUTTON_BORDER"
                            }
                        ),
                        "style_toolbar" to json(
                            "display" to "flex",
                            "flex-direction" to "row",
                            "flex-wrap" to "wrap",
                            "justify-content" to "space-between",
                            "align-items" to "center",        // "baseline" ?
                            "padding" to styleControlPadding(),
                            "background" to COLOR_PANEL_BACK
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
                            "padding" to styleControlTitlePadding()
                        ),
                        "style_icon_button" to json(
                            "background" to COLOR_BUTTON_BACK,
                            "border" to "1px solid $COLOR_BUTTON_BORDER",
                            "border-radius" to BORDER_RADIUS,
                            "font-size" to styleCommonButtonFontSize(),
                            "padding" to styleIconButtonPadding(),
                            "margin" to styleCommonMargin(),
                            "cursor" to "pointer"
                        ),
                        "style_custom_grid" to null,
                        "style_comp" to json(
                            "justify-self" to "stretch", // horizontal align
                            "align-self" to "stretch",   // vertical align
                        ),
                    ).add(
                        getXySpecificComponentData()
                    ).add(
                        getGraphicSpecificComponentData()
                    )
                }
            }
        }
    }
}

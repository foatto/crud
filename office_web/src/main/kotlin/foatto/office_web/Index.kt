package foatto.office_web

import foatto.core_web.external.vue.*
import foatto.core_web.*
import org.w3c.dom.HTMLSpanElement
import kotlin.browser.document
import kotlin.browser.localStorage
import kotlin.browser.window
import kotlin.js.Json
import kotlin.js.json

@Suppress("UnsafeCastFromDynamic")
fun main() {

    window.onload = {
        val index = Index()
        index.init()
    }
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

class Index {

    fun init() {

        Vue( vueComponentOptions().apply {
            this.el = "#app"

            this.template = """
                <div v-bind:style="style_top_container">

                    <div id="tab_panel" v-bind:style="style_tab_panel">
                        <component v-if="menuBar" v-bind:is="menuBar"></component>
                """ +
                if( styleIsNarrowScreen ) {
                    """
                        <select v-model="currentTabIndex"
                                v-bind:style="style_tab_combo"
                        >
                            <option v-for="(tab, index) in arrTabInfo"
                                    v-bind:value="index"
                            >
                                {{ tab.text }}
                            </option>
                        </select>
                        <img src="/web/images/ic_close_black_48dp.png" 
                             v-show="arrTabInfo.length > 0"
                             v-bind:style="style_tab_closer_button"
                             v-on:click="closeTab( currentTabIndex )"
                             title="Закрыть вкладку"
                        >
                    """
                }
                else {
                    """
                        <template v-for="(tab, index) in arrTabInfo">
                            <button v-show="tab.text"
                                    v-bind:style="currentTabIndex == index ? style_tab_current_title : style_tab_other_title"
                                    v-on:click="currentTabIndex = index"
                                    v-bind:key="'t'+tab.id">
                                {{ tab.text }}
                            </button>
                            <img src="/web/images/ic_close_black_16dp.png"
                                 width=16
                                 height=16
                                 v-show="tab.text"
                                 v-bind:style="currentTabIndex == index ? style_tab_current_closer : style_tab_other_closer"
                                 v-on:click="closeTab( index )"
                                 v-bind:key="'c'+tab.id"
                                 title="Закрыть вкладку"
                            >
                        </template>
                    """
                } +
                """
                    </div>
                    <template v-if="arrTabComp.length>0">
                        <keep-alive>
                            <component v-bind:is="arrTabComp[ currentTabIndex ].comp">
                            </component>
                        </keep-alive>
                    </template>

                    <div v-if="waitCount > 0"
                         v-bind:style="style_wait"
                    >
                        <div v-bind:style="style_wait_top_expander">
                            &nbsp;
                        </div>
                        <div v-bind:style="style_loader">
                        </div>
                        <div v-bind:style="style_wait_bottom_expander">
                            &nbsp;
                        </div>
                    </div>
                </div>
            """

            this.methods = json(
                "setMenuBar" to { menuData: Json ->
                    that().menuBar = menuData
                },
                "addTabComp" to { appParam: String ->
                    val currentTabID = that().lastTabID.unsafeCast<Int>() + 1
                    val currentTabIndex = that().currentTabIndex.unsafeCast<Int>() + 1

                    that().arrTabInfo.splice( currentTabIndex, 0, TabInfo( currentTabID, "", "" ) )
                    that().arrTabComp.splice( currentTabIndex, 0, TabComp( appControl( appParam, currentTabIndex ) ) )

                    that().currentTabIndex = currentTabIndex
                    that().lastTabID = currentTabID
                },
                "addTabInfo" to { tabIndex: Int, tabText: String, tabToolTip: String ->
                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()
                    arrTabInfo[ tabIndex ].text =
                        if( tabText.length > styleTabComboTextLen() )
                            tabText.substring( 0, styleTabComboTextLen() ) + "..."
                        else tabText
                    arrTabInfo[ tabIndex ].tooltip = tabToolTip
                    that().arrTabInfo = arrTabInfo
                },
                "openTab" to { newAppParam: String ->
                    //--- файловое урло
                    if( newAppParam[ 0 ] == '/' )
                        window.open( newAppParam )
                    else
                        that().addTabComp( newAppParam )
                },
                "closeTab" to { tabIndex: Int ->
                    that().arrTabInfo.splice( tabIndex, 1 )
                    that().arrTabComp.splice( tabIndex, 1 )

                    var currentTabIndex = that().currentTabIndex.unsafeCast<Int>()
                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()

                    if( tabIndex <= currentTabIndex ) {
                        currentTabIndex--
                    }

                    if( currentTabIndex < 0 && arrTabInfo.isNotEmpty() )
                        currentTabIndex = 0

                    that().currentTabIndex = currentTabIndex
                },
                "setWait" to { isWait: Boolean ->
                    val waitCount = that().waitCount.unsafeCast<Int>()
                    that().waitCount = waitCount + ( if( isWait ) 1 else -1 )
                }
            )
            this.mounted = {
                that().scaleKoef =
                    if( screenDPR <= 1 ) 1.0
                    else 0.5

                val localStartAppParam = localStorage.getItem( LOCAL_STORAGE_APP_PARAM )
                that().addTabComp(
                    if( localStartAppParam.isNullOrBlank() )
                        ( document.getElementById( "startAppParam" ) as HTMLSpanElement ).innerText
                    else
                        localStartAppParam
                )
            }
            this.data = {
                json(
                    "menuBar" to null,
                    "currentTabIndex" to -1,
                    "lastTabID" to 0,
                    "arrTabComp" to arrayOf<TabComp>(),
                    "arrTabInfo" to arrayOf<TabInfo>(),
//                    "isChangePassword" to false,
                    "waitCount" to 0,

                    "scaleKoef" to 1.0,
                    "timeOffset" to 0,

                    "curAppParam" to "",

                    "style_top_container" to json(
                        "display" to "flex",
                        "flex-direction" to "column",
                        "height" to "100%"
                    ),
                    "style_tab_panel" to json(
                        "flex-grow" to 0,
                        "flex-shrink" to 0,
                        "display" to "flex",
                        "flex-direction" to "row",
                        "flex-wrap" to "wrap",
                        "justify-content" to if( !styleIsNarrowScreen ) "flex-start" else "space-between",
                        //--- необязательно - пусть лучше по высоте равны кнопке меню
                        //"align-items" to "flex-end",            // прижимаем вкладки к нижнему контролу
                        "padding" to styleTabPanelPadding(),
                        "background" to COLOR_BACK
                    ),

                    "style_tab_combo" to json (
                        "flex-grow" to 1,
                        "flex-shrink" to 1,
                        "background" to COLOR_BUTTON_BACK,
                        "border" to "1px solid $COLOR_BUTTON_BORDER",
                        "border-radius" to BORDER_RADIUS,
                        "font-size" to styleTabComboFontSize(),
                        "padding" to styleTabComboPadding(),
                        "margin" to styleTabComboMargin()
                    ),
                    "style_tab_closer_button" to json(
                        "flex-grow" to 0,
                        "flex-shrink" to 0,
                        "align-self" to "flex-start", //"flex-end" - сдвигает в правый нижний угол
                        "background" to COLOR_BUTTON_BACK,
                        "border" to "1px solid $COLOR_BUTTON_BORDER",
                        "border-radius" to BORDER_RADIUS,
                        "padding" to styleIconButtonPadding(),
                        "margin" to styleTabCloserButtonMargin(),
                        "cursor" to "pointer"
                    ),

                    "style_tab_current_title" to json(
                        "background" to COLOR_TAB_BACK_CURRENT,
                        "border-left" to "1px solid $COLOR_TAB_BORDER",
                        "border-top" to "1px solid $COLOR_TAB_BORDER",
                        "border-right" to "none",
                        "border-bottom" to "none",
                        "border-radius" to "$BORDER_RADIUS 0 0 0",
                        "font-size" to styleTabButtonFontSize(),
                        "padding" to styleTabCurrentTitlePadding(),
                        "cursor" to "pointer"
                    ),
                    "style_tab_current_closer" to json(
                        "background" to COLOR_TAB_BACK_CURRENT,
                        "border-left" to "none",
                        "border-top" to "1px solid $COLOR_TAB_BORDER",
                        "border-right" to "1px solid $COLOR_TAB_BORDER",
                        "border-bottom" to "1px solid $COLOR_PANEL_BACK",   // иначе белая полоска вместо пустого места от бордера
                        "border-radius" to "0 $BORDER_RADIUS 0 0",
                        "padding" to styleTabCurrentCloserPadding(),
                        "cursor" to "pointer"
                    ),
                    "style_tab_other_title" to json(
                        "background" to COLOR_TAB_BACK_OTHER,
                        "border-left" to "1px solid $COLOR_TAB_BORDER",
                        "border-top" to "1px solid $COLOR_TAB_BORDER",
                        "border-right" to "none",
                        "border-bottom" to "1px solid $COLOR_TAB_BORDER",
                        "border-radius" to "$BORDER_RADIUS 0 0 0",
                        "font-size" to styleTabButtonFontSize(),
                        "padding" to styleTabOtherTitlePadding(),
                        "cursor" to "pointer"
                    ),
                    "style_tab_other_closer" to json(
                        "background" to COLOR_TAB_BACK_OTHER,
                        "border-left" to "none",
                        "border-top" to "1px solid $COLOR_TAB_BORDER",
                        "border-right" to "1px solid $COLOR_TAB_BORDER",
                        "border-bottom" to "1px solid $COLOR_TAB_BORDER",
                        "border-radius" to "0 $BORDER_RADIUS 0 0",
                        "padding" to styleTabOtherCloserPadding(),
                        "cursor" to "pointer"
                    ),

                    "style_wait" to json(
                        "position" to "fixed",
                        "top" to 0,
                        "left" to 0,
                        "width" to "100%",
                        "height" to "100%",
                        "z-index" to "1000",
                        "background" to "rgba( $COLOR_WAIT, 0.7 )",
                        "display" to "grid",
                        "grid-template-rows" to "1fr auto 1fr",
                        "grid-template-columns" to "1fr auto 1fr"
                    ),
                    "style_wait_top_expander" to json (
                        "grid-area" to "1 / 2 / 2 / 3"
                    ),
                    "style_loader" to json(
                        "grid-area" to "2 / 2 / 3 / 3",
                        "width" to "16rem",
                        "height" to "16rem",
                        "z-index" to "1001",
                        "border-top" to "2rem solid $COLOR_LOADER_3",
                        "border-right" to "2rem solid $COLOR_LOADER_2",
                        "border-bottom" to "2rem solid $COLOR_LOADER_1",
                        "border-left" to "2rem solid $COLOR_LOADER_0",
                        "border-radius" to "50%",
                        "animation" to "spin 2s linear infinite"
                    ),
                    "style_wait_bottom_expander" to json (
                        "grid-area" to "3 / 2 / 4 / 3"
                    )
                )
            }
        })
    }

    private class TabInfo( val id: Int, var text: String, var tooltip: String )
    private class TabComp( val comp: VueComponentOptions )
}

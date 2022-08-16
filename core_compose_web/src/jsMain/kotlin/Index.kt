//import foatto.core.app.*
//import foatto.core_web.external.vue.Vue
//import foatto.core_web.external.vue.VueComponentOptions
//import foatto.core_web.external.vue.that
//import foatto.core_web.external.vue.vueComponentOptions
//import kotlinx.browser.document
//import kotlinx.browser.localStorage
//import kotlinx.browser.window
//import org.w3c.dom.HTMLSpanElement
//import kotlin.js.Json
//import kotlin.js.json
//
//const val LOCAL_STORAGE_APP_PARAM = "app_param"
//
//const val TOP_BAR_ID = "top_bar"
//const val MENU_CLOSER_BUTTON_ID = "menu_closer_button_id"
//
//var dialogActionFun: (that: dynamic) -> Unit = { _: dynamic -> }

open class Index {

//    fun init() {
//
//        hmTableIcon[ICON_NAME_SELECT] = "/web/images/ic_reply_${styleIconNameSuffix()}dp.png"
//
//        hmTableIcon[ICON_NAME_ADD_FOLDER] = "/web/images/ic_create_new_folder_${styleIconNameSuffix()}dp.png"
//        hmTableIcon[ICON_NAME_ADD_ITEM] = "/web/images/ic_add_${styleIconNameSuffix()}dp.png"
//
//        //--- подразделение
//        hmTableIcon[ICON_NAME_DIVISION] = "/web/images/ic_folder_shared_${styleIconNameSuffix()}dp.png"
//        //--- руководитель
//        hmTableIcon[ICON_NAME_BOSS] = "/web/images/ic_account_box_${styleIconNameSuffix()}dp.png"
//        //--- работник
//        hmTableIcon[ICON_NAME_WORKER] = "/web/images/ic_account_circle_${styleIconNameSuffix()}dp.png"
//
//        //--- подраздел
//        hmTableIcon[ICON_NAME_FOLDER] = "/web/images/ic_folder_open_${styleIconNameSuffix()}dp.png"
//
//        //--- печать
//        hmTableIcon[ICON_NAME_PRINT] = "/web/images/ic_print_${styleIconNameSuffix()}dp.png"
//
//        //----------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//
//            this.template = if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
//                ""
//            } else {
//                """
//                    <div v-bind:style="style_top_container">
//                        <component v-if="menuBar && isShowMainMenu"
//                                   v-bind:is="menuBar"
//                """ +
//                    styleMenuBar +
//                    """
//                        >
//                        </component>
//                        <div id="$MENU_CLOSER_BUTTON_ID"
//                             v-bind:style="style_menu_closer"
//                        >
//                            <button v-bind:style="style_menu_closer_button"
//                                    v-on:click="isShowMainMenu=!isShowMainMenu"
//                            >
//                                {{ isShowMainMenu ? '&lt;' : '&gt;' }}
//                            </button>
//                        </div>
//                """
//            } +
//                """
//                    <div v-bind:style="style_main_container">
//                """ +
//                if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
//                    ""
//                } else {
//                    styleTopBar
//                } +
//                """
//                        <div id="tab_panel" v-bind:style="[ style_tab_panel, { 'display' : ( isTabPanelVisible ? 'flex' : 'none' ) } ]">
//                """ +
//                if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
//                    """
//                        <component v-if="menuBar" v-bind:is="menuBar"></component>
//                    """
//                } else {
//                    ""
//                } +
//                if (styleIsNarrowScreen) {
//                    """
//                        <select v-model="currentTabIndex"
//                                v-bind:style="style_tab_combo"
//                        >
//                            <option v-for="(tab, index) in arrTabInfo"
//                                    v-bind:value="index"
//                            >
//                                {{ tab.arrText[0] }}
//                            </option>
//                        </select>
//                        <img src="/web/images/ic_close_black_48dp.png"
//                             v-show="arrTabInfo.length > 0"
//                             v-bind:style="style_tab_closer_button"
//                             v-on:click="closeTabByIndex( currentTabIndex )"
//                             title="Закрыть вкладку"
//                        >
//                    """
//                } else {
//                    """
//                        <template v-for="(tab, tabIndex) in arrTabInfo">
//                            <button v-show="tab.arrText"
//                                    v-bind:style="currentTabIndex == tabIndex ? style_tab_current_title : style_tab_other_title"
//                                    v-on:click="currentTabIndex = tabIndex"
//                                    v-bind:key="'t'+tab.id"
//                                    v-bind:title="tab.tooltip"
//                            >
//                                <span v-for="(title, textIndex) in tab.arrText"
//                                      v-bind:style="{ 'font-weight': ( currentTabIndex == tabIndex && textIndex == 0 ? 'bold' : 'normal' ) }"
//                                >
//                                    {{title}}
//                                    <br>
//                                </span>
//                            </button>
//                            <img src="/web/images/ic_close_black_16dp.png"
//                                 width=16
//                                 height=16
//                                 v-show="tab.arrText[0]"
//                                 v-bind:style="currentTabIndex == tabIndex ? style_tab_current_closer : style_tab_other_closer"
//                                 v-on:click="closeTabByIndex( tabIndex )"
//                                 v-bind:key="'c'+tab.id"
//                                 title="Закрыть вкладку"
//                            >
//                        </template>
//                    """
//                } +
//                """
//                        </div>
//
//                        <template v-if="currentTabIndex >= 0 && arrTabComp.length > 0">
//                            <keep-alive>
//                                <component v-bind:is="arrTabComp[ currentTabIndex ].comp">
//                                </component>
//                            </keep-alive>
//                        </template>
//
//                        <div v-if="waitCount > 0"
//                             v-bind:style="style_wait"
//                        >
//                            <div v-bind:style="style_wait_top_expander">
//                                &nbsp;
//                            </div>
//                            <div v-bind:style="style_loader">
//                            </div>
//                            <div v-bind:style="style_wait_bottom_expander">
//                                &nbsp;
//                            </div>
//                        </div>
//
//                        <div v-if="showDialog"
//                             v-bind:style="style_dialog"
//                        >
//                            <div v-bind:style="style_dialog_top_expander">
//                                &nbsp;
//                            </div>
//                            <div v-bind:style="style_dialog_cell">
//                                <div v-bind:style="style_dialog_text"
//                                     v-html="dialogQuestion"
//                                >
//                                </div>
//                                <br>
//                                <div v-bind:style="style_dialog_div">
//                                    <button v-bind:style="style_dialog_button_ok"
//                                            v-on:click="doDialogOk()"
//                                    >
//                                        {{ dialogButtonOkText }}
//                                    </button>
//                                    &nbsp;
//                                    <button v-if="showDialogCancel"
//                                            v-bind:style="style_dialog_button_cancel"
//                                            v-on:click="doDialogCancel()"
//                                    >
//                                        {{ dialogButtonCancelText }}
//                                    </button>
//                                </div>
//                            </div>
//                            <div v-bind:style="style_dialog_bottom_expander">
//                                &nbsp;
//                            </div>
//                        </div>
//                    </div>
//                """ +
//                if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
//                    ""
//                } else {
//                    """
//                        </div>
//                    """
//                }
//
//            this.methods = json(
//                "setMenuBar" to { menuData: Json ->
//                    that().menuBar = menuData
//                    that().isTabPanelVisible = true
//                },
//                "addTabComp" to { appParam: String ->
//                    val newTabID = that().lastTabID.unsafeCast<Int>() + 1
//
//                    that().arrTabInfo.push(TabInfo(newTabID, arrayOf(), ""))
//                    that().arrTabComp.push(TabComp(appControl(appParam, newTabID)))
//
//                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()
//                    that().currentTabIndex = arrTabInfo.lastIndex
//
//                    that().lastTabID = newTabID
//                },
//                "setTabInfo" to { tabId: Int, tabText: String, tabToolTip: String ->
//                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()
//
//                    arrTabInfo.find { tabInfo ->
//                        tabInfo.id == tabId
//                    }?.let { tabInfo ->
//                        tabInfo.arrText = tabText.split('\n').filter { tabWord ->
//                            tabWord.isNotBlank()
//                        }.map { tabWord ->
//                            if (tabWord.length > styleTabComboTextLen()) {
//                                tabWord.substring(0, styleTabComboTextLen()) + "..."
//                            } else {
//                                tabWord
//                            }
//                        }.toTypedArray()
//                        tabInfo.tooltip = tabToolTip
//                    }
//
//                    that().arrTabInfo = arrTabInfo
//                },
//                "openTab" to { newAppParam: String ->
//                    //--- файловое урло
//                    if (newAppParam[0] == '/')
//                        window.open(newAppParam)
//                    else
//                        that().addTabComp(newAppParam)
//                },
//                "closeTabByIndex" to { indexForClose: Int ->
//                    val currentTabIndex = that().currentTabIndex.unsafeCast<Int>()
//                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()
//
//                    //--- for last tab removing case
//                    if (currentTabIndex == arrTabInfo.lastIndex) {
//                        that().currentTabIndex = currentTabIndex - 1
//                    }
//
//                    that().arrTabComp.splice(indexForClose, 1)
//                    that().arrTabInfo.splice(indexForClose, 1)
//                },
//                "closeTabById" to { tabId: Int ->
//                    val currentTabIndex = that().currentTabIndex.unsafeCast<Int>()
//                    val arrTabInfo = that().arrTabInfo.unsafeCast<Array<TabInfo>>()
//
//                    //--- for last tab removing case
//                    if (currentTabIndex == arrTabInfo.lastIndex) {
//                        that().currentTabIndex = currentTabIndex - 1
//                    }
//
//                    val indexForClose = arrTabInfo.indexOfFirst { tabInfo ->
//                        tabInfo.id == tabId
//                    }
//
//                    that().arrTabComp.splice(indexForClose, 1)
//                    that().arrTabInfo.splice(indexForClose, 1)
//                },
//                "setWait" to { isWait: Boolean ->
//                    val waitCount = that().waitCount.unsafeCast<Int>()
//                    that().waitCount = waitCount + (if (isWait) 1 else -1)
//                },
//                "doDialogOk" to {
//                    that().showDialog = false
//                    dialogActionFun(that())
//                },
//                "doDialogCancel" to {
//                    that().showDialog = false
//                },
//            )
//
//            this.mounted = {
//                addBeforeMounted()
//
//                that().scaleKoef =
//                    if (screenDPR <= 1) {
//                        1.0
//                    } else {
//                        0.5
//                    }
//
//                val localStartAppParam = localStorage.getItem(LOCAL_STORAGE_APP_PARAM)
//                that().addTabComp(
//                    if (localStartAppParam.isNullOrBlank())
//                        (document.getElementById("startAppParam") as HTMLSpanElement).innerText
//                    else
//                        localStartAppParam
//                ) as Unit
//            }
//
//            this.data = {
//                json(
//                    "menuBar" to null,
//                    "isShowMainMenu" to !(styleIsNarrowScreen || styleIsHiddenMenuBar),
//                    "isTabPanelVisible" to false,
//                    "currentTabIndex" to -1,
//                    "lastTabID" to 0,
//                    "arrTabComp" to arrayOf<TabComp>(),
//                    "arrTabInfo" to arrayOf<TabInfo>(),
////                    "isChangePassword" to false,
//                    "waitCount" to 0,
//
//                    "showDialog" to false,
//                    "showDialogCancel" to false,
//                    "dialogQuestion" to "",
//                    "dialogButtonOkText" to "OK",
//                    "dialogButtonCancelText" to "Отмена",
//
//                    "currentUserName" to "",
//                    "scaleKoef" to 1.0,
//                    "timeOffset" to 0,
//
//                    "curAppParam" to "",
//
//                    "style_top_container" to json(
//                        "display" to "flex",
//                        "flex-direction" to "row",
//                        "width" to "100%",
//                        "height" to "100%",
//                    ),
//                    "style_main_container" to json(
//                        "display" to "flex",
//                        "flex-direction" to "column",
//                        "width" to "100%",
//                        "height" to "100%",
//                    ),
//                    "style_tab_panel" to json(
//                        "flex-grow" to 0,
//                        "flex-shrink" to 0,
//                        "flex-direction" to "row",
//                        "flex-wrap" to "wrap",
//                        "justify-content" to if (!styleIsNarrowScreen) {
//                            "flex-start"
//                        } else {
//                            "space-between"
//                        },
//                        //--- необязательно - пусть лучше по высоте равны кнопке меню
//                        //"align-items" to "flex-end",            // прижимаем вкладки к нижнему контролу
//                        "padding" to styleTabPanelPadding(),
//                        "background" to colorTabPanelBack,
//                    ),
//
//                    "style_menu_closer" to json(
//                        "display" to "flex",
//                        "flex-direction" to "column",
//                        "justify-content" to "center",
//                        "align-items" to "center",
//                        "background" to colorMenuCloserBack,
//                    ),
//                    "style_menu_closer_button" to json(
//                        "border" to "none",
//                        "height" to "8rem",
//                        "font-size" to "1.0rem",
//                        "font-weight" to "bold",
//                        "padding" to "0 0.1rem",
//                        "background" to colorMenuCloserButtonBack,
//                        "color" to colorMenuCloserButtonText,
//                    ),
//                    "style_tab_combo" to json(
//                        "flex-grow" to 1,
//                        "flex-shrink" to 1,
//                        "background" to colorButtonBack(),
//                        "border" to "1px solid ${colorMainBorder()}",
//                        "border-radius" to styleFormBorderRadius,
//                        "font-size" to styleTabComboFontSize(),
//                        "padding" to styleTabComboPadding(),
//                        "margin" to styleTabComboMargin()
//                    ),
//                    "style_tab_closer_button" to json(
//                        "flex-grow" to 0,
//                        "flex-shrink" to 0,
//                        "align-self" to "flex-start", //"flex-end" - сдвигает в правый нижний угол
//                        "background" to colorButtonBack(),
//                        "border" to "1px solid ${colorMainBorder()}",
//                        "border-radius" to styleFormBorderRadius,
//                        "padding" to styleIconButtonPadding(),
//                        "margin" to styleTabCloserButtonMargin(),
//                        "cursor" to "pointer"
//                    ),
//
//                    "style_tab_current_title" to json(
//                        "background" to colorTabCurrentBack(),
//                        "border-left" to styleTabCurrentTitleBorderLeft(),
//                        "border-top" to styleTabCurrentTitleBorderTop(),
//                        "border-right" to styleTabCurrentTitleBorderRight(),
//                        "border-bottom" to styleTabCurrentTitleBorderBottom(),
//                        "border-radius" to "$styleFormBorderRadius 0 0 0",
//                        "font-size" to styleTabButtonFontSize,
//                        "padding" to styleTabCurrentTitlePadding,
//                        "cursor" to "pointer",
//                    ),
//                    "style_tab_current_closer" to json(
//                        "background" to colorTabCurrentBack(),
//                        "border-left" to styleTabCurrentCloserBorderLeft(),
//                        "border-top" to styleTabCurrentCloserBorderTop(),
//                        "border-right" to styleTabCurrentCloserBorderRight(),
//                        "border-bottom" to styleTabCurrentCloserBorderBottom(),
//                        "border-radius" to "0 $styleFormBorderRadius 0 0",
//                        "padding" to styleTabCurrentCloserPadding,
//                        "cursor" to "pointer"
//                    ),
//                    "style_tab_other_title" to json(
//                        "background" to colorTabOtherBack(),
//                        "border-left" to styleTabOtherTitleBorderLeft(),
//                        "border-top" to styleTabOtherTitleBorderTop(),
//                        "border-right" to styleTabOtherTitleBorderRight(),
//                        "border-bottom" to styleTabOtherTitleBorderBottom(),
//                        "border-radius" to "$styleFormBorderRadius 0 0 0",
//                        "font-size" to styleTabButtonFontSize,
//                        "padding" to styleTabOtherTitlePadding,
//                        "cursor" to "pointer",
//                    ),
//                    "style_tab_other_closer" to json(
//                        "background" to colorTabOtherBack(),
//                        "border-left" to styleTabOtherCloserBorderLeft(),
//                        "border-top" to styleTabOtherCloserBorderTop(),
//                        "border-right" to styleTabOtherCloserBorderRight(),
//                        "border-bottom" to styleTabOtherCloserBorderBottom(),
//                        "border-radius" to "0 $styleFormBorderRadius 0 0",
//                        "padding" to styleTabOtherCloserPadding,
//                        "cursor" to "pointer"
//                    ),
//
//                    "style_wait" to json(
//                        "position" to "fixed",
//                        "top" to 0,
//                        "left" to 0,
//                        "width" to "100%",
//                        "height" to "100%",
//                        "z-index" to Z_INDEX_WAIT,
//                        "background" to colorWaitBack,
//                        "display" to "grid",
//                        "grid-template-rows" to "1fr auto 1fr",
//                        "grid-template-columns" to "1fr auto 1fr",
//                    ),
//                    "style_wait_top_expander" to json(
//                        "grid-area" to "1 / 2 / 2 / 3",
//                    ),
//                    "style_loader" to json(
//                        "grid-area" to "2 / 2 / 3 / 3",
//                        "width" to "16rem",
//                        "height" to "16rem",
//                        "z-index" to Z_INDEX_LOADER,
//                        "border-top" to "2rem solid $colorWaitLoader3",
//                        "border-right" to "2rem solid $colorWaitLoader2",
//                        "border-bottom" to "2rem solid $colorWaitLoader1",
//                        "border-left" to "2rem solid $colorWaitLoader0",
//                        "border-radius" to "50%",
//                        "animation" to "spin 2s linear infinite",
//                    ),
//                    "style_wait_bottom_expander" to json(
//                        "grid-area" to "3 / 2 / 4 / 3",
//                    ),
//
//                    "style_dialog" to json(
//                        "position" to "fixed",
//                        "top" to 0,
//                        "left" to 0,
//                        "width" to "100%",
//                        "height" to "100%",
//                        "z-index" to Z_INDEX_DIALOG,
//                        "background" to colorDialogBack,
//                        "display" to "grid",
//                        "grid-template-rows" to "1fr auto 1fr",
//                        "grid-template-columns" to "1fr auto 1fr",
//                    ),
//                    "style_dialog_top_expander" to json(
//                        "grid-area" to "1 / 2 / 2 / 3",
//                    ),
//                    "style_dialog_cell" to json(
//                        "grid-area" to "2 / 2 / 3 / 3",
//                        "padding" to styleDialogCellPadding(),
//                        "border" to "1px solid $colorDialogBorder",
//                        "border-radius" to styleFormBorderRadius,
//                        "background" to colorDialogBackCenter,
//                        "display" to "flex",
//                        "flex-direction" to "column",
//                        "align-items" to "center",
//                    ),
//                    "style_dialog_bottom_expander" to json(
//                        "grid-area" to "3 / 2 / 4 / 3",
//                    ),
//                    "style_dialog_div" to json(
//                        "font-size" to styleControlTextFontSize(),
//                        "padding" to styleDialogControlPadding()
//                    ),
//                    "style_dialog_text" to json(
//                        "align-self" to "center",
//                        "font-size" to styleControlTextFontSize(),
//                        "font-weight" to "bold",
//                        "color" to COLOR_MAIN_TEXT,
//                    ),
//                    "style_dialog_button_ok" to json(
//                        "background" to colorDialogButtonBack,
//                        "border" to "1px solid $colorDialogButtonBorder",
//                        "border-radius" to styleButtonBorderRadius,
//                        "font-size" to styleCommonButtonFontSize(),
//                        "padding" to styleDialogButtonPadding(),
////                        "margin" to styleDialogButtonMargin(),
//                        "cursor" to "pointer",
//                    ),
//                    "style_dialog_button_cancel" to json(
//                        "background" to colorDialogButtonBack,
//                        "border" to "1px solid $colorDialogButtonBorder",
//                        "border-radius" to styleButtonBorderRadius,
//                        "font-size" to styleCommonButtonFontSize(),
//                        "padding" to styleDialogButtonPadding(),
////                        "margin" to styleDialogButtonMargin(),
//                        "cursor" to "pointer",
//                    ),
//                )
//            }
//        })
//    }
//
//    open fun addBeforeMounted() {}
//
//    private class TabInfo(val id: Int, var arrText: Array<String>, var tooltip: String)
//    private class TabComp(val comp: VueComponentOptions)
}



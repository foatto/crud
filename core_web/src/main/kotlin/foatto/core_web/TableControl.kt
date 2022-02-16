package foatto.core_web

import foatto.core.app.*
import foatto.core.link.*
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.max

val hmTableIcon = mutableMapOf(

    ICON_NAME_SELECT to "/web/images/ic_reply_black_48dp.png",

    ICON_NAME_ADD_FOLDER to "/web/images/ic_create_new_folder_black_48dp.png",
    ICON_NAME_ADD_ITEM to "/web/images/ic_add_black_48dp.png",

    //--- system_user ---

    //--- подразделение
    ICON_NAME_DIVISION to "/web/images/ic_folder_shared_black_48dp.png",
    //--- руководитель
    ICON_NAME_BOSS to "/web/images/ic_account_box_black_48dp.png",
    //--- работник
    ICON_NAME_WORKER to "/web/images/ic_account_circle_black_48dp.png",

    //--- shop_catalog ---

    //--- подраздел
    ICON_NAME_FOLDER to "/web/images/ic_folder_open_black_48dp.png",

    //--- shop_document_content, office_task_thread ---

    //--- распечатка накладной, обсуждения поручения
    ICON_NAME_PRINT to "/web/images/ic_print_black_48dp.png"
)

var tableTemplateAdd: String = ""
var tableClientActionFun: (action: String, params: Array<Pair<String, String>>, that: dynamic) -> Unit = { _: String, _: Array<Pair<String, String>>, _: dynamic -> }
var tableMethodsAdd: Json = json()
var tableDataAdd: Json = json()

@Suppress("UnsafeCastFromDynamic")
fun tableControl(appParam: String, tableResponse: TableResponse, tabId: Int) = vueComponentOptions().apply {

    this.template = """
        <div v-bind:style="style_table">
            <div v-bind:style="style_header">
                <template v-for="titleData in arrTitleData">
                    <button v-if="titleData.url"
                            v-on:click="invoke( titleData.url, false )"
                            v-bind:key="'thb'+titleData.id"
                            v-bind:style="[ style_text_button, style_button_with_border ]"
                    >
                            
                        {{ titleData.text }}
                    </button>
                    <span v-else v-bind:key="'ths'+titleData.id" v-bind:style="style_title">
                        {{ titleData.text }}
                    </span>
                </template>
            </div>

            <div v-bind:style="style_toolbar">
                <span v-bind:style="style_toolbar_block">
                    <img src="/web/images/ic_reply_all_black_48dp.png" 
                         v-if="selectorCancelURL"
                         v-on:click="invoke( selectorCancelURL, false )"
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Отменить выбор"
                    >
                    <input type="text"
                           v-show="isFindTextVisible"
                           v-model="findText" 
                           v-bind:size="style_find_editor_len"
                           v-bind:style="style_find_editor"
                           placeholder="Поиск..." 
                           v-on:keyup.enter="doFind( false )"
                    >
                    <img src="/web/images/ic_search_black_48dp.png" 
                         v-on:click="doFind( false )"
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Искать"
                    >
                    <img src="/web/images/ic_youtube_searched_for_black_48dp.png" 
                         v-show="findText" 
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         v-on:click="doFind( true )"
                         title="Отключить поиск"
                    >
                </span>
                
                <span v-bind:style="style_toolbar_block">
                    <img v-show="!${styleIsNarrowScreen} || !isFindTextVisible" 
                         v-for="addButton in arrAddButton"
                         v-bind:key="'add'+addButton.id"
                         v-bind:src="addButton.icon"                         
                         v-bind:title="addButton.tooltip"
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         v-on:click="invoke( addButton.url, false )"
                    >
                    <img src="/web/images/ic_mode_edit_black_48dp.png" 
                         v-show="( !${styleIsNarrowScreen} || !isFindTextVisible ) && isFormButtonVisible" 
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Открыть форму"
                         v-on:click="doForm()"
                    >
                    <img src="/web/images/ic_exit_to_app_black_48dp.png" 
                         v-show="( !${styleIsNarrowScreen} || !isFindTextVisible ) && isGotoButtonVisible" 
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Перейти"
                         v-on:click="doGoto()"
                    >
                    <img src="/web/images/ic_menu_black_48dp.png" 
                         v-show="( !${styleIsNarrowScreen} || !isFindTextVisible ) && isPopupButtonVisible" 
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Показать операции по строке"
                         v-on:click="doPopup()"
                    >
                </span>
                
                <span v-bind:style="style_toolbar_block">
                    <button v-for="serverButton in arrServerButton"
                            v-show="!${styleIsNarrowScreen} || (!isFindTextVisible && !serverButton.isForWideScreenOnly)" 
                            v-bind:key="'sb'+serverButton.id"
                            v-on:click="invoke( serverButton.url, serverButton.inNewWindow )"
                            v-bind:style="[ style_icon_button, style_button_with_border ]"
                            v-bind:title="serverButton.tooltip"
                    >
                        <img v-if="serverButton.icon" v-bind:src="serverButton.icon">
                        <span v-else>
                            {{serverButton.caption}}
                        </span>
                    </button>
                </span>

                <span v-bind:style="style_toolbar_block">
                    <button v-for="clientButton in arrClientButton"
                            v-show="!${styleIsNarrowScreen} || (!isFindTextVisible && !clientButton.isForWideScreenOnly)" 
                            v-bind:key="'cb'+clientButton.id"
                            v-on:click="clientAction( clientButton.action, clientButton.params )"
                            v-bind:style="[ style_icon_button, style_button_with_border ]"
                            v-bind:title="clientButton.tooltip"
                    >
                        <img v-if="clientButton.icon" v-bind:src="clientButton.icon">
                        <span v-else>
                            {{clientButton.caption}}
                        </span>
                    </button>
                </span>
                
                <span v-bind:style="style_toolbar_block">
                    <input type="text"
                           readonly
                           size=1
                           v-bind:style="style_cursor_box"
                           id="table_cursor_$tabId"
                           v-on:keyup.up="doKeyUp()"
                           v-on:keyup.down="doKeyDown()"
                           v-on:keyup.home="doKeyHome()"
                           v-on:keyup.end="doKeyEnd()"
                           v-on:keyup.page-up="doKeyPageUp()"
                           v-on:keyup.page-down="doKeyPageDown()"
                           v-on:keyup.enter="doKeyEnter()"
                           v-on:keyup.esc="doKeyEsc()"
                           v-on:keyup.f4="closeTabById()"
                    >
                    <img src="/web/images/ic_sync_black_48dp.png" 
                         v-show="( !${styleIsNarrowScreen} || !isFindTextVisible )"
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Обновить"
                         v-on:click="invoke( '$appParam', false )"
                    >
                </span>
            </div>

            <div v-bind:style="style_grid">
                <div v-for="gridData in arrGridData"
                     v-bind:key="'gd'+gridData.id"
                     v-bind:style="[
                        gridData.cellStyle,
                        { 'background' : ( gridData.row >= 0 && currentRow >= gridData.row && currentRow < gridData.row + gridData.rowSpan ?
                                          '$colorCurrentAndHover' : gridData.backColor ) }
                     ]"
                     v-on:dblclick.prevent="gridData.row >= 0 && arrRowData[ gridData.row ].rowURL ?
                                    invoke( arrRowData[ gridData.row ].rowURL, arrRowData[ gridData.row ].itRowURLInNewWindow ) : null"
                     v-on:click="gridData.cellURL ? invoke( gridData.cellURL, false ) : setCurrentRow( gridData.row )"
                     v-on:click.right.prevent="gridData.row >= 0 ? showPopupMenu( gridData.row, ${'$'}event ) : null"
                >
                    
                    <template v-if="gridData.cellType == '${TableCellType.CHECKBOX}'"> 
                        <input type="checkbox" v-model="gridData.booleanValue" 
                               v-bind:style="gridData.elementStyle"
                               v-bind:title="gridData.tooltip"
                               v-on:click.prevent="isShowPopupMenu = false"
                        >
                    </template>
                    <template v-else-if="gridData.cellType == '${TableCellType.TEXT}'">
                        <div v-bind:style="gridData.elementStyle"
                             v-bind:title="gridData.tooltip"
                        >
                            <img v-if="gridData.textCellData.icon" v-bind:src="gridData.textCellData.icon">
                            <img v-else-if="gridData.textCellData.image" v-bind:src="gridData.textCellData.image">
                            <span v-else v-html="gridData.textCellData.text">
                            </span>
                        </div>
                    </template>
                    <template v-else-if="gridData.cellType == '${TableCellType.BUTTON}'">
                        <template v-for="cellData in gridData.arrButtonCellData">                    
                            <img v-if="cellData.icon" 
                                 v-bind:src="cellData.icon"
                                 v-bind:style="cellData.style"
                                 v-bind:title="gridData.tooltip"
                                 v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                            <img v-else-if="cellData.image" 
                                 v-bind:src="cellData.image"
                                 v-bind:style="cellData.style"
                                 v-bind:title="gridData.tooltip"
                                 v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                            <button v-else 
                                    v-bind:style="cellData.style"
                                    v-bind:title="gridData.tooltip"
                                    v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                                <span v-html="cellData.text">
                                </span>
                            </button>
                        </template>
                    </template>
                    <template v-else-if="gridData.cellType == '${TableCellType.GRID}'">
                        <template v-for="arrRow in gridData.arrGridCellData">                    
                            <div v-for="cellData in arrRow" 
                                 v-bind:style="cellData.style"
                                 v-bind:title="gridData.tooltip"
                            >
                                <img v-if="cellData.icon" v-bind:src="cellData.icon">
                                <img v-else-if="cellData.image" v-bind:src="cellData.image">
                                <span v-else v-html="cellData.text">
                                </span>
                            </div>
                        </template>
                    </template>
                </div>
            </div>

            <div v-bind:style="style_pagebar">
                <template v-for="pageButton in arrPageButton">
                    <button v-if="pageButton.url"
                            v-on:click="invoke( pageButton.url, false )"
                            v-bind:key="'pb'+pageButton.id"
                            v-bind:style="[ style_icon_button, style_button_with_border, style_page_button ]">

                        {{pageButton.text}}
                    </button>
                    <button v-else
                          v-bind:key="'ps'+pageButton.id"
                          v-bind:style="[ style_icon_button, style_button_no_border, style_page_button ]">
                          
                        {{pageButton.text}}
                    </button>
                </template>
            </div>

            <div v-if="isShowPopupMenu"
                v-bind:style="[style_popup_menu_start, style_popup_menu_pos]"
                v-on:mouseleave="isShowPopupMenu=false"
            >
                ${menuGenerateBody("arrCurPopupData", "popupMenuClick", "")}
            </div>
    """ +
        tableTemplateAdd +
        """
        </div>
    """

    this.methods = json(
        "readHeader" to {
            var tabToolTip = ""

            //--- загрузка заголовка таблицы/формы
            var titleID = 0
            val alTitleData = mutableListOf<TableTitleData>()
            for (headerData in tableResponse.arrHeader) {
                val url = headerData.first
                val text = headerData.second

                tabToolTip += (
                    if (tabToolTip.isEmpty()) {
                        ""
                    } else {
                        " | "
                    }
                    ) + text
                alTitleData.add(TableTitleData(titleID++, url, text))

//                //--- запомним последнюю кнопку заголовка в табличном режиме как кнопку отмены или возврата на уровень выше
//                butTableCancel = button
            }
            that().`$root`.setTabInfo(tabId, tableResponse.tab, tabToolTip)
            that().arrTitleData = alTitleData.toTypedArray()
        },
        "readAddButtons" to {
            var addButtonID = 0
            val alAddButton = mutableListOf<AddActionButton_>()
            for (aab in tableResponse.arrAddActionButton) {
                val icon = hmTableIcon[aab.icon] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if (aab.icon.isNotBlank() && icon.isBlank()) {
                    aab.icon
                } else {
                    aab.tooltip.replace("\n", "<br>")
                }
                alAddButton.add(
                    AddActionButton_(
                        id = addButtonID++,
                        caption = caption,
                        tooltip = aab.tooltip,
                        icon = icon,
                        url = aab.url
                    )
                )
            }
            that().arrAddButton = alAddButton.toTypedArray()
        },
        "readServerButtons" to {
            var serverButtonID = 0
            val alServerButton = mutableListOf<ServerActionButton_>()
            for (sab in tableResponse.arrServerActionButton) {
                val icon = hmTableIcon[sab.icon] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if (sab.icon.isNotBlank() && icon.isBlank()) {
                    sab.icon
                } else {
                    sab.caption.replace("\n", "<br>")
                }
                alServerButton.add(
                    ServerActionButton_(
                        id = serverButtonID++,
                        caption = caption,
                        tooltip = sab.tooltip,
                        icon = icon,
                        url = sab.url,
                        inNewWindow = sab.inNewWindow,
                        isForWideScreenOnly = sab.isForWideScreenOnly,
                    )
                )
            }
            that().arrServerButton = alServerButton.toTypedArray()
        },
        "readClientButtons" to {
            var clientButtonID = 0
            val alClientButton = mutableListOf<ClientActionButton_>()
            for (cab in tableResponse.arrClientActionButton) {
                val icon = hmTableIcon[cab.icon] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if (cab.icon.isNotBlank() && icon.isBlank()) {
                    cab.icon
                } else {
                    cab.caption.replace("\n", "<br>")
                }
                alClientButton.add(
                    ClientActionButton_(
                        id = clientButtonID++,
                        caption = caption,
                        tooltip = cab.tooltip,
                        icon = icon,
                        action = cab.action,
                        params = cab.params,
                        isForWideScreenOnly = cab.isForWideScreenOnly,
                    )
                )
            }
            that().arrClientButton = alClientButton.toTypedArray()
        },
        "readPageButtons" to {
            var pageButtonID = 0
            var isEmptyPassed = false
            var pageUpUrl = ""
            var pageDownUrl = ""
            val alPageButton = mutableListOf<PageButton>()
            //--- вывести новую разметку страниц
            for (value in tableResponse.arrPageButton) {
                val url = value.first
                val text = value.second

                alPageButton.add(PageButton(pageButtonID++, url, text))

                if (url.isEmpty()) {
                    isEmptyPassed = true
                } else {
                    if (!isEmptyPassed) {
                        pageUpUrl = url
                    }
                    if (isEmptyPassed && pageDownUrl.isEmpty()) {
                        pageDownUrl = url
                    }
                }
            }

            val pageButtonStyle = json(
                "width" to styleTablePageButtonWidth(tableResponse.arrPageButton.size),
                "font-size" to styleTablePageButtonFontSize(tableResponse.arrPageButton.size)
            )

            that().pageUpUrl = pageUpUrl
            that().pageDownUrl = pageDownUrl
            that().arrPageButton = alPageButton.toTypedArray()
            that().style_page_button = pageButtonStyle
        },
        "readTable" to {
            val alGridData = mutableListOf<TableGridData>()
            var gridCellID = 0
            var startRow = 0

            //--- заголовки столбцов таблицы
            for ((index, value) in tableResponse.arrColumnCaption.withIndex()) {
                val url = value.first
                val text = value.second
                val captionCell = TableGridData(
                    id = gridCellID++,
                    cellType = TableCellType.TEXT,
                    cellStyle = json(
                        "grid-area" to "${startRow + 1} / ${index + 1} / ${startRow + 2} / ${index + 2}",
                        "justify-self" to "stretch",
                        "align-self" to "stretch",
                        "border-left" to "0.5px solid $colorMainBorder",
                        "border-top" to "none",
                        "border-right" to "0.5px solid $colorMainBorder",
                        "border-bottom" to "1px solid $colorMainBorder",
                        "cursor" to if (url.isBlank()) {
                            "default"
                        } else {
                            "pointer"
                        },
                        "display" to "flex",
                        "justify-content" to "center",
                        "align-items" to "center",
                        "font-size" to styleTableTextFontSize(),
                        "padding" to styleControlPadding(),
                        //--- sticky header
                        "position" to "sticky",
                        "top" to "0",
                        "z-index" to "1",   // workaround for bug with CheckBoxes in table, which above, than typical cell, include "sticky" table headers
                    ),
                    elementStyle = json(
                    ),
                    rowSpan = 1,
                    backColor = colorMainBack1,
                    tooltip = if (url.isBlank()) "" else "Сортировать по этому столбцу",
                    textCellData = TableTextCellData_(
                        text = text
                    ),
                    row = -1            // special row number as flag for table header row
                )
                captionCell.cellURL = url
                alGridData.add(captionCell)
            }
            startRow++
            var maxRow = 0
            var maxCol = tableResponse.arrColumnCaption.size

            for (tc in tableResponse.arrTableCell) {
                val backColor =
                    when (tc.backColorType.toString()) {
                        TableCellBackColorType.DEFINED.toString() -> getColorFromInt(tc.backColor)
                        TableCellBackColorType.GROUP_0.toString() -> colorGroupBack0
                        TableCellBackColorType.GROUP_1.toString() -> colorGroupBack1
                        else -> if (tc.row % 2 == 0) {
                            colorTableRowBack0
                        } else {
                            colorTableRowBack1
                        }
                    }
                val textColor =
                    when (tc.foreColorType.toString()) {
                        TableCellForeColorType.DEFINED.toString() -> getColorFromInt(tc.foreColor)
                        else -> COLOR_MAIN_TEXT
                    }
                val align =
                    when (tc.cellType.toString()) {
                        TableCellType.BUTTON.toString() -> {
                            "center"
                        }
                        TableCellType.CHECKBOX.toString() -> {
                            "center"
                        }
                        //--- на самом деле нет других вариантов
                        //TableCellType.TEXT.toString() -> {
                        else -> {
                            when (tc.align.toString()) {
                                TableCellAlign.LEFT.toString() -> "flex-start"
                                TableCellAlign.CENTER.toString() -> "center"
                                TableCellAlign.RIGHT.toString() -> "flex-end"
                                else -> "center"
                            }
                        }
                    }
                val cellStyle = json(
                    "grid-area" to "${startRow + tc.row + 1} / ${tc.col + 1} / ${startRow + tc.row + 1 + tc.rowSpan} / ${tc.col + 1 + tc.colSpan}",
                    "justify-self" to "stretch",
                    "align-self" to "stretch",
                    //--- пока не будем менять размер вместе с толщиной шрифта (потом сделаем явную передачу увеличения размера шрифта)
                    "font-weight" to (if (tc.fontStyle == 0) "normal" else "bold"),
                    "padding" to styleControlPadding()
                )
                val elementStyle = json()

                var textCellData: TableTextCellData_? = null
                val alButtonCellData = mutableListOf<TableButtonCellData_>()
                val alGridCellData = mutableListOf<MutableList<TableGridCellData_>>()

                when (tc.cellType.toString()) {
                    TableCellType.CHECKBOX.toString() -> {
                        cellStyle.add(
                            json(
                                "display" to "flex",
                                "justify-content" to align,
                                "align-items" to "center"
                            )
                        )
                        //--- for checkbox only
                        elementStyle.add(
                            json(
                                "color" to textColor,
                                "transform" to styleControlCheckBoxTransform()
                            )
                        )
                    }
                    TableCellType.TEXT.toString() -> {
                        val icon = hmTableIcon[tc.textCellData.icon] ?: ""
                        //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                        var text = if (tc.textCellData.icon.isNotBlank() && icon.isBlank()) {
                            tc.textCellData.icon
                        } else {
                            tc.textCellData.text.replace("\n", "<br>")
                        }
                        if (!tc.isWordWrap) {
                            text = text.replace(" ", "&nbsp;")
                        }
                        textCellData = TableTextCellData_(
                            icon = icon,
                            image = tc.textCellData.image,
                            text = text,
                        )
                        cellStyle.add(
                            json(
                                "display" to "flex",
                                "justify-content" to align,
                                "align-items" to "center"
                            )
                        )
                        elementStyle.add(
                            json(
                                "color" to textColor,
                                "font-size" to styleTableTextFontSize(),
                                "user-select" to if (styleIsTouchScreen()) "none" else "auto"
                            )
                        )
                    }
                    TableCellType.BUTTON.toString() -> {
                        tc.arrButtonCellData.forEachIndexed { index, cellData ->
                            val icon = hmTableIcon[cellData.icon] ?: ""
                            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                            val text = if (cellData.icon.isNotBlank() && icon.isBlank()) {
                                cellData.icon
                            } else {
                                cellData.text.replace("\n", "<br>")
                            }
                            alButtonCellData.add(
                                TableButtonCellData_(
                                    icon = icon,
                                    image = cellData.image,
                                    text = text,
                                    url = cellData.url,
                                    inNewWindow = cellData.inNewWindow,
                                    style = json(
                                        "border" to "1px solid $colorButtonBorder",
                                        "border-radius" to BORDER_RADIUS,
                                        "background" to colorButtonBack,
                                        "color" to textColor,
                                        "font-size" to styleCommonButtonFontSize(),
                                        "padding" to styleTextButtonPadding(),
                                        "cursor" to "pointer",
                                        "grid-area" to "${index + 1} / 1 / ${index + 2} / 2",
                                        "justify-self" to "center", // horizontal align
                                        "align-self" to "center",   // vertical align
                                    )
                                )
                            )
                        }
                        if (tc.arrButtonCellData.isNotEmpty()) {
                            cellStyle.add(
                                json(
                                    "display" to "grid",
                                    "grid-template-rows" to "repeat(${tc.arrButtonCellData.size},auto)",
                                    "grid-template-columns" to "repeat(1,auto)",
                                )
                            )
                        }
                    }
                    TableCellType.GRID.toString() -> {
                        tc.arrGridCellData.forEachIndexed { rowIndex, cellRow ->
                            alGridCellData.add(mutableListOf())

                            cellRow.forEachIndexed { colIndex, cellData ->
                                val icon = hmTableIcon[cellData.icon] ?: ""
                                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                                var text = if (cellData.icon.isNotBlank() && icon.isBlank()) {
                                    cellData.icon
                                } else {
                                    cellData.text.replace("\n", "<br>")
                                }
                                if (!tc.isWordWrap) {
                                    text = text.replace(" ", "&nbsp;")
                                }
                                alGridCellData.last().add(
                                    TableGridCellData_(
                                        icon = icon,
                                        image = cellData.image,
                                        text = text,
                                        style = json(
                                            "color" to textColor,
                                            "font-size" to styleTableTextFontSize(),
                                            "user-select" to if (styleIsTouchScreen()) "none" else "auto",
                                            "grid-area" to "${rowIndex + 1} / ${colIndex + 1} / ${rowIndex + 2} / ${colIndex + 2}",
                                            "justify-self" to "stretch",
                                            "align-self" to "stretch",
                                            "display" to "flex",
                                            "justify-content" to align,
                                            "align-items" to "center",
                                            "padding" to styleTableGridCellTypePadding(),
                                        )
                                    )
                                )
                            }
                        }
                        if (tc.arrGridCellData.isNotEmpty()) {
                            cellStyle.add(
                                json(
                                    "display" to "grid",
                                    "grid-template-rows" to "repeat(${tc.arrGridCellData.size},auto)",
                                    "grid-template-columns" to "repeat(${tc.arrGridCellData.first().size},auto)",
                                )
                            )
                        }
                    }
                }
                alGridData.add(
                    TableGridData(
                        id = gridCellID++,
                        cellType = tc.cellType,
                        cellStyle = cellStyle,
                        elementStyle = elementStyle,
                        rowSpan = tc.rowSpan,
                        backColor = backColor,
                        tooltip = tc.tooltip,
                        booleanValue = tc.booleanValue,
                        textCellData = textCellData,
                        arrButtonCellData = alButtonCellData.toTypedArray(),
                        arrGridCellData = alGridCellData.map { it.toTypedArray() }.toTypedArray(),
                        row = tc.row,
                    )
                )
                maxRow = max(maxRow, startRow + tc.row + tc.rowSpan)
                maxCol = max(maxCol, tc.col + tc.colSpan)
            }
            maxRow++

            that().arrGridData = alGridData.toTypedArray()
            that().style_grid = json(
                "flex-grow" to 1,
                "flex-shrink" to 1,
                "height" to "100%",  //- необязательно
                "overflow" to "auto",
                "display" to "grid",
                "grid-template-rows" to "repeat($maxRow,max-content)",
                "grid-template-columns" to "repeat($maxCol,auto)",
                //--- полностью запретить выделение текста - простейший способ победить паразитное выделение текста вместо лонг-тача на больших сенсорных экранах
                "user-select" to "none" // if( styleIsNarrowScreen ) "none" else "auto"
            )
            that().arrRowData = tableResponse.arrTableRowData
        },
        "closeTabById" to {
            that().`$root`.closeTabById(tabId)
        },
        "doFind" to { isClear: Boolean ->
            val isFindTextVisible = that().isFindTextVisible.unsafeCast<Boolean>()

            if (isClear) that().findText = ""

            if (!isClear && !isFindTextVisible) {
                that().isFindTextVisible = true
            } else {
                val findURL = that().findURL.unsafeCast<String>()
                val findText = that().findText.unsafeCast<String>()

                that().`$parent`.invoke(AppRequest(action = findURL, find = findText.trim()))
            }
        },
        "doForm" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if (currentRow >= 0 && arrRowData[currentRow].formURL.isNotEmpty())
                that().invoke(arrRowData[currentRow].formURL, false)
        },
        "doGoto" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if (currentRow >= 0 && arrRowData[currentRow].gotoURL.isNotEmpty())
                that().invoke(arrRowData[currentRow].gotoURL, arrRowData[currentRow].itGotoURLInNewWindow)
        },
        "doPopup" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if (currentRow >= 0 && arrRowData[currentRow].alPopupData.isNotEmpty())
                that().showPopupMenu(currentRow, null)
        },
        "setCurrentRow" to { rowNo: Int ->
            setCurrentRow(that(), rowNo)
            focusToCursorField(tabId)
        },
        "doKeyUp" to {
            var currentRow = that().currentRow.unsafeCast<Int>()
            if (currentRow > 0) {
                currentRow--
                setCurrentRow(that(), currentRow)
            }
        },
        "doKeyDown" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            var currentRow = that().currentRow.unsafeCast<Int>()
            if (currentRow < arrRowData.lastIndex) {
                currentRow++
                setCurrentRow(that(), currentRow)
            }
        },
        "doKeyHome" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            if (arrRowData.isNotEmpty()) {
                setCurrentRow(that(), 0)
            }
        },
        "doKeyEnd" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            if (arrRowData.isNotEmpty()) {
                setCurrentRow(that(), arrRowData.lastIndex)
            }
        },
        "doKeyPageUp" to {
            val pageUpUrl = that().pageUpUrl.unsafeCast<String>()
            if (pageUpUrl.isNotEmpty()) {
                that().invoke(pageUpUrl, false)
            }
        },
        "doKeyPageDown" to {
            val pageDownUrl = that().pageDownUrl.unsafeCast<String>()
            if (pageDownUrl.isNotEmpty()) {
                that().invoke(pageDownUrl, false)
            }
        },
        "doKeyEnter" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()
            if (currentRow >= 0 && currentRow < arrRowData.size) {
                val curRowData = arrRowData[currentRow]
                if (curRowData.rowURL.isNotEmpty()) {
                    that().invoke(curRowData.rowURL, curRowData.itRowURLInNewWindow)
                }
            }
        },
        "doKeyEsc" to {
            val selectorCancelURL = that().selectorCancelURL.unsafeCast<String>()
            if(selectorCancelURL.isNotEmpty()) {
                that().invoke(selectorCancelURL, false)
            }
        },
        "invoke" to { newAppParam: String, inNewWindow: Boolean ->
            if (inNewWindow)
                that().`$root`.openTab(newAppParam)
            else
                that().`$parent`.invoke(AppRequest(action = newAppParam))
        },
        "clientAction" to { action: String, params: Array<Pair<String, String>> ->
            tableClientActionFun(action, params, that())
        },
        "showPopupMenu" to { row: Int, event: Event ->
            //--- чтобы строчка выделялась и по правой кнопке мыши тоже
            that().setCurrentRow(row)

            val mouseEvent = event as? MouseEvent
            val mouseX = mouseEvent?.pageX ?: (window.innerWidth.toDouble() / 3)

            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            if (arrRowData[row].alPopupData.isNotEmpty()) {
                var menuSide = ""
                var menuPos = ""
                if (styleIsNarrowScreen) {
                    menuSide = "left"
                    menuPos = "5%"
                } else if (mouseX <= window.innerWidth / 2) {
                    menuSide = "left"
                    menuPos = "${mouseX}px"
                } else {
                    menuSide = "right"
                    menuPos = "${window.innerWidth - mouseX}px"
                }

                that().arrCurPopupData = convertPopupMenuData(arrRowData[row].alPopupData)

                //--- в данной ситуации clientX/Y == pageX/Y, offsetX/Y идёт от текущего элемента (ячейки таблицы), screenX/Y - от начала экрана
                that().style_popup_menu_pos = json(menuSide to menuPos)
                that().isShowPopupMenu = true
            } else that().isShowPopupMenu = false
        },
        "popupMenuClick" to { menuData: PopupMenuData ->
            that().isShowPopupMenu = false
            that().invoke(menuData.url, menuData.inNewWindow)
        },
    ).add(
        tableMethodsAdd
    )

    this.mounted = {
        that().readHeader()
        that().selectorCancelURL = tableResponse.selectorCancelURL

        //--- загрузка конфигурации тулбара
        that().findURL = tableResponse.findURL
        that().findText = tableResponse.findText

        that().readAddButtons()
        that().readServerButtons()
        that().readClientButtons()
        that().readPageButtons()
        that().readTable()

        //--- установка текущей строки
        that().setCurrentRow(tableResponse.selectedRow)

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        that().`$root`.curAppParam = appParam

        focusToCursorField(tabId)
    }

    this.data = {
        json(
            "arrTitleData" to arrayOf<TableTitleData>(),
            "selectorCancelURL" to "",

            "isFindTextVisible" to !styleIsNarrowScreen,
            "findURL" to "",
            "findText" to "",

            "isFormButtonVisible" to false,
            "isGotoButtonVisible" to false,
            "isPopupButtonVisible" to false,

            "arrAddButton" to arrayOf<AddActionButton_>(),
            "arrServerButton" to arrayOf<ServerActionButton_>(),
            "arrClientButton" to arrayOf<ClientActionButton_>(),
            "arrPageButton" to arrayOf<PageButton>(),
            "arrGridData" to arrayOf<TableGridData>(),
            "style_grid" to "",

            "pageUpUrl" to "",
            "pageDownUrl" to "",

            "arrRowData" to arrayOf<TableRowData>(),
            "arrCurPopupData" to null,
            "isShowPopupMenu" to false,
            "currentRow" to -1,

            "style_table" to json(
                "flex-grow" to 1,
                "flex-shrink" to 1,
                "display" to "flex",
                "flex-direction" to "column",
                "height" to "100%",
            ),
            "style_header" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "center",
                "align-items" to "center",        // "baseline" ?
                "border-top" to if (!styleIsNarrowScreen) "none" else "1px solid $colorMainBorder",
                "padding" to styleControlPadding(),
                "background" to colorMainBack1,
            ),
            "style_toolbar" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "space-between",
                "align-items" to "center",
                "border-bottom" to "1px solid $colorMainBorder",
                "padding" to styleControlPadding(),
                "background" to colorMainBack1,
            ),
            "style_toolbar_block" to json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "nowrap",
                "justify-content" to "center",
                "align-items" to "center",
            ),
            "style_pagebar" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "center",
                "align-items" to "center",
                "border-top" to "1px solid $colorMainBorder",
                "padding" to styleTablePageBarPadding(),
                "background" to colorMainBack1,
            ),
            "style_title" to json(
                "font-size" to styleControlTitleTextFontSize(),
                "padding" to styleControlTitlePadding(),
            ),
            "style_icon_button" to json(
                "background" to colorButtonBack,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleIconButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer"
            ),
            "style_text_button" to json(
                "background" to colorButtonBack,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleTextButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer",
            ),
            "style_button_with_border" to json(
                "border" to "1px solid $colorButtonBorder",
                "border-radius" to BORDER_RADIUS,
            ),
            "style_button_no_border" to json(
                "border" to "none",
            ),
            "style_find_editor_len" to styleTableFindEditLength(),
            "style_find_editor" to json(
                "border" to "1px solid $colorMainBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleTableFindEditorFontSize(),
                "padding" to styleCommonEditorPadding(),
                "margin" to styleCommonMargin(),
            ),
            "style_cursor_box" to json(
                "border" to "none",
                "outline" to "none",
                "background" to "hsla(0,0%,0%,0)",
                "color" to "hsla(0,0%,0%,0)",
            ),
            "style_page_button" to json(
                //--- определяются в зависимости от кол-ва кнопок
                //"width" to styleTablePageButtonWidth(),
                //"font-size" to styleTablePageButtonFontSize()
            ),
            "style_popup_menu_start" to json(
                "z-index" to "2",   // popup menu must be above than table headers
                "position" to "absolute",
                "top" to "20%",
                "bottom" to if (styleIsNarrowScreen) "20%" else "10%",
                "width" to styleMenuWidth(),
                "background" to colorMenuBack,
                "border" to "1px solid $colorMenuBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleMenuFontSize(),
                "padding" to styleMenuStartPadding(),
                "overflow" to "auto",
                "cursor" to "pointer",
            ),
            "style_popup_menu_pos" to json(
            ),
            "style_menu_summary_0" to json(
                "padding" to styleMenuItemPadding_0(),
            ),
            "style_menu_summary_1" to json(
                "padding" to styleMenuItemPadding_1(),
            ),
            "style_menu_item_0" to json(
                "padding" to styleMenuItemPadding_0(),
            ),
            "style_menu_item_1" to json(
                "padding" to styleMenuItemPadding_1(),
            ),
            "style_menu_item_2" to json(
                "padding" to styleMenuItemPadding_2(),
            ),
        ).add(
            tableDataAdd
        )
    }
}

private class TableTitleData(val id: Int, val url: String, val text: String)
private class AddActionButton_(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String
)

private class ServerActionButton_(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val url: String,
    val inNewWindow: Boolean,
    val isForWideScreenOnly: Boolean,
)

private class ClientActionButton_(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val action: String,
    val params: Array<Pair<String, String>>,
    val isForWideScreenOnly: Boolean,
)

private class PageButton(val id: Int, val url: String, val text: String)

private class TableGridData(
    val id: Int,
    val cellType: TableCellType,
    val cellStyle: Json,
    val rowSpan: Int,
    val backColor: String,
    val elementStyle: Json,
    val tooltip: String,

    //--- CHECKBOX
    val booleanValue: Boolean? = null,

    //--- TEXT
    val textCellData: TableTextCellData_? = null,

    //--- BUTTON
    val arrButtonCellData: Array<TableButtonCellData_>? = null,

    //--- общие данные для BUTTON
    val arrGridCellData: Array<Array<TableGridCellData_>>? = null,

    //--- для работы с row data popup menu
    val row: Int
//    var minWidth = 0
) {
    //--- для работы caption-click
    var cellURL = ""
}

private class TableTextCellData_(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
)

private class TableButtonCellData_(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
    val url: String = "",
    val inNewWindow: Boolean = false,
    val style: Json = json(),
)

private class TableGridCellData_(
    val icon: String = "",
    val image: String = "",
    val text: String = "",
//    val url: String = "",
//    val inNewWindow: Boolean = false,
    val style: Json = json(),
)

private class PopupMenuData(
    val url: String,
    val text: String,
    val arrSubMenu: Array<PopupMenuData>? = null,
    val inNewWindow: Boolean,
    val itHover: Boolean = false
)

private fun convertPopupMenuData(arrMenuData: Array<TablePopupData>): Array<PopupMenuData> {
    val alPopupMenuData = mutableListOf<PopupMenuData>()
    var i = 0
    while (i < arrMenuData.size) {
        val menuData = arrMenuData[i]

        if (menuData.group.isEmpty()) {
            alPopupMenuData.add(PopupMenuData(menuData.url, menuData.text, null, menuData.inNewWindow))
            i++
        } else {
            val groupName = menuData.group

            val alPopupSubMenuData = mutableListOf<PopupMenuData>()
            while (i < arrMenuData.size) {
                val subMenuData = arrMenuData[i]
                if (subMenuData.group.isEmpty() || subMenuData.group != groupName) {
                    break
                }

                alPopupSubMenuData.add(PopupMenuData(subMenuData.url, subMenuData.text, null, subMenuData.inNewWindow))
                i++
            }
            alPopupMenuData.add(PopupMenuData("", groupName, alPopupSubMenuData.toTypedArray(), false))
        }
    }
    return alPopupMenuData.toTypedArray()
}

private fun focusToCursorField(tabId: Int) {
    Vue.nextTick {
        val element = document.getElementById("table_cursor_$tabId")
        if (element is HTMLElement) {
            element.focus()
        }
    }
}

private fun setCurrentRow(that: dynamic, rowNo: Int) {
    val arrRowData = that.arrRowData.unsafeCast<Array<TableRowData>>()

    that.isFormButtonVisible = rowNo >= 0 && arrRowData[rowNo].formURL.isNotEmpty()
    that.isGotoButtonVisible = rowNo >= 0 && arrRowData[rowNo].gotoURL.isNotEmpty()
    that.isPopupButtonVisible = rowNo >= 0 && arrRowData[rowNo].alPopupData.isNotEmpty()

    that.currentRow = rowNo
    that.isShowPopupMenu = false
}

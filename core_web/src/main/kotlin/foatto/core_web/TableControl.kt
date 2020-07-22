package foatto.core_web

import foatto.core.app.*
import foatto.core.link.*
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import kotlin.browser.window
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.max

private val hmIcon = mutableMapOf(

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

@Suppress("UnsafeCastFromDynamic")
fun tableControl( appParam: String, tableResponse: TableResponse, tabIndex: Int ) = vueComponentOptions().apply {

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
                           placeholder="Поиск..." 
                           v-bind:size="style_find_editor_len"
                           v-bind:style="style_find_editor"
                           v-on:keyup.enter="doFind( false )"
                    >
                    <img src="/web/images/ic_search_black_48dp.png" 
                         v-on:click="doFind( false )"
                         v-bind:style="[ style_icon_button, style_button_with_border ]"
                         title="Искать"
                    >
                    <img src="/web/images/ic_youtube_searched_for_black_48dp.png" 
                         v-show="findText" 
                         v-bind:style="[ style_icon_button, style_button_with_border, { 'color': '$COLOR_TABLE_FIND_CLEAR' } ]"
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
                    <button v-show="!${styleIsNarrowScreen} || !isFindTextVisible" 
                            v-for="serverButton in arrServerButton"
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
                    <button v-show="!${styleIsNarrowScreen} || !isFindTextVisible" 
                            v-for="clientButton in arrClientButton"
                            v-bind:key="'cb'+clientButton.id"
                            v-bind:style="[ style_icon_button, style_button_with_border ]"
                    >
                        <img v-if="clientButton.icon" v-bind:src="clientButton.icon">
                        <span v-else>
                            {{clientButton.caption}}
                        </span>
                    </button>
                </span>
                
                <span v-bind:style="style_toolbar_block">
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
                                          '$COLOR_TABLE_ROW_SELECTED_BACK' : gridData.backColor ) }
                     ]"
                     v-on:dblclick.prevent="gridData.row >= 0 && arrRowData[ gridData.row ].rowURL ?
                                    invoke( arrRowData[ gridData.row ].rowURL, arrRowData[ gridData.row ].itRowURLInNewWindow ) : null"
                     v-on:click="gridData.cellURL ? invoke( gridData.cellURL, false ) : setCurrentRow( gridData.row )"
                     v-on:click.right.prevent="gridData.row >= 0 ? showPopupMenu( gridData.row, ${'$'}event ) : null"
                >
                    
                    <template v-if="gridData.cellType == '${TableCellType.TEXT}'">
                        <span v-for="cellData in gridData.arrCellData" 
                              v-bind:style="gridData.elementStyle"
                              v-bind:title="gridData.tooltip"
                        >
                            <img v-if="cellData.icon" v-bind:src="cellData.icon">
                            <img v-else-if="cellData.image" v-bind:src="cellData.image">
                            <span v-else v-html="cellData.text">
                            </span>
                        </span>
                    </template>
                    <template v-else-if="gridData.cellType == '${TableCellType.BUTTON}'">
                        <template v-for="cellData in gridData.arrCellData">                    
                            <img v-if="cellData.icon" 
                                 v-bind:src="cellData.icon"
                                 v-bind:style="gridData.elementStyle"
                                 v-bind:title="gridData.tooltip"
                                 v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                            <img v-else-if="cellData.image" 
                                 v-bind:src="cellData.image"
                                 v-bind:style="gridData.elementStyle"
                                 v-bind:title="gridData.tooltip"
                                 v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                            <button v-else 
                                    v-bind:style="gridData.elementStyle"
                                    v-bind:title="gridData.tooltip"
                                    v-on:click="invoke( cellData.url, cellData.inNewWindow )"
                            >
                                <span v-html="cellData.text">
                                </span>
                            </button>
                            <br>
                        </template>
                    </template>
                    <input v-else-if="gridData.cellType == '${TableCellType.CHECKBOX}'" type="checkbox" v-model="gridData.bool" 
                           v-bind:style="gridData.elementStyle"
                           v-bind:title="gridData.tooltip"
                           v-on:click.prevent="isShowPopupMenu = false"
                    >
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

            <ul v-if="isShowPopupMenu"
                v-bind:style="[style_popup_menu_start, style_popup_menu_pos, style_popup_menu_list]"
                v-on:mouseleave="isShowPopupMenu=false"
            >
                <li v-for="( menuData_0, index ) in arrCurPopupData"
                    v-bind:style="[ style_popup_menu_item,
                                    { 'background-color' : ( menuData_0.isHover ? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_BACK' ) },
                                    { 'text-decoration' : ( menuData_0.url || menuData_0.text ? '' : 'line-through' ) },
                                    { 'color' : ( menuData_0.url || menuData_0.text ? '$COLOR_TEXT' : '$COLOR_MENU_DELIMITER' ) }
                                  ]"
                    v-on:click.stop="menuData_0.url ? popupMenuClick( menuData_0 ) : setPopupMenuShow( 0, index )"
                    v-on:mouseenter="menuData_0.text ? menuData_0.isHover=true : menuData_0.isHover=false"
                    v-on:mouseleave="menuData_0.isHover=false">

                    {{ menuData_0.url ? menuData_0.text : ( menuData_0.text ? menuData_0.text + " &gt;" : "$MENU_DELIMITER" ) }}

                    <template v-if="menuData_0.alSubMenu">
                        <ul v-bind:style="style_popup_menu_list" v-show="arrShowMenu[ 0 ][ index ]">

                            <li v-for="( menuData_1, index ) in menuData_0.alSubMenu"
                                v-bind:style="[ style_popup_menu_item,
                                                { 'background-color' : ( menuData_1.isHover ? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_BACK' ) },
                                                { 'text-decoration' : ( menuData_1.url || menuData_1.text ? '' : 'line-through' ) },
                                                { 'color' : ( menuData_1.url || menuData_1.text ? '$COLOR_TEXT' : '$COLOR_MENU_DELIMITER' ) }
                                              ]"
                                v-on:click.stop="menuData_1.url ? popupMenuClick( menuData_1 ) : setPopupMenuShow( 1, index )"
                                v-on:mouseenter="menuData_1.text ? menuData_1.isHover=true : menuData_1.isHover=false"
                                v-on:mouseleave="menuData_1.isHover=false">

                                {{ menuData_1.url ? menuData_1.text : ( menuData_1.text ? menuData_1.text + " &gt;" : "$MENU_DELIMITER" ) }}

                                <template v-if="menuData_1.alSubMenu">
                                    <ul v-bind:style="style_popup_menu_list" v-show="arrShowMenu[ 1 ][ index ]">

                                        <li v-for="( menuData_2, index ) in menuData_1.alSubMenu"
                                            v-bind:style="[ style_popup_menu_item,
                                                            { 'background-color' : ( menuData_2.isHover ? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_BACK' ) },
                                                            { 'text-decoration' : ( menuData_2.url || menuData_2.text ? '' : 'line-through' ) },
                                                            { 'color' : ( menuData_2.url || menuData_2.text ? '$COLOR_TEXT' : '$COLOR_MENU_DELIMITER' ) }
                                                          ]"
                                            v-on:click.stop="menuData_2.url ? popupMenuClick( menuData_2 ) : setPopupMenuShow( 2, index )""
                                            v-on:mouseenter="menuData_2.text ? menuData_2.isHover=true : menuData_2.isHover=false"
                                            v-on:mouseleave="menuData_2.isHover=false">

                                            {{menuData_2.text}}
                                            {{menuData_2.url ? "" : "&gt;&gt;&gt;"}}
                                        </li>
                                    </ul>
                                </template>
                            </li>
                        </ul>
                    </template>
                </li>
            </ul>
        </div>
    """

    this.methods = json(
        "readHeader" to {
            var tabToolTip = ""

            //--- загрузка заголовка таблицы/формы
            var titleID = 0
            val alTitleData = mutableListOf<TableTitleData>()
            for( headerData in tableResponse.alHeader ) {
                val url = headerData.first
                val text = headerData.second

                tabToolTip += ( if( tabToolTip.isEmpty() ) "" else " | " ) + text
                alTitleData.add( TableTitleData( titleID++, url, text ) )

//                //--- запомним последнюю кнопку заголовка в табличном режиме как кнопку отмены или возврата на уровень выше
//                butTableCancel = button
            }
            that().`$root`.addTabInfo( tabIndex, tableResponse.tab, tabToolTip )
            that().arrTitleData = alTitleData.toTypedArray()
        },
        "readAddButtons" to {
            var addButtonID = 0
            val alAddButton = mutableListOf<AddActionButton_>()
            for( aab in tableResponse.alAddActionButton ) {
                val icon = hmIcon[ aab.icon ] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if( aab.icon.isNotBlank() && icon.isBlank() ) aab.icon else aab.tooltip.replace( "\n", "<br>" )
                alAddButton.add( AddActionButton_(
                    id = addButtonID++,
                    caption = caption,
                    tooltip = aab.tooltip,
                    icon = icon,
                    url = aab.url
                ) )
            }
            that().arrAddButton = alAddButton.toTypedArray()
        },
        "readServerButtons" to {
            var serverButtonID = 0
            val alServerButton = mutableListOf<ServerActionButton_>()
            for( sab in tableResponse.alServerActionButton ) {
                val icon = hmIcon[ sab.icon ] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if( sab.icon.isNotBlank() && icon.isBlank() ) sab.icon else sab.caption.replace( "\n", "<br>" )
                alServerButton.add( ServerActionButton_(
                    id = serverButtonID++,
                    caption = caption,
                    tooltip = sab.tooltip,
                    icon = icon,
                    url = sab.url,
                    inNewWindow = sab.inNewWindow
                ) )
            }
            that().arrServerButton = alServerButton.toTypedArray()
        },
        "readClientButtons" to {
            var clientButtonID = 0
            val alClientButton = mutableListOf<ClientActionButton_>()
            for( cab in tableResponse.alClientActionButton ) {
                val icon = hmIcon[ cab.icon ] ?: ""
                //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                val caption = if( cab.icon.isNotBlank() && icon.isBlank() ) cab.icon else cab.caption.replace( "\n", "<br>" )
                alClientButton.add( ClientActionButton_(
                    id = clientButtonID++,
                    caption = caption,
                    tooltip = cab.tooltip,
                    icon = icon,
                    className = cab.className,
                    param = cab.param
                ) )
            }
            that().arrClientButton = alClientButton.toTypedArray()
        },
        "readPageButtons" to {
//            butTablePageUp = null
//            butTablePageDown = null
//            var isEmptyPassed = false
            var pageButtonID = 0
            val alPageButton = mutableListOf<PageButton>()
            //--- вывести новую разметку страниц
            for( value in tableResponse.alPageButton ) {
                val url = value.first
                val text = value.second

                alPageButton.add( PageButton( pageButtonID++, url, text ) )

//                if( url.isEmpty() ) {
//                    isEmptyPassed = true
//                }
//                else {
//                    if( !isEmptyPassed ) butTablePageUp = butPage
//                    if( isEmptyPassed && butTablePageDown == null ) butTablePageDown = butPage
//                }
            }

            val pageButtonStyle = json (
                "width" to styleTablePageButtonWidth( tableResponse.alPageButton.size ),
                "font-size" to styleTablePageButtonFontSize( tableResponse.alPageButton.size )
            )

            that().arrPageButton = alPageButton.toTypedArray()
            that().style_page_button = pageButtonStyle
        },
        "readTable" to {
            val alGridData = mutableListOf<TableGridData>()
            var gridCellID = 0
            var startRow = 0

            //--- заголовки столбцов таблицы
            for( ( index, value ) in tableResponse.alColumnCaption.withIndex() ) {
                val url = value.first
                val text = value.second
                val cellData = TableCellData(
                    text = text
                )
                val captionCell = TableGridData(
                    id = gridCellID++,
                    cellType = TableCellType.TEXT,
                    cellStyle = json (
                        "grid-area" to "${startRow + 1} / ${index + 1} / ${startRow + 2} / ${index + 2}",
                        "justify-self" to "stretch",
                        "align-self" to "stretch",
                        "border-left" to "0.5px solid $COLOR_TAB_BORDER",
                        "border-top" to "none",
                        "border-right" to "0.5px solid $COLOR_TAB_BORDER",
                        "border-bottom" to "1px solid $COLOR_TAB_BORDER",
                        "cursor" to if( url.isBlank() ) "default" else "pointer",
                        "display" to "flex",
                        "justify-content" to "center",
                        "align-items" to "center",
                        "font-size" to styleTableTextFontSize(),
                        "padding" to styleControlPadding()
                    ),
                    elementStyle = json(
                    ),
                    rowSpan = 1,
                    backColor = COLOR_PANEL_BACK,
                    tooltip = if( url.isBlank() ) "" else "Сортировать по этому столбцу",
                    arrCellData = arrayOf( cellData ),
                    bool = false,
                    row = -1            // специальный номер строки, флаг что это заголовок таблицы
                )
                captionCell.cellURL = url
                alGridData.add( captionCell )
            }
            startRow++
            var maxRow = 0
            var maxCol = tableResponse.alColumnCaption.size

            for( tc in tableResponse.alTableCell ) {
                val backColor =
                    when( tc.backColorType.toString() ) {
                        TableCellBackColorType.DEFINED.toString() -> getColorFromInt( tc.backColor )
                        TableCellBackColorType.GROUP_0.toString() -> COLOR_GROUP_BACK_0
                        TableCellBackColorType.GROUP_1.toString() -> COLOR_GROUP_BACK_1
                        else                                      -> if( tc.row % 2 == 0 ) COLOR_TABLE_ROW_0_BACK else COLOR_TABLE_ROW_1_BACK
                    }
                val textColor =
                    when( tc.foreColorType.toString() ) {
                        TableCellForeColorType.DEFINED.toString() -> getColorFromInt( tc.foreColor )
                        else                                      -> COLOR_TEXT
                    }
                val align =
                    when( tc.cellType.toString() ) {
                        TableCellType.BUTTON.toString() -> {
                            "center"
                        }
                        TableCellType.CHECKBOX.toString() -> {
                            "center"
                        }
                        //--- на самом деле нет других вариантов
                        //TableCellType.TEXT.toString() -> {
                        else -> {
                            when( tc.align.toString() ) {
                                TableCellAlign.LEFT.toString()   -> "flex-start"
                                TableCellAlign.CENTER.toString() -> "center"
                                TableCellAlign.RIGHT.toString()  -> "flex-end"
                                else                             -> "center"
                            }
                        }
                    }
                val cellStyle = json (
                    "grid-area" to "${startRow + tc.row + 1} / ${tc.col + 1} / ${startRow + tc.row + 1 + tc.rowSpan} / ${tc.col + 1 + tc.colSpan}",
                    "justify-self" to "stretch",
                    "align-self" to "stretch",
                    //"background" to backColor,
                    //--- пока не будем менять размер вместе с толщиной шрифта (потом сделаем явную передачу увеличения размера шрифта)
                    "font-weight" to ( if( tc.fontStyle == 0 ) "normal" else "bold" ),
                    "padding" to styleControlPadding()
                )
                lateinit var elementStyle: Json
                val alCellData = mutableListOf<TableCellData>()
                when( tc.cellType.toString() ) {
                    TableCellType.TEXT.toString() -> {
                        for( cellData in tc.alCellData ) {
                            val icon = hmIcon[ cellData.icon ] ?: ""
                            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                            var text = if( cellData.icon.isNotBlank() && icon.isBlank() ) cellData.icon else cellData.text.replace( "\n", "<br>" )
                            if( !tc.isWordWrap ) text = text.replace( " ", "&nbsp;" )
                            alCellData.add( TableCellData(
                                icon = icon,
                                image = cellData.image,
                                text = text
                            ) )
                        }
                        cellStyle.add( json(
                            "display" to "flex",
                            "justify-content" to align,
                            "align-items" to "center"
                        ) )
                        elementStyle = json(
                            "color" to textColor,
                            "font-size" to styleTableTextFontSize(),
                            "user-select" to if( styleIsTouchScreen() ) "none" else "auto"
                        )
                    }
                    TableCellType.BUTTON.toString() -> {
                        var isIcon = false
                        for( cellData in tc.alCellData ) {
                            val icon = hmIcon[ cellData.icon ] ?: ""
                            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
                            val text = if( cellData.icon.isNotBlank() && icon.isBlank() ) cellData.icon else cellData.text.replace( "\n", "<br>" )
                            alCellData.add( TableCellData(
                                icon = icon,
                                image = cellData.image,
                                text = text,
                                url = cellData.url,
                                inNewWindow = cellData.inNewWindow
                            ) )
                            isIcon = icon.isNotBlank()
                        }
                        elementStyle = json(
                            "border" to "1px solid $COLOR_TAB_BORDER",
                            "border-radius" to BORDER_RADIUS,
                            "background" to COLOR_BUTTON_BACK,
                            "color" to if( isIcon ) COLOR_TABLE_BUTTON else textColor,
                            "font-size" to styleCommonButtonFontSize(),
                            "padding" to styleTextButtonPadding(),
                            "cursor" to "pointer"
                        )
                    }
                    //--- на самом деле нет других вариантов
                    //TableCellType.CHECKBOX.toString() -> {
                    else -> {
                        cellStyle.add( json(
                            "display" to "flex",
                            "justify-content" to align,
                            "align-items" to "center"
                        ) )
                        elementStyle = json(
                            "color" to textColor,
                            "transform" to styleControlCheckBoxTransform()
                        )
                    }
                }
                alGridData.add( TableGridData(
                    id = gridCellID++,
                    cellType = tc.cellType,
                    cellStyle = cellStyle,
                    elementStyle = elementStyle,
                    rowSpan = tc.rowSpan,
                    backColor = backColor,
                    tooltip = tc.tooltip,
                    arrCellData = alCellData.toTypedArray(),
                    bool = tc.booleanValue,
                    row = tc.row
                ) )
                maxRow = max( maxRow, startRow + tc.row + tc.rowSpan )
                maxCol = max( maxCol, tc.col + tc.colSpan )
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
            that().arrRowData = tableResponse.alTableRowData
        },
        "doFind" to { isClear: Boolean ->
            val isFindTextVisible = that().isFindTextVisible.unsafeCast<Boolean>()

            if( isClear ) that().findText = ""

            if( !isClear && !isFindTextVisible ) {
                that().isFindTextVisible = true
            }
            else {
                val findURL = that().findURL.unsafeCast<String>()
                val findText = that().findText.unsafeCast<String>()

                that().`$parent`.invoke( AppRequest( action = findURL, find = findText.trim() ) )
            }
        },
        "doForm" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if( currentRow >= 0 && arrRowData[ currentRow ].formURL.isNotEmpty() )
                that().invoke( arrRowData[ currentRow ].formURL, false )
        },
        "doGoto" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if( currentRow >= 0 && arrRowData[ currentRow ].gotoURL.isNotEmpty() )
                that().invoke( arrRowData[ currentRow ].gotoURL, arrRowData[ currentRow ].itGotoURLInNewWindow )
        },
        "doPopup" to {
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            val currentRow = that().currentRow.unsafeCast<Int>()

            //--- проверка лишней не будет
            if( currentRow >= 0 && arrRowData[ currentRow ].alPopupData.isNotEmpty() )
                that().showPopupMenu( currentRow, null )
        },
        "setCurrentRow" to { rowNo: Int ->
            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            
            that().isFormButtonVisible = rowNo >= 0 && arrRowData[ rowNo ].formURL.isNotEmpty()
            that().isGotoButtonVisible = rowNo >= 0 && arrRowData[ rowNo ].gotoURL.isNotEmpty()
            that().isPopupButtonVisible = rowNo >= 0 && arrRowData[ rowNo ].alPopupData.isNotEmpty()

            that().currentRow = rowNo
            that().isShowPopupMenu = false
        },
//--- grid не focusable и клавиши на нём не отрабатывают
//        "doKeyUp" to {
//            var currentRow = that().currentRow.unsafeCast<Int>()
//            if( currentRow > 0 ) {
//                currentRow--
//                that().setCurrentRow( currentRow )
//            }
//        },
//        "doKeyDown" to {
//            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
//            var currentRow = that().currentRow.unsafeCast<Int>()
//            if( currentRow < arrRowData.size - 1 ) {
//                currentRow++
//                that().setCurrentRow( currentRow )
//            }
//        },
        "invoke" to { newAppParam: String, inNewWindow: Boolean ->
            if( inNewWindow )
                that().`$root`.openTab( newAppParam )
            else
                that().`$parent`.invoke( AppRequest( action = newAppParam ) )
        },
        "showPopupMenu" to { row: Int, event: Event ->
            //--- чтобы строчка выделялась и по правой кнопке мыши тоже
            that().setCurrentRow( row )

            val mouseEvent = event as? MouseEvent
            val mouseX = mouseEvent?.pageX ?: window.innerWidth.toDouble() / 3

            val arrRowData = that().arrRowData.unsafeCast<Array<TableRowData>>()
            if( arrRowData[ row ].alPopupData.isNotEmpty() ) {
                var menuSide = ""
                var menuPos = ""
                if( styleIsNarrowScreen ) {
                    menuSide = "left"
                    menuPos = "5%"
                }
                else if( mouseX <= window.innerWidth / 2 ) {
                    menuSide = "left"
                    menuPos = "${mouseX}px"
                }
                else {
                    menuSide = "right"
                    menuPos = "${window.innerWidth - mouseX}px"
                }

                that().arrCurPopupData = convertPopupMenuData( arrRowData[ row ].alPopupData )

                val arrShowMenu = Array( 5 ) { BooleanArray( 100 ) { false } }
                that().arrShowMenu = arrShowMenu

                //--- в данной ситуации clientX/Y == pageX/Y, offsetX/Y идёт от текущего элемента (ячейки таблицы), screenX/Y - от начала экрана
                that().style_popup_menu_pos = json( menuSide to menuPos )
                that().isShowPopupMenu = true
            }
            else that().isShowPopupMenu = false
        },
        "popupMenuClick" to { menuData: PopupMenuData ->
            that().isShowPopupMenu = false
            that().invoke( menuData.url, menuData.inNewWindow )
        },
        "setPopupMenuShow" to { level: Int, index: Int ->
            val oldValue = that().arrShowMenu[ level ][ index ].unsafeCast<Boolean>()
            that().arrShowMenu[ level ].splice( index, 1, !oldValue )
        }

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
        that().setCurrentRow( tableResponse.selectedRow )

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        that().`$root`.curAppParam = appParam

//        onRequestFocus()
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

            "arrRowData" to arrayOf<TableRowData>(),
            "arrCurPopupData" to null,
            "isShowPopupMenu" to false,
            "arrShowMenu" to "[]",
            "currentRow" to -1,

            "style_table" to json(
                "flex-grow" to 1,
                "flex-shrink" to 1,
                "display" to "flex",
                "flex-direction" to "column",
                "height" to "100%" 
            ),
            "style_header" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "center",
                "align-items" to "center",        // "baseline" ?
                "border-top" to if( !styleIsNarrowScreen ) "none" else "1px solid $COLOR_BUTTON_BORDER",
                "padding" to styleControlPadding(),
                "background" to COLOR_PANEL_BACK
            ),
            "style_toolbar" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "space-between",
                "align-items" to "center",
                "border-bottom" to "1px solid $COLOR_BUTTON_BORDER",
                "padding" to styleControlPadding(),
                "background" to COLOR_PANEL_BACK
            ),
            "style_toolbar_block" to json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "nowrap",
                "justify-content" to "center",
                "align-items" to "center"
            ),
            "style_pagebar" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "center",
                "align-items" to "center",
                "border-top" to "1px solid $COLOR_BUTTON_BORDER",
                "padding" to styleTablePageBarPadding(),
                "background" to COLOR_PANEL_BACK
            ),
            "style_title" to json (
                "font-size" to styleControlTitleTextFontSize(),
                "padding" to styleControlTitlePadding()
            ),
            "style_icon_button" to json(
                "background" to COLOR_BUTTON_BACK,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleIconButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer"
            ),
            "style_text_button" to json(
                "background" to COLOR_BUTTON_BACK,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleTextButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer"
            ),
            "style_button_with_border" to json (
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS
            ),
            "style_button_no_border" to json (
                "border" to "none"
            ),
            "style_find_editor_len" to styleTableFindEditLength(),
            "style_find_editor" to json (
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleTableFindEditorFontSize(),
                "padding" to styleCommonEditorPadding(),
                "margin" to styleCommonMargin()
            ),
            "style_page_button" to json (
                //--- определяются в зависимости от кол-ва кнопок
                //"width" to styleTablePageButtonWidth(),
                //"font-size" to styleTablePageButtonFontSize()
            ),
            "style_popup_menu_start" to json(
                "position" to "absolute",
                "border" to "1px solid $COLOR_MENU_BORDER",
                "border-radius" to BORDER_RADIUS,
                "top" to "20%",
                "bottom" to if( styleIsNarrowScreen ) "20%" else "10%",
                "width" to styleMenuWidth(),
                "overflow" to "auto",
                "font-size" to styleMenuFontSize(),
                "padding" to styleMenuStartPadding(),
                "cursor" to "pointer"
            ),
            "style_popup_menu_pos" to json(
            ),
            "style_popup_menu_list" to json(
                "list-style-type" to "none",
                "background" to COLOR_MENU_BACK
            ),
            "style_popup_menu_item" to json(
                //"padding-right" to "1rem" - появляется лишняя вертикальная жёлтая полоса на всё подменю
                "padding" to styleMenuItemPadding()
            )
        )
    }
}

private class TableTitleData( val id: Int, val url: String, val text: String )
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
    val inNewWindow: Boolean
)
private class ClientActionButton_(
    val id: Int,
    val caption: String,
    val tooltip: String,
    val icon: String,
    val className: String,
    val param: String
)
private class PageButton( val id: Int, val url: String, val text: String )
private class TableGridData(
    val id: Int,
    val cellType: TableCellType,
    val cellStyle: Json,
    val rowSpan: Int,
    val backColor: String,
    val elementStyle: Json,
    val tooltip: String,

    //--- общие данные для TEXT / BUTTON
    val arrCellData: Array<TableCellData>,

    //--- CHECKBOX
    val bool: Boolean,

    //--- для работы с row data popup menu
    val row: Int
//    var minWidth = 0
) {
    //--- для работы caption-click
    var cellURL = ""
}

private class PopupMenuData(
    val url: String,
    val text: String,
    val alSubMenu: Array<PopupMenuData>? = null,
    val inNewWindow: Boolean,
    val isHover: Boolean = false
)

private fun convertPopupMenuData( arrMenuData: Array<TablePopupData> ): Array<PopupMenuData> {
    val alPopupMenuData = mutableListOf<PopupMenuData>()
    var i = 0
    while( i < arrMenuData.size ) {
        val menuData = arrMenuData[ i ]

        if( menuData.group.isEmpty() ) {
            alPopupMenuData.add( PopupMenuData( menuData.url, menuData.text, null, menuData.inNewWindow ) )
            i++
        }
        else {
            val groupName = menuData.group

            val alPopupSubMenuData = mutableListOf<PopupMenuData>()
            while( i < arrMenuData.size ) {
                val subMenuData = arrMenuData[ i ]
                if( subMenuData.group.isEmpty() || subMenuData.group != groupName ) break

                alPopupSubMenuData.add( PopupMenuData( subMenuData.url, subMenuData.text, null, subMenuData.inNewWindow ) )
                i++
            }
            alPopupMenuData.add( PopupMenuData( "", groupName, alPopupSubMenuData.toTypedArray(), false ) )
        }
    }
    return alPopupMenuData.toTypedArray()
}

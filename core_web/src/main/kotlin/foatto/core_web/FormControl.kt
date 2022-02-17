package foatto.core_web

import foatto.core.app.*
import foatto.core.link.AppRequest
import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.FormPinMode
import foatto.core.link.FormResponse
import foatto.core.util.getRandomInt
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.max

private val hmFormIcon = mutableMapOf(
    ICON_NAME_ARCHIVE to "/web/images/ic_archive_black_48dp.png",
    ICON_NAME_DELETE to "/web/images/ic_delete_forever_black_48dp.png",
    ICON_NAME_EXIT to "/web/images/ic_exit_to_app_black_48dp.png",
    ICON_NAME_FILE to "/web/images/ic_attachment_black_48dp.png",
    ICON_NAME_GRAPHIC to "/web/images/ic_timeline_black_48dp.png",
    ICON_NAME_MAP to "/web/images/ic_language_black_48dp.png",
    ICON_NAME_PRINT to "/web/images/ic_print_black_48dp.png",
    ICON_NAME_SAVE to "/web/images/ic_save_black_48dp.png",
    ICON_NAME_STATE to "/web/images/ic_router_black_48dp.png",
    ICON_NAME_UNARCHIVE to "/web/images/ic_unarchive_black_48dp.png",
    ICON_NAME_VIDEO to "/web/images/ic_play_circle_outline_black_48dp.png"
)

@Suppress("UnsafeCastFromDynamic")
fun formControl(formResponse: FormResponse, tabId: Int) = vueComponentOptions().apply {

    this.template = """
        <div v-bind:style="style_form">

            <div v-bind:style="style_header">
                <template v-for="titleData in arrTitleData">
                    <button v-if="titleData.url"
                            v-on:click="invoke( titleData.url, false )"
                            v-bind:key="'fhb_'+$tabId+'_'+titleData.id"
                            v-bind:style="style_text_button">

                        {{ titleData.text }}
                    </button>
                    <span v-else v-bind:key="'fhs_'+$tabId+'_'+titleData.id" v-bind:style="style_title">
                        {{ titleData.text }}
                    </span>
                </template>
            </div>

            <div v-bind:style="style_grid">
                <div v-for="gridData in arrGridData"
                     v-bind:key="'gd_'+$tabId+'_'+gridData.id"
                     v-bind:style="gridData.style"
                     v-if="!gridData.itHidden"
                     v-show="gridData.itVisible"
                >
                    <span v-if="gridData.cellType == '${FormCellType_.LABEL}'"
                          v-bind:style="style_form_row_label"
                          v-html="gridData.text"
                    >
                    </span>

                    <input v-else-if="gridData.cellType == '${FormCellType_.STRING}'"
                           v-bind:type="gridData.inputType"
                           v-model="gridData.text"
                           v-bind:id="'i_'+$tabId+'_'+gridData.id"
                           v-bind:size="gridData.colCount"
                           v-bind:readonly="gridData.itReadOnly"
                           v-bind:style="style_form_row_input"
                           v-on:focus="selectAllText( ${'$'}event )"
                           v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                           v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                           v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                           v-on:keyup.f4="closeTabById()"
                    >

                    <textarea v-else-if="gridData.cellType == '${FormCellType_.TEXT}'"
                              v-model="gridData.text"
                              v-bind:id="'i_'+$tabId+'_'+gridData.id"
                              v-bind:rows="gridData.rowCount"
                              v-bind:cols="gridData.colCount"
                              v-bind:readonly="gridData.itReadOnly"
                              v-bind:style="style_form_row_input"
                              v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                              v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                              v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                              v-on:keyup.f4="closeTabById()"
                    >
                    </textarea>

                    <input v-else-if="gridData.cellType == '${FormCellType_.CHECKBOX}'"
                           type="checkbox"
                           v-model="gridData.bool"
                           v-bind:id="'i_'+$tabId+'_'+gridData.id"
                           v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
                           v-bind:readonly="gridData.itReadOnly"
                           v-bind:style="style_form_row_checkbox"
                           v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                           v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                           v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                           v-on:keyup.f4="closeTabById()"
                    >

                    <template v-else-if="gridData.cellType == '${FormCellType_.SWITCH}'">
                            <button v-on:click="gridData.itReadOnly || !gridData.bool ? null : doVisibleAndCaptionChange( gridData )"
                                    v-bind:readonly="gridData.itReadOnly"
                                    v-bind:style="gridData.bool ? style_form_switch_off : style_form_switch_on"
                                    title="gridData.arrSwitchText[0]"                                                                        
                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                    v-on:keyup.f4="closeTabById()"
                            >
                                {{ gridData.arrSwitchText[0] }}
                            </button>
                            <button v-on:click="gridData.itReadOnly || gridData.bool ? null : doVisibleAndCaptionChange( gridData )"
                                    v-bind:id="'i_'+$tabId+'_'+gridData.id"
                                    v-bind:readonly="gridData.itReadOnly"
                                    v-bind:style="gridData.bool ? style_form_switch_on : style_form_switch_off"
                                    title="gridData.arrSwitchText[1]"                                                                        
                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                    v-on:keyup.f4="closeTabById()"
                            >
                                {{ gridData.arrSwitchText[1] }}
                            </button>
                    </template>

                    <template v-else-if="gridData.cellType == '${FormCellType_.DATE}' || gridData.cellType == '${FormCellType_.TIME}' || gridData.cellType == '${FormCellType_.DATE_TIME}'">
                        <input type="text"
                               v-for="(_, index) in gridData.arrDateTime"
                               v-model="gridData.arrDateTime[ index ]"
                               v-bind:id="'i_'+$tabId+'_'+gridData.id+'_'+gridData.arrSubId[index]"
                               v-bind:size="index == 2 && gridData.cellType != '${FormCellType_.TIME}' ? 2 : 1"
                               v-bind:readonly="gridData.itReadOnly"
                               v-bind:style="style_form_row_input"
                               v-on:keyup.enter.exact="doNextFocus( gridData.id, gridData.arrSubId[index] )"
                               v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                               v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                               v-on:keyup.f4="closeTabById()"
                        >
                    </template>

                    <template v-else-if="gridData.cellType == '${FormCellType_.COMBO}'">
                        <select v-model="gridData.combo"
                                v-bind:id="'i_'+$tabId+'_'+gridData.id"
                                v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
                                v-bind:readonly="gridData.itReadOnly"
                                v-bind:style="style_form_row_combo"
                                v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                                v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                v-on:keyup.f4="closeTabById()"
                        >
                            <option v-for="comboData in gridData.arrComboData"
                                    v-bind:value="comboData.value"
                            >
                                {{ comboData.text }}
                            </option>
                        </select>
                    </template>

                    <template v-else-if="gridData.cellType == '${FormCellType_.RADIO}'">
                        <template v-for="(comboData, index) in gridData.arrComboData">
                            <input type="radio"
                                   v-model="gridData.combo"
                                   v-bind:id="'i_'+$tabId+'_'+gridData.id+'_'+gridData.arrSubId[index]"
                                   v-bind:value="comboData.value"
                                   v-on:change="gridData.itReadOnly ? null : doVisibleAndCaptionChange( gridData )"
                                   v-bind:readonly="gridData.itReadOnly"
                                   v-bind:style="style_form_row_radio_button"
                                   v-on:keyup.enter.exact="doNextFocus( gridData.id, gridData.arrSubId[index] )"
                                   v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                   v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                   v-on:keyup.f4="closeTabById()"
                            >
                            <span v-bind:style="style_form_row_radio_label"
                            >
                                {{comboData.text}}
                            </span>
                            <br>
                        </template>
                    </template>

                    <template v-else-if="gridData.cellType == '${FormCellType_.FILE}'">
                        <div v-for="fileData in gridData.arrFileData"
                             v-bind:style="style_form_row_file"
                        >
                            <button v-on:click="fileData.id < 0 ? null : showFile( fileData.url )"
                                    v-bind:readonly="fileData.id < 0"
                                    v-bind:style="style_row_file_name_button"
                                    title="Показать файл"                                                                        
                                    v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                                    v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                    v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                    v-on:keyup.f4="closeTabById()"
                            >
                                <span v-html="fileData.text">
                                </span>
                            </button>
                            <img src="/web/images/ic_delete_forever_black_48dp.png"
                                 v-if="!gridData.itReadOnly"
                                 v-on:click="deleteFile( gridData, fileData )"
                                 v-bind:style="style_icon_button"
                                 title="Удалить файл"
                            >
                        </div>
                        <br>
                        <input id="fileInput"
                               type="file"
                               multiple
                               style="display:none;"
                               v-on:change="addFile( gridData, ${'$'}event )"
                        >
                        <button v-if="!gridData.itReadOnly"
                                v-on:click="addFileDialog('fileInput')"
                                v-bind:style="style_row_file_name_button"
                                title="Добавить файл(ы)"                                                                        
                                v-on:keyup.enter.exact="doNextFocus( gridData.id, -1 )"
                                v-on:keyup.enter.ctrl.exact="formSaveURL ? invoke( formSaveURL, true ) : null"
                                v-on:keyup.esc.exact="formExitURL ? invoke( formExitURL, false ) : null"
                                v-on:keyup.f4="closeTabById()"
                        >
                            Добавить файл(ы)
                        </button>
                    </template>
                    
                    <img src="/web/images/ic_reply_black_48dp.png"
                         v-if="gridData.selectorSetURL"
                         v-on:click="invoke( gridData.selectorSetURL, true )"
                         v-bind:style="style_icon_button"
                         title="Выбрать из справочника"
                    >

                    <img src="/web/images/ic_delete_forever_black_48dp.png"
                         v-if="gridData.selectorClearURL"
                         v-on:click="invoke( gridData.selectorClearURL, true )"
                         v-bind:style="style_icon_button"
                         title="Очистить выбор"
                    >

                    <div v-if="gridData.error"
                         v-bind:style="style_form_row_error"
                    >
                        {{ gridData.error }}
                    </div>
                </div>
            </div>

            <div v-bind:style="style_button_bar">
                <template v-for="formButton in arrFormButton">
                    <img v-if="formButton.icon" 
                         v-bind:src="formButton.icon"
                         v-on:click="invoke( formButton.url, formButton.withNewData )"
                         v-bind:key="'fb_'+$tabId+'_'+formButton.id"
                         v-bind:style="style_icon_button"
                         v-bind:title="formButton.tooltip"
                    >
                    <span v-else>
                        {{formButton.text}}
                    </span>
                </template>
            </div>

        </div>
    """
    this.methods = json(
        "readHeader" to {
            var tabToolTip = ""

            //--- загрузка заголовка таблицы/формы
            var titleID = 0
            val alTitleData = mutableListOf<FormTitleData>()
            for (headerData in formResponse.alHeader) {
                val url = headerData.first
                val text = headerData.second

                tabToolTip += (if (tabToolTip.isEmpty()) "" else " | ") + text
                alTitleData.add(FormTitleData(titleID++, url, text))
            }
            that().`$root`.setTabInfo(tabId, formResponse.tab, tabToolTip)
            that().arrTitleData = alTitleData.toTypedArray()
        },
        "selectAllText" to { event: Event ->
            //--- программный селект текста на тачскринах вызывает показ надоедливого окошка с копированием/вырезанием текста (и так на каждый input)
            if (!styleIsTouchScreen()) {
                (event.target as? HTMLInputElement)?.select()
            }
        },
        "closeTabById" to {
            that().`$root`.closeTabById(tabId)
        },
        "doVisibleAndCaptionChange" to { gdMaster: FormGridData ->
            val that = that()
            if (gdMaster.cellType == FormCellType_.SWITCH) {
                //--- из-за применения onlick
                Vue.nextTick {
                    //--- manual switch because button realization instead standart checkbox
                    gdMaster.bool = !gdMaster.bool
                    doVisibleAndCaptionChangeBody(that, gdMaster)
                }
            } else {
                doVisibleAndCaptionChangeBody(that, gdMaster)
            }
        },
        "showFile" to { url: String ->
            that().`$root`.openTab(url)
        },
        "deleteFile" to { gridData: FormGridData, fileData: FormFileData ->
            gridData.arrFileData = gridData.arrFileData!!.filter { it.id != fileData.id }.toTypedArray()
            //--- сохраним ID удаляемого файла для передачи на сервер
            if (fileData.id > 0) {
                gridData.alFileRemovedID.add(fileData.id)
            }
            //--- или просто удалим ранее добавленный файл из списка
            else {
                gridData.hmFileAdd.remove(fileData.id)
            }
        },
        "addFileDialog" to { id: String ->
            (document.getElementById(id) as HTMLElement).click()
        },
        "addFile" to { gridData: FormGridData, event: Event ->
            val files = (event.target as? HTMLInputElement)?.files
            files?.let {
                val alFileData = gridData.arrFileData!!.toMutableList()
                val hmFileAdd = gridData.hmFileAdd

                val formData = org.w3c.xhr.FormData().also {
                    for (file in files.asList()) {
                        val id = -getRandomInt()

                        alFileData.add(FormFileData(id, "", file.name))
                        hmFileAdd[id] = file.name

                        it.append("form_file_ids", id.toString())
                        it.append("form_file_blobs", file)
                    }
                }

                invokeUploadFormFile(formData)

                gridData.arrFileData = alFileData.toTypedArray()
            }
        },
        "doNextFocus" to { gridDataId: Int, gridDataSubId: Int ->
            val arrGridData = that().arrGridData.unsafeCast<Array<FormGridData>>()

            val curIndex = arrGridData.indexOfFirst { formGridData ->
                formGridData.id == gridDataId
            }

            if (curIndex >= 0) {
                val curGridData = arrGridData[curIndex]

                var nextGridId = -1
                var nextSubGridId = -1

                //--- try set focus to next sub-field into fields group (date/time-textfields or radio-buttons)
                curGridData.arrSubId?.let { arrSubId ->
                    val curSubIndex = arrSubId.indexOf(gridDataSubId)
                    if (curSubIndex >= 0) {
                        if (curSubIndex < arrSubId.lastIndex) {
                            nextGridId = gridDataId
                            nextSubGridId = arrSubId[curSubIndex + 1]
                        }
                    }
                }
                //--- else try set focus to next field or first sub-field in next field
                if(nextGridId == -1 && nextSubGridId == -1) {
                    if (curIndex < arrGridData.lastIndex) {
                        var nextIndex = curIndex + 1
                        //--- search non-label element
                        while(nextIndex <= arrGridData.lastIndex && arrGridData[nextIndex].cellType == FormCellType_.LABEL) {
                            nextIndex++
                        }
                        val nextGridData = arrGridData[nextIndex]
                        nextGridId = nextGridData.id
                        nextSubGridId = nextGridData.arrSubId?.firstOrNull() ?: -1
                    }
                }

                val nextFocusId = if (nextSubGridId < 0) {
                    "i_${tabId}_${nextGridId}"
                } else {
                    "i_${tabId}_${nextGridId}_${nextSubGridId}"
                }

//                Vue.nextTick {
                    val element = document.getElementById(nextFocusId)
                    if (element is HTMLElement) {
                        element.focus()
                    }
//                }
            }
        },
        "invoke" to { formAppParam: String, withNewData: Boolean ->
            val arrGridData = that().arrGridData.unsafeCast<Array<FormGridData>>()
            val alFormData = mutableListOf<FormData>()

            arrGridData.forEach { gridData ->
                val withNewValues = withNewData && !gridData.itHidden

                when (gridData.cellType) {
                    FormCellType_.STRING -> {
                        alFormData.add(FormData(stringValue = if (withNewValues) gridData.text else gridData.oldText))
                    }
                    FormCellType_.TEXT -> {
                        alFormData.add(FormData(textValue = if (withNewValues) gridData.text else gridData.oldText))
                    }
                    FormCellType_.CHECKBOX, FormCellType_.SWITCH -> {
                        alFormData.add(FormData(booleanValue = if (withNewValues) gridData.bool else gridData.oldBool))
                    }
                    FormCellType_.DATE, FormCellType_.TIME, FormCellType_.DATE_TIME -> {
                        alFormData.add(FormData(alDateTimeValue = (if (withNewValues) gridData.arrDateTime else gridData.oldArrDateTime)!!.toList()))
                    }
                    FormCellType_.COMBO -> {
                        alFormData.add(FormData(comboValue = if (withNewValues) gridData.combo else gridData.oldCombo))
                    }
                    FormCellType_.RADIO -> {
                        alFormData.add(FormData(comboValue = if (withNewValues) gridData.combo else gridData.oldCombo))
                    }
                    FormCellType_.FILE -> {
                        alFormData.add(FormData(
                            fileId = gridData.fileID,
                            hmFileAdd = if (withNewValues) {
                                gridData.hmFileAdd.mapKeys { it.key.toString() }
                            } else {
                                mapOf()
                            },
                            alFileRemovedId = if (withNewValues) gridData.alFileRemovedID else listOf()
                        ))
                    }
                }
            }
            that().`$parent`.invoke(AppRequest(action = formAppParam, alFormData = alFormData))
        }
    )

    this.mounted = {
        that().readHeader()

        var columnNo = 0       // счётчик столбцов grid-формы
        var columnIndex = 0
        var rowIndex = 0
        val hmFormCellMaster = mutableMapOf<String, Int>()
        val hmFormCellVisible = mutableMapOf<Int, MutableList<FormCellVisibleInfo>>()
        val hmFormCellCaption = mutableMapOf<Int, MutableList<FormCellCaptionInfo>>()
        var autoFocusId: String? = null
        val alFormCellMasterPreAction = mutableListOf<FormGridData>()
        val alGridData = mutableListOf<FormGridData>()
        var autoClickURL: String? = null

        rowIndex = getGridFormCaptions(formResponse.alFormColumn, rowIndex, 0, alGridData)
        //--- с запасом отдадим предыдущую сотню id-шек на построение grid-заголовков
        var gridCellID = 100

        var maxColumnCount = 0
        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormGridData(
                    formCell = formCell,
                    gridCellID = gridCellID,
                    itHidden = true
                )

                alGridData.add(formGridData)
                when (formCell.cellType.toString()) {
                    FormCellType.BOOLEAN.toString() -> {
                        hmFormCellMaster[formCell.booleanName] = gridCellID
                        hmFormCellVisible[gridCellID] = mutableListOf()
                        hmFormCellCaption[gridCellID] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }
                    FormCellType.COMBO.toString() -> {
                        hmFormCellMaster[formCell.comboName] = gridCellID
                        hmFormCellVisible[gridCellID] = mutableListOf()
                        hmFormCellCaption[gridCellID] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }
                    FormCellType.RADIO.toString() -> {
                        hmFormCellMaster[formCell.comboName] = gridCellID
                        hmFormCellVisible[gridCellID] = mutableListOf()
                        hmFormCellCaption[gridCellID] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }
                }

                gridCellID++
                continue
            }

            val alSlaveID = mutableListOf<Int>()

            //--- обычная форма: начинаем новую строку
            if (formResponse.alFormColumn.isEmpty()) {
                columnIndex = 0
                if (formCell.formPinMode.toString() == FormPinMode.ON.toString() ||
                    formCell.formPinMode.toString() == FormPinMode.AUTO.toString() && !formCell.itEditable && formCell.selectorSetURL.isEmpty()
                ) {

                    rowIndex++
                } else {
                    //--- добавим разделитель для отвязки от предыдущего блока полей ввода
                    rowIndex++

                    val emptyCell = FormGridData(
                        id = gridCellID,
                        cellType = FormCellType_.LABEL,
                        style = json(
                            "grid-area" to "${rowIndex + 1} / 1 / ${rowIndex + 2} / 2"
                        ),
                        aText = "<br>"
                    )

                    alGridData.add(emptyCell)
                    alSlaveID.add(gridCellID)
                    gridCellID++

                    rowIndex++
                }
            }

            //--- если это обычная форма или первое поле в строке GRID-формы, то добавляем левый заголовок поля
            var captionGridCell: FormGridData? = null
            if (formResponse.alFormColumn.isEmpty() || columnNo == 0) {
                val isWideOrGrid = !styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()
                captionGridCell = FormGridData(
                    id = gridCellID,
                    cellType = FormCellType_.LABEL,
                    style = json(
                        "grid-area" to "${rowIndex + 1} / ${columnIndex + 1} / ${rowIndex + 2} / ${columnIndex + 2}",
                        "justify-self" to if (isWideOrGrid) "flex-end" else "flex-start",
                        "align-self" to "center",
                        "padding-left" to if (isWideOrGrid) "0" else styleFormLabelPadding(),
                        "padding-right" to if (isWideOrGrid) styleFormRowPadding() else "0",
                        "padding-top" to if (isWideOrGrid) styleFormRowTopBottomPadding() else styleFormLabelPadding(),
                        "padding-bottom" to styleFormRowTopBottomPadding()
                    ),
                    aText = formCell.caption
                )

                alGridData.add(captionGridCell)

                alSlaveID.add(gridCellID)
                gridCellID++
                //--- если это широкий экран или строка GRID-формы
                if (!styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()) columnIndex++ else rowIndex++
            }

            val formGridData = getFormGridData(
                formCell = formCell,
                gridCellID = gridCellID,
                itHidden = false,
                rowIndex = rowIndex,
                columnIndex = columnIndex,
                isGridForm = formResponse.alFormColumn.isNotEmpty()
            )
            //--- на тачскринах автофокус только бесит автоматическим включением клавиатуры
            if (!styleIsTouchScreen()) {
                //--- set autofocus from server
                if (formCell.itAutoFocus) {
                    formGridData.arrSubId?.let { arrSubId ->
                        autoFocusId = "i_${tabId}_${formGridData.id}_${arrSubId[0]}"
                    } ?: run {
                        autoFocusId = "i_${tabId}_${formGridData.id}"
                    }
                }
                //--- automatic autofocus setting
                else if (autoFocusId == null && !formGridData.itReadOnly) {
                    formGridData.arrSubId?.let { arrSubId ->
                        autoFocusId = "i_${tabId}_${formGridData.id}_${arrSubId[0]}"
                    } ?: run {
                        autoFocusId = "i_${tabId}_${formGridData.id}"
                    }
                }
            }
            alGridData.add(formGridData)

            //--- проверка на изначальную пустоту поля-автоселектора
            //--- (по умолчанию оно заполнено, чтобы не запустить автоселектор на непроверяемых полях)
            var isEmptyFieldValue = false
            when (formCell.cellType.toString()) {
                FormCellType.STRING.toString(), FormCellType.INT.toString(), FormCellType.DOUBLE.toString() -> {
                    isEmptyFieldValue = formCell.value.isEmpty()
                }
                FormCellType.TEXT.toString() -> {
                    isEmptyFieldValue = formCell.textValue.isEmpty()
                }
                FormCellType.BOOLEAN.toString() -> {
                    hmFormCellMaster[formCell.booleanName] = gridCellID
                    hmFormCellVisible[gridCellID] = mutableListOf()
                    hmFormCellCaption[gridCellID] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }
                FormCellType.DATE.toString(), FormCellType.TIME.toString(), FormCellType.DATE_TIME.toString() -> {
                    isEmptyFieldValue = formCell.alDateTimeField[0].second.isEmpty()
                }
                FormCellType.COMBO.toString() -> {
                    hmFormCellMaster[formCell.comboName] = gridCellID
                    hmFormCellVisible[gridCellID] = mutableListOf()
                    hmFormCellCaption[gridCellID] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }
                FormCellType.RADIO.toString() -> {
                    hmFormCellMaster[formCell.comboName] = gridCellID
                    hmFormCellVisible[gridCellID] = mutableListOf()
                    hmFormCellCaption[gridCellID] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }
            }
            alSlaveID.add(gridCellID)
            gridCellID++

            columnNo++

            //--- если это широкий экран или строка GRID-формы
            if (!styleIsNarrowScreen || formResponse.alFormColumn.isNotEmpty()) {
                columnIndex++
            } else {
                rowIndex++
            }

            //--- если это последнее поле в строке GRID-формы, то добавляем правый заголовок поля
            if (formResponse.alFormColumn.isNotEmpty() && columnNo == formResponse.columnCount) {
                alGridData.add(
                    FormGridData(
                        id = gridCellID++,
                        cellType = FormCellType_.LABEL,
                        style = json(
                            "grid-area" to "${rowIndex + 1} / ${columnIndex + 1} / ${rowIndex + 2} / ${columnIndex + 2}",
                            "justify-self" to "flex-start",
                            "align-self" to "center"
                        ),
                        aText = formCell.caption
                    )
                )

                //columnIndex++     // здесь нет смысла

                columnNo = 0   // новая строка GRID-формы
                columnIndex = 0
                rowIndex++
            }

            //--- автостарт селектора запускается, только если поле данных пустое,
            //-- иначе зациклимся на старте
            if (formCell.itAutoStartSelector && isEmptyFieldValue && autoClickURL == null) {
                autoClickURL = formGridData.selectorSetURL
            }

            //--- определим visible-зависимости
            for (v in formCell.alVisible) {
                val name = v.first
                val state = v.second
                val value = v.third

                val masterID = hmFormCellMaster[name]!!
                for (slaveID in alSlaveID) {
                    hmFormCellVisible[masterID]!!.add(FormCellVisibleInfo(state, value, slaveID))
                }
            }

            //--- определим caption-зависимости
            if (captionGridCell != null)
                for (c in formCell.alCaption) {
                    val name = c.first
                    val str = c.second
                    val value = c.third

                    val masterID = hmFormCellMaster[name]!!
                    hmFormCellCaption[masterID]!!.add(FormCellCaptionInfo(str, value, captionGridCell.id))
                }

            maxColumnCount = max(maxColumnCount, columnIndex)
        }
        rowIndex = getGridFormCaptions(formResponse.alFormColumn, rowIndex + 1, gridCellID, alGridData)

        var buttonID = 0
        val alFormButton = mutableListOf<FormButtonData>()
        formResponse.alFormButton.forEach {
            val icon = hmFormIcon[it.iconName] ?: ""
            //--- если иконка задана, но её нет в локальном справочнике, то выводим её имя (для диагностики)
            val caption = if (it.iconName.isNotBlank() && icon.isBlank()) it.iconName else it.caption

            alFormButton.add(
                FormButtonData(
                    id = buttonID++,
                    url = it.url,
                    withNewData = it.withNewData,
                    icon = icon,
                    text = caption,
                    tooltip = it.caption
                )
            )
            //--- назначение кнопок на горячие клавиши
            when (it.key) {
                BUTTON_KEY_AUTOCLICK -> if (autoClickURL == null) autoClickURL = it.url
                BUTTON_KEY_SAVE -> that().formSaveURL = it.url
                BUTTON_KEY_EXIT -> that().formExitURL = it.url
            }
        }

        that().arrGridData = alGridData.toTypedArray()
        //--- если это грид-форма, то сделать одинаковые ячейки, если обычная - поделить между caption и edit в пропорции 1fr и 2fr
        that().style_grid = json(
            "flex-grow" to 1,
            "flex-shrink" to 1,
            "height" to "100%", //- необязательно
            "overflow" to "auto",
            "display" to "grid",
            "grid-template-rows" to "repeat(${rowIndex + 1},max-content)",
            "grid-template-columns" to "repeat($maxColumnCount,auto)",
            "background" to colorMainBack1
        )
        //--- перепакуем внутренние списки в массивы
        that().hmFormCellVisible = hmFormCellVisible.mapValues { entry -> entry.value.toTypedArray() }
        //--- перепакуем внутренние списки в массивы
        that().hmFormCellCaption = hmFormCellCaption.mapValues { entry -> entry.value.toTypedArray() }

        that().arrFormButton = alFormButton.toTypedArray()

        //--- начальные установки видимости и caption-зависимостей
        for (gridData in alFormCellMasterPreAction) {
            doVisibleAndCaptionChangeBody(that(), gridData)
        }
        if (autoClickURL != null) {
            that().invoke(autoClickURL, true)
        } else if (autoFocusId != null) {
            Vue.nextTick {
                val element = document.getElementById(autoFocusId!!)
                if (element is HTMLElement) {
                    element.focus()
                }
            }
        }
    }

    this.data = {
        json(
            "arrTitleData" to arrayOf<FormTitleData>(),
            "arrGridData" to arrayOf<FormGridData>(),
            "hmFormCellVisible" to null,
            "hmFormCellCaption" to null,
            "arrFormButton" to arrayOf<FormButtonData>(),
            "formSaveURL" to "",
            "formExitURL" to "",
            "style_grid" to "",

            "style_form" to json(
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
                "border-bottom" to "1px solid $colorMainBorder",
                "padding" to styleControlPadding(),
                "background" to colorMainBack1,
            ),
            "style_button_bar" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to if (!styleIsNarrowScreen) "center" else "space-between",
                "align-items" to "center",
                "border-top" to "1px solid $colorMainBorder",
                "padding" to styleControlPadding(),
                "background" to colorMainBack1,
            ),
            "style_title" to json(
                "font-size" to styleControlTitleTextFontSize(),
                "padding" to styleControlTitlePadding(),
            ),
            "style_icon_button" to json(
                "background" to colorButtonBack,
                "border" to "1px solid $colorButtonBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleIconButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer",
            ),
            "style_text_button" to json(
                "background" to colorButtonBack,
                "border" to "1px solid $colorButtonBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleTextButtonPadding(),
                "margin" to styleCommonMargin(),
                "cursor" to "pointer",
            ),
            "style_form_row_label" to json(
                "font-size" to styleControlTextFontSize(),
            ),
            "style_form_row_input" to json(
                "background" to COLOR_MAIN_BACK_0,
                "border" to "1px solid $colorMainBorder",
                "border-radius" to BORDER_RADIUS_SMALL,
                "font-size" to styleControlTextFontSize(),
                "padding" to styleCommonEditorPadding(),
                "margin" to styleCommonMargin(),
            ),
            "style_form_row_checkbox" to json(
                "transform" to styleControlCheckBoxTransform(),
                "margin" to styleFormCheckboxAndRadioMargin(),
            ),
            "style_form_switch_off" to json(
                "background" to colorMainBack1,
                "border" to "1px solid $colorMainBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleFileNameButtonPadding(),
                "cursor" to "pointer",
            ),
            "style_form_switch_on" to json(
                "background" to COLOR_FORM_SWITCH_BACK_ON,
                "border" to "1px solid $colorMainBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleFileNameButtonPadding(),
                "cursor" to "pointer",
            ),
            "style_form_row_combo" to json(
                "font-size" to styleControlTextFontSize(),
                "padding" to styleCommonEditorPadding(),
                "margin" to styleCommonMargin(),
            ),
            "style_form_row_radio_button" to json(
                "transform" to styleControlRadioTransform(),
                "margin" to styleFormCheckboxAndRadioMargin(),
            ),
            "style_form_row_radio_label" to json(
                "font-size" to styleControlTextFontSize(),
            ),
            "style_form_row_file" to json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "justify-content" to "flex-start",
                "align-items" to "center",
            ),
            "style_row_file_name_button" to json(
                "background" to colorButtonBack,
                "border" to "1px solid $colorButtonBorder",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleFileNameButtonPadding(),
                "margin" to styleFileNameButtonMargin(),
                "cursor" to "pointer",
            ),
            "style_form_row_error" to json(
                "color" to "red",
                "font-size" to styleControlTextFontSize(),
                "font-weight" to "bold",
                "margin" to styleCommonMargin(),
            )
        )
    }
}

private fun getGridFormCaptions(alFormColumn: Array<String>, aRowIndex: Int, aGridCellID: Int, alGridData: MutableList<FormGridData>): Int {
    var rowIndex = aRowIndex
    var gridCellID = aGridCellID
    //--- верхние/нижние заголовки столбцов GRID-формы
    if (alFormColumn.isNotEmpty()) {
        var columnIndex = 1
        for (caption in alFormColumn) {
            alGridData.add(
                FormGridData(
                    id = gridCellID++,
                    cellType = FormCellType_.LABEL,
                    style = json(
                        "grid-area" to "${rowIndex + 1} / ${columnIndex + 1} / ${rowIndex + 2} / ${columnIndex + 2}",
                        "justify-self" to "center",
                        "align-self" to "center",
                        "padding" to styleFormRowPadding()
                    ),
                    aText = caption
                )
            )
            columnIndex++
        }
        rowIndex++
    }
    return rowIndex
}

private fun getFormGridData(formCell: FormCell, gridCellID: Int, itHidden: Boolean, rowIndex: Int = 0, columnIndex: Int = 0, isGridForm: Boolean = false): FormGridData {
    val style = json(
        "grid-area" to "${rowIndex + 1} / ${columnIndex + 1} / ${rowIndex + 2} / ${columnIndex + 2}",
        "justify-self" to if (isGridForm) "center" else "flex-start",
        "align-self" to "center",
        "padding-left" to if (isGridForm) "0" else styleFormRowPadding(),
        "padding-right" to "0",
        "padding-top" to if (!styleIsNarrowScreen || isGridForm) styleFormRowTopBottomPadding() else styleFormRowPadding(),
        "padding-bottom" to if (!styleIsNarrowScreen || isGridForm) styleFormRowTopBottomPadding() else styleFormRowPadding(),
    )
    //--- добавляем отдельно только для тех, у кого есть select-кнопки,
    //--- иначе разъезжаются radio-buttons в одну строчку (а не в один столбец)
    if (formCell.selectorSetURL.isNotBlank()) {
        style.add(
            json(
                "display" to "flex",
                "flex-direction" to "row",
                "flex-wrap" to "wrap",
                "align-items" to "center",
            )
        )
    }

    val gridData = when (formCell.cellType.toString()) {
        FormCellType.STRING.toString(), FormCellType.INT.toString(), FormCellType.DOUBLE.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = FormCellType_.STRING,
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aText = formCell.value,
                inputType = if (formCell.itPassword) {
                    "password"
                } else {
                    "text"
                },
                colCount = styleFormEditBoxColumn(formCell.column),
                itReadOnly = !formCell.itEditable,
            )
        }
        FormCellType.TEXT.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = FormCellType_.TEXT,
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aText = formCell.textValue,
                rowCount = formCell.textRow,
                colCount = styleFormEditBoxColumn(formCell.textColumn),
                itReadOnly = !formCell.itEditable,
            )
        }
        FormCellType.BOOLEAN.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = if (formCell.arrSwitchText.isEmpty()) {
                    FormCellType_.CHECKBOX
                } else {
                    FormCellType_.SWITCH
                },
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aBool = formCell.booleanValue,
                itReadOnly = !formCell.itEditable,
                arrSwitchText = formCell.arrSwitchText,
            )
        }
        FormCellType.DATE.toString(), FormCellType.TIME.toString(), FormCellType.DATE_TIME.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = when (formCell.cellType) {
                    FormCellType.DATE -> {
                        FormCellType_.DATE
                    }
                    FormCellType.TIME -> {
                        FormCellType_.TIME
                    }
                    else -> {
                        FormCellType_.DATE_TIME
                    }
                },
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aArrDateTime = formCell.alDateTimeField.map { it.second }.toTypedArray(),
                itReadOnly = !formCell.itEditable,
            ).apply {
                val alSubId = mutableListOf<Int>()
                (0 until arrDateTime!!.size).forEach { i ->
                    alSubId += i
                }
                arrSubId = alSubId.toTypedArray()
            }
        }
        FormCellType.COMBO.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = FormCellType_.COMBO,
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aCombo = formCell.comboValue,
                arrComboData = formCell.alComboData.map { FormComboData(it.first, it.second) }.toTypedArray(),
                itReadOnly = !formCell.itEditable,
            )
        }
        FormCellType.RADIO.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = FormCellType_.RADIO,
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                aCombo = formCell.comboValue,
                arrComboData = formCell.alComboData.map { FormComboData(it.first, it.second) }.toTypedArray(),
                itReadOnly = !formCell.itEditable,
            ).apply {
                val alSubId = mutableListOf<Int>()
                (0 until arrComboData!!.size).forEach { i ->
                    alSubId += i
                }
                arrSubId = alSubId.toTypedArray()
            }
        }
        FormCellType.FILE.toString() -> {
            FormGridData(
                id = gridCellID,
                cellType = FormCellType_.FILE,
                style = style,
                itHidden = itHidden,
                error = formCell.errorMessage,
                fileID = formCell.fileID,
                arrFileData = formCell.alFile.map { FormFileData(it.first, it.second, it.third) }.toTypedArray(),
            )
        }
        //--- недогадливость (ошибка) парсера/компилятора из-за использования enum.toString() - на самом деле больше нет вариантов
        else -> {
            println("ERROR: Call when-else-block in getFormGridData!")
            FormGridData(
                id = 0,
                cellType = FormCellType_.LABEL,
                style = style,
                itHidden = true, // от греха подальше :)
                error = formCell.errorMessage,
            )
        }
    }
    gridData.selectorSetURL = formCell.selectorSetURL
    gridData.selectorClearURL = formCell.selectorClearURL

    return gridData
}

private fun doVisibleAndCaptionChangeBody(that: dynamic, gdMaster: FormGridData) {
    //--- определение контрольного значения
    val controlValue =
        when (gdMaster.cellType) {
            FormCellType_.CHECKBOX, FormCellType_.SWITCH -> {
                if (if (gdMaster.itHidden) gdMaster.oldBool else gdMaster.bool) {
                    1
                } else {
                    0
                }
            }
            FormCellType_.COMBO -> if (gdMaster.itHidden) {
                gdMaster.oldCombo
            } else {
                gdMaster.combo
            }
            FormCellType_.RADIO -> if (gdMaster.itHidden) {
                gdMaster.oldCombo
            } else {
                gdMaster.combo
            }
            else -> 0
        }

    val arrGridData = that.arrGridData.unsafeCast<Array<FormGridData>>()
    val hmFormCellVisible = that.hmFormCellVisible.unsafeCast<Map<Int, Array<FormCellVisibleInfo>>>()
    val hmFormCellCaption = that.hmFormCellCaption.unsafeCast<Map<Int, Array<FormCellCaptionInfo>>>()

    val alFCVI = hmFormCellVisible[gdMaster.id]
    alFCVI?.forEach { fcvi ->
        val gdSlave = arrGridData.find { gd -> gd.id == fcvi.id }
        gdSlave?.itVisible = fcvi.state == fcvi.hsValue.contains(controlValue)
    }

    val alFCCI = hmFormCellCaption[gdMaster.id]
    alFCCI?.forEach { fcci ->
        val gdSlave = arrGridData.find { gd -> gd.id == fcci.id }
        if (gdSlave != null && fcci.hsValue.contains(controlValue)) {
            gdSlave.text = fcci.caption
        }
    }
}

private enum class FormCellType_ { LABEL, STRING, TEXT, CHECKBOX, SWITCH, DATE, TIME, DATE_TIME, COMBO, RADIO, FILE }

private class FormTitleData(val id: Int, val url: String, val text: String)

private class FormGridData(
    val id: Int,

    val cellType: FormCellType_,
    val style: Json,

    val itHidden: Boolean = false,
    var itVisible: Boolean = true,
    val error: String = "",

    aText: String = "",
    aBool: Boolean = false,
    aArrDateTime: Array<String>? = null,
    aCombo: Int = 0,

    val inputType: String = "text",  // password

    val colCount: Int = 20,
    val rowCount: Int = 1,
    val itReadOnly: Boolean = false,

    val arrSwitchText: Array<String>? = null,
    val arrComboData: Array<FormComboData>? = null,

    val fileID: Int = 0,
    var arrFileData: Array<FormFileData>? = null
) {
    var arrSubId: Array<Int>? = null

    var text: String = aText
    val oldText: String = aText

    var bool: Boolean = aBool
    val oldBool: Boolean = aBool

    val arrDateTime: Array<String>? = aArrDateTime?.copyOf()
    val oldArrDateTime: Array<String>? = aArrDateTime?.copyOf()

    val combo: Int = aCombo
    val oldCombo: Int = aCombo

    var selectorSetURL: String? = null
    var selectorClearURL: String? = null

    val hmFileAdd = mutableMapOf<Int, String>()
    val alFileRemovedID = mutableListOf<Int>()
}

private class FormComboData(val value: Int, val text: String)

private class FormFileData(val id: Int, val url: String, val text: String)

private class FormCellVisibleInfo(val state: Boolean, val hsValue: Array<Int>, val id: Int)
private class FormCellCaptionInfo(val caption: String, val hsValue: Array<Int>, val id: Int)

private class FormButtonData(
    val id: Int,
    val url: String,
    val withNewData: Boolean,
    val icon: String,
    val text: String,
    val tooltip: String
)

/*
пока не понятно, как сделать в веб-версии
        if( formCell.alComboString.isNotEmpty() ) {
            fci.comboBox = ComboBox()
            fci.comboBox.getItems().addAll( formCell.alComboString )
            //--- надо в самом конце инициализации combo-box'a, иначе не срабатывает!
            fci.comboBox.getSelectionModel().select( formCell.value )
            fci.comboBox.setDisable( !formCell.itEditable )
            fci.comboBox.setEditable( true )
            if( fci.comboBox.getEditor() != null ) fci.comboBox.getEditor().font = curFont
            if( fci.comboBox.getButtonCell() != null ) fci.comboBox.getButtonCell().font = curFont

            //fci.comboBox.setOnAction(  this  ); - это свободно-редактируемый combo-box, cell-action на его выбор не предусмотрено
            fci.comboBox.setOnKeyPressed( this )

            GridPane.setHalignment( fci.comboBox, HPos.LEFT )
            GridPane.setValignment( fci.comboBox, VPos.CENTER )

            if( focusableComponent == null && formCell.itEditable ) focusableComponent = fci.comboBox

            return fci.comboBox
        }

 */


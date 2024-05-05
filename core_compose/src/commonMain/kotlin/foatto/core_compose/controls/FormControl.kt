package foatto.core_compose.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import foatto.core.app.BUTTON_KEY_AUTOCLICK
import foatto.core.app.BUTTON_KEY_EXIT
import foatto.core.app.BUTTON_KEY_SAVE
import foatto.core.link.AppRequest
import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.FormPinMode
import foatto.core.link.FormResponse
import foatto.core_compose.AppControl
import foatto.core_compose.Root
import foatto.core_compose.composables.ImageOrTextFromName
import foatto.core_compose.controls.models.TitleData
import foatto.core_compose.controls.models.form.FormButtonData
import foatto.core_compose.controls.models.form.FormCellCaptionInfo
import foatto.core_compose.controls.models.form.FormCellTypeClient
import foatto.core_compose.controls.models.form.FormCellVisibleInfo
import foatto.core_compose.controls.models.form.FormComboData
import foatto.core_compose.controls.models.form.FormFileData
import foatto.core_compose.controls.models.form.FormGridData
import foatto.core_compose.getStyleOtherIconNameSuffix
import foatto.core_compose.invokeUploadFormFile
import foatto.core_compose.styleOtherIconSize
import foatto.core_compose.utils.maxDp
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class FormControl(
    private val root: Root,
    private val appControl: AppControl,
    private val formResponse: FormResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val FORM_CELL_PADDING = 16.dp
    }

    private val hmHiddenCells = mutableMapOf<Int, FormGridData>()
    private val alGridRows = mutableStateListOf<MutableList<FormGridData?>>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val hmFormCellVisible = mutableMapOf<Int, MutableList<FormCellVisibleInfo>>()
    private val hmFormCellCaption = mutableMapOf<Int, MutableList<FormCellCaptionInfo>>()

    private val alFormButton = mutableListOf<FormButtonData>()

    private var formSaveUrl = ""
    private var formExitUrl = ""

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var isFileAddInProgress by mutableStateOf(false)
    private var fileGridData: FormGridData? = null

    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        var isFilePickerVisible by remember { mutableStateOf(false) }

        FilePicker(
            isFilePickerVisible,
        ) { platformFile ->
            isFilePickerVisible = false

            if (platformFile != null) {
                isFileAddInProgress = true
                coroutineScope.launch {
                    invokeUploadFormFile(fileGridData!!, listOf(platformFile)) {
                        isFileAddInProgress = false
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getTableAndFormHeader(true) { url: String ->
                call(url, false, null)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(state = verticalScrollState)
                    .horizontalScroll(state = horizontalScrollState)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                horizontalScrollState.scrollBy(-delta)
                            }
                        },
                    )
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            coroutineScope.launch {
                                verticalScrollState.scrollBy(-delta)
                            }
                        },
                    )
                    .weight(1.0f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                for (alGridRow in alGridRows) {
                    Row(
                        modifier = Modifier
                            //.width(intrinsicSize = IntrinsicSize.Max) - оставлю на память, чтобы опять не искать, но пользы/разницы не увидел
                            //--- чтобы ячейки занимали всю отведённую им (общую) высоту (работает только совместно с .fillMaxHeight())
                            .height(intrinsicSize = IntrinsicSize.Max),
//?                        horizontalArrangement = Arrangement.aligned(Alignment.CenterHorizontally),
                    ) {
                        alGridRow.forEachIndexed { col, gridData ->
//!!!                                if (!gridData.isVisible.value) {
//                                    hidden()
//                                }
                            Row(
                                horizontalArrangement = gridData?.align ?: Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxHeight()    // работает только совместно с .height(intrinsicSize = IntrinsicSize.Max)
                                    //!!! только на время отладки
                                    .border(BorderStroke(width = 1.dp, color = Color.LightGray))
                                    //.wrapContentSize(align = Alignment.Center, unbounded = true)  //- ничем не помогает
                                    .onSizeChanged { size ->
                                        gridData?.let {
                                            val componentWidth = with(density) {
                                                maxDp(size.width.toDp(), FORM_CELL_PADDING * 2)    //!!! minWidth
                                            }
                                            gridData.componentWidth = maxDp(gridData.componentWidth, componentWidth)
                                            hmColumnMaxWidth[col] = maxDp(hmColumnMaxWidth[col] ?: 0.dp, componentWidth)
                                        }
                                    }
                                    .then(
                                        gridData?.let {
                                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                                if (gridData.componentWidth < maxColWidth) {
                                                    Modifier.width(maxColWidth)
                                                } else {
                                                    Modifier.width(gridData.componentWidth)
                                                }
                                            }
                                        } ?: Modifier
                                    )

                            ) {
                                when (gridData?.cellType) {
                                    FormCellTypeClient.LABEL -> {
                                        Text(
                                            modifier = Modifier.padding(FORM_CELL_PADDING),
                                            text = gridData.text,
                                        )
//                                            if (gridData.text.value == BR) {
//                                                Br()
//                                            } else {
//                                                Text(gridData.text.value)
//                                            }
                                    }

                                    FormCellTypeClient.STRING -> {
                                        OutlinedTextField(
                                            modifier = Modifier.padding(FORM_CELL_PADDING),
                                            value = gridData.text,
                                            onValueChange = { newText ->
                                                gridData.text = newText
                                            },
                                            readOnly = gridData.isReadOnly,
                                            //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                            isError = gridData.error.isNotEmpty(),
                                            supportingText = if (gridData.error.isNotEmpty()) {
                                                {
                                                    Text(
                                                        text = gridData.error,
                                                        color = Color.Red,
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            singleLine = true,
                                            visualTransformation = if (gridData.isPassword) {
                                                PasswordVisualTransformation()
                                            } else {
                                                VisualTransformation.None
                                            },
                                        )
//                                            id(getFocusId(gridData.id))
//                                            size(gridData.colCount)
//                                            onFocusIn { syntheticFocusEvent ->
//                                                selectAllText(syntheticFocusEvent)
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.TEXT -> {
                                        OutlinedTextField(
                                            modifier = Modifier.padding(FORM_CELL_PADDING),
                                            value = gridData.text,
                                            onValueChange = { newText ->
                                                gridData.text = newText
                                            },
                                            readOnly = gridData.isReadOnly,
                                            //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                            isError = gridData.error.isNotEmpty(),
                                            supportingText = if (gridData.error.isNotEmpty()) {
                                                {
                                                    Text(
                                                        text = gridData.error,
                                                        color = Color.Red,
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            singleLine = false,
                                            minLines = gridData.rowCount,
                                            maxLines = gridData.rowCount,
                                        )
//                                            id(getFocusId(gridData.id))
//                                            cols(gridData.colCount)
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.CHECKBOX -> {
                                        Checkbox(
                                            modifier = Modifier.padding(FORM_CELL_PADDING),
                                            checked = gridData.bool,
                                            onCheckedChange = { newState ->
                                                if (!gridData.isReadOnly) {
                                                    gridData.bool = newState
                                                    doVisibleAndCaptionChange(gridData)
                                                }
                                            },
                                        )
//                                            id(getFocusId(gridData.id))
                                        //onInput { syntheticInputEvent -> - тоже можно
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.SWITCH -> {
                                        //!!! заменить на настоящий switch
                                        Checkbox(
                                            modifier = Modifier.padding(FORM_CELL_PADDING),
                                            checked = gridData.bool,
                                            onCheckedChange = { newState ->
                                                if (!gridData.isReadOnly) {
                                                    gridData.bool = newState
                                                    doVisibleAndCaptionChange(gridData)
                                                }
                                            },
                                        )
//                                                id(getFocusId(gridData.id, 0))
//                                                onClick {
//                                                    if (!(gridData.isReadOnly || !gridData.bool.value)) {
//                                                        gridData.bool.value = !gridData.bool.value
//                                                        doVisibleAndCaptionChange(gridData)
//                                                    }
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
//                                            Text(gridData.alSwitchText[0])

//                                                id(getFocusId(gridData.id, 1))
//                                                title(gridData.alSwitchText[1])
//                                                onClick {
//                                                    if (!(gridData.isReadOnly || gridData.bool.value)) {
//                                                        gridData.bool.value = !gridData.bool.value
//                                                        doVisibleAndCaptionChange(gridData)
//                                                    }
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
//                                            Text(gridData.alSwitchText[1])
                                    }

                                    FormCellTypeClient.DATE, FormCellTypeClient.TIME, FormCellTypeClient.DATE_TIME -> {
                                        for (index in 0 until gridData.alDateTime.size) {
                                            //--- time fields on new line on narrow screens
//                                            if (styleIsNarrowScreen && index == 3) {
//                                                Br()
//                                            }
                                            val isYearField = index == 2 && gridData.cellType != FormCellTypeClient.TIME
                                            OutlinedTextField(
                                                modifier = Modifier
                                                    .padding(FORM_CELL_PADDING)
                                                    .width(
                                                        if (isYearField) {
                                                            76.dp   // после ввода пятой (лишней) цифры первая наполовину уходит
                                                        } else {
                                                            58.dp   // после ввода третьей (лишней) цифры первая наполовину уходит
                                                        }
                                                    ),
                                                value = gridData.alDateTime[index].value,
                                                onValueChange = { newText ->
                                                    gridData.alDateTime[index].value = newText
                                                },
                                                readOnly = gridData.isReadOnly,
                                                //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                                isError = gridData.error.isNotEmpty(),
                                                supportingText = if (isYearField && gridData.error.isNotEmpty()) {
                                                    {
                                                        Text(
                                                            text = gridData.error,
                                                            color = Color.Red,
                                                        )
                                                    }
                                                } else {
                                                    null
                                                },
                                                singleLine = true,
                                            )
//                                                id(getFocusId(gridData.id, gridData.alSubId?.get(index)))
//                                                onFocusIn { syntheticFocusEvent ->
//                                                    selectAllText(syntheticFocusEvent)
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
                                        }
                                    }

                                    FormCellTypeClient.COMBO -> {
//                                                id(getFocusId(gridData.id))
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
                                        var isExpanded by remember { mutableStateOf(false) }
                                        var textFieldSize by remember { mutableStateOf(Size.Zero) }

                                        OutlinedTextField(
                                            modifier = Modifier
                                                .padding(FORM_CELL_PADDING)
                                                .onGloballyPositioned { coordinates ->
                                                    textFieldSize = coordinates.size.toSize()
                                                },
                                            value = gridData.alComboData.find { fcb ->
                                                fcb.value == gridData.combo
                                            }?.text ?: "(неизвестное значение)",
                                            onValueChange = {},
                                            readOnly = gridData.isReadOnly,
                                            //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                            isError = gridData.error.isNotEmpty(),
                                            supportingText = if (gridData.error.isNotEmpty()) {
                                                {
                                                    Text(
                                                        text = gridData.error,
                                                        color = Color.Red,
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                            singleLine = true,
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = if (isExpanded) {
                                                        Icons.Filled.KeyboardArrowUp
                                                    } else {
                                                        Icons.Filled.KeyboardArrowDown
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                                                )
                                            },
                                        )
                                        DropdownMenu(
                                            modifier = Modifier
                                                .offset(
                                                    //!!! x = FORM_CELL_PADDING, - почему-то не срабатывает, даже если включить внутри Box или Column
                                                    //y = +- FORM_CELL_PADDING, - бесполезно, т.к. меню может вылезти как снизу, так и сверху
                                                )
                                                .width(
                                                    with(density) {
                                                        textFieldSize.width.toDp()
                                                    }
                                                ),
                                            expanded = isExpanded,
                                            onDismissRequest = { isExpanded = false },
                                        ) {
                                            for (comboData in gridData.alComboData) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(text = comboData.text)
                                                    },
                                                    onClick = {
                                                        gridData.combo = comboData.value
                                                        isExpanded = false
//                                                            doVisibleAndCaptionChange(gridData)
                                                    },
                                                    //contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                                                )
                                            }
                                        }
                                    }

                                    FormCellTypeClient.RADIO -> {
                                        Column(
                                            modifier = Modifier.selectableGroup(),
                                        ) {
                                            for (comboData in gridData.alComboData) {
                                                Row(
                                                    modifier = Modifier.selectable(
                                                        selected = comboData.value == gridData.combo,
                                                        onClick = {
                                                            if (!gridData.isReadOnly) {
                                                                gridData.combo = comboData.value
//                                                            doVisibleAndCaptionChange(gridData)
                                                            }
                                                        }
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    RadioButton(
                                                        selected = comboData.value == gridData.combo,
                                                        onClick = {
                                                            if (!gridData.isReadOnly) {
                                                                gridData.combo = comboData.value
//                                                            doVisibleAndCaptionChange(gridData)
                                                            }
                                                        }
                                                    )
                                                    Text(
                                                        text = comboData.text,
                                                    )
                                                }
                                            }
                                        }
//                                            gridData.alComboData.forEachIndexed { index, comboData ->
//                                                val focusId = getFocusId(gridData.id, gridData.alSubId?.get(index))
//                                                    id(focusId)
//                                                    onKeyUp { syntheticKeyboardEvent ->
//                                                        doKeyUp(gridData, syntheticKeyboardEvent)
//                                                    }
//                                                }
                                    }

                                    FormCellTypeClient.FILE -> {
                                        Column {
                                            for (fileData in gridData.alFileData) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Button(
                                                        enabled = fileData.id >= 0,
                                                        shape = RoundedCornerShape(0.dp),
                                                        onClick = {
                                                            if (fileData.id >= 0) {
                                                                showFile(fileData.url)
                                                            }
                                                        },
                                                    ) {
                                                        Text(text = fileData.text)
                                                    }
//                                                            backgroundColor(getColorButtonBack())
//                                                            setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                                            fontSize(styleCommonButtonFontSize)
//                                                            doKeyUp(gridData, syntheticKeyboardEvent)
                                                    if (!gridData.isReadOnly) {
                                                        IconButton(
                                                            onClick = {
                                                                if (!isFileAddInProgress) {
                                                                    deleteFile(gridData, fileData)
                                                                }
                                                            }
                                                        ) {
                                                            ImageOrTextFromName("/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png", "Удалить файл", styleOtherIconSize)
                                                        }
//                                                                backgroundColor(getColorFormButtonBack())
//                                                                setBorder(getStyleFormButtonBorder())
//                                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                                    }
                                                }
                                            }
                                        }
                                        if (!gridData.isReadOnly) {
                                            IconButton(
                                                enabled = !isFileAddInProgress,
//                                                        backgroundColor(getColorButtonBack())
//                                                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                                                onClick = {
                                                    fileGridData = gridData
                                                    isFilePickerVisible = true
                                                }
//                                                    onKeyUp { syntheticKeyboardEvent ->
//                                                        doKeyUp(gridData, syntheticKeyboardEvent)
//                                                    }
                                            ) {
                                                ImageOrTextFromName("/web/images/ic_add_${getStyleOtherIconNameSuffix()}.png", "Добавить файл", styleOtherIconSize)
                                            }
                                        }
//                                            id(getFocusId(gridData.id))
                                    }

                                    else -> {}
                                }

                                gridData?.let {
                                    if (gridData.selectorSetURL.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                if (!isFileAddInProgress) {
                                                    call(gridData.selectorSetURL, true, null)
                                                }
                                            }
                                        ) {
                                            ImageOrTextFromName("/web/images/ic_reply_${getStyleOtherIconNameSuffix()}.png", "Выбрать из справочника", styleOtherIconSize)
                                        }
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                    }

                                    if (gridData.selectorClearURL.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                if (!isFileAddInProgress) {
                                                    call(gridData.selectorClearURL, true, null)
                                                }
                                            }
                                        ) {
                                            ImageOrTextFromName("/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png", "Очистить выбор", styleOtherIconSize)
                                        }
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //--- Form Button Bar
//                        backgroundColor(getColorFormBack())
//                        borderTop(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
//                        padding(styleControlPadding)
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
//                            if (!styleIsNarrowScreen) {
//                                JustifyContent.Center
//                            } else {
//                                JustifyContent.SpaceBetween
//                            }
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (formButton in alFormButton) {
                    IconButton(
                        modifier = Modifier.padding(1.dp),
                        enabled = !isFileAddInProgress,
                        onClick = {
                            if (!isFileAddInProgress) {
                                call(formButton.url, formButton.withNewData, formButton.question)
                            }
                        }
                    ) {
                        ImageOrTextFromName(formButton.name, formButton.tooltip, styleOtherIconSize)
//!!!                                    backgroundColor(
//                                        if (formButton.withNewData) {
//                                            getColorFormActionButtonSaveBack()
//                                        } else {
//                                            getColorFormActionButtonOtherBack()
//                                        }
//                                    )
                    }
                }
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        readHeader()
        readForm()
    }

    private fun readHeader() {
        var tabToolTip = ""

        alTitleData.clear()
        for ((url, text) in formResponse.alHeader) {
            tabToolTip += (if (tabToolTip.isEmpty()) "" else " | ") + text
            alTitleData.add(TitleData(url, text))
        }
        root.setTabInfo(tabId, formResponse.tab, tabToolTip)
    }

    private fun readForm() {
        hmHiddenCells.clear()
        alGridRows.clear()

        var columnNo = 0       // счётчик столбцов grid-формы
        var columnIndex = 0
        var rowIndex = 0
        val hmFormCellMaster = mutableMapOf<String, Int>()
        var autoFocusId: String? = null
        val alFormCellMasterPreAction = mutableListOf<FormGridData>()
        var autoClickUrl: String? = null

        rowIndex = getGridFormTopBottomCaptions(formResponse.alFormColumn, rowIndex, 0)
        //--- с запасом отдадим предыдущую сотню id-шек на построение grid-заголовков
        var gridCellId = 100

        var maxColumnCount = 0
        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormDataCell(
                    formCell = formCell,
                    gridCellId = gridCellId,
                )
                hmHiddenCells[formGridData.id] = formGridData

                when (formCell.cellType) {
                    FormCellType.BOOLEAN -> {
                        hmFormCellMaster[formCell.booleanName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    FormCellType.COMBO -> {
                        hmFormCellMaster[formCell.comboName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    FormCellType.RADIO -> {
                        hmFormCellMaster[formCell.comboName] = gridCellId
                        hmFormCellVisible[gridCellId] = mutableListOf()
                        hmFormCellCaption[gridCellId] = mutableListOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    else -> {}
                }

                gridCellId++
                continue
            }

            val alSlaveId = mutableListOf<Int>()

            //--- обычная форма: начинаем новую строку
            if (formResponse.alFormColumn.isEmpty()) {
                columnIndex = 0
                if (formCell.formPinMode == FormPinMode.ON ||
                    formCell.formPinMode == FormPinMode.AUTO && !formCell.isEditable && formCell.selectorSetURL.isEmpty()
                ) {
                    rowIndex++
                } else {
                    //--- добавим разделитель для отвязки от предыдущего блока полей ввода
                    rowIndex++
//                    val emptyCell = getFormEmptyCell(gridCellId, rowIndex)

//                    alGridData.add(emptyCell)
                    alSlaveId.add(gridCellId++)

                    rowIndex++
                }
            }

            //--- если это обычная форма или первое поле в строке GRID-формы, то добавляем левый заголовок поля
            var captionGridCell: FormGridData? = null
            if (formResponse.alFormColumn.isEmpty() || columnNo == 0) {
                captionGridCell = getFormLabelCell(formCell, gridCellId)

                addCellToGrid(alGridRows, formResponse.alFormColumn.size, rowIndex, columnIndex, captionGridCell)
                alSlaveId.add(gridCellId++)

                //--- если это широкий экран или строка GRID-формы
                if (root.isWideScreen || formResponse.alFormColumn.isNotEmpty()) {
                    columnIndex++
                } else {
                    rowIndex++
                }
            }

            val formGridData = getFormDataCell(
                formCell = formCell,
                gridCellId = gridCellId,
                isGridForm = formResponse.alFormColumn.isNotEmpty()
            )
            //--- на тачскринах автофокус только бесит автоматическим включением клавиатуры
//            if (!getStyleIsTouchScreen()) {
//                //--- set autofocus from server
//                if (formCell.isAutoFocus) {
//                    autoFocusId = getFocusId(formGridData.id, formGridData.alSubId?.get(0))
//                }
//                //--- automatic autofocus setting
//                else if (autoFocusId == null && !formGridData.isReadOnly) {
//                    autoFocusId = getFocusId(formGridData.id, formGridData.alSubId?.get(0))
//                }
//            }
            addCellToGrid(alGridRows, formResponse.alFormColumn.size, rowIndex, columnIndex, formGridData)

            //--- проверка на изначальную пустоту поля-автоселектора
            //--- (по умолчанию оно заполнено, чтобы не запустить автоселектор на непроверяемых полях)
            var isEmptyFieldValue = false
            when (formCell.cellType) {
                FormCellType.STRING, FormCellType.INT, FormCellType.DOUBLE -> {
                    isEmptyFieldValue = formCell.strValue.isEmpty()
                }

                FormCellType.TEXT -> {
                    isEmptyFieldValue = formCell.textValue.isEmpty()
                }

                FormCellType.BOOLEAN -> {
                    hmFormCellMaster[formCell.booleanName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                FormCellType.DATE, FormCellType.TIME, FormCellType.DATE_TIME -> {
                    isEmptyFieldValue = formCell.alDateTimeField[0].isEmpty()
                }

                FormCellType.COMBO -> {
                    hmFormCellMaster[formCell.comboName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                FormCellType.RADIO -> {
                    hmFormCellMaster[formCell.comboName] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                else -> {}
            }
            alSlaveId.add(gridCellId++)

            columnNo++

            //--- если это широкий экран или строка GRID-формы
            if (root.isWideScreen || formResponse.alFormColumn.isNotEmpty()) {
                columnIndex++
            } else {
                rowIndex++
            }

            //--- если это последнее поле в строке GRID-формы, то добавляем правый заголовок поля
            if (formResponse.alFormColumn.isNotEmpty() && columnNo == formResponse.columnCount) {
                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = formResponse.alFormColumn.size,
                    row = rowIndex,
                    col = columnIndex,
                    cell = getGridFormRightCaption(
                        formCell = formCell,
                        gridCellId = gridCellId++,
                    ),
                )

                //columnIndex++     // здесь нет смысла

                columnNo = 0   // новая строка GRID-формы
                columnIndex = 0
                rowIndex++
            }

            //--- автостарт селектора запускается, только если поле данных пустое,
            //-- иначе зациклимся на старте
            if (formCell.isAutoStartSelector && isEmptyFieldValue && autoClickUrl == null) {
                autoClickUrl = formGridData.selectorSetURL
            }

            //--- определим visible-зависимости
            for ((name, state, value) in formCell.alVisible) {
                hmFormCellMaster[name]?.let { masterId ->
                    for (slaveId in alSlaveId) {
                        hmFormCellVisible[masterId]?.add(FormCellVisibleInfo(state, value, slaveId))
                    }
                }
            }

            //--- определим caption-зависимости
            if (captionGridCell != null)
                for ((name, str, value) in formCell.alCaption) {
                    hmFormCellMaster[name]?.let { masterId ->
                        hmFormCellCaption[masterId]?.add(FormCellCaptionInfo(str, value, captionGridCell.id))
                    }
                }

            maxColumnCount = max(maxColumnCount, columnIndex)
        }
        rowIndex = getGridFormTopBottomCaptions(formResponse.alFormColumn, rowIndex + 1, gridCellId)

        formResponse.alFormButton.forEach { formButton ->
            alFormButton.add(
                FormButtonData(
                    url = formButton.url,
                    withNewData = formButton.withNewData,
                    name = formButton.iconName ?: formButton.caption ?: "(не заданы иконка и текст)",
                    tooltip = formButton.caption ?: "",
                    question = formButton.question,
                )
            )
            //--- назначение кнопок на горячие клавиши
            when (formButton.key) {
                BUTTON_KEY_AUTOCLICK -> if (autoClickUrl == null) {
                    autoClickUrl = formButton.url
                }

                BUTTON_KEY_SAVE -> formSaveUrl = formButton.url
                BUTTON_KEY_EXIT -> formExitUrl = formButton.url
            }
        }

        //--- начальные установки видимости и caption-зависимостей
        for (gridData in alFormCellMasterPreAction) {
            doVisibleAndCaptionChange(gridData)
        }

//        autoClickUrl?.let { acUrl ->
//            call(acUrl, true, null)
//        } ?: autoFocusId?.let { afId ->
//            //--- две попытки установить фокус, с минимальными задержками
//            window.setTimeout({
//                if (!setFocus(afId)) {
//                    window.setTimeout({
//                        setFocus(afId)
//                    }, 100)
//                }
//            }, 100)
//        }
    }

    private fun getGridFormTopBottomCaptions(alFormColumn: List<String>, aRowIndex: Int, aGridCellId: Int): Int {
        var rowIndex = aRowIndex
        var gridCellId = aGridCellId
        //--- верхние/нижние заголовки столбцов GRID-формы
        if (alFormColumn.isNotEmpty()) {
            var columnIndex = 1
            for (caption in alFormColumn) {
                //--- иначе в style-лямбде будут использоваться только самые последние значения rowIndex/colIndex
                //--- и все тексты уползут в самую правую ячейку
                val ri = rowIndex
                val ci = columnIndex
                val formGridData = FormGridData(
                    cellType = FormCellTypeClient.LABEL,
                    id = gridCellId++,
                    cellName = "",
                    aText = caption,
                    align = Arrangement.Center,
                )
                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = alFormColumn.size,
                    row = ri,
                    col = ci,
                    cell = formGridData,
                )
                columnIndex++
            }
            rowIndex++
        }
        return rowIndex
    }

    private fun getGridFormRightCaption(
        formCell: FormCell,
        gridCellId: Int,
    ) = FormGridData(
        cellType = FormCellTypeClient.LABEL,
        id = gridCellId,
        cellName = "",
        aText = formCell.caption,
        align = Arrangement.Start,
    )

    private fun getFormEmptyCell(
        gridCellId: Int,
        rowIndex: Int,
    ) = FormGridData(
        cellType = FormCellTypeClient.LABEL,
        id = gridCellId,
        cellName = "",
        aText = "", //BR,
        align = Arrangement.Center,
    )

    private fun getFormLabelCell(
        formCell: FormCell,
        gridCellId: Int,
    ): FormGridData {
        val isWideOrGrid = root.isWideScreen || formResponse.alFormColumn.isNotEmpty()
        return FormGridData(
            cellType = FormCellTypeClient.LABEL,
            id = gridCellId,
            cellName = "",
            aText = formCell.caption,
            align = if (isWideOrGrid) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
        )
    }

    private fun getFormDataCell(
        formCell: FormCell,
        gridCellId: Int,
        isGridForm: Boolean = false
    ): FormGridData {

        val align = if (isGridForm) {
            Arrangement.Center
        } else {
            Arrangement.Start
        }

        val gridData = when (formCell.cellType) {
            FormCellType.STRING, FormCellType.INT, FormCellType.DOUBLE -> {
                FormGridData(
                    cellType = FormCellTypeClient.STRING,
                    id = gridCellId,
                    cellName = formCell.strName,
                    align = align,
                    error = formCell.errorMessage,
                    aText = formCell.strValue,
                    isPassword = formCell.isPassword,
                    colCount = getStyleFormEditBoxColumn(formCell.strColumn),
                    isReadOnly = !formCell.isEditable,
                )
            }

            FormCellType.TEXT -> {
                FormGridData(
                    cellType = FormCellTypeClient.TEXT,
                    id = gridCellId,
                    cellName = formCell.textName,
                    align = align,
                    error = formCell.errorMessage,
                    aText = formCell.textValue,
                    colCount = getStyleFormEditBoxColumn(formCell.textColumn),
                    rowCount = formCell.textRow,
                    isReadOnly = !formCell.isEditable,
                )
            }

            FormCellType.BOOLEAN -> {
                FormGridData(
                    cellType = if (formCell.alSwitchText.isEmpty()) {
                        FormCellTypeClient.CHECKBOX
                    } else {
                        FormCellTypeClient.SWITCH
                    },
                    id = gridCellId,
                    cellName = formCell.booleanName,
                    align = align,
                    error = formCell.errorMessage,
                    aBool = formCell.booleanValue,
                    isReadOnly = !formCell.isEditable,
                    alSwitchText = formCell.alSwitchText.toList(),
                )
            }

            FormCellType.DATE, FormCellType.TIME, FormCellType.DATE_TIME -> {
                FormGridData(
                    cellType = when (formCell.cellType) {
                        FormCellType.DATE -> {
                            FormCellTypeClient.DATE
                        }

                        FormCellType.TIME -> {
                            FormCellTypeClient.TIME
                        }

                        else -> {
                            FormCellTypeClient.DATE_TIME
                        }
                    },
                    id = gridCellId,
                    cellName = formCell.dtName,
                    align = align,
                    error = formCell.errorMessage,
                    aAlDateTime = formCell.alDateTimeField,
                    isReadOnly = !formCell.isEditable,
                ).apply {
                    alSubId = mutableListOf()
                    alDateTime.indices.forEach { i ->
                        alSubId?.add(i)
                    }
                }
            }

            FormCellType.COMBO -> {
                FormGridData(
                    cellType = FormCellTypeClient.COMBO,
                    id = gridCellId,
                    cellName = formCell.comboName,
                    align = align,
                    error = formCell.errorMessage,
                    aCombo = formCell.comboValue,
                    isReadOnly = !formCell.isEditable,
                    alComboData = formCell.alComboData.map { FormComboData(it.first, it.second) },
                )
            }

            FormCellType.RADIO -> {
                FormGridData(
                    cellType = FormCellTypeClient.RADIO,
                    id = gridCellId,
                    cellName = formCell.comboName,
                    align = align,
                    error = formCell.errorMessage,
                    aCombo = formCell.comboValue,
                    isReadOnly = !formCell.isEditable,
                    alComboData = formCell.alComboData.map { FormComboData(it.first, it.second) },
                ).apply {
                    alSubId = mutableListOf()
                    alComboData.indices.forEach { i ->
                        alSubId?.add(i)
                    }
                }
            }

            FormCellType.FILE -> {
                FormGridData(
                    cellType = FormCellTypeClient.FILE,
                    id = gridCellId,
                    cellName = formCell.fileName,
                    align = align,
                    error = formCell.errorMessage,
                    fileId = formCell.fileId,
                    alFileData = formCell.alFile.map { FormFileData(it.first, it.second, it.third) }.toMutableStateList(),
                )
            }

        }
        gridData.selectorSetURL = formCell.selectorSetURL
        gridData.selectorClearURL = formCell.selectorClearURL

        return gridData
    }

    private fun doVisibleAndCaptionChange(gdMaster: FormGridData) {
        //--- определение контрольного значения
        val controlValue =
            when (gdMaster.cellType) {
                FormCellTypeClient.CHECKBOX, FormCellTypeClient.SWITCH -> {
                    if (
                        if (hmHiddenCells.contains(gdMaster.id)) {
                            gdMaster.oldBool
                        } else {
                            gdMaster.bool
                        }
                    ) {
                        1
                    } else {
                        0
                    }
                }

                FormCellTypeClient.COMBO -> if (hmHiddenCells.contains(gdMaster.id)) {
                    gdMaster.oldCombo
                } else {
                    gdMaster.combo
                }

                FormCellTypeClient.RADIO -> if (hmHiddenCells.contains(gdMaster.id)) {
                    gdMaster.oldCombo
                } else {
                    gdMaster.combo
                }

                else -> 0
            }
        hmFormCellVisible[gdMaster.id]?.let { alFCVI ->
            alFCVI.forEach { fcvi ->
                alGridRows.forEach { alGridRow ->
                    alGridRow.find { gd -> gd?.id == fcvi.id }?.let { gdSlave ->
                        gdSlave.isVisible.value = (fcvi.state == fcvi.hsValue.contains(controlValue))
                        //--- Тут бы по-хорошему выйти из цикла пробежки по гриду, но фигушки.
                        //--- Впрочем грид в форме мелкий и лишняя полная пробежка по нему на производительность не повлияет
                    }
                }
            }
        }

        hmFormCellCaption[gdMaster.id]?.let { alFCCI ->
            alFCCI.forEach { fcci ->
                alGridRows.forEach { alGridRow ->
                    alGridRow.find { gd -> gd?.id == fcci.id }?.let { gdSlave ->
                        if (fcci.hsValue.contains(controlValue)) {
                            gdSlave.text = fcci.caption
                            //--- Тут бы по-хорошему выйти из цикла пробежки по гриду, но фигушки.
                            //--- Впрочем грид в форме мелкий и лишняя полная пробежка по нему на производительность не повлияет
                        }
                    }
                }
            }
        }
    }

//    private fun selectAllText(syntheticFocusEvent: SyntheticFocusEvent) {
//        //--- программный селект текста на тачскринах вызывает показ надоедливого окошка с копированием/вырезанием текста (и так на каждый input)
//        if (!getStyleIsTouchScreen()) {
//            (syntheticFocusEvent.target as? HTMLInputElement)?.select()
//        }
//    }

//    private fun doKeyUp(gridData: FormGridData, syntheticKeyboardEvent: SyntheticKeyboardEvent) {
//        when (syntheticKeyboardEvent.key) {
//            "Enter" -> {
//                if (syntheticKeyboardEvent.ctrlKey) {
//                    if (formSaveUrl.isNotEmpty()) {
//                        call(formSaveUrl, true, null)
//                    }
//                } else {
//                    doNextFocus(gridData.id, -1)
//                }
//            }
//
//            "Escape" -> {
//                if (formExitUrl.isNotEmpty()) {
//                    call(formExitUrl, false, null)
//                }
//            }
//
//            "F4" -> {
//                closeTabById()
//            }
//        }
//    }

//    private fun doNextFocus(gridDataId: Int, gridDataSubId: Int) {
//        val curIndex = alGridData.indexOfFirst { formGridData ->
//            formGridData.id == gridDataId
//        }
//
//        if (curIndex >= 0) {
//            val curGridData = alGridData[curIndex]
//
//            var nextGridId = -1
//            var nextSubGridId = -1
//
//            //--- try set focus to next sub-field into fields group (date/time-textfields or radio-buttons)
//            curGridData.alSubId?.let { alSubId ->
//                val curSubIndex = alSubId.indexOf(gridDataSubId)
//                if (curSubIndex >= 0) {
//                    if (curSubIndex < alSubId.lastIndex) {
//                        nextGridId = gridDataId
//                        nextSubGridId = alSubId[curSubIndex + 1]
//                    }
//                }
//            }
//            //--- else try set focus to next field or first sub-field in next field
//            if (nextGridId == -1 && nextSubGridId == -1) {
//                if (curIndex < alGridData.lastIndex) {
//                    var nextIndex = curIndex + 1
//                    //--- search non-label element
//                    while (nextIndex <= alGridData.lastIndex && alGridData[nextIndex].cellType == FormCellTypeClient.LABEL) {
//                        nextIndex++
//                    }
//                    val nextGridData = alGridData[nextIndex]
//                    nextGridId = nextGridData.id
//                    nextSubGridId = nextGridData.alSubId?.firstOrNull() ?: -1
//                }
//            }
//
//            val nextFocusId = getFocusId(
//                nextGridId, if (nextSubGridId < 0) {
//                    null
//                } else {
//                    nextSubGridId
//                }
//            )
//
//            val element = document.getElementById(nextFocusId)
//            if (element is HTMLElement) {
//                element.focus()
//            }
//        }
//    }

//    private fun closeTabById() {
//        root.closeTabById(tabId)
//    }

    private fun showFile(url: String) {
        root.openTab(url)
    }

    private fun deleteFile(gridData: FormGridData, fileData: FormFileData) {
        gridData.alFileData.removeAll { formFileData ->
            formFileData.id == fileData.id
        }
        //--- сохраним ID удаляемого файла для передачи на сервер
        if (fileData.id > 0) {
            gridData.alFileRemovedId.add(fileData.id)
        }
        //--- или просто удалим ранее добавленный файл из списка
        else {
            gridData.hmFileAdd.remove(fileData.id)
        }
    }

    private fun call(formAppParam: String, withNewData: Boolean, dialogQuestion: String?) {
        dialogQuestion?.let {
//            root.dialogActionFun = {
            doInvoke(formAppParam, withNewData)
//            }
//            root.dialogQuestion.value = dialogQuestion
//            root.showDialogCancel.value = true
//            root.showDialog.value = true
        } ?: run {
            doInvoke(formAppParam, withNewData)
        }
    }

    private fun doInvoke(formAppParam: String, withNewValues: Boolean) {
        val hmFormData = mutableMapOf<String, FormData>()

        hmHiddenCells.forEach { (_, gridData) ->
            fillFormData(gridData, false, hmFormData)
        }
        alGridRows.forEach { alGridRow ->
            alGridRow.forEach { gridData ->
                gridData?.let {
                    fillFormData(gridData, withNewValues, hmFormData)
                }
            }
        }
        appControl.call(AppRequest(action = formAppParam, hmFormData = hmFormData))
    }

    private fun fillFormData(gridData: FormGridData, withNewValues: Boolean, hmFormData: MutableMap<String, FormData>) {
        when (gridData.cellType) {
            FormCellTypeClient.STRING -> {
                hmFormData[gridData.cellName] = FormData(
                    stringValue = if (withNewValues) {
                        gridData.text
                    } else {
                        gridData.oldText
                    }
                )
            }

            FormCellTypeClient.TEXT -> {
                hmFormData[gridData.cellName] = FormData(
                    textValue = if (withNewValues) {
                        gridData.text
                    } else {
                        gridData.oldText
                    }
                )
            }

            FormCellTypeClient.CHECKBOX, FormCellTypeClient.SWITCH -> {
                hmFormData[gridData.cellName] = FormData(
                    booleanValue = if (withNewValues) {
                        gridData.bool
                    } else {
                        gridData.oldBool
                    }
                )
            }

            FormCellTypeClient.DATE, FormCellTypeClient.TIME, FormCellTypeClient.DATE_TIME -> {
                hmFormData[gridData.cellName] = FormData(
                    alDateTimeValue = (if (withNewValues) {
                        gridData.alDateTime.map { it.value }
                    } else {
                        gridData.oldAlDateTime
                    }).toList())
            }

            FormCellTypeClient.COMBO -> {
                hmFormData[gridData.cellName] = FormData(
                    comboValue = if (withNewValues) {
                        gridData.combo
                    } else {
                        gridData.oldCombo
                    }
                )
            }

            FormCellTypeClient.RADIO -> {
                hmFormData[gridData.cellName] = FormData(
                    comboValue = if (withNewValues) {
                        gridData.combo
                    } else {
                        gridData.oldCombo
                    }
                )
            }

            FormCellTypeClient.FILE -> {
                hmFormData[gridData.cellName] = FormData(
                    fileId = gridData.fileId,
                    hmFileAdd = if (withNewValues) {
                        gridData.hmFileAdd.mapKeys { it.key.toString() }
                    } else {
                        mapOf()
                    },
                    alFileRemovedId = if (withNewValues) {
                        gridData.alFileRemovedId
                    } else {
                        listOf()
                    }
                )
            }

            else -> {}
        }
    }

    private fun getFocusId(gridId: Int, subGridId: Int? = null): String =
        subGridId?.let {
            "i_${tabId}_${gridId}_${subGridId}"
        } ?: run {
            "i_${tabId}_${gridId}"
        }

//    private fun setFocus(focusElementId: String): Boolean {
//        document.getElementById(focusElementId)?.let { element ->
//            if (element is HTMLElement) {
//                element.focus()
//                return true
//            }
//        }
//        return false
//    }

    private fun getStyleFormEditBoxColumn(initSize: Int) = if (root.isWideScreen) {
        min(initSize, root.scaledScreenWidth / 19)
    } else {
        min(initSize, 30)
    }

}
package foatto.compose.control

import androidx.compose.foundation.ScrollState
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
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.composable.ImageOrTextFromName
import foatto.compose.control.model.TitleData
import foatto.compose.control.model.form.FormButtonData
import foatto.compose.control.model.form.FormCellCaptionInfo
import foatto.compose.control.model.form.FormCellVisibleInfo
import foatto.compose.control.model.form.cell.FormBaseCellClient
import foatto.compose.control.model.form.cell.FormBooleanCellClient
import foatto.compose.control.model.form.cell.FormComboCellClient
import foatto.compose.control.model.form.cell.FormDateTimeCellClient
import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.compose.control.model.form.cell.FormLabelCellClient
import foatto.compose.control.model.form.cell.FormSimpleCellClient
import foatto.compose.getStyleOtherIconNameSuffix
import foatto.compose.invokeRequest
import foatto.compose.invokeUploadFormFile
import foatto.compose.styleOtherIconSize
import foatto.compose.utils.maxDp
import foatto.core.model.AppAction
import foatto.core.model.request.system.AppRequest
import foatto.core.model.request.system.FormActionData
import foatto.core.model.request.system.FormActionRequest
import foatto.core.model.response.system.AppResponse
import foatto.core.model.response.system.FormActionResponse
import foatto.core.model.response.system.FormResponse
import foatto.core.model.response.system.ResponseCode
import foatto.core.model.response.system.form.FormButtonKey
import foatto.core.model.response.system.form.FormPinMode
import foatto.core.model.response.system.form.cells.FormBaseCell
import foatto.core.model.response.system.form.cells.FormBooleanCell
import foatto.core.model.response.system.form.cells.FormComboCell
import foatto.core.model.response.system.form.cells.FormDateTimeCell
import foatto.core.model.response.system.form.cells.FormFileCell
import foatto.core.model.response.system.form.cells.FormSimpleCell
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class FormControl(
    private val root: Root,
    private val appControl: AppControl,
    private val formAction: AppAction,
    private val formResponse: FormResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    //    companion object {
//        private val hmFormIcon = mapOf(
//            IconName.DELETE to "/web/images/ic_delete_forever_black_48dp.png",
//            IconName.EXIT to "/web/images/ic_exit_to_app_black_48dp.png",
//            IconName.FILE to "/web/images/ic_attachment_black_48dp.png",
//            IconName.PRINT to "/web/images/ic_print_black_48dp.png",
//            IconName.SAVE to "/web/images/ic_save_black_48dp.png",
//            IconName.SELECT to "/web/images/ic_reply_black_48dp.png",
//            IconName.SAVE to "/web/images/ic_save_black_48dp.png",
//        )
//    }
    companion object {
        private val FORM_CELL_PADDING = 16.dp
    }

    //    private val alGridData = mutableStateListOf<FormBaseCellClient>()
    private val hmHiddenCells = mutableMapOf<Int, FormBaseCellClient>()
    private val alGridRows = mutableStateListOf<MutableList<FormBaseCellClient?>>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val hmFormCellVisible = mutableMapOf<Int, MutableList<FormCellVisibleInfo>>()
    private val hmFormCellCaption = mutableMapOf<Int, MutableList<FormCellCaptionInfo>>()

    private val alFormButton = mutableListOf<FormButtonData>()

    private var formSaveActionType: String? = null
    private var formExitActionType: String? = null

    private var selectorControl by mutableStateOf<TableControl?>(null)

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var isFileAddInProgress by mutableStateOf(false)
    private var fileGridData: FormFileCellClient? = null

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
                    fileGridData?.let { formFileCellClient ->
                        invokeUploadFormFile(formFileCellClient, listOf(platformFile)) {
                            isFileAddInProgress = false
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                getTableAndFormHeader(true) { _: AppAction -> }

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
                                //--- специальное хитрое условие: показывать если gridData == null или isVisible == true
                                if (gridData?.isVisible?.value != false) {
                                    Row(
                                        horizontalArrangement = gridData?.align ?: Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxHeight()    // работает только совместно с .height(intrinsicSize = IntrinsicSize.Max)
                                            //!!! только на время отладки
                                            //.border(BorderStroke(width = 1.dp, color = Color.LightGray))
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
                                        when (gridData) {
                                            is FormLabelCellClient -> {
                                                Text(
                                                    modifier = Modifier.padding(FORM_CELL_PADDING),
                                                    text = gridData.caption,
                                                )
//                                            if (gridData.text.value == BR) {
//                                                Br()
//                                            } else {
//                                                Text(gridData.text.value)
//                                            }
                                            }

                                            is FormSimpleCellClient -> {
                                                OutlinedTextField(
                                                    modifier = Modifier.padding(FORM_CELL_PADDING),
                                                    value = gridData.current.value,
                                                    onValueChange = { newText ->
                                                        gridData.current.value = newText
                                                    },
                                                    readOnly = !gridData.data.isEditable,
                                                    //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                                    isError = gridData.error != null,
                                                    supportingText = gridData.error?.let { error ->
                                                        {
                                                            Text(
                                                                text = error,
                                                                color = Color.Red,
                                                            )
                                                        }
                                                    },
                                                    singleLine = gridData.data.rows == 1,
                                                    minLines = gridData.data.rows,
                                                    maxLines = gridData.data.rows,
                                                    visualTransformation = if (gridData.data.isPassword) {
                                                        PasswordVisualTransformation()
                                                    } else {
                                                        VisualTransformation.None
                                                    },
                                                )
//                                            id(getFocusId(gridData.id))
//                                            size(gridData.data.columns)
//                                            onFocusIn { syntheticFocusEvent ->
//                                                selectAllText(syntheticFocusEvent)
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                            }

                                            is FormBooleanCellClient -> {
                                                if (gridData.data.alSwitchText.isEmpty()) {
                                                    Checkbox(
                                                        modifier = Modifier.padding(FORM_CELL_PADDING),
                                                        checked = gridData.current.value,
                                                        //enabled = !gridData.data.isEditable, - слишком сильно отличается по стилю от обычных read-only текстовых полей ввода
                                                        onCheckedChange = { newState ->
                                                            if (gridData.data.isEditable) {
                                                                gridData.current.value = newState
                                                                doVisibleAndCaptionChange(gridData)
                                                            }
                                                        },
                                                    )
                                                    //                                            id(getFocusId(gridData.id))
                                                    //onInput { syntheticInputEvent -> - тоже можно
                                                    //                                            onKeyUp { syntheticKeyboardEvent ->
                                                    //                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                                    //                                            }
                                                } else {
//                                                getFormSwitch(
//                                                    id0 = getFocusId(gridData.id, 0),
//                                                    id1 = getFocusId(gridData.id, 1),
//                                                    switchText0 = gridData.data.alSwitchText[0],
//                                                    switchText1 = gridData.data.alSwitchText[1],
//                                                    value = gridData.current.value,
//                                                    isReadOnly = !gridData.data.isEditable,
//                                                    onClick0 = {
//                                                        if (gridData.data.isEditable && !gridData.current.value) {
//                                                            gridData.current.value = !gridData.current.value
//                                                            doVisibleAndCaptionChange(gridData)
//                                                        }
//                                                    },
//                                                    onClick1 = {
//                                                        if (gridData.data.isEditable && gridData.current.value) {
//                                                            gridData.current.value = !gridData.current.value
//                                                            doVisibleAndCaptionChange(gridData)
//                                                        }
//                                                    },
//                                                    onKeyUp = { key, isCtrlKey ->
//                                                        doKeyUp(gridData, key, isCtrlKey)
//                                                    },
//                                                )
                                                }
                                            }

                                            is FormDateTimeCellClient -> {
                                                for (index in 0 until gridData.data.value.size) {
                                                    //--- time fields on new line on narrow screens
//                                            if (styleIsNarrowScreen && index == 3) {
//                                                Br()
//                                            }
//!!! - как различать разные подтипы даты/времени?
//                                                if (gridData.cellType != FormCellTypeClient.TIME && index in (1..2)) {
//                                                    Text(text = ".")
//                                                } else if (gridData.cellType == FormCellTypeClient.TIME && index in (1..2)) {
//                                                    Text(text = ":")
//                                                } else if (gridData.cellType == FormCellTypeClient.DATE_TIME && index in (4..5)) {
//                                                    Text(text = ":")
//                                                }

//!!!                                                val isYearField = gridData.cellType != FormCellTypeClient.TIME && index == 2
                                                    OutlinedTextField(
                                                        modifier = Modifier
                                                            .padding(FORM_CELL_PADDING)
                                                            .width(
//!!!                                                            if (isYearField) {
//                                                                76.dp   // после ввода пятой (лишней) цифры первая наполовину уходит
//                                                            } else {
                                                                58.dp   // после ввода третьей (лишней) цифры первая наполовину уходит
//                                                            }
                                                            ),
                                                        value = gridData.current[index].value,
                                                        onValueChange = { newText ->
                                                            gridData.current[index].value = newText
                                                        },
                                                        readOnly = !gridData.data.isEditable,
                                                        //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                                        isError = gridData.error != null,
                                                        supportingText = gridData.error?.let { error ->
                                                            if (index == 0) {
                                                                {
                                                                    Text(
                                                                        text = error,
                                                                        color = Color.Red,
                                                                    )
                                                                }
                                                            } else {
                                                                null
                                                            }
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

                                            is FormComboCellClient -> {
                                                if (gridData.data.asRadioButtons) {
                                                    Column(
                                                        modifier = Modifier.selectableGroup(),
                                                    ) {
                                                        for ((value, descr) in gridData.data.values) {
                                                            Row(
                                                                modifier = Modifier.selectable(
                                                                    selected = gridData.current.value == value,
                                                                    onClick = {
                                                                        if (gridData.data.isEditable) {
                                                                            gridData.current.value = value
                                                                            doVisibleAndCaptionChange(gridData)
                                                                        }
                                                                    }
                                                                ),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                RadioButton(
                                                                    selected = gridData.current.value == value,
                                                                    onClick = {
                                                                        if (gridData.data.isEditable) {
                                                                            gridData.current.value = value
                                                                            doVisibleAndCaptionChange(gridData)
                                                                        }
                                                                    }
                                                                )
                                                                Text(
                                                                    text = descr,
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
                                                } else {
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
                                                        value = gridData.data.values.find { (value, _) ->
                                                            gridData.current.value == value
                                                        }?.second ?: "(неизвестное значение)",
                                                        onValueChange = {},
                                                        readOnly = !gridData.data.isEditable,
                                                        //!!! label = { Text("Имя") }, - использовать для мобильной версии без FormCellTypeClient.LABEL
                                                        isError = gridData.error != null,
                                                        supportingText = gridData.error?.let { error ->
                                                            {
                                                                Text(
                                                                    text = error,
                                                                    color = Color.Red,
                                                                )
                                                            }
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
                                                                modifier = Modifier.clickable {
                                                                    if (gridData.data.isEditable) {
                                                                        isExpanded = !isExpanded
                                                                    }
                                                                }
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
                                                        for ((value, descr) in gridData.data.values) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(text = descr)
                                                                },
                                                                onClick = {
                                                                    gridData.current.value = value
                                                                    isExpanded = false
                                                                    doVisibleAndCaptionChange(gridData)
                                                                },
                                                                //contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            is FormFileCellClient -> {
                                                Column {
                                                    for (fileData in gridData.files) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Button(
                                                                enabled = fileData.id >= 0 && fileData.action != null,
                                                                shape = RoundedCornerShape(0.dp),
                                                                onClick = {
                                                                    if (fileData.id >= 0) {
                                                                        fileData.action?.let {
                                                                            showFile(fileData.action)
                                                                        }
                                                                    }
                                                                },
                                                            ) {
                                                                Text(text = fileData.text)
                                                            }
//                                                            backgroundColor(getColorButtonBack())
//                                                            setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                                            fontSize(styleCommonButtonFontSize)
//                                                            doKeyUp(gridData, syntheticKeyboardEvent)
                                                            if (gridData.data.isEditable) {
                                                                IconButton(
                                                                    onClick = {
                                                                        if (!isFileAddInProgress) {
                                                                            deleteFile(gridData, fileData.id)
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
                                                if (gridData.data.isEditable) {
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
                                        }

                                        if (gridData is FormSimpleCellClient) {
                                            gridData.data.selectorSetAction?.let { selectorSetAction ->
                                                IconButton(
                                                    onClick = {
                                                        if (!isFileAddInProgress) {
                                                            //!!! call(gridData.selectorSetURL, true, null)
                                                            callSelector(selectorSetAction)
                                                        }
                                                    }
                                                ) {
                                                    ImageOrTextFromName("/web/images/ic_reply_${getStyleOtherIconNameSuffix()}.png", "Выбрать из справочника", styleOtherIconSize)
                                                }
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                            }

//                                        if (gridData.selectorClearURL.isNotEmpty()) {
//                                            IconButton(
//                                                onClick = {
//                                                    if (!isFileAddInProgress) {
//                                                        call(gridData.selectorClearURL, true, null)
//                                                    }
//                                                }
//                                            ) {
//                                                ImageOrTextFromName("/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png", "Очистить выбор", styleOtherIconSize)
//                                            }
//
////                                                backgroundColor(getColorFormButtonBack())
////                                                setBorder(getStyleFormButtonBorder())
////                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                        }
//TODO !!!
//                            gridData.selectorClearAction?.let { selectorClearAction ->
//                                getFormSelectorClear(
//                                    onClick = {
//                                        //TODO: не забыть вернуть/реализовать
//                                        //call(selectorClearAction, true)
//                                    },
//                                    onKeyUp = { key, isCtrlKey ->
//                                        doKeyUp(gridData, key, isCtrlKey)
//                                    },
//                                )
//                            }

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
                                    call(formButton.actionType, formButton.withNewData, formButton.question)
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

            selectorControl?.Body()
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
        for ((action, text) in formResponse.alHeader) {
            tabToolTip += (if (tabToolTip.isEmpty()) "" else " | ") + text
            alTitleData.add(TitleData(action, text))
        }
        root.setTabInfo(tabId, formResponse.tabCaption, tabToolTip)
    }

    private fun readForm() {
        hmHiddenCells.clear()
        alGridRows.clear()

        var columnNo = 0       // счётчик столбцов grid-формы
        var columnIndex = 0
        var rowIndex = 0
        val hmFormCellMaster = mutableMapOf<String, Int>()
        var autoFocusId: String? = null
        val alFormCellMasterPreAction = mutableListOf<FormBaseCellClient>()
        var autoClickAction: AppAction? = null

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

                if (formCell is FormBooleanCell || formCell is FormComboCell) {
                    hmFormCellMaster[formCell.name] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                gridCellId++
                continue
            }

            val alSlaveId = mutableListOf<Int>()

            //--- обычная форма: начинаем новую строку
            if (formResponse.alFormColumn.isEmpty()) {
                columnIndex = 0
                if (formCell.formPinMode == FormPinMode.ON ||
                    formCell.formPinMode == FormPinMode.AUTO && !formCell.isEditable //!!! && (formCell as? FormSimpleCell)?.selectorSetAction == null
                ) {
                    rowIndex++
                } else {
                    //--- добавим разделитель для отвязки от предыдущего блока полей ввода
                    rowIndex++
//!!!                    val emptyCell = getFormEmptyCell(gridCellId, rowIndex)

//!!!                    alGridData.add(emptyCell)
                    alSlaveId.add(gridCellId++)

                    rowIndex++
                }
            }

            //--- если это обычная форма или первое поле в строке GRID-формы, то добавляем левый заголовок поля
            var captionGridCell: FormBaseCellClient? = null
            if (formResponse.alFormColumn.isEmpty() || columnNo == 0) {
                captionGridCell = getFormLabelCell(formCell, gridCellId)

                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = formResponse.alFormColumn.size,
                    row = rowIndex,
                    col = columnIndex,
                    cell = captionGridCell
                )
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
            addCellToGrid(
                alGridRows = alGridRows,
                maxColCount = formResponse.alFormColumn.size,
                row = rowIndex,
                col = columnIndex,
                cell = formGridData
            )

            //--- проверка на изначальную пустоту поля-автоселектора
            //--- (по умолчанию оно заполнено, чтобы не запустить автоселектор на непроверяемых полях)
            var isEmptyFieldValue = false
            when (formCell) {
                is FormSimpleCell -> {
                    isEmptyFieldValue = formCell.value.isEmpty()
                }

                is FormBooleanCell, is FormComboCell -> {
                    hmFormCellMaster[formCell.name] = gridCellId
                    hmFormCellVisible[gridCellId] = mutableListOf()
                    hmFormCellCaption[gridCellId] = mutableListOf()
                    alFormCellMasterPreAction.add(formGridData)
                }

                is FormDateTimeCell -> {
                    isEmptyFieldValue = formCell.value[0].isEmpty()
                }

                is FormFileCell -> {}
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
//!!!            if (formCell.isAutoStartSelector && isEmptyFieldValue && autoClickAction == null) {
//                autoClickAction = formGridData.selectorSetAction
//            }

            //--- определим visible-зависимости
//!!!            for ((name, state, value) in formCell.alVisible) {
//                hmFormCellMaster[name]?.let { masterId ->
//                    for (slaveId in alSlaveId) {
//                        hmFormCellVisible[masterId]?.add(FormCellVisibleInfo(state, value, slaveId))
//                    }
//                }
//            }

            //--- определим caption-зависимости
//!!!            if (captionGridCell != null)
//                for ((name, str, value) in formCell.alCaption) {
//                    hmFormCellMaster[name]?.let { masterId ->
//                        hmFormCellCaption[masterId]?.add(FormCellCaptionInfo(str, value, captionGridCell.id))
//                    }
//                }

            maxColumnCount = max(maxColumnCount, columnIndex)
        }
        rowIndex = getGridFormTopBottomCaptions(formResponse.alFormColumn, rowIndex + 1, gridCellId)

        formResponse.alFormButton.forEach { formButton ->
            alFormButton.add(
                FormButtonData(
                    actionType = formButton.actionType,
                    withNewData = formButton.withNewData,
                    name = formButton.iconName ?: formButton.caption ?: "(не заданы иконка и текст)",
                    tooltip = formButton.caption ?: "",
                    question = formButton.question,
                )
            )
            //--- назначение кнопок на горячие клавиши
            when (formButton.key) {
//!!!                FormButtonKey.AUTOCLICK -> if (autoClickAction == null) {
//                    autoClickAction = formButton.actionType
//                }

                FormButtonKey.SAVE -> formSaveActionType = formButton.actionType
                FormButtonKey.EXIT -> formExitActionType = formButton.actionType
                else -> {}  // на время недописанности AUTOCLICK
            }
        }

        //--- начальные установки видимости и caption-зависимостей
        for (gridData in alFormCellMasterPreAction) {
            doVisibleAndCaptionChange(gridData)
        }

//!!!        autoClickUrl?.let { acUrl ->
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
                val formGridData = FormLabelCellClient(
                    id = gridCellId++,
                    cellName = "",
                    caption = caption,
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
        formCell: FormBaseCell,
        gridCellId: Int,
    ) = FormLabelCellClient(
        id = gridCellId,
        cellName = "",
        caption = formCell.caption,
        align = Arrangement.Start,
    )

    private fun getFormEmptyCell(
        gridCellId: Int,
    ) = FormLabelCellClient(
        id = gridCellId,
        cellName = "",
        caption = "", //BR,
        align = Arrangement.Center,
    )

    private fun getFormLabelCell(
        formCell: FormBaseCell,
        gridCellId: Int,
    ): FormLabelCellClient {
        val isWideOrGrid = root.isWideScreen || formResponse.alFormColumn.isNotEmpty()
        return FormLabelCellClient(
            id = gridCellId,
            cellName = "",
            caption = formCell.caption,
            align = if (isWideOrGrid) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
        )
    }

    private fun getFormDataCell(
        formCell: FormBaseCell,
        gridCellId: Int,
        isGridForm: Boolean = false
    ): FormBaseCellClient {

        val align = if (isGridForm) {
            Arrangement.Center
        } else {
            Arrangement.Start
        }

        val gridData = when (formCell) {
            is FormSimpleCell -> {
                FormSimpleCellClient(
                    id = gridCellId,
                    cellName = formCell.name,
                    align = align,
                    data = formCell,
                )
            }

            is FormBooleanCell -> {
                FormBooleanCellClient(
                    id = gridCellId,
                    cellName = formCell.name,
                    align = align,
                    data = formCell,
                )
            }

            is FormDateTimeCell -> {
                FormDateTimeCellClient(
//!!!                    cellType = when (formCell.cellType) {
//                        FormCellType.DATE -> {
//                            FormCellTypeClient.DATE
//                        }
//
//                        FormCellType.TIME -> {
//                            FormCellTypeClient.TIME
//                        }
//
//                        else -> {
//                            FormCellTypeClient.DATE_TIME
//                        }
//                    },
                    id = gridCellId,
                    cellName = formCell.name,
                    align = align,
                    data = formCell,
                ).apply {
                    formCell.value.indices.forEach { i ->
                        alSubId.add(i)
                    }
                }
            }

            is FormComboCell -> {
                FormComboCellClient(
                    id = gridCellId,
                    cellName = formCell.name,
                    align = align,
                    data = formCell,
                ).apply {
                    if (formCell.asRadioButtons) {
                        formCell.values.indices.forEach { i ->
                            alSubId.add(i)
                        }
                    }
                }
            }

            is FormFileCell -> {
                FormFileCellClient(
                    id = gridCellId,
                    cellName = formCell.name,
                    align = align,
                    data = formCell,
                )
            }

        }

        return gridData
    }

    private fun doVisibleAndCaptionChange(gdMaster: FormBaseCellClient) {
        //--- определение контрольного значения
        val controlValue =
            when (gdMaster) {
                is FormBooleanCellClient -> gdMaster.current.value.toString()
                is FormComboCellClient -> gdMaster.current.value
                else -> "0"
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
                            (gdSlave as? FormLabelCellClient)?.current?.value = fcci.caption
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

    private fun closeTabById() {
        root.closeTabById(tabId)
    }

    private fun showFile(action: AppAction) {
        root.openTab(action)
    }

    private fun deleteFile(gridData: FormFileCellClient, fileDataId: Long) {
        gridData.files.removeAll { formFileData ->
            formFileData.id == fileDataId
        }
        //--- сохраним ID удаляемого файла для передачи на сервер
        if (fileDataId > 0) {
            gridData.fileRemovedIds.add(fileDataId)
        }
        //--- или просто удалим ранее добавленный файл из списка
        else {
            gridData.addFiles.remove(fileDataId)
        }
    }

    private fun call(actionType: String, withNewData: Boolean, dialogQuestion: String?) {
        doInvoke(actionType, withNewData)
//        dialogQuestion?.let {
//            root.dialogActionFun = {
//            doInvoke(actionType, withNewData)
//            }
//            root.dialogQuestion.value = dialogQuestion
//            root.showDialogCancel.value = true
//            root.showDialog.value = true
//        } ?: run {
//            doInvoke(actionType, withNewData)
//        }
    }

    private fun doInvoke(actionType: String, withNewValues: Boolean) {
        val formActionData = mutableMapOf<String, FormActionData>()

        hmHiddenCells.forEach { (_, gridData) ->
            fillFormData(gridData, false, formActionData)
        }
        alGridRows.forEach { alGridRow ->
            alGridRow.forEach { gridData ->
                gridData?.let {
                    fillFormData(gridData, withNewValues, formActionData)
                }
            }
        }

        invokeRequest(
            FormActionRequest(
                action = AppAction(
                    type = actionType,
                    module = formAction.module,
                    id = formResponse.id,
                    prevAction = formResponse.prevAction,
                ),
                formActionData = formActionData,
            )
        ) { formActionResponse: FormActionResponse ->
            //TODO:
            // 1. разные страницы в зависимости от responseCode
            // 2. возможен кастомный/нестандартный prevAction от сервера
            when (formActionResponse.responseCode) {
                ResponseCode.ERROR -> {
                    formActionResponse.errors?.let { errors ->
                        alGridRows.forEach { alGridRow ->
                            alGridRow.forEach { gridData ->
                                gridData?.error = when (gridData) {
                                    is FormSimpleCellClient -> errors[gridData.data.name]
                                    is FormBooleanCellClient -> errors[gridData.data.name]
                                    is FormDateTimeCellClient -> errors[gridData.data.name]
                                    is FormComboCellClient -> errors[gridData.data.name]
                                    is FormFileCellClient -> errors[gridData.data.name]
                                    else -> null
                                }
                            }
                        }
                    }
                }

                else -> {   // и в т.ч. ResponseCode.OK
                    formResponse.prevAction?.let { prevAction ->
                        appControl.call(AppRequest(prevAction))
                    }
                }
            }
        }
    }

    private fun fillFormData(gridData: FormBaseCellClient, withNewValues: Boolean, formActionData: MutableMap<String, FormActionData>) {
        when (gridData) {
            is FormSimpleCellClient -> {
                formActionData += gridData.data.name to FormActionData(
                    stringValue = if (withNewValues) {
                        gridData.current.value
                    } else {
                        gridData.data.value
                    }
                )
            }

            is FormBooleanCellClient -> {
                formActionData += gridData.data.name to FormActionData(
                    booleanValue = if (withNewValues) {
                        gridData.current.value
                    } else {
                        gridData.data.value
                    }
                )
            }

            is FormDateTimeCellClient -> {
                formActionData += gridData.data.name to FormActionData(
                    alDateTimeValue = (
                        if (withNewValues) {
                            gridData.current.map { s -> s.value }
                        } else {
                            gridData.data.value
                        }
                        ).toList()
                )
            }

            is FormComboCellClient -> {
                formActionData += gridData.data.name to FormActionData(
                    stringValue = if (withNewValues) {
                        gridData.current.value
                    } else {
                        gridData.data.value
                    }
                )
            }

            is FormFileCellClient -> {
                formActionData += gridData.data.name to FormActionData(
                    fileId = gridData.data.fileId,
                    addFiles = if (withNewValues) {
                        gridData.addFiles.mapKeys { it.key.toString() }
                    } else {
                        mapOf()
                    },
                    fileRemovedIds = if (withNewValues) {
                        gridData.fileRemovedIds
                    } else {
                        listOf()
                    },
                )
            }
        }
    }

    private fun callSelector(selectorSetAction: AppAction) {
        root.setWait(true)
        invokeRequest(AppRequest(action = selectorSetAction)) { appResponse: AppResponse ->

            val tableControl = TableControl(
                root = root,
                appControl = appControl,
                tableAction = selectorSetAction,
                tableResponse = appResponse.table!!,
                tabId = -1,
                selectorFun = { selectorData ->
                    selectorSetAction.selectorPath.forEach { (fromField, toField) ->
                        alGridRows.forEach { alGridRow ->
                            alGridRow.forEach { gridData ->
                                if ((gridData as? FormSimpleCellClient)?.data?.name == toField) {
                                    selectorData[fromField]?.let { fromData ->
                                        (gridData as? FormSimpleCellClient)?.current?.value = fromData
                                    }
                                }
                            }
                        }
                    }
                    selectorControl = null
                }
            )
            selectorControl = tableControl
            tableControl.start()

            root.setWait(false)
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
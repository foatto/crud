package foatto.compose.control

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import foatto.compose.*
import foatto.compose.composable.ImageOrTextFromNameControl
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
import foatto.compose.utils.maxDp
import foatto.core.model.AppAction
import foatto.core.model.request.AppRequest
import foatto.core.model.request.FormActionData
import foatto.core.model.request.FormActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.FormResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.form.FormPinMode
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.util.getRandomLong
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

    companion object {
        private val FORM_CELL_PADDING = 16.dp
    }

    private val alHiddenCells = mutableListOf<FormBaseCellClient>()
    private val alGridRows = mutableStateListOf<MutableList<FormBaseCellClient?>>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val hmFormCellVisible = mutableMapOf<String, MutableMap<String, FormCellVisibleInfo>>()
    private val hmFormCellCaption = mutableMapOf<String, MutableMap<String, List<FormCellCaptionInfo>>>()

    private val alFormButton = mutableListOf<FormButtonData>()

    private var formSaveActionType: String? = null
    private var formExitActionType: String? = null

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

        Column(
            modifier = Modifier.fillMaxSize().background(colorFormBack),
        ) {
            getTableAndFormHeader(isSelectorMode = false, withBottomBorder = true) { _: AppAction -> }

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
                                                text = gridData.current.value,
                                            )
//                                            if (gridData.text.value == BR) {
//                                                Br()
//                                            } else {
//                                                Text(gridData.text.value)
//                                            }
                                        }

                                        is FormSimpleCellClient -> {
                                            OutlinedTextField(
                                                modifier = Modifier
                                                    .padding(FORM_CELL_PADDING)
                                                    .widthIn(max = root.scaledScreenWidth.dp / 2)
                                                    .background(colorControlBack),
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
                                                    modifier = Modifier
                                                        .padding(FORM_CELL_PADDING)
                                                        .background(colorControlBack),
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
                                            val dateTimeFieldCount = gridData.data.value.size
                                            for (index in 0 until dateTimeFieldCount) {
                                                //--- time fields on new line on narrow screens
//                                            if (styleIsNarrowScreen && index == 3) {
//                                                Br()
//                                            }
                                                if (dateTimeFieldCount >= 3 && index in (1..2)) {
                                                    Text(text = ".")
                                                } else if (dateTimeFieldCount < 3 && index in (1..2)) {
                                                    Text(text = ":")
                                                } else if (index in (4..5)) {
                                                    Text(text = ":")
                                                }

                                                OutlinedTextField(
                                                    modifier = Modifier
                                                        .padding(FORM_CELL_PADDING)
                                                        .width(
                                                            if (index == 2) {
                                                                76.dp   // после ввода пятой (лишней) цифры первая наполовину уходит
                                                            } else {
                                                                58.dp   // после ввода третьей (лишней) цифры первая наполовину уходит
                                                            }
                                                        )
                                                        .background(colorControlBack),
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
                                                        }
                                                        .background(colorControlBack),
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
                                                            ImageOrTextFromNameControl(
                                                                name = "/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
                                                                iconSize = styleOtherIconSize,
                                                                imageButton = { func ->
                                                                    IconButton(
                                                                        onClick = {
                                                                            if (!isFileAddInProgress) {
                                                                                deleteFile(gridData, fileData.id)
                                                                            }
                                                                        }
                                                                    ) {
                                                                        func()
                                                                    }
                                                                },
                                                                textButton = {},
                                                            )
//                                                                backgroundColor(getColorFormButtonBack())
//                                                                setBorder(getStyleFormButtonBorder())
//                                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                                        }
                                                    }
                                                }
                                            }
                                            if (gridData.data.isEditable) {
                                                ImageOrTextFromNameControl(
                                                    name = "/web/images/ic_add_${getStyleOtherIconNameSuffix()}.png",
                                                    iconSize = styleOtherIconSize,
                                                    imageButton = { func ->
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
                                                            func()
                                                        }
                                                    },
                                                    textButton = {},
                                                )
                                            }
//                                            id(getFocusId(gridData.id))
                                        }
                                    }

                                    if (gridData is FormSimpleCellClient) {
                                        gridData.data.selectorAction?.let { selectorAction ->
                                            ImageOrTextFromNameControl(
                                                name = "/web/images/ic_reply_${getStyleOtherIconNameSuffix()}.png",
                                                iconSize = styleOtherIconSize,
                                                imageButton = { func ->
                                                    IconButton(
                                                        onClick = {
                                                            if (!isFileAddInProgress) {
                                                                callSelector(selectorAction)
                                                            }
                                                        }
                                                    ) {
                                                        func()
                                                    }
                                                },
                                                textButton = {},
                                            )
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                doKeyUp(gridData, syntheticKeyboardEvent)

                                            ImageOrTextFromNameControl(
                                                name = "/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
                                                iconSize = styleOtherIconSize,
                                                imageButton = { func ->
                                                    IconButton(
                                                        onClick = {
                                                            if (!isFileAddInProgress) {
                                                                clearSelector(selectorAction)
                                                            }
                                                        }
                                                    ) {
                                                        func()
                                                    }
                                                },
                                                textButton = {},
                                            )
////                                                backgroundColor(getColorFormButtonBack())
////                                                setBorder(getStyleFormButtonBorder())
////                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                        }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(colorBottomBar),
//                            if (!styleIsNarrowScreen) {
//                                JustifyContent.Center
//                            } else {
//                                JustifyContent.SpaceBetween
//                            }
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (formButton in alFormButton) {
                    ImageOrTextFromNameControl(
                        name = formButton.name,
                        iconSize = styleOtherIconSize,
                        imageButton = { func ->
                            IconButton(
                                modifier = Modifier.padding(1.dp),
                                enabled = !isFileAddInProgress,
                                onClick = {
                                    if (!isFileAddInProgress) {
                                        call(formButton.actionType, formButton.withNewData, formButton.question)
                                    }
                                }
                            ) {
                                func()
                            }
                        },
                        textButton = {},
                    )
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

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun start() {
        readHeader()
        if (formResponse.alFormColumn.isEmpty()) {
            readClassicForm()
        } else {
            readGridForm()
        }
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

    private fun readClassicForm() {
        alHiddenCells.clear()
        alGridRows.clear()

        var rowIndex = 0
        var colIndex = 0
        val alFormCellMasterPreAction = mutableListOf<FormBaseCellClient>()
        var autoFocusId: String? = null

        var cellFocusId = 0

        //--- хотя бы один столбец с полями всё равно будет
        var maxColCount = 1
        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormDataCell(
                    formCell = formCell,
                    cellFocusId = -1,
                )
                alHiddenCells += formGridData

                if (formCell is FormBooleanCell || formCell is FormComboCell) {
                    hmFormCellVisible[formCell.name] = mutableMapOf()
                    hmFormCellCaption[formCell.name] = mutableMapOf()
                    alFormCellMasterPreAction.add(formGridData)
                }
            } else {
                colIndex = 0

                if (formCell.pinMode == FormPinMode.ON ||
                    formCell.pinMode == FormPinMode.AUTO &&
                    !formCell.isEditable &&
                    (formCell as? FormSimpleCell)?.selectorAction == null
                ) {
                    rowIndex++
                } else {
                    //--- добавим разделитель для отвязки от предыдущего блока полей ввода
                    rowIndex++
//!!!                    val emptyCell = getFormEmptyCell(gridCellId, rowIndex)

//!!!                    alGridData.add(emptyCell)

                    rowIndex++
                }

                //--- добавляем левый заголовок поля
                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = maxColCount,
                    row = rowIndex,
                    col = colIndex,
                    cell = getClassicFormLabelCell(formCell)
                )

                //--- если это широкий экран
                if (root.isWideScreen) {
                    colIndex++
                } else {
                    rowIndex++
                }

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

                val formGridData = getFormDataCell(
                    formCell = formCell,
                    cellFocusId = cellFocusId++,
                )
                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = maxColCount,
                    row = rowIndex,
                    col = colIndex,
                    cell = formGridData,
                )

                when (formCell) {
                    is FormSimpleCell -> {}

                    is FormBooleanCell, is FormComboCell -> {
                        hmFormCellVisible[formCell.name] = mutableMapOf()
                        hmFormCellCaption[formCell.name] = mutableMapOf()
                        alFormCellMasterPreAction.add(formGridData)
                    }

                    is FormDateTimeCell -> {}

                    is FormFileCell -> {}
                }

                //--- если это широкий экран
                if (root.isWideScreen) {
                    colIndex++
                } else {
                    rowIndex++
                }

                //--- определим visible-зависимости
                formCell.visibility?.let { formCellVisibility ->
                    val hmFCVI = hmFormCellVisible.getOrPut(formCellVisibility.name) { mutableMapOf() }
                    hmFCVI[formCell.name] = FormCellVisibleInfo(
                        state = formCellVisibility.state,
                        values = formCellVisibility.values,
                    )
                }

                //--- определим caption-зависимости
                formCell.captions?.let { formCellCaption ->
                    val hmFCCI = hmFormCellCaption.getOrPut(formCellCaption.name) { mutableMapOf() }
                    hmFCCI[formCell.name] = formCellCaption.captions.map { (caption, values) ->
                        FormCellCaptionInfo(
                            caption = caption,
                            values = values,
                        )
                    }
                }

                maxColCount = max(maxColCount, colIndex)
            }
        }

        readFormButtons()

        //--- начальные установки видимости и caption-зависимостей
        for (gridData in alFormCellMasterPreAction) {
            doVisibleAndCaptionChange(gridData)
        }

//        autoFocusId?.let { afId ->
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

    private fun readGridForm() {
        alHiddenCells.clear()
        alGridRows.clear()

        val maxColCount = formResponse.alFormColumn.size + 2

        var rowIndex = 1    // rowIndex = 0 занят top-captions
        var colIndex = 0
        var autoFocusId: String? = null

        var cellFocusId = 0

        getGridFormTopBottomCaptions(formResponse.alFormColumn, 0)

        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormDataCell(
                    formCell = formCell,
                    cellFocusId = -1,
                    isGridForm = true,
                )
                alHiddenCells += formGridData
            } else {
                //--- если это первое поле в строке GRID-формы, то добавляем левый заголовок поля
                if (colIndex == 0) {
                    addCellToGrid(
                        alGridRows = alGridRows,
                        maxColCount = maxColCount,
                        row = rowIndex,
                        col = colIndex,
                        cell = getGridFormLeftCaption(formCell)
                    )
                    colIndex++
                }

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

                val formGridData = getFormDataCell(
                    formCell = formCell,
                    cellFocusId = cellFocusId++,
                    isGridForm = true,
                )
                addCellToGrid(
                    alGridRows = alGridRows,
                    maxColCount = maxColCount,
                    row = rowIndex,
                    col = colIndex,
                    cell = formGridData
                )

                colIndex++

                if (colIndex == maxColCount - 1) {
                    addCellToGrid(
                        alGridRows = alGridRows,
                        maxColCount = maxColCount,
                        row = rowIndex,
                        col = colIndex,
                        cell = getGridFormRightCaption(formCell)
                    )
                    colIndex = 0
                    rowIndex++
                }
            }
        }
        getGridFormTopBottomCaptions(formResponse.alFormColumn, rowIndex + 1)

        readFormButtons()

//        autoFocusId?.let { afId ->
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

    private fun getClassicFormLabelCell(formCell: FormBaseCell): FormLabelCellClient {
        return FormLabelCellClient(
            cellName = formCell.name,
            caption = formCell.caption,
            align = if (root.isWideScreen) {
                Arrangement.End
            } else {
                Arrangement.Start
            },
        )
    }

    private fun getGridFormTopBottomCaptions(alFormColumn: List<String>, rowIndex: Int) {
        var colIndex = 1
        for (caption in alFormColumn) {
            val formGridData = FormLabelCellClient(
                cellName = "",
                caption = caption,
                align = Arrangement.Center,
            )
            addCellToGrid(
                alGridRows = alGridRows,
                maxColCount = alFormColumn.size,
                row = rowIndex,
                col = colIndex,
                cell = formGridData,
            )
            colIndex++
        }
    }

    private fun getGridFormLeftCaption(formCell: FormBaseCell) = FormLabelCellClient(
        cellName = "",
        caption = formCell.caption,
        align = Arrangement.End,
    )

    private fun getGridFormRightCaption(formCell: FormBaseCell) = FormLabelCellClient(
        cellName = "",
        caption = formCell.caption,
        align = Arrangement.Start,
    )

    private fun getFormEmptyCell() = FormLabelCellClient(
        cellName = "",
        caption = "", //BR,
        align = Arrangement.Center,
    )

    private fun getFormDataCell(
        formCell: FormBaseCell,
        cellFocusId: Int,
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
                    cellName = formCell.name,
                    align = align,
                    focusId = cellFocusId,
                    data = formCell,
                )
            }

            is FormBooleanCell -> {
                FormBooleanCellClient(
                    cellName = formCell.name,
                    align = align,
                    focusId = cellFocusId,
                    data = formCell,
                )
            }

            is FormDateTimeCell -> {
                FormDateTimeCellClient(
                    cellName = formCell.name,
                    align = align,
                    focusId = cellFocusId,
                    data = formCell,
                ).apply {
                    formCell.value.indices.forEach { i ->
                        alSubFocusId.add(i)
                    }
                }
            }

            is FormComboCell -> {
                FormComboCellClient(
                    cellName = formCell.name,
                    align = align,
                    focusId = cellFocusId,
                    data = formCell,
                ).apply {
                    if (formCell.asRadioButtons) {
                        formCell.values.indices.forEach { i ->
                            alSubFocusId.add(i)
                        }
                    }
                }
            }

            is FormFileCell -> {
                FormFileCellClient(
                    cellName = formCell.name,
                    align = align,
                    focusId = cellFocusId,
                    data = formCell,
                )
            }

        }

        return gridData
    }

    private fun readFormButtons() {
        formResponse.alFormButton.forEach { formButton ->
            alFormButton.add(
                FormButtonData(
                    actionType = formButton.actionType,
                    withNewData = formButton.withNewData,
                    name = formButton.iconName,
                    tooltip = formButton.caption,
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
    }

    private fun doVisibleAndCaptionChange(gdMaster: FormBaseCellClient) {
        //--- определение контрольного значения
        val controlValue =
            when (gdMaster) {
                is FormBooleanCellClient -> gdMaster.current.value.toString()
                is FormComboCellClient -> gdMaster.current.value
                else -> "null"
            }

        hmFormCellVisible[gdMaster.cellName]?.let { hmFCVI ->
            alGridRows.forEach { alGridRow ->
                alGridRow.forEach { gdSlave ->
                    gdSlave?.let {
                        hmFCVI[gdSlave.cellName]?.let { fcvi ->
                            gdSlave.isVisible.value = (fcvi.state == fcvi.values.contains(controlValue))
                        }
                    }
                }
            }
        }

        hmFormCellCaption[gdMaster.cellName]?.let { hmFCCI ->
            alGridRows.forEach { alGridRow ->
                alGridRow.forEach { gdSlave ->
                    gdSlave?.let {
                        hmFCCI[gdSlave.cellName]?.let { alFCCI ->
                            (gdSlave as? FormLabelCellClient)?.let { gd ->
                                var caption = gd.caption
                                for (fcci in alFCCI) {
                                    if (fcci.values.contains(controlValue)) {
                                        caption = fcci.caption
                                        break
                                    }
                                }
                                gd.current.value = caption
                            }
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

    private fun deleteFile(gridData: FormFileCellClient, fileDataId: Int) {
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

        alHiddenCells.forEach { gridData ->
            fillFormData(gridData, withNewValues, formActionData)
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

    private fun callSelector(selectorAction: AppAction) {
        root.setWait(true)

        val selectorFunId = getRandomLong()
        val selectorFun: SelectorFunType = { selectorData ->
            selectorAction.selectorPath.forEach { (fromField, toField) ->
                alHiddenCells.forEach { gridData ->
                    setSelectorData(selectorData, fromField, toField, gridData)
                }
                alGridRows.forEach { alGridRow ->
                    alGridRow.forEach { gridData ->
                        setSelectorData(selectorData, fromField, toField, gridData)
                    }
                }
            }
        }
        tableSelectorFuns[selectorFunId] = selectorFun

        invokeRequest(AppRequest(action = selectorAction.copy(selectorFunId = selectorFunId))) { appResponse: AppResponse ->
            val tableControl = TableControl(
                root = root,
                appControl = appControl,
                tableAction = selectorAction.copy(selectorFunId = selectorFunId),
                tableResponse = appResponse.table!!,
                tabId = -1,
            )
            root.selectorControl = tableControl
            tableControl.start()

            root.setWait(false)
        }
    }

    private fun setSelectorData(
        selectorData: Map<String, String>,
        fromField: String,
        toField: String,
        gridData: FormBaseCellClient?,
    ) {
        val toCell = gridData as? FormSimpleCellClient
        if (toCell?.data?.name == toField) {
            selectorData[fromField]?.let { fromData ->
                toCell.current.value = fromData
            }
        }
    }

    private fun clearSelector(selectorAction: AppAction) {
        selectorAction.selectorClear.forEach { (toField, clearValue) ->
            alHiddenCells.forEach { gridData ->
                clearSelectorData(toField, clearValue, gridData)
            }
            alGridRows.forEach { alGridRow ->
                alGridRow.forEach { gridData ->
                    clearSelectorData(toField, clearValue, gridData)
                }
            }
        }
    }

    private fun clearSelectorData(
        toField: String,
        clearValue: String,
        gridData: FormBaseCellClient?,
    ) {
        val toCell = gridData as? FormSimpleCellClient
        if (toCell?.data?.name == toField) {
            toCell.current.value = clearValue
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
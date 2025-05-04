package foatto.compose.control

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import foatto.compose.*
import foatto.compose.composable.FileLinkLifeTimeInputDialog
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.composable.StardartDialog
import foatto.compose.control.composable.onPointerEvents
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
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.AppRequest
import foatto.core.model.request.FormActionData
import foatto.core.model.request.FormActionRequest
import foatto.core.model.request.GetShortFileLinkRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.GetShortFileLinkResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.form.FormDateTimeCellMode
import foatto.core.model.response.form.FormPinMode
import foatto.core.model.response.form.FormResponse
import foatto.core.model.response.form.cells.FormBaseCell
import foatto.core.model.response.form.cells.FormBooleanCell
import foatto.core.model.response.form.cells.FormComboCell
import foatto.core.model.response.form.cells.FormDateTimeCell
import foatto.core.model.response.form.cells.FormFileCell
import foatto.core.model.response.form.cells.FormSimpleCell
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getLocalDateTime
import foatto.core.util.getRandomLong
import foatto.core.util.getTimeZone
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
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
        private val SCROLL_BAR_TICKNESS = 16.dp
    }

    private val alHiddenCells = mutableListOf<FormBaseCellClient>()
    private val alGridRows = mutableStateListOf<MutableList<FormBaseCellClient?>>()

    private var formBodyHeight: Float by mutableFloatStateOf(0.0f)

    private var vScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private val rowHeights = mutableStateMapOf<Int, Float>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val hmFormCellVisible = mutableMapOf<String, MutableMap<String, FormCellVisibleInfo>>()
    private val hmFormCellCaption = mutableMapOf<String, MutableMap<String, List<FormCellCaptionInfo>>>()

    private var formSaveActionType: String? = null
    private var formExitActionType: String? = null

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var datePickerData by mutableStateOf<FormDateTimeCellClient?>(null)
    private var timePickerData by mutableStateOf<FormDateTimeCellClient?>(null)

    private var isFileAddInProgress by mutableStateOf(false)
    private var fileGridData: FormFileCellClient? = null

    private var dialogActionFun: () -> Unit = {}
    private var dialogQuestion by mutableStateOf("")
    private var showDialogCancel by mutableStateOf(false)
    private var showDialog by mutableStateOf(false)
    private val dialogButtonOkText by mutableStateOf("OK")
    private val dialogButtonCancelText by mutableStateOf("Отмена")

    private var showFileLinkLifeTimeInputDialog by mutableStateOf(false)
    private var copyFileRef: Long = 0

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val clipboardManager = LocalClipboardManager.current
        val coroutineScope = rememberCoroutineScope()

        var isFilePickerVisible by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(
            initialDisplayMode = DisplayMode.Picker,
        )
        val timePickerState = rememberTimePickerState()
        val focusRequester = remember { FocusRequester() }

        var isFocusRequesterDefined = false

        datePickerData?.let { dateTimeData ->
            AlertDialog(
                shape = alertDialogShape,
                text = {
                    DatePicker(
                        colors = colorDatePicker ?: DatePickerDefaults.colors(),
                        state = datePickerState,
                    )
                },
                onDismissRequest = {
                    datePickerData = null
                },
                confirmButton = {
                    TextButton(
                        shape = singleButtonShape,
                        colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                dateTimeData.current.value = (millis / 1000).toInt()
                            }
                            datePickerData = null
                        }
                    ) {
                        Text(text = "OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        shape = singleButtonShape,
                        colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                        onClick = {
                            datePickerData = null
                        }
                    ) {
                        Text(text = "Отмена")
                    }
                },
            )
        }

        timePickerData?.let { dateTimeData ->
            val localDateTime = getLocalDateTime(
                timeOffset = root.appUserConfig.timeOffset,
                seconds = dateTimeData.current.value ?: Clock.System.now().epochSeconds.toInt()
            )

            timePickerState.selection = TimePickerSelectionMode.Hour
            timePickerState.is24hour = true
            timePickerState.hour = localDateTime.hour
            timePickerState.minute = localDateTime.minute

            AlertDialog(
                shape = alertDialogShape,
                text = {
                    TimePicker(
                        colors = colorTimePicker ?: TimePickerDefaults.colors(),
                        state = timePickerState,
                    )
                },
                onDismissRequest = {
                    timePickerData = null
                },
                confirmButton = {
                    TextButton(
                        shape = singleButtonShape,
                        colors = colorTextButtonDefault ?: ButtonDefaults.textButtonColors(),
                        onClick = {
                            dateTimeData.current.value = LocalDateTime(
                                year = localDateTime.year,
                                monthNumber = localDateTime.monthNumber,
                                dayOfMonth = localDateTime.dayOfMonth,
                                hour = timePickerState.hour,
                                minute = timePickerState.minute,
                                second = 0
                            ).toInstant(getTimeZone(root.appUserConfig.timeOffset)).epochSeconds.toInt()
                            timePickerData = null
                        }
                    ) {
                        Text(text = "OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        shape = singleButtonShape,
                        colors = colorTextButton ?: ButtonDefaults.textButtonColors(),
                        onClick = {
                            timePickerData = null
                        }
                    ) {
                        Text(text = "Отмена")
                    }
                },
            )
        }

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

        if (showDialog) {
            StardartDialog(
                question = dialogQuestion,
                buttonOkText = dialogButtonOkText,
                buttonCancelText = dialogButtonCancelText,
                showCancelButton = showDialogCancel,
                onOkClick = {
                    showDialog = false
                    dialogActionFun()
                },
                onCancelClick = { showDialog = false },
            )
        }

        if (showFileLinkLifeTimeInputDialog) {
            FileLinkLifeTimeInputDialog(
                onOkClick = { hour ->
                    showFileLinkLifeTimeInputDialog = false
                    coroutineScope.launch {
                        invokeRequest(GetShortFileLinkRequest(copyFileRef, hour)) { getShortFileLinkResponse: GetShortFileLinkResponse ->
                            clipboardManager.setText(AnnotatedString(getShortFileLinkResponse.url))

                            dialogActionFun = {}
                            dialogQuestion = "Новая ссылка скопирована в буфер обмена"
                            showDialogCancel = false
                            showDialog = true
                        }
                    }
                },
                onCancelClick = {
                    showFileLinkLifeTimeInputDialog = false
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().background(colorFormBack),
        ) {
            getHeader()

            //--- bugfix for verticalScrollState/horizontalScrollState value cashing
            key(verticalScrollState, horizontalScrollState) {
                Row(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxHeight()
                            .onSizeChanged { size ->
                                formBodyHeight = size.height.toFloat()
                            }
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
                        alGridRows.forEachIndexed { index, alGridRow ->
                            Row(
                                modifier = Modifier
                                    //.width(intrinsicSize = IntrinsicSize.Max) - оставлю на память, чтобы опять не искать, но пользы/разницы не увидел
                                    //--- чтобы ячейки занимали всю отведённую им (общую) высоту (работает только совместно с .fillMaxHeight())
                                    .height(intrinsicSize = IntrinsicSize.Max)
                                    .onSizeChanged { size ->
                                        rowHeights[index] = size.height.toFloat()
                                    },
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
                                                .padding(16.dp)
                                                .onSizeChanged { size ->
                                                    gridData?.let {
                                                        val componentWidth = with(density) {
                                                            maxDp(size.width.toDp(), gridData.minWidth.dp)
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
                                                    Text(text = gridData.current.value)
//                                            if (gridData.text.value == BR) {
//                                                Br()
//                                            } else {
//                                                Text(gridData.text.value)
//                                            }
                                                }

                                                is FormSimpleCellClient -> {
                                                    OutlinedTextField(
                                                        modifier = Modifier
                                                            .widthIn(max = root.scaledWindowWidth.dp / 2)
                                                            .then(
                                                                if (gridData.data.isEditable) {
                                                                    Modifier.background(colorControlBack)
                                                                } else {
                                                                    Modifier
                                                                }
                                                            ).then(
                                                                if (gridData.data.isEditable && gridData.data.isAutoFocus) {
                                                                    isFocusRequesterDefined = true
                                                                    Modifier.focusRequester(focusRequester)
                                                                } else {
                                                                    Modifier
                                                                }
                                                            ),
                                                        colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
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
                                                                .then(
                                                                    if (gridData.data.isEditable) {
                                                                        Modifier.background(Color.Transparent)
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                ),
                                                            colors = colorCheckBox ?: CheckboxDefaults.colors(),
                                                            checked = gridData.current.value,
                                                            onCheckedChange = { newState ->
                                                                if (gridData.data.isEditable) {
                                                                    gridData.current.value = newState
                                                                    doVisibleAndCaptionChange(gridData)
                                                                }
                                                            },
                                                        )
                                                        gridData.error?.let { error ->
                                                            Text(
                                                                text = error,
                                                                color = Color.Red,
                                                            )
                                                        }
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
                                                    val (dateString, timeString) = gridData.current.value?.let { curDateTimeValue ->
                                                        val dateTimeString = getDateTimeDMYHMSString(
                                                            root.appUserConfig.timeOffset,
                                                            curDateTimeValue,
                                                        )
                                                        when (gridData.data.mode) {
                                                            FormDateTimeCellMode.DMY -> dateTimeString.substring(0, 10) to null
                                                            FormDateTimeCellMode.DMYHMS -> dateTimeString.substring(0, 10) to dateTimeString.substring(11)
                                                        }
                                                    } ?: run {
                                                        when (gridData.data.mode) {
                                                            FormDateTimeCellMode.DMY -> {
                                                                if (gridData.data.isEditable) {
                                                                    "-" to null
                                                                } else {
                                                                    "" to null
                                                                }
                                                            }

                                                            FormDateTimeCellMode.DMYHMS -> {
                                                                if (gridData.data.isEditable) {
                                                                    "-" to "-"
                                                                } else {
                                                                    "" to ""
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Text(text = dateString)
                                                    if (gridData.data.isEditable) {
                                                        FilledIconButton(
                                                            shape = singleButtonShape,
                                                            colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                            onClick = {
                                                                val curDateTime = gridData.current.value?.let { seconds ->
                                                                    seconds * 1000L
                                                                } ?: Clock.System.now().toEpochMilliseconds()

                                                                datePickerState.displayedMonthMillis = curDateTime
                                                                datePickerState.selectedDateMillis = curDateTime

                                                                datePickerData = gridData
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = null,
                                                            )
                                                        }
                                                    }
                                                    //--- time fields on new line on narrow screens
//                                            if (styleIsNarrowScreen && index == 3) {
//                                                Br()
//                                            }
                                                    if (!gridData.data.isEditable) {
                                                        Text(modifier = Modifier.padding(8.dp), text = "")
                                                    }

                                                    timeString?.let {
                                                        Text(text = timeString)
                                                        if (gridData.data.isEditable) {
                                                            FilledIconButton(
                                                                shape = singleButtonShape,
                                                                colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                onClick = {
                                                                    timePickerData = gridData
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit,
                                                                    contentDescription = null,
                                                                )
                                                            }
                                                        }
                                                    }
//                                                id(getFocusId(gridData.id, gridData.alSubId?.get(index)))
//                                                onFocusIn { syntheticFocusEvent ->
//                                                    selectAllText(syntheticFocusEvent)
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
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
                                                                        colors = colorRadioButton ?: RadioButtonDefaults.colors(),
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
                                                                .onGloballyPositioned { coordinates ->
                                                                    textFieldSize = coordinates.size.toSize()
                                                                }.then(
                                                                    if (gridData.data.isEditable) {
                                                                        Modifier.background(colorControlBack)
                                                                    } else {
                                                                        Modifier
                                                                    }
                                                                ),
                                                            colors = colorOutlinedTextInput ?: OutlinedTextFieldDefaults.colors(),
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
                                                    if (gridData.data.isEditable && gridData.data.maxFileCount > gridData.files.size) {
                                                        if (isFileAddInProgress) {
                                                            CircularProgressIndicator()
                                                        } else {
                                                            ImageOrTextFromNameControl(
                                                                name = "/images/ic_add_${getStyleOtherIconNameSuffix()}.png",
                                                                iconSize = styleOtherIconSize,
                                                                imageButton = { func ->
                                                                    FilledIconButton(
                                                                        shape = singleButtonShape,
                                                                        colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
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
                                                    }
                                                    Column {
                                                        for (fileData in gridData.files) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                Button(
                                                                    shape = singleButtonShape,
                                                                    colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                                                                    enabled = fileData.id >= 0,
                                                                    onClick = {
                                                                        if (fileData.id >= 0) {
                                                                            coroutineScope.launch {
                                                                                showFile(fileData.action)
                                                                            }
                                                                        }
                                                                    },
                                                                ) {
                                                                    Text(text = fileData.name)
                                                                }
                                                                ImageOrTextFromNameControl(
                                                                    name = IconName.COPY,
                                                                    iconSize = styleOtherIconSize,
                                                                    imageButton = { func ->
                                                                        FilledIconButton(
                                                                            shape = singleButtonShape,
                                                                            colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                            onClick = {
                                                                                if (fileData.id >= 0) {
                                                                                    copyFileRef = fileData.ref
                                                                                    showFileLinkLifeTimeInputDialog = true
                                                                                }
                                                                            }
                                                                        ) {
                                                                            func()
                                                                        }
                                                                    },
                                                                    textButton = {},
                                                                )
                                                                if (gridData.data.isEditable) {
                                                                    ImageOrTextFromNameControl(
                                                                        name = "/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
                                                                        iconSize = styleOtherIconSize,
                                                                        imageButton = { func ->
                                                                            FilledIconButton(
                                                                                shape = singleButtonShape,
                                                                                colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                                onClick = {
                                                                                    if (!isFileAddInProgress) {
                                                                                        coroutineScope.launch {
                                                                                            deleteFile(gridData, fileData.id)
                                                                                        }
                                                                                    }
                                                                                }
                                                                            ) {
                                                                                func()
                                                                            }
                                                                        },
                                                                        textButton = {},
                                                                    )
//                                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                                                }
                                                            }
                                                        }
                                                        gridData.error?.let { error ->
                                                            Text(
                                                                text = error,
                                                                color = Color.Red,
                                                            )
                                                        }
                                                    }
//                                            id(getFocusId(gridData.id))
                                                }
                                            }

                                            if (gridData is FormSimpleCellClient) {
                                                gridData.data.selectorAction?.let { selectorAction ->
                                                    ImageOrTextFromNameControl(
                                                        name = "/images/ic_reply_${getStyleOtherIconNameSuffix()}.png",
                                                        iconSize = styleOtherIconSize,
                                                        imageButton = { func ->
                                                            FilledIconButton(
                                                                shape = singleButtonShape,
                                                                colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                onClick = {
                                                                    if (!isFileAddInProgress) {
                                                                        coroutineScope.launch {
                                                                            callSelector(selectorAction)
                                                                        }
                                                                    }
                                                                }
                                                            ) {
                                                                func()
                                                            }
                                                        },
                                                        textButton = {},
                                                    )
//                                                doKeyUp(gridData, syntheticKeyboardEvent)

                                                    ImageOrTextFromNameControl(
                                                        name = "/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
                                                        iconSize = styleOtherIconSize,
                                                        imageButton = { func ->
                                                            FilledIconButton(
                                                                shape = singleButtonShape,
                                                                colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
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
////                                                doKeyUp(gridData, syntheticKeyboardEvent)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollBody()
                }
            }

            //--- Form Button Bar
//                        borderTop(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
//                        padding(styleControlPadding)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(colorBottomBar),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (formButton in formResponse.alFormButton) {
                    ImageOrTextFromNameControl(
                        name = formButton.name,
                        iconSize = styleOtherIconSize,
                        imageButton = { func ->
                            FilledIconButton(
                                modifier = Modifier.padding(2.dp),
                                shape = singleButtonShape,
                                colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                enabled = !isFileAddInProgress,
                                onClick = {
                                    if (!isFileAddInProgress) {
                                        coroutineScope.launch {
                                            call(formButton)
                                        }
                                    }
                                },
                            ) {
                                func()
                            }
                        },
                        textButton = { caption ->
                            Button(
                                modifier = Modifier.padding(2.dp),
                                shape = singleButtonShape,
                                colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                                border = BorderStroke(
                                    width = 2.dp,
                                    color = Color.Black,
                                ),
                                enabled = !isFileAddInProgress,
                                onClick = {
                                    if (!isFileAddInProgress) {
                                        coroutineScope.launch {
                                            call(formButton)
                                        }
                                    }
                                },
                            ) {
                                Text(
                                    text = caption,
//                                    fontWeight = if (gridData.isBoldText) {
//                                        FontWeight.Bold
//                                    } else {
//                                        null
//                                    },
                                )
                            }
                        },
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

        LaunchedEffect(Unit) {
            if (isFocusRequesterDefined) {
                focusRequester.requestFocus()
            }
        }
    }

    @Composable
    private fun VerticalScrollBody() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Canvas(
            modifier = Modifier
                .width(SCROLL_BAR_TICKNESS)
                .fillMaxHeight()
                .onSizeChanged { size ->
                    vScrollBarLength = size.height.toFloat()
                }
                .clipToBounds()
                .onPointerEvents(
                    withInteractive = true,
                    onPointerDown = { pointerInputChange -> },
                    onPointerUp = { pointerInputChange -> },
                    onDragStart = { offset -> },
                    onDrag = { pointerInputChange, offset ->
                        val dy = offset.y
                        if (
                            dy < 0 && verticalScrollState.value > 0 ||
                            dy > 0 && verticalScrollState.value < verticalScrollState.maxValue
                        ) {
                            coroutineScope.launch {
                                val rowHeightSum = rowHeights.values.sum()
                                val incScaleY = max(1.0f, rowHeightSum / vScrollBarLength)
                                verticalScrollState.scrollBy(dy * incScaleY)
                                //verticalScrollState.scrollTo((verticalScrollState.value + dy * incScaleY).toInt())
                            }
                        }
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                )
        ) {
            drawRect(
                topLeft = Offset(0.0f, 0.0f),
                size = Size(with(density) { SCROLL_BAR_TICKNESS.toPx() }, vScrollBarLength),
                color = colorScrollBarBack,
                style = Fill,
            )
            val rowHeightSum = rowHeights.values.sum()
            val decScaleY = min(1.0f, formBodyHeight / rowHeightSum)
            val scrollBarH = vScrollBarLength * decScaleY
            val scrollBarY = if (formBodyHeight >= rowHeightSum) {
                0.0f
            } else {
                (vScrollBarLength - scrollBarH) * verticalScrollState.value / verticalScrollState.maxValue
            }
            drawRect(
                topLeft = Offset(0.0f, scrollBarY),
                size = Size(with(density) { SCROLL_BAR_TICKNESS.toPx() }, scrollBarH),
                color = colorScrollBarFore,
                style = Fill,
            )
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    override suspend fun start() {
        root.setTabInfo(tabId, formResponse.tabCaption)
        headerData = formResponse.headerData

        if (formResponse.alFormColumn.isEmpty()) {
            readClassicForm()
        } else {
            readGridForm()
        }
    }

    private fun readClassicForm() {
        alHiddenCells.clear()
        alGridRows.clear()

        var rowIndex = 0
        var colIndex = 0
        val alFormCellMasterPreAction = mutableListOf<FormBaseCellClient>()

        //--- хотя бы один столбец с полями всё равно будет
        var maxColCount = 1
        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormDataCell(
                    formCell = formCell,
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

                maxColCount = max(maxColCount, colIndex - 1)
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

        getGridFormTopBottomCaptions(formResponse.alFormColumn, 0)

        for (formCell in formResponse.alFormCell) {
            //--- поле без заголовка считается невидимым (hidden)
            if (formCell.caption.isEmpty()) {
                val formGridData = getFormDataCell(
                    formCell = formCell,
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
            minWidth = formCell.minWidth,
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
                minWidth = 0,
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
        minWidth = formCell.minWidth,
        caption = formCell.caption,
        align = Arrangement.End,
    )

    private fun getGridFormRightCaption(formCell: FormBaseCell) = FormLabelCellClient(
        cellName = "",
        minWidth = formCell.minWidth,
        caption = formCell.caption,
        align = Arrangement.Start,
    )

    private fun getFormEmptyCell() = FormLabelCellClient(
        cellName = "",
        minWidth = 0,
        caption = "", //BR,
        align = Arrangement.Center,
    )

    private fun getFormDataCell(
        formCell: FormBaseCell,
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
                    minWidth = formCell.minWidth,
                    align = align,
                    data = formCell,
                )
            }

            is FormBooleanCell -> {
                FormBooleanCellClient(
                    cellName = formCell.name,
                    minWidth = formCell.minWidth,
                    align = align,
                    data = formCell,
                )
            }

            is FormDateTimeCell -> {
                FormDateTimeCellClient(
                    cellName = formCell.name,
                    minWidth = formCell.minWidth,
                    align = align,
                    data = formCell,
                ).apply {
//                    formCell.value.indices.forEach { i ->
//                        alSubFocusId.add(i)
//                    }
                }
            }

            is FormComboCell -> {
                FormComboCellClient(
                    cellName = formCell.name,
                    minWidth = formCell.minWidth,
                    align = align,
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
                    minWidth = formCell.minWidth,
                    align = align,
                    data = formCell,
                ).apply {
                    files.clear()
                    files.addAll(formCell.files)
                }
            }

        }

        return gridData
    }

    private fun readFormButtons() {
        formResponse.alFormButton.forEach { formButton ->
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

    private suspend fun showFile(action: AppAction) {
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

    private suspend fun call(formButton: FormButton) {
        doInvoke(formButton)
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

    private suspend fun doInvoke(formButton: FormButton) {
        val formActionData = mutableMapOf<String, FormActionData>()

        alHiddenCells.forEach { gridData ->
            fillFormData(gridData, formButton.withNewData, formActionData)
        }
        alGridRows.forEach { alGridRow ->
            alGridRow.forEach { gridData ->
                gridData?.let {
                    fillFormData(gridData, formButton.withNewData, formActionData)
                }
            }
        }

        invokeRequest(
            FormActionRequest(
                action = formAction.copy(
                    type = formButton.actionType,
                    module = formAction.module,
                    id = formResponse.id,
                    prevAction = formResponse.prevAction,
                    params = formButton.params,
                ),
                formActionData = formActionData,
            )
        ) { formActionResponse: FormActionResponse ->
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
                    formActionResponse.newTabAction?.let { newTabAction ->
                        root.openTab(newTabAction)
                    } ?: formActionResponse.nextAction?.let { nextAction ->
                        appControl.call(AppRequest(nextAction))
                    } ?: formResponse.prevAction?.let { prevAction ->
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
                    dateTimeValue = if (withNewValues) {
                        gridData.current.value
                    } else {
                        gridData.data.value
                    }
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

    private suspend fun callSelector(selectorAction: AppAction) {
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
        min(initSize, root.scaledWindowWidth / 19)
    } else {
        min(initSize, 30)
    }

}
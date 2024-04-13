package foatto.core_compose.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import foatto.core_compose.utils.maxDp
import kotlin.math.max
import kotlin.math.min

class FormControl(
    private val root: Root,
    private val appControl: AppControl,
    private val formResponse: FormResponse,
    tabId: Int,
) : AbstractControl(tabId) {

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

//    private var addStyleGrid: StyleScope.() -> Unit = {}

    @Composable
    override fun Body() {
        val density = LocalDensity.current

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getTableAndFormHeader(true) { url: String ->
                call(url, false, null)
            }

            Column(
                modifier = Modifier
                    .verticalScroll(state = verticalScrollState)
                    .horizontalScroll(state = horizontalScrollState)
                    .weight(1.0f)
//                        addStyleGrid()
            ) {
                for (alGridRow in alGridRows) {
                    Row(
                        modifier = Modifier.height(intrinsicSize = IntrinsicSize.Max)
                    ) {
                        alGridRow.forEachIndexed { col, gridData ->
//                                    gridData.style(this)
//                                    gridData.styleAdd(this)
//                                if (!gridData.isVisible.value) {
//                                    hidden()
//                                }
                            val originalModifier = Modifier
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = originalModifier
                                    .fillMaxHeight()
                                    .border(BorderStroke(width = 1.dp, color = Color.LightGray))
                                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                                    .onGloballyPositioned { layoutCoordinates ->
                                        gridData?.let {
                                            val componentWidth = with(density) {
                                                maxDp(layoutCoordinates.size.width.toDp(), 20.dp)    //!!! minWidth
                                            }
                                            gridData.componentWidth = maxDp(gridData.componentWidth, componentWidth)
                                            hmColumnMaxWidth[col] = maxDp(componentWidth, hmColumnMaxWidth.getOrElse(col) { 0.dp })
                                        }
                                    }
                                    .then(
                                        gridData?.let {
                                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                                if (gridData.componentWidth < maxColWidth) {
                                                    Modifier.width(maxColWidth)
                                                } else {
                                                    Modifier.width(gridData.componentWidth)//originalModifier
                                                }
                                            }
                                        } ?: originalModifier
                                    )

                            ) {
                                when (gridData?.cellType) {
                                    FormCellTypeClient.LABEL -> {
                                        Text(text = gridData.text.value)
//                                                    fontSize(styleControlTextFontSize)
//                                                    fontWeight(styleFormLabelWeight)
//                                            if (gridData.text.value == BR) {
//                                                Br()
//                                            } else {
//                                                Text(gridData.text.value)
//                                            }
                                    }

                                    FormCellTypeClient.STRING -> {
                                        OutlinedTextField(
                                            value = gridData.text.value,
                                            onValueChange = { newText ->
                                                gridData.text.value = newText
                                            },
//                                            label = { Text("Имя") },
                                            singleLine = true,
                                            visualTransformation = if (gridData.isPassword) {
                                                PasswordVisualTransformation()
                                            } else {
                                                VisualTransformation.None
                                            },
                                            //!!! обязательно ли использование?
                                            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        )
//                                                backgroundColor(COLOR_MAIN_BACK_0)
//                                                setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
//                                                fontSize(styleControlTextFontSize)
//                                                padding(styleCommonEditorPadding)
//                                                setMargins(arrStyleCommonMargin)
//                                            id(getFocusId(gridData.id))
//                                            size(gridData.colCount)
//                                            if (gridData.isReadOnly) {
//                                                readOnly()
//                                            }
//                                            onFocusIn { syntheticFocusEvent ->
//                                                selectAllText(syntheticFocusEvent)
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.TEXT -> {
                                        OutlinedTextField(
                                            value = gridData.text.value,
                                            onValueChange = { newText ->
                                                gridData.text.value = newText
                                            },
//                                            label = { Text("Имя") },
                                            singleLine = false,
                                            minLines = gridData.rowCount,
                                            maxLines = gridData.rowCount,
                                        )
//                                                backgroundColor(COLOR_MAIN_BACK_0)
//                                                setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
//                                                fontSize(styleControlTextFontSize)
//                                                padding(styleCommonEditorPadding)
//                                                setMargins(arrStyleCommonMargin)
//                                            id(getFocusId(gridData.id))
//                                            cols(gridData.colCount)
//                                            if (gridData.isReadOnly) {
//                                                readOnly()
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.CHECKBOX -> {
                                        Checkbox(
                                            checked = gridData.bool.value,
                                            onCheckedChange = { newState ->
                                                gridData.bool.value = newState
                                                if (!gridData.isReadOnly) {
                                                    doVisibleAndCaptionChange(gridData)
                                                }
                                            },
                                        )
//                                                appearance("none")
//                                                width(styleCheckBoxWidth)
//                                                height(styleCheckBoxHeight)
//                                                backgroundColor(COLOR_MAIN_BACK_0)
//                                                setBorder(getStyleCheckBoxBorder())
//                                                margin(styleFormCheckboxAndRadioMargin)
//                                            id(getFocusId(gridData.id))
//                                            if (gridData.isReadOnly) {
//                                                readOnly()
//                                            }
                                        //onInput { syntheticInputEvent -> - тоже можно
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
                                    }

                                    FormCellTypeClient.SWITCH -> {
                                        //!!! заменить на настоящий switch
                                        Checkbox(
                                            checked = gridData.bool.value,
                                            onCheckedChange = { newState ->
                                                gridData.bool.value = newState
                                                if (!gridData.isReadOnly) {
                                                    doVisibleAndCaptionChange(gridData)
                                                }
                                            },
                                        )
//                                                    backgroundColor(
//                                                        if (gridData.bool.value) {
//                                                            colorMainBack1
//                                                        } else {
//                                                            colorFormSwitchBackOn
//                                                        }
//                                                    )
//                                                    setBorder(color = colorMainBorder, radius = styleButtonBorderRadius)
//                                                    fontSize(styleCommonButtonFontSize)
//                                                    padding(styleFileNameButtonPadding)
//                                                    cursor("pointer")
//                                                id(getFocusId(gridData.id, 0))
//                                                if (gridData.isReadOnly) {
//                                                    disabled()
//                                                }
//                                                title(gridData.alSwitchText[0])
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

//                                                    backgroundColor(
//                                                        if (gridData.bool.value) {
//                                                            colorFormSwitchBackOn
//                                                        } else {
//                                                            colorMainBack1
//                                                        }
//                                                    )
//                                                    setBorder(color = colorMainBorder, radius = styleButtonBorderRadius)
//                                                    fontSize(styleCommonButtonFontSize)
//                                                    padding(styleFileNameButtonPadding)
//                                                    cursor("pointer")
//                                                id(getFocusId(gridData.id, 1))
//                                                if (gridData.isReadOnly) {
//                                                    disabled()
//                                                }
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
                                            OutlinedTextField(
                                                value = gridData.alDateTime[index].value,
                                                onValueChange = { newText ->
                                                    gridData.alDateTime[index].value = newText
                                                },
                                                //                                            label = { Text("Имя") },
                                                singleLine = true,
                                            )
//                                                    backgroundColor(COLOR_MAIN_BACK_0)
//                                                    setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
//                                                    fontSize(styleControlTextFontSize)
//                                                    padding(styleCommonEditorPadding)
//                                                    setMargins(arrStyleCommonMargin)
//                                                id(getFocusId(gridData.id, gridData.alSubId?.get(index)))
//                                                size(
//                                                    if (index == 2 && gridData.cellType != FormCellTypeClient.TIME) {
//                                                        2
//                                                    } else {
//                                                        1
//                                                    }
//                                                )
//                                                if (gridData.isReadOnly) {
//                                                    readOnly()
//                                                }
//                                                onFocusIn { syntheticFocusEvent ->
//                                                    selectAllText(syntheticFocusEvent)
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
                                        }
                                    }

//                                    FormCellTypeClient.COMBO -> {
//                                        Select(
//                                            attrs = {
//                                                style {
//                                                    fontSize(styleControlTextFontSize)
//                                                    padding(styleCommonEditorPadding)
//                                                    setMargins(arrStyleCommonMargin)
//                                                }
//                                                id(getFocusId(gridData.id))
//                                                if (gridData.isReadOnly) {
//                                                    disabled()
//                                                }
//                                                //onInput { syntheticInputEvent -> - тоже можно
//                                                onChange { syntheticChangeEvent ->
//                                                    if (!gridData.isReadOnly) {
//                                                        syntheticChangeEvent.value?.let { value ->
//                                                            gridData.combo.value = value.toInt()
//                                                            doVisibleAndCaptionChange(gridData)
//                                                        }
//                                                    }
//                                                }
//                                                onKeyUp { syntheticKeyboardEvent ->
//                                                    doKeyUp(gridData, syntheticKeyboardEvent)
//                                                }
//                                            }
//                                        ) {
//                                            for (comboData in gridData.alComboData) {
//                                                Option(
//                                                    value = comboData.value.toString(),
//                                                    attrs = {
//                                                        if (comboData.value == gridData.combo.value) {
//                                                            selected()
//                                                        }
//                                                    }
//                                                ) {
//                                                    Text(comboData.text)
//                                                }
//                                            }
//                                        }
//                                    }

//                                    FormCellTypeClient.RADIO -> {
//                                        RadioGroup(
//                                            checkedValue = gridData.combo.value.toString(),
//                                        ) {
//                                            gridData.alComboData.forEachIndexed { index, comboData ->
//                                                val focusId = getFocusId(gridData.id, gridData.alSubId?.get(index))
//                                                RadioInput(
//                                                    value = comboData.value.toString(),
//                                                ) {
//                                                    style {
//                                                        transform {
//                                                            scale((if (styleIsNarrowScreen) COMMON_FONT_SIZE * 1.5 else COMMON_FONT_SIZE * 1.5).value)
//                                                        }
//                                                        margin(styleFormCheckboxAndRadioMargin)
//                                                    }
//                                                    id(focusId)
//                                                    if (gridData.isReadOnly) {
//                                                        readOnly()
//                                                    }
//                                                    onInput { syntheticInputEvent ->
//                                                        gridData.combo.value = comboData.value
//                                                        if (!gridData.isReadOnly) {
//                                                            doVisibleAndCaptionChange(gridData)
//                                                        }
//                                                    }
//                                                    onKeyUp { syntheticKeyboardEvent ->
//                                                        doKeyUp(gridData, syntheticKeyboardEvent)
//                                                    }
//                                                }
//                                                Label(
//                                                    forId = focusId,
//                                                    attrs = {
//                                                        style {
//                                                            fontSize(styleControlTextFontSize)
//                                                        }
//                                                    }
//                                                ) {
//                                                    Text(comboData.text)
//                                                }
//                                                Br()
//                                            }
//                                        }
//                                    }

//                                    FormCellTypeClient.FILE -> {
//                                        for (fileData in gridData.alFileData) {
//                                            Div(
//                                                attrs = {
//                                                    style {
//                                                        display(DisplayStyle.Flex)
//                                                        flexDirection(FlexDirection.Row)
//                                                        flexWrap(FlexWrap.Wrap)
//                                                        justifyContent(JustifyContent.FlexStart)
//                                                        alignItems(AlignItems.Center)
//                                                    }
//                                                }
//                                            ) {
//                                                Button(
//                                                    attrs = {
//                                                        style {
//                                                            backgroundColor(getColorButtonBack())
//                                                            setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                                            fontSize(styleCommonButtonFontSize)
//                                                            padding(styleFileNameButtonPadding)
//                                                            margin(styleFileNameButtonMargin)
//                                                            cursor("pointer")
//                                                        }
//                                                        title("Показать файл")
//                                                        if (fileData.id < 0) {
//                                                            disabled()
//                                                        }
//                                                        onClick {
//                                                            if (fileData.id >= 0) {
//                                                                showFile(fileData.url)
//                                                            }
//                                                        }
//                                                        onKeyUp { syntheticKeyboardEvent ->
//                                                            doKeyUp(gridData, syntheticKeyboardEvent)
//                                                        }
//                                                    }
//                                                ) {
//                                                    Text(fileData.text)
//                                                }
//                                                if (!gridData.isReadOnly) {
//                                                    Img(
//                                                        src = "/web/images/ic_delete_forever_black_48dp.png",
//                                                        attrs = {
//                                                            style {
//                                                                backgroundColor(getColorFormButtonBack())
//                                                                setBorder(getStyleFormButtonBorder())
//                                                                fontSize(styleCommonButtonFontSize)
//                                                                padding(styleIconButtonPadding)
//                                                                setMargins(arrStyleCommonMargin)
//                                                                cursor("pointer")
//                                                            }
//                                                            title("Удалить файл")
//                                                            onClick {
//                                                                deleteFile(gridData, fileData)
//                                                            }
//                                                            onKeyUp { syntheticKeyboardEvent ->
//                                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                                            }
//                                                        }
//                                                    )
//                                                }
//                                            }
//                                        }
//                                        Br()
//                                        Input(InputType.File) {
//                                            style {
//                                                display(DisplayStyle.None)
//                                            }
//                                            id(getFocusId(gridData.id))
//                                            multiple()
//                                            onInput { syntheticInputEvent ->
//                                                //filePath = it.value - from examples
//                                                val files = (syntheticInputEvent.target as? HTMLInputElement)?.files
//                                                addFile(gridData, files)
//                                            }
//                                        }
//                                        if (!gridData.isReadOnly) {
//                                            Button(
//                                                attrs = {
//                                                    style {
//                                                        backgroundColor(getColorButtonBack())
//                                                        setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                                        fontSize(styleCommonButtonFontSize)
//                                                        padding(styleFileNameButtonPadding)
//                                                        margin(styleFileNameButtonMargin)
//                                                        cursor("pointer")
//                                                    }
//                                                    title("Добавить файл(ы)")
//                                                    onClick {
//                                                        addFileDialog(getFocusId(gridData.id))
//                                                    }
//                                                    onKeyUp { syntheticKeyboardEvent ->
//                                                        doKeyUp(gridData, syntheticKeyboardEvent)
//                                                    }
//                                                }
//                                            ) {
//                                                Text("Добавить файл(ы)")
//                                            }
//                                        }
//                                    }

                                    else -> {}
                                }
                            }

//                                if (gridData.selectorSetURL.isNotEmpty()) {
//                                    Img(
//                                        src = "/web/images/ic_reply_black_48dp.png",
//                                        attrs = {
//                                            style {
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                fontSize(styleCommonButtonFontSize)
//                                                padding(styleIconButtonPadding)
//                                                setMargins(arrStyleCommonMargin)
//                                                cursor("pointer")
//                                            }
//                                            title("Выбрать из справочника")
//                                            onClick {
//                                                call(gridData.selectorSetURL, true, null)
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
//                                        }
//                                    )
//                                }

//                                if (gridData.selectorClearURL.isNotEmpty()) {
//                                    Img(
//                                        src = "/web/images/ic_delete_forever_black_48dp.png",
//                                        attrs = {
//                                            style {
//                                                backgroundColor(getColorFormButtonBack())
//                                                setBorder(getStyleFormButtonBorder())
//                                                fontSize(styleCommonButtonFontSize)
//                                                padding(styleIconButtonPadding)
//                                                setMargins(arrStyleCommonMargin)
//                                                cursor("pointer")
//                                            }
//                                            title("Очистить выбор")
//                                            onClick {
//                                                call(gridData.selectorClearURL, true, null)
//                                            }
//                                            onKeyUp { syntheticKeyboardEvent ->
//                                                doKeyUp(gridData, syntheticKeyboardEvent)
//                                            }
//                                        }
//                                    )
//                                }

//                                if (gridData.error.isNotEmpty()) {
//                                    Div(
//                                        attrs = {
//                                            style {
//                                                color(Color.red)
//                                                fontSize(styleControlTextFontSize)
//                                                fontWeight("bold")
//                                                setMargins(arrStyleCommonMargin)
//                                            }
//                                        }
//                                    ) {
//                                        Text(gridData.error)
//                                    }
//                                }
                        }
                    }
                }
            }

            //--- Form Button Bar
//                        justifyContent(
//                            if (!styleIsNarrowScreen) {
//                                JustifyContent.Center
//                            } else {
//                                JustifyContent.SpaceBetween
//                            }
//                        )
//                        alignItems(AlignItems.Center)
//                        backgroundColor(getColorFormBack())
//                        borderTop(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
//                        padding(styleControlPadding)
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                for (formButton in alFormButton) {
                    Button(
                        onClick = {
                            call(formButton.url, formButton.withNewData, formButton.question)
                        }
                    ) {
                        ImageOrTextFromName(formButton.name, formButton.tooltip)
//                                    backgroundColor(
//                                        if (formButton.withNewData) {
//                                            getColorFormActionButtonSaveBack()
//                                        } else {
//                                            getColorFormActionButtonOtherBack()
//                                        }
//                                    )
//                                    setBorder(getStyleFormActionButtonBorder())
//                                    fontSize(styleCommonButtonFontSize)
//                                    padding(styleIconButtonPadding)
//                                    setMargins(arrStyleCommonMargin)
//                                    cursor("pointer")
                    }
                }
            }
            /*
                            gridData.alButtonCellData?.let { alButtonCellData ->
                                for (cellData in alButtonCellData) {
                                }
                            }
             */
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

//            isGridForm = formResponse.alFormColumn.isNotEmpty()
            val formGridData = getFormDataCell(
                formCell = formCell,
                gridCellId = gridCellId,
//                isHidden = false,
//                rowIndex = rowIndex,
//                columnIndex = columnIndex,
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

        //--- если это грид-форма, то сделать одинаковые ячейки, если обычная - поделить между caption и edit в пропорции 1fr и 2fr
//        addStyleGrid = {
//            gridTemplateRows("repeat(${rowIndex + 1},max-content)")
//            gridTemplateColumns("repeat($maxColumnCount,auto)")
//        }

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
//                        style = {
//                            gridArea("${ri + 1}", "${ci + 1}", "${ri + 2}", "${ci + 2}")
//                            justifySelf("center")
//                            alignSelf(AlignSelf.Center)
//                            padding(styleFormRowPadding)
//                        },
//                        styleAdd = {},
                    aText = caption,
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
//        style = {
//            gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
//            justifySelf("flex-start")
//            alignSelf(AlignSelf.Center)
//        },
//        styleAdd = {},
        aText = formCell.caption,
    )

    private fun getFormEmptyCell(
        gridCellId: Int,
        rowIndex: Int,
    ) = FormGridData(
        cellType = FormCellTypeClient.LABEL,
        id = gridCellId,
        cellName = "",
//        style = {
//            gridArea("${rowIndex + 1}", "1", "${rowIndex + 2}", "2")
//        },
//        styleAdd = {},
        aText = "", //BR,
    )

    private fun getFormLabelCell(
        formCell: FormCell,
        gridCellId: Int,
    ): FormGridData {
//        val isWideOrGrid = root.isWideScreen || formResponse.alFormColumn.isNotEmpty()
        return FormGridData(
            cellType = FormCellTypeClient.LABEL,
            id = gridCellId,
            cellName = "",
//            style = {
//                gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
//                justifySelf(
//                    if (isWideOrGrid) {
//                        "flex-end"
//                    } else {
//                        "flex-start"
//                    }
//                )
//                alignSelf(AlignSelf.Center)
//                paddingTop(
//                    if (isWideOrGrid) {
//                        styleFormRowTopBottomPadding
//                    } else {
//                        styleFormLabelPadding
//                    }
//                )
//                paddingRight(
//                    if (isWideOrGrid) {
//                        styleFormRowPadding
//                    } else {
//                        0.px
//                    }
//                )
//                paddingBottom(styleFormRowTopBottomPadding)
//                paddingLeft(
//                    if (isWideOrGrid) {
//                        0.px
//                    } else {
//                        styleFormLabelPadding
//                    }
//                )
//            },
//            styleAdd = {},
            aText = formCell.caption,
        )
    }

    private fun getFormDataCell(
        formCell: FormCell,
        gridCellId: Int,
//        isHidden: Boolean,
//        isGridForm: Boolean = false
    ): FormGridData {

//        val style: StyleScope.() -> Unit = {
//            gridArea("${rowIndex + 1}", "${columnIndex + 1}", "${rowIndex + 2}", "${columnIndex + 2}")
//            justifySelf(
//                if (isGridForm) {
//                    "center"
//                } else {
//                    "flex-start"
//                }
//            )
//            alignSelf(AlignSelf.Center)
//            paddingTop(
//                if (!styleIsNarrowScreen || isGridForm) {
//                    styleFormRowTopBottomPadding
//                } else {
//                    styleFormRowPadding
//                }
//            )
//            paddingRight(0.px)
//            paddingBottom(
//                if (!styleIsNarrowScreen || isGridForm) {
//                    styleFormRowTopBottomPadding
//                } else {
//                    styleFormRowPadding
//                }
//            )
//            paddingLeft(
//                if (isGridForm) {
//                    0.px
//                } else {
//                    styleFormRowPadding
//                }
//            )
//        }
//        //--- добавляем отдельно только для тех строк, где есть select-кнопки,
//        //--- иначе разъезжаются radio-buttons в одну строчку (а не в один столбец)
//        val styleAdd: StyleScope.() -> Unit = if (formCell.selectorSetURL.isNotBlank()) {
//            {
//                display(DisplayStyle.Flex)
//                flexDirection(FlexDirection.Row)
//                flexWrap(FlexWrap.Wrap)
//                alignItems(AlignItems.Center)
//            }
//        } else {
//            {}
//        }

        val gridData = when (formCell.cellType) {
            FormCellType.STRING, FormCellType.INT, FormCellType.DOUBLE -> {
                FormGridData(
                    cellType = FormCellTypeClient.STRING,
                    id = gridCellId,
                    cellName = formCell.strName,
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
//                    style = style,
//                    styleAdd = styleAdd,
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
                            gdMaster.bool.value
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
                    gdMaster.combo.value
                }

                FormCellTypeClient.RADIO -> if (hmHiddenCells.contains(gdMaster.id)) {
                    gdMaster.oldCombo
                } else {
                    gdMaster.combo.value
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
                            gdSlave.text.value = fcci.caption
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

//    private fun showFile(url: String) {
//        root.openTab(url)
//    }

//    private fun deleteFile(gridData: FormGridData, fileData: FormFileData) {
//        gridData.alFileData.removeAll { formFileData ->
//            formFileData.id == fileData.id
//        }
//        //--- сохраним ID удаляемого файла для передачи на сервер
//        if (fileData.id > 0) {
//            gridData.alFileRemovedId.add(fileData.id)
//        }
//        //--- или просто удалим ранее добавленный файл из списка
//        else {
//            gridData.hmFileAdd.remove(fileData.id)
//        }
//    }

//    private fun addFileDialog(id: String) {
//        (document.getElementById(id) as HTMLElement).click()
//    }

//    private fun addFile(gridData: FormGridData, files: FileList?) {
//        files?.let {
//            val hmFileAdd = gridData.hmFileAdd
//
//            val formData = org.w3c.xhr.FormData().also { formData_ ->
//                for (file in files.asList()) {
//                    val id = -getRandomInt()
//
//                    gridData.alFileData.add(FormFileData(id, "", file.name))
//                    hmFileAdd[id] = file.name
//
//                    formData_.append("form_file_ids", id.toString())
//                    formData_.append("form_file_blobs", file)
//                }
//            }
//
//            invokeUploadFormFile(formData)
//        }
//    }

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
                        gridData.text.value
                    } else {
                        gridData.oldText
                    }
                )
            }

            FormCellTypeClient.TEXT -> {
                hmFormData[gridData.cellName] = FormData(
                    textValue = if (withNewValues) {
                        gridData.text.value
                    } else {
                        gridData.oldText
                    }
                )
            }

            FormCellTypeClient.CHECKBOX, FormCellTypeClient.SWITCH -> {
                hmFormData[gridData.cellName] = FormData(
                    booleanValue = if (withNewValues) {
                        gridData.bool.value
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
                        gridData.combo.value
                    } else {
                        gridData.oldCombo
                    }
                )
            }

            FormCellTypeClient.RADIO -> {
                hmFormData[gridData.cellName] = FormData(
                    comboValue = if (withNewValues) {
                        gridData.combo.value
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
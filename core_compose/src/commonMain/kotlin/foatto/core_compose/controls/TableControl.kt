package foatto.core_compose.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.core.link.AppRequest
import foatto.core.link.TableCellType
import foatto.core.link.TableResponse
import foatto.core.link.TableRowData
import foatto.core_compose.AppControl
import foatto.core_compose.Root
import foatto.core_compose.composables.ImageOrTextFromName
import foatto.core_compose.controls.composables.table.TableToolBar
import foatto.core_compose.controls.models.TitleData
import foatto.core_compose.controls.models.table.*
import foatto.core_compose.getStyleIconNameSuffix
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.utils.maxDp
import kotlin.math.max

//--- for define inherited/custom table/grid controls in inherited application modules

var getTableControl: (
    root: Root,
    appControl: AppControl,
    appParam: String,
    tableResponse: TableResponse,
    tabId: Int,
) -> TableControl = { root: Root,
                      appControl: AppControl,
                      appParam: String,
                      tableResponse: TableResponse,
                      tabId: Int ->
    TableControl(root, appControl, appParam, tableResponse, tabId)
}

//--- base/standart table/grid control

open class TableControl(
    private val root: Root,
    private val appControl: AppControl,
    private val appParam: String,
    private val tableResponse: TableResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    //--- for custom client actions in custom table/grid controls
    protected var tableClientActionFun: (
        action: String,
        alParam: List<Pair<String, String>>,
        tableControl: TableControl
    ) -> Unit = { _: String, _: List<Pair<String, String>>, _: TableControl ->
    }

    private var selectorCancelUrl by mutableStateOf("")
    private var isFindTextVisible by mutableStateOf(root.isWideScreen)
    private var findUrl by mutableStateOf("")
    private var findText by mutableStateOf("")

    private var isFormButtonVisible by mutableStateOf(false)
    private var isGotoButtonVisible by mutableStateOf(false)
    private var isPopupButtonVisible by mutableStateOf(false)

    private val alAddButton = mutableStateListOf<AddActionButtonClient>()
    private val alServerButton = mutableStateListOf<ServerActionButtonClient>()
    private val alClientButton = mutableStateListOf<ClientActionButtonClient>()
    private val alPageButton = mutableStateListOf<PageButton>()

    private var alCaptionData = mutableStateListOf<TableCaptionData>()

    private val alGridRows = mutableStateListOf<MutableList<TableGridData?>>()
    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val alRowData = mutableStateListOf<TableRowData>()

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var alCurPopupData by mutableStateOf<List<MenuDataClient>?>(null)
    private var isShowPopupMenu by mutableStateOf(false)
    private var currentRow by mutableIntStateOf(-1)

    private var pageUpUrl = ""
    private var pageDownUrl = ""

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Body() {
        val density = LocalDensity.current

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getTableAndFormHeader(false) { url: String ->
                call(url, false)
            }
            TableToolBar(
                isWideScreen = root.isWideScreen,
                styleIconNameSuffix = getStyleIconNameSuffix(),
                selectorCancelUrl = selectorCancelUrl,
                isFindTextVisible = isFindTextVisible,
                findText = findText,
                alAddButton = alAddButton,
                isFormButtonVisible = isFormButtonVisible,
                isGotoButtonVisible = isGotoButtonVisible,
                isPopupButtonVisible = isPopupButtonVisible,
                alServerButton = alServerButton,
                alClientButton = alClientButton,
                appParam = appParam,
                onFindInput = { newText: String -> findText = newText },
                doFind = { isClear: Boolean -> doFind(isClear) },
                doForm = { doForm() },
                doGoto = { doGoto() },
                doPopup = { doPopup() },
                clientAction = { action: String, params: List<Pair<String, String>> -> clientAction(action, params) },
                call = { appParam: String, inNewWindow: Boolean -> call(appParam, inNewWindow) },
            )

            Row(
                modifier = Modifier
                    .horizontalScroll(state = horizontalScrollState)
            ) {
                alCaptionData.forEachIndexed { col, captionData ->
                    val originalModifier = Modifier
                    Button(
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        modifier = originalModifier.then(
                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                Modifier.width(maxColWidth)
                            } ?: originalModifier
                        ),
                        onClick = {
                            captionData.url?.let {
                                call(captionData.url, false)
                            }
                        }
                    ) {
                        Text(
                            text = captionData.text,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
//                    title(gridData.tooltip)
//                    gridData.elementStyle(this)
//                    getMultilineText(gridData.textCellData!!.text)
                }
            }

            Column(
                modifier = Modifier
                    .verticalScroll(state = verticalScrollState)
                    .horizontalScroll(state = horizontalScrollState)
                    .weight(1.0f)
            ) {
                for (alGridRow in alGridRows) {
                    Row(
                        modifier = Modifier.height(intrinsicSize = IntrinsicSize.Max)
                    ) {
                        alGridRow.forEachIndexed { col, gridData ->
                            val originalModifier = Modifier
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = originalModifier
                                    .fillMaxHeight()
                                    .border(BorderStroke(width = 1.dp, color = Color.LightGray))
                                    .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                                    .combinedClickable(
                                        onDoubleClick = {
                                            gridData?.let {
                                                if (gridData.dataRow >= 0 && alRowData[gridData.dataRow].rowURL.isNotEmpty()) {
                                                    call(alRowData[gridData.dataRow].rowURL, alRowData[gridData.dataRow].isRowURLInNewWindow)
                                                }
                                            }
                                        },
                                        onClick = {

                                        }
                                    )
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
                                    TableCellType.CHECKBOX -> {
                                        Checkbox(
                                            checked = gridData.booleanValue ?: false,
                                            onCheckedChange = { _ ->
//                                        isShowPopupMenu.value = false
                                            }
//                                    title(gridData.tooltip)
//                                    style {
//                                        gridData.elementStyle(this)
//                                    }
                                        )
                                    }

                                    TableCellType.TEXT -> {
                                        if (gridData.textCellData?.icon?.isNotEmpty() == true) {
                                            ImageOrTextFromName(gridData.textCellData.icon, gridData.tooltip)
//                                                gridData.elementStyle(this)
                                        } else if (gridData.textCellData?.image?.isNotEmpty() == true) {
                                            ImageOrTextFromName(gridData.textCellData.image, gridData.tooltip)
//                                                gridData.elementStyle(this)
                                        } else {
                                            Text(
                                                text = gridData.textCellData?.text ?: "(empty text cell)",
                                            )
//                                            title(gridData.tooltip)
//                                                gridData.elementStyle(this)
//                                        getMultilineText(gridData.textCellData!!.text)
                                        }
                                    }

                                    TableCellType.BUTTON -> {
                                        gridData.alButtonCellData?.let { alButtonCellData ->
                                            for (cellData in alButtonCellData) {
                                                Button(
                                                    onClick = {
                                                        call(cellData.url, cellData.inNewWindow)
                                                    }
                                                ) {
                                                    if (cellData.icon.isNotEmpty()) {
                                                        ImageOrTextFromName(cellData.icon, gridData.tooltip)
    //                                                        cellData.style(this)
                                                    } else if (cellData.image.isNotEmpty()) {
                                                        ImageOrTextFromName(cellData.image, gridData.tooltip)
    //                                                        cellData.style(this)
                                                    } else {
    //                                                    title(gridData.tooltip)
    //                                                    style {
    //                                                        cellData.style(this)
    //                                                    }
                                                        Text(text = cellData.text)
    //                                                getMultilineText(cellData.text)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    TableCellType.GRID -> {
                                        Row {
                                            for (alRow in gridData.alGridCellData!!) {
                                                Column {
                                                    for (cellData in alRow) {
//                                                        title(gridData.tooltip)
//                                                        style {
//                                                            cellData.style(this)
//                                                        }
                                                        if (cellData.icon.isNotEmpty()) {
                                                            ImageOrTextFromName(cellData.icon, gridData.tooltip)
                                                        } else if (cellData.image.isNotEmpty()) {
                                                            ImageOrTextFromName(cellData.image, gridData.tooltip)
                                                        } else {
                                                            Text(text = cellData.text)
//                                                    getMultilineText(cellData.text)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                                //    gridData.cellStyleCommon(this)
                                //    gridData.cellStyleAdd(this)
                                //    backgroundColor(
                                //        if (gridData.dataRow >= 0 && gridData.dataRow == currentRow.value) {
                                //            getColorTableRowHover()
                                //        } else {
                                //            gridData.backColor
                                //        }
                                //    )
                                //
                                //onClick {
                                //    if (gridData.cellURL.isNotEmpty()) {
                                //        call(gridData.cellURL, false)
                                //    } else {
                                //        setCurrentRow(gridData.dataRow)
                                //    }
                                //}
                                //onContextMenu { mouseEvent ->
                                //    if (gridData.dataRow >= 0) {
                                //        showPopupMenu(gridData.dataRow, mouseEvent)
                                //    }
                                //    mouseEvent.preventDefault()
                                //}
                            }
                        }
                    }
                }
            }
            //coroutineScope.launch {
            //    gridState.scrollToItem(...)
            //}

            //--- Page Bar
            Row {
//                            flexGrow(0)
//                            flexShrink(0)
//                            display(DisplayStyle.Flex)
//                            flexDirection(FlexDirection.Row)
//                            flexWrap(FlexWrap.Wrap)
//                            justifyContent(JustifyContent.Center)
//                            alignItems(AlignItems.Center)
//                            borderTop(
//                                width = getStyleTableCaptionBorderTop().width,
//                                lineStyle = getStyleTableCaptionBorderTop().style,
//                                color = getStyleTableCaptionBorderTop().color,
//                            )
//                            setPaddings(arrStyleTablePageBarPadding)
//                            backgroundColor(getColorTablePagebarBack())

                for (pageButton in alPageButton) {
                    Button(
//                                backgroundColor(
//                                    if (pageButton.url.isNotEmpty()) {
//                                        getColorButtonBack()
//                                    } else {
//                                        getColorTablePageBarCurrentBack()
//                                    }
//                                )
//                                if (pageButton.url.isNotEmpty()) {
//                                    setBorder(getStyleTablePageBarOtherBorder())
//                                } else {
//                                    border(width = 0.px)
//                                }
//                                width(getStyleTablePageButtonWidth(tableResponse.alPageButton.size))
//                                fontSize(getStyleTablePageButtonFontSize(tableResponse.alPageButton.size))
//                                padding(styleIconButtonPadding)
//                                setMargins(arrStyleCommonMargin)
//                                cursor("pointer")
                        onClick = {
                            if (pageButton.url.isNotEmpty()) {
                                call(pageButton.url, false)
                            }
                        }
                    ) {
                        Text(pageButton.text)
                    }
                }
            }
        }
    }

    /*
                //--- Popup Menu
                if (isShowPopupMenu.value) {
                    Div(
                        attrs = {
                            style {
                                zIndex(Z_INDEX_TABLE_POPUP)
                                position(Position.Absolute)
                                top(20.percent)
                                bottom(
                                    if (styleIsNarrowScreen) {
                                        20.percent
                                    } else {
                                        10.percent
                                    }
                                )
                                //"min-width" to styleMenuWidth(), - не уверен, что потребуется
                                setMenuWidth()
                                backgroundColor(getColorPopupMenuBack())
                                color(colorMenuTextDefault)
                                setBorder(color = getColorMenuBorder(), radius = styleFormBorderRadius)
                                fontSize(arrStyleMenuFontSize[0])
                                setPaddings(arrStyleMenuStartPadding)
                                overflow("auto")
                                cursor("pointer")
                                popupMenuPosFun()
                            }
                            onMouseLeave {
                                isShowPopupMenu.value = false
                            }
                        }
                    ) {
                        Menu.generateMenuBody(this@TableControl, false, arrCurPopupData.value!!, 0)
                    }
                }

                getAdditionalBody()
            }
        }

        @Composable
        open fun getAdditionalBody() {
        }
     */

    override fun start() {
        readHeader()
        selectorCancelUrl = tableResponse.selectorCancelURL

        findUrl = tableResponse.findURL
        findText = tableResponse.findText

        readAddButtons()
        readServerButtons()
        readClientButtons()
        readPageButtons()
        readTable()

        //--- установка текущей строки
//        setCurrentRow(tableResponse.selectedRow)

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        root.curAppParam = appParam

//        //--- две попытки установить фокус, с минимальными задержками
//        window.setTimeout({
//            if (!focusToCursorField(tabId)) {
//                window.setTimeout({
//                    focusToCursorField(tabId)
//                }, 100)
//            }
//        }, 100)
    }

    private fun readHeader() {
        var tabToolTip = ""

        //--- загрузка заголовка таблицы/формы
        alTitleData.clear()
        for ((url, text) in tableResponse.alHeader) {
            tabToolTip += (
                if (tabToolTip.isEmpty()) {
                    ""
                } else {
                    " | "
                }
                ) + text
            alTitleData.add(TitleData(url, text))
            //--- запомним последнюю кнопку заголовка в табличном режиме как кнопку отмены или возврата на уровень выше
            //butTableCancel = button - not used yet
        }
        root.setTabInfo(tabId, tableResponse.tab, tabToolTip)
    }

    private fun readAddButtons() {
        alAddButton.clear()
        for (aab in tableResponse.alAddActionButton) {
            alAddButton.add(
                AddActionButtonClient(
                    name = aab.icon ?: aab.text ?: "(не заданы иконка и текст)",
                    tooltip = aab.tooltip,
                    action = aab.action
                )
            )
        }
    }

    private fun readServerButtons() {
        alServerButton.clear()
        for (sab in tableResponse.alServerActionButton) {
            alServerButton.add(
                ServerActionButtonClient(
                    name = sab.icon ?: sab.text ?: "(не заданы иконка и текст)",
                    tooltip = sab.tooltip,
                    action = sab.action,
                    inNewWindow = sab.inNewWindow,
                    isForWideScreenOnly = sab.isForWideScreenOnly,
                )
            )
        }
    }

    private fun readClientButtons() {
        alClientButton.clear()
        for (cab in tableResponse.alClientActionButton) {
            alClientButton.add(
                ClientActionButtonClient(
                    name = cab.icon ?: cab.text ?: "(не заданы иконка и текст)",
                    tooltip = cab.tooltip,
                    action = cab.action,
                    params = cab.alParam.toList(),
                    isForWideScreenOnly = cab.isForWideScreenOnly,
                )
            )
        }
    }

    private fun readPageButtons() {
        pageUpUrl = ""
        pageDownUrl = ""
        alPageButton.clear()

        var isEmptyPassed = false
        //--- вывести новую разметку страниц
        for ((url, text) in tableResponse.alPageButton) {

            alPageButton.add(PageButton(url, text))

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
    }

    private fun readTable() {
        readCaption()

        alGridRows.clear()

        var maxColCount = tableResponse.alColumnCaption.size

        for (tc in tableResponse.alTableCell) {
            maxColCount = max(maxColCount, tc.col + tc.colSpan)

//            val backColor = when (tc.backColorType) {
//                TableCellBackColorType.DEFINED -> getColorFromInt(tc.backColor)
//                TableCellBackColorType.GROUP_0 -> colorTableGroupBack0
//                TableCellBackColorType.GROUP_1 -> colorTableGroupBack1
//                else -> if (tc.dataRow % 2 == 0) {
//                    colorTableRowBack0
//                } else {
//                    colorTableRowBack1
//                }
//            }
//            val textColor = when (tc.foreColorType) {
//                TableCellForeColorType.DEFINED -> getColorFromInt(tc.foreColor)
//                else -> COLOR_MAIN_TEXT
//            }
//            val align = when (tc.cellType) {
//                TableCellType.BUTTON -> {
//                    JustifyContent.Center
//                }
//
//                TableCellType.CHECKBOX -> {
//                    JustifyContent.Center
//                }
//                //--- на самом деле нет других вариантов
//                //TableCellType.TEXT -> {
//                else -> {
//                    when (tc.align) {
//                        TableCellAlign.LEFT -> JustifyContent.FlexStart
//                        TableCellAlign.CENTER -> JustifyContent.Center
//                        TableCellAlign.RIGHT -> JustifyContent.FlexEnd
//                        else -> JustifyContent.Center
//                    }
//                }
//            }

//            val cellStyleCommon: StyleScope.() -> Unit = {
//                gridArea("${startRow + tc.row + 1}", "${tc.col + 1}", "${startRow + tc.row + 1 + tc.rowSpan}", "${tc.col + 1 + tc.colSpan}")
//                justifySelf("stretch")
//                alignSelf(AlignSelf.Stretch)
//                fontWeight(
//                    if (tc.fontStyle == 0) {
//                        "normal"
//                    } else {
//                        "bold"
//                    }
//                )
//                padding(styleControlPadding)
//            }

            var textCellData: TableTextCellDataClient? = null
            val alButtonCellData = mutableListOf<TableButtonCellDataClient>()
            val alGridCellData = mutableListOf<MutableList<TableGridCellDataClient>>()

//            var cellStyleAdd: StyleScope.() -> Unit = {}
//            var elementStyle: StyleScope.() -> Unit = {}

            when (tc.cellType) {
                TableCellType.CHECKBOX -> {
//                    cellStyleAdd = {
//                        display(DisplayStyle.Flex)
//                        justifyContent(align)
//                        alignItems(AlignItems.Center)
//                    }
//                    //--- for checkbox only
//                    elementStyle = {
//                        color(textColor)
//                        appearance("none")
//                        width(styleCheckBoxWidth)
//                        height(styleCheckBoxHeight)
//                        setBorder(getStyleCheckBoxBorder())
//                    }
                }

                TableCellType.TEXT -> {
                    textCellData = TableTextCellDataClient(
                        icon = tc.textCellData.icon,
                        image = tc.textCellData.image,
                        text = tc.textCellData.text,
                    )
//                    cellStyleAdd = {
//                        display(DisplayStyle.Flex)
//                        justifyContent(align)
//                        alignItems(AlignItems.Center)
//                    }
//                    elementStyle = {
//                        color(textColor)
//                        fontSize(getStyleTableTextFontSize())
//                        if (!tc.isWordWrap) {
//                            whiteSpace("nowrap")
//                        }
//                        userSelect(if (getStyleIsTouchScreen()) "none" else "auto")
//                    }
                }

                TableCellType.BUTTON -> {
                    tc.alButtonCellData.forEachIndexed { index, cellData ->
                        alButtonCellData.add(
                            TableButtonCellDataClient(
                                icon = cellData.icon,
                                image = cellData.image,
                                text = cellData.text,
                                url = cellData.url,
                                inNewWindow = cellData.inNewWindow,
//                                style = {
//                                    setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                    backgroundColor(getColorButtonBack())
//                                    color(textColor)
//                                    fontSize(styleCommonButtonFontSize)
//                                    padding(styleTextButtonPadding)
//                                    cursor("pointer")
//                                    gridArea("${index + 1}", "1", "${index + 2}", "2")
//                                    justifySelf("center")
//                                    alignSelf(AlignSelf.Center)
//                                }
                            )
                        )
                    }
//                    if (tc.alButtonCellData.isNotEmpty()) {
//                        cellStyleAdd = {
//                            display(DisplayStyle.Grid)
//                            gridTemplateRows("repeat(${tc.alButtonCellData.size},auto)")
//                            gridTemplateColumns("repeat(1,auto)")
//                        }
//                    }
                }

                TableCellType.GRID -> {
                    tc.alGridCellData.forEachIndexed { rowIndex, cellRow ->
                        alGridCellData.add(mutableListOf())

                        cellRow.forEachIndexed { colIndex, cellData ->
                            alGridCellData.last().add(
                                TableGridCellDataClient(
                                    icon = cellData.icon,
                                    image = cellData.image,
                                    text = cellData.text,
//                                    style = {
//                                        color(textColor)
//                                        fontSize(getStyleTableTextFontSize())
//                                        userSelect(if (getStyleIsTouchScreen()) "none" else "auto")
//                                        if (!tc.isWordWrap) {
//                                            whiteSpace("nowrap")
//                                        }
//                                        gridArea("${rowIndex + 1}", "${colIndex + 1}", "${rowIndex + 2}", "${colIndex + 2}")
//                                        justifySelf("stretch")
//                                        alignSelf(AlignSelf.Stretch)
//                                        display(DisplayStyle.Flex)
//                                        justifyContent(align)
//                                        alignItems(AlignItems.Center)
//                                        setPaddings(arrStyleTableGridCellTypePadding)
//                                    }
                                )
                            )
                        }
                    }
//                    if (tc.alGridCellData.isNotEmpty()) {
//                        cellStyleAdd = {
//                            display(DisplayStyle.Grid)
//                            gridTemplateRows("repeat(${tc.alGridCellData.size},auto)")
//                            gridTemplateColumns("repeat(${tc.alGridCellData.first().size},auto)")
//                        }
//                    }
                }
            }
            val tableCell = TableGridData(
                cellType = tc.cellType,
                //                    cellStyleCommon = cellStyleCommon,
                //                    elementStyle = elementStyle,
                //                    //rowSpan = tc.rowSpan, - not used?
                //                    backColor = backColor,
                tooltip = tc.tooltip,
                //                    cellStyleAdd = cellStyleAdd,
                booleanValue = tc.booleanValue,
                textCellData = textCellData,
                alButtonCellData = alButtonCellData,
                alGridCellData = alGridCellData,
                dataRow = tc.dataRow,
            )
            addCellToGrid(
                alGridRows = alGridRows,
                maxColCount = maxColCount,
                row = tc.row,
                col = tc.col,
                cell = tableCell,
            )
        }

        alRowData.clear()
        alRowData.addAll(tableResponse.alTableRowData)
    }

    private fun readCaption() {
        alCaptionData.clear()
        //--- заголовки столбцов таблицы
        for ((url, text) in tableResponse.alColumnCaption) {
            val captionCell = TableCaptionData(
//                    justifySelf("stretch")
//                    alignSelf(AlignSelf.Stretch)
//                    //--- use toolbar bottom border instead
//                    borderTop(width = 0.px, lineStyle = LineStyle.None, color = COLOR_MAIN_BACK_0)
//                    borderRight(
//                        width = getStyleTableCaptionBorderRight().width,
//                        lineStyle = getStyleTableCaptionBorderRight().style,
//                        color = getStyleTableCaptionBorderRight().color,
//                    )
//                    borderBottom(
//                        width = getStyleTableCaptionBorderBottom().width,
//                        lineStyle = getStyleTableCaptionBorderBottom().style,
//                        color = getStyleTableCaptionBorderBottom().color,
//                    )
//                    borderLeft(
//                        width = getStyleTableCaptionBorderLeft().width,
//                        lineStyle = getStyleTableCaptionBorderLeft().style,
//                        color = getStyleTableCaptionBorderLeft().color,
//                    )
//                    cursor(
//                        if (url.isBlank()) {
//                            "default"
//                        } else {
//                            "pointer"
//                        }
//                    )
//                    display(DisplayStyle.Flex)
//                    justifyContent(getStyleTableCaptionJustifyH())
//                    alignItems(getStyleTableCaptionAlignH())
//                    fontSize(getStyleTableCaptionFontSize())
//                    fontWeight(getStyleTableCaptionFontWeight())
//                    setPaddings(getStyleTableCaptionPadding())
//                    //--- sticky header
//                    position(Position.Sticky)
//                    top(0.px)
//                    //--- workaround for bug with CheckBoxes in table, which above, than typical cell, include "sticky" table headers
//                    zIndex(Z_INDEX_TABLE_CAPTION)
//                backColor = getColorTableCaptionBack(),
                tooltip = if (url.isBlank()) {
                    ""
                } else {
                    "Сортировать по этому столбцу"
                },
                text = text,
                url = url,
            )
            alCaptionData.add(captionCell)
        }
    }

    private fun closeTabById() {
        root.closeTabById(tabId)
    }

    private fun doFind(isClear: Boolean) {
        if (isClear) {
            findText = ""
        }

        if (!isClear && !isFindTextVisible) {
            isFindTextVisible = true
        } else {
            appControl.call(AppRequest(action = findUrl, find = findText.trim()))
        }
    }

    private fun doForm() {
        if (currentRow >= 0 && alRowData[currentRow].formURL.isNotEmpty()) {
            call(alRowData[currentRow].formURL, false)
        }
    }

    private fun doGoto() {
        if (currentRow >= 0 && alRowData[currentRow].gotoURL.isNotEmpty()) {
            call(alRowData[currentRow].gotoURL, alRowData[currentRow].isGotoURLInNewWindow)
        }
    }

    private fun doPopup() {
        if (currentRow >= 0 && alRowData[currentRow].alPopupData.isNotEmpty()) {
//            showPopupMenu(currentRow, null)
        }
    }

//    private fun setCurrentRow(rowNo: Int) {
//        isFormButtonVisible = rowNo >= 0 && alRowData[rowNo].formURL.isNotEmpty()
//        isGotoButtonVisible = rowNo >= 0 && alRowData[rowNo].gotoURL.isNotEmpty()
//        isPopupButtonVisible = rowNo >= 0 && alRowData[rowNo].alPopupData.isNotEmpty()
//
//        currentRow = rowNo
//        isShowPopupMenu = false
//
////        focusToCursorField(tabId)
//    }

    private fun doKeyEnter() {
        if (currentRow >= 0 && currentRow < alRowData.size) {
            val curRowData = alRowData[currentRow]
            if (curRowData.rowURL.isNotEmpty()) {
                call(curRowData.rowURL, curRowData.isRowURLInNewWindow)
            }
        }
    }

    private fun doKeyEsc() {
        if (selectorCancelUrl.isNotEmpty()) {
            call(selectorCancelUrl, false)
        }
    }

    private fun doKeyUp() {
        if (currentRow > 0) {
//            setCurrentRow(currentRow - 1)
        }
    }

    private fun doKeyDown() {
        if (currentRow < alRowData.lastIndex) {
//            setCurrentRow(currentRow + 1)
        }
    }

    private fun doKeyHome() {
        if (alRowData.isNotEmpty()) {
//            setCurrentRow(0)
        }
    }

    private fun doKeyEnd() {
        if (alRowData.isNotEmpty()) {
//            setCurrentRow(alRowData.lastIndex)
        }
    }

    private fun doKeyPageUp() {
        if (pageUpUrl.isNotEmpty()) {
            call(pageUpUrl, false)
        }
    }

    private fun doKeyPageDown() {
        if (pageDownUrl.isNotEmpty()) {
            call(pageDownUrl, false)
        }
    }

    protected fun call(newAppParam: String, inNewWindow: Boolean) {
        if (inNewWindow) {
            root.openTab(newAppParam)
        } else {
            appControl.call(AppRequest(action = newAppParam))
        }
    }

    private fun clientAction(action: String, params: List<Pair<String, String>>) {
        tableClientActionFun(action, params, this)
    }

//    private fun showPopupMenu(row: Int, mouseEvent: SyntheticMouseEvent?) {
//        //--- чтобы строчка выделялась и по правой кнопке мыши тоже
//        setCurrentRow(row)
//
//        val mouseX = mouseEvent?.pageX ?: (window.innerWidth.toDouble() / 3)
//
//        if (alRowData[row].alPopupData.isNotEmpty()) {
//            //--- в данной ситуации clientX/Y == pageX/Y, offsetX/Y идёт от текущего элемента (ячейки таблицы), screenX/Y - от начала экрана
//            popupMenuPosFun = if (styleIsNarrowScreen) {
//                {
//                    left(5.percent)
//                }
//            } else if (mouseX <= window.innerWidth / 2) {
//                {
//                    left(mouseX.px)
//                }
//            } else {
//                {
//                    right((window.innerWidth - mouseX).px)
//                }
//            }
//            convertPopupMenuDataClient(alRowData[row].alPopupData)
//            isShowPopupMenu.value = true
//        } else {
//            isShowPopupMenu.value = false
//        }
//    }

//    override fun menuClick(url: String, inNewWindow: Boolean) {
//        isShowPopupMenu.value = false
//        call(url, inNewWindow)
//    }
//
//    private fun convertPopupMenuDataClient(alMenuDataClient: List<TablePopupData>) {
//        val alCurPopupData = mutableListOf<MenuDataClient>()
//
//        var i = 0
//        while (i < alMenuDataClient.size) {
//            val MenuDataClient = alMenuDataClient[i]
//
//            if (MenuDataClient.group.isEmpty()) {
//                alCurPopupData.add(
//                    MenuDataClient(
//                        url = MenuDataClient.url,
//                        text = MenuDataClient.text,
//                        arrSubMenu = null,
//                        inNewWindow = MenuDataClient.inNewWindow
//                    )
//                )
//                i++
//            } else {
//                val groupName = MenuDataClient.group
//
//                val alPopupSubMenuDataClient = mutableListOf<MenuDataClient>()
//                while (i < alMenuDataClient.size) {
//                    val subMenuDataClient = alMenuDataClient[i]
//                    if (subMenuDataClient.group.isEmpty() || subMenuDataClient.group != groupName) {
//                        break
//                    }
//
//                    alPopupSubMenuDataClient.add(
//                        MenuDataClient(
//                            url = subMenuDataClient.url,
//                            text = subMenuDataClient.text,
//                            arrSubMenu = null,
//                            inNewWindow = subMenuDataClient.inNewWindow
//                        )
//                    )
//                    i++
//                }
//                alCurPopupData.add(
//                    MenuDataClient(
//                        url = "",
//                        text = groupName,
//                        arrSubMenu = alPopupSubMenuDataClient.toTypedArray(),
//                        inNewWindow = false
//                    )
//                )
//            }
//        }
//
//        arrCurPopupData.value = alCurPopupData.toTypedArray()
//    }

//    private fun focusToCursorField(tabId: Int): Boolean {
//        document.getElementById("table_cursor_$tabId")?.let { element ->
//            if (element is HTMLElement) {
//                element.focus()
//                return true
//            }
//        }
//        return false
//    }

}
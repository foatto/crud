package foatto.compose.control

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.colorMainBack0
import foatto.compose.colorMainText
import foatto.compose.composable.GenerateMenuBody
import foatto.compose.composable.ImageOrTextFromName
import foatto.compose.control.model.TitleData
import foatto.compose.control.model.table.AddActionButtonClient
import foatto.compose.control.model.table.ClientActionButtonClient
import foatto.compose.control.model.table.PageButton
import foatto.compose.control.model.table.ServerActionButtonClient
import foatto.compose.control.model.table.TableCaptionData
import foatto.compose.control.model.table.cell.TableBaseCellClient
import foatto.compose.control.model.table.cell.TableBooleanCellClient
import foatto.compose.control.model.table.cell.TableButtonCellClient
import foatto.compose.control.model.table.cell.TableCellDataClient
import foatto.compose.control.model.table.cell.TableGridCellClient
import foatto.compose.control.model.table.cell.TableSimpleCellClient
import foatto.compose.controls.composables.table.TableImageOrTextCell
import foatto.compose.controls.composables.table.TableToolBar
import foatto.compose.model.MenuDataClient
import foatto.compose.styleOtherIconSize
import foatto.compose.utils.maxDp
import foatto.core.model.AppAction
import foatto.core.model.request.system.AppRequest
import foatto.core.model.response.system.TableResponse
import foatto.core.model.response.system.table.TablePopupData
import foatto.core.model.response.system.table.TableRowData
import foatto.core.model.response.system.table.cell.TableBooleanCell
import foatto.core.model.response.system.table.cell.TableButtonCell
import foatto.core.model.response.system.table.cell.TableCellAlign
import foatto.core.model.response.system.table.cell.TableCellBackColorType
import foatto.core.model.response.system.table.cell.TableGridCell
import foatto.core.model.response.system.table.cell.TableSimpleCell
import kotlinx.coroutines.launch
import kotlin.math.max

var colorTableCellBorder: Color = Color.hsl(0f, 0f, 0.9f)
var colorTableRowHover: Color = Color.hsl(133f, 0.54f, 0.93f)
var colorTableGroupBack0: Color = Color.hsl(0f, 0f, 0.8f)
var colorTableGroupBack1: Color = Color.hsl(0f, 0f, 0.7f)
var colorTableRowBack0: Color = Color.hsl(0f, 0f, 1.0f)
var colorTableRowBack1: Color = Color.hsl(0f, 0f, 0.95f)

//--- base/standart table/grid control

class TableControl(
    private val root: Root,
    private val appControl: AppControl,
    private val tableAction: AppAction,
    private val tableResponse: TableResponse,
    tabId: Int,
    private val selectorFun: ((selectorData: Map<String, String>) -> Unit)? = null,
) : AbstractControl(tabId) {

    companion object {
        private val TABLE_CELL_PADDING = 16.dp
    }

    //--- for custom client actions in custom table/grid controls
    protected var tableClientActionFun: (
        action: AppAction,
        alParam: List<Pair<String, String>>,
        tableControl: TableControl
    ) -> Unit = { _: AppAction, _: List<Pair<String, String>>, _: TableControl ->
    }

    private var selectorCancelAction by mutableStateOf<AppAction?>(null)

    private var isFindTextVisible by mutableStateOf(root.isWideScreen)
    private var findText by mutableStateOf("")

    private var isFormButtonVisible by mutableStateOf(false)
    private var isGotoButtonVisible by mutableStateOf(false)
    private var isPopupButtonVisible by mutableStateOf(false)

    private val alAddButton = mutableStateListOf<AddActionButtonClient>()
    private val alServerButton = mutableStateListOf<ServerActionButtonClient>()
    private val alClientButton = mutableStateListOf<ClientActionButtonClient>()
    private val alPageButton = mutableStateListOf<PageButton>()

    private var alCaptionData = mutableStateListOf<TableCaptionData>()

    private val alGridRows = mutableStateListOf<MutableList<TableBaseCellClient?>>()
    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    private val alRowData = mutableStateListOf<TableRowData>()

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var currentGridData by mutableStateOf<TableBaseCellClient?>(null)
    private var currentRowNo by mutableStateOf<Int?>(null)

    private var pageUpAction: AppAction? = null
    private var pageDownAction: AppAction? = null

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    selectorFun?.let {
                        Modifier.background(colorMainBack0)
                    } ?: Modifier
                ),
        ) {
            getTableAndFormHeader(false) { action: AppAction ->
                call(action, false)
            }
            TableToolBar(
                isSelectorMode = tableAction.isSelectorMode,
                isWideScreen = root.isWideScreen,
                selectorCancelAction = selectorCancelAction,
                isFindTextVisible = isFindTextVisible,
                findText = findText,
                alAddButton = alAddButton,
                isFormButtonVisible = isFormButtonVisible,
                isGotoButtonVisible = isGotoButtonVisible,
                isPopupButtonVisible = isPopupButtonVisible,
                alServerButton = alServerButton,
                alClientButton = alClientButton,
                tableAction = tableAction,
                onFindInput = { newText: String -> findText = newText },
                doFind = { isClear: Boolean -> doFind(isClear) },
                doForm = { doForm() },
                doGoto = { doGoto() },
                doPopup = { doPopup() },
                clientAction = { action: AppAction, params: List<Pair<String, String>> -> clientAction(action, params) },
                call = { action: AppAction, inNewTab: Boolean -> call(action, inNewTab) },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(state = horizontalScrollState)
            ) {
                alCaptionData.forEachIndexed { col, captionData ->
                    Button(
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.then(
                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                Modifier.width(maxColWidth)
                            } ?: Modifier
                        ),
                        onClick = {
                            captionData.action?.let {
                                call(captionData.action, false)
                            }
                        }
                    ) {
                        Text(
                            text = captionData.text,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
                    .weight(1.0f)
            ) {
                for (alGridRow in alGridRows) {
                    Row(
                        modifier = Modifier
                            //.width(intrinsicSize = IntrinsicSize.Max) - оставлю на память, чтобы опять не искать, но пользы/разницы не увидел
                            //--- чтобы ячейки занимали всю отведённую им (общую) высоту (работает только совместно с .fillMaxHeight())
                            .height(intrinsicSize = IntrinsicSize.Max)
                    ) {
                        alGridRow.forEachIndexed { col, gridData ->
                            Box(
                                contentAlignment = gridData?.align ?: Alignment.Center,
                                modifier = Modifier
                                    .fillMaxHeight()    // работает только совместно с .height(intrinsicSize = IntrinsicSize.Max)
                                    .background(
                                        gridData?.let {
                                            if (gridData.dataRow == currentRowNo) {
                                                colorTableRowHover
                                            } else {
                                                gridData.backColor
                                            }
                                        } ?: colorMainBack0
                                    )
                                    .border(BorderStroke(width = 0.1.dp, color = colorTableCellBorder))
                                    //.wrapContentSize(align = Alignment.Center, unbounded = true)  //- ничем не помогает
//!!! ждём, когда завезут onPointerEvent (кнопки мыши) на мультиплатформу
//                                    .onPointerEvent(
//                                      eventType = PointerEventType.Press,
//                                      pass = PointerEventPass.Initial,
//                                      onEvent = { event ->
//                                        t = event.button?.index?.toString() ?: "null"
//                                      }
//                                    )
//                                    .pointerInput(Unit) {
//                                        awaitPointerEventScope {
//                                            println("Mouse Right 1 = ${currentEvent.buttons.isSecondaryPressed}")
//                                        }
//                                    }
                                    .combinedClickable(
                                        onDoubleClick = {
                                            gridData?.dataRow?.let { dataRow ->
                                                val rowData = alRowData[dataRow]
                                                rowData.rowAction?.let { rowAction ->
                                                    if (tableAction.isSelectorMode) {
                                                        selectorFun?.invoke(rowAction.selectorData)
                                                    } else {
                                                        call(rowAction, rowData.isRowUrlInNewTab)
                                                    }
                                                }
                                            }
                                        },
                                        onClick = {
                                            currentGridData = gridData
                                            setCurrentRow(gridData?.dataRow)
                                        },
                                        onLongClick = {
                                            showPopupMenu(gridData)
                                        },
                                    )
                                    .onSizeChanged { size ->
                                        gridData?.let {
                                            val componentWidth = with(density) {
                                                maxDp(size.width.toDp(), TABLE_CELL_PADDING * 2)    // minWidth
                                            }
                                            gridData.componentWidth = maxDp(gridData.componentWidth, componentWidth)

                                            hmColumnMaxWidth[col] = maxDp(
                                                hmColumnMaxWidth[col] ?: 0.dp,
                                                if (gridData.colSpan == 1) {
                                                    componentWidth
                                                } else {
                                                    8.dp    // minWidth for explicitly empty column (from group-column)
                                                }
                                            )
                                        }
                                    }
                                    .then(
                                        gridData?.let {
                                            var maxColWidth: Dp? = null
                                            for (c in col until col + gridData.colSpan) {
                                                hmColumnMaxWidth[c]?.let { cmw ->
                                                    maxColWidth = (maxColWidth ?: 0.dp) + cmw
                                                }
                                            }
                                            maxColWidth?.let { mcw ->
                                                if (gridData.componentWidth < mcw) {
                                                    Modifier.width(mcw)
                                                } else {
                                                    Modifier.width(gridData.componentWidth)
                                                }
                                            }
                                        } ?: Modifier
                                    )
                            ) {
                                when (gridData) {
                                    is TableBooleanCellClient -> {
                                        Checkbox(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                            checked = gridData.value,
                                            onCheckedChange = null,
                                        )
                                    }

                                    is TableSimpleCellClient -> {
                                        TableImageOrTextCell(
                                            baseCellData = gridData,
                                            cellData = gridData.data,
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        )
                                    }

                                    is TableButtonCellClient -> {
                                        Column(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        ) {
                                            gridData.data.forEach { cellData ->
                                                cellData.icon?.let {
                                                    IconButton(
                                                        onClick = {
                                                            cellData.action?.let {
                                                                call(cellData.action, cellData.inNewTab)
                                                            }
                                                        }
                                                    ) {
                                                        ImageOrTextFromName(cellData.icon, styleOtherIconSize)
                                                    }
                                                } ?: cellData.image?.let {
                                                    IconButton(
                                                        onClick = {
                                                            cellData.action?.let {
                                                                call(cellData.action, cellData.inNewTab)
                                                            }
                                                        }
                                                    ) {
                                                        ImageOrTextFromName(cellData.image, styleOtherIconSize)
                                                    }
                                                } ?: run {
                                                    Button(
                                                        shape = RoundedCornerShape(0.dp),
                                                        onClick = {
                                                            cellData.action?.let {
                                                                call(cellData.action, cellData.inNewTab)
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = cellData.text ?: "???",
//                                                                color = gridData.textColor,
                                                            fontWeight = if (gridData.isBoldText) {
                                                                FontWeight.Bold
                                                            } else {
                                                                null
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is TableGridCellClient -> {
                                        Column(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        ) {
                                            gridData.data.forEach { alRow ->
                                                Row(
                                                    //horizontalArrangement = Arrangement.SpaceBetween, - don't work properly (?), use Spacer
                                                ) {
                                                    alRow.forEachIndexed { index, cellData ->
                                                        if (index > 0) {
                                                            Spacer(modifier = Modifier.weight(1.0f))
                                                        }
                                                        val modifier = if (index > 0) {
                                                            Modifier.padding(start = TABLE_CELL_PADDING)
                                                        } else {
                                                            Modifier
                                                        }
                                                        TableImageOrTextCell(
                                                            baseCellData = gridData,
                                                            cellData = cellData,
                                                            modifier = modifier,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                                gridData?.let {
                                    if (!tableAction.isSelectorMode) {
                                        DropdownMenu(
                                            expanded = gridData.isShowPopupMenu,
                                            onDismissRequest = {
                                                gridData.isShowPopupMenu = false
                                            },
                                        ) {
                                            GenerateMenuBody(
                                                alMenuDataClient = gridData.alCurrentPopupData!!,
                                                level = 0,
                                                closeMenu = {
                                                    gridData.isShowPopupMenu = false
                                                },
                                                menuClick = { action: AppAction, inNewTab: Boolean ->
                                                    gridData.isShowPopupMenu = false
                                                    call(action, inNewTab)
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
//!!!
                //coroutineScope.launch {
                //    gridState.scrollToItem(...)
                //}
            }
            //--- Page Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
//                            borderTop(
//                                width = getStyleTableCaptionBorderTop().width,
//                                lineStyle = getStyleTableCaptionBorderTop().style,
//                                color = getStyleTableCaptionBorderTop().color,
//                            )
//                            backgroundColor(getColorTablePagebarBack())

                for (pageButton in alPageButton) {
                    Button(
                        modifier = Modifier.padding(1.dp),
                        shape = RoundedCornerShape(0.dp),
                        enabled = pageButton.action != null,
//                                cursor("pointer")
                        onClick = {
                            pageButton.action?.let { action ->
                                call(action, false)
                            }
                        }
                    ) {
                        Text(pageButton.text)
                    }
                }
            }
        }
    }

    override fun start() {
        readHeader()
        selectorCancelAction = tableResponse.selectorCancelAction

        findText = tableResponse.findText

        readAddButtons()
        readServerButtons()
        readClientButtons()
        readPageButtons()
        readTable()

        //--- установка текущей строки
        setCurrentRow(tableResponse.selectedRowNo)

        //--- запоминаем текущий appParam для возможной установки в виде стартовой
        root.curAction = tableAction

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
        for ((action, text) in tableResponse.alHeader) {
            tabToolTip += (
                if (tabToolTip.isEmpty()) {
                    ""
                } else {
                    " | "
                }
                ) + text
            alTitleData.add(TitleData(action, text))
            //--- запомним последнюю кнопку заголовка в табличном режиме как кнопку отмены или возврата на уровень выше
            //butTableCancel = button - not used yet
        }
        root.setTabInfo(tabId, tableResponse.tabCaption, tabToolTip)
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
            alServerButton.add(ServerActionButtonClient.readFromServerActionButton(sab))
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
        pageUpAction = null
        pageDownAction = null
        alPageButton.clear()

        var isEmptyPassed = false
        //--- вывести новую разметку страниц
        for ((action, text) in tableResponse.alPageButton) {

            alPageButton.add(PageButton(action, text))

            action?.let {
                if (!isEmptyPassed) {
                    pageUpAction = action
                }
                if (isEmptyPassed && pageDownAction == null) {
                    pageDownAction = action
                }
            } ?: run {
                isEmptyPassed = true
            }
        }
    }

    private fun readTable() {
        readCaption()

        alGridRows.clear()

        var maxColCount = tableResponse.alColumnCaption.size

        for (tc in tableResponse.alTableCell) {
            maxColCount = max(maxColCount, tc.col + tc.colSpan)

            val backColor = tc.backColor?.let { bc ->
                Color(bc)
            } ?: when (tc.backColorType) {
                TableCellBackColorType.GROUP_0 -> colorTableGroupBack0
                TableCellBackColorType.GROUP_1 -> colorTableGroupBack1
                else -> if (tc.dataRow % 2 == 0) {
                    colorTableRowBack0
                } else {
                    colorTableRowBack1
                }
            }
            val textColor = tc.foreColor?.let { fc ->
                Color(fc)
            } ?: colorMainText

            val align = when (tc.align) {
                TableCellAlign.LEFT -> Alignment.CenterStart
                TableCellAlign.CENTER -> Alignment.Center
                TableCellAlign.RIGHT -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            val tableCellClient = when (tc) {
                is TableBooleanCell -> {
                    TableBooleanCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.fontStyle != 0,
                        value = tc.value,
                    )
                }

                is TableSimpleCell -> {
                    TableSimpleCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.fontStyle != 0,
                        //tc.isWordWrap
                        data = TableCellDataClient(
                            icon = tc.iconName,
                            image = tc.imageUrl,
                            text = tc.text,
                        )
                    )
                }

                is TableButtonCell -> {
                    TableButtonCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.fontStyle != 0,
                        data = tc.values.map { tableButtonCellData ->
                            TableCellDataClient(
                                icon = tableButtonCellData.iconName,
                                image = tableButtonCellData.imageUrl,
                                text = tableButtonCellData.text,
                                action = tableButtonCellData.action,
                                inNewTab = tableButtonCellData.inNewTab,
                            )
                        }
                    )
                }

                is TableGridCell -> {
                    TableGridCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.fontStyle != 0,
                        data = tc.values.map { alTableGridCellData ->
                            alTableGridCellData.map { tableGridCellData ->
                                TableCellDataClient(
                                    icon = tableGridCellData.iconName,
                                    image = tableGridCellData.imageUrl,
                                    text = tableGridCellData.text,
                                )
                            }
                        }
                    )
                }

            }

            addCellToGrid(
                alGridRows = alGridRows,
                maxColCount = maxColCount,
                row = tc.row,
                col = tc.col,
                cell = tableCellClient,
            )
        }

        alRowData.clear()
        alRowData.addAll(tableResponse.alTableRowData)
    }

    private fun readCaption() {
        alCaptionData.clear()
        //--- заголовки столбцов таблицы
        for ((action, text) in tableResponse.alColumnCaption) {
            val captionCell = TableCaptionData(
//                    cursor(
//                        if (url.isBlank()) {
//                            "default"
//                        } else {
//                            "pointer"
//                        }
//                    )
                tooltip = action?.let { "Сортировать по этому столбцу" } ?: "",
                text = text,
                action = action,
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
            appControl.call(AppRequest(action = tableAction.copy(findText = findText.trim())))
        }
    }

    private fun doForm() {
        currentRowNo?.let { curRow ->
            alRowData[curRow].formAction?.let { formAction ->
                call(formAction, false)
            }
        }
    }

    private fun doGoto() {
        currentRowNo?.let { curRow ->
            alRowData[curRow].gotoAction?.let { gotoAction ->
                call(gotoAction, alRowData[curRow].isGotoUrlInNewTab)
            }
        }
    }

    private fun doPopup() {
        showPopupMenu(currentGridData)
    }

    private fun setCurrentRow(rowNo: Int?) {
        isFormButtonVisible = rowNo != null && alRowData[rowNo].formAction != null
        isGotoButtonVisible = rowNo != null && alRowData[rowNo].gotoAction != null
        isPopupButtonVisible = rowNo != null && alRowData[rowNo].alPopupData.isNotEmpty()

        currentRowNo = rowNo

//        focusToCursorField(tabId)
    }

    private fun doKeyEnter() {
        currentRowNo?.let { curRow ->
            if (curRow >= 0 && curRow < alRowData.size) {
                val curRowData = alRowData[curRow]
                curRowData.rowAction?.let { rowAction ->
                    call(rowAction, curRowData.isRowUrlInNewTab)
                }
            }
        }
    }

    private fun doKeyEsc() {
        selectorCancelAction?.let { cancelAction ->
            call(cancelAction, false)
        }
    }

    private fun doKeyUp() {
        currentRowNo?.let { curRow ->
            if (curRow > 0) {
                setCurrentRow(curRow - 1)
            }
        }
    }

    private fun doKeyDown() {
        currentRowNo?.let { curRow ->
            if (curRow < alRowData.lastIndex) {
                setCurrentRow(curRow + 1)
            }
        }
    }

    private fun doKeyHome() {
        if (alRowData.isNotEmpty()) {
            setCurrentRow(0)
        }
    }

    private fun doKeyEnd() {
        if (alRowData.isNotEmpty()) {
            setCurrentRow(alRowData.lastIndex)
        }
    }

    private fun doKeyPageUp() {
        pageUpAction?.let { action ->
            call(action, false)
        }
    }

    private fun doKeyPageDown() {
        pageDownAction?.let { action ->
            call(action, false)
        }
    }

    private fun call(newAppAction: AppAction, inNewTab: Boolean) {
        if (inNewTab) {
            root.openTab(newAppAction)
        } else {
            appControl.call(AppRequest(action = newAppAction.copy(prevAction = tableAction)))
        }
    }

    private fun clientAction(action: AppAction, params: List<Pair<String, String>>) {
        tableClientActionFun(action, params, this)
    }

    private fun showPopupMenu(gridData: TableBaseCellClient?) {
        gridData?.let {
            //--- чтобы строчка выделялась и по правой кнопке мыши тоже
            setCurrentRow(gridData.dataRow)
            gridData.dataRow?.let { dataRow ->
                if (alRowData[dataRow].alPopupData.isNotEmpty()) {
                    convertPopupMenuDataClient(gridData, alRowData[dataRow].alPopupData)
                    gridData.isShowPopupMenu = true
                } else {
                    gridData.isShowPopupMenu = false
                }
            } ?: run {
                gridData.isShowPopupMenu = false
            }
        }
    }

    private fun convertPopupMenuDataClient(gridData: TableBaseCellClient, alMenuDataClient: List<TablePopupData>) {
        val alCurPopupData = mutableListOf<MenuDataClient>()

        var i = 0
        while (i < alMenuDataClient.size) {
            val menuDataClient = alMenuDataClient[i]

            menuDataClient.group?.let { groupName ->
                val alPopupSubMenuDataClient = mutableListOf<MenuDataClient>()
                while (i < alMenuDataClient.size) {
                    val subMenuDataClient = alMenuDataClient[i]
                    if (subMenuDataClient.group == null || subMenuDataClient.group != groupName) {
                        break
                    }

                    alPopupSubMenuDataClient.add(
                        MenuDataClient(
                            action = subMenuDataClient.action,
                            caption = subMenuDataClient.text,
                            alSubMenu = null,
                            inNewTab = subMenuDataClient.inNewTab
                        )
                    )
                    i++
                }
                alCurPopupData.add(
                    MenuDataClient(
                        action = null,
                        caption = groupName,
                        alSubMenu = alPopupSubMenuDataClient,
                        inNewTab = false
                    )
                )
            } ?: run {
                alCurPopupData.add(
                    MenuDataClient(
                        action = menuDataClient.action,
                        caption = menuDataClient.text,
                        alSubMenu = null,
                        inNewTab = menuDataClient.inNewTab,
                    )
                )
                i++
            }
        }

        gridData.alCurrentPopupData = alCurPopupData
    }

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
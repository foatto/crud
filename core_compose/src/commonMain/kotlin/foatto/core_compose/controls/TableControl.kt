package foatto.core_compose.controls

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
import foatto.core.link.*
import foatto.core_compose.AppControl
import foatto.core_compose.Root
import foatto.core_compose.composables.GenerateMenuBody
import foatto.core_compose.composables.ImageOrTextFromName
import foatto.core_compose.controls.composables.table.TableImageOrTextCell
import foatto.core_compose.controls.composables.table.TableToolBar
import foatto.core_compose.controls.models.TitleData
import foatto.core_compose.controls.models.table.AddActionButtonClient
import foatto.core_compose.controls.models.table.ClientActionButtonClient
import foatto.core_compose.controls.models.table.PageButton
import foatto.core_compose.controls.models.table.ServerActionButtonClient
import foatto.core_compose.controls.models.table.TableCaptionData
import foatto.core_compose.controls.models.table.TableCellDataClient
import foatto.core_compose.controls.models.table.TableGridData
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.styleOtherIconSize
import foatto.core_compose.utils.maxDp
import kotlinx.coroutines.launch
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
    private val appParam: String,
    private val tableResponse: TableResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val TABLE_CELL_PADDING = 16.dp
    }

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

    private var currentGridData by mutableStateOf<TableGridData?>(null)
    private var currentRowNo by mutableIntStateOf(-1)

    private var pageUpUrl = ""
    private var pageDownUrl = ""

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getTableAndFormHeader(false) { url: String ->
                call(url, false)
            }
            TableToolBar(
                isWideScreen = root.isWideScreen,
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
                                            if (gridData.dataRow >= 0 && gridData.dataRow == currentRowNo) {
                                                colorTableRowHover
                                            } else {
                                                gridData.backColor
                                            }
                                        } ?: Color.White
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
                                            gridData?.let {
                                                if (gridData.dataRow >= 0 && alRowData[gridData.dataRow].rowURL.isNotEmpty()) {
                                                    call(alRowData[gridData.dataRow].rowURL, alRowData[gridData.dataRow].isRowURLInNewWindow)
                                                }
                                            }
                                        },
                                        onClick = {
                                            gridData?.let {
                                                currentGridData = gridData
                                                setCurrentRow(gridData.dataRow)
                                            }
                                        },
                                        onLongClick = {
                                            gridData?.let {
                                                if (gridData.dataRow >= 0) {
                                                    showPopupMenu(gridData)
                                                }
                                            }
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
                                when (gridData?.cellType) {
                                    TableCellType.CHECKBOX -> {
                                        Checkbox(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                            checked = gridData.booleanValue ?: false,
                                            onCheckedChange = null,
                                        )
                                    }

                                    TableCellType.TEXT -> {
                                        TableImageOrTextCell(
                                            gridData = gridData,
                                            cellData = gridData.textCellData!!,
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        )
                                    }

                                    TableCellType.BUTTON -> {
                                        Column(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        ) {
                                            gridData.alButtonCellData?.let { alButtonCellData ->
                                                for (cellData in alButtonCellData) {
                                                    if (cellData.icon.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                call(cellData.url, cellData.inNewWindow)
                                                            }
                                                        ) {
                                                            ImageOrTextFromName(cellData.icon, gridData.tooltip, styleOtherIconSize)
                                                        }
                                                    } else if (cellData.image.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = {
                                                                call(cellData.url, cellData.inNewWindow)
                                                            }
                                                        ) {
                                                            ImageOrTextFromName(cellData.image, gridData.tooltip, styleOtherIconSize)
                                                        }
                                                    } else {
                                                        Button(
                                                            shape = RoundedCornerShape(0.dp),
                                                            onClick = {
                                                                call(cellData.url, cellData.inNewWindow)
                                                            }
                                                        ) {
                                                            Text(
                                                                text = cellData.text,
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
                                    }

                                    TableCellType.GRID -> {
                                        Column(
                                            modifier = Modifier.padding(TABLE_CELL_PADDING),
                                        ) {
                                            gridData.alGridCellData?.let { alGridCellData ->
                                                for (alRow in alGridCellData) {
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
                                                                gridData = gridData,
                                                                cellData = cellData,
                                                                modifier = modifier,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                                gridData?.let {
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
                                            menuClick = { url: String, inNewWindow: Boolean ->
                                                gridData.isShowPopupMenu = false
                                                call(url, inNewWindow)
                                            },
                                        )
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
                        enabled = pageButton.url.isNotEmpty(),
//                                cursor("pointer")
                        onClick = {
                            call(pageButton.url, false)
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
        selectorCancelUrl = tableResponse.selectorCancelURL

        findUrl = tableResponse.findURL
        findText = tableResponse.findText

        readAddButtons()
        readServerButtons()
        readClientButtons()
        readPageButtons()
        readTable()

        //--- установка текущей строки
        setCurrentRow(tableResponse.selectedRow)

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

            val backColor = when (tc.backColorType) {
                TableCellBackColorType.DEFINED -> Color(tc.backColor)
                TableCellBackColorType.GROUP_0 -> colorTableGroupBack0
                TableCellBackColorType.GROUP_1 -> colorTableGroupBack1
                else -> if (tc.dataRow % 2 == 0) {
                    colorTableRowBack0
                } else {
                    colorTableRowBack1
                }
            }
            val textColor = when (tc.foreColorType) {
                TableCellForeColorType.DEFINED -> Color(tc.foreColor)
                else -> Color.Black
            }
            val align = when (tc.cellType) {
                TableCellType.BUTTON -> {
                    Alignment.Center
                }

                TableCellType.CHECKBOX -> {
                    Alignment.Center
                }
                //--- на самом деле нет других вариантов
                //TableCellType.TEXT -> {
                else -> {
                    when (tc.align) {
                        TableCellAlign.LEFT -> Alignment.CenterStart
                        TableCellAlign.CENTER -> Alignment.Center
                        TableCellAlign.RIGHT -> Alignment.CenterEnd
                        else -> Alignment.Center
                    }
                }
            }

            var textCellData: TableCellDataClient? = null
            val alButtonCellData = mutableListOf<TableCellDataClient>()
            val alGridCellData = mutableListOf<MutableList<TableCellDataClient>>()

            when (tc.cellType) {
                TableCellType.CHECKBOX -> {
                }

                TableCellType.TEXT -> {
                    textCellData = TableCellDataClient(
                        icon = tc.textCellData.icon,
                        image = tc.textCellData.image,
                        text = tc.textCellData.text,
                    )
//                        if (!tc.isWordWrap) {
//                            whiteSpace("nowrap")
//                        }
//                        userSelect(if (getStyleIsTouchScreen()) "none" else "auto")
                }

                TableCellType.BUTTON -> {
                    tc.alButtonCellData.forEachIndexed { index, cellData ->
                        alButtonCellData.add(
                            TableCellDataClient(
                                icon = cellData.icon,
                                image = cellData.image,
                                text = cellData.text,
                                url = cellData.url,
                                inNewWindow = cellData.inNewWindow,
//                                    setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
//                                    backgroundColor(getColorButtonBack())
//                                    color(textColor)
//                                    fontSize(styleCommonButtonFontSize)
//                                    padding(styleTextButtonPadding)
                            )
                        )
                    }
                }

                TableCellType.GRID -> {
                    tc.alGridCellData.forEach { cellRow ->
                        alGridCellData.add(mutableListOf())

                        cellRow.forEach { cellData ->
                            alGridCellData.last().add(
                                TableCellDataClient(
                                    icon = cellData.icon,
                                    image = cellData.image,
                                    text = cellData.text,
//                                        userSelect(if (getStyleIsTouchScreen()) "none" else "auto")
//                                        if (!tc.isWordWrap) {
//                                            whiteSpace("nowrap")
//                                        }
                                )
                            )
                        }
                    }
                }
            }
            val tableCell = TableGridData(
                cellType = tc.cellType,
                backColor = backColor,
                textColor = textColor,
                align = align,
                isBoldText = tc.fontStyle != 0,
                tooltip = tc.tooltip,
                colSpan = tc.colSpan,
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
//                    cursor(
//                        if (url.isBlank()) {
//                            "default"
//                        } else {
//                            "pointer"
//                        }
//                    )
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
        if (currentRowNo >= 0 && alRowData[currentRowNo].formURL.isNotEmpty()) {
            call(alRowData[currentRowNo].formURL, false)
        }
    }

    private fun doGoto() {
        if (currentRowNo >= 0 && alRowData[currentRowNo].gotoURL.isNotEmpty()) {
            call(alRowData[currentRowNo].gotoURL, alRowData[currentRowNo].isGotoURLInNewWindow)
        }
    }

    private fun doPopup() {
        currentGridData?.let { gridData ->
            if (gridData.dataRow >= 0) {
                showPopupMenu(gridData)
            }
        }
    }

    private fun setCurrentRow(rowNo: Int) {
        isFormButtonVisible = rowNo >= 0 && alRowData[rowNo].formURL.isNotEmpty()
        isGotoButtonVisible = rowNo >= 0 && alRowData[rowNo].gotoURL.isNotEmpty()
        isPopupButtonVisible = rowNo >= 0 && alRowData[rowNo].alPopupData.isNotEmpty()

        currentRowNo = rowNo

//        focusToCursorField(tabId)
    }

    private fun doKeyEnter() {
        if (currentRowNo >= 0 && currentRowNo < alRowData.size) {
            val curRowData = alRowData[currentRowNo]
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
        if (currentRowNo > 0) {
            setCurrentRow(currentRowNo - 1)
        }
    }

    private fun doKeyDown() {
        if (currentRowNo < alRowData.lastIndex) {
            setCurrentRow(currentRowNo + 1)
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

    private fun showPopupMenu(gridData: TableGridData) {
        val row = gridData.dataRow

        //--- чтобы строчка выделялась и по правой кнопке мыши тоже
        setCurrentRow(row)

//        val mouseX = mouseEvent?.pageX ?: (window.innerWidth.toDouble() / 3)

        if (alRowData[row].alPopupData.isNotEmpty()) {
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
            convertPopupMenuDataClient(gridData, alRowData[row].alPopupData)
            gridData.isShowPopupMenu = true
        } else {
            gridData.isShowPopupMenu = false
        }
    }

    private fun convertPopupMenuDataClient(gridData: TableGridData, alMenuDataClient: List<TablePopupData>) {
        val alCurPopupData = mutableListOf<MenuDataClient>()

        var i = 0
        while (i < alMenuDataClient.size) {
            val MenuDataClient = alMenuDataClient[i]

            if (MenuDataClient.group.isEmpty()) {
                alCurPopupData.add(
                    MenuDataClient(
                        url = MenuDataClient.url,
                        text = MenuDataClient.text,
                        alSubMenu = null,
                        inNewWindow = MenuDataClient.inNewWindow
                    )
                )
                i++
            } else {
                val groupName = MenuDataClient.group

                val alPopupSubMenuDataClient = mutableListOf<MenuDataClient>()
                while (i < alMenuDataClient.size) {
                    val subMenuDataClient = alMenuDataClient[i]
                    if (subMenuDataClient.group.isEmpty() || subMenuDataClient.group != groupName) {
                        break
                    }

                    alPopupSubMenuDataClient.add(
                        MenuDataClient(
                            url = subMenuDataClient.url,
                            text = subMenuDataClient.text,
                            alSubMenu = null,
                            inNewWindow = subMenuDataClient.inNewWindow
                        )
                    )
                    i++
                }
                alCurPopupData.add(
                    MenuDataClient(
                        url = "",
                        text = groupName,
                        alSubMenu = alPopupSubMenuDataClient,
                        inNewWindow = false
                    )
                )
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
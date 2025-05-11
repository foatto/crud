package foatto.compose.control

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import foatto.compose.*
import foatto.compose.composable.GenerateMenuBody
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.composable.table.TableImageOrTextCell
import foatto.compose.control.composable.table.TableToolBar
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
import foatto.compose.model.MenuDataClient
import foatto.compose.utils.maxDp
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.AppRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.table.TablePopupData
import foatto.core.model.response.table.TableResponse
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBooleanCell
import foatto.core.model.response.table.cell.TableButtonCell
import foatto.core.model.response.table.cell.TableCellAlign
import foatto.core.model.response.table.cell.TableCellBackColorType
import foatto.core.model.response.table.cell.TableGridCell
import foatto.core.model.response.table.cell.TableSimpleCell
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

typealias SelectorFunType = ((selectorData: Map<String, String>) -> Unit)

val tableSelectorFuns: MutableMap<Long, SelectorFunType> = mutableMapOf()

//--- base/standart table/grid control

@OptIn(ExperimentalFoundationApi::class)
class TableControl(
    private val root: Root,
    private val appControl: AppControl,
    private val tableAction: AppAction,
    private val tableResponse: TableResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val SCROLL_BAR_TICKNESS = 16.dp

        private val TABLE_CELL_PADDING = 16.dp
    }

    //--- for custom client actions in custom table/grid controls
    protected var tableClientActionFun: (
        action: AppAction,
        alParam: List<Pair<String, String>>,
        tableControl: TableControl
    ) -> Unit = { _: AppAction, _: List<Pair<String, String>>, _: TableControl ->
    }

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

    private val alRowData = mutableStateListOf<TableRowData>()

    private val hmColumnMaxWidth = mutableStateMapOf<Int, Dp>()

    private var currentGridData by mutableStateOf<TableBaseCellClient?>(null)
    private var currentRowNo by mutableStateOf<Int?>(null)

    private var pageUpAction: AppAction? = null
    private var pageDownAction: AppAction? = null

    private var tableBodyWidth: Float by mutableFloatStateOf(0.0f)
    private var tableBodyHeight: Float by mutableFloatStateOf(0.0f)

    private var vScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private val rowHeights = mutableStateMapOf<Int, Float>()

    private var hScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private val colWidths = mutableStateMapOf<Int, Float>()

    private val verticalScrollState = ScrollState(0)
    private val horizontalScrollState = ScrollState(0)

    @Composable
    override fun Body() {
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (tableAction.isSelectorMode) {
                        Modifier.background(colorTableSelectorBack)
                    } else {
                        Modifier
                    }
                ),
        ) {
            getHeader { action: AppAction ->
                coroutineScope.launch {
                    call(action, false)
                }
            }
            TableToolBar(
                isSelectorMode = tableAction.isSelectorMode,
                isWideScreen = root.isWideScreen,
                selectorCancelAction = if (tableAction.isSelectorMode) {
                    { closeSelector() }
                } else {
                    null
                },
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
                doFind = { isClear: Boolean -> coroutineScope.launch { doFind(isClear) } },
                doForm = { coroutineScope.launch { doForm() } },
                doGoto = { coroutineScope.launch { doGoto() } },
                doPopup = { doPopup() },
                clientAction = { action: AppAction, params: List<Pair<String, String>> -> clientAction(action, params) },
                call = { action: AppAction, inNewTab: Boolean -> coroutineScope.launch { call(action, inNewTab) } },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorToolBar)
                    .horizontalScroll(state = horizontalScrollState)
            ) {
                alCaptionData.forEachIndexed { col, captionData ->
                    Button(
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(0.dp),   // чтобы получился сплошной ряд заголовков
                        colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                        modifier = Modifier.then(
                            hmColumnMaxWidth[col]?.let { maxColWidth ->
                                Modifier.width(maxColWidth)
                            } ?: Modifier
                        ),
                        onClick = {
                            captionData.action?.let {
                                coroutineScope.launch {
                                    call(captionData.action, false)
                                }
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

            TableBody(Modifier.weight(1.0f))

            //--- Page Bar
            if (alPageButton.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorBottomBar),
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
                            shape = RoundedCornerShape(0.dp),   // чтобы получился почти сплошной ряд кнопок
                            colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                            enabled = pageButton.action != null,
//                                cursor("pointer")
                            onClick = {
                                pageButton.action?.let { action ->
                                    coroutineScope.launch {
                                        call(action, false)
                                    }
                                }
                            }
                        ) {
                            Text(pageButton.text)
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

    @Composable
    fun TableBody(modifier: Modifier) {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        //--- bugfix for verticalScrollState/horizontalScrollState value cashing
        key(verticalScrollState, horizontalScrollState) {
            Column(
                modifier = modifier
                    .fillMaxHeight()
            ) {
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
                                tableBodyWidth = size.width.toFloat()
                                tableBodyHeight = size.height.toFloat()
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
                    ) {
                        alGridRows.forEachIndexed { index, alGridRow ->
                            Row(
                                modifier = Modifier
                                    //.width(intrinsicSize = IntrinsicSize.Max) - оставлю на память, чтобы опять не искать, но пользы/разницы не увидел
                                    //--- чтобы ячейки занимали всю отведённую им (общую) высоту (работает только совместно с .fillMaxHeight())
                                    .height(intrinsicSize = IntrinsicSize.Max)
                                    .onSizeChanged { size ->
                                        rowHeights[index] = size.height.toFloat()
                                    }
                            ) {
                                alGridRow.forEachIndexed { col, gridData ->
                                    Box(
                                        contentAlignment = gridData?.align ?: Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxHeight()    // работает только совместно с .height(intrinsicSize = IntrinsicSize.Max)
                                            .background(
                                                gridData?.let {
                                                    if (gridData.dataRow == currentRowNo) {
                                                        colorTableCurrentRow
                                                    } else {
                                                        gridData.backColor
                                                    }
                                                } ?: colorMainBack0
                                            )
                                            .border(BorderStroke(width = 0.1.dp, color = colorTableCellBorder))
                                            //.wrapContentSize(align = Alignment.Center, unbounded = true)  //- ничем не помогает
                                            //--- Непрямой метод поймать правую кнопку мыши. Ждём, когда завезут официальную реакцию на правую мышь.
                                            .pointerInput(Unit) {
                                                awaitEachGesture {
                                                    val event = awaitPointerEvent()
                                                    if (event.buttons.isSecondaryPressed) {
                                                        showPopupMenu(gridData)
                                                    }
                                                }
                                            }
                                            .combinedClickable(
                                                onDoubleClick = {
                                                    gridData?.dataRow?.let { dataRow ->
                                                        val rowData = alRowData[dataRow]
                                                        rowData.rowAction?.let { rowAction ->
                                                            coroutineScope.launch {
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
                                                        maxDp(size.width.toDp(), gridData.minWidth.dp)
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

                                                    colWidths[col] = with(density) {
                                                        hmColumnMaxWidth[col]?.toPx() ?: 0.0f
                                                    }
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
                                                    modifier = Modifier.padding(TABLE_CELL_PADDING),    //.background(Color.Transparent), - и без этого хорошо
                                                    colors = colorCheckBox ?: CheckboxDefaults.colors(),
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
                                                        ImageOrTextFromNameControl(
                                                            name = cellData.name,
                                                            iconSize = styleOtherIconSize,
                                                            imageButton = { func ->
                                                                FilledIconButton(
                                                                    shape = singleButtonShape,
                                                                    colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
                                                                    onClick = {
                                                                        cellData.action?.let {
                                                                            coroutineScope.launch {
                                                                                call(cellData.action, cellData.inNewTab)
                                                                            }
                                                                        }
                                                                    }
                                                                ) {
                                                                    func()
                                                                }
                                                            },
                                                            textButton = { caption ->
                                                                Button(
                                                                    shape = singleButtonShape,
                                                                    colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                                                                    onClick = {
                                                                        cellData.action?.let {
                                                                            coroutineScope.launch {
                                                                                call(cellData.action, cellData.inNewTab)
                                                                            }
                                                                        }
                                                                    }
                                                                ) {
                                                                    Text(
                                                                        text = caption,
//                                                                color = gridData.textColor,
                                                                        fontWeight = if (gridData.isBoldText) {
                                                                            FontWeight.Bold
                                                                        } else {
                                                                            null
                                                                        },
                                                                    )
                                                                }
                                                            }
                                                        )
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
                                        if (!tableAction.isSelectorMode) {
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
                                                        menuClick = { action: AppAction, inNewTab: Boolean ->
                                                            gridData.isShowPopupMenu = false
                                                            coroutineScope.launch {
                                                                call(action, inNewTab)
                                                            }
                                                        },
                                                    )
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
                HorizontalScrollBody()
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
            val decScaleY = min(1.0f, tableBodyHeight / rowHeightSum)
            val scrollBarH = vScrollBarLength * decScaleY
            val scrollBarY = if (tableBodyHeight >= rowHeightSum) {
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

    @Composable
    private fun HorizontalScrollBody() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SCROLL_BAR_TICKNESS)
                .onSizeChanged { size ->
                    hScrollBarLength = size.width.toFloat()
                }
                .clipToBounds()
                .onPointerEvents(
                    withInteractive = true,
                    onPointerDown = { pointerInputChange -> },
                    onPointerUp = { pointerInputChange -> },
                    onDragStart = { offset -> },
                    onDrag = { pointerInputChange, offset ->
                        val dx = offset.x
                        if (
                            dx < 0 && horizontalScrollState.value > 0 ||
                            dx > 0 && horizontalScrollState.value < horizontalScrollState.maxValue
                        ) {
                            coroutineScope.launch {
                                val colWidthSum = colWidths.values.sum()
                                val incScaleX = max(1.0f, colWidthSum / hScrollBarLength)
                                horizontalScrollState.scrollBy(dx * incScaleX)
                                //horizontalScrollState.scrollTo((horizontalScrollState.value + dx * incScaleX).toInt())
                            }
                        }
                    },
                    onDragEnd = { },
                    onDragCancel = { },
                )
        ) {
            drawRect(
                topLeft = Offset(0.0f, 0.0f),
                size = Size(hScrollBarLength, with(density) { SCROLL_BAR_TICKNESS.toPx() }),
                color = colorScrollBarBack,
                style = Fill,
            )
            val colWidthSum = colWidths.values.sum()
            val decScaleX = min(1.0f, tableBodyWidth / colWidthSum)
            val scrollBarW = hScrollBarLength * decScaleX
            val scrollBarX = if (tableBodyWidth >= colWidthSum) {
                0.0f
            } else {
                (hScrollBarLength - scrollBarW) * horizontalScrollState.value / horizontalScrollState.maxValue
            }
            drawRect(
                topLeft = Offset(scrollBarX, 0.0f),
                size = Size(scrollBarW, with(density) { SCROLL_BAR_TICKNESS.toPx() }),
                color = colorScrollBarFore,
                style = Fill,
            )
        }
    }

    override suspend fun start() {
        root.setTabInfo(tabId, tableResponse.tabCaption)

        startTableBody()
    }

    fun startTableBody() {
        //--- вынесено сюда для использования во внешнем CompositeControl
        headerData = tableResponse.headerData

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
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        value = tc.value,
                    )
                }

                is TableSimpleCell -> {
                    TableSimpleCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        //tc.isWordWrap
                        data = TableCellDataClient(
                            name = tc.name,
                        )
                    )
                }

                is TableButtonCell -> {
                    TableButtonCellClient(
                        colSpan = tc.colSpan,
                        dataRow = tc.dataRow,
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        data = tc.values.map { tableButtonCellData ->
                            TableCellDataClient(
                                name = tableButtonCellData.name,
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
                        minWidth = tc.minWidth,
                        align = align,
                        backColor = backColor,
                        textColor = textColor,
                        isBoldText = tc.isBoldText,
                        data = tc.values.map { alTableGridCellData ->
                            alTableGridCellData.map { name ->
                                TableCellDataClient(
                                    name = name,
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

    private suspend fun doFind(isClear: Boolean) {
        if (isClear) {
            findText = ""
        }

        if (!isClear && !isFindTextVisible) {
            isFindTextVisible = true
        } else {
            call(
                newAppAction = tableAction.copy(
                    findText = findText.trim(),
                    pageNo = 0,
                ),
                inNewTab = false,
            )
        }
    }

    private suspend fun doForm() {
        currentRowNo?.let { curRow ->
            alRowData[curRow].formAction?.let { formAction ->
                call(formAction, false)
            }
        }
    }

    private suspend fun doGoto() {
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

    private suspend fun doKeyEnter() {
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
        if (tableAction.isSelectorMode) {
            closeSelector()
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

    private suspend fun doKeyPageUp() {
        pageUpAction?.let { action ->
            call(action, false)
        }
    }

    private suspend fun doKeyPageDown() {
        pageDownAction?.let { action ->
            call(action, false)
        }
    }

    suspend fun refreshTableFromComposite() {
//!!!        call(tableAction, false, false) - сейчас обновляется вся вкладка!
    }

    private suspend fun call(newAppAction: AppAction, inNewTab: Boolean, withWait: Boolean = true) {
        if (tableAction.isSelectorMode) {
            when (newAppAction.type) {
                ActionType.FORM_SELECTOR -> {
                    tableSelectorFuns[tableAction.selectorFunId]?.invoke(newAppAction.selectorData)
                    closeSelector()
                }

                ActionType.MODULE_TABLE -> {
                    val selectorAppAction = newAppAction.copy(
                        prevAction = tableAction,
                        isSelectorMode = true,
                        selectorFunId = tableAction.selectorFunId,
                    )
                    if (withWait) {
                        root.setWait(true)
                    }
                    invokeRequest(
                        AppRequest(
                            action = selectorAppAction
                        )
                    ) { appResponse: AppResponse ->
                        when (appResponse.responseCode) {
                            ResponseCode.MODULE_TABLE -> {
                                root.selectorControl = TableControl(root, appControl, selectorAppAction, appResponse.table!!, tabId)
                                root.selectorControl?.start()
                            }

                            else -> {
                                //!!! dialog с ошибкой
                                println("Wrong response.responseCode in SELECTOR_MODE = ${appResponse.responseCode}")
                            }
                        }
                        if (withWait) {
                            root.setWait(false)
                        }
                    }

                }

                else -> {    //--- невозможная (?) ситуация
                    //!!! dialog с ошибкой
                    println("Wrong call in SELECTOR_MODE = $newAppAction")
                }
            }
        } else {
            if (inNewTab) {
                root.openTab(newAppAction)
            } else {
                appControl.call(
                    AppRequest(
                        action = newAppAction.copy(
                            prevAction = tableAction,
                        )
                    )
                )
            }
        }
    }

    private fun closeSelector() {
        tableSelectorFuns.remove(tableAction.selectorFunId)
        root.selectorControl = null
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
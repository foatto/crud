package foatto.compose.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.colorScrollBarBack
import foatto.compose.colorScrollBarFore
import foatto.compose.control.composable.composite.CompositeToolBar
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.model.composite.CompositeBlockControl
import foatto.core.model.response.composite.CompositeBlock
import foatto.core.model.response.composite.CompositeResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class CompositeControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val compositeResponse: CompositeResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        private val SCROLL_BAR_TICKNESS = 16.dp

        //--- 350 - узковато - не помещаются некоторые наименования, при этом 6-й столбец на стандартный FullHD уже не помещается;
        //--- 400 - ширина достаточна, при этом 5-й столбец на стандартный FullHD помещается не полностью;
        //--- 380 == (1920 - 16) / 5 (с учётом вертикального тулбара)
        const val BLOCK_MIN_WIDTH: Int = 360

        //--- 200 - низковато, соседние заголовки и подписи располагаются слишком близко
        const val BLOCK_MIN_HEIGHT: Int = 240
    }

    private var refreshInterval: Int by mutableIntStateOf(0)

    private var canvasWidth: Float by mutableFloatStateOf(0.0f)
    private var canvasHeight: Float by mutableFloatStateOf(0.0f)

    private var oneBlockWidth by mutableFloatStateOf(0.0f)
    private var oneBlockHeight by mutableFloatStateOf(0.0f)

    private var blocks by mutableStateOf<List<CompositeBlockControl>>(emptyList())

    private var vScrollBarLength: Float by mutableFloatStateOf(0.0f)
    private var hScrollBarLength: Float by mutableFloatStateOf(0.0f)

    private var screenOffsetX: Float by mutableFloatStateOf(0.0f)
    private var screenOffsetY: Float by mutableFloatStateOf(0.0f)

    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
//        val textMeasurer = rememberTextMeasurer()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getHeader()

            CompositeToolBar(
                isWideScreen = root.isWideScreen,
                refreshInterval = refreshInterval,
                setInterval = { interval: Int -> coroutineScope.launch { setInterval(interval) } },
            )

            //--- bugfix for compositeResponse value caching
            key(compositeResponse) {
                Row(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { size ->
                                canvasWidth = size.width.toFloat()
                                canvasHeight = size.height.toFloat()

                                oneBlockWidth = max(BLOCK_MIN_WIDTH * root.scaleKoef, canvasWidth / compositeResponse.cols)
                                oneBlockHeight = max(BLOCK_MIN_HEIGHT * root.scaleKoef, canvasHeight / compositeResponse.rows)
                            }
                            .onPointerEvents(
                                withInteractive = true,
                                onPointerDown = { pointerInputChange -> },
                                onPointerUp = { pointerInputChange -> },
                                onDragStart = { offset -> },
                                onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset.x, offset.y) },
                                onDragEnd = { },
                                onDragCancel = { },
                            ),
                    ) {
                        for (block in blocks) {
                            val blockX = block.x * oneBlockWidth + if (block.isScrollable) {
                                screenOffsetX
                            } else {
                                0.0f
                            }
                            val blockY = block.y * oneBlockHeight + if (block.isScrollable) {
                                screenOffsetY
                            } else {
                                0.0f
                            }
                            val blockW = block.w * oneBlockWidth
                            val blockH = block.h * oneBlockHeight

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = with(density) { blockX.toDp() },
                                        y = with(density) { blockY.toDp() },
                                    )
                                    .size(
                                        width = with(density) { blockW.toDp() },
                                        height = with(density) { blockH.toDp() },
                                    )
                            ) {
                                block.chartBlock?.MainChartBody(false)
                                    ?: block.mapBlock?.getXyElementTemplate(false)
                                    ?: block.schemeBlock?.getXyElementTemplate(false)
                                    ?: block.tableBlock?.TableBody(Modifier)
                                    ?: run {
                                        Text(text = "Не задан тип блока!")
                                    }
                            }
                        }
                    }
                    VerticalScrollBody()
                }
                HorizontalScrollBody()
            }
        }

        coroutineScope.launch {
            while (refreshInterval > 0) {
                refreshAll()
                delay(refreshInterval * 1000L)
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
                        val pixEndY = compositeResponse.rows * oneBlockHeight
                        val incScaleY = pixEndY / vScrollBarLength
                        onDrag(pointerInputChange, 0.0f, -offset.y * incScaleY)
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
            val pixEndY = compositeResponse.rows * oneBlockHeight
            val decScaleY = min(1.0f, canvasHeight / pixEndY)
            val scrollBarH = vScrollBarLength * decScaleY
            val scrollBarY = if (canvasHeight >= pixEndY) {
                0.0f
            } else {
                (vScrollBarLength - scrollBarH) * (-screenOffsetY) / (pixEndY - canvasHeight)
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
                        val pixEndX = compositeResponse.cols * oneBlockWidth
                        val incScaleX = pixEndX / hScrollBarLength
                        onDrag(pointerInputChange, -offset.x * incScaleX, 0.0f)
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
            val pixEndX = compositeResponse.cols * oneBlockWidth
            val decScaleX = min(1.0f, canvasWidth / pixEndX)
            val scrollBarW = hScrollBarLength * decScaleX
            val scrollBarX = if (canvasWidth >= pixEndX) {
                0.0f
            } else {
                (hScrollBarLength - scrollBarW) * (-screenOffsetX) / (pixEndX - canvasWidth)
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
        root.setTabInfo(tabId, compositeResponse.tabCaption)
        headerData = compositeResponse.headerData

        val newBlocks = mutableListOf<CompositeBlockControl>()

        //--- scrollable blocks first
        compositeResponse.blocks.filter { block -> block.isScrollable }.forEach { blockControlData ->
            readBlock(blockControlData, newBlocks)
        }
        //--- static/non-scrollable blocks seconds (as next layer for drawing)
        compositeResponse.blocks.filter { block -> !block.isScrollable }.forEach { blockControlData ->
            readBlock(blockControlData, newBlocks)
        }

        blocks = newBlocks
        /*
            !!! setInterval(10)
         */
    }

    private suspend fun readBlock(blockControlData: CompositeBlock, newBlocks: MutableList<CompositeBlockControl>) {
        newBlocks += CompositeBlockControl(
            isScrollable = blockControlData.isScrollable,
            x = blockControlData.x.toFloat(),
            y = blockControlData.y.toFloat(),
            w = blockControlData.w.toFloat(),
            h = blockControlData.h.toFloat(),
            chartBlock = blockControlData.chartResponse?.let { chartResponse ->
                ChartControl(root, appControl, blockControlData.action, chartResponse, tabId).apply {
                    startChartBody()
                }
            },
            mapBlock = blockControlData.mapResponse?.let { mapResponse ->
                MapControl(root, appControl, blockControlData.action, mapResponse, tabId).apply {
                    startMapBody(curScale = this.minXyScale)
                }
            },
            schemeBlock = blockControlData.schemeResponse?.let { schemeResponse ->
                SchemeControl(root, appControl, blockControlData.action, schemeResponse, tabId).apply {
                    startSchemeBody(
                        startExpandKoef = SchemeControl.SCHEME_START_EXPAND_KOEF,
                        isCentered = true,
                        curScale = 1,
                    )
                }
            },
            tableBlock = blockControlData.tableResponse?.let { tableResponse ->
                TableControl(root, appControl, blockControlData.action, tableResponse, tabId).apply {
                    startTableBody()
                }
            },
        )
    }

    private suspend fun setInterval(sec: Int) {
        refreshInterval = sec

        if (sec == 0) {
            refreshAll()
        }
    }

    private suspend fun refreshAll() {
        for (block in blocks) {
            block.chartBlock?.doChartRefresh(null, false)
                ?: run {
                    block.chartBlock?.doChartRefresh(null, false)
                        ?: block.mapBlock?.mapRefreshView(null, false)
                        ?: block.schemeBlock?.schemeRefreshView(null, false)
                        ?: block.tableBlock?.refreshTableFromComposite()
                        ?: println("refreshAll: не задан тип блока!")
                }
        }
    }

    private fun onDrag(change: PointerInputChange, dx: Float, dy: Float) {
        val pixEndX = compositeResponse.cols * oneBlockWidth
        val pixEndY = compositeResponse.rows * oneBlockHeight

        val newOffsetX = screenOffsetX + dx
        val newOffsetY = screenOffsetY + dy

        if (newOffsetX <= 0) {
            screenOffsetX = if (newOffsetX >= canvasWidth - pixEndX) {
                newOffsetX
            } else {
                canvasWidth - pixEndX
            }
        } else {
            screenOffsetX = 0.0f
        }

        if (newOffsetY <= 0) {
            screenOffsetY = if (newOffsetY >= canvasHeight - pixEndY) {
                newOffsetY
            } else {
                canvasHeight - pixEndY
            }
        } else {
            screenOffsetY = 0.0f
        }
    }

}
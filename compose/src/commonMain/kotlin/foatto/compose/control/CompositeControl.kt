package foatto.compose.control

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.control.composable.composite.CompositeToolBar
import foatto.compose.control.model.composite.CompositeBlockControl
import foatto.core.model.response.composite.CompositeResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CompositeControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val compositeResponse: CompositeResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    private var refreshInterval: Int by mutableIntStateOf(0)

    private var oneBlockWidth by mutableFloatStateOf(0.0f)
    private var oneBlockHeight by mutableFloatStateOf(0.0f)

    private val blocks = mutableStateListOf<CompositeBlockControl>()

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

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { size ->
                        oneBlockWidth = size.width.toFloat() / compositeResponse.cols
                        oneBlockHeight = size.height.toFloat() / compositeResponse.rows
                    }
//                    .pointerInput(Unit) {
//                        awaitEachGesture {
//                            awaitFirstDown().also { pointerInputChange: PointerInputChange ->
//                                pointerInputChange.consume()
//                                onPointerDown(pointerInputChange)
//                            }
//                            while (true) {
//                                val pointerInputChange = waitForUpOrCancellation()
//                                if (pointerInputChange != null) {
//                                    pointerInputChange.consume()
//                                    onPointerUp(pointerInputChange)
//                                    break
//                                }
//                            }
//                        }
//                    }
//                    .pointerInput(Unit) {
//                        detectDragGestures(
//                            onDragStart = { offset: Offset -> onDragStart(offset) },
//                            onDragEnd = { onDragEnd() },
//                            onDragCancel = { onDragCancel() },
//                            onDrag = { change, dragAmount ->
//                                change.consume()
//                                onDrag(change, dragAmount)
//                            },
//                        )
//                    }
            ) {
                for (block in blocks) {
                    val blockX = block.x * oneBlockWidth
                    val blockY = block.y * oneBlockHeight
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
                            ?: run {
                                Text(text = "Не задан тип блока!")
                            }
                    }
                }
            }
        }

        coroutineScope.launch {
            while (refreshInterval > 0) {
                refreshAll()
                delay(refreshInterval * 1000L)
            }
        }
    }

    override suspend fun start() {
        root.setTabInfo(tabId, compositeResponse.tabCaption)
        headerData = compositeResponse.headerData

        blocks.clear()
        compositeResponse.blocks.forEach { blockControlData ->
            blocks += CompositeBlockControl(
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
                        startMapBody(
                            startExpandKoef = MapControl.MAP_START_EXPAND_KOEF,
                            isCentered = false,
                            curScale = this.minXyScale
                        )
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
                }
            )
        }
        /*
            !!! setInterval(10)
         */
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
                    block.mapBlock?.mapRefreshView(null, false)
                        ?: run {
                            block.schemeBlock?.schemeRefreshView(null, false)
                        }
                }
        }
    }

}
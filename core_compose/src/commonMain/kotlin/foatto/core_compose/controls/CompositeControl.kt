package foatto.core_compose.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import foatto.core.app.composite.CompositeResponse
import foatto.core.link.XyDocumentClientType
import foatto.core_compose.AppControl
import foatto.core_compose.Root
import foatto.core_compose.controls.models.TitleData
import foatto.core_compose.controls.models.composite.CompositeBlock

class CompositeControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val compositeResponse: CompositeResponse,
    tabId: Int,
) : AbstractControl(tabId) {

    private var compositeWidth: Float by mutableFloatStateOf(0.0f)
    private var compositeHeight: Float by mutableFloatStateOf(0.0f)

    private val blocks = mutableListOf<CompositeBlock>()

    @Composable
    override fun Body() {
        val density = LocalDensity.current
//        val coroutineScope = rememberCoroutineScope()
//        val textMeasurer = rememberTextMeasurer()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getChartAndXyAndCompositeHeader()

//            CompositeToolBar(
//                isWideScreen = root.isWideScreen,
//                isToolBarsVisible = isToolBarsVisible,
//                refreshInterval = refreshInterval,
//                isPanButtonEnabled = isPanButtonEnabled,
//                isZoomButtonEnabled = isZoomButtonEnabled,
//                isDistancerButtonEnabled = isDistancerButtonEnabled,
//                isSelectButtonEnabled = isSelectButtonEnabled,
//                isZoomInButtonEnabled = isZoomInButtonEnabled,
//                isZoomOutButtonEnabled = isZoomOutButtonEnabled,
//                isAddElementButtonVisible = isAddElementButtonVisible,
//                isEditPointButtonVisible = isEditPointButtonVisible,
//                isMoveElementsButtonVisible = isMoveElementsButtonVisible,
//                isActionOkButtonVisible = isActionOkButtonVisible,
//                isActionCancelButtonVisible = isActionCancelButtonVisible,
//                alAddEC = alAddEC,
//                setMode = { mapWorkMode: MapWorkMode -> setMode(mapWorkMode) },
//                zoomIn = { zoomIn() },
//                zoomOut = { zoomOut() },
//                startAdd = { elementConfig: XyElementConfig -> /*!!! startAdd(elementConfig)*/ },
//                startEditPoint = { /*!!! startEditPoint()*/ },
//                startMoveElements = { /*!!! startMoveElements()*/ },
//                actionOk = { /*!!! actionOk()*/ },
//                actionCancel = { /*!!! actionCancel()*/ },
//                setInterval = { interval: Int -> setInterval(interval) }
//            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { size ->
                        compositeWidth = size.width.toFloat()
                        compositeHeight = size.height.toFloat()
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
                val oneBlockWidth = compositeWidth / compositeResponse.cols
                val oneBlockHeight = compositeHeight / compositeResponse.rows

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
                        block.xyBlock?.getXyElementTemplate(false)
                            ?: block.chartBlock?.MainChartBody(false)
                            ?: run {
                                Text(text = "Не задан тип блока!")
                            }
                    }
                }
            }
        }
    }

    override fun start() {
        root.setTabInfo(tabId, compositeResponse.title, compositeResponse.title)

        alTitleData.addAll(compositeResponse.title.split('\n').filter { it.isNotBlank() }.map { TitleData("", it) })

        compositeResponse.blocks.forEach { blockDTO ->
            blocks += CompositeBlock(
                x = blockDTO.x.toFloat(),
                y = blockDTO.y.toFloat(),
                w = blockDTO.w.toFloat(),
                h = blockDTO.h.toFloat(),
                xyBlock = blockDTO.xyResponse?.let { xyResponse ->
                    when (xyResponse.documentConfig.clientType) {
                        XyDocumentClientType.MAP -> MapControl(root, appControl, xyResponse, tabId).apply {
                            startXySpecific(
                                startExpandKoef = MapControl.MAP_START_EXPAND_KOEF,
                                isCentered = false,
                                curScale = xyResponse.documentConfig.hmElementConfig.minBy { (_, value) -> value.scaleMin }.value.scaleMin
                            )
                        }

                        XyDocumentClientType.SCHEME -> SchemeControl(root, appControl, xyResponse, tabId).apply {
                            startXySpecific(
                                startExpandKoef = SchemeControl.SCHEME_START_EXPAND_KOEF,
                                isCentered = true,
                                curScale = 1,
                            )
                        }
                    }
                },
                chartBlock = blockDTO.chartResponse?.let { chartResponse ->
                    ChartControl(root, appControl, chartResponse, tabId).apply {
                        startChartSpecific()
                    }
                }
            )
        }
        /*
                setInterval(10)
         */
    }
}
package foatto.compose.control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.hsl
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.platform.LocalDensity
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.control.composable.scheme.SchemeToolBar
import foatto.compose.control.model.scheme.ElementMoveData
import foatto.compose.control.model.table.ServerActionButtonClient
import foatto.compose.invokeRequest
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.model.scheme.SchemeParam
import foatto.core.model.model.xy.XyViewCoord
import foatto.core.model.request.SchemeActionRequest
import foatto.core.model.response.SchemeActionResponse
import foatto.core.model.response.xy.scheme.SchemeResponse
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class SchemeControl(
    root: Root,
    appControl: AppControl,
    private val schemeAction: AppAction,
    private val schemeResponse: SchemeResponse,
    tabId: Int
) : AbstractXyControl(root, appControl, schemeResponse.elementConfigs, tabId) {

    companion object {
        private val COLOR_SCHEME_LINE: Color = hsl(180.0f, 1.0f, 1.0f)
        private const val COLOR_SCHEME_LINE_WIDTH = 1.0f   //!!! max(1.0, scaleKoef).roundToInt()
        private val COLOR_SCHEME_FILL: Color = hsl(180.0f, 1.0f, 0.5f)

        const val SCHEME_START_EXPAND_KOEF: Float = 0.0f
    }

    private var isElementMoveEnabled by mutableStateOf(false)
    private var isShowElementList by mutableStateOf(false)

    private val alMoveableElementData = mutableStateListOf<ElementMoveData>()
    private val alServerButton = mutableStateListOf<ServerActionButtonClient>()

    private var isShowStateAlert by mutableStateOf(false)
    private var stateAlertMessage by mutableStateOf("")

    private var isMoveRectVisible by mutableStateOf(false)
    private var moveRectX by mutableFloatStateOf(0.0f)
    private var moveRectY by mutableFloatStateOf(0.0f)

    private var moveElementId = 0

    @Composable
    override fun Body() {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            getHeader()

            SchemeToolBar(
                isWideScreen = root.isWideScreen,
                isElementMoveEnabled = isElementMoveEnabled,
                isShowElementList = isShowElementList,
                alServerButton = alServerButton,
                alMoveableElementData = alMoveableElementData,
                refreshInterval = refreshInterval,
                onServerButtonClick = { action: AppAction -> coroutineScope.launch { invokeServerButton(action) } },
                onMoveButtonClick = { isShowElementList = !isShowElementList },
                doCloseElementList = { isShowElementList = false },
                onElementListClick = { data: ElementMoveData ->
                    moveElementId = data.id
                    isMoveRectVisible = true
                    moveRectX = (data.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef
                    moveRectY = (data.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef
                },
            ) { interval: Int -> coroutineScope.launch { setInterval(interval) } }

            getXyElementTemplate(true)
        }

//!!!        getStateAlertBody()

        coroutineScope.launch {
            while (refreshInterval > 0) {
                schemeRefreshView(null, false)
                delay(refreshInterval * 1000L)
            }
        }
    }

    override fun addSpecificXy(drawScope: DrawScope) {
        super.addSpecificXy(drawScope)

        drawScope.apply {
            if (isMoveRectVisible) {
                drawRectOnCanvas(
                    drawScope = this,
                    x = moveRectX,
                    y = moveRectY,
                    width = 64.0f,
                    height = 64.0f,
                    fillColor = COLOR_SCHEME_FILL,
                    fillAlpha = 0.5f,
                    strokeColor = COLOR_SCHEME_LINE,
                    strokeAlpha = 1.0f,
                    strokeStyle = Stroke(width = COLOR_SCHEME_LINE_WIDTH),
                )
            }
        }
    }

    override suspend fun start() {
        root.setTabInfo(tabId, schemeResponse.tabCaption)

        alServerButton.clear()
        schemeResponse.alServerActionButton?.let { alServerActionButton ->
            for (sab in alServerActionButton) {
                alServerButton.add(ServerActionButtonClient.readFromServerActionButton(sab))
            }
        }

        startSchemeBody(
            startExpandKoef = SCHEME_START_EXPAND_KOEF,
            isCentered = true,
            curScale = 1,
        )
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun startSchemeBody(
        startExpandKoef: Float,
        isCentered: Boolean,
        curScale: Int,
    ) {
        //--- вынесено сюда для использования во внешнем CompositeControl
        headerData = schemeResponse.headerData

        root.setWait(true)
        invokeRequest(
            SchemeActionRequest(
                action = schemeAction.copy(
                    type = ActionType.GET_COORDS
                )
            )
        ) { schemeActionResponse: SchemeActionResponse ->

            GlobalScope.launch {
                while (xyCanvasWidth == 0.0f) {
                    yield()
                }
                val newViewCoord = getXyCoordsDone(
                    startExpandKoef = startExpandKoef,
                    isCentered = isCentered,
                    curScale = curScale,
                    minCoord = schemeActionResponse.minCoord!!,
                    maxCoord = schemeActionResponse.maxCoord!!,
                )

                //--- именно до xyRefreshView, чтобы не сбросить сразу после включения
                root.setWait(false)
                schemeRefreshView(newViewCoord, true)
            }
        }
    }

    suspend fun schemeRefreshView(
        aView: XyViewCoord?,
        withWait: Boolean,
    ) {
        doStateRefreshView(
            aView = aView,
            withWait = withWait,
            doAdditionalWork = { schemeActionResponse: SchemeActionResponse ->
                schemeActionResponse.hmParam[SchemeParam.SCHEME_ALERT_MESSAGE]?.let { value ->
                    stateAlertMessage = value
                    isShowStateAlert = true
                }

                isElementMoveEnabled = (schemeActionResponse.hmParam[SchemeParam.SCHEME_ELEMENT_MOVE_ENABLED]?.toBooleanStrictOrNull() == true)

                val sElementMoveId = schemeActionResponse.hmParam[SchemeParam.SCHEME_ELEMENT_MOVE_ID]
                val sElementMoveDescr = schemeActionResponse.hmParam[SchemeParam.SCHEME_ELEMENT_MOVE_DESCR]
                val sElementMoveX = schemeActionResponse.hmParam[SchemeParam.SCHEME_ELEMENT_MOVE_X]
                val sElementMoveY = schemeActionResponse.hmParam[SchemeParam.SCHEME_ELEMENT_MOVE_Y]

                if (!sElementMoveId.isNullOrBlank() && !sElementMoveDescr.isNullOrEmpty() &&
                    !sElementMoveX.isNullOrBlank() && !sElementMoveY.isNullOrEmpty()
                ) {

                    val alElementMoveId = sElementMoveId.split('\n').filter { it.isNotBlank() }
                    val alElementMoveDescr = sElementMoveDescr.split('\n').filter { it.isNotBlank() }
                    val alElementMoveX = sElementMoveX.split('\n').filter { it.isNotBlank() }
                    val alElementMoveY = sElementMoveY.split('\n').filter { it.isNotBlank() }

                    alMoveableElementData.clear()
                    alElementMoveId.forEachIndexed { index, id ->
                        alMoveableElementData.add(
                            ElementMoveData(
                                id = id.toInt(),
                                descr = alElementMoveDescr[index],
                                x = alElementMoveX[index].toInt(),
                                y = alElementMoveY[index].toInt(),
                            )
                        )
                    }
                }
            },
        )
    }

    suspend fun doStateRefreshView(
        aView: XyViewCoord?,
        withWait: Boolean,
        doAdditionalWork: (xyActionResponse: SchemeActionResponse) -> Unit = { _: SchemeActionResponse -> },
    ) {
        aView?.let {
            //--- принимаем новый ViewCoord как есть, но корректируем масштаб в зависимости от текущего размера выводимой области
            aView.scale = calcXyScale(aView.x1, aView.y1, aView.x2, aView.y2)
            xyViewCoord = aView
        }
        getSchemeElements(
            mapBitmapTypeName = "",
            withWait = withWait,
            doAdditionalWork = doAdditionalWork,
        )
    }

    private suspend fun getSchemeElements(
        mapBitmapTypeName: String,
        withWait: Boolean,
        doAdditionalWork: (schemeActionResponse: SchemeActionResponse) -> Unit = { _: SchemeActionResponse -> },
    ) {
        if (withWait) {
            root.setWait(true)
        }
        invokeRequest(
            SchemeActionRequest(action = schemeAction.copy(type = ActionType.GET_ELEMENTS))
        ) { schemeActionResponse: SchemeActionResponse ->

            //--- сбрасываем горизонтальный и вертикальный скроллинг/смещение
            screenOffsetX = 0.0f
            screenOffsetY = 0.0f

            readXyElements(schemeActionResponse.elements)

            if (withWait) {
                root.setWait(false)
            }

            doAdditionalWork(schemeActionResponse)
        }
    }

    override fun onPointerDown(pointerInputChange: PointerInputChange) {
        println("Drag Down!")
    }

    override fun onPointerUp(pointerInputChange: PointerInputChange) {
        println("Drag Up!")
    }

    override fun onDragStart(offset: Offset) {
        println("Drag Start!")
    }

    override fun onDrag(pointerInputChange: PointerInputChange, dragAmount: Offset) {
        screenOffsetX += dragAmount.x
        screenOffsetY += dragAmount.y
        println("DRAG!")
    }

    override suspend fun onDragEnd() {
        println("Drag Finish!")
    }

    override fun onDragCancel() {
        println("Drag Cancel!")
    }

    private suspend fun setInterval(sec: Int) {
        refreshInterval = sec

        if (sec == 0) {
            schemeRefreshView(null, false)
        }
    }

    /*
        @Composable
        fun getStateAlertBody() {
            if (isShowStateAlert.value) {
                Div(
                    attrs = {
                        style {
                            position(Position.Fixed)
                            top(4.cssRem)
                            left(0.px)
                            width(100.percent)
                            bottom(0.px)
                            zIndex(Z_INDEX_STATE_ALERT)
                            backgroundColor(colorDialogBackColor)
                            display(DisplayStyle.Grid)
                            gridTemplateRows("1fr auto 1fr")
                            gridTemplateColumns("1fr auto 1fr")
                        }
                    }
                ) {
                    Div(
                        attrs = {
                            style {
                                gridArea("1", "2", "2", "3")
                            }
                        }
                    ) {
                        Br()
                    }
                    Div(
                        attrs = {
                            style {
                                gridArea("2", "2", "3", "3")
                                padding(styleDialogCellPadding)
                                setBorder(color = getColorDialogBorder(), radius = styleFormBorderRadius)
                                backgroundColor(getColorDialogCenterBack())
                                display(DisplayStyle.Flex)
                                flexDirection(FlexDirection.Column)
                                alignItems(AlignItems.Center)
                            }
                        }
                    ) {
                        Div(
                            attrs = {
                                style {
                                    alignSelf(AlignSelf.Center)
                                    fontSize(styleDialogTextFontSize)
                                    fontWeight("bold")
                                    textAlign("center")
                                    color(getColorDialogCenterText())
                                }
                            }
                        ) {
                            getMultilineText(stateAlertMessage.value)
                        }
                    }
                    Div(
                        attrs = {
                            style {
                                gridArea("3", "2", "4", "3")
                            }
                        }
                    ) {
                        Br()
                    }
                }
            }
        }

    //--------------------------------------------------------------------------------------------------------------------------------------------------------------

        override fun onXyMousePressed(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {}

        override fun onXyMouseMove(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double) {
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            if (isNeedOffsetCompensation) {
                mouseX -= xySvgLeft
                mouseY -= xySvgTop
            }

            if (isMoveRectVisible.value) {
                moveRectX.value = mouseX
                moveRectY.value = mouseY
            }
        }

        override fun onXyMouseReleased(isNeedOffsetCompensation: Boolean, aMouseX: Double, aMouseY: Double, shiftKey: Boolean, ctrlKey: Boolean, altKey: Boolean) {
            var mouseX = aMouseX.toInt()
            var mouseY = aMouseY.toInt()

            if (isNeedOffsetCompensation) {
                mouseX -= xySvgLeft
                mouseY -= xySvgTop
            }

            if (isMoveRectVisible.value) {
                val xyActionRequest = XyActionRequest(
                    documentTypeName = xyResponse.documentConfig.name,
                    action = XyAction.MOVE_ELEMENTS,
                    startParamId = xyResponse.startParamId,
                    elementId = moveElementId,
                    dx = xyViewCoord.x1 + (mouseX * xyViewCoord.scale / scaleKoef).roundToInt(),
                    dy = xyViewCoord.y1 + (mouseY * xyViewCoord.scale / scaleKoef).roundToInt(),
                )

                root.setWait(true)
                invokeXy(xyActionRequest) {
                    root.setWait(false)
                    xyRefreshView(null, true)
                }

                isMoveRectVisible.value = false
            }
        }

        override fun onXyMouseWheel(syntheticWheelEvent: SyntheticWheelEvent) {}

        override fun onXyTextPressed(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
            doStateTextPressed(xyElement)
        }

        override fun onXyTextPressed(syntheticTouchEvent: SyntheticTouchEvent, xyElement: XyElementData) {
            doStateTextPressed(xyElement)
        }

    */
    private suspend fun invokeServerButton(action: AppAction) {
        root.openTab(action)
    }

    /*
        private fun doStateTextPressed(xyElement: XyElementData) {
            root.dialogActionFun = {
                val xyActionRequest = XyActionRequest(
                    documentTypeName = xyResponse.documentConfig.name,
                    action = XyAction.CLICK_ELEMENT,
                    startParamId = xyResponse.startParamId,
                    elementId = xyElement.elementId,
                    objectId = xyElement.objectId
                )

                root.setWait(true)
                invokeXy(
                    xyActionRequest
                ) {
                    root.setWait(false)

                    root.dialogActionFun = {}
                    root.dialogQuestion.value = "Действие выполнено!"
                    root.showDialogCancel.value = false
                    root.showDialog.value = true
                }
            }
            root.dialogQuestion.value = xyElement.dialogQuestion!!
            root.showDialogCancel.value = true
            root.showDialog.value = true
        }

    }
     */
}


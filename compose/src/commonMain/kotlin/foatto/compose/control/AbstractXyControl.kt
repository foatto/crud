package foatto.compose.control

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import foatto.compose.AppControl
import foatto.compose.Root
import foatto.compose.control.composable.onPointerEvents
import foatto.compose.control.model.xy.XyElementData
import foatto.compose.control.model.xy.XyElementDataType
import foatto.core.model.model.xy.XyElement
import foatto.core.model.model.xy.XyViewCoord
import foatto.core.model.response.xy.XyElementClientType
import foatto.core.model.response.xy.XyElementConfig
import foatto.core.model.response.xy.geom.XyPoint
import foatto.core.model.response.xy.geom.XyRect
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlin.math.max
import kotlin.math.roundToInt

abstract class AbstractXyControl(
    protected val root: Root,
    private val appControl: AppControl,
    protected val elementConfigs: Map<String, XyElementConfig>,
    tabId: Int,
) : AbstractControl(tabId) {

    companion object {
        val COLOR_XY_LABEL_BACK = Color.hsl(60.0f, 1.0f, 0.5f)
        val COLOR_XY_LABEL_BORDER = Color.hsl(60.0f, 1.0f, 0.25f)

        private val COLOR_XY_POLYGON_CASUAL = Color.hsl(60.0f, 1.0f, 0.5f)  // полупрозрачный жёлтый
        private val COLOR_XY_POLYGON_ACTUAL = Color.hsl(30.0f, 1.0f, 0.5f)  // полупрозрачный оранжевый
        val COLOR_XY_POLYGON_BORDER: Color = Color.hsl(0.0f, 1.0f, 0.5f)    // красный
    }

    protected var xyCanvasWidth: Float by mutableFloatStateOf(0.0f)
    protected var xyCanvasHeight: Float by mutableFloatStateOf(0.0f)

    private val xyElements = mutableStateListOf<List<XyElementData>>()

//    private val xyTooltipVisible = mutableStateOf(false)
//    private val xyTooltipText = mutableStateOf("")
//    private val xyTooltipLeft = mutableStateOf(0.px)
//    private val xyTooltipTop = mutableStateOf(0.px)

    protected var refreshInterval: Int by mutableIntStateOf(0)

    val minXyScale: Int = elementConfigs.minByOrNull { (_, value) -> value.scaleMin }!!.value.scaleMin
    val maxXyScale: Int = elementConfigs.maxByOrNull { (_, value) -> value.scaleMax }!!.value.scaleMax

    protected var xyViewCoord: XyViewCoord = XyViewCoord(1, 0, 0, 1, 1)

    protected var screenOffsetX: Float by mutableFloatStateOf(0.0f)
    protected var screenOffsetY: Float by mutableFloatStateOf(0.0f)

//    private var xyTooltipOffTime = 0.0

    @Composable
    fun getXyElementTemplate(withInteractive: Boolean) {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        val textMeasurer = rememberTextMeasurer()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    xyCanvasWidth = size.width.toFloat()
                    xyCanvasHeight = size.height.toFloat()
                }
                .offset(
                    x = with(density) { screenOffsetX.toDp() },
                    y = with(density) { screenOffsetY.toDp() },
                )
                .clipToBounds()
                .onPointerEvents(
                    withInteractive = withInteractive,
                    onPointerDown = { pointerInputChange -> onPointerDown(pointerInputChange) },
                    onPointerUp = { pointerInputChange -> onPointerUp(pointerInputChange) },
                    onDragStart = { offset -> onDragStart(offset) },
                    onDrag = { pointerInputChange, offset -> onDrag(pointerInputChange, offset) },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                ),
        ) {
            for (alElement in xyElements) {
                for (element in alElement) {
                    if (element.type == XyElementDataType.IMAGE) {
                        KamelImage(
                            modifier = Modifier
                                .size(
                                    width = with(density) { element.width.toDp() },
                                    height = with(density) { element.height.toDp() },
                                )
                                .offset(
                                    x = with(density) { element.x.toDp() },
                                    y = with(density) { element.y.toDp() },
                                ),
                            resource = asyncPainterResource(data = element.url),
                            contentDescription = null,
                        )
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                for (alElement in xyElements) {
                    for (element in alElement) {
                        when (element.type) {
                            XyElementDataType.ARC -> {
                                element.fillColor?.let {
                                    drawArc(
                                        topLeft = Offset(element.x - element.radius, element.y - element.radius),
                                        size = Size(element.radius * 2, element.radius * 2),
                                        startAngle = element.startAngle,
                                        sweepAngle = element.sweepAngle,
                                        useCenter = true,
                                        color = element.fillColor,
                                        alpha = element.alpha,
                                        style = Fill
                                    )
                                }
                                element.strokeColor?.let {
                                    drawArc(
                                        topLeft = Offset(element.x - element.radius, element.y - element.radius),
                                        size = Size(element.radius * 2, element.radius * 2),
                                        startAngle = element.startAngle,
                                        sweepAngle = element.sweepAngle,
                                        useCenter = false,
                                        color = element.strokeColor,
                                        alpha = element.alpha,
                                        style = getElementStrokeStyle(element),
                                    )
                                }
                            }

                            XyElementDataType.CIRCLE -> {
                                drawCircleOnCanvas(
                                    drawScope = this,
                                    x = element.x,
                                    y = element.y,
                                    radius = element.radius,
                                    fillAlpha = element.alpha,
                                    fillColor = element.fillColor,
                                    strokeColor = element.strokeColor,
                                    strokeAlpha = 1.0f,
                                    strokeStyle = getElementStrokeStyle(element),
                                )
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
                            }

                            XyElementDataType.ELLIPSE -> {
                                element.fillColor?.let {
                                    drawOval(
                                        topLeft = Offset(element.x - element.rx, element.y - element.ry),
                                        size = Size(element.rx * 2, element.ry * 2),
                                        color = element.fillColor,
                                        alpha = element.alpha,
                                        style = Fill
                                    )
                                }
                                element.strokeColor?.let {
                                    drawOval(
                                        topLeft = Offset(element.x - element.rx, element.y - element.ry),
                                        size = Size(element.rx * 2, element.ry * 2),
                                        color = element.strokeColor,
                                        alpha = element.alpha,
                                        style = getElementStrokeStyle(element),
                                    )
                                }
                            }
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }

                            XyElementDataType.IMAGE -> {
                                //--- Сделано через KamelImage в нижележащем слое.
                                //--- Compose-resource пока не готов нормально грузить карты со внешнего урла.
                                //element.imageBitmap.value?.let { imageBitmap ->
                                //    drawImage(
                                //        srcOffset = IntOffset(element.x.toInt(), element.y.toInt()),
                                //        srcSize = IntSize(element.width.toInt(), element.height.toInt()),
                                //        image = imageBitmap,
                                //    )
                                //}
                            }

                            XyElementDataType.LINE -> {
                                drawLine(
                                    start = Offset(element.x1, element.y1),
                                    end = Offset(element.x2, element.y2),
                                    color = element.strokeColor ?: Color.Black,
                                    alpha = element.alpha,
                                    strokeWidth = element.strokeWidth ?: 1.0f,    // 2.dp.toPx()
                                    pathEffect = if (element.isSelected) {
                                        element.strokeDash?.let {
                                            PathEffect.dashPathEffect(element.strokeDash)
                                        }
                                    } else {
                                        null
                                    },
                                )
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
                            }

                            XyElementDataType.POLY -> {
                                drawPathOnCanvas(
                                    drawScope = this,
                                    path = element.getPath(),
                                    fillColor = element.fillColor,
                                    fillAlpha = element.alpha,
                                    strokeColor = if (element.isSelected) {
                                        COLOR_XY_POLYGON_BORDER
                                    } else {
                                        element.strokeColor
                                    },
                                    strokeAlpha = 1.0f,
                                    strokeStyle = getElementStrokeStyle(element),
                                )
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
                            }

                            XyElementDataType.RECT -> {
                                drawRectOnCanvas(
                                    drawScope = this,
                                    x = element.x,
                                    y = element.y,
                                    width = element.width,
                                    height = element.height,
                                    fillColor = element.fillColor,
                                    fillAlpha = element.alpha,
                                    strokeColor = element.strokeColor,
                                    strokeAlpha = 1.0f,
                                    strokeStyle = getElementStrokeStyle(element),
                                )
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
                            }

                            XyElementDataType.TEXT -> {
                                if (element.isVisible) {
                                    drawTextOnCanvas(
                                        drawScope = this,
                                        scaleKoef = root.scaleKoef,
                                        canvasWidth = xyCanvasWidth,
                                        canvasHeight = xyCanvasHeight,
                                        textMeasurer = textMeasurer,
                                        text = element.text,
                                        fontSize = element.textFontSize,
                                        textIsBold = element.textIsBold,
                                        x = element.x,
                                        y = element.y,
                                        textLimitWidth = element.textLimitWidth,
                                        textLimitHeight = element.textLimitHeight,
                                        rotateDegree = null,
                                        fillColor = element.fillColor,
                                        fillAlpha = element.alpha,
                                        strokeColor = element.strokeColor,
                                        strokeWidth = element.strokeWidth,
                                        textAnchor = element.textAnchor,
                                        textColor = element.textColor ?: Color.Black,
                                    )
                                }
//                                    onMouseEnter { syntheticMouseEvent ->
//                                        onXyMouseOver(syntheticMouseEvent, element)
//                                    }
//                                    onMouseLeave {
//                                        onXyMouseOut()
//                                    }
//                                    //--- поскольку нажатия на тексты могут быть срочными/неотложными действиями операторов/диспетчеров,
//                                    //--- то отрабатываем их независимо от режима обновления экрана и режима включенности интерактива
//                                    //if (refreshInterval.value == 0 && withInteractive) {
//                                    onMouseDown { syntheticMouseEvent ->
//                                        if (!element.isReadOnly) {
//                                            onXyTextPressed(syntheticMouseEvent, element)
//                                        }
//                                        syntheticMouseEvent.preventDefault()
//                                    }
//                                    onTouchStart { syntheticTouchEvent ->
//                                        if (!element.isReadOnly) {
//                                            onXyTextPressed(syntheticTouchEvent, element)
//                                        }
//                                        syntheticTouchEvent.preventDefault()
//                                    }
                            }
                        }
                    }
                }

                //--- for adding specific XY-elements
                addSpecificXy(this)
            }
        }

//        if (xyTooltipVisible.value) {
//            Div(
//                attrs = {
//                    style {
//                        position(Position.Absolute)
//                        left(xyTooltipLeft.value)
//                        top(xyTooltipTop.value)
//                        color(COLOR_MAIN_TEXT)
//                        backgroundColor(COLOR_XY_LABEL_BACK)
//                        setBorder(color = COLOR_XY_LABEL_BORDER, radius = styleButtonBorderRadius)
//                        setPaddings(arrStyleControlTooltipPadding)
//                        userSelect("none")
//                    }
//                }
//            ) {
//                getMultilineText(xyTooltipText.value)
//            }
//
//        }
    }

    protected open fun addSpecificXy(drawScope: DrawScope) {}

    protected fun getElementStrokeStyle(element: XyElementData): DrawStyle =
        Stroke(
            width = element.strokeWidth ?: 1.0f,    // 2.dp.toPx()
            pathEffect = if (element.isSelected) {
                element.strokeDash?.let {
                    PathEffect.dashPathEffect(element.strokeDash)
                }
            } else {
                null
            },
        )

    protected fun getXyCoordsDone(
        startExpandKoef: Float,
        isCentered: Boolean,
        curScale: Int,
        minCoord: XyPoint,
        maxCoord: XyPoint,
    ): XyViewCoord {

        var x1 = minCoord.x
        var y1 = minCoord.y
        var x2 = maxCoord.x
        var y2 = maxCoord.y

        val tmpW = x2 - x1
        val tmpH = y2 - y1
        //--- если пришли граничные координаты только одной точки,
        //--- то оставим текущий масштаб
        val scale = if (tmpW == 0 && tmpH == 0) {
            curScale
        } else {
            //--- прибавим по краям startExpandKoef, чтобы искомые элементы не тёрлись об края экрана
            x1 -= (tmpW * startExpandKoef).toInt()
            y1 -= (tmpH * startExpandKoef).toInt()
            x2 += (tmpW * startExpandKoef).toInt()
            y2 += (tmpH * startExpandKoef).toInt()
            //--- масштаб вычисляется исходя из размеров docView (аналогично zoomBox)
            calcXyScale(x1, y1, x2, y2)
        }

        if (isCentered) {
            val fullXyWidth = (xyCanvasWidth * scale / root.scaleKoef).toInt()
            val restXyWidth = fullXyWidth - (x2 - x1)
            x1 -= restXyWidth / 2
            x2 -= restXyWidth / 2

            val fullXyHeight = (xyCanvasHeight * scale / root.scaleKoef).toInt()
            val restXyHeight = fullXyHeight - (y2 - y1)
            y1 -= restXyHeight / 2
            y2 -= restXyHeight / 2
        }

        return XyViewCoord(scale, x1, y1, x2, y2)
    }

    protected fun calcXyScale(
        x1: Int,
        y1: Int,
        x2: Int,
        y2: Int
    ): Int = max((x2 - x1) * root.scaleKoef / xyCanvasWidth, (y2 - y1) * root.scaleKoef / xyCanvasHeight).roundToInt()

//    abstract fun xyRefreshView(aView: XyViewCoord?, withWait: Boolean, doAfterFullLoad: (SchemeControl) -> Unit)

    protected fun getXyViewCoord(aScale: Int, aCenterX: Int, aCenterY: Int): XyViewCoord {
        val vc = XyViewCoord()

        vc.scale = aScale

        val rw = (xyCanvasWidth * aScale / root.scaleKoef).roundToInt()
        val rh = (xyCanvasHeight * aScale / root.scaleKoef).roundToInt()

        vc.x1 = aCenterX - rw / 2
        vc.y1 = aCenterY - rh / 2
        //--- чтобы избежать неточностей из-за целочисленного деления нечетных чисел пополам,
        //--- правую/нижнюю границу получим в виде ( t1 + rw ), а не в виде ( newCenterX + rw / 2 )
        vc.x2 = vc.x1 + rw
        vc.y2 = vc.y1 + rh

        return vc
    }

    //--- проверка масштаба на минимальный/максимальный и на кратность степени двойки
    protected fun checkXyScale(isScaleAlign: Boolean, curScale: Int, newScale: Int, isAdaptive: Boolean): Int {

        if (newScale < minXyScale) {
            return minXyScale
        }
        if (newScale > maxXyScale) {
            return maxXyScale
        }

        //--- нужно ли выравнивание масштаба к степени двойки?
        if (isScaleAlign) {
            //--- ПРОТЕСТИРОВАНО: нельзя допускать наличие масштабов, не являющихся степенью двойки,
            //--- иначе при приведении (растягивании/сжимании) произвольного масштаба к выровненному (степени 2)
            //--- получается битмап-карта отвратного качества

            //--- адаптивный алгоритм - "докручивает" масштаб до ожидаемого пользователем
            if (isAdaptive) {
                //--- если идёт процесс увеличения масштаба (удаление от пользователя),
                //--- то поможем ему - округлим масштаб в бОльшую сторону
                if (newScale >= curScale) {
                    var scale = minXyScale
                    while (scale <= maxXyScale) {
                        if (newScale <= scale) {
                            return scale
                        }
                        scale *= 2
                    }
                }
                //--- иначе (если идёт процесс уменьшения масштаба - приближение к пользователю),
                //--- то поможем ему - округлим масштаб в меньшую сторону
                else {
                    var scale = maxXyScale
                    while (scale >= minXyScale) {
                        if (newScale >= scale) {
                            return scale
                        }
                        scale /= 2
                    }
                }
            }
            //--- обычный алгоритм - просто даёт больший или равный масштаб, чтобы всё гарантированно уместилось
            else {
                var scale = minXyScale
                while (scale <= maxXyScale) {
                    if (newScale <= scale) {
                        return scale
                    }
                    scale *= 2
                }
            }
        } else {
            return newScale
        }
        //--- такого быть не должно, но всё-таки для проверки вернём 0, чтобы получить деление на 0
        return 0   //XyConfig.MAX_SCALE;
    }

    protected fun readXyElements(alElement: List<XyElement>) {
        val hmLayer = mutableMapOf<Int, MutableList<XyElementData>>()
        alElement.forEach { element ->
            elementConfigs[element.typeName]?.let { elementConfig ->
                val alLayer = hmLayer.getOrPut(elementConfig.layer) { mutableListOf() }
                readXyElementData(elementConfig, element, alLayer)
            }
        }

        xyElements.clear()
        xyElements.addAll(hmLayer.toList().sortedBy { it.first }.map { it.second })
    }

    private fun readXyElementData(
        elementConfig: XyElementConfig,
        element: XyElement,
        alLayer: MutableList<XyElementData>,
    ) {

        val lineWidth = element.lineWidth ?: 0
        val drawColor = element.drawColor?.let { Color(it) }
        val fillColor = element.fillColor?.let { Color(it) }

        when (elementConfig.clientType) {
            XyElementClientType.ARC -> {
                val p = element.alPoint.first()

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.ARC,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef),
                        y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef),
                        radius = element.radius / xyViewCoord.scale * root.scaleKoef,
                        startAngle = element.startAngle.toFloat(),
                        sweepAngle = element.sweepAngle.toFloat(),
                        fillColor = fillColor,
                        strokeColor = drawColor,
                        strokeWidth = lineWidth * root.scaleKoef,
                        strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                        isReadOnly = element.isReadOnly
                    )
                )
            }

            XyElementClientType.BITMAP -> {
                val p = element.alPoint.first()

                if (element.imageName.isBlank()) {
                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.RECT,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef),
                            y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef),
                            width = (element.imageWidth / xyViewCoord.scale * root.scaleKoef),
                            height = (element.imageHeight / xyViewCoord.scale * root.scaleKoef),
                            strokeColor = Color.hsl(0.0f, 0.0f, 0.5f), // gray
                            strokeWidth = 1.0f, //!!! root.scaleKoef,
                            strokeDash = floatArrayOf(root.scaleKoef * 2, root.scaleKoef * 2),
                            isReadOnly = element.isReadOnly,
                        )
                    )
                } else {
                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.IMAGE,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef),
                            y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef),
                            width = (element.imageWidth / xyViewCoord.scale * root.scaleKoef),
                            height = (element.imageHeight / xyViewCoord.scale * root.scaleKoef),
                            url = element.imageName,
                            isReadOnly = element.isReadOnly,
                        )
                    )
                }
            }

            XyElementClientType.ICON -> {
                val p = element.alPoint.first()

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.IMAGE,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = ((p.x - xyViewCoord.x1) / xyViewCoord.scale - element.calcAnchorXKoef() * element.imageWidth).toInt() * root.scaleKoef,
                        y = ((p.y - xyViewCoord.y1) / xyViewCoord.scale - element.calcAnchorYKoef() * element.imageHeight).toInt() * root.scaleKoef,
                        width = element.imageWidth * root.scaleKoef,
                        height = element.imageHeight * root.scaleKoef,
                        rotateDegree = element.rotateDegree.toFloat(),
                        url = element.imageName,
                        isReadOnly = element.isReadOnly
                    )
                )
            }

            XyElementClientType.MARKER -> {
                val p = element.alPoint.first()
                val x = (p.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef
                val y = (p.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef
                val halfX = element.markerSize * root.scaleKoef / 2
                val halfY = element.markerSize * root.scaleKoef / 2

                when (element.markerType) {

                    XyElement.MarkerType.ARROW -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLY,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                alPoints = mutableListOf(
                                    XyPoint(x - halfX / 2, y),
                                    XyPoint(x - halfX, y - halfY),
                                    XyPoint(x + halfX * 2, y),
                                    XyPoint(x - halfX, y + halfY),
                                ),
                                isClosed = true,
                                strokeColor = drawColor,
                                fillColor = fillColor,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateDegree = element.rotateDegree.toFloat(),
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.CIRCLE -> {
                        if (element.markerSize2 == 0 || element.markerSize == element.markerSize2) {
                            alLayer.add(
                                XyElementData(
                                    type = XyElementDataType.CIRCLE,
                                    elementId = element.elementId,
                                    objectId = element.objectId,
                                    x = x,
                                    y = y,
                                    radius = halfX,
                                    fillColor = fillColor,
                                    strokeColor = drawColor,
                                    strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                    strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                    rotateDegree = element.rotateDegree.toFloat(),
                                    isReadOnly = element.isReadOnly
                                )
                            )
                        } else {
                            alLayer.add(
                                XyElementData(
                                    type = XyElementDataType.ELLIPSE,
                                    elementId = element.elementId,
                                    objectId = element.objectId,
                                    x = x,
                                    y = y,
                                    rx = element.markerSize * root.scaleKoef / 2,
                                    ry = element.markerSize2 * root.scaleKoef / 2,
                                    strokeColor = drawColor,
                                    fillColor = fillColor,
                                    strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                    strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                    rotateDegree = element.rotateDegree.toFloat(),
                                    isReadOnly = element.isReadOnly
                                )
                            )
                        }
                    }

                    /* временно отключим как неиспользуемое и нереализуемое только одним path-ом
                    XyElement.MarkerType.CROSS -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLY,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                alPoints = mutableListOf(
                                    XyPoint(x - halfX, y - halfY),
                                    XyPoint(x + halfX, y + halfY),
                                    XyPoint(x + halfX, y - halfY),
                                    XyPoint(x - halfX, y + halfY),
                                ),
                                strokeColor = drawColor,
                                fillColor = null,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateAngle = element.rotateDegree.toFloat(),
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }
                    */
                    XyElement.MarkerType.DIAMOND -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLY,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                alPoints = mutableListOf(
                                    XyPoint(x, y - halfY),
                                    XyPoint(x + halfX, y),
                                    XyPoint(x, y + halfY),
                                    XyPoint(x - halfX, y),
                                ),
                                isClosed = true,
                                strokeColor = drawColor,
                                fillColor = fillColor,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateDegree = element.rotateDegree.toFloat(),
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    /* временно отключим как неиспользуемое и нереализуемое только одним path-ом
                    XyElement.MarkerType.PLUS -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLY,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                alPoints = mutableListOf(
                                    XyPoint(x, y - halfY),
                                    XyPoint(x, y + halfY),
                                    XyPoint(x - halfX, y),
                                    XyPoint(x + halfX, y),
                                ),
                                strokeColor = drawColor,
                                fillColor = null,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateAngle = element.rotateDegree.toFloat(),
                                tooltip = element.toolTipText,
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }
                    */

                    XyElement.MarkerType.SQUARE -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.RECT,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                x = x - halfX,
                                y = y - halfY,
                                width = 2 * halfX,
                                height = 2 * halfY,
                                strokeColor = drawColor,
                                fillColor = fillColor,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateDegree = element.rotateDegree.toFloat(),
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }

                    XyElement.MarkerType.TRIANGLE -> {
                        alLayer.add(
                            XyElementData(
                                type = XyElementDataType.POLY,
                                elementId = element.elementId,
                                objectId = element.objectId,
                                alPoints = mutableListOf(
                                    XyPoint(x, y - halfY),
                                    XyPoint(x + halfX, y + halfY),
                                    XyPoint(x - halfX, y + halfY),
                                ),
                                isClosed = true,
                                strokeColor = drawColor,
                                fillColor = fillColor,
                                strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                                strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                                rotateDegree = element.rotateDegree.toFloat(),
                                isReadOnly = element.isReadOnly
                            )
                        )
                    }
                }
            }

            XyElementClientType.POLY -> {
//!!!                val style: StyleScope.() -> Unit = if (!element.isReadOnly && element.isClosed) {
//                    {
//                        cursor("pointer")
//                    }
//                } else {
//                    {}
//                }
                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.POLY,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        alPoints = element.alPoint.map {
                            XyPoint(
                                ((it.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef).roundToInt(),
                                ((it.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef).roundToInt(),
                            )
                        }.toMutableList(),
                        isClosed = element.isClosed,
                        strokeColor = drawColor,
                        fillColor = fillColor,
                        strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                        strokeDash = floatArrayOf(root.scaleKoef * lineWidth * 2, root.scaleKoef * lineWidth * 2),
                        isReadOnly = element.isReadOnly,
                        dialogQuestion = element.dialogQuestion,
                    )
                )
            }

            XyElementClientType.TEXT -> {
                val p = element.alPoint.first()

                val x = (p.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef
                val y = (p.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef

                val limitWidth = if (element.limitWidth == 0) {
                    null
                } else {
                    element.limitWidth / xyViewCoord.scale * root.scaleKoef
                }
                val limitHeight = if (element.limitHeight == 0) {
                    null
                } else {
                    element.limitHeight / xyViewCoord.scale * root.scaleKoef
                }

                val textAnchor = when (element.anchorY) {
                    XyElement.Anchor.LT -> {
                        when (element.anchorX) {
                            XyElement.Anchor.LT -> Alignment.TopStart
                            XyElement.Anchor.CC -> Alignment.TopCenter
                            XyElement.Anchor.RB -> Alignment.TopEnd
                        }
                    }

                    XyElement.Anchor.CC -> {
                        when (element.anchorX) {
                            XyElement.Anchor.LT -> Alignment.CenterStart
                            XyElement.Anchor.CC -> Alignment.Center
                            XyElement.Anchor.RB -> Alignment.CenterEnd
                        }
                    }

                    XyElement.Anchor.RB -> {
                        when (element.anchorX) {
                            XyElement.Anchor.LT -> Alignment.BottomStart
                            XyElement.Anchor.CC -> Alignment.BottomCenter
                            XyElement.Anchor.RB -> Alignment.BottomEnd
                        }
                    }
                }

                //--- сбор неизменяемых val-переменных для передачи в лямбду
//!!!                val fontSize = COMMON_FONT_SIZE * element.fontSize / iCoreAppContainer.BASE_FONT_SIZE
//                val isPointerCursor = !element.isReadOnly

                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.TEXT,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        x = x,
                        y = y,
                        strokeColor = element.drawColor?.let { Color(it) },
                        strokeWidth = (element.lineWidth ?: 0) * root.scaleKoef,
                        fillColor = element.fillColor?.let { Color(it) },
                        text = element.text,
                        textLimitWidth = limitWidth,
                        textLimitHeight = limitHeight,
                        textAnchor = textAnchor,
                        textColor = Color(element.textColor),
                        textFontSize = element.fontSize,
                        textIsBold = element.isFontBold,
                        isReadOnly = element.isReadOnly,
                        isVisible = true,
                        dialogQuestion = element.dialogQuestion,
                    )
                )
            }

            XyElementClientType.TRACE -> {
                for (i in 0 until element.alPoint.size - 1) {
                    val p1 = element.alPoint[i]
                    val p2 = element.alPoint[i + 1]

                    alLayer.add(
                        XyElementData(
                            type = XyElementDataType.LINE,
                            elementId = element.elementId,
                            objectId = element.objectId,
                            x1 = (p1.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef,
                            y1 = (p1.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef,
                            x2 = (p2.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef,
                            y2 = (p2.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef,
                            strokeColor = Color(element.drawColors[i]),
                            strokeWidth = max(1.0f, lineWidth * root.scaleKoef),
                            isReadOnly = element.isReadOnly,
                        )
                    )
                }
            }

            XyElementClientType.ZONE -> {
                alLayer.add(
                    XyElementData(
                        type = XyElementDataType.POLY,
                        elementId = element.elementId,
                        objectId = element.objectId,
                        alPoints = element.alPoint.map {
                            XyPoint(
                                ((it.x - xyViewCoord.x1) / xyViewCoord.scale * root.scaleKoef).roundToInt(),
                                ((it.y - xyViewCoord.y1) / xyViewCoord.scale * root.scaleKoef).roundToInt(),
                            )
                        }.toMutableList(),
                        isClosed = true,
                        strokeColor = COLOR_XY_POLYGON_ACTUAL,
                        fillColor = COLOR_XY_POLYGON_ACTUAL,
                        alpha = 0.25f,
                        strokeWidth = 2 * root.scaleKoef,
                        strokeDash = floatArrayOf(root.scaleKoef * 2 /*lineWidth*/ * 2, root.scaleKoef * 2 /*lineWidth*/ * 2),
                        isReadOnly = element.isReadOnly,
                        isEditablePoint = !element.isReadOnly,
                        isMoveable = !element.isReadOnly,
                    )
                )
            }
        }
    }

    protected fun xyDeselectAll() {
        xyElements.forEach { alElement ->
            alElement.forEach { xyElement ->
                xyElement.isSelected = false
            }
        }
    }

    protected abstract fun onPointerDown(pointerInputChange: PointerInputChange)
    protected abstract fun onPointerUp(pointerInputChange: PointerInputChange)
    protected abstract fun onDragStart(offset: Offset)
    protected abstract fun onDrag(pointerInputChange: PointerInputChange, dragAmount: Offset)
    protected abstract suspend fun onDragEnd()
    protected abstract fun onDragCancel()

    protected fun getXyClickRect(mouseX: Int, mouseY: Int): XyRect = XyRect(
        aX = mouseX - MIN_USER_RECT_SIZE / 2,
        aY = mouseY - MIN_USER_RECT_SIZE / 2,
        aWidth = MIN_USER_RECT_SIZE,
        aHeight = MIN_USER_RECT_SIZE
    )

    protected fun getXyElementList(rect: XyRect, isCollectEditableOnly: Boolean): List<XyElementData> =
        xyElements.flatten().filter { xyElement ->
            isCollectEditableOnly.xor(xyElement.isReadOnly) && xyElement.isIntersects(rect)
        }.asReversed()

}

/*

    protected fun getXyEmptyElementData(elementConfig: XyElementConfig): XyElementData? {
        var result: XyElementData? = null

        when (elementConfig.clientType) {
            XyElementClientType.BITMAP -> {
            }

            XyElementClientType.ICON -> {
            }

            XyElementClientType.MARKER -> {
            }

            XyElementClientType.POLY -> {
            }

            XyElementClientType.SVG_TEXT -> {
            }

            XyElementClientType.HTML_TEXT -> {
            }

            XyElementClientType.TRACE -> {
            }

            XyElementClientType.ZONE -> {
                result = XyElementData(
                    type = XyElementDataType.POLYGON,
                    elementId = -getRandomInt(),
                    objectId = 0,
                    arrPoints = mutableStateOf(emptyArray()),
                    stroke = COLOR_XY_POLYGON_ACTUAL,
                    fill = COLOR_XY_POLYGON_ACTUAL,
                    strokeWidth = (2 * root.scaleKoef).roundToInt(),
                    strokeDash = "${root.scaleKoef * 2 /*lineWidth*/ * 2},${root.scaleKoef * 2 /*lineWidth*/ * 2}",
                    tooltip = "",
                    isReadOnly = false,
                    alPoint = mutableListOf(),
                    isEditablePoint = true,
                    isMoveable = true,
                    //--- при добавлении сразу выбранный
                    isSelected = true,
                    //--- данные для добавления на сервере
                    typeName = "mms_zone",
                    alAddInfo = listOf(
                        Pair("zone_name") {
                            (window.prompt("Введите наименование геозоны")?.trim() ?: "").ifEmpty { "-" }
                        },
                        Pair("zone_descr") {
                            (window.prompt("Введите описание геозоны")?.trim() ?: "").ifEmpty { "-" }
                        }
                    )
                )
            }
        }

        return result
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected open fun onXyMouseOver(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData) {
        if (!xyElement.tooltip.isNullOrBlank()) {
            val (tooltipX, tooltipY) = getGraphixAndXyTooltipCoord(syntheticMouseEvent.clientX, syntheticMouseEvent.clientY)

            xyTooltipVisible.value = true
            xyTooltipText.value = xyElement.tooltip
            xyTooltipLeft.value = tooltipX.px
            xyTooltipTop.value = tooltipY.px
            xyTooltipOffTime = Date.now() + 3000
        } else {
            xyTooltipVisible.value = false
        }
    }

    private fun onXyMouseOut() {
        //--- через 3 сек выключить тултип, если не было других активаций тултипов
        //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
        //--- причём после ухода с графика других mouseleave не вызывается.
        setGraphicAndXyTooltipOffTimeout(xyTooltipOffTime, xyTooltipVisible)
    }

    protected abstract fun onXyMouseWheel(syntheticWheelEvent: SyntheticWheelEvent)
    protected abstract fun onXyTextPressed(syntheticMouseEvent: SyntheticMouseEvent, xyElement: XyElementData)
    protected abstract fun onXyTextPressed(syntheticTouchEvent: SyntheticTouchEvent, xyElement: XyElementData)

}

//--- Возвращает список выбранных элементов
//fun getXySelectedElementList( that: dynamic ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            if( xyElement.itSelected )
//                alResult.add( xyElement )
//        }
//    }
//    return alResult.asReversed()
//}

//fun getXyElementList( that: dynamic, x: Int, y: Int ): List<XyElementData> {
//    val arrXyElement = that.arrXyElement.unsafeCast<Array<Array<XyElementData>>>()
//    val alResult = mutableListOf<XyElementData>()
//    arrXyElement.forEach { arrXyElementIn ->
//        arrXyElementIn.forEach { xyElement ->
//            //--- небольшой хак: список элементов нужен только для интерактива, поэтмоу прежде чем тратить время на проверки геометрии - проверяем, а надо ли вообще проверять
//            if( !xyElement.itReadOnly && xyElement.isContains( x, y ))
//                alResult.add( xyElement )
//        }
//    }
//
//    return alResult.asReversed()
//}

 */

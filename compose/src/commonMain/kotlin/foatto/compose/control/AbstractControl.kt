package foatto.compose.control

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import foatto.compose.colorHeader
import foatto.compose.colorTextButton
import foatto.core.model.AppAction
import foatto.core.model.response.HeaderData
import kotlin.math.max

abstract class AbstractControl(
    protected val tabId: Int,
) {

    companion object {
        const val MIN_USER_RECT_SIZE: Float = 8.0f
    }

    var headerData: HeaderData? by mutableStateOf(null)

    @Composable
    abstract fun Body()

    abstract suspend fun start()

    @Composable
    protected fun getHeader(
        call: (action: AppAction) -> Unit = {},
    ) {
        headerData?.let { headerData ->
            Row(
                modifier = Modifier.fillMaxWidth().background(colorHeader),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
//                    borderTop(
//                        width = if (!styleIsNarrowScreen) 0.px else 1.px,
//                        lineStyle = LineStyle.Solid,
//                        color = colorMainBorder,
//                    )
//                    if (withBottomBorder) {
//                        borderBottom(width = 1.px, lineStyle = LineStyle.Solid, color = colorMainBorder)
//                    }
            ) {
                for (titleData in headerData.titles) {
                    titleData.action?.let { action ->
                        Button(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            shape = RoundedCornerShape(0.dp),   // чтобы образовалась почти сплошная полоса кнопок
                            colors = colorTextButton ?: ButtonDefaults.buttonColors(),
                            onClick = {
                                call(action)
                            },
//                                setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                        ) {
                            Text(titleData.text)
                        }
                    } ?: run {
                        Text(
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp),
                            text = titleData.text,
                            fontWeight = if (titleData.isBold) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                        )
                    }
                }
            }
            headerData.rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(colorHeader),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text(modifier = Modifier.align(Alignment.End), text = label)
                    }
                    Text(text = " : ")
                    Column(
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Text(modifier = Modifier.align(Alignment.Start), text = value)
                    }
                }
            }
        }
    }

    protected fun <T> addCellToGrid(
        alGridRows: SnapshotStateList<MutableList<T?>>,
        maxColCount: Int,
        row: Int,
        col: Int,
        cell: T,
    ) {
        val alGridRow = if (row < alGridRows.size) {
            alGridRows[row]
        } else {
            repeat(row - alGridRows.lastIndex) {
                alGridRows.add(mutableListOf())
            }
            alGridRows.last()
        }
        val maxCol = max(maxColCount, col)
        repeat(maxCol - alGridRow.lastIndex) {
            alGridRow.add(null)
        }
        alGridRow[col] = cell
    }

    protected fun drawCircleOnCanvas(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        radius: Float,
        fillColor: Color?,
        fillAlpha: Float,
        strokeColor: Color?,
        strokeAlpha: Float,
        strokeStyle: DrawStyle?,
    ) {
        drawScope.apply {
            fillColor?.let {
                drawCircle(
                    center = Offset(x, y),
                    radius = radius,
                    color = fillColor,
                    alpha = fillAlpha,
                    style = Fill,
                )
            }
            if (strokeColor != null && strokeStyle != null) {
                drawCircle(
                    center = Offset(x, y),
                    radius = radius,
                    color = strokeColor,
                    alpha = strokeAlpha,
                    style = strokeStyle
                )
            }
        }
    }

    protected fun drawPathOnCanvas(
        drawScope: DrawScope,
        path: Path,
        fillColor: Color?,
        fillAlpha: Float,
        strokeColor: Color?,
        strokeAlpha: Float,
        strokeStyle: DrawStyle?,
    ) {
        drawScope.apply {
            fillColor?.let {
                drawPath(
                    path = path,
                    color = fillColor,
                    alpha = fillAlpha,
                    style = Fill,
                )
            }
            if (strokeColor != null && strokeStyle != null) {
                drawPath(
                    path = path,
                    color = strokeColor,
                    alpha = strokeAlpha,
                    style = strokeStyle,
                )
            }
        }
    }

    protected fun drawRectOnCanvas(
        drawScope: DrawScope,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fillColor: Color?,
        fillAlpha: Float,
        strokeColor: Color?,
        strokeAlpha: Float,
        strokeStyle: DrawStyle?,
    ) {
        drawScope.apply {
            fillColor?.let {
                drawRect(
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    color = fillColor,
                    alpha = fillAlpha,
                    style = Fill,
                )
            }
            if (strokeColor != null && strokeStyle != null) {
                drawRect(
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    color = strokeColor,
                    alpha = strokeAlpha,
                    style = strokeStyle,
                )
            }
        }
//        rx(element.rx)
//        ry(element.ry)
    }

    protected fun drawTextOnCanvas(
        drawScope: DrawScope,
        scaleKoef: Float,
        canvasWidth: Float,
        canvasHeight: Float,
        textMeasurer: TextMeasurer,
        text: String,
        fontSize: Int,
        textIsBold: Boolean,
        x: Float,
        y: Float,
        textLimitWidth: Float?,
        textLimitHeight: Float?,
        rotateDegree: Float?,
        fillColor: Color?,
        fillAlpha: Float,
        strokeColor: Color?,
        strokeWidth: Float?,
        textAnchor: Alignment,
        textColor: Color,
    ) {
        val isTextSizeLimited = textLimitWidth != null && textLimitHeight != null
        val isBordered = strokeColor != null && (strokeWidth ?: 0.0f) > 0
        val borderWidth = strokeWidth ?: (1.0f * scaleKoef)
        val anchor = (textAnchor as? BiasAlignment) ?: Alignment.TopStart as BiasAlignment

        //--- constant offset from text anchor point
        val (constOffsetX, constOffsetY) = if (isTextSizeLimited) {
            0.0f to 0.0f
        } else {
            1.0f * scaleKoef to 1.0f * scaleKoef
        }
        val (paddingX, paddingY) = if (isBordered || fillColor != null) {
            borderWidth + 2.0f * scaleKoef to borderWidth + 1.0f * scaleKoef
        } else {
            0.0f to 0.0f
        }

        val textStyle = TextStyle(
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = if (textIsBold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
        val textSize = if (isTextSizeLimited) {
            Size(textLimitWidth!! - paddingX * 2, textLimitHeight!! - paddingY * 2)
        } else {
            textMeasurer.measure(
                text = text,
                style = textStyle,
                softWrap = false,
            ).size.toSize()
        }

        val startX = x - (anchor.horizontalBias + 1) * (textSize.width / 2 + paddingX) - anchor.horizontalBias * constOffsetX
        val topY = y - (anchor.verticalBias + 1) * (textSize.height / 2 + paddingY) - anchor.verticalBias * constOffsetY

        drawScope.apply {
            drawRectOnCanvas(
                drawScope = this,
                x = startX,
                y = topY,
                width = textSize.width + 2 * paddingX,
                height = textSize.height + 2 * paddingY,
                fillColor = fillColor,
                fillAlpha = fillAlpha,
                strokeColor = strokeColor,
                strokeAlpha = 1.0f,
                strokeStyle = Stroke(width = borderWidth),
            )
            rotate(
                degrees = rotateDegree ?: 0.0f,
                pivot = Offset(x, y),
            ) {
                text.split('\n').forEachIndexed { index, textLine ->
                    try {
                        drawText(
                            textMeasurer = textMeasurer,
                            topLeft = Offset(startX + paddingX, topY + paddingY + index * 16 * scaleKoef),
                            text = textLine,
//!!! BUG: выводится только первая строка многострочного текста
//                        text = text,
//!!! BUG: buildAnnotatedString через SpanStyle не выводит вторую и последующие строки друг под другом,
// но если убрать appendLine() - выводит строки друг за другом в одну линию
//                        text = buildAnnotatedString {
//                            withStyle(
//                                style = SpanStyle(
//                                    color = textColor,
//                                ),
//                            ) {
//                                append(text)
//                                text.split('\n').forEachIndexed { index, textLine ->
//                                    if (index > 0) {
//                                        appendLine()
//                                    }
//                                    append(textLine)
//                                }
//                            }
//                        },
//!!! BUG: вариант через ParagraphStyle тоже не работает
//                        text = buildAnnotatedString {
//                            text.split('\n').forEach { textLine ->
//                                withStyle(
//                                    style = ParagraphStyle(),
//                                ) {
//                                    append(textLine)
//                                }
//                            }
//                        },
                            style = textStyle,
                            softWrap = false,
                            overflow = if (isTextSizeLimited) {
                                TextOverflow.Ellipsis
                            } else {
                                TextOverflow.Visible
                            },
                            size = if (isTextSizeLimited) {
                                textSize
                            } else {
                                Size.Unspecified
                            },
                        )
                    } catch (t: Throwable) {
//                        println("--- TEXT = $text")
//                        println("--- canvasWidth = $canvasWidth")
//                        println("--- canvasHeight = $canvasHeight")
//                        println("--- x = ${startX + paddingX}")
//                        println("--- y = ${topY + paddingY}")
//                        println(
//                            "--- size = ${
//                                if (isTextSizeLimited) {
//                                    textSize
//                                } else {
//                                    Size.Unspecified
//                                }
//                            }"
//                        )
//                        t.printStackTrace()
                    }

                }
            }
        }
    }

}

/*
    protected fun getGraphixAndXyTooltipCoord(x: Int, y: Int): Pair<Int, Int> =
        Pair(
            x - (0 * scaleKoef).roundToInt(),
            y - (32 * scaleKoef).roundToInt(),
        )

    protected fun setGraphicAndXyTooltipOffTimeout(tooltipOffTime: Double, tooltipVisible: MutableState<Boolean>) {
        //--- через 3 сек выключить тултип, если не было других активаций тултипов
        //--- причина: баг (?) в том, что mouseleave вызывается сразу после mouseenter,
        //--- причём после ухода с графика других mouseleave не вызывается.
        window.setTimeout({
            if (Date.now() > tooltipOffTime) {
                tooltipVisible.value = false
            }
        }, 3000)
    }
*/
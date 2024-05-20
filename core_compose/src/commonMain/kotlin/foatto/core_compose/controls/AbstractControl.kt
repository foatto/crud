package foatto.core_compose.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import foatto.core_compose.controls.models.TitleData
import kotlin.math.max

abstract class AbstractControl(
    protected val tabId: Int,
) {

    companion object {
        val MIN_USER_RECT_SIZE: Float = 8.0f
    }

    protected val alTitleData: SnapshotStateList<TitleData> = mutableStateListOf()

    @Composable
    abstract fun Body()

    abstract fun start()

    @Composable
    protected fun getTableAndFormHeader(
        withBottomBorder: Boolean,
        call: (url: String) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
//                    backgroundColor(getColorTableHeaderBack())
        ) {
            for (titleData in alTitleData) {
                if (titleData.url.isNotEmpty()) {
                    Button(
                        onClick = {
                            call(titleData.url)
                        },
//                                backgroundColor(getColorButtonBack())
//                                setBorder(color = getColorButtonBorder(), radius = styleButtonBorderRadius)
                    ) {
                        Text(titleData.text)
                    }
                } else {
                    Text(titleData.text)
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

    @Composable
    protected fun getChartAndXyHeader() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
//                    borderTop(
//                        width = (if (!styleIsNarrowScreen) {
//                            0
//                        } else {
//                            1
//                        }).px,
//                        lineStyle = LineStyle.Solid,
//                        color = colorMainBorder,
//                    )
//                    backgroundColor(getColorGraphicAndXyToolbarBack())
        ) {
            alTitleData.forEachIndexed { index, titleData ->
                Text(
                    text = titleData.text,
                    fontWeight = if (alTitleData.size > 1 && index == 0) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }
                )
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
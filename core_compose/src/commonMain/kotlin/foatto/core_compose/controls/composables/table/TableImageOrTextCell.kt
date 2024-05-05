package foatto.core_compose.controls.composables.table

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import foatto.core_compose.composables.ImageOrTextFromName
import foatto.core_compose.controls.models.table.TableCellDataClient
import foatto.core_compose.controls.models.table.TableGridData
import foatto.core_compose.styleOtherIconSize

@Composable
fun TableImageOrTextCell(
    gridData: TableGridData,
    cellData: TableCellDataClient,
    modifier: Modifier = Modifier,
) {
    if (cellData.icon.isNotEmpty()) {
        ImageOrTextFromName(cellData.icon, gridData.tooltip, styleOtherIconSize)
    } else if (cellData.image.isNotEmpty()) {
        ImageOrTextFromName(cellData.image, gridData.tooltip, styleOtherIconSize)
    } else {
        Text(
            modifier = modifier,
            text = cellData.text,
            color = gridData.textColor,
            fontWeight = if (gridData.isBoldText) {
                FontWeight.Bold
            } else {
                null
            },
        )
    }
}

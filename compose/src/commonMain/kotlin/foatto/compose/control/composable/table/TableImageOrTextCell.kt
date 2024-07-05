package foatto.compose.controls.composables.table

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import foatto.compose.composable.ImageOrTextFromName
import foatto.compose.control.model.table.cell.TableBaseCellClient
import foatto.compose.control.model.table.cell.TableCellDataClient
import foatto.compose.styleOtherIconSize

@Composable
fun TableImageOrTextCell(
    baseCellData: TableBaseCellClient,
    cellData: TableCellDataClient,
    modifier: Modifier = Modifier,
) {
    cellData.icon?.let {
        ImageOrTextFromName(cellData.icon, styleOtherIconSize)
    } ?: cellData.image?.let {
        ImageOrTextFromName(cellData.image, styleOtherIconSize)
    } ?: run {
        Text(
            modifier = modifier,
            text = cellData.text ?: "???",
            color = baseCellData.textColor,
            fontWeight = if (baseCellData.isBoldText) {
                FontWeight.Bold
            } else {
                null
            },
        )
    }
}

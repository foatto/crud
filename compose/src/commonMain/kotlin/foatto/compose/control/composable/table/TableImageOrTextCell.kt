package foatto.compose.control.composable.table

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import foatto.compose.composable.ImageOrTextFromNameControl
import foatto.compose.control.model.table.cell.TableBaseCellClient
import foatto.compose.control.model.table.cell.TableCellDataClient
import foatto.compose.styleOtherIconSize

@Composable
fun TableImageOrTextCell(
    baseCellData: TableBaseCellClient,
    cellData: TableCellDataClient,
    modifier: Modifier = Modifier,
) {
    ImageOrTextFromNameControl(
        name = cellData.name,
        iconSize = styleOtherIconSize,
        imageButton = { func ->
            FilledIconButton(
                // modifier = modifier, - несоразмерно увеличивает размер ячейки
                shape = RoundedCornerShape(0.dp),               // чтобы не срезало углы иконок
                colors = IconButtonDefaults.iconButtonColors(), // иконки должны быть на прозрачном фоне, т.к. не clickable
                onClick = {},
            ) {
                func()
            }
        },
        textButton = { caption ->
            Text(
                modifier = modifier,
                text = caption,
                color = baseCellData.textColor,
                fontWeight = if (baseCellData.isBoldText) {
                    FontWeight.Bold
                } else {
                    null
                },
            )
        }
    )
}

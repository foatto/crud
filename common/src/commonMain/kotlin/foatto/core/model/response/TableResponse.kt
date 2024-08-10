package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable
import foatto.core.model.response.table.AddActionButton
import foatto.core.model.response.table.ClientActionButton
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell

@Serializable
class TableResponse(
    val tabCaption: String,
    val alHeader: List<Pair<AppAction?, String>>,
    val findText: String,
    val alAddActionButton: List<AddActionButton>,
    val alServerActionButton: List<ServerActionButton>,
    val alClientActionButton: List<ClientActionButton>,
    val alColumnCaption: List<Pair<AppAction?, String>>,
    val alTableCell: List<TableBaseCell>,
    val alTableRowData: List<TableRowData>,
    val selectedRowNo: Int?,
    val alPageButton: List<Pair<AppAction?, String>>
)

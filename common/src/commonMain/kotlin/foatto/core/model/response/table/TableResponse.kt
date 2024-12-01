package foatto.core.model.response.table

import foatto.core.model.AppAction
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ServerActionButton
import kotlinx.serialization.Serializable
import foatto.core.model.response.table.cell.TableBaseCell

@Serializable
class TableResponse(
    val tabCaption: String,

    val headerData: HeaderData,

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

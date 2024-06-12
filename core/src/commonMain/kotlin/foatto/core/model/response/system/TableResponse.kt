package foatto.core.model.response.system

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable
import foatto.core.model.response.system.table.AddActionButton
import foatto.core.model.response.system.table.ClientActionButton
import foatto.core.model.response.system.table.ServerActionButton
import foatto.core.model.response.system.table.TableRowData
import foatto.core.model.response.system.table.cells.TableBaseCell

@Serializable
class TableResponse(
    val tabCaption: String,
    val alHeader: List<Pair<AppAction?, String>>,
    val selectorCancelAction: AppAction? = null,
    val findAction: AppAction? = null,
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

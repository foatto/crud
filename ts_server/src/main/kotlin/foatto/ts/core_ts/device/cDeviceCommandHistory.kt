package foatto.ts.core_ts.device

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.iData
import foatto.ts_core.app.CommandStatusCode

class cDeviceCommandHistory : cStandart() {

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mdch = model as mDeviceCommandHistory

        if ((hmColumnData[mdch.columnStatus] as DataComboBox).intValue == CommandStatusCode.DELETED) {
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
        }
    }

}
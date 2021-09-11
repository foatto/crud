package foatto.office

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataTime3Int
import foatto.core_server.app.server.data.iData
import java.time.LocalDate
import java.time.LocalDateTime

class cReminder : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val tn = renameTableName(hsTableRenameList, model.tableName)
        val m = model as mReminder

        return super.addSQLWhere(hsTableRenameList) +
            if (m.type != -1) {
                """
                    AND $tn.${m.columnType.getFieldName()} = ${m.type}
                """
            } else {
                ""
            }
    }

    override fun setTableGroupColumnStyle(hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell): TableCell {
        super.setTableGroupColumnStyle(hmColumnData, column, tci)

        val mr = model as mReminder
        if (column == mr.columnDate) {
            val localDate = (hmColumnData[mr.columnDate] as DataDate3Int).localDate
            localDate.plusDays(1)
            if (localDate.isBefore(LocalDate.now())) {
                tci.foreColorType = TableCellForeColorType.DEFINED
                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            }
        } else if (column == mr.columnTime) {
            val localDate = (hmColumnData[mr.columnDate] as DataDate3Int).localDate
            val localTime = (hmColumnData[mr.columnTime] as DataTime3Int).localTime
            val dateTime = LocalDateTime.of(localDate, localTime)
            if (dateTime.isBefore(LocalDateTime.now())) {
                tci.foreColorType = TableCellForeColorType.DEFINED
                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            }
        }
        return tci
    }

}

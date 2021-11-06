package foatto.office

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.iData
import java.time.ZonedDateTime

class cTask : cStandart() {

    private var toDay = ZonedDateTime.now()

    //--- можно добавлять поручение, только в активных исходящих поручениях
    //--- (в принципе, это можно отобразить через права доступа, но подстраховаться не помешает)
    override fun isAddEnabled(): Boolean = aliasConfig.alias == "office_task_out"

    override fun getTableRowIsReaded(valueID: Int?): Boolean {
        //--- проверка своей записи - если она не читана, то достаточно, можно переписку не проверять
        var isReaded = super.getTableRowIsReaded(valueID)

        if(isReaded && valueID != null) {
            //--- сколько всего сообщений в этой ветке обсуждения
            var rs = stm.executeQuery(" SELECT COUNT(*) FROM OFFICE_task_thread WHERE task_id = $valueID")
            val countAll = if(rs.next()) rs.getInt(1) else 0
            rs.close()

            //--- сколько прочитанных сообщений в этой ветке обсуждения
            rs = stm.executeQuery(
                " SELECT COUNT(*) FROM SYSTEM_new " +
                    " WHERE table_name = 'OFFICE_task_thread' " +
                    " AND user_id = ${userConfig.userId} " +
                    " AND row_id IN ( SELECT id FROM OFFICE_task_thread WHERE task_id = $valueID ) "
            )
            val countReaded = if(rs.next()) rs.getInt(1) else 0
            rs.close()

            //--- если кол-во прочитанных сообщений равно общему кол-ву сообщений в этой ветке,
            //--- то ничего нового там нет
            isReaded = countAll == countReaded
        }
        return isReaded
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val mt = model as mTask
        if(column == mt.columnTaskSubj) {
            //--- архивные записи как просроченные не окрашиваем - они там по сути все просроченные
            if(!model.isArchiveAlias) {
                val gc = (hmColumnData[mt.columnDate] as DataDate3Int).localDate.atStartOfDay(zoneId)
                if(gc.isBefore(toDay)) {
                    tci.foreColorType = TableCellForeColorType.DEFINED
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
            }
        }
    }
}

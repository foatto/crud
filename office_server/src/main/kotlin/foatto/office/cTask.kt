package foatto.office

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData
import java.time.ZonedDateTime

class cTask : cStandart() {

    private var toDay = ZonedDateTime.now()

    //--- можно добавлять поручение, только в активных исходящих поручениях
    //--- (в принципе, это можно отобразить через права доступа, но подстраховаться не помешает)
    override fun isAddEnabled(): Boolean = aliasConfig.name == "office_task_out"

    override fun getTableRowIsReaded(valueID: Int?): Boolean {
        //--- проверка своей записи - если она не читана, то достаточно, можно переписку не проверять
        var isReaded = super.getTableRowIsReaded(valueID)

        if (isReaded && valueID != null) {
            //--- сколько всего сообщений в этой ветке обсуждения
            var rs = conn.executeQuery(
                """
                    SELECT COUNT(*) FROM OFFICE_task_thread WHERE task_id = $valueID
                """
            )
            val countAll = if (rs.next()) {
                rs.getInt(1)
            } else {
                0
            }
            rs.close()

            //--- сколько прочитанных сообщений в этой ветке обсуждения
            rs = conn.executeQuery(
                """
                    SELECT COUNT(*) FROM SYSTEM_new
                    WHERE table_name = 'OFFICE_task_thread'  
                    AND user_id = ${userConfig.userId}  
                    AND row_id IN ( SELECT id FROM OFFICE_task_thread WHERE task_id = $valueID )
                """
            )
            val countReaded = if (rs.next()) {
                rs.getInt(1)
            } else {
                0
            }
            rs.close()

            //--- если кол-во прочитанных сообщений равно общему кол-ву сообщений в этой ветке,
            //--- то ничего нового там нет
            isReaded = countAll == countReaded
        }
        return isReaded
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mt = model as mTask
        if (column == mt.columnTaskSubj) {
            //--- архивные записи как просроченные не окрашиваем - они там по сути все просроченные
            if (!model.isArchiveAlias) {
                val gc = (hmColumnData[mt.columnDate] as DataDate3Int).localDate.atStartOfDay(zoneId)
                if (gc.isBefore(toDay)) {
                    tci.foreColorType = TableCellForeColorType.DEFINED
                    tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
                }
            }
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.preSave(id, hmColumnData)

        val mt = model as mTask
        (hmColumnData[mt.columnTaskLastUpdate] as DataDateTimeInt).setDateTime(getCurrentTimeInt())
    }

    override fun doAdd(alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>, hmOut: MutableMap<String, Any>): Pair<Int, String?> {
        val isTaskOwner = aliasConfig.name.startsWith("office_task_out")
        val m = model as mTask

        return if (isTaskOwner) {
            var result = Pair(0, null as String?)
            m.alColumnOtherUser.forEach { colOtherUser ->
                val otherUserId = (hmColumnData[colOtherUser] as DataInt).intValue
                //--- если этот исполнитель задан
                if (otherUserId != 0) {
                    //--- устанавливаем значение стандартного поля, чтобы воспользоваться процедурой стандартного добавления
                    (hmColumnData[m.columnOtherUser] as DataInt).intValue = otherUserId
                    //--- далее стандартное сохранение
                    result = super.doAdd(alColumnList, hmColumnData, hmOut)
                }
            }
            result
        } else {
            super.doAdd(alColumnList, hmColumnData, hmOut)
        }
    }
}

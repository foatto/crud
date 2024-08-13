package foatto.mms.core_mms

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataAbstractIntValue
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection
import foatto.util.getCurrentTimeInt

class cWorkShift : cMMSOneObjectParent() {

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        prepareAutoAddWorkShift(hmColumnData)
        prepareAutoCreateWorkShift(hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        prepareAutoAddWorkShift(hmColumnData)
        prepareAutoCreateWorkShift(hmColumnData)

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)

        val m = model as mWorkShift
        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue

        val rs = conn.executeQuery(" SELECT id FROM MMS_work_shift WHERE object_id = $objectId ")
        val isWorkShiftRest = rs.next()
        rs.close()

        //--- если рабочих смен не осталось, то обнулим флаг автосоздания,
        //--- т.к. не осталось шаблона для автосоздания
        if (!isWorkShiftRest) {
            conn.executeUpdate(" UPDATE MMS_object SET is_auto_work_shift = 0 WHERE id = $objectId ")
        }
    }

    private fun prepareAutoAddWorkShift(hmColumnData: Map<iColumn, iData>) {
        val m = model as mWorkShift

        val userId = (hmColumnData[model.columnUser!!] as DataAbstractIntValue).intValue
        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue

        val begTime = (hmColumnData[m.columnShiftBegDoc] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()
        val endTime = (hmColumnData[m.columnShiftEndDoc] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()

        if ((hmColumnData[m.columnIsAutoAddWorkShift] as DataBoolean).value && begTime < endTime) {
            autoAddWorkShift(conn, userId, objectId, begTime, endTime)
        }
    }

    private fun prepareAutoCreateWorkShift(hmColumnData: Map<iColumn, iData>) {
        val m = model as mWorkShift

        val userId = (hmColumnData[model.columnUser!!] as DataAbstractIntValue).intValue
        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue
        val isAutoCreateWorkShift = (hmColumnData[m.columnIsAutoCreateWorkShift] as DataBoolean).value

        conn.executeUpdate(" UPDATE MMS_object SET is_auto_work_shift = ${if (isAutoCreateWorkShift) 1 else 0} WHERE id = $objectId ")

        if (isAutoCreateWorkShift) {
            autoCreateWorkShift(conn, userId, objectId)
        }
    }

    companion object {

        fun autoAddWorkShift(conn: CoreAdvancedConnection, userId: Int, objectId: Int, begDT: Int, endDT: Int) {
            var begTime = begDT
            var endTime = endDT

            val workShiftDuration = endTime - begTime
            //--- автоматически создаём смены до настоящего времени
            while (endTime <= getCurrentTimeInt()) {
                begTime = endTime
                endTime += workShiftDuration

                //--- проверяем наличие уже существующей и пересекающейся по времени рабочей смены
                val rs = conn.executeQuery(
                    """
                        SELECT id 
                        FROM MMS_work_shift
                        WHERE object_id = $objectId 
                            AND $begTime < end_dt
                            AND $endTime > beg_dt
                    """
                )
                val isExists = rs.next()
                rs.close()

                if (!isExists) {
                    conn.executeUpdate(
                        StringBuilder(
                            " INSERT INTO MMS_work_shift ( "
                        )
                            .append(" id , user_id , object_id , beg_dt , end_dt , beg_dt_fact , end_dt_fact , worker_id , shift_no , run ) VALUES ( ")
                            .append(conn.getNextIntId("MMS_work_shift", "id")).append(" , ")
                            .append(userId).append(" , ").append(objectId).append(" , ")
                            //--- при автосоздании примем фактическое время == документальному времени
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(" 0 , '' , 0 ) ").toString()
                    )
                }
            }
        }

        fun autoCreateWorkShift(conn: CoreAdvancedConnection, userId: Int, objectId: Int): Int? {
            //--- найдем последнюю рабочую смену
            var begTime = 0
            var endTime = 0
            val rs = conn.executeQuery(" SELECT beg_dt , end_dt FROM MMS_work_shift WHERE object_id = $objectId ORDER BY end_dt DESC ")
            if (rs.next()) {
                begTime = rs.getInt(1)
                endTime = rs.getInt(2)
            }
            rs.close()
            //--- нашлась такая смена
            if (begTime != 0 && endTime != 0) {
                val workShiftDuration = endTime - begTime
                //--- автоматически создаём смены до настоящего времени
                while (endTime <= getCurrentTimeInt()) {
                    begTime = endTime
                    endTime += workShiftDuration

                    conn.executeUpdate(
                        StringBuilder(
                            " INSERT INTO MMS_work_shift ( "
                        )
                            .append(" id , user_id , object_id , beg_dt , end_dt , beg_dt_fact , end_dt_fact , worker_id , shift_no , run ) VALUES ( ")
                            .append(conn.getNextIntId("MMS_work_shift", "id")).append(" , ")
                            .append(userId).append(" , ").append(objectId).append(" , ")
                            //--- при автосоздании примем фактическое время == документальному времени
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(" 0 , '' , 0 ) ").toString()
                    )
                }
                return endTime
            } else {
                return null
            }
        }
    }
}

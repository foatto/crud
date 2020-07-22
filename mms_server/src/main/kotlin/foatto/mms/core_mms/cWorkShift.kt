package foatto.mms.core_mms

import foatto.core.util.getCurrentTimeInt
import foatto.sql.CoreAdvancedStatement
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataAbstractValue
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData

class cWorkShift : cMMSOneObjectParent() {

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        prepareAutoWorkShift(hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        prepareAutoWorkShift(hmColumnData)

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)

        val m = model as mWorkShift
        val objectID = (hmColumnData[m.columnObject] as DataInt).value

        val rs = stm.executeQuery( " SELECT id FROM MMS_work_shift WHERE object_id = $objectID " )
        val isWorkShiftRest = rs.next()
        rs.close()

        //--- если рабочих смен не осталось, то обнулим флаг автосоздания,
        //--- т.к. не осталось шаблона для автосоздания
        if(!isWorkShiftRest)
            stm.executeUpdate( " UPDATE MMS_object SET is_auto_work_shift = 0 WHERE id = $objectID " )
    }

    private fun prepareAutoWorkShift( hmColumnData: Map<iColumn, iData> ) {
        val m = model as mWorkShift

        val userID = ( hmColumnData[ model.columnUser!! ] as DataAbstractValue).value
        val objectID = (hmColumnData[m.columnObject] as DataInt).value
        val isAutoWorkShift = (hmColumnData[m.columnIsAutoWorkShift] as DataBoolean).value

        stm.executeUpdate( " UPDATE MMS_object SET is_auto_work_shift = ${if(isAutoWorkShift) 1 else 0} WHERE id = $objectID " )

        if(isAutoWorkShift) autoCreateWorkShift(stm, userID, objectID)
    }

    companion object {

        fun autoCreateWorkShift(stm: CoreAdvancedStatement, userID: Int, objectID: Int): Int? {
            //--- найдем последнюю рабочую смену
            var begTime = 0
            var endTime = 0
            val rs = stm.executeQuery(" SELECT beg_dt , end_dt FROM MMS_work_shift WHERE object_id = $objectID ORDER BY end_dt DESC ")
            if(rs.next()) {
                begTime = rs.getInt(1)
                endTime = rs.getInt(2)
            }
            rs.close()
            //--- нашлась такая смена
            if(begTime != 0 && endTime != 0) {
                val workShiftDuration = endTime - begTime
                //--- автоматически создаём смены до настоящего времени
                while(endTime <= getCurrentTimeInt()) {
                    begTime = endTime
                    endTime += workShiftDuration

                    stm.executeUpdate(
                        StringBuilder(
                            " INSERT INTO MMS_work_shift ( "
                        )
                            .append(" id , user_id , object_id , beg_dt , end_dt , beg_dt_fact , end_dt_fact , worker_id , shift_no , run ) VALUES ( ")
                            .append(stm.getNextID("MMS_work_shift", "id")).append(" , ")
                            .append(userID).append(" , ").append(objectID).append(" , ")
                            //--- при автосоздании примем фактическое время == документальному времени
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(begTime).append(" , ").append(endTime).append(" , ")
                            .append(" 0 , '' , 0 ) ")
                    )
                }
                return endTime
            }
            else
                return null
        }
    }
}

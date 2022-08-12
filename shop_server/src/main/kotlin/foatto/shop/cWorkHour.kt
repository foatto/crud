package foatto.shop

import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.iData
import java.time.LocalDate

class cWorkHour : cStandart() {

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val mwh = model as mWorkHour

        val currentWorkerId = (hmColumnData[mwh.columnWorker] as DataRadioButton).intValue

        val curDateData = hmColumnData[mwh.columnWorkDate] as DataDate3Int
        val curYear = curDateData.localDate.year
        val curMonth = curDateData.localDate.monthValue
        val curDay = curDateData.localDate.dayOfMonth
        val maxDaysInMonth = curDateData.localDate.month.length(curDateData.localDate.isLeapYear)

        val rs = stm.executeQuery(
            """ 
                SELECT COUNT(*) 
                FROM SHOP_work_hour 
                WHERE ye = $curYear
                AND mo = $curMonth
            """
        )
        rs.next()
        val rowCount = rs.getInt(1)
        rs.close()

        //--- если это единственная запись за этот месяц - то это первое создание и можно автоматически создать другие записи
        if (rowCount == 1) {
            val alWorkerId = (application as iShopApplication).alWorkHourUserId
            alWorkerId.forEachIndexed { workerIndex, sWorkerId ->
                val workerId = sWorkerId.toInt()

                for (day in 1..maxDaysInMonth) {
                    val workHour = when (LocalDate.of(curYear, curMonth, day).dayOfWeek.value) {
                        in 1..5 -> (application as iShopApplication).workHourInWorkDay?.toDoubleOrNull() ?: 9.0
                        6 -> (application as iShopApplication).workHourInHolyDay?.toDoubleOrNull() ?: 6.0
                        else -> 0.0
                    }
                    val hourTax = when (LocalDate.of(curYear, curMonth, day).dayOfWeek.value) {
                        in 1..5 -> (application as iShopApplication).alWorkDayHourTax[workerIndex].toDoubleOrNull() ?: 90.0
                        6 -> (application as iShopApplication).alHolyDayHourTax[workerIndex].toDoubleOrNull() ?: 135.0
                        else -> 0.0
                    }
                    //--- не будем дублировать уже созданную запись
                    if (workerId == currentWorkerId && day == curDay) {
                    } else if (workHour > 0.0) {
                        stm.executeUpdate(
                            """
                                INSERT INTO SHOP_work_hour ( id , worker_id , ye , mo , da , work_hour , hour_tax )
                                VALUES ( 
                                    ${stm.getNextIntId("SHOP_work_hour", "id")} ,
                                    $workerId , $curYear , $curMonth , $day , $workHour , $hourTax                                                                        
                                )
                            """
                        )
                    }
                }
            }
        }

        return postURL
    }

}
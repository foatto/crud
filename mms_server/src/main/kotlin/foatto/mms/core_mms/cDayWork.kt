package foatto.mms.core_mms

import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.mms.core_mms.calc.ObjectCalc
import java.time.temporal.ChronoUnit

class cDayWork : cStandart() {

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mODW = model as mDayWork

        val objectID = (hmColumnData[mODW.columnObject] as DataInt).value
        val dd = hmColumnData[mODW.columnObjectDayWorkDate] as DataDate3Int
        val gc = dd.localDate.atStartOfDay(zoneId)
        val begTime = gc.toEpochSecond().toInt()
        val endTime = gc.plus(1, ChronoUnit.DAYS).toEpochSecond().toInt()

        val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)
        val calc = ObjectCalc.calcObject(stm, userConfig, oc, begTime, endTime)

        (hmColumnData[mODW.columnObjectDayWorkRun] as DataString).text = calc.sbGeoRun.toString()
        (hmColumnData[mODW.columnObjectDayWorkHourName] as DataString).text = calc.sbWorkName.toString()
        (hmColumnData[mODW.columnObjectDayWorkHourValue] as DataString).text = calc.sbWorkTotal.toString()
        (hmColumnData[mODW.columnObjectDayWorkLevelName] as DataString).text = calc.sbLiquidLevelName.toString()
        (hmColumnData[mODW.columnObjectDayWorkLevelBeg] as DataString).text = calc.sbLiquidLevelBeg.toString()
        (hmColumnData[mODW.columnObjectDayWorkLevelEnd] as DataString).text = calc.sbLiquidLevelEnd.toString()
        (hmColumnData[mODW.columnObjectDayWorkLiquidName] as DataString).text = calc.sbLiquidUsingName.toString()
        (hmColumnData[mODW.columnObjectDayWorkLiquidValue] as DataString).text = calc.sbLiquidUsingTotal.toString()
    }
}

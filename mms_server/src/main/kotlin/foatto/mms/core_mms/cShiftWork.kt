package foatto.mms.core_mms

import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDateTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.iMMSApplication

class cShiftWork : cStandart() {

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val m = model as mShiftWork

        val objectId = (hmColumnData[m.columnObject] as DataInt).intValue
        val begTime = (hmColumnData[m.columnShiftBegDoc] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()
        val endTime = (hmColumnData[m.columnShiftEndDoc] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt()

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
        val calc = ObjectCalc.calcObject(stm, userConfig, oc, begTime, endTime)

        (hmColumnData[m.columnObjectShiftWorkRun] as DataString).text = calc.sGeoRun
        (hmColumnData[m.columnObjectShiftWorkHourName] as DataString).text = calc.sWorkName
        (hmColumnData[m.columnObjectShiftWorkHourValue] as DataString).text = calc.sWorkValue
        (hmColumnData[m.columnObjectShiftWorkLevelName] as DataString).text = calc.sLiquidLevelName
        (hmColumnData[m.columnObjectShiftWorkLevelBeg] as DataString).text = calc.sLiquidLevelBeg
        (hmColumnData[m.columnObjectShiftWorkLevelEnd] as DataString).text = calc.sLiquidLevelEnd
        (hmColumnData[m.columnObjectShiftWorkLiquidName] as DataString).text = calc.sLiquidUsingName
        (hmColumnData[m.columnObjectShiftWorkLiquidValue] as DataString).text = calc.sLiquidUsingValue
    }
}

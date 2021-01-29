package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.iMMSApplication
import java.time.temporal.ChronoUnit

class cDayWork : cStandart() {

    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mODW = model as mDayWork

        val objectID = (hmColumnData[mODW.columnObject] as DataInt).intValue
        val dd = hmColumnData[mODW.columnDate] as DataDate3Int
        val gc = dd.localDate.atStartOfDay(zoneId)
        val begTime = gc.toEpochSecond().toInt()
        val endTime = gc.plus(1, ChronoUnit.DAYS).toEpochSecond().toInt()

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectID)
        try {
            val calc = ObjectCalc.calcObject(stm, userConfig, oc, begTime, endTime)

            (hmColumnData[mODW.columnRun] as DataString).text = calc.sGeoRun

            (hmColumnData[mODW.columnWorkName] as DataString).text = calc.sWorkName
            (hmColumnData[mODW.columnWorkValue] as DataString).text = calc.sWorkValue

            (hmColumnData[mODW.columnEnergoName] as DataString).text = calc.sEnergoName
            (hmColumnData[mODW.columnEnergoValue] as DataString).text = calc.sEnergoValue
//        //--- if tmGroupSum.size == 1, then values are equal all sums
//        (hmColumnData[mODW.columnGroupSumEnergoName] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumEnergoName else ""
//        (hmColumnData[mODW.columnGroupSumEnergoValue] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumEnergoValue else ""
            (hmColumnData[mODW.columnAllSumEnergoName] as DataString).text = calc.sAllSumEnergoName
            (hmColumnData[mODW.columnAllSumEnergoValue] as DataString).text = calc.sAllSumEnergoValue

            (hmColumnData[mODW.columnLiquidName] as DataString).text = calc.sLiquidUsingName
            (hmColumnData[mODW.columnLiquidValue] as DataString).text = calc.sLiquidUsingValue
//        //--- if tmGroupSum.size == 1, then values are equal all sums
//        (hmColumnData[mODW.columnGroupSumLiquidName] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumLiquidName else ""
//        (hmColumnData[mODW.columnGroupSumLiquidValue] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumLiquidValue else ""
            (hmColumnData[mODW.columnAllSumLiquidName] as DataString).text = calc.sAllSumLiquidName
            (hmColumnData[mODW.columnAllSumLiquidValue] as DataString).text = calc.sAllSumLiquidValue

            (hmColumnData[mODW.columnLevelName] as DataString).text = calc.sLiquidLevelName
            (hmColumnData[mODW.columnLevelBeg] as DataString).text = calc.sLiquidLevelBeg
            (hmColumnData[mODW.columnLevelEnd] as DataString).text = calc.sLiquidLevelEnd

            (hmColumnData[mODW.columnLevelLiquidName] as DataString).text = calc.sLiquidLevelLiquidName
            (hmColumnData[mODW.columnLevelLiquidInc] as DataString).text = calc.sLiquidLevelLiquidInc
            (hmColumnData[mODW.columnLevelLiquidDec] as DataString).text = calc.sLiquidLevelLiquidDec
        } catch (t: Throwable) {
            println("objectID = $objectID")
            println("dayOfMonth = ${gc.dayOfMonth}")
            AdvancedLogger.error(t)
            t.printStackTrace()
        }
    }
}

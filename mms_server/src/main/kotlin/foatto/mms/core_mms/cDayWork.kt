package foatto.mms.core_mms

import foatto.core.util.AdvancedLogger
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataGrid
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.iMMSApplication
import java.time.temporal.ChronoUnit

class cDayWork : cStandart() {

    override fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mODW = model as mDayWork

        val objectId = (hmColumnData[mODW.columnObject] as DataInt).intValue
        val dd = hmColumnData[mODW.columnDate] as DataDate3Int
        val gc = dd.localDate.atStartOfDay(zoneId)
        val begTime = gc.toEpochSecond().toInt()
        val endTime = gc.plus(1, ChronoUnit.DAYS).toEpochSecond().toInt()

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
        try {
            val calc = ObjectCalc.calcObject(conn, userConfig, oc, begTime, endTime)

            (hmColumnData[mODW.columnRun] as DataString).text = calc.sGeoRun

            calc.tmWork.forEach { (name, value) ->
                (hmColumnData[mODW.columnWork] as DataGrid).addRowData(
                    name,
                    getSplittedDouble(value.onTime.toDouble() / 60.0 / 60.0, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

            calc.tmEnergo.forEach { (name, value) ->
                (hmColumnData[mODW.columnEnergo] as DataGrid).addRowData(
                    name,
                    getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

//        //--- if tmGroupSum.size == 1, then values are equal all sums
//        (hmColumnData[mODW.columnGroupSumEnergoName] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumEnergoName else ""
//        (hmColumnData[mODW.columnGroupSumEnergoValue] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumEnergoValue else ""

            calc.allSumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
                dataByPhase.forEach { (phase, value) ->
                    (hmColumnData[mODW.columnAllSumEnergo] as DataGrid).addRowData(
                        (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + ObjectCalc.getPhaseDescr(phase),
                        getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                    )
                }
            }

            calc.tmLiquidUsing.forEach { (name, value) ->
                (hmColumnData[mODW.columnLiquid] as DataGrid).addRowData(
                    name,
                    getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

//        //--- if tmGroupSum.size == 1, then values are equal all sums
//        (hmColumnData[mODW.columnGroupSumLiquidName] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumLiquidName else ""
//        (hmColumnData[mODW.columnGroupSumLiquidValue] as DataString).text = if(calc.tmGroupSum.size > 1 ) calc.sGroupSumLiquidValue else ""

            calc.allSumData.tmLiquidUsing.forEach { (name, value) ->
                (hmColumnData[mODW.columnAllSumLiquid] as DataGrid).addRowData(
                    name,
                    getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

            calc.tmLiquidLevel.forEach { (liquidName, llcd) ->
                (hmColumnData[mODW.columnLevel] as DataGrid).addRowData(
                    liquidName,
                    getSplittedDouble(llcd.begLevel, ObjectCalc.getPrecision(llcd.begLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                    getSplittedDouble(llcd.endLevel, ObjectCalc.getPrecision(llcd.endLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

            calc.allSumData.tmLiquidIncDec.forEach { (liquidName, pairIncDec) ->
                (hmColumnData[mODW.columnLevelLiquid] as DataGrid).addRowData(
                    liquidName,
                    getSplittedDouble(pairIncDec.first, ObjectCalc.getPrecision(pairIncDec.first), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                    getSplittedDouble(pairIncDec.second, ObjectCalc.getPrecision(pairIncDec.second), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                )
            }

        } catch (t: Throwable) {
            println("objectId = $objectId")
            println("dayOfMonth = ${gc.dayOfMonth}")
            AdvancedLogger.error(t)
            t.printStackTrace()
        }
    }
}

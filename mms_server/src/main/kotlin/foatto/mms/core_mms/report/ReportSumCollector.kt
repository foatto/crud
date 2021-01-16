package foatto.mms.core_mms.report

import foatto.mms.core_mms.calc.ObjectCalc
import java.util.*

class ReportSumCollector {

    val sumUser = ReportSumData()
    val tmSumObject = TreeMap<String, ReportSumData>()

    fun add(objectName: String?, objectCalc: ObjectCalc) {

        add(sumUser, objectCalc)

        objectName?.let {
            val sumObject = tmSumObject.getOrPut(objectName) { ReportSumData() }
            add(sumObject, objectCalc)
        }
    }

    private fun add(sumData: ReportSumData, objectCalc: ObjectCalc) {

        //--- циклы пробегаются именно по objectCalc,
        //--- т.к. в процессе суммирования будут добавляться новые имена датчиков, топлива и т.п.

        objectCalc.objectConfig.scg?.let { scg ->
            sumData.scg = scg
            objectCalc.gcd?.let { gcd ->
                sumData.run += if (scg.isUseRun) gcd.run else 0.0
                sumData.movingTime += if (scg.isUseSpeed) gcd.movingTime else 0
                sumData.parkingTime += if (scg.isUseSpeed) gcd.parkingTime else 0
                sumData.parkingCount += if (scg.isUseSpeed) gcd.parkingCount else 0
            }
        }

        objectCalc.allSumData.tmWork.forEach { (workName, onTime) ->
            val onTimeSum = sumData.tmWork[workName] ?: 0
            sumData.tmWork[workName] = onTimeSum + onTime
        }

        objectCalc.allSumData.tmEnergo.forEach { (type, byPhase) ->
            val byPhaseSum = sumData.tmEnergo.getOrPut(type) { TreeMap<Int, Double>() }
            byPhase.forEach { (phase, value) ->
                val eSum = byPhaseSum[phase] ?: 0.0
                byPhaseSum[phase] = eSum + value
            }
        }

        objectCalc.allSumData.tmLiquidUsing.forEach { (name, using) ->
            val sumUsing = sumData.tmLiquidUsing[name] ?: 0.0
            sumData.tmLiquidUsing[name] = sumUsing + using
        }

        objectCalc.allSumData.tmLiquidIncDec.forEach { (name, pairIncDec) ->
            val (curInc, curDec) = sumData.tmLiquidIncDec[name] ?: Pair(0.0, 0.0)
            sumData.tmLiquidIncDec[name] = Pair(curInc + pairIncDec.first, curDec + pairIncDec.second)
        }
    }
}

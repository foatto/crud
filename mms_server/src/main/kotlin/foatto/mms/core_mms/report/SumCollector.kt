package foatto.mms.core_mms.report

import foatto.mms.core_mms.calc.ObjectCalc
import java.util.*

class SumCollector {

    val sumUser = SumData()
    val tmSumObject = TreeMap<String, SumData>()

    fun add(objectName: String?, count: Int, objectCalc: ObjectCalc) {

        add(sumUser, count, objectCalc)

        if (objectName != null) {
            val sumObject = tmSumObject.getOrPut(objectName) { SumData() }
            add(sumObject, count, objectCalc)
        }
    }

    private fun add(sumData: SumData, count: Int, objectCalc: ObjectCalc) {

        sumData.count += count

        //--- циклы пробегаются именно по objectCalc,
        //--- т.к. в процессе суммирования будут добавляться новые имена датчиков, топлива и т.п.

        if(objectCalc.objectConfig.scg != null) {
            //--- незачем суммировать многочисленные названия
            sumData.gcd.descr = "Гео-датчик"
            //if( sumData.gcd.descr == null ) sumData.gcd.descr = objectCalc.objectConfig.scg.descr;
            //else if( ! sumData.gcd.descr.equals( objectCalc.objectConfig.scg.descr ) )
            //    sumData.gcd.descr += objectCalc.objectConfig.scg.descr;
            sumData.gcd.run += if(objectCalc.objectConfig.scg!!.isUseRun) objectCalc.gcd!!.run else 0.0
            sumData.gcd.movingTime += if (objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.movingTime else 0
            sumData.gcd.parkingCount += if (objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.parkingCount else 0
            sumData.gcd.parkingTime += if (objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.parkingTime else 0
        }

        for ((workName, wcd) in objectCalc.tmWorkCalc) {
            val wcdSum = sumData.tmWorkCalc[workName] ?: 0
            sumData.tmWorkCalc[workName] = wcdSum + wcd.onTime
        }

        objectCalc.tmLiquidUsingTotal.forEach { (name, total) ->
            val sumTotal = sumData.tmLiquidUsingTotal[name] ?: 0.0
            sumData.tmLiquidUsingTotal[name] = sumTotal + total

            val sumCalc = sumData.tmLiquidUsingCalc[name] ?: 0.0
            sumData.tmLiquidUsingCalc[name] = sumCalc + (objectCalc.tmLiquidUsingCalc[name] ?: 0.0)
        }

        for (energoName in objectCalc.tmEnergoCalc.keys) {
            val eSum = sumData.tmEnergoCalc[energoName] ?: 0.0
            val e = objectCalc.tmEnergoCalc[energoName] ?: 0.0

            sumData.tmEnergoCalc[energoName] = eSum + e
        }
    }
}

package foatto.mms.core_mms.report

import foatto.mms.core_mms.calc.LiquidUsingCalcData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.WorkCalcData
import java.util.*

class SumCollector {

    var sumUser = SumData()
    var tmSumObject = TreeMap<String, SumData>()

    fun add( objectName: String?, count: Int, objectCalc: ObjectCalc ) {

        add(sumUser, count, objectCalc)

        if(objectName != null) {
            var sumObject: SumData? = tmSumObject[objectName]
            if(sumObject == null) {
                sumObject = SumData()
                tmSumObject[objectName] = sumObject
            }
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
            sumData.gcd.movingTime += if(objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.movingTime else 0
            sumData.gcd.parkingCount += if(objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.parkingCount else 0
            sumData.gcd.parkingTime += if(objectCalc.objectConfig.scg!!.isUseSpeed) objectCalc.gcd!!.parkingTime else 0
        }

        for( ( workName, wcd ) in objectCalc.tmWorkCalc) {
            var wcdSum: WorkCalcData? = sumData.tmWorkCalc[workName]
            if(wcdSum == null) {
                wcdSum = WorkCalcData()
                sumData.tmWorkCalc[workName] = wcdSum
            }
            wcdSum.onTime += wcd.onTime
            wcdSum.onMovingTime += wcd.onMovingTime
            wcdSum.onParkingTime += wcd.onParkingTime
        }

        for( ( liquidName, lucd) in objectCalc.tmLiquidUsingCalc ) {
            var lucdSum: LiquidUsingCalcData? = sumData.tmLiquidUsingCalc[liquidName]
            if(lucdSum == null) {
                lucdSum = LiquidUsingCalcData()
                sumData.tmLiquidUsingCalc[liquidName] = lucdSum
            }
            lucdSum.usingMoving += lucd.usingMoving
            lucdSum.usingParking += lucd.usingParking
            lucdSum.usingTotal += lucd.usingTotal
        }

        for(energoName in objectCalc.tmEnergoCalc.keys) {
            val e = objectCalc.tmEnergoCalc[energoName]
            val eSum = sumData.tmEnergoCalc[energoName]

            sumData.tmEnergoCalc[energoName] = (eSum ?: 0) + e!!
        }
    }
}

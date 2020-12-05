package foatto.mms.core_mms.report

import foatto.core.util.getSplittedDouble
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

abstract class cAbstractPeriodSummary : cMMSReport() {

    protected var isGlobalUseSpeed = false
    protected var isGlobalUseRun = false
    protected var isGlobalUsingCalc = false

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4

        printPageOrientation = if (isGlobalUseSpeed) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT

        printMarginLeft = if (isGlobalUseSpeed) 10 else 20
        printMarginRight = 10
        printMarginTop = if (isGlobalUseSpeed) 20 else 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    protected fun defineSummaryReportHeaders(sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        val alDim = ArrayList<Int>()

        offsY++

        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
        //--- setting the sizes of headers (total width = 140 for A4-landscape margins of 10 mm)
        alDim.add(5)    // row no
        alDim.add(if (isGlobalUseSpeed) 54 else if (isGlobalUsingCalc) 31 else 40)    // Наименование
        //--- further, depending on options and the presence of a geo-sensor, data can be displayed
        //--- in 5 to 9 columns of equal width
        for (i in 0 until if (isGlobalUseSpeed) 9 else if (isGlobalUsingCalc) 6 else 5) alDim.add(9)

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        return offsY
    }

    protected fun addGroupTitle(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        sheet.addCell(Label(1, offsY, title, wcfCellCB))
        sheet.mergeCells(1, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY + 2)
        offsY += 4
        return offsY
    }

    protected open fun outRow(
        sheet: WritableSheet, aOffsY: Int, objectConfig: ObjectConfig, objectCalc: ObjectCalc
    ): Int {
        var offsY = aOffsY
        //--- geo-sensor report
        if (objectConfig.scg != null && (objectConfig.scg!!.isUseSpeed || objectConfig.scg!!.isUseRun)) {
            var offsX = 1
            sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
            sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
            offsX++
            if (objectConfig.scg!!.isUseRun) {
                sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
            }
            if (objectConfig.scg!!.isUseSpeed) {
                sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX + 4, offsY)
                sheet.addCell(Label(offsX, offsY + 1, "выезда", wcfCaptionHC))
                sheet.addCell(Label(offsX + 1, offsY + 1, "заезда", wcfCaptionHC))
                sheet.addCell(Label(offsX + 2, offsY + 1, "в пути", wcfCaptionHC))
                sheet.addCell(Label(offsX + 3, offsY + 1, "в движении", wcfCaptionHC))
                sheet.addCell(Label(offsX + 4, offsY + 1, "на стоянках", wcfCaptionHC))
                sheet.addCell(Label(offsX + 5, offsY, "Кол-во стоянок", wcfCaptionHC))
                sheet.mergeCells(offsX + 5, offsY, offsX + 5, offsY + 1)
                offsX += 6
            }
            offsY += 2

            offsX = 1
            sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoName.toString(), wcfCellC))
            if (objectConfig.scg!!.isUseRun) {
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoRun.toString(), wcfCellC))
            }
            if (objectConfig.scg!!.isUseSpeed) {
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoOutTime.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoInTime.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoWayTime.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoMovingTime.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoParkingTime.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, objectCalc.sbGeoParkingCount.toString(), wcfCellC))
            }
            offsY += 2
        }

        //--- report on sensors of equipment operation
        if (objectCalc.tmWorkCalc.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
            offsY++
            objectCalc.tmWorkCalc.forEach { (workDescr, wcd) ->
                sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
                offsY++
            }
            offsY++
        }

        //--- report on liquid/fuel using
        if (objectCalc.tmLiquidUsingCalc.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "В т.ч. расчётный расход", wcfCaptionHC))
            offsY++
            objectCalc.tmLiquidUsingTotal.forEach { (liquidDescr, totalValue) ->
                sheet.addCell(Label(1, offsY, liquidDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(totalValue, ObjectCalc.getPrecision(totalValue)).toString(), wcfCellC))
                val calcValue = objectCalc.tmLiquidUsingCalc[liquidDescr]
                if (calcValue != null && calcValue > 0) {
                    sheet.addCell(Label(3, offsY, getSplittedDouble(calcValue, ObjectCalc.getPrecision(calcValue)).toString(), wcfCellC))
                }
                offsY++
            }
            offsY++
        }

        //--- report on energo sensors
        if (objectCalc.tmEnergoCalc.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход/Генерация", wcfCaptionHC))
            offsY++
            objectCalc.tmEnergoCalc.forEach { (energoDescr, energoValue) ->
                sheet.addCell(Label(1, offsY, energoDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(energoValue, 1).toString(), wcfCellC))
                offsY++
            }
            offsY++
        }

        //--- отчёт по датчикам уровня жидкости
        if (objectCalc.tmLiquidLevelCalc.isNotEmpty()) {
            //--- используется ли вообще usingCalc
            var isUsingCalc = false
            for (llcd in objectCalc.tmLiquidLevelCalc.values) {
                if (llcd.usingCalc > 0.0) {
                    isUsingCalc = true
                    break
                }
            }

            sheet.addCell(Label(1, offsY, "Наименование ёмкости [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Остаток на начало периода", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Остаток на конец периода", wcfCaptionHC))
            sheet.addCell(Label(4, offsY, "Заправка", wcfCaptionHC))
            sheet.addCell(Label(5, offsY, "Слив", wcfCaptionHC))
            sheet.addCell(Label(6, offsY, "Расход", wcfCaptionHC))
            if (isUsingCalc) sheet.addCell(Label(7, offsY, "В т.ч. расчётный расход", wcfCaptionHC))
            offsY++

            objectCalc.tmLiquidLevelCalc.forEach { (liquidName, llcd) ->
                sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(llcd.begLevel, ObjectCalc.getPrecision(llcd.begLevel)).toString(), wcfCellC))
                sheet.addCell(Label(3, offsY, getSplittedDouble(llcd.endLevel, ObjectCalc.getPrecision(llcd.endLevel)).toString(), wcfCellC))
                sheet.addCell(Label(4, offsY, getSplittedDouble(llcd.incTotal, ObjectCalc.getPrecision(llcd.incTotal)).toString(), wcfCellC))
                sheet.addCell(Label(5, offsY, getSplittedDouble(llcd.decTotal, ObjectCalc.getPrecision(llcd.decTotal)).toString(), wcfCellC))
                sheet.addCell(Label(6, offsY, getSplittedDouble(llcd.usingTotal, ObjectCalc.getPrecision(llcd.usingTotal)).toString(), wcfCellC))

                if (isUsingCalc) sheet.addCell(
                    Label(
                        7, offsY, if (llcd.usingCalc <= 0) "-"
                        else getSplittedDouble(llcd.usingCalc, ObjectCalc.getPrecision(llcd.usingCalc)).toString(), wcfCellC
                    )
                )
                offsY++
            }
            offsY++
        }

        if (objectCalc.tmTemperature.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Температура начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Температура конечная", wcfCaptionHC))
            offsY++
            objectCalc.tmTemperature.forEach { (descr, lineData) ->
                if (lineData.alGLD.isNotEmpty()) {
                    sheet.addCell(Label(1, offsY, descr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lineData.alGLD.first().y, ObjectCalc.getPrecision(lineData.alGLD.first().y)).toString(), wcfCellC))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(lineData.alGLD.last().y, ObjectCalc.getPrecision(lineData.alGLD.last().y)).toString(), wcfCellC))
                    offsY++
                }
            }
            offsY++
        }

        if (objectCalc.tmDensity.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Плотность начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Плотность конечная", wcfCaptionHC))
            offsY++
            objectCalc.tmDensity.forEach { (descr, lineData) ->
                if (lineData.alGLD.isNotEmpty()) {
                    sheet.addCell(Label(1, offsY, descr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lineData.alGLD.first().y, ObjectCalc.getPrecision(lineData.alGLD.first().y)).toString(), wcfCellC))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(lineData.alGLD.last().y, ObjectCalc.getPrecision(lineData.alGLD.last().y)).toString(), wcfCellC))
                    offsY++
                }
            }
            offsY++
        }

        //--- withdrawal of the amount for each amount group
        objectCalc.tmGroupSum.forEach { (sumName, sumData) ->
            sheet.addCell(Label(1, offsY, "ИТОГО по '$sumName':", wcfCellRBStdYellow))
            offsY++

            sheet.addCell(Label(1, offsY, "Время работы [час]", wcfCellRBStdYellow))
            sheet.addCell(Label(2, offsY, getSplittedDouble(sumData.onTime.toDouble() / 60.0 / 60.0, 1).toString(), wcfCellC))
            offsY++

            sheet.addCell(Label(1, offsY, "Расход жидкостей/топлива", wcfCellRBStdYellow))
            sumData.tmLiquidUsingTotal.forEach { (name, using) ->
                sheet.addCell(Label(1, offsY, name, wcfCellRBStdYellow))
                sheet.addCell(Label(2, offsY, getSplittedDouble(using, ObjectCalc.getPrecision(using)).toString(), wcfCellC))
                offsY++
            }
            sheet.addCell(Label(1, offsY, "в т.ч. расчётный", wcfCellRBStdYellow))
            sumData.tmLiquidUsingCalc.forEach { (name, using) ->
                sheet.addCell(Label(1, offsY, name, wcfCellRBStdYellow))
                sheet.addCell(Label(2, offsY, getSplittedDouble(using, ObjectCalc.getPrecision(using)).toString(), wcfCellC))
                offsY++
            }

            sheet.addCell(Label(1, offsY, "Расход/генерация э/энергии", wcfCellRBStdYellow))
            sumData.tmEnergoUsing.forEach { (sensorType, dataByPhase) ->
                dataByPhase.forEach { (phase, value) ->
                    sheet.addCell(Label(1, offsY, (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase), wcfCellRBStdYellow))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(value, 1).toString(), wcfCellC))
                    offsY++
                }
            }

            sheet.addCell(Label(1, offsY, "Уровень жидкости/топлива", wcfCellRBStdYellow))
            sheet.addCell(Label(2, offsY, "Начальный", wcfCellRBStdYellow))
            sheet.addCell(Label(3, offsY, "Конечный", wcfCellRBStdYellow))
            sheet.addCell(Label(4, offsY, "Заправка", wcfCellRBStdYellow))
            sheet.addCell(Label(5, offsY, "Слив", wcfCellRBStdYellow))
            offsY++
            sheet.addCell(Label(2, offsY, getSplittedDouble(sumData.begLevel, ObjectCalc.getPrecision(sumData.begLevel)).toString(), wcfCellCBStdYellow))
            sheet.addCell(Label(3, offsY, getSplittedDouble(sumData.endLevel, ObjectCalc.getPrecision(sumData.endLevel)).toString(), wcfCellCBStdYellow))
            sheet.addCell(Label(4, offsY, getSplittedDouble(sumData.incTotal, ObjectCalc.getPrecision(sumData.incTotal)).toString(), wcfCellCBStdYellow))
            sheet.addCell(Label(5, offsY, getSplittedDouble(sumData.decTotal, ObjectCalc.getPrecision(sumData.decTotal)).toString(), wcfCellCBStdYellow))
        }

        offsY++    // еще одна пустая строчка снизу

        return offsY

//!!! вернуть расчёты среднего расхода ?
//                //--- дополним стандартный расчёт средними расходами: при наличии пробега посчитаем общий средний расход
//                val sAvgUsingTotal = if(objectConfig.scg!!.isUseRun) {
//                    val usingCalc = objectCalc.tmGroupSum[objectConfig.scg!!.sumGroup]?.usingTotal
//                    if( objectCalc.gcd!!.run <= 0.0 || usingCalc == null) "-"
//                    else getSplittedDouble(100.0 * usingCalc / objectCalc.gcd!!.run, 1).toString()
//                }
//                else {
//                    "-"
//                }
//                if(objectConfig.scg!!.isUseRun) {
//                    sheet.addCell(Label(offsX, offsY, "Сред. расх. [л/100км]", wcfCaptionHC))
//                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
//                    offsX++
//                }
//                if(objectConfig.scg!!.isUseRun) sheet.addCell(Label(offsX++, offsY, sAvgUsingTotal, wcfCellC))

//                //--- дополним стандартный расчёт средним расходом
//                val tmAvgUsingTotal = TreeMap<String, String>()
//                for((workDescr,wcd) in objectCalc.tmWorkCalc) {
//
//                    var sumUsingTotal: Double? = null
//                    for(llcd in objectCalc.tmLiquidLevelCalc.values) {
//                        //--- если в группе у этого уровнемера есть датчик работы оборудования, он один и "этот самый",
//                        //--- и при этом нет геодатчика и электросчётчика (т.е. генераторов), то считаем средний расход
//                        if(llcd.tmWorkCalc.size == 1 && llcd.tmWorkCalc[workDescr] != null && llcd.gcd == null && llcd.tmEnergoCalc.isEmpty()) {
//
//                            sumUsingTotal = (sumUsingTotal ?: 0.0) + llcd.usingTotal
//                            if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
//                                sumUsingMoving = (sumUsingMoving ?: 0.0) + llcd.usingMoving
//                                sumUsingParking = (sumUsingParking ?: 0.0) + llcd.usingParking
//                            }
//                        }
//                    }
//                    tmAvgUsingTotal[workDescr] = if(wcd.onTime == 0 || sumUsingTotal == null || sumUsingTotal < 0) "-"
//                    else getSplittedDouble(sumUsingTotal / (wcd.onTime.toDouble() / 60.0 / 60.0), 1).toString()
//                    if(objectConfig.scg != null && objectConfig.scg!!.isUseSpeed) {
//                        tmAvgUsingMoving[workDescr] = if(wcd.onMovingTime == 0 || sumUsingMoving == null || sumUsingMoving < 0) "-"
//                        else getSplittedDouble(sumUsingMoving / (wcd.onMovingTime.toDouble() / 60.0 / 60.0), 1).toString()
//                        tmAvgUsingParking[workDescr] = if(wcd.onParkingTime == 0 || sumUsingParking == null || sumUsingParking < 0) "-"
//                        else getSplittedDouble(sumUsingParking / (wcd.onParkingTime.toDouble() / 60.0 / 60.0), 1).toString()
//                    }
//                }
//                sheet.addCell(Label(3, offsY, "Средний расход [л/час]", wcfCaptionHC))
//                    sheet.addCell(Label(3, offsY, tmAvgUsingTotal[workDescr], wcfCellC))

//            //--- дополним стандартный расчёт средним расходом
//            val tmEnergoCalcAvgUsing = TreeMap<String, String>()
//            for(energoDescr in objectCalc.tmEnergoCalc.keys) {
//                val e = objectCalc.tmEnergoCalc[energoDescr]
//                var sumUsing: Double? = null
//                for(llcd in objectCalc.tmLiquidLevelCalc.values) {
//                    //--- если в группе у этого уровнемера есть электросчётчик, он один и "этот самый",
//                    //--- и при этом нет геодатчика и оборудования, то считаем средний расход
//                    if(llcd.tmEnergoCalc.size == 1 && llcd.tmEnergoCalc[energoDescr] != null && llcd.gcd == null && llcd.tmWorkCalc.isEmpty())
//
//                        sumUsing = (sumUsing ?: 0.0) + llcd.usingTotal
//                }
//                //--- выводим в кВт*ч
//                tmEnergoCalcAvgUsing[energoDescr] = if(e == 0 || sumUsing == null || sumUsing < 0) "-"
//                else getSplittedDouble(sumUsing / (e!! / 1000.0), 1).toString()
//            }
//            sheet.addCell(Label(3, offsY, "Средний расход [л/кВт*ч]", wcfCaptionHC))
//                sheet.addCell(Label(3, offsY, tmEnergoCalcAvgUsing[energoDescr], wcfCellC))
    }

    protected fun outPeriodSum(sheet: WritableSheet, aOffsY: Int, periodSumCollector: SumCollector): Int {
        var offsY = aOffsY

        sheet.addCell(Label(1, offsY, "ИТОГО за период:", wcfCellCBStdYellow))
        offsY++
        periodSumCollector.sumUser.tmLiquidUsingTotal.forEach { (liquidDescr, totalValue) ->
            sheet.addCell(Label(1, offsY, liquidDescr, wcfCellCBStdYellow))
            sheet.addCell(Label(2, offsY, getSplittedDouble(totalValue, ObjectCalc.getPrecision(totalValue)).toString(), wcfCellCBStdYellow))
//                val calcValue = periodSumCollector.sumUser.tmLiquidUsingCalc[liquidDescr]
//                if(calcValue != null && calcValue > 0 ) {
//                    sheet.addCell(Label(3, offsY, getSplittedDouble(calcValue, ObjectCalc.getPrecision(calcValue)).toString(), wcfCellC))
//                }
            offsY++
        }
        offsY++
        periodSumCollector.sumUser.tmEnergoCalc.forEach { (energoDescr, value) ->
            sheet.addCell(Label(1, offsY, energoDescr, wcfCellCBStdYellow))
            sheet.addCell(Label(2, offsY, getSplittedDouble(value, ObjectCalc.getPrecision(value)).toString(), wcfCellCBStdYellow))
            offsY++
        }
        offsY += 2

        return offsY
    }

    protected fun outSumData(sheet: WritableSheet, aOffsY: Int, sumData: SumData, isBold: Boolean): Int {
        var offsY = aOffsY

        val sbGeoName = StringBuilder()
        val sbGeoRun = StringBuilder()
        val sbGeoMovingTime = StringBuilder()
        val sbGeoParkingTime = StringBuilder()
        val sbGeoParkingCount = StringBuilder()

        //--- сумма пробегов, времени и моточасов имеет смысл только в разрезе конкретной единицы оборудования
        if(!isBold) {
            ObjectCalc.fillGeoString(
                gcd = sumData.gcd,
                zoneId = zoneId,
                sbGeoName = sbGeoName,
                sbGeoRun = sbGeoRun,
                sbGeoOutTime = StringBuilder(),
                sbGeoInTime = StringBuilder(),
                sbGeoWayTime = StringBuilder(),
                sbGeoMovingTime = sbGeoMovingTime,
                sbGeoParkingTime = sbGeoParkingTime,
                sbGeoParkingCount = sbGeoParkingCount
            )
        }

        //-- отчёт по гео-датчику - определяется по наличию данных о пробеге,
        //-- т.к. наименование датчика могут забыть ввести
        //-- (sbGeoRun.length() > 0 тоже не показатель, т.к. всегда равен какому-нибудь числу)
        if ((isGlobalUseSpeed || isGlobalUseRun) && sumData.gcd.descr != null && !isBold) {
            var offsX = 1
            sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
            sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
            offsX++
            if (isGlobalUseRun) {
                sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
            }
            if (isGlobalUseSpeed) {
                sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX + 1, offsY)
                sheet.addCell(Label(offsX, offsY + 1, "в движении", wcfCaptionHC))
                sheet.addCell(Label(offsX + 1, offsY + 1, "на стоянках", wcfCaptionHC))
                sheet.addCell(Label(offsX + 2, offsY, "Кол-во стоянок", wcfCaptionHC))
                sheet.mergeCells(offsX + 2, offsY, offsX + 2, offsY + 1)
            }
            offsY += 2

            offsX = 1
            sheet.addCell(Label(offsX++, offsY, sbGeoName.toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
            if (isGlobalUseRun) sheet.addCell(Label(offsX++, offsY, sbGeoRun.toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
            if (isGlobalUseSpeed) {
                sheet.addCell(Label(offsX++, offsY, sbGeoMovingTime.toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sbGeoParkingTime.toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sbGeoParkingCount.toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
            }
            offsY++
        }

        //--- отчёт по датчикам оборудования  - определяется по наличию данных о моточасах,
        //--- т.к. наименование датчика могут забыть ввести
        if (sumData.tmWorkCalc.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Время работы", wcfCaptionHC))
            offsY++
            sumData.tmWorkCalc.forEach { (workDescr, onTime) ->
                sheet.addCell(Label(1, offsY, workDescr, if (isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(onTime.toDouble() / 60.0 / 60.0, 1).toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
                offsY++
            }
            offsY++
        }

        //--- отчёт по совместному расходу жидкости - определяется по наличию данных об общем расходе,
        //--- т.к. наименование жидкости могут забыть ввести
        if (sumData.tmLiquidUsingTotal.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Расход по видам топлива", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
            offsY++
            sumData.tmLiquidUsingTotal.forEach { (liquidDescr, total) ->
                sheet.addCell(Label(1, offsY, liquidDescr, if (isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(total, ObjectCalc.getPrecision(total)).toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
                offsY++
            }
            offsY++
        }

        //--- отчёт по электросчётчикам  - определяется по наличию данных об электроэнергии,
        //--- т.к. наименование датчика могут забыть ввести
        if (sumData.tmEnergoCalc.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Электросчётчик", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Электроэнергия", wcfCaptionHC))
            offsY++
            sumData.tmEnergoCalc.forEach { (energoDescr, energoData) ->
                sheet.addCell(Label(1, offsY, energoDescr, if (isBold) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(energoData, ObjectCalc.getPrecision(energoData)).toString(), if (isBold) wcfCellCBStdYellow else wcfCellC))
                offsY++
            }
            offsY++
        }

        offsY++    // еще одна пустая строчка снизу

        return offsY
    }

    protected fun outReportTrail(sheet: WritableSheet, offsY: Int): Int {
        sheet.addCell(Label(if (isGlobalUseSpeed) 8 else if (isGlobalUsingCalc) 5 else 4, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(
            if (isGlobalUseSpeed) 8 else if (isGlobalUsingCalc) 5 else 4, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY
        )

        return offsY
    }

    private fun getPhaseDescr(phase: Int) =
        when (phase) {
            0 -> "(сумма фаз)"
            1 -> "(фаза A)"
            2 -> "(фаза B)"
            3 -> "(фаза C)"
            else -> "(неизв. фаза)"
        }

}

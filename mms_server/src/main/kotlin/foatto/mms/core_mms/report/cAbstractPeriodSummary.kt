package foatto.mms.core_mms.report

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import foatto.core.util.secondIntervalToString
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.CalcSumData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.ObjectCalc.Companion.getPhaseDescr
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

abstract class cAbstractPeriodSummary : cMMSReport() {

    protected var isGlobalUseSpeed = false

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
        alDim.add(if (isGlobalUseSpeed) 63 else 31)    // name
        //--- further, depending on options and the presence of a geo-sensor, data can be displayed
        //--- in 5 to 9 columns of equal width
        for (i in 0 until getColumnCount(2)) {
            alDim.add(9)
        }

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
        sheet.mergeCells(1, offsY, getColumnCount(1), offsY + 2)
        offsY += 4
        return offsY
    }

    protected fun outRow(
        sheet: WritableSheet,
        aOffsY: Int,
        objectConfig: ObjectConfig,
        objectCalc: ObjectCalc,
        isOutTemperature: Boolean,
        isOutDensity: Boolean,
        isKeepPlaceForComment: Boolean,
        troubles: GraphicDataContainer?,
        isOutGroupSum: Boolean,
    ): Int {
        var offsY = aOffsY
        //--- geo-sensor report
        objectConfig.scg?.let { scg ->
            if (scg.isUseSpeed || scg.isUseRun) {
                var offsX = 1
                sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
                if (scg.isUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
                }
                if (scg.isUseSpeed) {
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
                if (scg.isUseRun) {
                    sheet.addCell(Label(offsX, offsY, "Сред. расх. [на 100 км]", wcfCaptionHC))
                    sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                    offsX++
                }
                offsY += 2

                offsX = 1
                sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoName, wcfCellC))
                if (scg.isUseRun) {
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoRun, wcfCellC))
                }
                if (scg.isUseSpeed) {
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoOutTime, wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoInTime, wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoWayTime, wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoMovingTime, wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoParkingTime, wcfCellC))
                    sheet.addCell(Label(offsX++, offsY, objectCalc.sGeoParkingCount, wcfCellC))
                }
                if (scg.isUseRun) {
                    val tmLiquidUsing = objectCalc.tmGroupSum[scg.group]?.tmLiquidUsing
                    val sAvgUsing = if (tmLiquidUsing?.size == 1 && objectCalc.gcd!!.run > 0.0) {
                        getSplittedDouble(100.0 * tmLiquidUsing.firstEntry().value / objectCalc.gcd!!.run, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                    } else {
                        "-"
                    }
                    sheet.addCell(Label(offsX++, offsY, sAvgUsing, wcfCellC))
                }
                offsY++
                if (isKeepPlaceForComment) {
                    sheet.addCell(Label(1, offsY, "", wcfComment))
                    sheet.mergeCells(1, offsY, getColumnCount(1), offsY)
                    offsY += 2
                }
            }
        }

        //--- report on sensors of equipment operation
        if (objectCalc.tmWork.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Сред. расх. [на 1 час]", wcfCaptionHC))
            offsY++
            objectCalc.tmWork.forEach { (workDescr, wcd) ->
                sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(wcd.onTime.toDouble() / 60.0 / 60.0, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                val tmWork = objectCalc.tmGroupSum[wcd.group]?.tmWork
                val tmLiquidUsing = objectCalc.tmGroupSum[wcd.group]?.tmLiquidUsing
                val sAvgUsing = if (tmWork?.size == 1 && tmLiquidUsing?.size == 1 && wcd.onTime > 0) {
                    getSplittedDouble(tmLiquidUsing.firstEntry().value / (wcd.onTime.toDouble() / 60.0 / 60.0), 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                } else {
                    "-"
                }
                sheet.addCell(Label(3, offsY, sAvgUsing, wcfCellC))
                if (isKeepPlaceForComment) {
                    sheet.addCell(Label(4, offsY, "", wcfComment))
                    sheet.mergeCells(4, offsY, getColumnCount(1), offsY)
                }
                offsY++
            }
            offsY++
        }

        //--- report on energo sensors
        if (objectCalc.tmEnergo.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход/Генерация", wcfCaptionHC))
            offsY++
            objectCalc.tmEnergo.forEach { (energoDescr, energoValue) ->
                sheet.addCell(Label(1, offsY, energoDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(energoValue, ObjectCalc.getPrecision(energoValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                if (isKeepPlaceForComment) {
                    sheet.addCell(Label(3, offsY, "", wcfComment))
                    sheet.mergeCells(3, offsY, getColumnCount(1), offsY)
                }
                offsY++
            }
            offsY++
        }

        //--- report on liquid/fuel using
        if (objectCalc.tmLiquidUsing.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
            offsY++
            objectCalc.tmLiquidUsing.forEach { (liquidDescr, totalValue) ->
                sheet.addCell(Label(1, offsY, liquidDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(totalValue, ObjectCalc.getPrecision(totalValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                if (isKeepPlaceForComment) {
                    sheet.addCell(Label(3, offsY, "", wcfComment))
                    sheet.mergeCells(3, offsY, getColumnCount(1), offsY)
                }
                offsY++
            }
            offsY++
        }

        //--- отчёт по датчикам уровня жидкости
        if (objectCalc.tmLiquidLevel.isNotEmpty()) {
            //--- используется ли вообще usingCalc
            var isUsingCalc = false
            for (llcd in objectCalc.tmLiquidLevel.values) {
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
            if (isUsingCalc) {
                sheet.addCell(Label(7, offsY, "В т.ч. расчётный расход", wcfCaptionHC))
            }
            offsY++

            objectCalc.tmLiquidLevel.forEach { (liquidName, llcd) ->
                sheet.addCell(Label(1, offsY, liquidName, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(llcd.begLevel, ObjectCalc.getPrecision(llcd.begLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                sheet.addCell(Label(3, offsY, getSplittedDouble(llcd.endLevel, ObjectCalc.getPrecision(llcd.endLevel), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                sheet.addCell(Label(4, offsY, getSplittedDouble(llcd.incTotal, ObjectCalc.getPrecision(llcd.incTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                sheet.addCell(Label(5, offsY, getSplittedDouble(llcd.decTotal, ObjectCalc.getPrecision(llcd.decTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                sheet.addCell(Label(6, offsY, getSplittedDouble(llcd.usingTotal, ObjectCalc.getPrecision(llcd.usingTotal), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))

                if (isUsingCalc) sheet.addCell(
                    Label(
                        7, offsY, if (llcd.usingCalc <= 0) "-"
                        else getSplittedDouble(llcd.usingCalc, ObjectCalc.getPrecision(llcd.usingCalc), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC
                    )
                )
                offsY++
                if (isKeepPlaceForComment) {
                    sheet.addCell(Label(1, offsY, "", wcfComment))
                    sheet.mergeCells(1, offsY, getColumnCount(1), offsY)
                    offsY += 2
                }
            }
            offsY++
        }

        if (isOutTemperature && objectCalc.tmTemperature.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Температура начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Температура конечная", wcfCaptionHC))
            offsY++
            objectCalc.tmTemperature.forEach { (descr, lineData) ->
                if (lineData.alGLD.isNotEmpty()) {
                    sheet.addCell(Label(1, offsY, descr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lineData.alGLD.first().y, ObjectCalc.getPrecision(lineData.alGLD.first().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(lineData.alGLD.last().y, ObjectCalc.getPrecision(lineData.alGLD.last().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                    if (isKeepPlaceForComment) {
                        sheet.addCell(Label(4, offsY, "", wcfComment))
                        sheet.mergeCells(4, offsY, getColumnCount(1), offsY)
                    }
                    offsY++
                }
            }
            offsY++
        }

        if (isOutDensity && objectCalc.tmDensity.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Плотность начальная", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Плотность конечная", wcfCaptionHC))
            offsY++
            objectCalc.tmDensity.forEach { (descr, lineData) ->
                if (lineData.alGLD.isNotEmpty()) {
                    sheet.addCell(Label(1, offsY, descr, wcfCellC))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(lineData.alGLD.first().y, ObjectCalc.getPrecision(lineData.alGLD.first().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                    sheet.addCell(Label(3, offsY, getSplittedDouble(lineData.alGLD.last().y, ObjectCalc.getPrecision(lineData.alGLD.last().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                    if (isKeepPlaceForComment) {
                        sheet.addCell(Label(4, offsY, "", wcfComment))
                        sheet.mergeCells(4, offsY, getColumnCount(1), offsY)
                    }
                    offsY++
                }
            }
            offsY++
        }

        troubles?.alGTD?.let { alGTD ->
            if (alGTD.isNotEmpty()) {
                sheet.addCell(Label(1, offsY, "Неисправность", wcfCaptionHC))
                sheet.addCell(Label(2, offsY, "Время начала", wcfCaptionHC))
                sheet.addCell(Label(3, offsY, "Время окончания", wcfCaptionHC))
                offsY++
                alGTD.forEach { gtd ->
                    if (gtd.fillColorIndex == GraphicColorIndex.FILL_CRITICAL ||
                        gtd.borderColorIndex == GraphicColorIndex.BORDER_CRITICAL ||
                        gtd.textColorIndex == GraphicColorIndex.TEXT_CRITICAL
                    ) {
                        sheet.addCell(Label(1, offsY, gtd.text, wcfCellR))
                        sheet.addCell(Label(2, offsY, DateTime_DMYHMS(zoneId, gtd.textX1), wcfCellC))
                        sheet.addCell(Label(3, offsY, DateTime_DMYHMS(zoneId, gtd.textX2), wcfCellC))
                        if (isKeepPlaceForComment) {
                            sheet.addCell(Label(4, offsY, "", wcfComment))
                            sheet.mergeCells(4, offsY, getColumnCount(1), offsY)
                        }
                        offsY++
                    }
                }
                offsY++
            }
        }

        //--- withdrawal of the amount for each amount group
        if (isOutGroupSum && objectCalc.tmGroupSum.size > 1) {
            objectCalc.tmGroupSum.forEach { (sumName, sumData) ->
                sheet.addCell(Label(1, offsY, "ИТОГО по '$sumName':", wcfCellRBStdYellow))
                offsY++

                offsY = outGroupSum(sheet, offsY, sumData)
                offsY++
            }
        }

        sheet.addCell(Label(1, offsY, "ИТОГО:", wcfCellRBStdYellow))
        offsY++

        offsY = outGroupSum(sheet, offsY, objectCalc.allSumData)
        offsY++

        return offsY
    }

    private fun outGroupSum(sheet: WritableSheet, aOffsY: Int, sumData: CalcSumData): Int {
        var offsY = aOffsY

        if (sumData.tmEnergo.isNotEmpty()) {
            sheet.addCell(Label(1, offsY++, "Расход/генерация э/энергии", wcfCellRBStdYellow))

            sheet.addCell(Label(1, offsY, "Наименование", wcfCellRBStdYellow))
            sheet.addCell(Label(2, offsY, "Расход/генерация", wcfCellRBStdYellow))
            sheet.addCell(Label(3, offsY, "Средний расход топлива", wcfCellRBStdYellow))
            offsY++

            sumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
                dataByPhase.forEach { (phase, value) ->
                    sheet.addCell(Label(1, offsY, (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase), wcfCellRBStdYellow))
                    sheet.addCell(Label(2, offsY, getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                    val tmLiquidUsing = sumData.tmLiquidUsing
                    val sAvgUsing = if (tmLiquidUsing.size == 1 && value > 0) {
                        getSplittedDouble(tmLiquidUsing.firstEntry().value / value, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
                    } else {
                        "-"
                    }
                    sheet.addCell(Label(3, offsY, sAvgUsing, wcfCellC))
                    offsY++
                }
            }
        }

        if (sumData.tmLiquidUsing.isNotEmpty()) {
            sheet.addCell(Label(1, offsY++, "Расход жидкостей/топлива", wcfCellRBStdYellow))

            sheet.addCell(Label(1, offsY, "Наименование", wcfCellRBStdYellow))
            sheet.addCell(Label(2, offsY, "Расход", wcfCellRBStdYellow))
            offsY++

            sumData.tmLiquidUsing.forEach { (name, using) ->
                sheet.addCell(Label(1, offsY, name, wcfCellRBStdYellow))
                sheet.addCell(Label(2, offsY, getSplittedDouble(using, ObjectCalc.getPrecision(using), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                offsY++
            }
        }
//        sheet.addCell(Label(1, offsY++, "Уровень жидкости/топлива", wcfCellRBStdYellow))
//
//        sheet.addCell(Label(1, offsY, "Начальный", wcfCellRBStdYellow))
//        sheet.addCell(Label(2, offsY, "Конечный", wcfCellRBStdYellow))
//        sheet.addCell(Label(3, offsY, "Заправка", wcfCellRBStdYellow))
//        sheet.addCell(Label(4, offsY, "Слив", wcfCellRBStdYellow))
//        offsY++
//        sheet.addCell(Label(1, offsY, getSplittedDouble(sumData.begLevel, ObjectCalc.getPrecision(sumData.begLevel)), wcfCellCBStdYellow))
//        sheet.addCell(Label(2, offsY, getSplittedDouble(sumData.endLevel, ObjectCalc.getPrecision(sumData.endLevel)), wcfCellCBStdYellow))
//        sheet.addCell(Label(3, offsY, getSplittedDouble(sumData.incTotal, ObjectCalc.getPrecision(sumData.incTotal)), wcfCellCBStdYellow))
//        sheet.addCell(Label(4, offsY, getSplittedDouble(sumData.decTotal, ObjectCalc.getPrecision(sumData.decTotal)), wcfCellCBStdYellow))
//        offsY++

        return offsY
    }

    protected fun outObjectAndUserSum(
        sheet: WritableSheet,
        aOffsY: Int,
        reportSumUser: Boolean,
        reportSumObject: Boolean,
        tmUserSumCollector: TreeMap<String, ReportSumCollector>,
        allSumCollector: ReportSumCollector,
    ): Int {
        var offsY = aOffsY

        if (reportSumUser) {
            sheet.addCell(Label(0, offsY, "ИТОГО по объектам и их владельцам", wcfCellCBStdYellow))
            sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
            offsY += 4

            for ((userName, sumUser) in tmUserSumCollector) {
                sheet.addCell(Label(0, offsY, userName, wcfCellLBStdYellow))
                sheet.mergeCells(0, offsY, getColumnCount(1), offsY)
                offsY += 2
                if (reportSumObject) {
                    val tmObjectSum = sumUser.tmSumObject
                    for ((objectInfo, objectSum) in tmObjectSum) {
                        sheet.addCell(Label(1, offsY, objectInfo, wcfCellLB))
                        offsY++

                        offsY = outSumData(sheet, offsY, objectSum, false, objectSum.scg)
                    }
                }

                sheet.addCell(Label(0, offsY, "ИТОГО по владельцу:", wcfCellLBStdYellow))
                sheet.mergeCells(0, offsY, 1, offsY)
                offsY++

                offsY = outSumData(sheet, offsY, sumUser.sumUser, true, null)
            }
        }

        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
        sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
        offsY += 4

        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)

        return offsY
    }

    protected fun outSumData(sheet: WritableSheet, aOffsY: Int, sumData: ReportSumData, isManyObjects: Boolean, scg: SensorConfigGeo?): Int {
        var offsY = aOffsY

        //--- сумма пробегов, времени и стоянок имеет смысл только в разрезе конкретной единицы оборудования
        scg?.let {
            val sGeoRun = if (sumData.run < 0) "-" else getSplittedDouble(sumData.run, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            val sGeoMovingTime = if (sumData.movingTime < 0) "-" else secondIntervalToString(sumData.movingTime)
            val sGeoParkingTime = if (sumData.parkingTime < 0) "-" else secondIntervalToString(sumData.parkingTime)
            val sGeoParkingCount = if (sumData.parkingCount < 0) {
                "-"
            } else if (userConfig.upIsUseThousandsDivider) {
                getSplittedLong(sumData.parkingCount.toLong())
            } else {
                sumData.parkingCount.toString()
            }

            var offsX = 1
            sheet.addCell(Label(offsX, offsY, "Датчик ГЛОНАСС/GPS", wcfCaptionHC))
            sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
            offsX++
            if (scg.isUseRun) {
                sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
                offsX++
            }
            if (scg.isUseSpeed) {
                sheet.addCell(Label(offsX, offsY, "Время", wcfCaptionHC))
                sheet.mergeCells(offsX, offsY, offsX + 1, offsY)
                sheet.addCell(Label(offsX, offsY + 1, "в движении", wcfCaptionHC))
                sheet.addCell(Label(offsX + 1, offsY + 1, "на стоянках", wcfCaptionHC))
                sheet.addCell(Label(offsX + 2, offsY, "Кол-во стоянок", wcfCaptionHC))
                sheet.mergeCells(offsX + 2, offsY, offsX + 2, offsY + 1)
            }
            offsY += 2

            offsX = 1
            sheet.addCell(Label(offsX++, offsY, scg.descr, wcfCellC))
            if (scg.isUseRun) {
                sheet.addCell(Label(offsX++, offsY, sGeoRun, wcfCellC))
            }
            if (scg.isUseSpeed) {
                sheet.addCell(Label(offsX++, offsY, sGeoMovingTime, wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sGeoParkingTime, wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sGeoParkingCount, wcfCellC))
            }
            offsY++
        }

        //--- сумма моточасов имеет смысл только в разрезе конкретной единицы оборудования
        if (!isManyObjects && sumData.tmWork.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Оборудование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Время работы [час]", wcfCaptionHC))
            offsY++
            sumData.tmWork.forEach { (workDescr, onTime) ->
                sheet.addCell(Label(1, offsY, workDescr, wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(onTime.toDouble() / 60.0 / 60.0, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
                offsY++
            }
        }

        if (sumData.tmEnergo.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход/генерация", wcfCaptionHC))
            offsY++
            sumData.tmEnergo.forEach { (sensorType, dataByPhase) ->
                dataByPhase.forEach { (phase, value) ->
                    sheet.addCell(
                        Label(
                            1,
                            offsY,
                            (SensorConfig.hmSensorDescr[sensorType] ?: "(неизв. тип датчика)") + getPhaseDescr(phase),
                            if (isManyObjects) wcfCellCBStdYellow else wcfCellC
                        )
                    )
                    sheet.addCell(
                        Label(
                            2,
                            offsY,
                            getSplittedDouble(value, ObjectCalc.getPrecision(value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                            if (isManyObjects) wcfCellCBStdYellow else wcfCellC
                        )
                    )
                    offsY++
                }
            }
        }

        if (sumData.tmLiquidUsing.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование топлива", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Расход", wcfCaptionHC))
            offsY++
            sumData.tmLiquidUsing.forEach { (liquidDescr, total) ->
                sheet.addCell(Label(1, offsY, liquidDescr, if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(total, ObjectCalc.getPrecision(total), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
                offsY++
            }
            offsY++
        }

        if (sumData.tmLiquidIncDec.isNotEmpty()) {
            sheet.addCell(Label(1, offsY, "Наименование топлива", wcfCaptionHC))
            sheet.addCell(Label(2, offsY, "Заправка", wcfCaptionHC))
            sheet.addCell(Label(3, offsY, "Слив", wcfCaptionHC))
            offsY++
            sumData.tmLiquidIncDec.forEach { (liquidDescr, pairIncDec) ->
                sheet.addCell(Label(1, offsY, liquidDescr, if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(pairIncDec.first, ObjectCalc.getPrecision(pairIncDec.first), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
                sheet.addCell(Label(3, offsY, getSplittedDouble(pairIncDec.second, ObjectCalc.getPrecision(pairIncDec.second), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), if (isManyObjects) wcfCellCBStdYellow else wcfCellC))
                offsY++
            }
            offsY++
        }

        return offsY
    }

    protected fun outReportTrail(sheet: WritableSheet, offsY: Int): Int {
        sheet.addCell(Label(getColumnCount(3), offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(getColumnCount(3), offsY, getColumnCount(1), offsY)

        return offsY
    }

    protected fun defineGlobalFlags(oc: ObjectConfig) {
        oc.scg?.let { scg ->
            isGlobalUseSpeed = isGlobalUseSpeed or scg.isUseSpeed
        }
    }

    protected fun getColumnCount(offsX: Int) = (if (isGlobalUseSpeed) 10 else 8) - offsX

}

package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.getDateTime
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataDouble
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cSummaryBNGRE : cAbstractPeriodSummary() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        fillReportParam(model as mOP)

        val m = model as mBNGRE

        hmReportParam["report_period_begin_value"] = (hmColumnData[m.columnPeriodBeginValue] as DataDouble).doubleValue

        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4

        printPageOrientation = PageOrientation.PORTRAIT

        printMarginLeft = 20
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        val reportObject = hmReportParam["report_object"] as Int
        val (begTime, endTime) = getBegEndTimeFromParam()
        val periodBeginValue = hmReportParam["report_period_begin_value"] as Double

        val zdtBeg = getDateTime(zoneId, begTime)
        val zdtEnd = getDateTime(zoneId, endTime)

        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, reportObject)

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(objectConfig, sheet, offsY)

        offsY = defineSummaryReportHeadersBNGRE(sheet, offsY)

        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Период", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Ед. изм.", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Остаток на начало периода", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Приход", wcfCaptionHC))
        sheet.addCell(Label(5, offsY, "Расход", wcfCaptionHC))
        sheet.addCell(Label(6, offsY, "Остаток на конец периода", wcfCaptionHC))
        offsY++

        var countNN = 1

        var zdtCurBeg = ZonedDateTime.from(zdtBeg)
        var curPeriodBeginValue = periodBeginValue
        while (zdtCurBeg.isBefore(zdtEnd)) {
            val zdtCurEnd = ZonedDateTime.from(zdtCurBeg).plus(1, ChronoUnit.DAYS)

            val t1 = zdtCurBeg.toEpochSecond().toInt()
            val t2 = zdtCurEnd.toEpochSecond().toInt()

            val objectCalc = ObjectCalc.calcObject(conn, userConfig, objectConfig, t1, t2)

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(1, offsY, DateTime_DMY(zdtCurBeg), wcfCellC))
            sheet.addCell(Label(2, offsY, "кг", wcfCellC))
            sheet.addCell(Label(3, offsY, getSplittedDouble(curPeriodBeginValue, ObjectCalc.getPrecision(curPeriodBeginValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))

            var sumIn = 0.0
            var sumOut = 0.0
            objectCalc.tmCounterData.filter { (_, counterData) ->
                counterData.scc.sensorType == SensorConfig.SENSOR_MASS_ACCUMULATED
            }.forEach { (_, counterData) ->
                if (counterData.scc.inOutType == SensorConfigCounter.CALC_TYPE_IN) {
                    sumIn += counterData.value
                } else {
                    sumOut += counterData.value
                }
            }
            sheet.addCell(Label(4, offsY, getSplittedDouble(sumIn, ObjectCalc.getPrecision(sumIn), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))
            sheet.addCell(Label(5, offsY, getSplittedDouble(sumOut, ObjectCalc.getPrecision(sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))

            val newPeriodBeginValue = curPeriodBeginValue + sumIn - sumOut
            sheet.addCell(Label(6, offsY, getSplittedDouble(newPeriodBeginValue, ObjectCalc.getPrecision(newPeriodBeginValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellC))

            curPeriodBeginValue = newPeriodBeginValue
            zdtCurBeg = zdtCurBeg.plus(1, ChronoUnit.DAYS)
            offsY++
        }
        offsY++

        sheet.addCell(Label(3, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(3, offsY, 6, offsY)
    }

    private fun defineSummaryReportHeadersBNGRE(sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        val alDim = ArrayList<Int>()

        offsY++

        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
        alDim.add(5)    // row no
        alDim.add(9)    // beg date
        alDim.add(4)    // dim
        alDim.add(8)    // beg value
        alDim.add(8)    // in
        alDim.add(8)    // out  (7 - недостаточно для чисел типа "-140 000")
        alDim.add(8)    // end value

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        return offsY
    }

}

//class cSummaryBNGRE : cAbstractPeriodSummary() {
//
//    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
//        val returnURL = super.doSave(action, alFormData, hmOut)
//        if (returnURL != null) {
//            return returnURL
//        }
//
//        fillReportParam(model as mOP)
//
//        val m = model as mBNGRE
//
//        hmReportParam["report_period_begin_value"] = (hmColumnData[m.columnPeriodBeginValue] as DataDouble).doubleValue
//
//        return getReport()
//    }
//
//    override fun setPrintOptions() {
//        printPaperSize = PaperSize.A4
//
//        printPageOrientation = PageOrientation.PORTRAIT
//
//        printMarginLeft = 20
//        printMarginRight = 10
//        printMarginTop = 10
//        printMarginBottom = 10
//
//        printKeyX = 0.0
//        printKeyY = 0.0
//        printKeyW = 1.0
//        printKeyH = 2.0
//    }
//
//    override fun postReport(sheet: WritableSheet) {
//
//        //--- загрузка стартовых параметров
//        val reportObject = hmReportParam["report_object"] as Int
//        val (begTime, endTime) = getBegEndTimeFromParam()
//        val periodBeginValue = hmReportParam["report_period_begin_value"] as Double
//
//        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, reportObject)
//        val objectCalc = ObjectCalc.calcObject(conn, userConfig, objectConfig, begTime, endTime)
//
//        defineFormats(8, 2, 0)
//        var offsY = fillReportTitleWithTime(sheet)
//
//        offsY = fillReportHeader(objectConfig, sheet, offsY)
//
//        offsY = defineSummaryReportHeadersBNGRE(sheet, offsY)
//
//        var countNN = 1
//
//        //--- первая строка: порядковый номер и наименование объекта
//        sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
//        offsY = addGroupTitle(sheet, offsY, objectConfig.name)
//        offsY = outRow(
//            sheet = sheet,
//            aOffsY = offsY,
//            objectCalc = objectCalc,
//            periodBeginValue,
//        )
//
//        offsY++
//
//        sheet.addCell(Label(4, offsY, getPreparedAt(), wcfCellL))
//    }
//
//    private fun defineSummaryReportHeadersBNGRE(sheet: WritableSheet, aOffsY: Int): Int {
//        var offsY = aOffsY
//        val alDim = ArrayList<Int>()
//
//        offsY++
//
//        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
//        //--- setting the sizes of headers (total width = 140 for A4-landscape margins of 10 mm)
//        alDim.add(5)    // row no
//        alDim.add(22)   // name
//        alDim.add(10)   // serial No
//        alDim.add(9)    // beg date
//        alDim.add(31)   // time = 14 + 3 + 14
//        alDim.add(11)   // density = 4 + 3 + 4
//        alDim.add(8)    // in
//        alDim.add(8)    // out  (7 - недостаточно для чисел типа "-140 000")
//        // 104 (14 - лишних для 90, 36 - осталось для 140)
//
//        for (i in alDim.indices) {
//            val cvNN = CellView()
//            cvNN.size = alDim[i] * 256
//            sheet.setColumnView(i, cvNN)
//        }
//        return offsY
//    }
//
//    private fun outRow(
//        sheet: WritableSheet,
//        aOffsY: Int,
//        objectCalc: ObjectCalc,
//        periodBeginValue: Double,
//    ): Int {
//        var offsY = aOffsY
//
//        sheet.addCell(Label(5, offsY, "Остаток на начало периода", wcfTitleName))
//        sheet.addCell(
//            Label(
//                6,
//                offsY,
//                getSplittedDouble(periodBeginValue, ObjectCalc.getPrecision(periodBeginValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
//                wcfCellC
//            )
//        )
//        offsY++
//
//        //--- report on liquid/fuel using
//        sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
//        sheet.addCell(Label(2, offsY, "Номер", wcfCaptionHC))
//        sheet.addCell(Label(3, offsY, "Дата", wcfCaptionHC))
//        sheet.addCell(Label(4, offsY, "Время", wcfCaptionHC))
//        sheet.addCell(Label(5, offsY, "Плотность", wcfCaptionHC))
//        sheet.addCell(Label(6, offsY, "Приход", wcfCaptionHC))
//        sheet.addCell(Label(7, offsY, "Расход", wcfCaptionHC))
//        offsY++
//
//        var sumIn = 0.0
//        var sumOut = 0.0
//
//        objectCalc.tmCounterData.filter { (_, counterData) ->
//            counterData.scc.sensorType == SensorConfig.SENSOR_MASS_ACCUMULATED
//        }.forEach { (counterDescr, counterData) ->
//            sheet.addCell(Label(1, offsY, counterDescr, wcfCellC))
//            sheet.addCell(Label(2, offsY, counterData.scc.serialNo, wcfCellC))
//            sheet.addCell(Label(3, offsY, DateTime_DMY(arrayOf(counterData.scc.begYe, counterData.scc.begMo, counterData.scc.begDa)), wcfCellC))
//
//            var workTime = ""
//            counterData.alWorkOnOff.forEach { apd ->
//                workTime += if (workTime.isNotEmpty()) {
//                    "\n"
//                } else {
//                    ""
//                } +
//                    DateTime_YMDHMS(zoneId, apd.begTime) + " - " + DateTime_YMDHMS(zoneId, apd.endTime)
//            }
//            sheet.addCell(Label(4, offsY, workTime, wcfCellC))
//
//            val density = counterData.density?.alGLD?.let { alGLD ->
//                if (alGLD.isNotEmpty()) {
//                    getSplittedDouble(alGLD.first().y, ObjectCalc.getPrecision(alGLD.first().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) +
//                        " - " +
//                        getSplittedDouble(alGLD.last().y, ObjectCalc.getPrecision(alGLD.last().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//
//                } else {
//                    "-"
//                }
//            } ?: "-"
//            sheet.addCell(Label(5, offsY, density, wcfCellC))
//
//            val sValue = getSplittedDouble(counterData.value, ObjectCalc.getPrecision(counterData.value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
//            if (counterData.scc.inOutType == SensorConfigCounter.CALC_TYPE_IN) {
//                sheet.addCell(Label(6, offsY, sValue, wcfCellC))
//                sumIn += counterData.value
//            } else {
//                sheet.addCell(Label(7, offsY, sValue, wcfCellC))
//                sumOut += counterData.value
//            }
//            offsY++
//        }
//        sheet.addCell(Label(5, offsY, "ИТОГО:", wcfCaptionHC))
//        sheet.addCell(Label(6, offsY, getSplittedDouble(sumIn, ObjectCalc.getPrecision(sumIn), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
//        sheet.addCell(Label(7, offsY, getSplittedDouble(sumOut, ObjectCalc.getPrecision(sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
//        offsY++
//
//        sheet.addCell(Label(5, offsY, "Остаток на конец периода", wcfTitleName))
//        sheet.addCell(
//            Label(
//                7,
//                offsY,
//                getSplittedDouble(periodBeginValue + sumIn - sumOut, ObjectCalc.getPrecision(periodBeginValue + sumIn - sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
//                wcfCellC
//            )
//        )
//        offsY++
//
//        return offsY
//    }
//
//}

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

class cDayWorkBNGRE : cAbstractPeriodSummary() {

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
            offsY = addGroupTitle2(sheet, offsY, DateTime_DMY(zdtCurBeg))
            val (newOffsY, newPeriodBeginValue) = outRow(
                sheet = sheet,
                aOffsY = offsY,
                objectCalc = objectCalc,
                periodBeginValue = curPeriodBeginValue,
            )
            offsY = newOffsY
            curPeriodBeginValue = newPeriodBeginValue

            zdtCurBeg = zdtCurBeg.plus(1, ChronoUnit.DAYS)
            offsY++
        }
        offsY++

        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
    }

    private fun addGroupTitle2(sheet: WritableSheet, aOffsY: Int, title: String): Int {
        var offsY = aOffsY
        sheet.addCell(Label(1, offsY, title, wcfCellCB))
        sheet.mergeCells(1, offsY, 4, offsY + 2)
        offsY += 4
        return offsY
    }

    private fun outRow(
        sheet: WritableSheet,
        aOffsY: Int,
        objectCalc: ObjectCalc,
        periodBeginValue: Double,
    ): Pair<Int, Double> {
        var offsY = aOffsY

        sheet.addCell(Label(2, offsY, "Остаток на начало периода", wcfTitleName))
        sheet.addCell(
            Label(
                3,
                offsY,
                getSplittedDouble(periodBeginValue, ObjectCalc.getPrecision(periodBeginValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                wcfCellC
            )
        )
        sheet.mergeCells(3, offsY, 4, offsY)
        offsY++

        //--- report on liquid/fuel using
        sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Плотность", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Приход", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Расход", wcfCaptionHC))
        offsY++

        var sumIn = 0.0
        var sumOut = 0.0

        objectCalc.tmCounterData.filter { (_, counterData) ->
            counterData.scc.sensorType == SensorConfig.SENSOR_MASS_ACCUMULATED
        }.forEach { (counterDescr, counterData) ->
            sheet.addCell(Label(1, offsY, counterDescr, wcfCellC))

            val density = counterData.density?.alGLD?.let { alGLD ->
                if (alGLD.isNotEmpty()) {
                    getSplittedDouble(alGLD.first().y, ObjectCalc.getPrecision(alGLD.first().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) +
                        " - " +
                        getSplittedDouble(alGLD.last().y, ObjectCalc.getPrecision(alGLD.last().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)

                } else {
                    "-"
                }
            } ?: "-"
            sheet.addCell(Label(2, offsY, density, wcfCellC))

            val sValue = getSplittedDouble(counterData.value, ObjectCalc.getPrecision(counterData.value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            if (counterData.scc.inOutType == SensorConfigCounter.CALC_TYPE_IN) {
                sheet.addCell(Label(3, offsY, sValue, wcfCellC))
                sumIn += counterData.value
            } else {
                sheet.addCell(Label(4, offsY, sValue, wcfCellC))
                sumOut += counterData.value
            }
            offsY++
        }
        sheet.addCell(Label(2, offsY, "ИТОГО:", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, getSplittedDouble(sumIn, ObjectCalc.getPrecision(sumIn), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
        sheet.addCell(Label(4, offsY, getSplittedDouble(sumOut, ObjectCalc.getPrecision(sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
        offsY++

        val restValue = periodBeginValue + sumIn - sumOut
        sheet.addCell(Label(2, offsY, "Остаток на конец периода", wcfTitleName))
        sheet.addCell(
            Label(
                3,
                offsY,
                getSplittedDouble(restValue, ObjectCalc.getPrecision(restValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                wcfCellC
            )
        )
        sheet.mergeCells(3, offsY, 4, offsY)
        offsY++

        return Pair(offsY, restValue)
    }

    private fun defineSummaryReportHeadersBNGRE(sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        val alDim = ArrayList<Int>()

        offsY++

        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
        alDim.add(5)    // row no
        alDim.add(58)   // name
        alDim.add(11)   // density = 4 + 3 + 4
        alDim.add(8)    // in
        alDim.add(8)    // out  (7 - недостаточно для чисел типа "-140 000")

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        return offsY
    }

}
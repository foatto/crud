package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataDouble
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cSummaryBNGRE : cStandartPeriodSummary() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        fillReportParam(model as mUODGP)

        val m = model as mSummaryBNGRE

        hmReportParam["report_period_begin_value"] = (hmColumnData[m.columnPeriodBeginValue] as DataDouble).doubleValue
//        fillReportParam(m.sos)

        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4

        printPageOrientation = PageOrientation.LANDSCAPE

        printMarginLeft = 10
        printMarginRight = 10
        printMarginTop = 20
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        //        int reportObjectUser = (Integer) hmReportParam.get( "report_object_user" );
        //        int reportObject = (Integer) hmReportParam.get( "report_object" );
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val periodBeginValue = hmReportParam["report_period_begin_value"] as Double
//        val reportOutGroupSum = hmReportParam["report_out_group_sum"] as Boolean

        val (begTime, endTime) = getBegEndTimeFromParam()

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeadersBNGRE(sheet, offsY)

        val allSumCollector = ReportSumCollector()
        var countNN = 1
        for (objectIndex in alobjectId.indices) {
            val objectConfig = alObjectConfig[objectIndex]
            val objectCalc = ObjectCalc.calcObject(stm, userConfig, objectConfig, begTime, endTime)

            allSumCollector.add(null, objectCalc)

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            offsY = addGroupTitle(sheet, offsY, objectConfig.name)

            offsY = outRow(
                sheet = sheet,
                aOffsY = offsY,
                objectCalc = objectCalc,
                periodBeginValue,
//                isOutGroupSum = reportOutGroupSum,
            )
        }
        offsY++

//        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
//        sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
//        offsY += 4
//
//        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)

        sheet.addCell(Label(4, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells(4, offsY, 7, offsY)
    }

    private fun defineSummaryReportHeadersBNGRE(sheet: WritableSheet, aOffsY: Int): Int {
        var offsY = aOffsY
        val alDim = ArrayList<Int>()

        offsY++

        //--- setting the sizes of headers (total width = 90 for A4-portrait margins of 10 mm)
        //--- setting the sizes of headers (total width = 140 for A4-landscape margins of 10 mm)
        alDim.add(5)    // row no
        alDim.add(22)   // name
        alDim.add(10)   // serial No
        alDim.add(9)    // beg date
        alDim.add(31)   // time = 14 + 3 + 14
        alDim.add(11)   // density = 4 + 3 + 4  
        alDim.add(7)    // in
        alDim.add(7)    // out
        // 102 (12 - лишних для 90, 38 - осталось для 140)

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        return offsY
    }

    private fun outRow(
        sheet: WritableSheet,
        aOffsY: Int,
        objectCalc: ObjectCalc,
        periodBeginValue: Double,
//        isOutGroupSum: Boolean,
    ): Int {
        var offsY = aOffsY

        sheet.addCell(Label(5, offsY, "Остаток на начало периода", wcfTitleName))
        sheet.addCell(
            Label(
                6,
                offsY,
                getSplittedDouble(periodBeginValue, ObjectCalc.getPrecision(periodBeginValue), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                wcfCellC
            )
        )
        offsY++

        //--- report on liquid/fuel using
        sheet.addCell(Label(1, offsY, "Наименование [ед.изм.]", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Номер", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Дата", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Время", wcfCaptionHC))
        sheet.addCell(Label(5, offsY, "Плотность", wcfCaptionHC))
        sheet.addCell(Label(6, offsY, "Приход", wcfCaptionHC))
        sheet.addCell(Label(7, offsY, "Расход", wcfCaptionHC))
        offsY++

        var sumIn = 0.0
        var sumOut = 0.0

        objectCalc.tmCounterData.filter { (_, counterData) ->
            counterData.scc.sensorType == SensorConfig.SENSOR_MASS_ACCUMULATED
        }.forEach { (counterDescr, counterData) ->
            sheet.addCell(Label(1, offsY, counterDescr, wcfCellC))
            sheet.addCell(Label(2, offsY, counterData.scc.serialNo, wcfCellC))
            sheet.addCell(Label(3, offsY, DateTime_DMY(arrayOf(counterData.scc.begYe, counterData.scc.begMo, counterData.scc.begDa)), wcfCellC))

            var workTime = ""
            counterData.alWorkOnOff.forEach { apd ->
                workTime = if (workTime.isNotEmpty()) {
                    "\n"
                } else {
                    ""
                } +
                    DateTime_YMDHMS(zoneId, apd.begTime) + " - " + DateTime_YMDHMS(zoneId, apd.endTime)
            }
            sheet.addCell(Label(4, offsY, workTime, wcfCellC))

            val density = counterData.density?.alGLD?.let { alGLD ->
                if (alGLD.isNotEmpty()) {
                    getSplittedDouble(alGLD.first().y, ObjectCalc.getPrecision(alGLD.first().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) +
                        " - " +
                        getSplittedDouble(alGLD.last().y, ObjectCalc.getPrecision(alGLD.last().y), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)

                } else {
                    "-"
                }
            } ?: "-"
            sheet.addCell(Label(5, offsY, density, wcfCellC))

            val sValue = getSplittedDouble(counterData.value, ObjectCalc.getPrecision(counterData.value), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider)
            if (counterData.scc.inOutType == SensorConfigCounter.CALC_TYPE_IN) {
                sheet.addCell(Label(6, offsY, sValue, wcfCellC))
                sumIn += counterData.value
            } else {
                sheet.addCell(Label(7, offsY, sValue, wcfCellC))
                sumOut += counterData.value
            }
            offsY++
        }
        sheet.addCell(Label(5, offsY, "ИТОГО:", wcfCaptionHC))
        sheet.addCell(Label(6, offsY, getSplittedDouble(sumIn, ObjectCalc.getPrecision(sumIn), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
        sheet.addCell(Label(7, offsY, getSplittedDouble(sumOut, ObjectCalc.getPrecision(sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCaptionHC))
        offsY++

        sheet.addCell(Label(5, offsY, "Остаток на конец периода", wcfTitleName))
        sheet.addCell(
            Label(
                7,
                offsY,
                getSplittedDouble(periodBeginValue + sumIn - sumOut, ObjectCalc.getPrecision(periodBeginValue + sumIn - sumOut), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                wcfCellC
            )
        )
        offsY++

//        //--- withdrawal of the amount for each amount group
//        if (isOutGroupSum && objectCalc.tmGroupSum.size > 1) {
//            objectCalc.tmGroupSum.forEach { (sumName, sumData) ->
//                sheet.addCell(Label(1, offsY, "ИТОГО по '$sumName':", wcfCellRBStdYellow))
//                offsY++
//
//                offsY = outGroupSum(sheet, offsY, sumData)
//                offsY++
//            }
//        }
//
//        sheet.addCell(Label(1, offsY, "ИТОГО:", wcfCellRBStdYellow))
//        offsY++
//
//        offsY = outGroupSum(sheet, offsY, objectCalc.allSumData)
//        offsY++

        return offsY
    }

}

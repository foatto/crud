package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core_server.app.server.data.DataBoolean
import foatto.mms.core_mms.calc.ObjectCalc
import jxl.write.Label
import jxl.write.WritableSheet

class cSummary : cStandartPeriodSummary() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        fillReportParam(model as mUODGP)

        val m = model as mSummary

        hmReportParam["report_out_temperature"] = (hmColumnData[m.columnOutTemperature] as DataBoolean).value
        hmReportParam["report_out_density"] = (hmColumnData[m.columnOutDensity] as DataBoolean).value

        fillReportParam(m.sos)

        return getReport()
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        //        int reportObjectUser = (Integer) hmReportParam.get( "report_object_user" );
        //        int reportObject = (Integer) hmReportParam.get( "report_object" );
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportOutTemperature = hmReportParam["report_out_temperature"] as Boolean
        val reportOutDensity = hmReportParam["report_out_density"] as Boolean

        val (begTime, endTime) = getBegEndTimeFromParam()

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeaders(sheet, offsY)

        val allSumCollector = ReportSumCollector()
        var countNN = 1
        for (objectIndex in alObjectID.indices) {
            val objectConfig = alObjectConfig[objectIndex]
            val objectCalc = ObjectCalc.calcObject(stm, userConfig, objectConfig, begTime, endTime)

            allSumCollector.add(null, objectCalc)

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            offsY = addGroupTitle(sheet, offsY, objectConfig.name)

            offsY = outRow(sheet, offsY, objectConfig, objectCalc, reportOutTemperature, reportOutDensity)
        }

        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
        sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
        offsY += 4

        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)

        outReportTrail(sheet, offsY)
    }
}

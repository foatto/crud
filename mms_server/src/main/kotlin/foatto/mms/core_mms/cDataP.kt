package foatto.mms.core_mms

import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.core_mms.report.cMMSReport
import foatto.mms.core_mms.report.mOP
import jxl.write.WritableSheet
import java.time.ZonedDateTime

class cDataP : cMMSReport() {

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val m = model as mOP

        val objectId = (hmColumnData[m.columnReportObject] as DataInt).intValue
        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
        val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime

        val begDT = ZonedDateTime.of(begDate.year, begDate.monthValue, begDate.dayOfMonth, begTime.hour, begTime.minute, 0, 0, zoneId).toEpochSecond().toInt()
        val endDT = ZonedDateTime.of(endDate.year, endDate.monthValue, endDate.dayOfMonth, endTime.hour, endTime.minute, 0, 0, zoneId).toEpochSecond().toInt()

        hmParentData["mms_object"] = objectId
        hmParentData["mms_data_beg"] = begDT
        hmParentData["mms_data_end"] = endDT

        return getParamURL(
            aAlias = "mms_data",
            aAction = AppAction.TABLE,
            aRefererId = null,
            aId = 0,
            aParentData = hmParentData,
            aParentUserId = null,
            aAltParams = null
        )
    }

    override fun setPrintOptions() {}

    override fun postReport(sheet: WritableSheet) {}
}
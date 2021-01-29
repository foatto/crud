package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.mms.core_mms.calc.ObjectCalc
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class cDayWork : cStandartPeriodSummary() {

    //----------------------------------------------------------------------------------------------

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mDayWork

        //--- выборка данных параметров для отчета
        fillReportParam(m.uodg)

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth

        hmReportParam["report_group_type"] = (hmColumnData[m.columnReportGroupType] as DataComboBox).intValue

        hmReportParam["report_out_temperature"] = (hmColumnData[m.columnOutTemperature] as DataBoolean).value
        hmReportParam["report_out_density"] = (hmColumnData[m.columnOutDensity] as DataBoolean).value

        fillReportParam(m.sos)

        return getReport()
    }

    override fun postReport(sheet: WritableSheet) {

        val tmDayWorkCalcResult = calcReport()

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportGroupType = hmReportParam["report_group_type"] as Int
        val reportOutTemperature = hmReportParam["report_out_temperature"] as Boolean
        val reportOutDensity = hmReportParam["report_out_density"] as Boolean
        var reportSumOnly = hmReportParam["report_sum_only"] as Boolean
        val reportSumUser = hmReportParam["report_sum_user"] as Boolean
        val reportSumObject = hmReportParam["report_sum_object"] as Boolean

        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
        if (tmDayWorkCalcResult.size > java.lang.Short.MAX_VALUE) reportSumOnly = true

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeaders(sheet, offsY)

        val allSumCollector = ReportSumCollector()
        val tmUserSumCollector = TreeMap<String, ReportSumCollector>()
        var daySumCollector: ReportSumCollector? = null

        var countNN = 1
        var lastDate = ""
        var lastObjectInfo = ""

        for (dwcr in tmDayWorkCalcResult.values) {

            //--- пропускаем строки с нулевыми/неизвестными пробегами, моточасами, уровнями, расходом жидкости и выработкой энергии
            if (dwcr.objectCalc.sGeoRun.isEmpty() &&
                dwcr.objectCalc.sWorkValue.isEmpty() &&
                dwcr.objectCalc.sLiquidLevelBeg.isEmpty() &&
                dwcr.objectCalc.sEnergoValue.isEmpty() &&
                dwcr.objectCalc.sLiquidUsingValue.isEmpty()
            ) continue

            val userName = getRecordUserName(dwcr.objectCalc.objectConfig.userId)
            val sumUser = tmUserSumCollector.getOrPut(userName) { ReportSumCollector() }
            sumUser.add(dwcr.objectCalc.objectConfig.name, dwcr.objectCalc)
            allSumCollector.add(null, dwcr.objectCalc)

            if (!reportSumOnly) {
                //--- вывод заголовков групп (дата или номер+модель а/м)
                if (reportGroupType == mDayWork.GROUP_BY_DATE) {
                    if (lastDate != dwcr.date) {
                        daySumCollector?.let {
                            offsY = outSumData(sheet, offsY, it.sumUser, false, null)
                        }
                        daySumCollector = ReportSumCollector()
                        offsY = addGroupTitle(sheet, offsY, dwcr.date)
                        lastDate = dwcr.date
                    }
                    daySumCollector!!.add(null, dwcr.objectCalc)
                } else {
                    if (lastObjectInfo != dwcr.objectCalc.objectConfig.name) {
                        offsY = addGroupTitle(sheet, offsY, dwcr.objectCalc.objectConfig.name)
                        lastObjectInfo = dwcr.objectCalc.objectConfig.name
                    }
                }
                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(1, offsY, if (reportGroupType == mDayWork.GROUP_BY_DATE) dwcr.objectCalc.objectConfig.name else dwcr.date, wcfCellLBStdYellow))
                sheet.mergeCells(1, offsY, getColumnCount(1), offsY)
                offsY += 2
                offsY = outRow(sheet, offsY, dwcr.objectCalc.objectConfig, dwcr.objectCalc, reportOutTemperature, reportOutDensity)
            }
        }
        daySumCollector?.let {
            offsY = outSumData(sheet, offsY, it.sumUser, false, null)
        }

        offsY = outObjectAndUserSum(sheet, offsY, reportSumUser, reportSumObject, tmUserSumCollector, allSumCollector)

        outReportTrail(sheet, offsY)
    }

    private fun calcReport(): TreeMap<String, DayWorkCalcResult> {

        val tmResult = TreeMap<String, DayWorkCalcResult>()

        //--- загрузка стартовых параметров
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val reportGroupType = hmReportParam["report_group_type"] as Int

        val zdtBeg = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0, 0, zoneId)
        val zdtEnd = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS)

        for (objectIndex in alObjectID.indices) {
            val objectConfig = alObjectConfig[objectIndex]

            var zdtCurBeg = ZonedDateTime.from(zdtBeg)
            while (zdtCurBeg.isBefore(zdtEnd)) {

                val zdtCurEnd = ZonedDateTime.from(zdtCurBeg).plus(1, ChronoUnit.DAYS)

                val crKey = StringBuilder().append(if (reportGroupType == mDayWork.GROUP_BY_DATE) zdtCurBeg.toEpochSecond().toInt() else objectConfig.name).append(if (reportGroupType == mDayWork.GROUP_BY_DATE) objectConfig.name else zdtCurBeg.toEpochSecond().toInt()).toString()

                //--- заполнение первой порции результатов
                val dwcr = DayWorkCalcResult(
                    DateTime_DMY(zdtCurBeg), ObjectCalc.calcObject(
                        stm, userConfig, objectConfig, zdtCurBeg.toEpochSecond().toInt(), zdtCurEnd.toEpochSecond().toInt()
                    )
                )
                tmResult[crKey] = dwcr
                zdtCurBeg = zdtCurBeg.plus(1, ChronoUnit.DAYS)
            }
        }
        return tmResult
    }

    private class DayWorkCalcResult(val date: String, val objectCalc: ObjectCalc)
}

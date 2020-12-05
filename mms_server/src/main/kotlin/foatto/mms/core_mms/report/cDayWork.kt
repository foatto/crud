package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
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
        if(returnURL != null) return returnURL

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

        hmReportParam["report_group_type"] = (hmColumnData[m.columnReportGroupType] as DataComboBox).value

        fillReportParam(m.sos)

        return getReport()
    }

    override fun postReport(sheet: WritableSheet) {

        val tmDayWorkCalcResult = calcReport()

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportGroupType = hmReportParam["report_group_type"] as Int
        var reportSumOnly = hmReportParam["report_sum_only"] as Boolean
        val reportSumUser = hmReportParam["report_sum_user"] as Boolean
        val reportSumObject = hmReportParam["report_sum_object"] as Boolean

        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
        if(tmDayWorkCalcResult.size > java.lang.Short.MAX_VALUE) reportSumOnly = true

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeaders(sheet, offsY)

        val allSumCollector = SumCollector()
        val tmUserSumCollector = TreeMap<String, SumCollector>()
        var daySumCollector: SumCollector? = null

        var countNN = 1
        var lastDate: String? = ""
        var lastObjectInfo = ""

        for(dwcr in tmDayWorkCalcResult.values) {
            //--- пропускаем строки с нулевыми/неизвестными пробегами, моточасами, уровнями, расходом жидкости и выработкой энергии
            if(dwcr.objectCalc.sbGeoRun.isEmpty() && dwcr.objectCalc.sbWorkTotal.isEmpty() && dwcr.objectCalc.sbLiquidLevelBeg.isEmpty() &&
                dwcr.objectCalc.sbLiquidUsingTotal.isEmpty() && dwcr.objectCalc.sbEnergoValue.isEmpty()
            ) continue

            val userName = getRecordUserName(dwcr.objectCalc.objectConfig.userID)

            var sumUser: SumCollector? = tmUserSumCollector[userName]
            if(sumUser == null) {
                sumUser = SumCollector()
                tmUserSumCollector[userName] = sumUser
            }
            sumUser.add(dwcr.objectCalc.objectConfig.name, 0, dwcr.objectCalc)
            allSumCollector.add(null, 0, dwcr.objectCalc)

            if(!reportSumOnly) {
                //--- вывод заголовков групп (дата или номер+модель а/м)
                if(reportGroupType == mDayWork.GROUP_BY_DATE) {
                    if(lastDate != dwcr.date) {
                        if(daySumCollector != null) offsY = outPeriodSum(sheet, offsY, daySumCollector)
                        daySumCollector = SumCollector()
                        offsY = addGroupTitle(sheet, offsY, dwcr.date)
                        lastDate = dwcr.date
                    }
                    daySumCollector!!.add(null, 0, dwcr.objectCalc)
                } else {
                    if (lastObjectInfo != dwcr.objectCalc.objectConfig.name) {
                        offsY = addGroupTitle(sheet, offsY, dwcr.objectCalc.objectConfig.name)
                        lastObjectInfo = dwcr.objectCalc.objectConfig.name
                    }
                }
                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(
                    Label(1, offsY, if (reportGroupType == mDayWork.GROUP_BY_DATE) dwcr.objectCalc.objectConfig.name else dwcr.date, wcfCellLBStdYellow)
                )
                sheet.mergeCells(1, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY)
                offsY += 2
                offsY = outRow(sheet, offsY, dwcr.objectCalc.objectConfig, dwcr.objectCalc)
            }
        }
        if(daySumCollector != null) offsY = outPeriodSum(sheet, offsY, daySumCollector)
        offsY += 2

        //--- вывод сумм
        if(reportSumUser) {
            sheet.addCell(Label(0, offsY, "ИТОГО по объектам и их владельцам", wcfCellCBStdYellow))
            sheet.mergeCells(0, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY + 2)
            offsY += 4

            for ((userName, sumUser) in tmUserSumCollector) {
                sheet.addCell(Label(0, offsY, userName, wcfCellLBStdYellow))
                sheet.mergeCells(0, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY)
                offsY += 2
                if (reportSumObject) {
                    val tmObjectSum = sumUser.tmSumObject
                    for ((objectInfo, objectSum) in tmObjectSum) {
                        sheet.addCell(Label(1, offsY, objectInfo, wcfCellLB))
                        offsY++

                        offsY = outSumData(sheet, offsY, objectSum, false)
                    }
                }

                sheet.addCell(Label(0, offsY, "ИТОГО по владельцу:", wcfCellLBStdYellow))
                sheet.mergeCells(0, offsY, 1, offsY)
                offsY++

                offsY = outSumData(sheet, offsY, sumUser.sumUser, true)

                //--- если выводятся суммы по объектам, добавим ещё одну (третью) пустую строчку между этой суммой и
                //--- следующим объектом следующего пользователя
                offsY += 2
            }
        }

        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
        sheet.mergeCells(0, offsY, if (isGlobalUseSpeed) 10 else if (isGlobalUsingCalc) 7 else 6, offsY + 2)
        offsY += 4

        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true)

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

        for(objectIndex in alObjectID.indices) {
            val objectConfig = alObjectConfig[objectIndex]

            var zdtCurBeg = ZonedDateTime.from(zdtBeg)
            while(zdtCurBeg.isBefore(zdtEnd)) {

                val zdtCurEnd = ZonedDateTime.from(zdtCurBeg).plus(1, ChronoUnit.DAYS)

                val crKey = StringBuilder().append(if(reportGroupType == mDayWork.GROUP_BY_DATE) zdtCurBeg.toEpochSecond().toInt() else objectConfig.name).append(if(reportGroupType == mDayWork.GROUP_BY_DATE) objectConfig.name else zdtCurBeg.toEpochSecond().toInt()).toString()

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

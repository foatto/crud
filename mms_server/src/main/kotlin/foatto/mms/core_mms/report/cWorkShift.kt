package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSBFromIterable
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.iMMSApplication
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class cWorkShift : cAbstractPeriodSummary() {

    private lateinit var alWSD: MutableList<WorkShiftData>

    //----------------------------------------------------------------------------------------------

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mWorkShift

        //--- выборка данных параметров для отчета
        hmReportParam["report_work_shift"] = (hmColumnData[m.columnWorkShift] as DataInt).value

        fillReportParam(m.uodg)

        hmReportParam["report_worker"] = (hmColumnData[m.columnWorker] as DataInt).value

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth

        hmReportParam["report_add_before"] = (hmColumnData[m.columnAddBefore] as DataInt).value
        hmReportParam["report_add_after"] = (hmColumnData[m.columnAddAfter] as DataInt).value

        hmReportParam["report_group_type"] = (hmColumnData[m.columnReportGroupType] as DataComboBox).value

        hmReportParam["report_out_temperature"] = (hmColumnData[m.columnOutTemperature] as DataBoolean).value
        hmReportParam["report_out_density"] = (hmColumnData[m.columnOutDensity] as DataBoolean).value

        fillReportParam(m.sos)

        return getReport()
    }

    override fun getReport(): String {
        //--- предварительно определим наличие гео-датчика -
        //--- от него зависят кол-во и формат выводимых данных и отчёта
        val reportWorkShift = hmReportParam["report_work_shift"] as Int
        val reportWorker = hmReportParam["report_worker"] as Int
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        val begTime = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0, 0, zoneId).toEpochSecond().toInt()
        val endTime = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS).toEpochSecond().toInt()

        val sbSQL = StringBuilder(" SELECT MMS_work_shift.object_id , ")
            .append(" MMS_work_shift.beg_dt , MMS_work_shift.end_dt , MMS_work_shift.beg_dt_fact , MMS_work_shift.end_dt_fact , ")
            .append(" MMS_work_shift.shift_no , MMS_worker.name ")
            .append(" FROM MMS_work_shift , MMS_worker ")
            .append(" WHERE MMS_work_shift.worker_id = MMS_worker.id ")
        val alObjectID = mutableListOf<Int>()
        alWSD = mutableListOf()

        //--- если указана рабочая смена/путевой лист, то загрузим только его
        if (reportWorkShift != 0) {
            sbSQL.append(" AND MMS_work_shift.id = ").append(reportWorkShift)
        } else {
            //--- если объект не указан, то загрузим полный список доступных объектов
            if (reportObject == 0) {
                loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
            } else {
                alObjectID.add(reportObject)
            }

            sbSQL.append(" AND MMS_work_shift.object_id IN ( ").append(getSBFromIterable(alObjectID, " , ")).append(" ) ")
                //--- пока будем брать путевки, только полностью входящие в требуемый диапазон
                .append(" AND MMS_work_shift.beg_dt >= ").append(begTime).append(" AND MMS_work_shift.end_dt <= ").append(endTime)
            //--- применим именно такое "обратное" условие, если потребуется перехватить все путевки,
            //--- хотя бы частично пересекающиеся с заданным временным диапазоном
            //.append( " AND beg_dt < " ).append( endTime / 1000 )
            //.append( " AND end_dt > " ).append( begTime / 1000 );
            if (reportWorker != 0) {
                sbSQL.append(" AND MMS_work_shift.worker_id = ").append(reportWorker)
            }
        }

        val rs = stm.executeQuery(sbSQL.toString())
        while (rs.next()) {
            val objectID = rs.getInt(1)
            //--- если список не был ранее заполнен (для варианта с явно указанным reportWorkShift)
            if (alObjectID.isEmpty()) alObjectID.add(objectID)
            alWSD.add(
                WorkShiftData(
                    objectID = objectID,
                    begTimeDoc = rs.getInt(2),
                    endTimeDoc = rs.getInt(3),
                    begTimeFact = rs.getInt(4),
                    endTimeFact = rs.getInt(5),
                    shiftNo = rs.getString(6),
                    workerName = rs.getString(7)
                )
            )
        }
        rs.close()

        //--- в отдельном цикле, т.к. будут открываться новые ResultSet'ы в этом же Statement'e
        for (objectID in alObjectID) {
            val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectID)
            defineGlobalFlags(oc)
        }

        return super.getReport()
    }

    override fun postReport(sheet: WritableSheet) {

        val tmWorkShiftCalcResult = calcReport()

        //--- загрузка стартовых параметров
        val reportWorkShift = hmReportParam["report_work_shift"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val reportGroupType = hmReportParam["report_group_type"] as Int
        val reportOutTemperature = hmReportParam["report_out_temperature"] as Boolean
        val reportOutDensity = hmReportParam["report_out_density"] as Boolean
        var reportSumOnly = hmReportParam["report_sum_only"] as Boolean
        val reportSumUser = hmReportParam["report_sum_user"] as Boolean
        val reportSumObject = hmReportParam["report_sum_object"] as Boolean

        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
        if (tmWorkShiftCalcResult.size > java.lang.Short.MAX_VALUE) reportSumOnly = true

        defineFormats(8, 2, 0)

        var offsY = 0
        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        if (reportWorkShift == 0) sheet.addCell(
            Label(
                1, offsY++, StringBuilder("за период с ").append(if (reportBegDay < 10) '0' else "").append(reportBegDay).append('.').append(if (reportBegMonth < 10) '0' else "")
                    .append(reportBegMonth).append('.').append(reportBegYear).append(" по ").append(if (reportEndDay < 10) '0' else "").append(reportEndDay).append('.')
                    .append(if (reportEndMonth < 10) '0' else "").append(reportEndMonth).append('.').append(reportEndYear).toString(), wcfTitleL
            )
        )

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeaders(sheet, offsY)

        val allSumCollector = ReportSumCollector()
        val tmUserSumCollector = TreeMap<String, ReportSumCollector>()
        var shiftSumCollector: ReportSumCollector? = null

        var countNN = 1
        var lastShift = ""
        var lastObjectInfo = ""

        for (wscr in tmWorkShiftCalcResult.values) {

            //--- пропускаем строки с нулевыми/неизвестными пробегами, моточасами, уровнями и расходом жидкости
            if (wscr.objectCalc.sGeoRun.isEmpty() &&
                wscr.objectCalc.sWorkValue.isEmpty() &&
                wscr.objectCalc.sLiquidLevelBeg.isEmpty() &&
                wscr.objectCalc.sLiquidUsingValue.isEmpty() &&
                wscr.objectCalc.sEnergoValue.isEmpty()
            ) continue

            val userName = getRecordUserName(wscr.objectCalc.objectConfig.userId)
            val sumUser = tmUserSumCollector.getOrPut(userName) { ReportSumCollector() }
            sumUser.add(wscr.objectCalc.objectConfig.name, wscr.objectCalc)
            allSumCollector.add(null, wscr.objectCalc)

            if (!reportSumOnly) {
                //--- вывод заголовков групп (дата или номер+модель а/м)
                if (reportGroupType == mWorkShift.GROUP_BY_DATE) {
                    if (lastShift != wscr.shift) {
                        if (shiftSumCollector != null) {
                            offsY = outSumData(sheet, offsY, shiftSumCollector.sumUser, false, null)
                        }
                        shiftSumCollector = ReportSumCollector()
                        offsY = addGroupTitle(sheet, offsY, wscr.shift)
                        lastShift = wscr.shift
                    }
                    shiftSumCollector!!.add(null, wscr.objectCalc)
                } else {
                    if (lastObjectInfo != wscr.objectCalc.objectConfig.name) {
                        offsY = addGroupTitle(sheet, offsY, wscr.objectCalc.objectConfig.name)
                        lastObjectInfo = wscr.objectCalc.objectConfig.name
                    }
                }
                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(1, offsY, if (reportGroupType == mWorkShift.GROUP_BY_DATE) wscr.objectCalc.objectConfig.name else wscr.shift, wcfCellLBStdYellow))
                sheet.mergeCells(1, offsY, getColumnCount(1), offsY)
                offsY += 2
                offsY = outRow(sheet, offsY, wscr.objectCalc.objectConfig, wscr.objectCalc, reportOutTemperature, reportOutDensity)
            }
        }
        if (shiftSumCollector != null) {
            offsY = outSumData(sheet, offsY, shiftSumCollector.sumUser, false, null)
        }

        offsY = outObjectAndUserSum(sheet, offsY, reportSumUser, reportSumObject, tmUserSumCollector, allSumCollector)

        outReportTrail(sheet, offsY)
    }

    private fun calcReport(): TreeMap<String, WorkShiftCalcResult> {

        val tmResult = TreeMap<String, WorkShiftCalcResult>()

        //--- загрузка стартовых параметров
        val reportAddBefore = hmReportParam["report_add_before"] as Int * 60
        val reportAddAfter = hmReportParam["report_add_after"] as Int * 60
        val reportGroupType = hmReportParam["report_group_type"] as Int

        for (wsd in alWSD) {
            val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, wsd.objectID)

            tmResult[StringBuilder().append(if (reportGroupType == mWorkShift.GROUP_BY_DATE) wsd.begTimeDoc else objectConfig.name)
                .append(if (reportGroupType == mWorkShift.GROUP_BY_DATE) objectConfig.name else wsd.begTimeDoc).toString()] =
                WorkShiftCalcResult(wsd, ObjectCalc.calcObject(stm, userConfig, objectConfig, wsd.begTimeDoc - reportAddBefore, wsd.endTimeDoc + reportAddAfter), zoneId)
        }
        return tmResult
    }

    private class WorkShiftData(
        val objectID: Int,
        val begTimeDoc: Int,
        val endTimeDoc: Int,
        val begTimeFact: Int,
        val endTimeFact: Int,
        val shiftNo: String,
        val workerName: String
    )

    private class WorkShiftCalcResult(val wsd: WorkShiftData, val objectCalc: ObjectCalc, zoneId: ZoneId) {

        var shift = ""

        init {
            val sb = StringBuilder()
            if (wsd.shiftNo.isNotEmpty()) sb.append("№ ").append(wsd.shiftNo).append(", ")
            if (wsd.workerName.isNotEmpty()) sb.append(wsd.workerName).append(", ")
            sb.append("с ").append(DateTime_DMYHMS(zoneId, wsd.begTimeDoc)).append(" по ").append(DateTime_DMYHMS(zoneId, wsd.endTimeDoc))

            shift = sb.toString()
        }
    }
}

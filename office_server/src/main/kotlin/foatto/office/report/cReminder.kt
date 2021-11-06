package foatto.office.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core_server.app.server.OtherOwnerData.getOtherOwner
import foatto.core_server.app.server.data.DataDate3Int
import foatto.office.mReminder
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime
import java.util.*

class cReminder : cOfficeReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val m = model as mP

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.month.value
        hmReportParam["report_beg_day"] = begDate.dayOfMonth
        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.month.value
        hmReportParam["report_end_day"] = endDate.dayOfMonth

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
        val alResult = calcReport()

        //--- загрузка стартовых параметров
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        defineFormats(8, 2, 0)

        var offsY = fillReportTitle(
            reportTitle = aliasConfig.descr,
            reportBegYear = reportBegYear,
            reportBegMonth = reportBegMonth,
            reportBegDay = reportBegDay,
            reportEndYear = reportEndYear,
            reportEndMonth = reportEndMonth,
            reportEndDay = reportEndDay,
            sheet = sheet,
            wcfTitle = wcfTitleL,
            offsX = 1,
            aOffsY = 0
        )
        offsY++

        //--- установка размеров заголовков (общая ширина = 140 для А4-ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim += 5 // "N п/п"
        alDim += 16 // "Тип/Действие"
        alDim += 14 // "Время (без секунд)"
        alDim += 35 // "Тема, Описание"
        alDim += 35 // "Контакт, Должность"
        alDim += 35 // "Компания, Город"
        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Действие", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Время", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Описание", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Контактное лицо", wcfCaptionHC))
        sheet.addCell(Label(5, offsY, "Предприятие", wcfCaptionHC))
        offsY++

        var countNN = 1
        for (rd in alResult) {
            sheet.addCell(Label(0, offsY, countNN++.toString(), wcfNN))
            //--- вырезаем секунды
            val sbTime = DateTime_DMYHMS(zoneId, rd.time)
            sheet.addCell(Label(1, offsY, mReminder.hmReminderName[rd.type], wcfCellC))
            sheet.addCell(Label(2, offsY, sbTime.substring(0, sbTime.length - 3), wcfCellC))
            sheet.addCell(Label(3, offsY, StringBuilder(rd.subj).append(",\n ").append(rd.descr).toString(), wcfCellL))
            sheet.addCell(Label(4, offsY, StringBuilder(rd.peopleName).append(",\n ").append(rd.peoplePost).toString(), wcfCellL))
            sheet.addCell(Label(5, offsY, StringBuilder(rd.companyName).append(",\n ").append(rd.cityName).toString(), wcfCellL))
            offsY++
        }
    }

    //----------------------------------------------------------------------------------------------------------------------

    private fun calcReport(): List<ReminderData> {
        val alResult = mutableListOf<ReminderData>()

        //--- загрузка стартовых параметров
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        val gcBeg = GregorianCalendar(reportBegYear, reportBegMonth - 1, reportBegDay)
        val gcEnd = GregorianCalendar(reportEndYear, reportEndMonth - 1, reportEndDay)
        gcEnd.add(GregorianCalendar.DAY_OF_MONTH, 1) // т.е. конец периода для dd2.mm.yyyy на самом деле == dd2+1.mm.yyyy 00:00

        val begTime = gcBeg.timeInMillis
        val endTime = gcEnd.timeInMillis

        var rs = stm.executeQuery(" SELECT id FROM SYSTEM_alias WHERE name = 'office_reminder' ")
        rs.next()
        val reminderAliasID = rs.getInt(1)
        rs.close()

        val hsObjectPermission = userConfig.userPermission["office_reminder"]!!
        val sb =
            """
                SELECT OFFICE_reminder.id , OFFICE_reminder.user_id , 
                       OFFICE_reminder.ye , OFFICE_reminder.mo , OFFICE_reminder.da , OFFICE_reminder.ho , OFFICE_reminder.mi , 
                       OFFICE_reminder.type , OFFICE_reminder.subj , OFFICE_reminder.descr , 
                       OFFICE_people.name , OFFICE_people.post , 
                       OFFICE_company.name , OFFICE_city.name 
                FROM OFFICE_reminder , OFFICE_people , OFFICE_company , OFFICE_city 
                WHERE OFFICE_reminder.people_id = OFFICE_people.id 
                AND OFFICE_people.company_id = OFFICE_company.id 
                AND OFFICE_company.city_id = OFFICE_city.id 
                AND OFFICE_reminder.id <> 0 
                AND OFFICE_reminder.in_archive = 0 
                ORDER BY OFFICE_reminder.type , OFFICE_reminder.ye , OFFICE_reminder.mo , OFFICE_reminder.da , OFFICE_reminder.ho , OFFICE_reminder.mi
            """
        val stmRS = conn.createStatement()
        rs = stmRS.executeQuery(sb)
        while (rs.next()) {
            val rID = rs.getInt(1)
            val uID = rs.getInt(2)
            val time = (ZonedDateTime.of(rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), 0, 0, zoneId).toEpochSecond() / 1000).toInt()
            //--- применяем именно conn-версию getOtherOwner, т.к. текущий Statement занят
            if (time in begTime..endTime &&
                checkPerm(
                    aUserConfig = userConfig,
                    aHsPermission = hsObjectPermission,
                    permName = PERM_TABLE,
                    recordUserID = getOtherOwner(conn, reminderAliasID, rID, uID, userConfig.userId)
                )
            ) alResult.add(
                ReminderData(
                    time = time,
                    type = rs.getInt(8),
                    subj = rs.getString(9),
                    descr = rs.getString(10),
                    peopleName = rs.getString(11),
                    peoplePost = rs.getString(12),
                    companyName = rs.getString(13),
                    cityName = rs.getString(14)
                )
            )
        }
        rs.close()
        stmRS.close()

        return alResult
    }

    private class ReminderData(
        var time: Int = 0,
        var type: Int = mReminder.REMINDER_TYPE_OTHER,
        var subj: String,
        var descr: String,
        var peopleName: String,
        var peoplePost: String,
        var companyName: String,
        var cityName: String,
    )
}

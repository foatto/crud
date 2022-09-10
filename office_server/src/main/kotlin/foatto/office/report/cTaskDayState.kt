package foatto.office.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.DateTime_YMDHMS
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class cTaskDayState : cOfficeReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val m = model as mUP

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_user"] = (hmColumnData[m.columnReportUser] as DataInt).intValue
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
        printPageOrientation = PageOrientation.PORTRAIT
        printMarginLeft = 20
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10
        printKeyX = 1.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        val reportUser = hmReportParam["report_user"] as Int
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val gcBeg = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0, 0, ZoneId.systemDefault())
        val gcEnd = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0, 0, ZoneId.systemDefault())

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
            offsX = 0,
            aOffsY = 0
        )
        offsY++

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim += 80   // Дата
        alDim += 5    // Просроченных
        alDim += 5    // Всего
        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "Дата", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Просроченных", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Всего", wcfCaptionHC))
        offsY++

        //--- между заголовком и первой строкой вставим пустую строку для красоты
        offsY++

        //--- прописывать ограничение по ye/mo/da полям в SQL-запросе очень громоздко,
        //--- будем брать все, а потом программно отсекать ненужные
        var sb =
            """
                SELECT SYSTEM_users.full_name , OFFICE_task_day_state.ye , OFFICE_task_day_state.mo , OFFICE_task_day_state.da , 
                       OFFICE_task_day_state.count_red , OFFICE_task_day_state.count_all 
                FROM OFFICE_task_day_state , SYSTEM_users 
                WHERE OFFICE_task_day_state.in_user_id = SYSTEM_users.id 
                AND OFFICE_task_day_state.out_user_id = ${userConfig.userId}
            """
        if (reportUser != 0) {
            sb += " AND OFFICE_task_day_state.in_user_id = $reportUser "
        }
        sb += " ORDER BY SYSTEM_users.full_name , OFFICE_task_day_state.ye , OFFICE_task_day_state.mo , OFFICE_task_day_state.da "

        val rs = conn.executeQuery(sb)
        var lastUserName = ""
        while (rs.next()) {
            val userName = rs.getString(1)
            val gc = ZonedDateTime.of(rs.getInt(2), rs.getInt(3), rs.getInt(4), 0, 0, 0, 0, ZoneId.systemDefault())
            val countRed = rs.getInt(5)
            val countAll = rs.getInt(6)

            //--- программный пропуск неподходящих дат
            if (gc.isBefore(gcBeg) || gc.isAfter(gcEnd)) {
                continue
            }
            //--- пошли данные по другому пользователю, выводим его имя
            if (userName != lastUserName) {
                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
                offsY++
                sheet.addCell(Label(0, offsY++, userName, wcfCellCBStdYellow))
                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
                offsY++
                lastUserName = userName
            }
            sheet.addCell(Label(0, offsY, DateTime_DMY(gc), wcfCellC))
            sheet.addCell(Label(1, offsY, countRed.toString(), if (countRed == 0) wcfCellC else wcfCellCRedStd))
            sheet.addCell(Label(2, offsY, countAll.toString(), wcfCellC))
            offsY++
        }
        rs.close()

    }
}

package foatto.office.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.DateTime_DMYHM
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataInt
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cTaskThread : cOfficeReport() {

    override fun isFormAutoClick() = if (getParentId("office_task_in") != null || getParentId("office_task_out") != null) {
        true
    } else {
        super.isFormAutoClick()
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val m = model as mTask

        //--- выборка данных параметров для отчета
        hmReportParam["report_task"] = (hmColumnData[m.columnReportTask] as DataInt).intValue

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
        val reportTask = hmReportParam["report_task"] as Int

        defineFormats(8, 2, 0)

        var offsY = 0

        //--- в алиасе написано "Распечатать поручение", не пойдёт как заголовок
        sheet.addCell(Label(1, offsY++, "Поручение", wcfTitleL))

        var rs = conn.executeQuery(" SELECT out_user_id , in_user_id , ye , mo , da , subj FROM OFFICE_task WHERE id = $reportTask ")
        if (rs.next()) {
            sheet.addCell(Label(1, offsY, "От кого:", wcfTitleName))
            sheet.addCell(Label(2, offsY, application.hmUserFullNames[rs.getInt(1)], wcfTitleValue))
            offsY++
            sheet.addCell(Label(1, offsY, "Кому:", wcfTitleName))
            sheet.addCell(Label(2, offsY, application.hmUserFullNames[rs.getInt(2)], wcfTitleValue))
            offsY++
            sheet.addCell(Label(1, offsY, "Срок исполнения:", wcfTitleName))
            sheet.addCell(Label(2, offsY, DateTime_DMY(arrayOf(rs.getInt(3), rs.getInt(4), rs.getInt(5), 0, 0, 0)), wcfTitleValue))
            offsY++
            sheet.addCell(Label(1, offsY, "Тема поручения:", wcfTitleName))
            sheet.addCell(Label(2, offsY, rs.getString(6), wcfTitleValue))
            offsY++
        }
        rs.close()

        offsY++

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(20)    // "Автор"
        alDim.add(14)    // "Дата/время" без секунд
        alDim.add(51)    // "Сообщение"

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Автор", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Время", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Сообщение", wcfCaptionHC))

        offsY++

        var countNN = 1
        rs = conn.executeQuery(
            " SELECT user_id , ye , mo , da , ho , mi , message FROM OFFICE_task_thread WHERE task_id = $reportTask ORDER BY ye DESC , mo DESC , da DESC , ho DESC , mi DESC "
        )
        while (rs.next()) {
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(1, offsY, application.hmUserFullNames[rs.getInt(1)], wcfCellL))
            sheet.addCell(Label(2, offsY, DateTime_DMYHM(arrayOf(rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), 0)), wcfCellC))
            sheet.addCell(Label(3, offsY, rs.getString(7), wcfCellL))

            offsY++
        }
        rs.close()
        offsY++

        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(1, offsY, 2, offsY)
    }

}

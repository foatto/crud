package foatto.office.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core_server.app.server.OtherOwnerData.getOtherOwner
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.lang.Integer.min
import java.time.ZonedDateTime
import java.util.*

class cTask : cOfficeReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val m = model as mUS

        hmReportParam["report_user"] = (hmColumnData[m.columnReportUser] as DataInt).intValue
        hmReportParam["report_sum_only"] = (hmColumnData[m.columnSumOnly] as DataBoolean).value

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
        printKeyW = 2.0
        printKeyH = 3.0
    }

    override fun postReport(sheet: WritableSheet) {
        val tmResult = calcReport()

        //--- загрузка стартовых параметров
        val reportSumOnly = hmReportParam["report_sum_only"] as Boolean

        defineFormats(8, 2, 0)

//        var offsY = 0
        var offsY = 1 // пропустим наверху строчку, чтобы водяной знак (красиво) уместился в нестандартном расположении

        sheet.addCell(Label(0, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(71) // Ф.И.О.
        alDim.add(5) // Просроченных
        alDim.add(5) // Всего
        alDim.add(9) // Срок первого поручения
        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "Ф.И.О.", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Просроченных", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Всего", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Срок первого поручения", wcfCaptionHC))
        offsY++

        //--- в подробном режиме вывода между заголовком и первой строкой вставим пустую строку для красоты
        if (!reportSumOnly) {
            offsY++
        }
        var sumRed = 0
        var sumAll = 0
        tmResult.values.forEach { td ->
            sheet.addCell(Label(0, offsY, td.userName, wcfCellLB))
            sheet.addCell(
                Label(
                    1,
                    offsY,
                    td.countRed.toString(),
                    if (td.countRed == 0) {
                        wcfCellCB
                    } else {
                        wcfCellCBRedStd
                    }
                )
            )
            sheet.addCell(Label(2, offsY, td.countAll.toString(), wcfCellCB))
            sheet.addCell(
                Label(
                    3,
                    offsY,
                    td.firstDate,
                    if (td.countRed == 0) {
                        wcfCellCB
                    } else {
                        wcfCellCBRedStd
                    }
                )
            )
            offsY++
            if (!reportSumOnly) {
                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
                offsY++
                for (taskStr in td.alTask) {
                    sheet.addCell(
                        Label(
                            0,
                            offsY,
                            taskStr,    //.replace('\n', ' '),
                            wcfCellL
                        )
                    )
                    //sheet.mergeCells(0, offsY, 3, offsY) - из-за этого глючит автоматическое расширение ячейки по содержимому
                    offsY++
                }
                //--- в начале и конце блока списка поручений по пользователю - по пустой строке разделителя
                offsY++
            }
            sumRed += td.countRed
            sumAll += td.countAll
        }
        //--- добавляем пустую строку только в режиме вывода только сумм, иначе лишняя строка получается
        if (reportSumOnly) {
            offsY++
        }
        sheet.addCell(Label(0, offsY, "ИТОГО:", wcfCellCB))
        sheet.addCell(Label(1, offsY, sumRed.toString(), wcfCellCBRedStd))
        sheet.addCell(Label(2, offsY, sumAll.toString(), wcfCellCB))
        offsY += 2
        sheet.addCell(Label(0, offsY, getPreparedAt(), wcfCellL))
    }

    //----------------------------------------------------------------------------------------------------------------------

    private fun calcReport(): SortedMap<String, TaskData> {
        val tmResult = sortedMapOf<String, TaskData>()

        //--- загрузка стартовых параметров
        val reportUser = hmReportParam["report_user"] as Int
        val reportSumOnly = hmReportParam["report_sum_only"] as Boolean

        var rs = stm.executeQuery(" SELECT id FROM SYSTEM_alias WHERE name = 'office_task_out' ")
        rs.next()
        val taskOutAliasID = rs.getInt(1)
        rs.close()

        val hsObjectPermission = userConfig.userPermission["office_task_out"]!!
        val toDay = ZonedDateTime.now(zoneId)
        val sb = """ 
            SELECT id , out_user_id , in_user_id , ye , mo , da , subj 
            FROM OFFICE_task 
            WHERE id <> 0 
            AND in_archive = 0
            """ +
            if (reportUser == 0) {
                " AND in_user_id <> 0 "
            } else {
                " AND in_user_id = $reportUser"
            } +
            " ORDER BY ye , mo , da "
        rs = stm.executeQuery(sb)
        while (rs.next()) {
            val rID: Int = rs.getInt(1)
            val uID: Int = rs.getInt(2)
            if (!checkPerm(
                    aUserConfig = userConfig,
                    aHsPermission = hsObjectPermission,
                    permName = PERM_TABLE,
                    recordUserID = getOtherOwner(
                        conn = conn,
                        aliasID = taskOutAliasID,
                        rowID = rID,
                        rowUserID = uID,
                        otherUserID = userConfig.userId
                    )
                )
            ) {
                continue
            }

            val userName = application.hmUserFullNames[rs.getInt(3)]!!
            val ye = rs.getInt(4)
            val mo = rs.getInt(5)
            val da = rs.getInt(6)
            val subj: String = rs.getString(7)
            val gc = ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId)
            val isRed = gc.isBefore(toDay)
            var td = tmResult[userName]
            if (td == null) {
                td = TaskData(userName, 0, 0, DateTime_DMY(gc))
                tmResult[userName] = td
            }
            if (isRed) {
                td.countRed++
            }
            td.countAll++
            if (!reportSumOnly) {
                td.alTask.add(DateTime_DMY(gc) + " - " + subj)
            }
        }
        rs.close()
        return tmResult
    }

    private class TaskData(
        var userName: String,
        var countRed: Int,
        var countAll: Int,
        var firstDate: String,
    ) {
        var alTask = mutableListOf<String>()
    }
}

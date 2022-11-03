package foatto.office.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
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

        val toDay = ZonedDateTime.now(zoneId)

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
                    DateTime_DMY(td.firstDate),
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
                td.alTask.forEach { (taskSubj, taskDate) ->
                    sheet.addCell(
                        Label(
                            0,
                            offsY,
                            taskSubj,    //.replace('\n', ' '),
                            wcfCellL
                        )
                    )
                    sheet.addCell(
                        Label(
                            3,
                            offsY,
                            DateTime_DMY(taskDate),
                            if (taskDate.isBefore(toDay)) {
                                wcfCellCRedStd
                            } else {
                                wcfCellC
                            }
                        )
                    )
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

        val forOutTasks = aliasConfig.name.endsWith("_out")

        //--- загрузка стартовых параметров
        val reportUser = hmReportParam["report_user"] as Int
        val reportSumOnly = hmReportParam["report_sum_only"] as Boolean

        val hsTaskPermission = if (forOutTasks) {
            userConfig.userPermission["office_task_out"]!!
        } else {
            userConfig.userPermission["office_task_in"]!!
        }
        val toDay = ZonedDateTime.now(zoneId)

        var sql =
            """ 
                SELECT id , out_user_id , in_user_id , ye , mo , da , subj 
                FROM OFFICE_task 
                WHERE id <> 0 
                AND in_archive = 0
            """
        if (forOutTasks) {
            sql += if (reportUser == 0) {
                " AND in_user_id <> 0 "
            } else {
                " AND in_user_id = $reportUser"
            }
        } else {
            sql += if (reportUser == 0) {
                " AND out_user_id <> 0 "
            } else {
                " AND out_user_id = $reportUser"
            }
        }
        sql += " ORDER BY ye , mo , da "

        val rs = conn.executeQuery(sql)
        while (rs.next()) {
            val id = rs.getInt(1)
            val outUserId = rs.getInt(2)
            val inUserId = rs.getInt(3)

            if (!checkPerm(
                    aUserConfig = userConfig,
                    aHsPermission = hsTaskPermission,
                    permName = PERM_TABLE,
                    recordUserId = if (forOutTasks) {
                        outUserId
                    } else {
                        inUserId
                    },
                )
            ) {
                continue
            }

            val userName = application.hmUserFullNames[if (forOutTasks) {
                inUserId
            } else {
                outUserId
            }]!!

            val ye = rs.getInt(4)
            val mo = rs.getInt(5)
            val da = rs.getInt(6)
            val subj: String = rs.getString(7)
            val gc = ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId)
            val isRed = gc.isBefore(toDay)
            var td = tmResult[userName]
            if (td == null) {
                td = TaskData(userName, 0, 0, gc)
                tmResult[userName] = td
            }
            if (isRed) {
                td.countRed++
            }
            td.countAll++
            if (!reportSumOnly) {
                td.alTask.add(Pair(subj, gc))
            }
        }
        rs.close()
        return tmResult
    }

    private class TaskData(
        var userName: String,
        var countRed: Int,
        var countAll: Int,
        var firstDate: ZonedDateTime,
    ) {
        var alTask = mutableListOf<Pair<String, ZonedDateTime>>()
    }
}

package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHM
import foatto.core.util.getDateTime
import foatto.core.util.getSBFromIterable
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cWorkShiftCompare : cMMSReport() {

    //----------------------------------------------------------------------------------------------

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mWorkShiftCompare

        //--- выборка данных параметров для отчета
        hmReportParam["report_work_shift"] = (hmColumnData[m.columnWorkShift] as DataInt).intValue

        fillReportParam(m.uodg)

        hmReportParam["report_worker"] = (hmColumnData[m.columnWorker] as DataInt).intValue

        val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth

        hmReportParam["report_time_type"] = (hmColumnData[m.columnTimeType] as DataComboBox).intValue
        hmReportParam["report_add_before"] = (hmColumnData[m.columnAddBefore] as DataInt).intValue
        hmReportParam["report_add_after"] = (hmColumnData[m.columnAddAfter] as DataInt).intValue

        hmReportParam["report_max_diff"] = (hmColumnData[m.columnMaxDiff] as DataInt).intValue
        hmReportParam["report_out_over_diff_only"] = (hmColumnData[m.columnOutOverDiffOnly] as DataBoolean).value
        hmReportParam["report_out_run_without_koef"] = (hmColumnData[m.columnOutRunWithoutKoef] as DataBoolean).value
        hmReportParam["report_sum_worker"] = (hmColumnData[m.columnSumWorker] as DataBoolean).value

        fillReportParam(m.sos)

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

        val tmWorkShiftCalcResult = calcReport()

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportTimeType = hmReportParam["report_time_type"] as Int
        val reportAddBefore = (hmReportParam["report_add_before"] as Int) * 60
        val reportAddAfter = (hmReportParam["report_add_after"] as Int) * 60
        val reportMaxDiff = hmReportParam["report_max_diff"] as Int
        val reportOutOverDiffOnly = hmReportParam["report_out_over_diff_only"] as Boolean
        val reportOutRunWithoutKoef = hmReportParam["report_out_run_without_koef"] as Boolean
        val reportSumWorker = hmReportParam["report_sum_worker"] as Boolean
        var reportSumOnly = hmReportParam["report_sum_only"] as Boolean
        val reportSumUser = hmReportParam["report_sum_user"] as Boolean
        val reportSumObject = hmReportParam["report_sum_object"] as Boolean

        //--- если отчет получается слишком длинный, то включаем режим вывода только сумм
        if (tmWorkShiftCalcResult.size > java.lang.Short.MAX_VALUE) reportSumOnly = true

        defineFormats(8, 2, 0)

        var offsY = fillReportTitleWithDate(sheet)
        offsY++

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        //--- выведем существенные параметры отчёта для информированности пользователя
        sheet.addCell(Label(4, offsY, "Используемое время:", wcfTitleName))
        sheet.addCell(
            Label(
                5, offsY, if (reportTimeType == mWorkShiftCompare.TIME_TYPE_DOC) "Заявленное"
                else if (reportTimeType == mWorkShiftCompare.TIME_TYPE_FACT) "Фактическое" else "Начало/окончание суток", wcfTitleValue
            )
        )
        offsY++
        sheet.addCell(Label(4, offsY, "Добавляется к началу [мин]:", wcfTitleName))
        sheet.addCell(Label(5, offsY, (reportAddBefore / 60).toString(), wcfTitleValue))
        offsY++
        sheet.addCell(Label(4, offsY, "Добавляется к концу [мин]:", wcfTitleName))
        sheet.addCell(Label(5, offsY, (reportAddAfter / 60).toString(), wcfTitleValue))
        offsY++
        sheet.addCell(Label(4, offsY, "Допустимое отклонение [%]:", wcfTitleName))
        sheet.addCell(Label(5, offsY, reportMaxDiff.toString(), wcfTitleValue))
        offsY++

        offsY++

        //--- установка размеров заголовков (общая ширина = 140 для А4 ландшафт поля по 10 мм)
        val alDim = ArrayList<Int>()
        alDim.add(5)  // "N п/п"

        alDim.add(7)  // "Пробег [км] - по п/л"
        alDim.add(7)  // "Пробег [км] - по контроллеру"
        alDim.add(7)  // "Пробег [км] - расхождение"
        alDim.add(6)  // "% расхождения"

        alDim.add(27)  // "Наименование"
        alDim.add(7)  // "Моточасы [ч] - по п/л"
        alDim.add(7)  // "Моточасы [ч] - по контроллеру"
        alDim.add(7)  // "Моточасы [ч] - расхождение"
        alDim.add(6)  // "% расхождения"

        alDim.add(27)  // "Наименование"
        alDim.add(7)  // "Расход - по п/л"
        alDim.add(7)  // "Расход - по контроллеру"
        alDim.add(7)  // "Расход - расхождение"
        alDim.add(6)  // "% расхождения"

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        var offsX = 0
        sheet.addCell(Label(offsX, offsY, "№ п/п", wcfCaptionHC))
        sheet.mergeCells(offsX, offsY, offsX, offsY + 1)
        offsX++

        sheet.addCell(Label(offsX, offsY, "Пробег [км]", wcfCaptionHC))
        sheet.mergeCells(offsX, offsY, offsX + 3, offsY)
        sheet.addCell(Label(offsX++, offsY + 1, "По п/л", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "По контр.", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "Расхож-дение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "%", wcfCaptionHC))

        sheet.addCell(Label(offsX, offsY, "Работа оборудования [час]", wcfCaptionHC))
        sheet.mergeCells(offsX, offsY, offsX + 4, offsY)
        sheet.addCell(Label(offsX++, offsY + 1, "Наименование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "По п/л", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "По контр.", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "Расхож-дение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "%", wcfCaptionHC))

        sheet.addCell(Label(offsX, offsY, "Расход топлива", wcfCaptionHC))
        sheet.mergeCells(offsX, offsY, offsX + 4, offsY)
        sheet.addCell(Label(offsX++, offsY + 1, "Наименование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "По п/л", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "По контр.", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "Расхож-дение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY + 1, "%", wcfCaptionHC))

        offsY += 2

        val allWorkShiftSumCollector = WorkShiftSumCollector()
        val allCalcSumCollector = WorkShiftSumCollector()
        val tmWorkShiftSumCollector = TreeMap<String, WorkShiftSumCollector>()
        val tmCalcSumCollector = TreeMap<String, WorkShiftSumCollector>()

        offsY++    // пропустим еще строку между шапкой и первой строкой отчёта
        var countNN = 1
        for (wscr in tmWorkShiftCalcResult.values) {
            val userName = getRecordUserName(wscr.objectConfig.userId)

            //--- неправильная строка:
            //--- 1. или путевка с нулевым пробегом и моточасами и расходом топлива
            //--- 2. или контроллер с нулевым пробегом и моточасами и расходом топлива (т.е. нерабочий контроллер).
            //--- такие строки выводим (для справки) серым цветом и в суммы не включаем.
            var isWrongRow = false
            if (!isWrongRow) {
                var isWorkShiftWork = false
                for (descr in wscr.wsd.tmWorkHour.keys) if (wscr.wsd.tmWorkHour[descr]!! > 0) {
                    isWorkShiftWork = true
                    break
                }
                var isWorkShiftLiquidUsing = false
                for (descr in wscr.wsd.tmLiquidUsing.keys) if (wscr.wsd.tmLiquidUsing[descr]!! > 0) {
                    isWorkShiftLiquidUsing = true
                    break
                }
                isWrongRow = wscr.wsd.run <= 0 && !isWorkShiftWork && !isWorkShiftLiquidUsing
            }
            if (!isWrongRow) {
                var isWorkShiftWork = false
                for (descr in wscr.tmWorkHour.keys) if (wscr.tmWorkHour[descr]!! > 0) {
                    isWorkShiftWork = true
                    break
                }
                var isWorkShiftLiquidUsing = false
                for (descr in wscr.tmLiquidUsing.keys) if (wscr.tmLiquidUsing[descr]!! > 0) {
                    isWorkShiftLiquidUsing = true
                    break
                }
                isWrongRow = wscr.runWK <= 0 && !isWorkShiftWork && !isWorkShiftLiquidUsing
            }

            //--- если задано выводить только строки с нарушениями, то пропускаем обычные/нормальные и wrong-строки
            if (reportOutOverDiffOnly && (isWrongRow || Math.abs(calcPercent(wscr.wsd.run, wscr.runWK)) <= reportMaxDiff)) continue

            var sumWorkShift: WorkShiftSumCollector? = tmWorkShiftSumCollector[userName]
            if (sumWorkShift == null) {
                sumWorkShift = WorkShiftSumCollector()
                tmWorkShiftSumCollector[userName] = sumWorkShift
            }
            var sumCalc: WorkShiftSumCollector? = tmCalcSumCollector[userName]
            if (sumCalc == null) {
                sumCalc = WorkShiftSumCollector()
                tmCalcSumCollector[userName] = sumCalc
            }

            //--- НЮАНС: считаем суммы ДО манипуляции со сравнением работы оборудования и расхода топлива
            //--- (т.к. в процессе сравнения они будут удалены из списка)
            if (!isWrongRow) {
                allWorkShiftSumCollector.add(null, null, wscr.wsd.run, wscr.wsd.run, wscr.wsd.tmWorkHour, wscr.wsd.tmLiquidUsing)
                sumWorkShift.add(wscr.objectConfig.name, wscr.wsd.workerName, wscr.wsd.run, wscr.wsd.run, wscr.wsd.tmWorkHour, wscr.wsd.tmLiquidUsing)
                allCalcSumCollector.add(null, null, wscr.runWK, wscr.runWOK, wscr.tmWorkHour, wscr.tmLiquidUsing)
                sumCalc.add(wscr.objectConfig.name, wscr.wsd.workerName, wscr.runWK, wscr.runWOK, wscr.tmWorkHour, wscr.tmLiquidUsing)
            }

            if (!reportSumOnly) {
                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                sheet.mergeCells(0, offsY, 0, offsY + 1)
                sheet.addCell(Label(1, offsY, DateTime_DMYHM(zoneId, wscr.wsd.begTime), wcfCellC))
                sheet.mergeCells(1, offsY, 2, offsY)
                sheet.addCell(Label(3, offsY, DateTime_DMYHM(zoneId, wscr.wsd.endTime), wcfCellC))
                sheet.mergeCells(3, offsY, 4, offsY)
                sheet.addCell(Label(5, offsY, wscr.objectConfig.name, wcfCellC))
                sheet.addCell(Label(6, offsY, wscr.wsd.workerName, wcfCellC))
                sheet.mergeCells(6, offsY, 9, offsY)
                sheet.addCell(Label(10, offsY, wscr.wsd.shiftNo, wcfCellC))

                offsY++

                outRow(
                    sheet, 1, offsY, reportMaxDiff.toDouble(), isWrongRow,
                    reportOutRunWithoutKoef, wscr.wsd.run, wscr.runWK, wscr.runWOK,
                    wscr.wsd.tmWorkHour, wscr.tmWorkHour, wscr.wsd.tmLiquidUsing, wscr.tmLiquidUsing
                )

                offsY += 2
            }
        }

        //--- вывод сумм -----------------------------------------------------------------------------------------------

        if (reportSumUser) {
            for (userName in tmWorkShiftSumCollector.keys) {
                val sumWorkShift = tmWorkShiftSumCollector[userName]!!
                val sumCalc = tmCalcSumCollector[userName]!!
                if (reportSumWorker) {
                    val tmDriverW = sumWorkShift.tmSumWorker
                    val tmDriverC = sumCalc.tmSumWorker
                    for (workerName in tmDriverW.keys) {
                        val sumW = tmDriverW[workerName]!!
                        val sumC = tmDriverC[workerName]!!
                        sheet.addCell(Label(6, offsY, workerName, wcfCellLBStdYellow))
                        sheet.mergeCells(6, offsY, 10, offsY)
                        offsY++
                        outRow(
                            sheet, 1, offsY, reportMaxDiff.toDouble(), false, reportOutRunWithoutKoef, sumW.runWK, sumC.runWK, sumC.runWOK,
                            sumW.tmWorkHour, sumC.tmWorkHour, sumW.tmLiquidUsing, sumC.tmLiquidUsing
                        )
                        offsY++
                    }
                }
                if (reportSumObject) {
                    val tmObjectW = sumWorkShift.tmSumObject
                    val tmObjectC = sumCalc.tmSumObject
                    for (objectName in tmObjectW.keys) {
                        val sumW = tmObjectW[objectName]!!
                        val sumC = tmObjectC[objectName]!!
                        sheet.addCell(Label(5, offsY, objectName, wcfCellLBStdYellow))
                        sheet.mergeCells(5, offsY, 9, offsY)
                        offsY++
                        outRow(
                            sheet, 1, offsY, reportMaxDiff.toDouble(), false, reportOutRunWithoutKoef, sumW.runWK, sumC.runWK, sumC.runWOK,
                            sumW.tmWorkHour, sumC.tmWorkHour, sumW.tmLiquidUsing, sumC.tmLiquidUsing
                        )
                        offsY++
                    }
                }
                sheet.addCell(Label(2, offsY, userName, wcfCellLBStdYellow))
                sheet.mergeCells(2, offsY, 5, offsY)
                offsY++

                val sumUserW = sumWorkShift.sumUser
                val sumUserC = sumCalc.sumUser
                outRow(
                    sheet, 1, offsY, reportMaxDiff.toDouble(), false, reportOutRunWithoutKoef, sumUserW.runWK, sumUserC.runWK, sumUserC.runWOK,
                    sumUserW.tmWorkHour, sumUserC.tmWorkHour, sumUserW.tmLiquidUsing, sumUserC.tmLiquidUsing
                )
                offsY++
            }
        }

        offsY++
        sheet.addCell(Label(1, offsY, "ИТОГО:", wcfCellLBStdYellow))
        sheet.mergeCells(1, offsY, 2, offsY)
        offsY++
        val sumAllW = allWorkShiftSumCollector.sumUser
        val sumAllC = allCalcSumCollector.sumUser
        outRow(
            sheet, 1, offsY, reportMaxDiff.toDouble(), false, reportOutRunWithoutKoef, sumAllW.runWK, sumAllC.runWK, sumAllC.runWOK,
            sumAllW.tmWorkHour, sumAllC.tmWorkHour, sumAllW.tmLiquidUsing, sumAllC.tmLiquidUsing
        )

        offsY += 2
        sheet.addCell(Label(11, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(11, offsY, 14, offsY)
    }

    private fun outRow(
        sheet: WritableSheet, startX: Int, offsY: Int, reportMaxDiff: Double, isWrongRow: Boolean,
        reportOutRunWithoutKoef: Boolean, wRun: Double, cRunWK: Double, cRunWOK: Double,
        tmWorkHourW: TreeMap<String, Double>, tmWorkHourC: TreeMap<String, Double>,
        tmLiquidUsingW: TreeMap<String, Double>, tmLiquidUsingC: TreeMap<String, Double>
    ) {

        val percentWK = calcPercent(wRun, cRunWK)
        val percentWOK = calcPercent(wRun, cRunWOK)

        val isOutWOK = reportOutRunWithoutKoef && cRunWK != cRunWOK
        val isOverRun = Math.abs(percentWK) > reportMaxDiff
        val runCellStyle1 = if (isWrongRow) wcfCellRGrayStd else if (isOverRun) wcfCellRRedStd else wcfCellR
        val runCellStyle2 = if (isWrongRow) wcfCellRGrayStd else if (isOverRun) wcfCellRBRedStd else wcfCellR

        sheet.addCell(Label(startX, offsY, getSplittedDouble(wRun, 1).toString(), runCellStyle1))
        sheet.addCell(
            Label(
                startX + 1, offsY,
                getSplittedDouble(cRunWK, 1).toString() + if (isOutWOK) "\n(" + getSplittedDouble(cRunWOK, 1).toString() + ")" else "",
                runCellStyle1
            )
        )
        sheet.addCell(
            Label(
                startX + 2, offsY,
                getSplittedDouble(wRun - cRunWK, 1).toString() + if (isOutWOK) "\n(" + getSplittedDouble(wRun - cRunWOK, 1).toString() + ")" else "",
                runCellStyle2
            )
        )
        sheet.addCell(
            Label(
                startX + 3, offsY,
                getSplittedDouble(percentWK, 1).toString() + if (isOutWOK) "\n(" + getSplittedDouble(percentWOK, 1).toString() + ")" else "",
                runCellStyle2
            )
        )

        val sbWorkDescr = StringBuilder()
        val sbWorkShiftWork = StringBuilder()
        val sbCalcWork = StringBuilder()
        val sbDiffWorkValue = StringBuilder()
        val sbDiffWorkPercent = StringBuilder()
        var isRed = false
        //--- проход по списку оборудования из путевки с одновременным сравнением по списку расчетного оборудования
        for (workDescr in tmWorkHourW.keys) {
            if (sbWorkDescr.isNotEmpty()) {
                sbWorkDescr.append('\n')
                sbWorkShiftWork.append('\n')
                sbCalcWork.append('\n')
                sbDiffWorkValue.append('\n')
                sbDiffWorkPercent.append('\n')
            }
            val wWorkHour = tmWorkHourW[workDescr]!!
            sbWorkDescr.append(workDescr)
            sbWorkShiftWork.append(getSplittedDouble(wWorkHour, 1))

            val cWorkHour = tmWorkHourC[workDescr]
            if (cWorkHour == null || cWorkHour < 0) {
                sbCalcWork.append('-')
                sbDiffWorkValue.append('-')
                sbDiffWorkPercent.append('-')
                isRed = true
            } else {
                sbCalcWork.append(getSplittedDouble(cWorkHour, 1))
                sbDiffWorkValue.append(getSplittedDouble(wWorkHour - cWorkHour, 1))
                sbDiffWorkPercent.append(
                    if (cWorkHour == 0.0) "100"
                    else getSplittedDouble((wWorkHour - cWorkHour) / cWorkHour * 100.0, 1)
                )
                isRed = isRed or (cWorkHour == 0.0 || Math.abs(wWorkHour - cWorkHour) / cWorkHour * 100.0 > reportMaxDiff)
            }
            tmWorkHourC.remove(workDescr)
        }
        //--- проход остатку списка расчетного оборудования
        for (workDescr in tmWorkHourC.keys) {
            if (sbWorkDescr.isNotEmpty()) {
                sbWorkDescr.append('\n')
                sbWorkShiftWork.append('\n')
                sbCalcWork.append('\n')
                sbDiffWorkValue.append('\n')
                sbDiffWorkPercent.append('\n')
            }
            val cWorkHour = tmWorkHourC[workDescr]!!
            sbWorkDescr.append(workDescr)
            sbWorkShiftWork.append('-')
            sbCalcWork.append(getSplittedDouble(cWorkHour, 1))
            sbDiffWorkValue.append('-')
            sbDiffWorkPercent.append('-')
            isRed = true
        }
        sheet.addCell(Label(startX + 4, offsY, sbWorkDescr.toString(), if (isWrongRow) wcfCellCGrayStd else if (isRed) wcfCellCRedStd else wcfCellC))
        sheet.addCell(Label(startX + 5, offsY, sbWorkShiftWork.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellR))
        sheet.addCell(Label(startX + 6, offsY, sbCalcWork.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellR))
        sheet.addCell(Label(startX + 7, offsY, sbDiffWorkValue.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellRB))
        sheet.addCell(Label(startX + 8, offsY, sbDiffWorkPercent.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRBRedStd else wcfCellRB))

        val sbLiquidDescr = StringBuilder()
        val sbWorkShiftLiquidUsing = StringBuilder()
        val sbCalcLiquidUsing = StringBuilder()
        val sbDiffLiquidUsingValue = StringBuilder()
        val sbDiffLiquidUsingPercent = StringBuilder()
        isRed = false
        //--- проход по списку расхода топлива из путевки с одновременным сравнением по списку расчетного расхода топлива
        for (liquidDescr in tmLiquidUsingW.keys) {
            if (sbLiquidDescr.isNotEmpty()) {
                sbLiquidDescr.append('\n')
                sbWorkShiftLiquidUsing.append('\n')
                sbCalcLiquidUsing.append('\n')
                sbDiffLiquidUsingValue.append('\n')
                sbDiffLiquidUsingPercent.append('\n')
            }
            val wLiquidUsing = tmLiquidUsingW[liquidDescr]!!
            sbLiquidDescr.append(liquidDescr)
            sbWorkShiftLiquidUsing.append(getSplittedDouble(wLiquidUsing, 1))

            val cLiquidUsing = tmLiquidUsingC[liquidDescr]
            if (cLiquidUsing == null || cLiquidUsing < 0) {
                sbCalcLiquidUsing.append('-')
                sbDiffLiquidUsingValue.append('-')
                sbDiffLiquidUsingPercent.append('-')
                isRed = true
            } else {
                sbCalcLiquidUsing.append(getSplittedDouble(cLiquidUsing, 1))
                sbDiffLiquidUsingValue.append(getSplittedDouble(wLiquidUsing - cLiquidUsing, 1))
                sbDiffLiquidUsingPercent.append(
                    if (cLiquidUsing == 0.0) "100"
                    else getSplittedDouble((wLiquidUsing - cLiquidUsing) / cLiquidUsing * 100.0, 1)
                )
                isRed = isRed or (cLiquidUsing == 0.0 || Math.abs(wLiquidUsing - cLiquidUsing) / cLiquidUsing * 100.0 > reportMaxDiff)
            }
            tmLiquidUsingC.remove(liquidDescr)
        }
        //--- проход остатку списка расчётного оборудования
        for (liquidDescr in tmLiquidUsingC.keys) {
            if (sbLiquidDescr.isNotEmpty()) {
                sbLiquidDescr.append('\n')
                sbWorkShiftLiquidUsing.append('\n')
                sbCalcLiquidUsing.append('\n')
                sbDiffLiquidUsingValue.append('\n')
                sbDiffLiquidUsingPercent.append('\n')
            }
            val cLiquidUsing = tmLiquidUsingC[liquidDescr]!!
            sbLiquidDescr.append(liquidDescr)
            sbWorkShiftLiquidUsing.append('-')
            sbCalcLiquidUsing.append(getSplittedDouble(cLiquidUsing, 1))
            sbDiffLiquidUsingValue.append('-')
            sbDiffLiquidUsingPercent.append('-')
            isRed = true
        }
        sheet.addCell(Label(startX + 9, offsY, sbLiquidDescr.toString(), if (isWrongRow) wcfCellCGrayStd else if (isRed) wcfCellCRedStd else wcfCellC))
        sheet.addCell(Label(startX + 10, offsY, sbWorkShiftLiquidUsing.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellR))
        sheet.addCell(Label(startX + 11, offsY, sbCalcLiquidUsing.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellR))
        sheet.addCell(Label(startX + 12, offsY, sbDiffLiquidUsingValue.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRRedStd else wcfCellRB))
        sheet.addCell(Label(startX + 13, offsY, sbDiffLiquidUsingPercent.toString(), if (isWrongRow) wcfCellRGrayStd else if (isRed) wcfCellRBRedStd else wcfCellRB))

    }

    private fun calcPercent(wRun: Double, cRun: Double): Double {
        return if (cRun == 0.0) if (wRun == 0.0) 0.0 else 100.0 else if (wRun == 0.0) -100.0 else (wRun - cRun) / wRun * 100.0
    }

    private fun calcReport(): TreeMap<String, WorkShiftCalcResult> {

        val tmResult = TreeMap<String, WorkShiftCalcResult>()

        //--- загрузка стартовых параметров
        val reportWorkShift = hmReportParam["report_work_shift"] as Int
        val reportWorker = hmReportParam["report_worker"] as Int
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportTimeType = hmReportParam["report_time_type"] as Int
        val reportAddBefore = (hmReportParam["report_add_before"] as Int) * 60
        val reportAddAfter = (hmReportParam["report_add_after"] as Int) * 60

        val (begTime, endTime) = getBegNextDayFromParam()

        val alWSD = ArrayList<WorkShiftData>()

        val sbSQL = StringBuilder(" SELECT MMS_work_shift.object_id , MMS_work_shift.id, MMS_work_shift.shift_no , ").append(" MMS_work_shift.beg_dt , MMS_work_shift.end_dt , ").append(" MMS_work_shift.beg_dt_fact , MMS_work_shift.end_dt_fact , ").append(" MMS_work_shift.run, MMS_worker.name ").append(" FROM MMS_work_shift , MMS_worker ").append(" WHERE MMS_work_shift.worker_id = MMS_worker.id ")
        //--- пропуск ремонтных путевок
        //.append( " AND in_repair = 0 " );

        //--- если указана рабочая смена/путевой лист, то загрузим только его
        if (reportWorkShift != 0) sbSQL.append(" AND MMS_work_shift.id = ").append(reportWorkShift)
        else {
            val alObjectID = ArrayList<Int>()
            //--- если объект не указан, то загрузим полный список доступных объектов
            if (reportObject == 0) cMMSReport.loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
            else alObjectID.add(reportObject)

            sbSQL.append(" AND MMS_work_shift.object_id IN ( ").append(getSBFromIterable(alObjectID, " , ")).append(" ) ")
                //--- пока будем брать путевки, только полностью входящие в требуемый диапазон
                //--- для выборки из диапазона используем только номинальное/документальное время начала/окончания путёвки
                .append(" AND MMS_work_shift.beg_dt >= ").append(begTime).append(" AND MMS_work_shift.end_dt <= ").append(endTime)
            //--- применим именно такое "обратное" условие, если потребуется перехватить все путевки,
            //--- хотя бы частично пересекающиеся с заданным временным диапазоном
            //.append( " AND beg_dt < " ).append( endTime )
            //.append( " AND end_dt > " ).append( begTime );
            if (reportWorker != 0) sbSQL.append(" AND MMS_work_shift.worker_id = ").append(reportWorker)
        }
        var rs = stm.executeQuery(sbSQL.toString())
        while (rs.next()) {
            val oID = rs.getInt(1)
            val sID = rs.getInt(2)
            val sNo = rs.getString(3)
            val begDoc = rs.getInt(4)
            val endDoc = rs.getInt(5)
            val begFact = rs.getInt(6)
            val endFact = rs.getInt(7)
            val run = rs.getDouble(8)
            val workerName = rs.getString(9)

            val begDT: Int
            val endDT: Int
            when (reportTimeType) {
                mWorkShiftCompare.TIME_TYPE_FACT -> {
                    begDT = begFact
                    endDT = endFact
                }

                mWorkShiftCompare.TIME_TYPE_DAY -> {
                    begDT = getDateTime(zoneId, begDoc).withHour(0).withMinute(0).withSecond(0).withNano(0).toEpochSecond().toInt()
                    endDT = getDateTime(zoneId, endDoc).withHour(0).withMinute(0).withSecond(0).withNano(0).toEpochSecond().toInt()
                }

                mWorkShiftCompare.TIME_TYPE_DOC -> {
                    begDT = begDoc
                    endDT = endDoc
                }
                else -> {
                    begDT = begDoc
                    endDT = endDoc
                }
            }
            alWSD.add(WorkShiftData(oID, sID, sNo, begDT, endDT, run, workerName))
        }
        rs.close()

        //--- "бумажная/документальная" информация по путевке
        for (wsd in alWSD) {
            rs = stm.executeQuery(" SELECT data_type, descr , data_value FROM MMS_work_shift_data WHERE shift_id = ${wsd.shiftID}")
            while (rs.next()) {
                when (rs.getInt(1)) {
                    SensorConfig.SENSOR_WORK -> wsd.tmWorkHour[rs.getString(2)] = rs.getDouble(3)
                    SensorConfig.SENSOR_LIQUID_USING -> wsd.tmLiquidUsing[rs.getString(2)] = rs.getDouble(3)
                }
            }
            rs.close()
        }

        for (wsd in alWSD) {
            val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, wsd.objectID)

            tmResult[StringBuilder().append(wsd.begTime).append(objectConfig.name).toString()] = WorkShiftCalcResult(wsd, ObjectCalc.calcObject(stm, userConfig, objectConfig, wsd.begTime - reportAddBefore, wsd.endTime + reportAddAfter))
        }
        return tmResult

        //                if( isDayWaybill && tmResult.containsKey( wcrKey ) ) {
        //                    wcr = tmResult.get( wcrKey );
        //                    wcr.driverName.append( '\n' ).append( waybillDriverName );
        //                    wcr.waybillNo.append( '\n' ).append( waybillNo );
        //                    wcr.waybillBeg = gcBegW.getTimeInMillis();
        //                    wcr.waybillEnd = gcEndW.getTimeInMillis();
        //                    wcr.waybillRun += waybillRun;
        //                    wcr.waybillEngineHour += waybillEngineHour;
        //                    wcr.waybillEquipHour += waybillEquipHour;
        //                }
        //                else ... как обычная путевка
    }

    //--------------------------------------------------------------------------------------------------------------

    private class WorkShiftData(val objectID: Int, val shiftID: Int, val shiftNo: String, val begTime: Int, val endTime: Int, val run: Double, val workerName: String) {
        var tmWorkHour = TreeMap<String, Double>()
        var tmLiquidUsing = TreeMap<String, Double>()
    }

    //--------------------------------------------------------------------------------------------------------------

    private class WorkShiftCalcResult(val wsd: WorkShiftData, aObjectCalc: ObjectCalc) {
        var objectConfig: ObjectConfig

        var runWK = 0.0
        var runWOK = 0.0
        var tmWorkHour = TreeMap<String, Double>()
        var tmLiquidUsing = TreeMap<String, Double>()

        init {
            objectConfig = aObjectCalc.objectConfig

            runWK = if (aObjectCalc.gcd == null) 0.0 else aObjectCalc.gcd!!.run
            runWOK = if (aObjectCalc.gcd == null) 0.0 else aObjectCalc.gcd!!.run / objectConfig.scg!!.runKoef

            for ((descr, wc) in aObjectCalc.tmWork) tmWorkHour[descr] = wc.onTime.toDouble() / 60.0 / 60.0

            //???
            aObjectCalc.tmLiquidUsing.forEach { (name, total) ->
                tmLiquidUsing[name] = total
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------

    private class WorkShiftSumCollector {

        var sumUser = WorkShiftSumData()
        var tmSumObject = TreeMap<String, WorkShiftSumData>()
        var tmSumWorker = TreeMap<String, WorkShiftSumData>()

        fun add(objectName: String?, workerName: String?, runWK: Double, runWOK: Double, tmWorkHour: TreeMap<String, Double>, tmLiquidUsing: TreeMap<String, Double>) {

            add(sumUser, runWK, runWOK, tmWorkHour, tmLiquidUsing)

            if (objectName != null) {
                var sumObject: WorkShiftSumData? = tmSumObject[objectName]
                if (sumObject == null) {
                    sumObject = WorkShiftSumData()
                    tmSumObject[objectName] = sumObject
                }
                add(sumObject, runWK, runWOK, tmWorkHour, tmLiquidUsing)
            }

            if (workerName != null) {
                var sumWorker: WorkShiftSumData? = tmSumWorker[workerName]
                if (sumWorker == null) {
                    sumWorker = WorkShiftSumData()
                    tmSumWorker[workerName] = sumWorker
                }
                add(sumWorker, runWK, runWOK, tmWorkHour, tmLiquidUsing)
            }
        }

        private fun add(aWorkShiftSumData: WorkShiftSumData, runWK: Double, runWOK: Double, tmWorkHour: TreeMap<String, Double>, tmLiquidUsing: TreeMap<String, Double>) {

            aWorkShiftSumData.runWK += runWK
            aWorkShiftSumData.runWOK += runWOK

            for ((workDescr, wh) in tmWorkHour) {
                val workHour = aWorkShiftSumData.tmWorkHour[workDescr]
                aWorkShiftSumData.tmWorkHour[workDescr] = (workHour ?: 0.0) + wh
            }

            for ((liquidName, lu) in tmLiquidUsing) {
                val liquidUsing = aWorkShiftSumData.tmLiquidUsing[liquidName]
                aWorkShiftSumData.tmLiquidUsing[liquidName] = (liquidUsing ?: 0.0) + lu
            }
        }
    }

    //--------------------------------------------------------------------------------------------------------------

    private class WorkShiftSumData {
        var runWK = 0.0
        var runWOK = 0.0
        var tmWorkHour = TreeMap<String, Double>()
        var tmLiquidUsing = TreeMap<String, Double>()
    }

}

package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataInt
import foatto.shop.iShopApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.lang.Double.max
import java.time.LocalDate

class cWorkHour : cAbstractShopReport() {

    private val MAX_DAY_IN_ROW = 16

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val mwh = model as mWorkHour

        hmReportParam["report_year"] = (hmColumnData[mwh.columnYear] as DataInt).intValue
        hmReportParam["report_month"] = (hmColumnData[mwh.columnMonth] as DataInt).intValue

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
        printKeyW = 0.0
        printKeyH = 0.0
    }

    override fun postReport(sheet: WritableSheet) {

        val reportYear = hmReportParam["report_year"] as Int
        val reportMonth = hmReportParam["report_month"] as Int

        val reportDate = LocalDate.of(reportYear, reportMonth, 1)
        val maxDaysInMonth = reportDate.month.length(reportDate.isLeapYear)
        val monthName = when (reportDate.monthValue) {
            1 -> "январь"
            2 -> "февраль"
            3 -> "март"
            4 -> "апрель"
            5 -> "май"
            6 -> "июнь"
            7 -> "июль"
            8 -> "август"
            9 -> "сентябрь"
            10 -> "октябрь"
            11 -> "ноябрь"
            12 -> "декабрь"
            else -> "(неизвестный номер месяца: ${reportDate.monthValue})"
        }

        val shopId = (application as iShopApplication).shopId!!.toInt()
        val alUserId = mutableListOf<Int>()
        val hmUserSalesPercent = mutableMapOf<Int, Double>()
        (application as iShopApplication).alWorkHourUserId.forEachIndexed { index, sUserId ->
            val userId = sUserId.toInt()

            alUserId += userId
            hmUserSalesPercent[userId] = (application as iShopApplication).alWorkHourSalesPercent[index].toDouble()
        }
        val otherSharePart = (application as iShopApplication).otherSharePart?.toDoubleOrNull() ?: 1.0

        //--- загрузка табеля/рабочих часов
        val tmWorkHourAndTax = sortedMapOf<Int, MutableMap<Int, Pair<Double, Double>>>()
        val hmMaxHourInDay = mutableMapOf<Int, Double>()
        val rs = conn.executeQuery(
            """ 
                SELECT worker_id , da , work_hour , hour_tax 
                FROM SHOP_work_hour 
                WHERE ye = $reportYear
                AND mo = $reportMonth
            """
        )
        while (rs.next()) {
            val workerId = rs.getInt(1)
            val workDay = rs.getInt(2)
            val workHour = rs.getDouble(3)
            val hourTax = rs.getDouble(4)

            val tmDayHour = tmWorkHourAndTax.getOrPut(workerId) { mutableMapOf() }
            tmDayHour[workDay] = Pair(workHour, hourTax)

            //--- определяем сколько рабочих часов в полном рабочем дне в эти сутки
            hmMaxHourInDay[workDay] = max(workHour, hmMaxHourInDay[workDay] ?: 0.0)
        }
        rs.close()

        //--- статистика по продажам посуточно
        val hmSalesInDay = mutableMapOf<Int, Double>()
        for (day in 1..maxDaysInMonth) {
            hmSalesInDay[day] = calcOut(shopId, arrayOf(reportYear, reportMonth, day))
        }

        //--- расчёт зарплаты
        val tmWorkMoney = mutableMapOf<Int, MutableMap<Int, Pair<Double, Double>>>()
        tmWorkHourAndTax.forEach { (workerId, tmDayHour) ->
            val tmDayMoney = tmWorkMoney.getOrPut(workerId) { mutableMapOf() }
            for (day in 1..maxDaysInMonth) {
                val dayHour = tmDayHour[day]?.first
                val fullSalesPercent = hmSalesInDay[day]!! * hmUserSalesPercent[workerId]!! / 100
                val mySalesPercent = if (dayHour != null && dayHour > 0.0) {
                    val (workHour, perHourTax) = tmDayHour[day]!!   // exactly not null
                    //--- hourly tax
                    val hourTax = workHour * perHourTax
                    //--- sales percents
                    val mySalesPercentLocal = (workHour / hmMaxHourInDay[day]!!) * fullSalesPercent

                    tmDayMoney[day] = tmDayMoney[day]?.let { (prevHourTax, prevSalesPercent) ->
                        Pair(prevHourTax + hourTax, prevSalesPercent + mySalesPercentLocal)
                    } ?: Pair(hourTax, mySalesPercentLocal)

                    mySalesPercentLocal
                } else {
                    0.0
                }
                val othersSalesPercent = fullSalesPercent - mySalesPercent
                //--- if worked non-fullday
                if (othersSalesPercent > 0 && tmWorkHourAndTax.size > 1) {
                    val otherWorkersCount = tmWorkHourAndTax.size - 1
                    val addPercent = othersSalesPercent / otherWorkersCount * otherSharePart
                    tmWorkHourAndTax.forEach { (otherWorkerId, tmOtherDayHour) ->
                        if (otherWorkerId != workerId) {
                            //--- if other manager worked in this day
                            val otherDayWorkHour = tmOtherDayHour[day]?.first
                            if (otherDayWorkHour != null && otherDayWorkHour > 0.0) {
                                val tmOtherDayMoney = tmWorkMoney.getOrPut(otherWorkerId) { mutableMapOf() }
                                tmOtherDayMoney[day] = tmOtherDayMoney[day]?.let { (otherPrevHourTax, otherPrevSalesPercent) ->
                                    Pair(otherPrevHourTax, otherPrevSalesPercent + addPercent)
                                } ?: Pair(0.0, addPercent)
                            }
                        }
                    }
                }
            }
        }

        defineFormats(8, 2, 0)

        for (day in 1..MAX_DAY_IN_ROW) {
            val cvNN = CellView()
            cvNN.size = 140 / MAX_DAY_IN_ROW * 256
            sheet.setColumnView(day - 1, cvNN)
        }

        var offsY = 0

        sheet.addCell(Label(1, offsY++, aliasConfig.descr + " за $monthName $reportYear года", wcfTitleL))
        offsY++

        tmWorkMoney.forEach { (workerId, tmDayMoney) ->
            val tmDayHour = tmWorkHourAndTax[workerId]!!

            sheet.addCell(Label(0, offsY, application.hmUserFullNames[workerId] ?: "(неизвестный работник)", wcfCellLBStdRed))
            sheet.mergeCells(0, offsY, MAX_DAY_IN_ROW - 1, offsY)
            offsY += 2

            var sumHourTax = 0.0
            var sumSalesPercent = 0.0

            for (day in 1..maxDaysInMonth) {
                val dx = (day - 1) % MAX_DAY_IN_ROW
                val dy = (day - 1) / MAX_DAY_IN_ROW * 5

                val workHour = tmDayHour[day]?.first
                val sWorkHour = workHour?.let { wh -> getSplittedDouble(wh, 1, true, '.') } ?: "-"

                sheet.addCell(Label(dx, offsY + dy, day.toString(), wcfCellCBStdYellow))
                sheet.addCell(Label(dx, offsY + dy + 1, sWorkHour, wcfCellC))
                if (workHour != null && workHour > 0) {
//                    val perHourTax = wh * hmUserHourTax[workerId]!!
//                    val salesPercent = (wh / hmMaxHourInDay[day]!!) * hmSalesInDay[day]!! * hmUserSalesPercent[workerId]!! / 100
                    val dayMoney = tmDayMoney[day]!!

                    sheet.addCell(Label(dx, offsY + dy + 2, getSplittedDouble(dayMoney.first, 2, true, '.'), wcfCellC))
                    sheet.addCell(Label(dx, offsY + dy + 3, getSplittedDouble(dayMoney.second, 2, true, '.'), wcfCellC))

                    sumHourTax += dayMoney.first
                    sumSalesPercent += dayMoney.second
                }
            }
            offsY += (maxDaysInMonth / MAX_DAY_IN_ROW + 1) * 3 + 4

            sheet.addCell(Label(0, offsY, "Оклад:", wcfCellRStdYellow))
            sheet.addCell(Label(1, offsY, getSplittedDouble(sumHourTax, 2, true, '.'), wcfCellCB))
            offsY++
            sheet.addCell(Label(0, offsY, "%:", wcfCellRStdYellow))
            sheet.addCell(Label(1, offsY, getSplittedDouble(sumSalesPercent, 2, true, '.'), wcfCellCB))
            offsY++
            sheet.addCell(Label(0, offsY, "Итого:", wcfCellRStdYellow))
            sheet.addCell(Label(1, offsY, getSplittedDouble(sumHourTax + sumSalesPercent, 2, true, '.'), wcfCellCB))
            offsY++

            offsY++
        }

        sheet.addCell(Label(0, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(0, offsY, 3, offsY)
    }
}

package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.shop.mWarehouse
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.time.ZonedDateTime

class cCashHistory : cAbstractShopReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mCashHistory

        //--- выборка данных параметров для отчета
        hmReportParam["report_warehouse_dest"] = (hmColumnData[m.columnWarehouseDest] as DataComboBox).intValue

        val begDate = (hmColumnData[m.columnBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
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
        val reportWarehouse = hmReportParam["report_warehouse_dest"] as Int
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        val (gcBeg, gcEnd) = getBegEndDayFromParam()

        //--- суммируем недостачу с начала текущего года
        val gcDiffSumStart = ZonedDateTime.of(ZonedDateTime.now(zoneId).year, 1, 1, 0, 0, 0, 0, zoneId)

        val hmWarehouseName = mWarehouse.fillWarehouseMap(conn)

        defineFormats(8, 2, 0)

        var offsY = 0

        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++
        sheet.addCell(
            Label(
                1, offsY++, "с ${if (reportBegDay < 10) "0" else ""}$reportBegDay.${if (reportBegMonth < 10) "0" else ""}$reportBegMonth.$reportBegYear" +
                    " по ${if (reportEndDay < 10) "0" else ""}$reportEndDay.${if (reportEndMonth < 10) "0" else ""}$reportEndMonth.$reportEndYear", wcfTitleL
            )
        )
        offsY++
        sheet.addCell(Label(1, offsY, "Склад / магазин:", wcfTitleName))
        sheet.addCell(Label(2, offsY, hmWarehouseName[reportWarehouse], wcfTitleValue))
        offsY++

        offsY++

        val alCaption = ArrayList<String>()
        val alDim = ArrayList<Int>()

        alCaption.add("№ п/п")
        alDim.add(5)
        alCaption.add("Дата")
        alDim.add(9)
        alCaption.add("На начало дня +")
        alDim.add(9)
        alCaption.add("Реализация +")
        alDim.add(12)
        alCaption.add("Сдано -")
        alDim.add(12)
        alCaption.add("Истрачено -")
        alDim.add(11)
        alCaption.add("Дано в долг -")
        alDim.add(11)
        alCaption.add("Возвращено долгов +")
        alDim.add(11)
        alCaption.add("Остаток расчётный")
        alDim.add(9)
        alCaption.add("Остаток факти-ческий")
        alDim.add(9)
        alCaption.add("Недостача")
        alDim.add(9)
        alCaption.add("Примечания")
        alDim.add(33)

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        //--- вывод заголовков
        var offsX = 0
        for (caption in alCaption) {
            sheet.addCell(Label(offsX++, offsY, caption, wcfCaptionHC))
        }

        offsY++

        var gcCur = ZonedDateTime.now(zoneId)
        var cashRestPrev = 0.0    // остаток кассы с предыдущего дня
        var countNN = 1
        var sumOut = 0.0
        var sumPut = 0.0
        var sumUsed = 0.0
        var sumDebtOut = 0.0
        var sumDebtIn = 0.0
        var sumDiff = 0.0

        val rs = conn.executeQuery(
            " SELECT ye , mo , da , cash_put , cash_used , debt_out , debt_in , cash_rest , descr FROM SHOP_cash WHERE warehouse_id = $reportWarehouse ORDER BY ye , mo , da "
        )
        while (rs.next()) {
            //--- данные по сдаче наличности по журналу
            val ye = rs.getInt(1)
            val mo = rs.getInt(2)
            val da = rs.getInt(3)
            val cashPut = rs.getDouble(4)
            val cashUsed = rs.getDouble(5)
            val debtOut = rs.getDouble(6)
            val debtIn = rs.getDouble(7)
            val cashRest = rs.getDouble(8)
            val cashDescr = rs.getString(9)

            gcCur = ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId)

            if (gcCur.isAfter(gcEnd)) {
                break
            }

            //--- реализация на дату по накладным
            val arrDT = arrayOf(ye, mo, da)
            val cashOut = calcOut(reportWarehouse, arrDT)
            val cashRestCalc = cashRestPrev + cashOut - cashPut - cashUsed - debtOut + debtIn
            val cashDiff = cashRestCalc - cashRest
            //--- нужна сумма за ВСЁ время
            //--- (поправка: за ВСЁ время - приходит много ранних ошибок, суммируем только с 01.01.2019)
            if (gcCur.isAfter(gcDiffSumStart)) {
                sumDiff += cashDiff
            }

            //--- запрашиваемый период
            if (!gcCur.isBefore(gcBeg)) {

                sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(1, offsY, DateTime_DMY(arrDT), wcfCellC))
                sheet.addCell(Label(2, offsY, getSplittedDouble(cashRestPrev, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(3, offsY, getSplittedDouble(cashOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(4, offsY, getSplittedDouble(cashPut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(5, offsY, getSplittedDouble(cashUsed, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(6, offsY, getSplittedDouble(debtOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(7, offsY, getSplittedDouble(debtIn, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(8, offsY, getSplittedDouble(cashRestCalc, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(9, offsY, getSplittedDouble(cashRest, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(10, offsY, getSplittedDouble(cashDiff, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))
                sheet.addCell(Label(11, offsY, cashDescr, wcfCellC))

                offsY++

                //--- эти суммы нужны только за период
                sumOut += cashOut
                sumPut += cashPut
                sumUsed += cashUsed
                sumDebtOut += debtOut
                sumDebtIn += debtIn
            }
            cashRestPrev = cashRest
        }
        rs.close()

        offsY++

        sheet.addCell(Label(2, offsY, "ИТОГО:", wcfCellR))
        sheet.addCell(Label(3, offsY, getSplittedDouble(sumOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))
        sheet.addCell(Label(4, offsY, getSplittedDouble(sumPut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))
        sheet.addCell(Label(5, offsY, getSplittedDouble(sumUsed, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))
        sheet.addCell(Label(6, offsY, getSplittedDouble(sumDebtOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))
        sheet.addCell(Label(7, offsY, getSplittedDouble(sumDebtIn, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))
        sheet.addCell(Label(10, offsY, getSplittedDouble(sumDiff, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellRBStdYellow))

        offsY += 2

        sheet.addCell(Label(11, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells(  1, offsY, 2, offsY  );
    }

}

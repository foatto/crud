package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.mms.core_mms.ObjectConfig
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cDowntime : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        fillReportParam(model as mUODGD)

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

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun postReport(sheet: WritableSheet) {
        val alResult = calcReport()

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithDate(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(26)    // "Объект"
        alDim.add(9)    // "Дата"
        alDim.add(50)    // "Причина"

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        var offsX = 0
        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Объект", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Дата", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Причина", wcfCaptionHC))
        offsY++

        var countNN = 1
        for(dd in alResult) {
            //            sheet.addCell( new Label( 1, offsY, alLIDD.get( 0 ).objectConfig.name, wcfCellCB ) );
            //            sheet.mergeCells( 1, offsY, 6, offsY + 2 );
            //            offsY += 3;
            //
            //            for( LiquidIncDecData lidd : alLIDD ) {
            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(offsX++, offsY, dd.objectConfig.name, wcfCellC))
            sheet.addCell(
                Label(
                    offsX++, offsY, DateTime_DMY(
                        intArrayOf(dd.ye, dd.mo, dd.da, 0, 0, 0)
                    ), wcfCellC
                )
            )
            sheet.addCell(Label(offsX++, offsY, dd.reason, wcfCellC))

            offsY++
            //            }
        }
        offsY++

        sheet.addCell(Label(3, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 5, offsY, 6, offsY );
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    private fun calcReport(): List<DowntimeData> {

        val alResult = mutableListOf<DowntimeData>()

        //--- загрузка стартовых параметров
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

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        //--- общий обработчик на всех
        for(objectID in alObjectID) {
            //--- загрузим данные по объекту до работы с ResultSet'ом
            val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)
            val rs = stm.executeQuery(
                " SELECT ye , mo , da , reason FROM MMS_downtime " +
                " WHERE object_id = $objectID " +
                " AND ( ye > $reportBegYear " +
                    " OR ye = $reportBegYear AND mo > $reportBegMonth " +
                    " OR ye = $reportBegYear AND mo = $reportBegMonth AND da >= $reportBegDay ) " +
                " AND ( ye < $reportEndYear " +
                    " OR ye = $reportEndYear AND mo < $reportEndMonth " +
                    " OR ye = $reportEndYear AND mo = $reportEndMonth AND da <= $reportEndDay ) " )
            while(rs.next()) alResult.add(DowntimeData(oc, rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getString(4)))
            rs.close()
        }

        return alResult
    }

    private class DowntimeData(val objectConfig: ObjectConfig, val ye: Int, val mo: Int, val da: Int, val reason: String)
}

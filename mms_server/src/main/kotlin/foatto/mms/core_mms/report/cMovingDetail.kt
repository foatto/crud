package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import foatto.core.util.secondIntervalToString
import foatto.mms.core_mms.calc.GeoPeriodData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet

class cMovingDetail : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        fillReportParam(model as mUODGP)

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
        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

//        offsY = Math.max(offsY, outReportCap(sheet, 4, 0) + 1)

        //--- установка размеров заголовков (общая ширина = 90 для А4 портрет и 140 для А4 ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // "Начало" - дата и время в две строки как в сравнительном отчёте
        alDim.add(9)    // "Окончание" - дата и время в две строки как в сравнительном отчёте
        alDim.add(5)    // "Событие"
        alDim.add(9)    // "Продолжительность"
        alDim.add(7)    // "Пробег"
        alDim.add(41)   // "Оборудование"
        alDim.add(7)    // "Время работы [час]"
        alDim.add(41)   // "Топливо"
        alDim.add(7)    // "Расход"

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        var offsX = 0
        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Начало", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Окончание", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Событие", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Продолжительность", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Пробег [км]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Оборудование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Время работы [час]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Топливо", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Расход", wcfCaptionHC))
        offsY++

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        var countNNObject = 1
        for( objectID in alObjectID ) {

            val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectID)
            //--- если не прописаны гео-датчики - выходим тут же
            if(objectConfig.scg == null) continue

            //--- единоразово загрузим данные по всем датчикам объекта
            val ( alRawTime, alRawData ) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)

            //--- в обычных расчётах нам не нужны точки траектории, поэтому даем максимальный масштаб.
            //--- превышения тоже не нужны, поэтому даём maxEnabledOverSpeed = 0
            val gcd = ObjectCalc.calcGeoSensor(alRawTime, alRawData, objectConfig, begTime, endTime, 1000000000, 0, null)

            //--- первая строка: порядковый номер и наименование объекта
            offsY++    // отодвинуться от предыдущей строки
            sheet.addCell(Label(0, offsY, Integer.toString(countNNObject++), wcfNN))
            sheet.addCell(Label(1, offsY, "${objectConfig.name}, ${objectConfig.model}, ${objectConfig.groupName}, ${objectConfig.departmentName}", wcfCellCBStdYellow))
            sheet.mergeCells(1, offsY, 8, offsY)
            offsY += 2 // +1 пустая строка снизу

            var countNNInObject = 1
            for(apd in gcd.alMovingAndParking!!) {
                val gpd = apd as GeoPeriodData

                val calc = ObjectCalc.calcObject(stm, userConfig, objectConfig, gpd.begTime, gpd.endTime)

                offsX = 0
                sheet.addCell(Label(offsX++, offsY, Integer.toString(countNNInObject++), wcfNN))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, gpd.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, gpd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, if(gpd.getState() != 0) "Дв." else "Ст.", if(gpd.getState() != 0) wcfCellL else wcfCellR))
                sheet.addCell(Label(offsX++, offsY, secondIntervalToString(gpd.begTime, gpd.endTime), wcfCellC))
                sheet.addCell(
                    Label(
                        offsX++, offsY, if(gpd.getState() != 0) getSplittedDouble(calc.gcd!!.run, 1).toString()
                        else "-", if(gpd.getState() != 0) wcfCellR else wcfCellC
                    )
                )
                sheet.addCell(Label(offsX++, offsY, calc.sbWorkName.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, calc.sbWorkTotal.toString(), wcfCellR))
                sheet.addCell(Label(offsX++, offsY, calc.sbLiquidUsingName.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, calc.sbLiquidUsingTotal.toString(), wcfCellR))

                offsY++
            }
            offsY++

            val calcSum = ObjectCalc.calcObject(stm, userConfig, objectConfig, begTime, endTime)

            sheet.addCell(Label(1, offsY, "В движении:", wcfCellRB))
            sheet.mergeCells(1, offsY, 3, offsY)
            offsX = 4
            sheet.addCell(Label(offsX++, offsY, secondIntervalToString(calcSum.gcd!!.movingTime), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(calcSum.gcd!!.run, 1).toString(), wcfCellC))
            offsY++

            sheet.addCell(Label(1, offsY, "На стоянках:", wcfCellRB))
            sheet.mergeCells(1, offsY, 3, offsY)
            offsX = 4
            sheet.addCell(Label(offsX++, offsY, secondIntervalToString(calcSum.gcd!!.parkingTime), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, getSplittedLong(calcSum.gcd!!.parkingCount.toLong()).toString(), wcfCellC))
            offsY++

            sheet.addCell(Label(1, offsY, "Общее:", wcfCellRB))
            sheet.mergeCells(1, offsY, 3, offsY)
            offsX = 6
            sheet.addCell(Label(offsX++, offsY, calcSum.sbWorkName.toString(), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, calcSum.sbWorkTotal.toString(), wcfCellR))
            sheet.addCell(Label(offsX++, offsY, calcSum.sbLiquidUsingName.toString(), wcfCellC))
            sheet.addCell(Label(offsX++, offsY, calcSum.sbLiquidUsingTotal.toString(), wcfCellR))
            offsY += 2
        }

        sheet.addCell(
            Label(
                8, offsY, getPreparedAt(), wcfCellL
            )
        )
    }
}

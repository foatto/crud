package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSBFromIterable
import foatto.core.util.secondIntervalToString
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.calc.GeoPeriodData
import foatto.mms.core_mms.calc.ObjectCalc
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cParking : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        fillReportParam(model as mUODGPZ)

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

    //----------------------------------------------------------------------------------------------------------------------------------------

    override fun postReport(sheet: WritableSheet) {
        //--- загрузить данные по ВСЕМ зонам (reportZone используется только для последующей фильтрации)
        val hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)
        val alAllResult = calcReport(hmZoneData)

        //--- загрузка стартовых параметров
        //int reportObjectUser = (Integer) hmReportParam.get( "report_object_user" );
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int


        //--- вылетает при попытке загрузить конфиг по нулевому объекту
        //        ObjectConfig oc = ObjectConfig.getObjectConfig( dataWorker.alStm.get( 0 ), userConfig, reportObject );
        //        //--- если не прописаны гео-датчики - выходим тут же
        //        if( oc.scg == null ) return;

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = fillReportHeader(if(reportZone == 0) null else hmZoneData[reportZone], sheet, offsY)

        //--- установка размеров заголовков (общая ширина = 90 для А4 портрет и 140 для А4 ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // "Начало" - в две строки
        alDim.add(9)    // "Окончание" - в две строки
        alDim.add(9)    // "Продолжительность"
        alDim.add(32)   // "Оборудование"
        alDim.add(7)    // "Время работы [час]"
        alDim.add(32)   // "Топливо"
        alDim.add(7)    // "Расход"
        alDim.add(30)   // "Место"

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
        sheet.addCell(Label(offsX++, offsY, "Продолжи-тельность", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Оборудование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Время работы [час]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Топливо", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Расход", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Место", wcfCaptionHC))
        offsY++

        var countNN = 1
        for(alGPD in alAllResult) {
            sheet.addCell(Label(1, offsY, alGPD[0].calc!!.objectConfig.name, wcfCellCB))
            sheet.mergeCells(1, offsY, 8, offsY + 2)
            offsY += 3

            for(gpd in alGPD) {
                offsX = 0
                sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, gpd.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, gpd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, secondIntervalToString(gpd.begTime, gpd.endTime), wcfCellC))

                val sbWorkName = StringBuilder()
                val sbWorkTotal = StringBuilder()
                val sbLiquidUsingName = StringBuilder()
                val sbLiquidUsingTotal = StringBuilder()

                ObjectCalc.fillWorkString(gpd.calc!!.tmWorkCalc, sbWorkName, sbWorkTotal)
                ObjectCalc.fillLiquidUsingString(
                    gpd.calc!!.tmLiquidUsingTotal, gpd.calc!!.tmLiquidUsingCalc, sbLiquidUsingName, sbLiquidUsingTotal, StringBuilder()
                )

                sheet.addCell(Label(offsX++, offsY, sbWorkName.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sbWorkTotal.toString(), wcfCellR))
                sheet.addCell(Label(offsX++, offsY, sbLiquidUsingName.toString(), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, sbLiquidUsingTotal.toString(), wcfCellR))
                sheet.addCell(Label(offsX++, offsY, gpd.sbZoneName!!.toString(), wcfCellL))

                offsY++
            }
        }
        offsY++

        //        ObjectCalc calcSum = ObjectCalc.calcObject( dataWorker.alStm, userConfig, oc, begTime, endTime );
        //
        //        sheet.addCell( new Label( 1, offsY, "ИТОГО:", wcfCellRB ) );
        //        sheet.mergeCells( 1, offsY, 2, offsY );
        //        offsX = 3;
        //        sheet.addCell( new Label( offsX++, offsY, MillisInterval_SB( calcSum.gcd.parkingTime ).toString(), wcfCellC ) );
        //        sheet.addCell( new Label( offsX++, offsY, calcSum.sbWorkName.toString(), wcfCellC ) );
        //        sheet.addCell( new Label( offsX++, offsY, calcSum.sbWorkParking.toString(), wcfCellR ) );
        //        sheet.addCell( new Label( offsX++, offsY, calcSum.sbLiquidUsingName.toString(), wcfCellC ) );
        //        sheet.addCell( new Label( offsX++, offsY, calcSum.sbLiquidUsingParking.toString(), wcfCellR ) );
        //        offsY += 2;

        sheet.addCell(Label(7, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(7, offsY, 8, offsY)
    }

    private fun calcReport(hmZoneData: Map<Int, ZoneData>): List<List<GeoPeriodData>> {

        val alAllResult = mutableListOf<MutableList<GeoPeriodData>>()

        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        for(objectID in alObjectID) {
            val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)
            //--- гео-датчик не прописан
            if(oc.scg == null) continue

            //--- единоразово загрузим данные по всем датчикам объекта
            val ( alRawTime, alRawData ) = ObjectCalc.loadAllSensorData(stm, oc, begTime, endTime)

            //--- в обычных расчётах нам не нужны точки траектории, поэтому даем максимальный масштаб.
            //--- превышения тоже не нужны, поэтому даём maxEnabledOverSpeed = 0
            val gcd = ObjectCalc.calcGeoSensor(alRawTime, alRawData, oc, begTime, endTime, 1000000000, 0, null)

            val alObjectResult = ArrayList<GeoPeriodData>()
            for(apd in gcd.alMovingAndParking!!) {
                val gpd = apd as GeoPeriodData

                //--- пропускаем периоды движения
                if(gpd.getState() != 0) continue

                val tsZoneName = TreeSet<String>()
                val inZone = ObjectCalc.fillZoneList(hmZoneData, reportZone, gpd.parkingCoord!!, tsZoneName)
                //--- фильтр по геозонам, если задано
                if(reportZone != 0 && !inZone) continue

                gpd.calc = ObjectCalc.calcObject(stm, userConfig, oc, gpd.begTime, gpd.endTime)
                gpd.sbZoneName = getSBFromIterable(iterable = tsZoneName, delimiter = ", ")

                alObjectResult.add(gpd)
            }
            if(!alObjectResult.isEmpty()) alAllResult.add(alObjectResult)
        }
        return alAllResult
    }
}

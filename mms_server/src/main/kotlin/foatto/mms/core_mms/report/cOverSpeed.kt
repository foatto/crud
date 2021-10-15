package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSBFromIterable
import foatto.core.util.secondIntervalToString
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.OverSpeedPeriodData
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cOverSpeed : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        fillReportParam(model as mUODGPZ)

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
        //--- загрузить данные по ВСЕМ зонам (reportZone используется только для последующей фильтрации)
        val hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)
        val alAllResult = calcReport(hmZoneData)

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = fillReportHeader(if (reportZone == 0) null else hmZoneData[reportZone], sheet, offsY)

        //--- установка размеров заголовков (общая ширина = 90 для А4 портрет и 140 для А4 ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // "Начало" - в две строки
        alDim.add(9)    // "Окончание" - в две строки
        alDim.add(9)    // "Продолжительность"
        alDim.add(8)    // "Макс. скорость на участке"
        alDim.add(6)    // "Макс. превышение на участке"
        alDim.add(44)    // "Место"

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
        sheet.addCell(Label(offsX++, offsY, "Макс. скорость [км/ч]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Макс. превы-шение [км/ч]", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Место", wcfCaptionHC))
        offsY++

        var countNN = 1
        for (alOSPD in alAllResult) {
            sheet.addCell(Label(1, offsY, alOSPD[0].objectConfig!!.name, wcfCellCB))
            sheet.mergeCells(1, offsY, 6, offsY + 2)
            offsY += 3

            for (ospd in alOSPD) {
                offsX = 0

                sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, ospd.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, ospd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, secondIntervalToString(ospd.begTime, ospd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, ospd.maxOverSpeedMax.toString(), wcfCellR))
                sheet.addCell(Label(offsX++, offsY, ospd.maxOverSpeedDiff.toString(), wcfCellR))
                sheet.addCell(Label(offsX++, offsY, ospd.sbZoneName!!.toString(), wcfCellL))

                offsY++
            }
        }
        offsY++

        sheet.addCell(
            Label(6, offsY, getPreparedAt(), wcfCellL)
        )
        //sheet.mergeCells( 5, offsY, 6, offsY );
    }

    //----------------------------------------------------------------------------------------------------------------------

    private fun calcReport(hmZoneData: Map<Int, ZoneData>): List<List<OverSpeedPeriodData>> {

        val alAllResult = mutableListOf<MutableList<OverSpeedPeriodData>>()

        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        val maxEnabledOverSpeed = (application as iMMSApplication).maxEnabledOverSpeed

        val alobjectId = ArrayList<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if (reportObject == 0) {
            loadObjectList(conn, userConfig, reportObjectUser, reportDepartment, reportGroup, alobjectId)
        } else {
            alobjectId.add(reportObject)
        }

        for (objectId in alobjectId) {
            val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectId)
            //--- гео-датчик не прописан
            if (oc.scg == null) continue

            val hmZoneLimit = ZoneLimitData.getZoneLimit(
                stm = stm,
                userConfig = userConfig,
                objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectId),
                hmZoneData = hmZoneData,
                zoneType = ZoneLimitData.TYPE_LIMIT_SPEED
            )

            //--- единоразово загрузим данные по всем датчикам объекта
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, begTime, endTime)

            val gcd = ObjectCalc.calcGeoSensor(
                alRawTime = alRawTime,
                alRawData = alRawData,
                scg = oc.scg!!,
                begTime = begTime,
                endTime = endTime,
                scale = hmXyDocumentConfig["mms_map"]!!.alElementConfig.minOf { it.second.scaleMin },
                maxEnabledOverSpeed = maxEnabledOverSpeed,
                alZoneSpeedLimit = hmZoneLimit[ZoneLimitData.TYPE_LIMIT_SPEED]
            )

            if (gcd.alOverSpeed.isEmpty()) continue

            val alObjectResult = ArrayList<OverSpeedPeriodData>()
            for (i in gcd.alOverSpeed.indices) {
                val ospd = gcd.alOverSpeed[i] as OverSpeedPeriodData
                if (ospd.getState() == 0) continue

                val tsZoneName = TreeSet<String>()
                val inZone = ObjectCalc.fillZoneList(hmZoneData, reportZone, ospd.maxOverSpeedCoord!!, tsZoneName)
                //--- фильтр по геозонам, если задано
                if (reportZone != 0 && !inZone) continue

                ospd.objectConfig = oc
                ospd.sbZoneName = getSBFromIterable(tsZoneName, ", ")

                alObjectResult.add(ospd)
            }
            if (alObjectResult.isNotEmpty()) alAllResult.add(alObjectResult)
        }
        return alAllResult
    }
}

//    private class OverSpeedSumData {
//        //--- счетчик больших (свыше 10 км/ч) превышений
//        public int bigCount = 0;
//        public long bigTime = 0;
//        //--- счетчик малых (до 10 км/ч) превышений
//        public int smallCount = 0;
//        public long smallTime = 0;
//    }

//            //--- сбор сумм
//            TreeMap<String,OverSpeedSumData> tmAutoSum = tmUserSum.get( userName );
//            if( tmAutoSum == null ) {
//                tmAutoSum = new TreeMap<>();
//                tmUserSum.put( userName, tmAutoSum );
//            }
//            OverSpeedSumData ossd = tmAutoSum.get( autoInfo );
//            if( ossd == null ) {
//                ossd = new OverSpeedSumData();
//                tmAutoSum.put( autoInfo, ossd );
//            }
//            if( cr.ospd.getMaxOverSpeedDiff() > 10 ) {
//                ossd.bigCount++;
//                ossd.bigTime += cr.ospd.getEndTime() - cr.ospd.getBegTime();
//            }
//            else {
//                ossd.smallCount++;
//                ossd.smallTime += cr.ospd.getEndTime() - cr.ospd.getBegTime();
//            }
//        }
//        offsY++;
//
//        //--- вывод заголовка сумм
//        label = new jxl.write.Label( 3, offsY, "Автомобиль", wcfCaptionHC );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 4, offsY, "Продол-житель-ность", wcfCaptionHC );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 5, offsY, "Кол-во", wcfCaptionHC );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 6, offsY, "Тип превы-шения", wcfCaptionHC );
//        sheet.addCell( label );
//
//        offsY++;
//
//        //--- вывод сумм
//        int allBigCount = 0;
//        long allBigTime = 0;
//        int allSmallCount = 0;
//        long allSmallTime = 0;
//
//        for( String userName : tmUserSum.keySet() ) {
//            TreeMap<String,OverSpeedSumData> tmAutoSum = tmUserSum.get( userName );
//            int userBigCount = 0;
//            long userBigTime = 0;
//            int userSmallCount = 0;
//            long userSmallTime = 0;
//
//            for( String autoInfo : tmAutoSum.keySet() ) {
//                OverSpeedSumData ossd = tmAutoSum.get( autoInfo );
//
//                if( reportSumAuto ) {
//                    label = new jxl.write.Label( 3, offsY, autoInfo, wcfCellL );
//                    sheet.addCell( label );
//
//                    outSumRow( sheet, offsY, ossd.bigCount, ossd.bigTime, ossd.smallCount, ossd.smallTime);
//                    offsY += 3;
//                }
//
//                userBigCount += ossd.bigCount;
//                userBigTime += ossd.bigTime;
//                userSmallCount += ossd.smallCount;
//                userSmallTime += ossd.smallTime;
//
//                allBigCount += ossd.bigCount;
//                allBigTime += ossd.bigTime;
//                allSmallCount += ossd.smallCount;
//                allSmallTime += ossd.smallTime;
//            }
//            if( reportSumUser ) {
//                label = new jxl.write.Label( 2, offsY, userName, wcfCellL );
//                sheet.addCell( label );
//                sheet.mergeCells( 2, offsY, 3, offsY );
//                outSumRow( sheet, offsY, userBigCount, userBigTime, userSmallCount, userSmallTime);
//                offsY += 4;
//            }
//        }
//        label = new jxl.write.Label( 3, offsY, "ИТОГО:", wcfCellL );
//        sheet.addCell( label );
//        outSumRow( sheet, offsY, allBigCount, allBigTime, allSmallCount, allSmallTime);
//        offsY += 3;
//
//        offsY++;
//        label = new jxl.write.Label( 7, offsY, new StringBuilder( "Подготовлено: " )
//                                     .append( DateTime_DMYHMS( new GregorianCalendar() ) ).toString(),
//                                     wcfCellL );
//        sheet.addCell( label );
//    }
//
//    private void outSumRow( WritableSheet sheet, int offsY, int bigCount, long bigTime, int smallCount, long smallTime ) throws Exception {
//
//        jxl.write.Label label = new jxl.write.Label( 4, offsY, MillisInterval_SB( 0, bigTime + smallTime ).toString(), wcfCellCB );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 5, offsY, Integer.toString( bigCount + smallCount ), wcfCellRB );
//        sheet.addCell( label );
//
//        offsY++;
//
//        label = new jxl.write.Label( 4, offsY, MillisInterval_SB( 0, bigTime ).toString(), wcfCellC );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 5, offsY, Integer.toString( bigCount ), wcfCellR );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 6, offsY, "> 10", wcfCellR );
//        sheet.addCell( label );
//
//        offsY++;
//
//        label = new jxl.write.Label( 4, offsY, MillisInterval_SB( 0, smallTime ).toString(), wcfCellC );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 5, offsY, Integer.toString( smallCount ), wcfCellR );
//        sheet.addCell( label );
//        label = new jxl.write.Label( 6, offsY, "<= 10", wcfCellR );
//        sheet.addCell( label );
//    }

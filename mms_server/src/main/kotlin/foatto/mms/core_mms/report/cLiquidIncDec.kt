package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getSplittedDouble
import foatto.core.util.secondIntervalToString
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.calc.LiquidIncDecData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cLiquidIncDec : cMMSReport() {

    //--- это отчёт по заправкам вне путевых листов?
    private var isWaybill = false

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        //--- это отчёт по заправкам вне путевых листов?
        isWaybill = aliasConfig.alias == "mms_report_liquid_inc_waybill"

        if(isWaybill) {
            val m = model as mLiquidIncWaybill

            fillReportParam(m.uodg)

            val begDate = (hmColumnData[m.columnReportBegDate] as DataDate3Int).localDate
            val begTime = (hmColumnData[m.columnReportBegTime] as DataTime3Int).localTime
            val endDate = (hmColumnData[m.columnReportEndDate] as DataDate3Int).localDate
            val endTime = (hmColumnData[m.columnReportEndTime] as DataTime3Int).localTime
    
            hmReportParam["report_beg_year"] = begDate.year
            hmReportParam["report_beg_month"] = begDate.monthValue
            hmReportParam["report_beg_day"] = begDate.dayOfMonth
            hmReportParam["report_beg_hour"] = begTime.hour
            hmReportParam["report_beg_minute"] = begTime.minute
    
            hmReportParam["report_end_year"] = endDate.year
            hmReportParam["report_end_month"] = endDate.monthValue
            hmReportParam["report_end_day"] = endDate.dayOfMonth
            hmReportParam["report_end_hour"] = endTime.hour
            hmReportParam["report_end_minute"] = endTime.minute

            hmReportParam["report_time_type"] = (hmColumnData[m.columnTimeType] as DataComboBox).value

            hmReportParam["report_zone"] = (hmColumnData[m.columnReportZone] as DataInt).value
        }
        else {
            fillReportParam(model as mUODGPZ)

            hmReportParam["report_time_type"] = mWorkShiftCompare.TIME_TYPE_DOC
        }
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
        val alResult = calcReport(hmZoneData)

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = fillReportHeader(if(reportZone == 0) null else hmZoneData[reportZone], sheet, offsY)

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // "Начало" - в две строки
        alDim.add(9)    // "Окончание" - в две строки
        alDim.add(9)    // "Продолжительность"
        alDim.add(17)    // "Ёмкость"
        alDim.add(7)    // "Объём"
        alDim.add(34)    // "Место"

        for(i in alDim.indices) {
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
        sheet.addCell(Label(offsX++, offsY, "Ёмкость", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Объём", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Место", wcfCaptionHC))
        offsY++

        var countNN = 1
        for(tmLIDD in alResult) {
            if(tmLIDD.isEmpty()) continue

            sheet.addCell(Label(1, offsY, tmLIDD.firstEntry().value.objectConfig!!.name, wcfCellCB))
            sheet.mergeCells(1, offsY, 6, offsY + 2)
            offsY += 3

            for(lidd in tmLIDD.values) {
                offsX = 0

                val levelDiff = Math.abs(lidd.begLevel - lidd.endLevel)

                sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, lidd.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, lidd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, secondIntervalToString(lidd.begTime, lidd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, lidd.sca!!.descr, wcfCellC))
                sheet.addCell(
                    Label(
                        offsX++, offsY, getSplittedDouble(
                            levelDiff, ObjectCalc.getPrecision(levelDiff)
                        ).toString(), wcfCellR
                    )
                )
                sheet.addCell(Label(offsX++, offsY, lidd.sbZoneName!!.toString(), wcfCellL))

                offsY++
            }
        }
        offsY++

        sheet.addCell(Label(6, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 5, offsY, 6, offsY );
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    private fun calcReport(hmZoneData: Map<Int, ZoneData>): List<TreeMap<String, LiquidIncDecData>> {

        val alResult = mutableListOf<TreeMap<String, LiquidIncDecData>>()

        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportTimeType = hmReportParam["report_time_type"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        val isInc = aliasConfig.alias == "mms_report_liquid_inc" || isWaybill

        val (begTime, endTime) = getBegEndTimeFromParam()

        val alObjectID = ArrayList<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        //--- общий обработчик на всех
        for(objectID in alObjectID) {
            val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)
            val hmSCLL = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]
            //--- уровнемеры не прописаны
            if(hmSCLL == null || hmSCLL.isEmpty()) continue

            //--- загрузим информацию по путевым листам
            val alBeg = ArrayList<Int>()
            val alEnd = ArrayList<Int>()
            if(isWaybill) {
                val rs = stm.executeQuery(
                    " SELECT beg_dt , end_dt , beg_dt_fact , end_dt_fact FROM MMS_work_shift " +
                    " WHERE object_id = $objectID"
                )
                while(rs.next()) {
                    alBeg.add(rs.getInt(if(reportTimeType == mWorkShiftCompare.TIME_TYPE_DOC) 1 else 3))
                    alEnd.add(rs.getInt(if(reportTimeType == mWorkShiftCompare.TIME_TYPE_DOC) 2 else 4))
                }
                rs.close()
            }

            //--- единоразово загрузим данные по всем датчикам объекта
            val ( alRawTime, alRawData ) = ObjectCalc.loadAllSensorData(stm, oc, begTime, endTime)

            val tmObjectResult = TreeMap<String, LiquidIncDecData>()
            for(portNum in hmSCLL.keys) {
                val sca = hmSCLL[portNum] as SensorConfigLiquidLevel
                //--- собираем заправки или сливы по одному датчику
                val alSCAResult = ObjectCalc.calcIncDec(
                    stm, alRawTime, alRawData, oc, sca, begTime, endTime, isWaybill, alBeg, alEnd, if(isInc) 1 else -1, hmZoneData, reportZone
                )
                //--- для этого отчёта суммируем данные по всему объекту с сортировкой по времени события
                for(lidd in alSCAResult) tmObjectResult[StringBuilder().append(lidd.begTime).append(lidd.endTime).toString()] = lidd
            }
            alResult.add(tmObjectResult)
        }
        return alResult
    }
}
//            //--- подсчет сумм для каждой емкости
//            TreeMap<String,TreeMap<String,Double>> tmSumValue = new TreeMap<>();
//            TreeMap<String,TreeMap<String,Long>> tmSumTime = new TreeMap<>();
//
//            TreeMap<String,LiquidIncDecData> tmObjectResult = tmAllResult.get( objectName );
//            TreeMap<String,Double[]> tmLevel = tmResultLevel.get( objectName );

//                TreeMap<String,Double> tmValue = tmSumValue.get( lidd.sca.descr );
//                if( tmValue == null ) {
//                    tmValue = new TreeMap<>();
//                    tmSumValue.put( lidd.sca.descr, tmValue );
//                }
//                Double sumValue = tmValue.get( action );
//                tmValue.put( action, ( sumValue == null ? 0 : sumValue ) + lidd.diff );
//
//                TreeMap<String,Long> tmTime = tmSumTime.get( lidd.sca.descr );
//                if( tmTime == null ) {
//                    tmTime = new TreeMap<>();
//                    tmSumTime.put( lidd.sca.descr, tmTime );
//                }
//                Long sumTime = tmTime.get( action );
//                tmTime.put( action, ( sumTime == null ? 0 : sumTime ) + ( lidd.endTime - lidd.begTime ) );

//            //--- вывод сумм по ёмкостям по объекту
//            sheet.addCell( new Label( 2, offsY, "ИТОГО:", wcfCellCBStdYellow ) );
//            for( String descr : tmSumValue.keySet() ) {
//                TreeMap<String,Double> tmValue = tmSumValue.get( descr );
//                TreeMap<String,Long> tmTime = tmSumTime.get( descr );
//                Double[] arrLevel = tmLevel.get( descr );
//                sheet.addCell( new Label( 3, offsY, descr, wcfCellCBStdYellow ) );
//                sheet.mergeCells( 3, offsY, 3, offsY + tmValue.size() - 1 + ( arrLevel == null ? 0 : 2 ) ) ;
//
//                if( arrLevel != null ) {
//                    sheet.addCell( new Label( 4, offsY, "Остаток на начало периода", wcfCellCBStdYellow ) );
//                    sheet.addCell( new Label( 5, offsY, getSplittedDouble( arrLevel[ 0 ], 0 ).toString(), wcfCellCBStdYellow ) );
//                    offsY++;
//                }
//                for( String action : tmValue.keySet() ) {
//                    sheet.addCell( new Label( 4, offsY, action, wcfCellCBStdYellow ) );
//                    sheet.addCell( new Label( 5, offsY, getSplittedDouble( tmValue.get( action ), 0 ).toString(), wcfCellCBStdYellow ) );
//                    sheet.addCell( new Label( 6, offsY, MillisInterval_SB( tmTime.get( action ) ).toString(), wcfCellCBStdYellow ) );
//                    offsY++;
//                }
//                if( arrLevel != null ) {
//                    sheet.addCell( new Label( 4, offsY, "Остаток на конец периода", wcfCellCBStdYellow ) );
//                    sheet.addCell( new Label( 5, offsY, getSplittedDouble( arrLevel[ 1 ], 0 ).toString(), wcfCellCBStdYellow ) );
//                    offsY++;
//                }
//            }

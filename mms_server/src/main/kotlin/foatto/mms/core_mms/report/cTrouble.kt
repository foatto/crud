package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.mms.MMSSpringController
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigAnalogue
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.nio.ByteOrder
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.max

class cTrouble : cMMSReport() {

    private enum class TroubleLevel { INFO, WARNING, ERROR }

    private class TroubleData( val begTime: Int, val sensorDescr: String, val troubleDescr: String, val level: TroubleLevel)

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        val m = model as mTrouble

        //--- выборка данных параметров для отчета
        fillReportParam(m.uodg)
        hmReportParam["report_period"] = (hmColumnData[m.columnReportPeriod] as DataInt).value

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

    override fun postReport(sheet: WritableSheet) {

        val tmResult = calcReport()

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        defineFormats(8, 2, 0)

        var offsY = 0
        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++    // еще одна пустая строчка снизу

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = Math.max(offsY, outReportCap(sheet, 4, 0) + 1)

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(30)    // "Объект"
        alDim.add(16)    // "Начало"
        alDim.add(14)    // "Оборудование"
        alDim.add(25)    // "Описание"

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовка
        sheet.addCell(Label(0, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(1, offsY, "Объект", wcfCaptionHC))
        sheet.addCell(Label(2, offsY, "Начало", wcfCaptionHC))
        sheet.addCell(Label(3, offsY, "Оборудование", wcfCaptionHC))
        sheet.addCell(Label(4, offsY, "Описание", wcfCaptionHC))
        offsY++

        var countNN = 1
        for((keyObject,alObjectResult) in tmResult) {
            if(alObjectResult.isEmpty()) continue

            val rc = alObjectResult.size

            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            if(rc > 1) sheet.mergeCells(0, offsY, 0, offsY + rc - 1)

            sheet.addCell(Label(1, offsY, keyObject, wcfCellL))
            if(rc > 1) sheet.mergeCells(1, offsY, 1, offsY + rc - 1)

            for(td in alObjectResult) {
                sheet.addCell(
                    Label(
                        2, offsY, if(td.begTime == 0) "-"
                        else DateTime_DMYHMS(zoneId, td.begTime), wcfCellC
                    )
                )
                sheet.addCell(Label(3, offsY, td.sensorDescr, wcfCellC))
                sheet.addCell(
                    Label(
                        4, offsY, td.troubleDescr,
                        /*td.level == TroubleData.LEVEL_ERROR ? wcfCellCStdRed :
                               td.level == TroubleData.LEVEL_WARNING ? wcfCellCStdYellow :*/ wcfCellC
                    )
                )
                offsY++
            }
        }
        offsY++

        //offsY += 2;
        sheet.addCell(Label(3, offsY, getPreparedAt(), wcfCellL))
        sheet.mergeCells(3, offsY, 4, offsY)

        outReportSignature(sheet, intArrayOf(0, 3, 4), offsY + 3)
    }

    //----------------------------------------------------------------------------------------------------------------------

    private fun calcReport(): TreeMap<String, ArrayList<TroubleData>> {

        val tmResult = TreeMap<String, ArrayList<TroubleData>>()

        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportPeriod = (hmReportParam["report_period"] as Int) * 24 * 60 * 60

        //--- загружаем дату/время последнего простоя техники
        val hmLastDowntime = mutableMapOf<Int, Int>()
        var rs = stm.executeQuery( " SELECT object_id, ye, mo, da FROM MMS_downtime ORDER BY ye, mo, da " )
        while(rs.next()) {
            val oID = rs.getInt(1)
            val zdt = ZonedDateTime.of(rs.getInt(2), rs.getInt(3), rs.getInt(4), 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS)

            hmLastDowntime[oID] = zdt.toEpochSecond().toInt()
        }

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        for(objectID in alObjectID) {
            val oc = ObjectConfig.getObjectConfig(stm, userConfig, objectID)

            //--- отключенные объекты в отчёт попасть не должны
            if(oc.isDisabled) continue

            //--- в заданный период объект не работал (простаивал, не выезжал, был в ремонте и т.п.)
            val lastDowntime = hmLastDowntime[objectID]
            if(lastDowntime != null && getCurrentTimeInt() - lastDowntime <= reportPeriod) continue
            //--- в прочих ситуациях делаем проверку вида lastDowntime == null ? curTime : Math.max( lastDowntime, curTime ),
            //--- чтобы неисправность "начиналась" ПОСЛЕ периода простоя

            //--- нестандартное object-info: сначала идёт имя пользователя
            val sbObjectKey = StringBuilder(getRecordUserName(oc.userID)).append('\n').append(oc.name)
            if(!oc.model.isEmpty()) sbObjectKey.append(", ").append(oc.model)
            if(!oc.groupName.isEmpty() || !oc.departmentName.isEmpty()) sbObjectKey.append('\n').append(oc.groupName).append(if(oc.groupName.isEmpty() || oc.departmentName.isEmpty()) "" else ", ").append(oc.departmentName)
            val objectKey = sbObjectKey.toString()

            val hmSCLL = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]

            //--- отчёт делаем только по активной базе (по полной архивной базе - слишком долго, а сроки свыше недели несущественны)
            rs = stm.executeQuery( " SELECT ontime , sensor_data FROM MMS_data_${oc.objectID} ORDER BY ontime DESC " )

            var isNoDataFinished = false
            var isNoGeoFinished = false
            var noGeoTime = getCurrentTimeInt()
            val hmLLTime = mutableMapOf<Int, Int>()
            val hmLLData = mutableMapOf<Int, Int>()
            val hsLLFinished = mutableSetOf<Int>()
            while(rs.next()) {
                val curTime = rs.getInt(1)
                val bbIn = rs.getByteBuffer(2, ByteOrder.BIG_ENDIAN)

                //--- ловля отключенности контроллера по отсутствию данных - достаточно одной точки
                if(!isNoDataFinished) {
                    if(getCurrentTimeInt() - curTime > reportPeriod) addTrouble(
                        tmResult, objectKey, TroubleData(
                            if(lastDowntime == null) curTime else max(lastDowntime, curTime), "Контроллер", "Нет данных", TroubleLevel.ERROR
                        )
                    )
                    isNoDataFinished = true
                }

                //--- ловля ошибок GPS
                if(oc.scg != null && !isNoGeoFinished) {
                    val gd = AbstractObjectStateCalc.getGeoData(oc, bbIn)

                    if(gd == null || gd.wgs.x == 0 && gd.wgs.y == 0) noGeoTime = curTime
                    else {
                        if(getCurrentTimeInt() - noGeoTime > reportPeriod) addTrouble(
                            tmResult, objectKey, TroubleData(
                                if(lastDowntime == null) noGeoTime else max(lastDowntime, noGeoTime), "Гео-датчик", "Нет данных с гео-датчика", TroubleLevel.ERROR
                            )
                        )
                        isNoGeoFinished = true
                    }
                }

                //--- ловля ошибок датчика уровня топлива
                if(hmSCLL != null)
                    for((portNum,sc) in hmSCLL) {
                        //--- этот датчик уже обработан
                        if(hsLLFinished.contains(portNum)) continue

                        val sca = sc as SensorConfigAnalogue
                        val sensorData = AbstractObjectStateCalc.getSensorData(oc, portNum, bbIn)?.toInt() ?: 0

                        val oldLLData = hmLLData[portNum]
                        if(oldLLData == null) {
                            hmLLTime[portNum] = curTime
                            hmLLData[portNum] = sensorData
                        }
                        else if(oldLLData == sensorData) {
                            hmLLTime[portNum] = curTime
                        }
                        else {
                            if (SensorConfigAnalogue.hmLLErrorCodeDescr[oldLLData] != null && getCurrentTimeInt() - hmLLTime[portNum]!! > reportPeriod)

                                addTrouble(
                                    tmResult, objectKey, TroubleData(
                                        if (lastDowntime == null) hmLLTime[portNum]!!
                                        else Math.max(lastDowntime, hmLLTime[portNum]!!), sca.descr, SensorConfigAnalogue.hmLLErrorCodeDescr[oldLLData]!!, TroubleLevel.ERROR
                                    )
                                )

                            hsLLFinished.add(portNum)
                        }
                    }

                //--- все обработались, пока выходить
                if(isNoDataFinished && (oc.scg == null || isNoGeoFinished) && (hmSCLL == null || hsLLFinished.size == hmSCLL.size)) break
            }
            rs.close()

            //--- если вообще нет данных
            if(!isNoDataFinished) {
                if(lastDowntime == null) addTrouble(
                    tmResult, objectKey, TroubleData(
                        0, "Контроллер", "Нет данных более ${(appController as MMSSpringController).expirePeriod} недель(и)", TroubleLevel.ERROR
                    )
                )
                else addTrouble(tmResult, objectKey, TroubleData(lastDowntime, "Контроллер", "Нет данных", TroubleLevel.ERROR))
            }
            else {
                if(oc.scg != null && !isNoGeoFinished) {
                    if(getCurrentTimeInt() - noGeoTime > reportPeriod) addTrouble(
                        tmResult, objectKey, TroubleData(
                            if(lastDowntime == null) noGeoTime else Math.max(lastDowntime, noGeoTime), "Гео-датчик", "Нет данных с гео-датчика", TroubleLevel.ERROR
                        )
                    )
                }
                if(hmSCLL != null) {
                    for(portNum in hmSCLL.keys) {
                        val sca = hmSCLL[portNum] as SensorConfigAnalogue
                        //--- этот датчик так и не обработан
                        if(!hsLLFinished.contains(portNum)) {
                            if(hmLLTime[portNum] == null) {
                                if(lastDowntime == null) addTrouble(tmResult, objectKey, TroubleData(0, sca.descr, "Нет данных", TroubleLevel.ERROR))
                                else addTrouble(tmResult, objectKey, TroubleData(lastDowntime, sca.descr, "Нет данных", TroubleLevel.ERROR))
                            }
                            else {
                                val oldLLData = hmLLData[portNum]
                                if (SensorConfigAnalogue.hmLLErrorCodeDescr[oldLLData] != null && getCurrentTimeInt() - hmLLTime[portNum]!! > reportPeriod)

                                    addTrouble(
                                        tmResult, objectKey, TroubleData(
                                            if (lastDowntime == null) hmLLTime[portNum]!! else Math.max(lastDowntime, hmLLTime[portNum]!!), sca.descr, SensorConfigAnalogue.hmLLErrorCodeDescr[oldLLData]!!, TroubleLevel.ERROR
                                        )
                                    )
                            }
                        }
                    }
                }
            }
            //--- закроем незакрытый период по гео-датчику
            //--- иначе закроем незакрытые периоды ошибок по гео-датчику и уровнемерам
            //--- отдельный пункт "неисправности" - передвижной объект имеет почти нулевые пробеги за заданный период
            if(oc.scg != null) {
                val objectCalc = ObjectCalc.calcObject(
                    stm, userConfig, oc, getCurrentTimeInt() - reportPeriod, getCurrentTimeInt()
                )
                //--- пробег менее 100 метров
                if(objectCalc.gcd!!.run < 0.1) addTrouble(
                    tmResult, objectKey, TroubleData(getCurrentTimeInt() - reportPeriod, "Объект", "Нет пробега", TroubleLevel.ERROR)
                )
            }
        }
        return tmResult
    }

    private fun addTrouble(tmResult: TreeMap<String, ArrayList<TroubleData>>, objectKey: String, troubleData: TroubleData) {
        var alTrouble: ArrayList<TroubleData>? = tmResult[objectKey]
        if(alTrouble == null) {
            alTrouble = ArrayList()
            tmResult[objectKey] = alTrouble
        }
        alTrouble.add(troubleData)
    }

}
//--- еще можно добавить проверку наличия GPS/ГЛОНАСС-сигнала
//
//            //--- ловля пропажи основного напряжения
//            HashMap<Integer,SensorConfig> hmSCV = oc.hmSensorConfig.get( SensorConfig.SENSOR_VOLTAGE );
//            if( hmSCV != null )
//                for( Integer portNum : hmSCV.keySet() ) {
//                    SensorConfigA sca = (SensorConfigA) hmSCV.get( portNum );
//                    //--- чтобы не смешивались разные ошибки по одному датчику и одинаковые ошибки по разным датчикам,
//                    //--- добавляем в описание ошибки не только само описание ошибки, но и описание датчика
//
//                    //--- 0 - отсутствие напряжения основного питания
//                    checkSensorError( alRawTime, alRawData, oc, portNum, sca.descr, begTime, endTime,
//                                      TroubleData.LEVEL_WARNING, 0, "Нет питания", 5 * 60 * 1000, tmObjectResult );
//                }
//        if( aliasConfig.getAlias().equals( "ft_report_new_controller" ) ) {
//            for( int autoID : alAutoID ) {
//                AutoConfig ac = AutoConfig.getAutoConfig( stm, userConfig, autoID );
//                //!!! MSSQL: TOP( 1 )
//                ResultSet rs = stm.executeQuery( new StringBuilder(
//                             " SELECT TOP( 1 ) controller_id , set_dt FROM GPS_controller_auto_history " )
//                    .append( " WHERE auto_id = " ).append( autoID )
//                    .append( " AND set_dt >= '" ).append( sbDate ).append( "' " )
//                    .append( " ORDER BY set_dt DESC , controller_id DESC " ).toString() );
//                if( rs.next() ) {
//                    int controllerID = rs.getInt( 1 );
//                    String setDateTime = rs.getString( 2 );
//                    //--- "свежее" снятие контроллеров нас не интересует
//                    if( controllerID != 0 )
//                        alResult.add( new MonitoringCalcResult( ac.gosNo, ac.modelName, setDateTime, ac.userID ) );
//                }
//                rs.close();
//            }
//        }
//        else if( aliasConfig.getAlias().equals( "ft_report_without_controller" ) ) {
//            for( int autoID : alAutoID ) {
//                AutoConfig ac = AutoConfig.getAutoConfig( stm, userConfig, autoID );
//                //--- отключенные машины в этот отчёт выводить не надо
//                if( ac.disabled ) continue;
//                boolean isConnected = ac.controllerType != mTrackerConfig.TYPE_UNKNOWN;
//                //--- возможно, этот а/м подключен к контроллеру АвтоТрекер
//                if( ! isConnected ) {
//                    ResultSet rs = stm.executeQuery( new StringBuilder( " SELECT module_id FROM AT_config WHERE auto_id = " )
//                                                     .append( autoID ).toString() );
//                    isConnected = rs.next();
//                    rs.close();
//                }
//                if( ! isConnected )
//                    alResult.add( new MonitoringCalcResult( ac.gosNo, ac.modelName, "-", ac.userID ) );
//            }
//        }

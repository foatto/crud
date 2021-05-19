package foatto.mms.core_mms.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.server.data.DataInt
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import foatto.mms.iMMSApplication
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cTrouble : cMMSReport() {

    private enum class TroubleLevel { INFO, WARNING, ERROR }

    private class TroubleData(val begTime: Int, val sensorDescr: String, val troubleDescr: String, val level: TroubleLevel)

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mTrouble

        //--- выборка данных параметров для отчета
        fillReportParam(m.uodg)
        hmReportParam["report_period"] = (hmColumnData[m.columnReportPeriod] as DataInt).intValue

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

        //--- установка размеров заголовков (общая ширина = 90 для А4-портрет поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(30)    // "Объект"
        alDim.add(16)    // "Начало"
        alDim.add(14)    // "Оборудование"
        alDim.add(25)    // "Описание"

        for (i in alDim.indices) {
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
        for ((keyObject, alObjectResult) in tmResult) {
            if (alObjectResult.isEmpty()) continue

            val rc = alObjectResult.size

            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            if (rc > 1) sheet.mergeCells(0, offsY, 0, offsY + rc - 1)

            sheet.addCell(Label(1, offsY, keyObject, wcfCellL))
            if (rc > 1) sheet.mergeCells(1, offsY, 1, offsY + rc - 1)

            for (td in alObjectResult) {
                sheet.addCell(
                    Label(
                        2, offsY, if (td.begTime == 0) "-"
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

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if (reportObject == 0) {
            loadObjectList(conn, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        } else {
            alObjectID.add(reportObject)
        }

        for (objectID in alObjectID) {
            val oc = (application as iMMSApplication).getObjectConfig(userConfig, objectID)

            //--- отключенные объекты в отчёт попасть не должны
            if (oc.isDisabled) continue

            //--- нестандартное object-info: сначала идёт имя пользователя
            val sbObjectKey = StringBuilder(getRecordUserName(oc.userId)).append('\n').append(oc.name)
            if (oc.model.isNotEmpty()) {
                sbObjectKey.append(", ").append(oc.model)
            }
            if (oc.groupName.isNotEmpty() || oc.departmentName.isNotEmpty()) {
                sbObjectKey.append('\n').append(oc.groupName).append(if (oc.groupName.isEmpty() || oc.departmentName.isEmpty()) "" else ", ").append(oc.departmentName)
            }
            val objectKey = sbObjectKey.toString()

            val curTime = getCurrentTimeInt()
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, curTime - (application as iMMSApplication).expirePeriod * 7 * 24 * 60 * 60, curTime)

            val lastDataTime = alRawTime.lastOrNull()
            if (lastDataTime != null) {
                if (curTime - lastDataTime > reportPeriod) {
                    addTrouble(tmResult, objectKey, TroubleData(lastDataTime, "Контроллер", "Нет данных", TroubleLevel.ERROR))
                }
            } else {
                addTrouble(
                    tmResult, objectKey, TroubleData(
                        0, "Контроллер", "Нет данных более ${(application as iMMSApplication).expirePeriod} недель(и)", TroubleLevel.ERROR
                    )
                )
            }

            oc.scg?.let { scg ->
                var noGeoTime: Int? = null
                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val gd = AbstractObjectStateCalc.getGeoData(scg, bbIn)
                    if (gd == null || gd.wgs.x == 0 && gd.wgs.y == 0) {
                        noGeoTime = time
                    } else {
                        if (curTime - (noGeoTime ?: time) > reportPeriod) {
                            addTrouble(
                                tmResult,
                                objectKey,
                                TroubleData(noGeoTime ?: time, "Гео-датчик", "Нет данных с гео-датчика", TroubleLevel.ERROR)
                            )
                        }
                        break
                    }
                }
                //--- отдельный пункт "неисправности" - передвижной объект имеет почти нулевые пробеги за заданный период
                val objectCalc = ObjectCalc.calcObject(stm, userConfig, oc, curTime - reportPeriod, curTime)
                //--- пробег менее 100 метров
                if (objectCalc.gcd!!.run < 0.1) {
                    addTrouble(tmResult, objectKey, TroubleData(curTime - reportPeriod, "Объект", "Нет пробега", TroubleLevel.ERROR))
                }
            }

            //--- liquid level sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
                val sca = sc as SensorConfigLiquidLevel

                var oldLLTime: Int? = null
                var oldLLData: Int? = null

                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(sca.portNum, bbIn)
                    if (sensorData != null) {
                        if (oldLLData == null) {
                            oldLLTime = time
                            oldLLData = sensorData.toInt()
                        } else if (oldLLData == sensorData) {
                            oldLLTime = time
                        } else {
                            break
                        }
                    }
                }

                if (oldLLTime == null) {
                    addTrouble(tmResult, objectKey, TroubleData(0, sca.descr, "Нет данных", TroubleLevel.ERROR))
                } else {
                    SensorConfigLiquidLevel.hmLLErrorCodeDescr[oldLLData]?.let { errorDescr ->
                        if (curTime - oldLLTime > reportPeriod) {
                            addTrouble(tmResult, objectKey, TroubleData(oldLLTime, sca.descr, errorDescr, TroubleLevel.ERROR))
                        }
                    }
                }
            }

            //--- liquid using counter's work state sensors
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
                var oldLCSTime: Int? = null
                var oldLCSData: Int? = null

                for (i in alRawTime.size - 1 downTo 0) {
                    val time = alRawTime[i]
                    val bbIn = alRawData[i]

                    val sensorData = AbstractObjectStateCalc.getSensorData(sc.portNum, bbIn)
                    if (sensorData != null) {
                        if (oldLCSData == null) {
                            oldLCSTime = time
                            oldLCSData = sensorData.toInt()
                        } else if (oldLCSData == sensorData) {
                            oldLCSTime = time
                        } else {
                            break
                        }
                    }
                }

                if (oldLCSTime == null) {
                    addTrouble(tmResult, objectKey, TroubleData(0, sc.descr, "Нет данных", TroubleLevel.ERROR))
                } else if (oldLCSData in listOf(
                        SensorConfigCounter.STATUS_OVERLOAD,
                        SensorConfigCounter.STATUS_CHEAT,
                        SensorConfigCounter.STATUS_REVERSE,
                        SensorConfigCounter.STATUS_INTERVENTION,
                    )
                ) {
                    SensorConfigCounter.hmStatusDescr[oldLCSData]?.let { errorDescr ->
                        if (curTime - oldLCSTime > reportPeriod) {
                            addTrouble(tmResult, objectKey, TroubleData(oldLCSTime, sc.descr, errorDescr, TroubleLevel.ERROR))
                        }
                    }
                }
            }

        }
        return tmResult
    }

    private fun addTrouble(tmResult: TreeMap<String, ArrayList<TroubleData>>, objectKey: String, troubleData: TroubleData) {
        var alTrouble: ArrayList<TroubleData>? = tmResult[objectKey]
        if (alTrouble == null) {
            alTrouble = ArrayList()
            tmResult[objectKey] = alTrouble
        }
        alTrouble.add(troubleData)
    }

}

package foatto.mms.core_mms.report

import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.secondIntervalToString
import foatto.core.util.getSBFromIterable
import foatto.core.util.getSplittedDouble
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.OverSensorPeriodData
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.sensor.SensorConfigA
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*
import kotlin.math.max

class cOverSensor : cMMSReport() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

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
        var sensorType = 0
        when(aliasConfig.alias) {
            "mms_report_over_weight" -> sensorType = SensorConfig.SENSOR_WEIGHT
            "mms_report_over_turn" -> sensorType = SensorConfig.SENSOR_TURN
            "mms_report_over_pressure" -> sensorType = SensorConfig.SENSOR_PRESSURE
            "mms_report_over_temperature" -> sensorType = SensorConfig.SENSOR_TEMPERATURE
            "mms_report_over_voltage" -> sensorType = SensorConfig.SENSOR_VOLTAGE
        }//??? liquid_level - просто уровень жидкости, без заправок/сливов
        //??? energo_power - электрическая мощность

        //--- загрузить данные по ВСЕМ зонам (reportZone используется только для последующей фильтрации)
        val hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)
        val alAllResult = calcReport(sensorType, hmZoneData)

        //--- загрузка стартовых параметров
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = fillReportHeader(if(reportZone == 0) null else hmZoneData[reportZone], sheet, offsY)

        offsY = max(offsY, outReportCap(sheet, 4, 0) + 1)

        //--- установка размеров заголовков (общая ширина = 90 для А4 портрет и 140 для А4 ландшафт поля по 10 мм)
        val alDim = mutableListOf<Int>()
        alDim.add(5)    // "N п/п"
        alDim.add(9)    // "Начало" - в две строки
        alDim.add(9)    // "Окончание" - в две строки
        alDim.add(9)    // "Продолжительность"
        alDim.add(8)    // "Макс. значение"
        alDim.add(6)    // "Макс. значение"
        alDim.add(44)    // "Место"

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
        sheet.addCell(Label(offsX++, offsY, "Макс. значение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Макс. нару-шение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Место", wcfCaptionHC))
        offsY++

        var countNN = 1
        for(alOSPD in alAllResult) {
            sheet.addCell(Label(1, offsY, alOSPD[0].objectConfig!!.name, wcfCellCB))
            sheet.mergeCells(1, offsY, 6, offsY + 2)
            offsY += 3

            for(ospd in alOSPD) {
                offsX = 0

                sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, ospd.begTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, DateTime_DMYHMS(zoneId, ospd.endTime), wcfCellC))
                sheet.addCell(Label(offsX++, offsY, secondIntervalToString(ospd.begTime, ospd.endTime), wcfCellC))
                sheet.addCell(
                    Label(
                        offsX++, offsY, getSplittedDouble(
                            ospd.maxOverSensorMax, ObjectCalc.getPrecision(ospd.maxOverSensorMax)
                        ).toString(), wcfCellR
                    )
                )
                sheet.addCell(
                    Label(
                        offsX++, offsY, getSplittedDouble(
                            ospd.maxOverSensorDiff, ObjectCalc.getPrecision(ospd.maxOverSensorDiff)
                        ).toString(), wcfCellR
                    )
                )
                sheet.addCell(Label(offsX++, offsY, ospd.sbZoneName!!.toString(), wcfCellL))

                offsY++
            }
        }
        offsY++

        sheet.addCell(Label(6, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 5, offsY, 6, offsY );

        outReportSignature(sheet, intArrayOf(0, 3, 4), offsY + 3)
    }

    //----------------------------------------------------------------------------------------------------------------------

    private fun calcReport(sensorType: Int, hmZoneData: Map<Int, ZoneData>): List<List<OverSensorPeriodData>> {

        val alAllResult = mutableListOf<MutableList<OverSensorPeriodData>>()

        //--- загрузка стартовых параметров
        val reportObjectUser = hmReportParam["report_object_user"] as Int
        val reportObject = hmReportParam["report_object"] as Int
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int

        val reportZone = hmReportParam["report_zone"] as Int

        val (begTime, endTime) = getBegEndTimeFromParam()

        val graphicHandler = AnalogGraphicHandler()

        val alObjectID = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if(reportObject == 0) loadObjectList(stm, userConfig, reportObjectUser, reportDepartment, reportGroup, alObjectID)
        else alObjectID.add(reportObject)

        for(objectID in alObjectID) {
            val objectConfig = ObjectConfig.getObjectConfig(stm, userConfig, objectID)

            val hmSensorConfig = objectConfig.hmSensorConfig[sensorType]
            if(hmSensorConfig == null || hmSensorConfig.isEmpty()) continue

            val alObjectResult = mutableListOf<OverSensorPeriodData>()

            //--- единоразово загрузим данные по всем датчикам объекта
            val ( alRawTime, alRawData ) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)

            for(portNum in hmSensorConfig.keys) {
                val sca = hmSensorConfig[portNum] as SensorConfigA

                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                ObjectCalc.getSmoothAnalogGraphicData(alRawTime, alRawData, objectConfig, sca, begTime, endTime, 0, 0.0, null, null, null, aLine, graphicHandler)

                var begPos = 0
                var curColorIndex = graphicHandler.lineNormalColorIndex
                for(i in 1 until aLine.alGLD.size) {
                    val gdl = aLine.alGLD[i]
                    val newColorIndex = gdl.colorIndex
                    //--- начался период нового типа, оканчиваем предыдущий период другого типа
                    if(newColorIndex != curColorIndex) {
                        //--- предыдущий период закончился в предыдущей точке
                        val endPos = i - 1
                        //--- в периоде должно быть как минимум две точки, одноточечные периоды отбрасываем
                        //--- (обычно это стартовая точка в "нормальном" состоянии)
                        if(begPos < endPos) {
                            //--- закончился предыдущий "ненормальный" период
                            if(newColorIndex == graphicHandler.lineNormalColorIndex) calcOverSensor(
                                reportZone, hmZoneData, graphicHandler, objectConfig, sca, aLine, begPos, endPos, curColorIndex, alObjectResult
                            )
                        }
                        //--- новый период на самом деле начинается с предыдущей точки
                        begPos = i - 1
                        curColorIndex = newColorIndex
                    }
                }
                //--- закончим последний период
                val endPos = aLine.alGLD.size - 1
                //--- заканчиваем предыдущий "ненормальный" период
                if(curColorIndex != graphicHandler.lineNormalColorIndex) calcOverSensor(
                    reportZone, hmZoneData, graphicHandler, objectConfig, sca, aLine, begPos, endPos, curColorIndex, alObjectResult
                )
            }
            if(!alObjectResult.isEmpty()) alAllResult.add(alObjectResult)
        }
        return alAllResult
    }

    private fun calcOverSensor(
        reportZone: Int, hmZoneData: Map<Int, ZoneData>, graphicHandler: AnalogGraphicHandler, objectConfig: ObjectConfig, sca: SensorConfigA, aLine: GraphicDataContainer,
        begPos: Int, endPos: Int, curColorIndex: GraphicColorIndex, alObjectResult: MutableList<OverSensorPeriodData>
    ) {
        //--- поиск точки с максимальным нарушением
        var maxOverSensorTime = 0
        var maxOverSensorCoord: XyPoint? = null
        var maxOverSensorMax = 0.0
        var maxOverSensorDiff = 0.0
        for(pos in begPos..endPos) {
            val y = aLine.alGLD[pos].y
            val over = if(curColorIndex == graphicHandler.lineCriticalColorIndex) y - sca.maxLimit else sca.minLimit - y
            if(over > maxOverSensorDiff) {
                maxOverSensorTime = aLine.alGLD.get(pos).x
                maxOverSensorCoord = aLine.alGLD.get(pos).coord
                maxOverSensorMax = y
                maxOverSensorDiff = over
            }
        }

        val tsZoneName = TreeSet<String>()
        //--- могут быть объекты без GPS-датчиков
        if(maxOverSensorCoord != null) {
            val inZone = ObjectCalc.fillZoneList(hmZoneData, reportZone, maxOverSensorCoord, tsZoneName)
            //--- фильтр по геозонам, если задано
            if(reportZone != 0 && !inZone) return
        }

        val ospd = OverSensorPeriodData(
            aLine.alGLD[begPos].x, aLine.alGLD.get(endPos).x, maxOverSensorTime, maxOverSensorCoord!!, maxOverSensorMax, maxOverSensorDiff
        )
        ospd.objectConfig = objectConfig
        ospd.sbZoneName = getSBFromIterable(tsZoneName, ", ")

        alObjectResult.add(ospd)
    }
}

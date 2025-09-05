package foatto.mms.core_mms.report

import foatto.core.app.chart.ChartColorIndex
import foatto.core.app.chart.ChartElementDTO
import foatto.core.app.chart.ChartElementTypeDTO
import foatto.core.app.xy.geom.XyPoint
import foatto.core.link.FormData
import foatto.core.util.getSplittedDouble
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.calc.OverSensorPeriodData
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.iMMSApplication
import foatto.util.DateTime_DMYHMS
import foatto.util.getSBFromIterable
import foatto.util.secondIntervalToString
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cOverSensor : cMMSReport() {

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

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
        val sensorType = when (aliasConfig.name) {
            "mms_report_over_weight" -> SensorConfig.SENSOR_WEIGHT
            "mms_report_over_turn" -> SensorConfig.SENSOR_TURN
            "mms_report_over_pressure" -> SensorConfig.SENSOR_PRESSURE
            "mms_report_over_temperature" -> SensorConfig.SENSOR_TEMPERATURE
            "mms_report_over_voltage" -> SensorConfig.SENSOR_VOLTAGE
            "mms_report_over_power" -> SensorConfig.SENSOR_POWER
            "mms_report_over_density" -> SensorConfig.SENSOR_DENSITY
            "mms_report_over_mass_flow" -> SensorConfig.SENSOR_MASS_FLOW
            "mms_report_over_volume_flow" -> SensorConfig.SENSOR_VOLUME_FLOW
            "mms_report_over_energo_voltage" -> SensorConfig.SENSOR_ENERGO_VOLTAGE
            "mms_report_over_energo_current" -> SensorConfig.SENSOR_ENERGO_CURRENT
            "mms_report_over_energo_power_koef" -> SensorConfig.SENSOR_ENERGO_POWER_KOEF
            "mms_report_over_energo_power_active" -> SensorConfig.SENSOR_ENERGO_POWER_ACTIVE
            "mms_report_over_energo_power_reactive" -> SensorConfig.SENSOR_ENERGO_POWER_REACTIVE
            "mms_report_over_energo_power_full" -> SensorConfig.SENSOR_ENERGO_POWER_FULL
//!!! не реализовано (и надо ли?)
//            "mms_report_over_energo_transform_koef_current" -> SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_CURRENT
//            "mms_report_over_energo_transform_koef_voltage" -> SensorConfig.SENSOR_ENERGO_TRANSFORM_KOEF_VOLTAGE
            else -> 0
        }

        //--- загрузить данные по ВСЕМ зонам (reportZone используется только для последующей фильтрации)
        val hmZoneData = ZoneData.getZoneData(conn, userConfig, 0)
        val alAllResult = calcReport(sensorType, hmZoneData)

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
        alDim.add(8)    // "Макс. значение"
        alDim.add(6)    // "Макс. значение"
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
        sheet.addCell(Label(offsX++, offsY, "Макс. значение", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Макс. нару-шение", wcfCaptionHC))
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
                sheet.addCell(
                    Label(
                        offsX++,
                        offsY,
                        getSplittedDouble(
                            ospd.maxOverSensorMax, ObjectCalc.getPrecision(ospd.maxOverSensorMax), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider
                        ),
                        wcfCellR
                    )
                )
                sheet.addCell(
                    Label(
                        offsX++,
                        offsY,
                        getSplittedDouble(
                            ospd.maxOverSensorDiff, ObjectCalc.getPrecision(ospd.maxOverSensorDiff), userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider
                        ),
                        wcfCellR
                    )
                )
                sheet.addCell(Label(offsX++, offsY, ospd.sbZoneName!!.toString(), wcfCellL))

                offsY++
            }
        }
        offsY++

        sheet.addCell(Label(6, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 5, offsY, 6, offsY );
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
        val overColorIndexes = setOf(graphicHandler.getLineAboveColorIndex(0), graphicHandler.getLineBelowColorIndex(0))

        val alobjectId = mutableListOf<Int>()
        //--- если объект не указан, то загрузим полный список доступных объектов
        if (reportObject == 0) {
            loadObjectList(conn, userConfig, reportObjectUser, reportDepartment, reportGroup, alobjectId)
        } else {
            alobjectId.add(reportObject)
        }

        for (objectId in alobjectId) {
            val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectId)

            val hmSensorConfig = objectConfig.hmSensorConfig[sensorType]
            if (hmSensorConfig == null || hmSensorConfig.isEmpty()) continue

            val alObjectResult = mutableListOf<OverSensorPeriodData>()

            //--- load data on all sensors of the object once
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(conn, objectConfig, begTime, endTime)

            for (portNum in hmSensorConfig.keys) {
                val scsc = hmSensorConfig[portNum] as SensorConfigAnalogue

                val aLine = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 1, false)
                ObjectCalc.getSmoothAnalogGraphicData(
                    conn = conn,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    scg = objectConfig.scg!!,
                    sca = scsc,
                    begTime = begTime,
                    endTime = endTime,
                    xScale = 0,
                    yScale = 0.0,
                    axisIndex = 0,
                    aMinLimit = null,
                    aMaxLimit = null,
                    aLine = aLine,
                    graphicHandler = graphicHandler,
                )

                var begPos = 0
                var curColorIndex = graphicHandler.getLineNormalColorIndex(0)
                for (i in 1 until aLine.alGLD.size) {
                    val gdl = aLine.alGLD[i]
                    val newColorIndex = gdl.colorIndex
                    //--- a period of a new type has begun, we end the previous period of a different type
                    if (newColorIndex != curColorIndex) {
                        //--- the previous period ended at the previous point
                        val endPos = i - 1
                        //--- the previous "abnormal" period has ended
                        if (curColorIndex in overColorIndexes
                            && newColorIndex !in overColorIndexes
                            && aLine.alGLD[endPos].x > aLine.alGLD[begPos].x
                        ) {
                            calcOverSensor(reportZone, hmZoneData, graphicHandler, objectConfig, scsc, aLine, begPos, endPos, curColorIndex, alObjectResult)
                        }
                        //--- the new period actually starts from the previous point
                        begPos = i - 1
                        curColorIndex = newColorIndex
                    }
                }
                //--- let's finish the last period
                val endPos = aLine.alGLD.size - 1
                //--- ending the previous "abnormal" period
                if (curColorIndex in overColorIndexes
                    && aLine.alGLD[endPos].x > aLine.alGLD[begPos].x
                ) {
                    calcOverSensor(reportZone, hmZoneData, graphicHandler, objectConfig, scsc, aLine, begPos, endPos, curColorIndex, alObjectResult)
                }
            }
            if (alObjectResult.isNotEmpty()) {
                alAllResult.add(alObjectResult)
            }
        }
        return alAllResult
    }

    private fun calcOverSensor(
        reportZone: Int,
        hmZoneData: Map<Int, ZoneData>,
        graphicHandler: AnalogGraphicHandler,
        objectConfig: ObjectConfig,
        scsc: SensorConfigAnalogue,
        aLine: ChartElementDTO,
        begPos: Int,
        endPos: Int,
        curColorIndex: ChartColorIndex,
        alObjectResult: MutableList<OverSensorPeriodData>
    ) {
        //--- finding the point with maximum violation
        var maxOverSensorTime = 0
        var maxOverSensorCoord: XyPoint? = null
        var maxOverSensorMax = 0.0
        var maxOverSensorDiff = 0.0
        for (pos in begPos..endPos) {
            val y = aLine.alGLD[pos].y
            val over = when (curColorIndex) {
                graphicHandler.getLineAboveColorIndex(0) -> {
                    y - scsc.maxLimit
                }

                graphicHandler.getLineBelowColorIndex(0) -> {
                    scsc.minLimit - y
                }

                else -> {
                    0.0
                }
            }
            if (over > maxOverSensorDiff) {
                maxOverSensorTime = aLine.alGLD[pos].x
                maxOverSensorCoord = aLine.alGLD[pos].coord
                maxOverSensorMax = y.toDouble()
                maxOverSensorDiff = over
            }
        }

        val tsZoneName = TreeSet<String>()
        //--- there may be objects without GPS sensors
        if (maxOverSensorCoord != null) {
            val inZone = ObjectCalc.fillZoneList(hmZoneData, reportZone, maxOverSensorCoord, tsZoneName)
            //--- filter by geofences, if set
            if (reportZone != 0 && !inZone) {
                return
            }
        }

        val ospd = OverSensorPeriodData(
            aBegTime = aLine.alGLD[begPos].x,
            aEndTime = aLine.alGLD[endPos].x,
            maxOverSensorTime = maxOverSensorTime,
            maxOverSensorCoord = maxOverSensorCoord,
            maxOverSensorMax = maxOverSensorMax,
            maxOverSensorDiff = maxOverSensorDiff
        )
        ospd.objectConfig = objectConfig
        ospd.sbZoneName = getSBFromIterable(tsZoneName, ", ")

        alObjectResult.add(ospd)
    }
}

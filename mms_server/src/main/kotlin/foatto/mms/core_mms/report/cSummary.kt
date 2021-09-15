package foatto.mms.core_mms.report

import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.graphic.server.document.sdcAnalog
import foatto.mms.core_mms.graphic.server.document.sdcLiquid
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import jxl.write.Label
import jxl.write.WritableSheet

class cSummary : cStandartPeriodSummary() {

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        fillReportParam(model as mUODGP)

        val m = model as mSummary

        fillReportParam(m.sros)
        fillReportParam(m.sos)

        return getReport()
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        //        int reportObjectUser = (Integer) hmReportParam.get( "report_object_user" );
        //        int reportObject = (Integer) hmReportParam.get( "report_object" );
        val reportDepartment = hmReportParam["report_department"] as Int
        val reportGroup = hmReportParam["report_group"] as Int
        val reportKeepPlaceForComment = hmReportParam["report_keep_place_for_comment"] as Boolean
        val reportOutLiquidLevelMainContainerUsing = hmReportParam["report_out_liquid_level_main_container_using"] as Boolean
        val reportOutTemperature = hmReportParam["report_out_temperature"] as Boolean
        val reportOutDensity = hmReportParam["report_out_density"] as Boolean
        val reportOutGroupSum = hmReportParam["report_out_group_sum"] as Boolean
        val reportOutTroubles = hmReportParam["report_out_troubles"] as Boolean

        val (begTime, endTime) = getBegEndTimeFromParam()

        defineFormats(8, 2, 0)
        var offsY = fillReportTitleWithTime(sheet)

        offsY = fillReportHeader(reportDepartment, reportGroup, sheet, 1, offsY)

        offsY = defineSummaryReportHeaders(sheet, offsY)

        val allSumCollector = ReportSumCollector()
        var countNN = 1
        for (objectIndex in alObjectID.indices) {
            val objectConfig = alObjectConfig[objectIndex]
            val objectCalc = ObjectCalc.calcObject(stm, userConfig, objectConfig, begTime, endTime)

            allSumCollector.add(null, objectCalc)

            val troubles = if (reportOutTroubles) {
                val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)
                val t = GraphicDataContainer(GraphicDataContainer.ElementType.TEXT, 0)
                sdcAnalog.checkCommonTrouble(alRawTime, alRawData, objectConfig, begTime, endTime, t)
                //--- ловим ошибки с датчиков уровня топлива
                objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.values?.forEach { sc ->
                    sdcLiquid.checkLiquidLevelSensorTrouble(
                        alRawTime = alRawTime,
                        alRawData = alRawData,
                        sca = sc as SensorConfigAnalogue,
                        begTime = begTime,
                        endTime = endTime,
                        aText = t,
                    )
                }
                t
            } else {
                null
            }

            //--- первая строка: порядковый номер и наименование объекта
            sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))
            offsY = addGroupTitle(sheet, offsY, objectConfig.name)

            offsY = outRow(
                sheet = sheet,
                aOffsY = offsY,
                objectConfig = objectConfig,
                objectCalc = objectCalc,
                isOutLiquidLevelMainContainerUsing = reportOutLiquidLevelMainContainerUsing,
                isOutTemperature = reportOutTemperature,
                isOutDensity = reportOutDensity,
                isKeepPlaceForComment = reportKeepPlaceForComment,
                troubles = troubles,
                isOutGroupSum = reportOutGroupSum,
            )
        }

        sheet.addCell(Label(0, offsY, "ИТОГО общее", wcfCellCBStdYellow))
        sheet.mergeCells(0, offsY, getColumnCount(1), offsY + 2)
        offsY += 4

        offsY = outSumData(sheet, offsY, allSumCollector.sumUser, true, null)

        outReportTrail(sheet, offsY)
    }
}

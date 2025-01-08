package foatto.server.service.chart

import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.AppResponse
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.HeaderData
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.TitleData
import foatto.core.model.response.chart.ChartColorIndex
import foatto.core.model.response.chart.ChartElement
import foatto.core.model.response.chart.ChartElementLine
import foatto.core.model.response.chart.ChartResponse
import foatto.server.SpringApp
import foatto.server.appModuleConfigs
import foatto.server.checkAccessPermission
import foatto.server.entity.SensorEntity
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.service.ApplicationService
import foatto.server.service.SensorService
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import kotlin.math.abs

abstract class AbstractChartService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) {

    companion object {

        val hmIndexColor: Map<ChartColorIndex, Int> = mapOf(
            ChartColorIndex.FILL_NEUTRAL to 0xFF_E0_FF_FF.toInt(),
            ChartColorIndex.FILL_NORMAL to 0xFF_E0_FF_E0.toInt(),
            ChartColorIndex.FILL_WARNING to 0xFF_FF_FF_E0.toInt(),
            ChartColorIndex.FILL_CRITICAL to 0xFF_FF_E0_E0.toInt(),

            ChartColorIndex.BORDER_NEUTRAL to 0xFF_C0_FF_FF.toInt(),
            ChartColorIndex.BORDER_NORMAL to 0xFF_B0_F0_B0.toInt(),
            ChartColorIndex.BORDER_WARNING to 0xFF_E0_E0_C0.toInt(),
            ChartColorIndex.BORDER_CRITICAL to 0xFF_FF_C0_C0.toInt(),

            ChartColorIndex.TEXT_NEUTRAL to 0xFF_00_00_80.toInt(),
            ChartColorIndex.TEXT_NORMAL to 0xFF_00_80_00.toInt(),
            ChartColorIndex.TEXT_WARNING to 0xFF_80_80_00.toInt(),
            ChartColorIndex.TEXT_CRITICAL to 0xFF_80_00_00.toInt(),

            ChartColorIndex.POINT_NEUTRAL to 0xFF_C0_C0_FF.toInt(),
            ChartColorIndex.POINT_NORMAL to 0xFF_C0_FF_C0.toInt(),
            ChartColorIndex.POINT_BELOW to 0xFF_E0_E0_A0.toInt(),
            ChartColorIndex.POINT_ABOVE to 0xFF_FF_C0_C0.toInt(),

            ChartColorIndex.AXIS_0 to 0xFF_80_C0_80.toInt(),
            ChartColorIndex.AXIS_1 to 0xFF_80_80_C0.toInt(),
            ChartColorIndex.AXIS_2 to 0xFF_C0_80_80.toInt(),
            ChartColorIndex.AXIS_3 to 0xFF_C0_80_C0.toInt(),

            ChartColorIndex.LINE_LIMIT to 0xFF_FF_A0_A0.toInt(),

            ChartColorIndex.LINE_NONE_0 to 0x00_80_80_80.toInt(),
            ChartColorIndex.LINE_NORMAL_0 to 0xFF_00_E0_00.toInt(),
            ChartColorIndex.LINE_BELOW_0 to 0xFF_00_60_E0.toInt(),
            ChartColorIndex.LINE_ABOVE_0 to 0xFF_E0_60_00.toInt(),

            ChartColorIndex.LINE_NONE_1 to 0x00_90_90_90.toInt(),
            ChartColorIndex.LINE_NORMAL_1 to 0xFF_00_00_E0.toInt(),

            ChartColorIndex.LINE_NONE_2 to 0x00_A0_A0_A0.toInt(),
            ChartColorIndex.LINE_NORMAL_2 to 0xFF_E0_00_00.toInt(),

            ChartColorIndex.LINE_NONE_3 to 0x00_B0_B0_B0.toInt(),
            ChartColorIndex.LINE_NORMAL_3 to 0xFF_E0_00_E0.toInt(),
        )

        const val UP_GRAPHIC_VISIBLE: String = "graphic_visible_"
    }

/*
    //--- данные по гео-датчику ( движение/стоянка/ошибка ) показываем только на первом/верхнем графике
    protected var isGeoSensorShowed = false

    //--- общие нештатные ситуации показываем только на первом/верхнем графике
    protected var isCommonTroubleShowed = false
*/
    
    fun chart(
        sessionId: Long,
        action: AppAction,
    ): AppResponse {
        val actionModule = action.module

        val sessionData = SpringApp.getSessionData(sessionId) ?: return AppResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return AppResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return AppResponse(ResponseCode.LOGON_NEED)
        }
        val moduleConfig = appModuleConfigs[actionModule] ?: return AppResponse(ResponseCode.LOGON_NEED)
        val objectEntity = objectRepository.findByIdOrNull(action.id) ?: return AppResponse(ResponseCode.LOGON_NEED)

        val caption = moduleConfig.caption
        val rows = listOf(
            "Наименование объекта" to (objectEntity.name ?: "-"),
            "Модель" to (objectEntity.model ?: "-"),
        )

        return AppResponse(
            responseCode = ResponseCode.MODULE_CHART,
            chart = ChartResponse(
                tabCaption = caption,
                headerData = HeaderData(
                    titles = listOf(
                        TitleData(
                            action = null,
                            text = caption,
                            isBold = true,
                        )
                    ),
                    rows = rows,
                ),
            )
        )
    }

    fun chartAction(chartActionRequest: ChartActionRequest): ChartActionResponse {
        val actionModule = chartActionRequest.action.module

        val sessionData = SpringApp.getSessionData(chartActionRequest.sessionId) ?: return ChartActionResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return ChartActionResponse(ResponseCode.LOGON_NEED)
        if (!checkAccessPermission(actionModule, userConfig.roles)) {
            return ChartActionResponse(ResponseCode.LOGON_NEED)
        }

        return when (chartActionRequest.action.type) {

            ActionType.GET_ELEMENTS -> {
                getCharts(chartActionRequest)
            }

            else -> {
                ChartActionResponse(
                    responseCode = ResponseCode.ERROR,
                )
            }
        }
    }

    protected abstract fun getCharts(chartActionRequest: ChartActionRequest): ChartActionResponse

    protected fun getLines(
        sensorEntity: SensorEntity,
        valueIndex: Int,
        begTime: Int,
        endTime: Int,
        xScale: Float,
        yScale: Float,
        colorIndexFun: (value: Double) -> ChartColorIndex,
        lines: ChartElement,
    ) {
        SensorService.checkAndCreateAggTable(entityManager, sensorEntity.id)

        ApplicationService.withConnection(entityManager) { conn ->
            val rs = conn.executeQuery(
                """
                    SELECT ontime_0, value_$valueIndex
                    FROM MMS_agg_${sensorEntity.id}
                    WHERE ontime_0 BETWEEN $begTime AND $endTime
                    ORDER BY ontime_0
                """
            )
            while (rs.next()) {
                val time = rs.getInt(1)
                val value = rs.getDouble(2)

                val curColorIndex = colorIndexFun(value)

                val lastPoint = lines.lines.lastOrNull()
                if (lastPoint == null || time - lastPoint.x > xScale || abs(value - lastPoint.y) > yScale || curColorIndex != lastPoint.colorIndex) {
                    lines.lines += ChartElementLine(time, value.toFloat(), curColorIndex)
                }
            }
            rs.close()
        }
    }

}

/*
    companion object {
        //const val MIN_CONNECT_OFF_TIME = 15 * 60
        private const val MIN_NO_DATA_TIME = 5 * 60
        private const val MIN_POWER_OFF_TIME = 5 * 60
        private const val MIN_LIQUID_COUNTER_STATE_TIME = 5 * 60

        //--- ловля основных/системных нештатных ситуаций, показываемых только на первом/верхнем графике:
        //--- нет связи, нет данных и резервное питание
        fun checkCommonTrouble(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            oc: ObjectConfig,
            begTime: Int,
            endTime: Int,
            aText: ChartElementDTO
        ) {
            val alGTD = aText.alGTD.toMutableList()

            //--- поиск значительных промежутков отсутствия данных ---

            var lastDataTime = begTime
            for (rawTime in alRawTime) {
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                if (rawTime - lastDataTime > MIN_NO_DATA_TIME) {
                    alGTD += ChartElementTextDTO(
                        textX1 = lastDataTime,
                        textX2 = rawTime,
                        fillColorIndex = ChartColorIndex.FILL_CRITICAL,
                        borderColorIndex = ChartColorIndex.BORDER_CRITICAL,
                        textColorIndex = ChartColorIndex.TEXT_CRITICAL,
                        text = "Нет данных от контроллера",
                        toolTip = "Нет данных от контроллера"
                    )
                }
                lastDataTime = rawTime
            }
            if (min(lastDataTime, endTime) - lastDataTime > MIN_NO_DATA_TIME) {
                alGTD += ChartElementTextDTO(
                    textX1 = lastDataTime,
                    textX2 = min(lastDataTime, endTime),
                    fillColorIndex = ChartColorIndex.FILL_CRITICAL,
                    borderColorIndex = ChartColorIndex.BORDER_CRITICAL,
                    textColorIndex = ChartColorIndex.TEXT_CRITICAL,
                    text = "Нет данных от контроллера",
                    toolTip = "Нет данных от контроллера"
                )
            }

            //--- поиск значительных промежутков отсутствия основного питания ( перехода на резервное питание )
            oc.hmSensorConfig[SensorConfig.SENSOR_VOLTAGE]?.values?.forEach { sc ->
                val sca = sc as SensorConfigAnalogue
                //--- чтобы не смешивались разные ошибки по одному датчику и одинаковые ошибки по разным датчикам,
                //--- добавляем в описание ошибки не только само описание ошибки, но и описание датчика
                checkSensorError(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    portNum = sca.portNum,
                    sensorDescr = sca.descr,
                    begTime = begTime,
                    endTime = endTime,
                    aFillColorIndex = ChartColorIndex.FILL_WARNING,
                    aBorderColorIndex = ChartColorIndex.BORDER_WARNING,
                    aTextColorIndex = ChartColorIndex.TEXT_WARNING,
                    troubleCode = 0,
                    troubleDescr = "Нет питания",
                    minTime = MIN_POWER_OFF_TIME,
                    alGTD = alGTD
                )
            }

//!!! временно отключим - больше мешают, чем помогают
            //--- поиск критических режимов работы счётчика топлива EuroSens Delta
//            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
//                listOf(
//                    SensorConfigCounter.STATUS_OVERLOAD,
//                    SensorConfigCounter.STATUS_CHEAT,
//                    SensorConfigCounter.STATUS_REVERSE,
//                    SensorConfigCounter.STATUS_INTERVENTION,
//                ).forEach { stateCode ->
//                    checkSensorError(
//                        alRawTime = alRawTime,
//                        alRawData = alRawData,
//                        portNum = sc.portNum,
//                        sensorDescr = sc.descr,
//                        begTime = begTime,
//                        endTime = endTime,
//                        aFillColorIndex = GraphicColorIndex.FILL_CRITICAL,
//                        aBorderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
//                        aTextColorIndex = GraphicColorIndex.TEXT_CRITICAL,
//                        troubleCode = stateCode,
//                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
//                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
//                        alGTD = alGTD
//                    )
//                }
//                listOf(
//                    SensorConfigCounter.STATUS_UNKNOWN,
//                    SensorConfigCounter.STATUS_IDLE,
//                    //SensorConfigCounter.STATUS_NORMAL,
//                ).forEach { stateCode ->
//                    checkSensorError(
//                        alRawTime = alRawTime,
//                        alRawData = alRawData,
//                        portNum = sc.portNum,
//                        sensorDescr = sc.descr,
//                        begTime = begTime,
//                        endTime = endTime,
//                        aFillColorIndex = GraphicColorIndex.FILL_WARNING,
//                        aBorderColorIndex = GraphicColorIndex.BORDER_WARNING,
//                        aTextColorIndex = GraphicColorIndex.TEXT_WARNING,
//                        troubleCode = stateCode,
//                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
//                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
//                        alGTD = alGTD
//                    )
//                }
//            }

            aText.alGTD = alGTD
        }

        fun checkSensorError(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            portNum: Int,
            sensorDescr: String,
            begTime: Int,
            endTime: Int,
            aFillColorIndex: ChartColorIndex,
            aBorderColorIndex: ChartColorIndex,
            aTextColorIndex: ChartColorIndex,
            troubleCode: Int,
            troubleDescr: String,
            minTime: Int,
            alGTD: MutableList<ChartElementTextDTO>
        ) {

            //--- в основном тексте пишем только текст ошибки, а в tooltips'e напишем вместе с описанием датчика
            val fullTroubleDescr = StringBuilder(sensorDescr).append(": ").append(troubleDescr).toString()
            var troubleBegTime = 0
            var sensorData: Int

            for (pos in alRawTime.indices) {
                val rawTime = alRawTime[pos]
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                sensorData = AbstractObjectStateCalc.getSensorData(portNum, alRawData[pos])?.toInt() ?: continue
                if (sensorData == troubleCode) {
                    if (troubleBegTime == 0) {
                        troubleBegTime = rawTime
                    }
                } else if (troubleBegTime != 0) {
                    if (rawTime - troubleBegTime > minTime) {
                        alGTD += ChartElementTextDTO(
                            textX1 = troubleBegTime,
                            textX2 = rawTime,
                            fillColorIndex = aFillColorIndex,
                            borderColorIndex = aBorderColorIndex,
                            textColorIndex = aTextColorIndex,
                            text = troubleDescr,
                            toolTip = fullTroubleDescr
                        )
                    }
                    troubleBegTime = 0
                }
            }
            //--- запись последней незакрытой проблемы
            if (troubleBegTime != 0 && min(getCurrentTimeInt(), endTime) - troubleBegTime > minTime) {
                alGTD += ChartElementTextDTO(
                    textX1 = troubleBegTime,
                    textX2 = min(getCurrentTimeInt(), endTime),
                    fillColorIndex = aFillColorIndex,
                    borderColorIndex = aBorderColorIndex,
                    textColorIndex = aTextColorIndex,
                    text = troubleDescr,
                    toolTip = fullTroubleDescr
                )
            }
        }
    }

 */
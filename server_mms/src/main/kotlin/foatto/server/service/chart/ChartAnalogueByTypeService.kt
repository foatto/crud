package foatto.server.service.chart

import foatto.core.model.request.ChartActionRequest
import foatto.core.model.response.ChartActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.chart.*
import foatto.core.util.getCurrentTimeInt
import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfig
import foatto.server.repository.ObjectRepository
import foatto.server.repository.SensorRepository
import foatto.server.util.AdvancedLogger
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.*

@Service
class ChartAnalogueByTypeService(
    private val entityManager: EntityManager,
    private val objectRepository: ObjectRepository,
    private val sensorRepository: SensorRepository,
) : AbstractChartService(
    entityManager = entityManager,
    objectRepository = objectRepository,
    sensorRepository = sensorRepository,
) {

    override fun getCharts(chartActionRequest: ChartActionRequest): ChartActionResponse {
//        isGeoSensorShowed = false
//        isCommonTroubleShowed = false

        val (begTime, endTime) = chartActionRequest.times ?: run {
            AdvancedLogger.error("ChartAnalogueByTypeService: chartActionRequest.times not defined")
            val defaultEndTime = getCurrentTimeInt()
            defaultEndTime - 86_400 to defaultEndTime
        }
        val (viewWidth, viewHeight) = chartActionRequest.viewSize ?: run {
            AdvancedLogger.error("ChartAnalogueByTypeService: chartActionRequest.viewSize not defined")
            3840.0f to 2160.0f
        }
        //!!! где-то здесь надо получить конкретный тип аналогового датчика (пока будем выводить графики по всем аналоговым датчикам сразу)

        val tmElement = sortedMapOf<String, ChartData>()
        val tmElementVisibleConfig = sortedMapOf<String, Triple<String, String, Boolean>>()

        chartActionRequest.action.id?.let { objectId ->
            objectRepository.findByIdOrNull(objectId)?.let { objectEntity ->
                SensorConfig.analogueSensorTypes.forEach { sensorType ->
                    sensorRepository.findByObjAndSensorType(objectEntity, sensorType).forEach { sensorEntity ->
                        getChart(
                            sensorEntity = sensorEntity,
                            begTime = begTime,
                            endTime = endTime,
                            viewWidth = viewWidth,
                            viewHeight = viewHeight,
                            tmElement = tmElement,
                            tmElementVisibleConfig = tmElementVisibleConfig,
                            legends = emptyList(),
                        )
                    }
                }
            }
        }

        return ChartActionResponse(
            responseCode = ResponseCode.OK,
            colorIndexes = hmIndexColor,
            charts = tmElement.toList(),
            visibleElements = tmElementVisibleConfig.values.toList(),
            legends = emptyList(),
        )
    }

    private fun getChart(
        sensorEntity: SensorEntity,
        begTime: Int,
        endTime: Int,
        viewWidth: Float,
        viewHeight: Float,
        tmElement: SortedMap<String, ChartData>,
        tmElementVisibleConfig: SortedMap<String, Triple<String, String, Boolean>>,
        legends: List<ChartLegend>,
    ) {
        val chartTitle = sensorEntity.descr ?: "-"
        val objectId = sensorEntity.obj?.id ?: 0
        val portNum = sensorEntity.portNum ?: 0
        val minView = sensorEntity.minView?.toFloat() ?: 0.0f
        val maxView = sensorEntity.maxView?.toFloat() ?: 100.0f

        val xScale = (endTime - begTime) / viewWidth
        val yScale = (maxView - minView) / viewHeight

        val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${objectId}_${portNum}"
        val isGraphicVisible = true //userConfig.getUserProperty(graphicVisibilityKey)?.toBooleanStrictOrNull() ?: true
        tmElementVisibleConfig[chartTitle] = Triple(chartTitle, graphicVisibilityKey, isGraphicVisible)

        if (isGraphicVisible) {
            val elements = mutableListOf<ChartElement>()

//            objectConfig.scg?.let { scg ->
//                if (!isGeoSensorShowed && scg.isUseSpeed) {
//                    val aBack = ChartElementDTO(ChartElementTypeDTO.BACK, 0, 0, false)
//                    calcGeoSensor(alRawTime, alRawData, scg, begTime, endTime, aBack)
//                    alGDC.add(aBack)
//
//                    isGeoSensorShowed = true
//                }
//            }

            val alAxisYData = mutableListOf<ChartAxisY>()

            val isReversedY = SensorConfig.isReversedChart(sensorEntity.sensorType)

            val axisIndex = 0
            alAxisYData += ChartAxisY(
                title = chartTitle,
                min = minView,
                max = maxView,
                colorIndex = getChartAxisColorIndexes(axisIndex),
                isReversedY = isReversedY
            )

            sensorEntity.minLimit?.let { minLimit ->
                val minLimitLines = ChartElement(ChartElementType.LINE, axisIndex, 1, isReversedY).apply {
                    lines += ChartElementLine(begTime, minLimit.toFloat(), ChartColorIndex.LINE_LIMIT)
                    lines += ChartElementLine(endTime, minLimit.toFloat(), ChartColorIndex.LINE_LIMIT)
                }
                elements += minLimitLines
            }

            sensorEntity.maxLimit?.let { maxLimit ->
                val maxLimitLines = ChartElement(ChartElementType.LINE, axisIndex, 1, isReversedY).apply {
                    lines += ChartElementLine(begTime, maxLimit.toFloat(), ChartColorIndex.LINE_LIMIT)
                    lines += ChartElementLine(endTime, maxLimit.toFloat(), ChartColorIndex.LINE_LIMIT)
                }
                elements += maxLimitLines
            }

            val lines = ChartElement(ChartElementType.LINE, axisIndex, 3, isReversedY)
            getLines(
                sensorEntity = sensorEntity,
                valueIndex = 1,
                begTime = begTime,
                endTime = endTime,
                xScale = xScale,
                yScale = yScale,
                colorIndexFun = { value: Double ->
                    when {
                        sensorEntity.minLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value < sensorEntity.minLimit -> {
                            getChartLineBelowColorIndexes(axisIndex)
                        }

                        sensorEntity.maxLimit != null && sensorEntity.minLimit != sensorEntity.maxLimit && value > sensorEntity.maxLimit -> {
                            getChartLineAboveColorIndexes(axisIndex)
                        }

                        else -> {
                            getChartLineNormalColorIndexes(axisIndex)
                        }
                    }
                },
                lines = lines,
            )
            elements += lines

//            val texts = ChartElement(ChartElementType.TEXT, axisIndex, 0, isReversedY)
            /*

                            //--- если вывод текстов задан, сделаем вывод режимов работы оборудования
                            objectConfig.hmSensorConfig[SensorConfig.SENSOR_WORK]?.let { hmSC ->
                                val alGTD = aText.alGTD.toMutableList()
                                hmSC.values.forEach { sc ->
                                    val scw = sc as SensorConfigWork
                                    //--- пропускаем датчики работы оборудования не из своей группы
                                    if (scw.group == sca.group) {
                                        val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime).alWorkOnOff
                                        for (apd in alWork) {
                                            val workDescr = StringBuilder(scw.descr).append(" : ").append(if (apd.getState() != 0) "ВКЛ" else "выкл").toString()
                                            alGTD += ChartElementTextDTO(
                                                textX1 = apd.begTime,
                                                textX2 = apd.endTime,
                                                fillColorIndex = if (apd.getState() != 0) {
                                                    ChartColorIndex.FILL_NORMAL
                                                } else {
                                                    ChartColorIndex.FILL_WARNING
                                                },
                                                borderColorIndex = if (apd.getState() != 0) {
                                                    ChartColorIndex.BORDER_NORMAL
                                                } else {
                                                    ChartColorIndex.BORDER_WARNING
                                                },
                                                textColorIndex = if (apd.getState() != 0) {
                                                    ChartColorIndex.TEXT_NORMAL
                                                } else {
                                                    ChartColorIndex.TEXT_WARNING
                                                },
                                                text = workDescr,
                                                toolTip = workDescr
                                            )
                                        }
                                    }
                                }
                                aText.alGTD = alGTD
                            }

                            //--- общие нештатные ситуации показываем после работы оборудования,
                            //--- отображаемого в виде сплошной полосы различного цвета и
                            //--- после специфических (как правило - более критических) ошибок конкретных датчиков
                            if (!isCommonTroubleShowed) {
                                checkCommonTrouble(alRawTime, alRawData, objectConfig, begTime, endTime, aText)
                                isCommonTroubleShowed = true
                            }

                            //--- пост-обработка графика
                            graphicElementPostCalc(
                                begTime = begTime,
                                endTime = endTime,
                                sca = sca,
                                alRawTime = alRawTime,
                                alRawData = alRawData,
                                axisIndex = axisIndex,
                                aLine = aLine,
                                aText = aText,
                            )
                            alGDC.addAll(listOfNotNull(aText, aMinLimit, aMaxLimit, aLine).filter { it.isNotEmpty() })
                            axisIndex++

                            //--- добавление дополнительных графиков на то же поле
                            axisIndex = addGraphicItem(
                                begTime = begTime,
                                endTime = endTime,
                                viewWidth = viewWidth,
                                viewHeight = viewHeight,
                                alRawTime = alRawTime,
                                alRawData = alRawData,
                                objectConfig = objectConfig,
                                sca = sca,
                                aAxisIndex = axisIndex,
                                alAxisYData = alAxisYData,
                                aLine = aLine,
                                aText = aText,
                                alGDC = alGDC,
                            )
             */
//            }

            tmElement[chartTitle] = ChartData(
                title = chartTitle,
                legends = legends,
                height = -1.0f,
                alAxisYData = alAxisYData,
                elements = elements,
            )
        }
    }

}
/*
    //--- упрощённый вывод данных по гео-датчику (движение/стоянка/нет данных АКА ошибка)
    //--- (без учёта минимального времени стоянки)
    private fun calcGeoSensor(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, scg: SensorConfigGeo, begTime: Int, endTime: Int, aBack: ChartElementDTO) {
        var lastStatus = -2 // -1 = нет гео-данных, 0 = стоянка, 1 - движение
        var lastTime = 0
        val alGBD = aBack.alGBD.toMutableList()
        for (pos in alRawTime.indices) {
            val rawTime = alRawTime[pos]
            //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и их можно пропустить
            if (rawTime < begTime) {
                continue
            }
            //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и можно прекращать обработку
            if (rawTime > endTime) {
                break
            }

            val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
            //--- самих геоданных может и не оказаться (нет датчика или нет или ошибка GPS-данных)
            if (gd == null) {
                //--- если до этого было другое состояние (движение или стоянка)
                if (lastStatus != -1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == 0) {
                                ChartColorIndex.FILL_WARNING
                            } else {
                                ChartColorIndex.FILL_NORMAL
                            }] ?: 0
                        )
                    }
                    lastStatus = -1
                    lastTime = rawTime
                }
            } else if (gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
                //--- если до этого было другое состояние (движение или ошибка)
                if (lastStatus != 0) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                ChartColorIndex.FILL_CRITICAL
                            } else {
                                ChartColorIndex.FILL_NORMAL
                            }] ?: 0
                        )
                    }
                    lastStatus = 0
                    lastTime = rawTime
                }
            } else {
                //--- если до этого было другое состояние (стоянка или ошибка)
                if (lastStatus != 1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGBD += ChartElementBackDTO(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                ChartColorIndex.FILL_CRITICAL
                            } else {
                                ChartColorIndex.FILL_WARNING
                            }] ?: 0
                        )
                    }
                    lastStatus = 1
                    lastTime = rawTime
                }
            }//--- движение
            //--- стоянка
        }
        //--- если это не первое состояние
        if (lastTime != 0) {
            alGBD += ChartElementBackDTO(
                x1 = lastTime,
                x2 = min(getCurrentTimeInt(), endTime),
                color = hmIndexColor[if (lastStatus == -1) {
                    ChartColorIndex.FILL_CRITICAL
                } else if (lastStatus == 0) {
                    ChartColorIndex.FILL_WARNING
                } else {
                    ChartColorIndex.FILL_NORMAL
                }] ?: 0
            )
        }
        aBack.alGBD = alGBD
    }

 */
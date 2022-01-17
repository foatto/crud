package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.graphic.*
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.mms.iMMSApplication
import java.util.*
import kotlin.math.min

abstract class sdcAbstractAnalog : sdcAbstractGraphic() {

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
            aText: GraphicDataContainer
        ) {
            val alGTD = aText.alGTD.toMutableList()

            //--- поиск значительных промежутков отсутствия данных ---

            var lastDataTime = begTime
            for (rawTime in alRawTime) {
                //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
                if (rawTime < begTime) continue
                if (rawTime > endTime) break

                if (rawTime - lastDataTime > MIN_NO_DATA_TIME) {
                    alGTD += GraphicTextData(
                        textX1 = lastDataTime,
                        textX2 = rawTime,
                        fillColorIndex = GraphicColorIndex.FILL_CRITICAL,
                        borderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
                        textColorIndex = GraphicColorIndex.TEXT_CRITICAL,
                        text = "Нет данных от контроллера",
                        toolTip = "Нет данных от контроллера"
                    )
                }
                lastDataTime = rawTime
            }
            if (min(lastDataTime, endTime) - lastDataTime > MIN_NO_DATA_TIME) {
                alGTD += GraphicTextData(
                    textX1 = lastDataTime,
                    textX2 = min(lastDataTime, endTime),
                    fillColorIndex = GraphicColorIndex.FILL_CRITICAL,
                    borderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
                    textColorIndex = GraphicColorIndex.TEXT_CRITICAL,
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
                    aFillColorIndex = GraphicColorIndex.FILL_WARNING,
                    aBorderColorIndex = GraphicColorIndex.BORDER_WARNING,
                    aTextColorIndex = GraphicColorIndex.TEXT_WARNING,
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

            aText.alGTD = alGTD.toTypedArray()
        }

        fun checkSensorError(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            portNum: Int,
            sensorDescr: String,
            begTime: Int,
            endTime: Int,
            aFillColorIndex: GraphicColorIndex,
            aBorderColorIndex: GraphicColorIndex,
            aTextColorIndex: GraphicColorIndex,
            troubleCode: Int,
            troubleDescr: String,
            minTime: Int,
            alGTD: MutableList<GraphicTextData>
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
                        alGTD += GraphicTextData(
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
                alGTD += GraphicTextData(
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

    //--- данные по гео-датчику ( движение/стоянка/ошибка ) показываем только на первом/верхнем графике
    private var isGeoSensorShowed = false

    //--- общие нештатные ситуации показываем только на первом/верхнем графике
    private var isCommonTroubleShowed = false

    override fun doGetElements(graphicActionRequest: GraphicActionRequest): GraphicActionResponse {
        isGeoSensorShowed = false
        isCommonTroubleShowed = false

        val graphicStartDataID = graphicActionRequest.startParamId
        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + graphicStartDataID] as GraphicStartData

        val (begTime, endTime) = graphicActionRequest.graphicCoords!!
        val (viewWidth, viewHeight) = graphicActionRequest.viewSize!!

        val objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, sd.objectId)
        //--- загрузка заголовочной информации по объекту
        var sObjectInfo = objectConfig.name

        if (objectConfig.model.isNotEmpty()) {
            sObjectInfo += ", " + objectConfig.model
        }
        if (objectConfig.groupName.isNotEmpty()) {
            sObjectInfo += ", " + objectConfig.groupName
        }
        if (objectConfig.departmentName.isNotEmpty()) {
            sObjectInfo += ", " + objectConfig.departmentName
        }

        //--- единоразово загрузим данные по объекту
        val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)

        val tmElement = sortedMapOf<String, GraphicElement>()
        val tmElementVisibleConfig = sortedMapOf<String, Triple<String, String, Boolean>>()

        getGraphicElements(
            sd = sd,
            begTime = begTime,
            endTime = endTime,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            alRawTime = alRawTime,
            alRawData = alRawData,
            objectConfig = objectConfig,
            tmElement = tmElement,
            tmElementVisibleConfig = tmElementVisibleConfig
        )

        return GraphicActionResponse(
            arrIndexColor = hmIndexColor.toList().toTypedArray(),
            arrElement = tmElement.toList().toTypedArray(),
            arrVisibleElement = tmElementVisibleConfig.values.toTypedArray(),
            arrLegend = emptyArray(),
        )
    }

    //--- realize list of charts/graphics
    abstract fun getGraphicElements(
        sd: GraphicStartData,
        begTime: Int,
        endTime: Int,
        viewWidth: Int,
        viewHeight: Int,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        objectConfig: ObjectConfig,
        tmElement: SortedMap<String, GraphicElement>,
        tmElementVisibleConfig: SortedMap<String, Triple<String, String, Boolean>>,
    )

    fun getGraphicElement(
        graphicTitle: String,
        begTime: Int,
        endTime: Int,
        viewWidth: Int,
        viewHeight: Int,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        objectConfig: ObjectConfig,
        alSca: List<SensorConfigAnalogue>,
        alGraphicHandler: List<AnalogGraphicHandler>,
        tmElement: SortedMap<String, GraphicElement>,
        tmElementVisibleConfig: SortedMap<String, Triple<String, String, Boolean>>,
        alLegend: Array<Triple<Int, Boolean, String>> = emptyArray(),
    ) {
        val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${objectConfig.objectId}_${alSca.first().portNum}"
        val isGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)?.toBooleanStrictOrNull() ?: true
        tmElementVisibleConfig[graphicTitle] = Triple(graphicTitle, graphicVisibilityKey, isGraphicVisible)

        if (isGraphicVisible) {
            val alGDC = mutableListOf<GraphicDataContainer>()

            objectConfig.scg?.let { scg ->
                if (!isGeoSensorShowed && scg.isUseSpeed) {
                    val aBack = GraphicDataContainer(GraphicDataContainer.ElementType.BACK, 0, 0, false)
                    calcGeoSensor(alRawTime, alRawData, scg, begTime, endTime, aBack)
                    alGDC.add(aBack)

                    isGeoSensorShowed = true
                }
            }

            val alAxisYData = mutableListOf<AxisYData>()

            var axisIndex = 0
            alSca.forEachIndexed { sensorIndex, sca ->
                val isReversedY = false //sca.sensorType == SensorConfig.SENSOR_DEPTH

                val graphicHandler = alGraphicHandler[sensorIndex]

                //--- Максимальный размер массива = кол-во точек по горизонтали = 3840 (максимальная ширина 4K-экрана), окгруляем до 4000
                val aMinLimit = if (graphicHandler.isStaticMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, isReversedY)
                } else if (graphicHandler.isDynamicMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, isReversedY)
                } else {
                    null
                }

                val aMaxLimit = if (graphicHandler.isStaticMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, isReversedY)
                } else if (graphicHandler.isDynamicMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, isReversedY)
                } else {
                    null
                }

                val aLine = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 3, isReversedY)
                val aText = GraphicDataContainer(GraphicDataContainer.ElementType.TEXT, axisIndex, 0, isReversedY)

                alAxisYData.add(AxisYData(sca.descr, sca.minView, sca.maxView, graphicAxisColorIndexes[axisIndex], isReversedY))

                ObjectCalc.getSmoothAnalogGraphicData(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    scg = objectConfig.scg,
                    sca = sca,
                    begTime = begTime,
                    endTime = endTime,
                    xScale = if (viewWidth == 0) {
                        0
                    } else {
                        (endTime - begTime) / (viewWidth / DOT_PER_MM)
                    },
                    yScale = if (viewHeight == 0) {
                        0.0
                    } else {
                        (sca.maxView - sca.minView) / (viewHeight / DOT_PER_MM)
                    },
                    axisIndex = axisIndex,
                    aMinLimit = aMinLimit,
                    aMaxLimit = aMaxLimit,
                    aLine = aLine,
                    graphicHandler = graphicHandler
                )

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
                                alGTD += GraphicTextData(
                                    textX1 = apd.begTime,
                                    textX2 = apd.endTime,
                                    fillColorIndex = if (apd.getState() != 0) {
                                        GraphicColorIndex.FILL_NORMAL
                                    } else {
                                        GraphicColorIndex.FILL_WARNING
                                    },
                                    borderColorIndex = if (apd.getState() != 0) {
                                        GraphicColorIndex.BORDER_NORMAL
                                    } else {
                                        GraphicColorIndex.BORDER_WARNING
                                    },
                                    textColorIndex = if (apd.getState() != 0) {
                                        GraphicColorIndex.TEXT_NORMAL
                                    } else {
                                        GraphicColorIndex.TEXT_WARNING
                                    },
                                    text = workDescr,
                                    toolTip = workDescr
                                )
                            }
                        }
                    }
                    aText.alGTD = alGTD.toTypedArray()
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
                alGDC.addAll(listOfNotNull(aText, aMinLimit, aMaxLimit, aLine).filter { it.itNotEmpty() })
                axisIndex++

                //!!! расчёт и добавление вычисленной скорости расхода топлива
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
            }

            tmElement[graphicTitle] = GraphicElement(
                graphicTitle = graphicTitle,
                alLegend = alLegend,
                graphicHeight = -1.0,
                alAxisYData = alAxisYData.toTypedArray(),
                alGDC = alGDC.toTypedArray()
            )
        }
    }

    //--- пост-обработка графика
    protected open fun graphicElementPostCalc(
        begTime: Int,
        endTime: Int,
        sca: SensorConfigAnalogue,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        axisIndex: Int,
        aLine: GraphicDataContainer?,
        aText: GraphicDataContainer?,
    ) {
    }

    protected open fun addGraphicItem(
        begTime: Int,
        endTime: Int,
        viewWidth: Int,
        viewHeight: Int,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        objectConfig: ObjectConfig,
        sca: SensorConfigAnalogue,
        aAxisIndex: Int,
        alAxisYData: MutableList<AxisYData>,
        aLine: GraphicDataContainer?,
        aText: GraphicDataContainer?,
        alGDC: MutableList<GraphicDataContainer>,
    ): Int = aAxisIndex

    //--- упрощённый вывод данных по гео-датчику (движение/стоянка/нет данных АКА ошибка)
    //--- (без учёта минимального времени стоянки)
    private fun calcGeoSensor(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, scg: SensorConfigGeo, begTime: Int, endTime: Int, aBack: GraphicDataContainer) {
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
                        alGBD += GraphicBackData(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == 0) {
                                GraphicColorIndex.FILL_WARNING
                            } else {
                                GraphicColorIndex.FILL_NORMAL
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
                        alGBD += GraphicBackData(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                GraphicColorIndex.FILL_CRITICAL
                            } else {
                                GraphicColorIndex.FILL_NORMAL
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
                        alGBD += GraphicBackData(
                            x1 = lastTime,
                            x2 = rawTime,
                            color = hmIndexColor[if (lastStatus == -1) {
                                GraphicColorIndex.FILL_CRITICAL
                            } else {
                                GraphicColorIndex.FILL_WARNING
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
            alGBD += GraphicBackData(
                x1 = lastTime,
                x2 = min(getCurrentTimeInt(), endTime),
                color = hmIndexColor[if (lastStatus == -1) {
                    GraphicColorIndex.FILL_CRITICAL
                } else if (lastStatus == 0) {
                    GraphicColorIndex.FILL_WARNING
                } else {
                    GraphicColorIndex.FILL_NORMAL
                }] ?: 0
            )
        }
        aBack.alGBD = alGBD.toTypedArray()
    }

}
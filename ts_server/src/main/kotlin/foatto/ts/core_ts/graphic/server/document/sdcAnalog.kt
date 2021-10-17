package foatto.ts.core_ts.graphic.server.document

import foatto.core.app.UP_GRAPHIC_SHOW_BACK
import foatto.core.app.UP_GRAPHIC_SHOW_LINE
import foatto.core.app.UP_GRAPHIC_SHOW_POINT
import foatto.core.app.UP_GRAPHIC_SHOW_TEXT
import foatto.core.app.graphic.*
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.calc.AbstractObjectStateCalc
import foatto.ts.core_ts.calc.ObjectCalc
import foatto.ts.core_ts.graphic.server.TSGraphicDocumentConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.ts.core_ts.graphic.server.graphic_handler.iGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication
import java.util.*
import kotlin.math.min

open class sdcAnalog : sdcAbstractGraphic() {

    companion object {
        //--- в худшем случае у нас как минимум 4 точки на мм ( 100 dpi ),
        //--- нет смысла выводить данные в каждый пиксель, достаточно в каждый мм
        private const val DOT_PER_MM = 4

        //const val MIN_CONNECT_OFF_TIME = 15 * 60
        private const val MIN_NO_DATA_TIME = 5 * 60

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
                        text = "Нет данных от прибора",
                        toolTip = "Нет данных от прибора"
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
                    text = "Нет данных от прибора",
                    toolTip = "Нет данных от прибора"
                )
            }

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

    override fun doGetElements(graphicActionRequest: GraphicActionRequest): GraphicActionResponse {
        //long begTime = System.currentTimeMillis();

        val tsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as TSGraphicDocumentConfig
        val sensorType = tsgdc.sensorType
        val agh = tsgdc.graphicHandler as AnalogGraphicHandler

        val graphicStartDataID = graphicActionRequest.startParamId
        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + graphicStartDataID] as GraphicStartData

        val x1 = graphicActionRequest.graphicCoords!!.first
        val x2 = graphicActionRequest.graphicCoords!!.second
        val viewWidth = graphicActionRequest.viewSize!!.first
        val viewHeight = graphicActionRequest.viewSize!!.second

        val smBack = userConfig.getUserProperty(UP_GRAPHIC_SHOW_BACK)
        val smPoint = userConfig.getUserProperty(UP_GRAPHIC_SHOW_POINT)
        val smLine = userConfig.getUserProperty(UP_GRAPHIC_SHOW_LINE)
        val smText = userConfig.getUserProperty(UP_GRAPHIC_SHOW_TEXT)

        //--- показ фона по умолчанию включен, если не указано явно иное
        val isShowBack = smBack?.toBoolean() ?: true
        //--- показ точек по умолчанию выключен, если не указано явно иное
        val isShowPoint = smPoint?.toBoolean() ?: false
        //--- показ линий по умолчанию включен, если не указано явно иное
        val isShowLine = smLine?.toBoolean() ?: true
        //--- показ текстов по умолчанию включен, если не указано явно иное
        val isShowText = smText?.toBoolean() ?: true

        val oc = (application as iTSApplication).getObjectConfig(userConfig, sd.objectId)
        //--- загрузка заголовочной информации по объекту
        var sObjectInfo = oc.name

        if (oc.model.isNotEmpty()) {
            sObjectInfo += ", " + oc.model
        }

        val hmSensorConfig = oc.hmSensorConfig[sensorType]
        val tmElement = TreeMap<String, GraphicElement>()
        val tmElementVisibleConfig = TreeMap<String, String>()
        if (hmSensorConfig != null) {
            //--- единоразово загрузим данные по объекту
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, x1, x2)
            //--- общие нештатные ситуации показываем только на первом/верхнем графике
            var isCommonTroubleShowed = false

            for (portNum in hmSensorConfig.keys) {
                val sca = hmSensorConfig[portNum] as SensorConfigAnalogue

                val isReversedY = sca.sensorType == SensorConfig.SENSOR_DEPTH

                //--- заранее заполняем список определений видимости графиков
                val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${sd.objectId}_${sca.portNum}"
                tmElementVisibleConfig[sca.descr] = graphicVisibilityKey

                //--- а сейчас уже можно и нужно проверять на видимость графика
                val strGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)
                val isGraphicVisible = strGraphicVisible?.toBoolean() ?: true

                if (!isGraphicVisible) {
                    continue
                }

                val alAxisYData = mutableListOf<AxisYData>()

                //--- Максимальный размер массива = кол-во точек по горизонтали = 3840 (максимальная ширина 4K-экрана), окгруляем до 4000
                val aMinLimit = if (agh.isStaticMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1, isReversedY)
                } else if (agh.isDynamicMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1, isReversedY)
                } else {
                    null
                }

                val aMaxLimit = if (agh.isStaticMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1, isReversedY)
                } else if (agh.isDynamicMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1, isReversedY)
                } else {
                    null
                }

                val aBack = if (isShowBack) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.BACK, 0, 0, isReversedY)
                } else {
                    null
                }
                //--- Если включён показ линий и выключено сглаживание, то точки можно не показывать, их всё равно не будет видно за покрывающей их линией
                val aPoint = if (isShowPoint && (!isShowLine || sca.smoothTime > 0)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.POINT, 0, 0, isReversedY)
                } else {
                    null
                }
                val aLine = if (isShowLine) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 3, isReversedY)
                } else {
                    null
                }
                val aText = if (isShowText) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.TEXT, 0, 0, isReversedY)
                } else {
                    null
                }

                calcGraphic(
                    graphicHandler = agh,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    oc = oc,
                    sca = sca,
                    begTime = x1,
                    endTime = x2,
                    xScale = if (viewWidth == 0) {
                        0
                    } else {
                        (x2 - x1) / (viewWidth / DOT_PER_MM)
                    },
                    yScale = if (viewHeight == 0) {
                        0.0
                    } else {
                        (sca.maxView - sca.minView) / (viewHeight / DOT_PER_MM)
                    },
                    alAxisYData = alAxisYData,
                    aMinLimit = aMinLimit,
                    aMaxLimit = aMaxLimit,
                    aBack = aBack,
                    aPoint = aPoint,
                    aLine = aLine,
                    aText = aText
                )

                //--- общие нештатные ситуации показываем после работы оборудования,
                //--- отображаемого в виде сплошной полосы различного цвета и
                //--- после специфических ( как правило - более критических ) ошибок конкретных датчиков
                if (!isCommonTroubleShowed && aText != null) {
                    checkCommonTrouble(alRawTime, alRawData, oc, x1, x2, aText)
                    isCommonTroubleShowed = true
                }

                val alGDC = mutableListOf(aBack, aText, aMinLimit, aMaxLimit, aPoint, aLine)

                outGraphicData(alGDC)

                val ge = GraphicElement(
                    graphicTitle = sca.descr,
                    alIndexColor = hmIndexColor.toList().toTypedArray(),
                    graphicHeight = -1.0,
                    alAxisYData = alAxisYData.toTypedArray(),
                    alGDC = alGDC.filterNotNull().filter { /*it != null &&*/ it.itNotEmpty() }.toTypedArray()
                )

                tmElement[ge.graphicTitle] = ge

                //--- вывод дополнительных графиков, напрямую связанных со стандартно выводимыми
                //outAddGraphic( sObjectInfo, tmElement)
            }
        }
        //--- вывод дополнительных графиков, не связанных напрямую со стандартно выводимыми
        //outOtherGraphic(sObjectInfo, tmElement)

        //AdvancedLogger.debug(  "Graphic time [ ms ] = " + (  System.currentTimeMillis() - begTime  )  );
        //AdvancedLogger.debug(  "------------------------------------------------------------"  );

        return GraphicActionResponse(
            alElement = tmElement.toList().toTypedArray(),
            alVisibleElement = tmElementVisibleConfig.toList().toTypedArray()
        )
    }

    protected open fun calcGraphic(
        graphicHandler: iGraphicHandler,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        oc: ObjectConfig,
        sca: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        alAxisYData: MutableList<AxisYData>,
        aMinLimit: GraphicDataContainer?,
        aMaxLimit: GraphicDataContainer?,
        aBack: GraphicDataContainer?,
        aPoint: GraphicDataContainer?,
        aLine: GraphicDataContainer?,
        aText: GraphicDataContainer?
    ) {
        val isReversedY = sca.sensorType == SensorConfig.SENSOR_DEPTH

        alAxisYData.add(AxisYData(sca.descr, sca.minView, sca.maxView, GraphicColorIndex.AXIS_0, isReversedY))

        aBack?.let {
            val hmSC = oc.hmSensorConfig[SensorConfig.SENSOR_STATE]
            if (!hmSC.isNullOrEmpty()) {
                val alGBD = aBack.alGBD.toMutableList()
                hmSC.values.forEach { sc ->
                    val scs = sc as SensorConfigState
                    //--- пропускаем датчики состояния не из своей группы
                    if (scs.group == sca.group) {
                        val alStatePeriods = ObjectCalc.calcStateSensor(alRawTime, alRawData, scs, begTime, endTime)
                        for (asd in alStatePeriods) {
                            alGBD += GraphicBackData(
                                x1 = asd.begTime,
                                x2 = asd.endTime,
                                color = SensorConfigState.hmStateInfo[asd.getState()]?.backColor ?: SensorConfigState.COLOR_UNKNOWN_BACK
                            )
                        }
                    }
                }
                aBack.alGBD = alGBD.toTypedArray()
            }
        }

        ObjectCalc.getSmoothAnalogGraphicData(alRawTime, alRawData, sca, begTime, endTime, xScale, yScale, aMinLimit, aMaxLimit, aPoint, aLine, graphicHandler)

//        if (aText != null) {
//            val hmSC = oc.hmSensorConfig[SensorConfig.SENSOR_WORK]
//            if (!hmSC.isNullOrEmpty()) {
//                val alGTD = aText.alGTD.toMutableList()
//                hmSC.values.forEach { sc ->
//                    val scw = sc as SensorConfigWork
//                    //--- пропускаем датчики работы оборудования не из своей группы
//                    if (sca.group == scw.group) {
//                        val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime).alWorkOnOff
//                        for (apd in alWork) {
//                            val workDescr = StringBuilder(scw.descr).append(" : ").append(if (apd.getState() != 0) "ВКЛ" else "выкл").toString()
//                            alGTD += GraphicTextData(
//                                textX1 = apd.begTime,
//                                textX2 = apd.endTime,
//                                fillColorIndex = if (apd.getState() != 0) GraphicColorIndex.FILL_NORMAL else GraphicColorIndex.FILL_WARNING,
//                                borderColorIndex = if (apd.getState() != 0) GraphicColorIndex.BORDER_NORMAL else GraphicColorIndex.BORDER_WARNING,
//                                textColorIndex = if (apd.getState() != 0) GraphicColorIndex.TEXT_NORMAL else GraphicColorIndex.TEXT_WARNING,
//                                text = workDescr,
//                                toolTip = workDescr
//                            )
//                        }
//                    }
//                }
//                aText.alGTD = alGTD.toTypedArray()
//            }
//        }
    }

    //--- собственно вывод данных для возможности перекрытия наследниками
    protected open fun outGraphicData(alGDC: MutableList<GraphicDataContainer?>) {}

//    //--- вывод дополнительных графиков, напрямую связанных со стандартно выводимыми
//    protected fun outAddGraphic( sbObjectInfo: StringBuilder, x1: Long, bbOut: AdvancedByteBuffer ) {}
//
//    //--- вывод дополнительных графиков, не связанных напрямую со стандартно выводимыми
//    protected fun outOtherGraphic( sbObjectInfo: StringBuilder, x1: Long, bbOut: AdvancedByteBuffer ) {}

}

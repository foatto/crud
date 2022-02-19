package foatto.ts.core_ts.graphic.server.document

import foatto.core.app.graphic.*
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.ts.core_ts.ObjectConfig
import foatto.ts.core_ts.calc.ObjectCalc
import foatto.ts.core_ts.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigAnalogue
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.ts.iTSApplication
import java.util.*

abstract class sdcAbstractAnalog : sdcAbstractGraphic() {

    override fun doGetElements(graphicActionRequest: GraphicActionRequest): GraphicActionResponse {
        val graphicStartDataID = graphicActionRequest.startParamId
        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + graphicStartDataID] as GraphicStartData

        val (begTime, endTime) = graphicActionRequest.graphicCoords!!
        val (viewWidth, viewHeight) = graphicActionRequest.viewSize!!

        val objectConfig = (application as iTSApplication).getObjectConfig(userConfig, sd.objectId)

        //--- единоразово загрузим данные по объекту
        val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, objectConfig, begTime, endTime)

        val tmElement = sortedMapOf<String, GraphicElement>()
        val tmElementVisibleConfig = sortedMapOf<String, Triple<String, String, Boolean>>()

//        //--- общие нештатные ситуации показываем только на первом/верхнем графике
//        var isCommonTroubleShowed = false

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
            arrLegend = SensorConfigState.alStateLegend.map { (color, descr) ->
                Triple(color, true, descr)
            }.toTypedArray(),
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
        graphicHandler: AnalogGraphicHandler,
        alSca: List<SensorConfigAnalogue>,
        tmElement: SortedMap<String, GraphicElement>,
        tmElementVisibleConfig: SortedMap<String, Triple<String, String, Boolean>>,
    ) {
        val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${objectConfig.objectId}_${alSca.first().portNum}"
        val isGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)?.toBooleanStrictOrNull() ?: true
        tmElementVisibleConfig[graphicTitle] = Triple(graphicTitle, graphicVisibilityKey, isGraphicVisible)

        if (isGraphicVisible) {
            val alGDC = mutableListOf<GraphicDataContainer>()

            val aBack = GraphicDataContainer(GraphicDataContainer.ElementType.BACK, 0, 0, false)
            objectConfig.hmSensorConfig[SensorConfig.SENSOR_STATE]?.values?.firstOrNull()?.let { sc ->
                val scs = sc as SensorConfigState

                val alGBD = aBack.alGBD.toMutableList()
                val alStatePeriods = ObjectCalc.calcStateSensor(alRawTime, alRawData, scs, begTime, endTime)
                for (asd in alStatePeriods) {
                    alGBD += GraphicBackData(
                        x1 = asd.begTime,
                        x2 = asd.endTime,
                        color = SensorConfigState.hmStateInfo[asd.getState()]?.darkColor ?: SensorConfigState.COLOR_UNKNOWN_DARK
                    )
                }
                aBack.alGBD = alGBD.toTypedArray()

                alGDC.add(aBack)
            }

            val alAxisYData = mutableListOf<AxisYData>()

            var axisIndex = 0
            alSca.forEach { sca ->
                val isReversedY = sca.sensorType == SensorConfig.SENSOR_DEPTH

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

//                //--- общие нештатные ситуации показываем после работы оборудования,
//                //--- отображаемого в виде сплошной полосы различного цвета и
//                //--- после специфических ( как правило - более критических ) ошибок конкретных датчиков
//                if (!isCommonTroubleShowed && aText != null) {
//                    checkCommonTrouble(alRawTime, alRawData, oc, x1, x2, aText)
//                    isCommonTroubleShowed = true
//                }

                axisIndex++
                alGDC.addAll(listOfNotNull(aText, aMinLimit, aMaxLimit, aLine).filter { it.itNotEmpty() })
            }

            tmElement[graphicTitle] = GraphicElement(
                graphicTitle = graphicTitle,
                alLegend = emptyArray(),
                graphicHeight = -1.0,
                alAxisYData = alAxisYData.toTypedArray(),
                alGDC = alGDC.toTypedArray()
            )
        }
    }

}
package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.UP_GRAPHIC_SHOW_BACK
import foatto.core.app.UP_GRAPHIC_SHOW_LINE
import foatto.core.app.UP_GRAPHIC_SHOW_POINT
import foatto.core.app.UP_GRAPHIC_SHOW_TEXT
import foatto.core.app.graphic.AxisYData
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicElement
import foatto.core.app.graphic.GraphicTextData
import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.graphic.server.MMSGraphicDocumentConfig
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import foatto.mms.iMMSApplication
import java.util.*
import kotlin.math.min

open class sdcAnalog : sdcAbstractGraphic() {

    companion object {
        //--- в худшем случае у нас как минимум 4 точки на мм ( 100 dpi ),
        //--- нет смысла выводить данные в каждый пиксель, достаточно в каждый мм
        private const val DOT_PER_MM = 4

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

            //--- поиск критических режимов работы счётчика топлива EuroSens Delta
            oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING_COUNTER_STATE]?.values?.forEach { sc ->
                listOf(
                    SensorConfigCounter.STATUS_OVERLOAD,
                    SensorConfigCounter.STATUS_CHEAT,
                    SensorConfigCounter.STATUS_REVERSE,
                    SensorConfigCounter.STATUS_INTERVENTION,
                ).forEach { stateCode ->
                    checkSensorError(
                        alRawTime = alRawTime,
                        alRawData = alRawData,
                        portNum = sc.portNum,
                        sensorDescr = sc.descr,
                        begTime = begTime,
                        endTime = endTime,
                        aFillColorIndex = GraphicColorIndex.FILL_CRITICAL,
                        aBorderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
                        aTextColorIndex = GraphicColorIndex.TEXT_CRITICAL,
                        troubleCode = stateCode,
                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
                        alGTD = alGTD
                    )
                }
                listOf(
                    SensorConfigCounter.STATUS_UNKNOWN,
                    SensorConfigCounter.STATUS_IDLE,
                    //SensorConfigCounter.STATUS_NORMAL,
                ).forEach { stateCode ->
                    checkSensorError(
                        alRawTime = alRawTime,
                        alRawData = alRawData,
                        portNum = sc.portNum,
                        sensorDescr = sc.descr,
                        begTime = begTime,
                        endTime = endTime,
                        aFillColorIndex = GraphicColorIndex.FILL_WARNING,
                        aBorderColorIndex = GraphicColorIndex.BORDER_WARNING,
                        aTextColorIndex = GraphicColorIndex.TEXT_WARNING,
                        troubleCode = stateCode,
                        troubleDescr = SensorConfigCounter.hmStatusDescr[stateCode] ?: "(неизвестный код состояния)",
                        minTime = MIN_LIQUID_COUNTER_STATE_TIME,
                        alGTD = alGTD
                    )
                }
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

        val mmsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as MMSGraphicDocumentConfig
        val sensorType = mmsgdc.sensorType
        val agh = mmsgdc.graphicHandler as AnalogGraphicHandler

        val graphicStartDataID = graphicActionRequest.startParamID
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

        val oc = (application as iMMSApplication).getObjectConfig(userConfig, sd.objectID)
        //--- загрузка заголовочной информации по объекту
        var sObjectInfo = oc.name

        if (oc.model.isNotEmpty()) sObjectInfo += ", " + oc.model
        if (oc.groupName.isNotEmpty()) sObjectInfo += ", " + oc.groupName
        if (oc.departmentName.isNotEmpty()) sObjectInfo += ", " + oc.departmentName

        val hmSensorConfig = oc.hmSensorConfig[sensorType]
        val tmElement = TreeMap<String, GraphicElement>()
        val tmElementVisibleConfig = TreeMap<String, String>()
        if (hmSensorConfig != null) {
            //--- единоразово загрузим данные по объекту
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, x1, x2)
            //--- данные по гео-датчику ( движение/стоянка/ошибка ) показываем только на первом/верхнем графике
            var isGeoSensorShowed = false
            //--- общие нештатные ситуации показываем только на первом/верхнем графике
            var isCommonTroubleShowed = false

            for (portNum in hmSensorConfig.keys) {
                val sca = hmSensorConfig[portNum] as SensorConfigAnalogue

                //--- заранее заполняем список опеределений видимости графиков
                val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${sd.objectID}_${sca.portNum}"
                tmElementVisibleConfig[sca.descr] = graphicVisibilityKey

                //--- а сейчас уже можно и нужно проверять на видимость графика
                val strGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)
                val isGraphicVisible = strGraphicVisible?.toBoolean() ?: true

                if (!isGraphicVisible) {
                    continue
                }

                val alAxisYData = mutableListOf<AxisYData>()

                //--- Максимальный размер массива = кол-во точек по горизонтали = 3840 ( максимальная ширина 4K-экрана ), окгруляем до 4000
                val aMinLimit = if (agh.isStaticMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                }
                else if (agh.isDynamicMinLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                } else {
                    null
                }

                val aMaxLimit = if (agh.isStaticMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                }
                else if (agh.isDynamicMaxLimit(sca)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                } else {
                    null
                }

                val aBack = if (isShowBack) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.BACK, 0)
                } else {
                    null
                }
                //--- Если включён показ линий и выключено сглаживание, то точки можно не показывать, их всё равно не будет видно за покрывающей их линией
                val aPoint = if (isShowPoint && (!isShowLine || sca.smoothTime > 0)) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.POINT, 0)
                } else {
                    null
                }
                val aLine = if (isShowLine) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 3)
                } else {
                    null
                }
                val aText = if (isShowText) {
                    GraphicDataContainer(GraphicDataContainer.ElementType.TEXT, 0)
                } else {
                    null
                }

                //--- данные по гео-датчику показываем до работы оборудования
                if (!isGeoSensorShowed && aText != null && oc.scg != null && oc.scg!!.isUseSpeed) {
                    calcGeoSensor(alRawTime, alRawData, oc.scg!!, x1, x2, aText)
                    isGeoSensorShowed = true
                }

                calcGraphic(
                    graphicHandler = agh,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    oc = oc,
                    sca = sca,
                    begTime = x1,
                    endTime = x2,
                    xScale = if (viewWidth == 0) 0 else (x2 - x1) / (viewWidth / DOT_PER_MM),
                    yScale = if (viewHeight == 0) 0.0 else (sca.maxView - sca.minView) / (viewHeight / DOT_PER_MM),
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

                val alGDC = mutableListOf(aText, aMinLimit, aMaxLimit, aPoint, aLine)

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
                //outAddGraphic( sObjectInfo, tmElement )
            }
        }
        //--- вывод дополнительных графиков, не связанных напрямую со стандартно выводимыми
        //outOtherGraphic( sObjectInfo, tmElement )

        //AdvancedLogger.debug(  "Graphic time [ ms ] = " + (  System.currentTimeMillis() - begTime  )  );
        //AdvancedLogger.debug(  "------------------------------------------------------------"  );

        return GraphicActionResponse(
            alElement = tmElement.toList().toTypedArray(),
            alVisibleElement = tmElementVisibleConfig.toList().toTypedArray()
        )
    }

    //--- упрощённый вывод данных по гео-датчику ( движение/стоянка/нет данных АКА ошибка )
    //--- ( без учёта минимального времени стоянки )
    private fun calcGeoSensor(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, scg: SensorConfigGeo, begTime: Int, endTime: Int, aText: GraphicDataContainer) {
        var lastStatus = -2 // -1 = нет гео-данных, 0 = стоянка, 1 - движение
        var lastTime = 0
        val alGTD = aText.alGTD.toMutableList()
        for (pos in alRawTime.indices) {
            val rawTime = alRawTime[pos]
            //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и их можно пропустить
            if (rawTime < begTime) continue
            //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и можно прекращать обработку
            if (rawTime > endTime) break

            val gd = AbstractObjectStateCalc.getGeoData(scg, alRawData[pos])
            //--- самих геоданных может и не оказаться ( нет датчика или нет или ошибка GPS-данных )
            if (gd == null) {
                //--- если до этого было другое состояние ( движение или стоянка )
                if (lastStatus != -1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGTD += GraphicTextData(
                            textX1 = lastTime,
                            textX2 = rawTime,
                            fillColorIndex = if (lastStatus == 0) GraphicColorIndex.FILL_WARNING else GraphicColorIndex.FILL_NORMAL,
                            borderColorIndex = if (lastStatus == 0) GraphicColorIndex.BORDER_WARNING else GraphicColorIndex.BORDER_NORMAL,
                            textColorIndex = if (lastStatus == 0) GraphicColorIndex.TEXT_WARNING else GraphicColorIndex.TEXT_NORMAL,
                            text = if (lastStatus == 0) "Стоянка" else "Движение",
                            toolTip = if (lastStatus == 0) "Стоянка" else "Движение"
                        )
                    }
                    lastStatus = -1
                    lastTime = rawTime
                }
            } else if (gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
                //--- если до этого было другое состояние ( движение или ошибка )
                if (lastStatus != 0) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGTD += GraphicTextData(
                            textX1 = lastTime,
                            textX2 = rawTime,
                            fillColorIndex = if (lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else GraphicColorIndex.FILL_NORMAL,
                            borderColorIndex = if (lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else GraphicColorIndex.BORDER_NORMAL,
                            textColorIndex = if (lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else GraphicColorIndex.TEXT_NORMAL,
                            text = if (lastStatus == -1) "Нет данных от гео-датчика" else "Движение",
                            toolTip = if (lastStatus == -1) "Нет данных от гео-датчика" else "Движение"
                        )
                    }
                    lastStatus = 0
                    lastTime = rawTime
                }
            } else {
                //--- если до этого было другое состояние ( стоянка или ошибка )
                if (lastStatus != 1) {
                    //--- если это не первое состояние
                    if (lastTime != 0) {
                        alGTD += GraphicTextData(
                            textX1 = lastTime,
                            textX2 = rawTime,
                            fillColorIndex = if (lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else GraphicColorIndex.FILL_WARNING,
                            borderColorIndex = if (lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else GraphicColorIndex.BORDER_WARNING,
                            textColorIndex = if (lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else GraphicColorIndex.TEXT_WARNING,
                            text = if (lastStatus == -1) "Нет данных от гео-датчика" else "Стоянка",
                            toolTip = if (lastStatus == -1) "Нет данных от гео-датчика" else "Стоянка"
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
            alGTD += GraphicTextData(
                textX1 = lastTime,
                textX2 = min(getCurrentTimeInt(), endTime),
                fillColorIndex = if (lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else if (lastStatus == 0) GraphicColorIndex.FILL_WARNING else GraphicColorIndex.FILL_NORMAL,
                borderColorIndex = if (lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else if (lastStatus == 0) GraphicColorIndex.BORDER_WARNING else GraphicColorIndex.BORDER_NORMAL,
                textColorIndex = if (lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else if (lastStatus == 0) GraphicColorIndex.TEXT_WARNING else GraphicColorIndex.TEXT_NORMAL,
                text = if (lastStatus == -1) "Нет данных от гео-датчика" else if (lastStatus == 0) "Стоянка" else "Движение",
                toolTip = if (lastStatus == -1) "Нет данных от гео-датчика" else if (lastStatus == 0) "Стоянка" else "Движение"
            )
        }
        aText.alGTD = alGTD.toTypedArray()
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

        alAxisYData.add(AxisYData(sca.descr, sca.minView, sca.maxView, GraphicColorIndex.AXIS_0))

        ObjectCalc.getSmoothAnalogGraphicData(alRawTime, alRawData, oc.scg, sca, begTime, endTime, xScale, yScale, aMinLimit, aMaxLimit, aPoint, aLine, graphicHandler)

        //--- если вывод текстов задан, сделаем вывод режимов работы оборудования
        aText?.let {
            val hmSC = oc.hmSensorConfig[SensorConfig.SENSOR_WORK]
            if (!hmSC.isNullOrEmpty()) {
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
                                fillColorIndex = if (apd.getState() != 0) GraphicColorIndex.FILL_NORMAL else GraphicColorIndex.FILL_WARNING,
                                borderColorIndex = if (apd.getState() != 0) GraphicColorIndex.BORDER_NORMAL else GraphicColorIndex.BORDER_WARNING,
                                textColorIndex = if (apd.getState() != 0) GraphicColorIndex.TEXT_NORMAL else GraphicColorIndex.TEXT_WARNING,
                                text = workDescr,
                                toolTip = workDescr
                            )
                        }
                    }
                }
                aText.alGTD = alGTD.toTypedArray()
            }
        }
    }

    //--- собственно вывод данных для возможности перекрытия наследниками
    protected open fun outGraphicData(alGDC: MutableList<GraphicDataContainer?>) {}

//    //--- вывод дополнительных графиков, напрямую связанных со стандартно выводимыми
//    protected fun outAddGraphic( sbObjectInfo: StringBuilder, x1: Long, bbOut: AdvancedByteBuffer ) {}
//
//    //--- вывод дополнительных графиков, не связанных напрямую со стандартно выводимыми
//    protected fun outOtherGraphic( sbObjectInfo: StringBuilder, x1: Long, bbOut: AdvancedByteBuffer ) {}

}

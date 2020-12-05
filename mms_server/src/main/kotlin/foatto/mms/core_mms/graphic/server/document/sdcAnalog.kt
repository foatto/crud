package foatto.mms.core_mms.graphic.server.document

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
import foatto.mms.core_mms.sensor.config.SensorConfigWork
import java.util.*
import kotlin.math.min

open class sdcAnalog : sdcAbstractGraphic() {

    companion object {
        //--- в худшем случае у нас как минимум 4 точки на мм ( 100 dpi ),
        //--- нет смысла выводить данные в каждый пиксель, достаточно в каждый мм
        private val DOT_PER_MM = 4

        val MIN_CONNECT_OFF_TIME = 15 * 60
        val MIN_NO_DATA_TIME = 5 * 60
        private val MIN_POWER_OFF_TIME = 5 * 60
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

        val smPoint = userConfig.getUserProperty(UP_GRAPHIC_SHOW_POINT)
        val smLine = userConfig.getUserProperty(UP_GRAPHIC_SHOW_LINE)
        val smText = userConfig.getUserProperty(UP_GRAPHIC_SHOW_TEXT)

        //--- показ точек по умолчанию выключен, если не указано явно иное
        val isShowPoint = smPoint?.toBoolean() ?: false
        //--- показ линий по умолчанию включен, если не указано явно иное
        val isShowLine = smLine?.toBoolean() ?: true
        //--- показ текстов по умолчанию включен, если не указано явно иное
        val isShowText = smText?.toBoolean() ?: true

        val oc = ObjectConfig.getObjectConfig(stm, userConfig, sd.objectID)
        //--- загрузка заголовочной информации по объекту
        var sObjectInfo = oc.name

        if(oc.model.isNotEmpty()) sObjectInfo += ", " + oc.model
        if(oc.groupName.isNotEmpty()) sObjectInfo += ", " + oc.groupName
        if(oc.departmentName.isNotEmpty()) sObjectInfo += ", " + oc.departmentName

        val hmSensorConfig = oc.hmSensorConfig[sensorType]
        val tmElement = TreeMap<String, GraphicElement>()
        val tmElementVisibleConfig = TreeMap<String, String>()
        if(hmSensorConfig != null) {
            //--- единоразово загрузим данные по объекту
            val (alRawTime, alRawData) = ObjectCalc.loadAllSensorData(stm, oc, x1, x2)
            //--- данные по гео-датчику ( движение/стоянка/ошибка ) показываем только на первом/верхнем графике
            var isGeoSensorShowed = false
            //--- общие нештатные ситуации показываем только на первом/верхнем графике
            var isCommonTroubleShowed = false

            for(portNum in hmSensorConfig.keys) {
                val sca = hmSensorConfig[portNum] as SensorConfigAnalogue

                //--- заранее заполняем список опеределений видимости графиков
                val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${sd.objectID}_${sca.portNum}"
                tmElementVisibleConfig[sca.descr] = graphicVisibilityKey

                //--- а сейчас уже можно и нужно проверять на видимость графика
                val strGraphicVisible = userConfig.getUserProperty(graphicVisibilityKey)
                val isGraphicVisible = strGraphicVisible?.toBoolean() ?: true

                if(!isGraphicVisible) continue

                val alAxisYData = mutableListOf<AxisYData>()

                //--- Максимальный размер массива = кол-во точек по горизонтали = 3840 ( максимальная ширина 4K-экрана ), окгруляем до 4000
                val aMinLimit = if(agh.isStaticMinLimit(sca)) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                else if(agh.isDynamicMinLimit(sca)) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1) else null

                val aMaxLimit = if(agh.isStaticMaxLimit(sca)) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1)
                else if(agh.isDynamicMaxLimit(sca)) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 1) else null

                //--- Если включён показ линий и выключено сглаживание, то точки можно не показывать,
                //--- их всё равно не будет видно за покрывающей их линией
                val aPoint = if(isShowPoint && (!isShowLine || sca.smoothTime > 0)) GraphicDataContainer(GraphicDataContainer.ElementType.POINT, 0) else null
                val aLine = if(isShowLine) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 0, 3) else null
                val aText = if(isShowText) GraphicDataContainer(GraphicDataContainer.ElementType.TEXT, 0) else null

                //--- данные по гео-датчику показываем до работы оборудования
                if(!isGeoSensorShowed) {
                    calcGeoSensor(alRawTime, alRawData, oc, x1, x2, aText)
                    isGeoSensorShowed = true
                }

                calcGraphic(
                    agh, alRawTime, alRawData, oc, sca, x1, x2,
                    if(viewWidth == 0) 0 else (x2 - x1) / (viewWidth / DOT_PER_MM),
                    if(viewHeight == 0) 0.0 else (sca.maxView - sca.minView) / (viewHeight / DOT_PER_MM),
                    isShowPoint, isShowLine, isShowText, alAxisYData, aMinLimit, aMaxLimit, aPoint, aLine, aText
                )

                //--- общие нештатные ситуации показываем после работы оборудования,
                //--- отображаемого в виде сплошной полосы различного цвета и
                //--- после специфических ( как правило - более критических ) ошибок конкретных датчиков
                if(!isCommonTroubleShowed) {
                    checkCommonTrouble(alRawTime, alRawData, oc, x1, x2, aText)
                    isCommonTroubleShowed = true
                }

                val alGDC = mutableListOf(aText, aMinLimit, aMaxLimit, aPoint, aLine)

                outGraphicData(alGDC)

                val ge = GraphicElement(
                    graphicTitle = sca.descr,
                    alIndexColor = hmIndexColor.toList(),
                    graphicHeight = -1.0,
                    alAxisYData = alAxisYData,
                    alGDC = alGDC.filterNotNull().filter { /*it != null &&*/ it.itNotEmpty() }
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
            alElement = tmElement.toList(),
            alVisibleElement = tmElementVisibleConfig.toList()
        )
    }

    //--- упрощённый вывод данных по гео-датчику ( движение/стоянка/нет данных АКА ошибка )
    //--- ( без учёта минимального времени стоянки )
    protected fun calcGeoSensor(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, begTime: Int, endTime: Int, aText: GraphicDataContainer?) {
        if(aText == null) return

        //--- если гео-датчика нет или в нём отключено использование понятия "скорости"
        if(oc.scg == null || !oc.scg!!.isUseSpeed) return

        var lastStatus = -2 // -1 = нет гео-данных, 0 = стоянка, 1 - движение
        var lastTime = 0
        for(pos in alRawTime.indices) {
            val rawTime = alRawTime[pos]
            //--- данные до запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и их можно пропустить
            if(rawTime < begTime) continue
            //--- данные после запрашиваемого диапазона ( расширенные для сглаживания )
            //--- в данном случае не интересны и можно прекращать обработку
            if(rawTime > endTime) break

            val gd = AbstractObjectStateCalc.getGeoData(oc, alRawData[pos])
            //--- самих геоданных может и не оказаться ( нет датчика или нет или ошибка GPS-данных )
            if(gd == null) {
                //--- если до этого было другое состояние ( движение или стоянка )
                if(lastStatus != -1) {
                    //--- если это не первое состояние
                    if(lastTime != 0) {
                        aText.alGTD.add(
                            GraphicTextData(
                                lastTime, rawTime,
                                if(lastStatus == 0) GraphicColorIndex.FILL_WARNING else GraphicColorIndex.FILL_NORMAL,
                                if(lastStatus == 0) GraphicColorIndex.BORDER_WARNING else GraphicColorIndex.BORDER_NORMAL,
                                if(lastStatus == 0) GraphicColorIndex.TEXT_WARNING else GraphicColorIndex.TEXT_NORMAL,
                                if(lastStatus == 0) "Стоянка" else "Движение",
                                if(lastStatus == 0) "Стоянка" else "Движение"
                            )
                        )
                    }
                    lastStatus = -1
                    lastTime = rawTime
                }
            } else if(gd.speed <= AbstractObjectStateCalc.MAX_SPEED_AS_PARKING) {
                //--- если до этого было другое состояние ( движение или ошибка )
                if(lastStatus != 0) {
                    //--- если это не первое состояние
                    if(lastTime != 0) {
                        aText.alGTD.add(
                            GraphicTextData(
                                lastTime, rawTime,
                                if(lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else GraphicColorIndex.FILL_NORMAL,
                                if(lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else GraphicColorIndex.BORDER_NORMAL,
                                if(lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else GraphicColorIndex.TEXT_NORMAL,
                                if(lastStatus == -1) "Нет данных от гео-датчика" else "Движение",
                                if(lastStatus == -1) "Нет данных от гео-датчика" else "Движение"
                            )
                        )
                    }
                    lastStatus = 0
                    lastTime = rawTime
                }
            } else {
                //--- если до этого было другое состояние ( стоянка или ошибка )
                if(lastStatus != 1) {
                    //--- если это не первое состояние
                    if(lastTime != 0) {
                        aText.alGTD.add(
                            GraphicTextData(
                                lastTime, rawTime,
                                if(lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else GraphicColorIndex.FILL_WARNING,
                                if(lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else GraphicColorIndex.BORDER_WARNING,
                                if(lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else GraphicColorIndex.TEXT_WARNING,
                                if(lastStatus == -1) "Нет данных от гео-датчика" else "Стоянка",
                                if(lastStatus == -1) "Нет данных от гео-датчика" else "Стоянка"
                            )
                        )
                    }
                    lastStatus = 1
                    lastTime = rawTime
                }
            }//--- движение
            //--- стоянка
        }
        //--- если это не первое состояние
        if(lastTime != 0) {
            aText.alGTD.add(
                GraphicTextData(
                    lastTime, min(getCurrentTimeInt(), endTime),
                    if(lastStatus == -1) GraphicColorIndex.FILL_CRITICAL else if(lastStatus == 0) GraphicColorIndex.FILL_WARNING else GraphicColorIndex.FILL_NORMAL,
                    if(lastStatus == -1) GraphicColorIndex.BORDER_CRITICAL else if(lastStatus == 0) GraphicColorIndex.BORDER_WARNING else GraphicColorIndex.BORDER_NORMAL,
                    if(lastStatus == -1) GraphicColorIndex.TEXT_CRITICAL else if(lastStatus == 0) GraphicColorIndex.TEXT_WARNING else GraphicColorIndex.TEXT_NORMAL,
                    if(lastStatus == -1) "Нет данных от гео-датчика" else if(lastStatus == 0) "Стоянка" else "Движение",
                    if(lastStatus == -1) "Нет данных от гео-датчика" else if(lastStatus == 0) "Стоянка" else "Движение"
                )
            )
        }
    }

    protected open fun calcGraphic(
        graphicHandler: iGraphicHandler, alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, sca: SensorConfigAnalogue,
        begTime: Int, endTime: Int, xScale: Int, yScale: Double, isShowPoint: Boolean, isShowLine: Boolean, isShowText: Boolean,
        alAxisYData: MutableList<AxisYData>, aMinLimit: GraphicDataContainer?, aMaxLimit: GraphicDataContainer?,
        aPoint: GraphicDataContainer?, aLine: GraphicDataContainer?, aText: GraphicDataContainer?
    ) {

        alAxisYData.add(AxisYData(sca.name, sca.minView, sca.maxView, GraphicColorIndex.AXIS_0))

        ObjectCalc.getSmoothAnalogGraphicData(alRawTime, alRawData, oc, sca, begTime, endTime, xScale, yScale, aMinLimit, aMaxLimit, aPoint, aLine, graphicHandler)

        //--- если вывод текстов задан, сделаем вывод режимов работы оборудования
        if(aText != null) {
            val hmSC = oc.hmSensorConfig[SensorConfig.SENSOR_WORK]
            if(hmSC != null && hmSC.isNotEmpty()) {
                for(portNum in hmSC.keys) {
                    val scw = hmSC[portNum] as SensorConfigWork
                    //--- пропускаем датчики работы оборудования не из своей группы
                    if(sca.group != scw.group) continue
                    val alWork = ObjectCalc.calcWorkSensor(alRawTime, alRawData, scw, begTime, endTime).alWorkOnOff
                    if(alWork != null) {
                        for(apd in alWork) {
                            val workDescr = StringBuilder(scw.descr).append(" : ").append(if(apd.getState() != 0) "ВКЛ" else "выкл").toString()
                            aText.alGTD.add(
                                GraphicTextData(
                                    apd.begTime, apd.endTime,
                                    if(apd.getState() != 0) GraphicColorIndex.FILL_NORMAL else GraphicColorIndex.FILL_WARNING,
                                    if(apd.getState() != 0) GraphicColorIndex.BORDER_NORMAL else GraphicColorIndex.BORDER_WARNING,
                                    if(apd.getState() != 0) GraphicColorIndex.TEXT_NORMAL else GraphicColorIndex.TEXT_WARNING, workDescr, workDescr
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    //--- ловля основных/системных нештатных ситуаций, показываемых только на первом/верхнем графике:
    //--- нет связи, нет данных и резервное питание
    protected fun checkCommonTrouble(alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, oc: ObjectConfig, begTime: Int, endTime: Int, aText: GraphicDataContainer?) {
        if(aText == null) return

        //--- дабы не загромождать код
        val curTime = getCurrentTimeInt()

        //--- поиск значительных промежутков отсутствия данных ---

        var lastDataTime = begTime
        for(rawTime in alRawTime) {
            //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
            if(rawTime < begTime) continue
            if(rawTime > endTime) break

            if(rawTime - lastDataTime > MIN_NO_DATA_TIME)
                aText.alGTD.add(
                    GraphicTextData(
                        lastDataTime, rawTime, GraphicColorIndex.FILL_CRITICAL, GraphicColorIndex.BORDER_CRITICAL, GraphicColorIndex.TEXT_CRITICAL,
                        "Нет данных от контроллера", "Нет данных от контроллера"
                    )
                )
            lastDataTime = rawTime
        }
        if(min(curTime, endTime) - lastDataTime > MIN_NO_DATA_TIME)
            aText.alGTD.add(
                GraphicTextData(
                    lastDataTime, min(curTime, endTime), GraphicColorIndex.FILL_CRITICAL, GraphicColorIndex.BORDER_CRITICAL, GraphicColorIndex.TEXT_CRITICAL,
                    "Нет данных от контроллера", "Нет данных от контроллера"
                )
            )

        //--- поиск значительных промежутков отсутствия основного питания ( перехода на резервное питание )
        val hmSCV = oc.hmSensorConfig[SensorConfig.SENSOR_VOLTAGE]
        if(hmSCV != null) {
            for(portNum in hmSCV.keys) {
                val sca = hmSCV[portNum] as SensorConfigAnalogue
                //--- чтобы не смешивались разные ошибки по одному датчику и одинаковые ошибки по разным датчикам,
                //--- добавляем в описание ошибки не только само описание ошибки, но и описание датчика
                checkSensorError(
                    alRawTime, alRawData, portNum, sca.descr, begTime, endTime, GraphicColorIndex.FILL_WARNING, GraphicColorIndex.BORDER_WARNING,
                    GraphicColorIndex.TEXT_WARNING, 0, "Нет питания", MIN_POWER_OFF_TIME, aText
                )
            }
        }
    }

    protected fun checkSensorError(
        alRawTime: List<Int>, alRawData: List<AdvancedByteBuffer>, portNum: Int, sensorDescr: String,
        begTime: Int, endTime: Int, aFillColorIndex: GraphicColorIndex, aBorderColorIndex: GraphicColorIndex, aTextColorIndex: GraphicColorIndex,
        troubleCode: Int, troubleDescr: String, minTime: Int, aText: GraphicDataContainer
    ) {

        //--- в основном тексте пишем только текст ошибки, а в tooltips'e напишем вместе с описанием датчика
        val fullTroubleDescr = StringBuilder(sensorDescr).append(": ").append(troubleDescr).toString()
        var troubleBegTime = 0
        var sensorData: Int

        for(pos in alRawTime.indices) {
            val rawTime = alRawTime[pos]
            //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
            if(rawTime < begTime) continue
            if(rawTime > endTime) break

            sensorData = AbstractObjectStateCalc.getSensorData(portNum, alRawData[pos])?.toInt() ?: continue
            if(sensorData == troubleCode) {
                if(troubleBegTime == 0) troubleBegTime = rawTime
            } else {
                if(troubleBegTime != 0) {
                    if(rawTime - troubleBegTime > minTime)
                        aText.alGTD.add(GraphicTextData(troubleBegTime, rawTime, aFillColorIndex, aBorderColorIndex, aTextColorIndex, troubleDescr, fullTroubleDescr))
                    troubleBegTime = 0
                }
            }
        }
        //--- запись последней незакрытой проблемы
        if(troubleBegTime != 0) {
            if(min(getCurrentTimeInt(), endTime) - troubleBegTime > minTime)
                aText.alGTD.add(
                    GraphicTextData(
                        troubleBegTime, min(getCurrentTimeInt(), endTime), aFillColorIndex, aBorderColorIndex, aTextColorIndex, troubleDescr, fullTroubleDescr
                    )
                )
            //troubleBegTime = 0; - уже и не надо
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

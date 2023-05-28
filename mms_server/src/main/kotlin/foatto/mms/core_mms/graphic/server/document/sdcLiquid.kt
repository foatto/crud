package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.graphic.AxisYData
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicElement
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.graphic.server.MMSGraphicDocumentConfig
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigGeo
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class sdcLiquid : sdcAbstractAnalog() {

    companion object {
        //--- ловля основных/системных нештатных ситуаций, показываемых только на первом/верхнем графике:
        //--- нет связи, нет данных и резервное питание
        fun checkLiquidLevelSensorTrouble(
            alRawTime: List<Int>,
            alRawData: List<AdvancedByteBuffer>,
            sca: SensorConfigAnalogue,
            begTime: Int,
            endTime: Int,
            aText: GraphicDataContainer
        ) {
            //--- ловим ошибки с датчиков уровня топлива
            val alGTD = aText.alGTD.toMutableList()
            SensorConfigLiquidLevel.hmLLErrorCodeDescr.keys.forEach { errorCode ->
                checkSensorError(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    portNum = sca.portNum,
                    sensorDescr = sca.descr,
                    begTime = begTime,
                    endTime = endTime,
                    aFillColorIndex = GraphicColorIndex.FILL_CRITICAL,
                    aBorderColorIndex = GraphicColorIndex.BORDER_CRITICAL,
                    aTextColorIndex = GraphicColorIndex.TEXT_CRITICAL,
                    troubleCode = errorCode,
                    troubleDescr = SensorConfigLiquidLevel.hmLLErrorCodeDescr[errorCode]!!,
                    minTime = SensorConfigLiquidLevel.hmLLMinSensorErrorTime[errorCode]!!,
                    alGTD = alGTD
                )
            }
            aText.alGTD = alGTD
        }
    }

    private lateinit var liquidGraphicHandler: AnalogGraphicHandler

    private lateinit var hmUsingSensors: MutableMap<Int, SensorConfig>

//    private val alScaep = mutableListOf<SensorConfigA>()
//    private val alEnergoPowerMin = mutableListOf<GraphicDataContainer?>()
//    private val alEnergoPowerMax = mutableListOf<GraphicDataContainer?>()

    //--- точки значений с электросчётчика будут уже совсем лишними
    //private MutableList<CoreGraphicDataContainerPoint> alEnergoPowerPoint = new MutableList<>();
//    private val alEnergoPowerLine = mutableListOf<GraphicDataContainer?>()

    override fun getGraphicElements(
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
    ) {
        val mmsgdc = GraphicDocumentConfig.hmConfig[documentTypeName] as MMSGraphicDocumentConfig
        liquidGraphicHandler = mmsgdc.graphicHandler as AnalogGraphicHandler

        hmUsingSensors = mutableMapOf<Int, SensorConfig>().apply {
            putAll((objectConfig.hmSensorConfig[SensorConfig.SENSOR_MASS_ACCUMULATED] ?: emptyMap()))
            putAll((objectConfig.hmSensorConfig[SensorConfig.SENSOR_VOLUME_ACCUMULATED] ?: emptyMap()))
            putAll((objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING] ?: emptyMap()))
        }

        val hmAllMassAndVolumeSensors = mutableMapOf<Int, SensorConfig>()
        objectConfig.hmSensorConfig[SensorConfig.SENSOR_MASS_FLOW]?.let { allMassFlowSensors ->
            hmAllMassAndVolumeSensors.putAll(allMassFlowSensors)
        }
        objectConfig.hmSensorConfig[SensorConfig.SENSOR_VOLUME_FLOW]?.let { allVolumeFlowSensors ->
            hmAllMassAndVolumeSensors.putAll(allVolumeFlowSensors)
        }

        //--- графики уровня топлива с сопутствующими графиками датчиков массовых/объёмных расходов
        //--- и графиков расчётного потока топлива, связанных с данным датчиков уровня топлива
        objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_LEVEL]?.let { hmSensorConfig ->
            hmSensorConfig.values.forEach { sc ->
                val sca = sc as SensorConfigAnalogue

                val massFlowSensorsInThisGroup = objectConfig.hmSensorConfig[SensorConfig.SENSOR_MASS_FLOW]?.values?.filter { smf ->
                    smf.group == sca.group
                }?.map { smf ->
                    smf as SensorConfigAnalogue
                } ?: emptyList()

                val volumeFlowSensorsInThisGroup = objectConfig.hmSensorConfig[SensorConfig.SENSOR_VOLUME_FLOW]?.values?.filter { svf ->
                    svf.group == sca.group
                }?.map { svf ->
                    svf as SensorConfigAnalogue
                } ?: emptyList()

                val flowSensorsGraphicHandlers = mutableListOf<AnalogGraphicHandler>()
                massFlowSensorsInThisGroup.forEach { _ ->
                    flowSensorsGraphicHandlers.add(AnalogGraphicHandler())
                }
                volumeFlowSensorsInThisGroup.forEach { _ ->
                    flowSensorsGraphicHandlers.add(AnalogGraphicHandler())
                }

                getGraphicElement(
                    graphicTitle = sca.descr,
                    begTime = begTime,
                    endTime = endTime,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    objectConfig = objectConfig,
                    alSca = listOf(sca) + massFlowSensorsInThisGroup + volumeFlowSensorsInThisGroup,
                    alGraphicHandler = listOf(liquidGraphicHandler) + flowSensorsGraphicHandlers,
                    tmElement = tmElement,
                    tmElementVisibleConfig = tmElementVisibleConfig,
                    alLegend = listOf(
                        Triple(hmIndexColor[GraphicColorIndex.LINE_ABOVE_0]!!, false, "Заправка"),
                        Triple(hmIndexColor[GraphicColorIndex.LINE_NORMAL_0]!!, false, "Расход"),
                        Triple(hmIndexColor[GraphicColorIndex.LINE_BELOW_0]!!, false, "Слив"),
                    ),
                )

                massFlowSensorsInThisGroup.forEach { smf ->
                    hmAllMassAndVolumeSensors.remove(smf.portNum)
                }
                volumeFlowSensorsInThisGroup.forEach { svf ->
                    hmAllMassAndVolumeSensors.remove(svf.portNum)
                }
            }
        }

//        //--- графики датчиков массовых/объёмных расходов, не входящих в группы уровнемеров
//        val hmMassAndVolumeFlowSensorsByGroup = mutableMapOf<String, MutableList<SensorConfigAnalogue>>()
//        hmAllMassAndVolumeSensors.values.forEach { sc ->
//            val groupedSensorList = hmMassAndVolumeFlowSensorsByGroup.getOrPut(sc.group) { mutableListOf() }
//            groupedSensorList += sc as SensorConfigAnalogue
//        }
//        hmMassAndVolumeFlowSensorsByGroup.forEach { (group, groupedSensorList) ->
//            getGraphicElement(
//                graphicTitle = group,
//                begTime = begTime,
//                endTime = endTime,
//                viewWidth = viewWidth,
//                viewHeight = viewHeight,
//                alRawTime = alRawTime,
//                alRawData = alRawData,
//                objectConfig = objectConfig,
//                alSca = groupedSensorList,
//                alGraphicHandler = groupedSensorList.map { AnalogGraphicHandler() },
//                tmElement = tmElement,
//                tmElementVisibleConfig = tmElementVisibleConfig,
//            )
//        }

        //--- графики расчётного потока топлива, связанного с расходомером
        hmUsingSensors.values.forEach { sc ->
            val scc = sc as SensorConfigCounter

            //--- есть ли вообще прописанный датчик расчётного потока жидкости на этом порту
            objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_FLOW_CALC]?.values?.firstOrNull { sclfc ->
                sclfc.portNum == scc.portNum
            }?.let { sclfc ->
                val sclfcOnSamePort = sclfc as SensorConfigAnalogue

                getGraphicElement(
                    graphicTitle = sclfcOnSamePort.descr,
                    begTime = begTime,
                    endTime = endTime,
                    viewWidth = viewWidth,
                    viewHeight = viewHeight,
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    objectConfig = objectConfig,
                    alSca = listOf(sclfcOnSamePort),
                    alGraphicHandler = listOf(AnalogGraphicHandler()),
                    tmElement = tmElement,
                    tmElementVisibleConfig = tmElementVisibleConfig,
                )
            }
        }

        //            //--- вывод датчика мощности
        //
        //            //--- есть ли вообще прописанные датчики мощности
        //            val hmSCEP = oc.hmSensorConfig[SensorConfig.SENSOR_POWER]
        //            var axisIndex = 2
        //            if(!hmSCEP.isNullOrEmpty()) {
        //                val pgh = PowerGraphicHandler()
        //                //--- есть ли датчик мощности на том же порту, что и текущий уровнемер
        //                val scaep = hmSCEP[sca.portNum] as? SensorConfigA
        //                if(scaep != null) {
        //                    alAxisYData.add(
        //                        AxisYData(
        //                            SensorConfig.hmSensorDescr[scaep.sensorType] + ", ${scaep.dim}",
        //                            scaep.minView, scaep.maxView, GraphicColorIndex.AXIS_2
        //                        )
        //                    )
        //
        //                    val aEnergoPowerMin = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1)
        //                    val aEnergoPowerMax = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1)
        //                    val aEnergoPowerPoint: GraphicDataContainer? = null/*isShowPoint && (  ! isShowLine || scaep.smoothTime > 0  ) ?
        //                                new CoreGraphicDataContainerPoint(  axisIndex, MAX_GPD_SIZE  ) :*/
        //                    val aEnergoPowerLine = if(isShowLine) GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 2) else null
        //
        //                    ObjectCalc.getSmoothAnalogGraphicData(
        //                        alRawTime, alRawData, oc, scaep, begTime, endTime, xScale, yScale,
        //                        aEnergoPowerMin, aEnergoPowerMax, aEnergoPowerPoint, aEnergoPowerLine, pgh
        //                    )
        //
        //                    alScaep.add(scaep)
        //                    alEnergoPowerMin.add(aEnergoPowerMin)
        //                    alEnergoPowerMax.add(aEnergoPowerMax)
        //                    //alEnergoPowerPoint.add(  aEnergoPowerPoint  );
        //                    alEnergoPowerLine.add(aEnergoPowerLine)
        //
        //                    axisIndex++
        //                }
        //            }
//        for(i in alScaep.indices) {
//            //        //--- различные текстовые обозначения
//            //        if(  aText != null && ! aText.alGTD.isEmpty()  ) aText.write(  bbOut, x1  );
//            alGDC.add(alEnergoPowerMin[i])
//            alGDC.add(alEnergoPowerMax[i])
//            //--- точные значения в виде точек
//            //if(  alEnergoPowerPoint.get(  i  ) != null && ! alEnergoPowerPoint.get(  i  ).alGPD.isEmpty()  )
//            //    alEnergoPowerPoint.get(  i  ).write(  bbOut, x1  );
//            //--- сглаженная линия усреднённых значений
//            alGDC.add(alEnergoPowerLine[i])
//        }
    }

    override fun calcGraphicData(
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        scg: SensorConfigGeo?,
        sca: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        axisIndex: Int,
        aMinLimit: GraphicDataContainer?,
        aMaxLimit: GraphicDataContainer?,
        aLine: GraphicDataContainer,
        graphicHandler: iGraphicHandler
    ) {
        if (sca.sensorType == SensorConfig.SENSOR_LIQUID_FLOW_CALC) {
            hmUsingSensors[sca.portNum]?.let { sc ->
                val scc = sc as SensorConfigCounter

                calcLiquidFlowOverLiquidUsing(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    scc = scc,
                    sclfc = sca,
                    begTime = begTime,
                    endTime = endTime,
                    xScale = xScale,
                    yScale = yScale,
                    aLiquidMin = aMinLimit,
                    aLiquidMax = aMaxLimit,
                    aLiquidFlow = aLine,
                )
            }
        } else {
            super.calcGraphicData(
                alRawTime = alRawTime,
                alRawData = alRawData,
                scg = scg,
                sca = sca,
                begTime = begTime,
                endTime = endTime,
                xScale = xScale,
                yScale = yScale,
                axisIndex = axisIndex,
                aMinLimit = aMinLimit,
                aMaxLimit = aMaxLimit,
                aLine = aLine,
                graphicHandler = graphicHandler
            )
        }
    }

    override fun graphicElementPostCalc(
        begTime: Int,
        endTime: Int,
        sca: SensorConfigAnalogue,
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        axisIndex: Int,
        aLine: GraphicDataContainer?,
        aText: GraphicDataContainer?,
    ) {
        //--- только для основого графика
        if (sca is SensorConfigLiquidLevel) {
            //--- постобработка/фильтрация заправок/сливов/расходов
            aLine?.let {
                ObjectCalc.getLiquidStatePeriodData(sca, axisIndex, aLine, mutableListOf(), liquidGraphicHandler as LiquidGraphicHandler)
            }
            aText?.let {
                //--- ловим ошибки с датчиков уровня топлива
                checkLiquidLevelSensorTrouble(
                    alRawTime = alRawTime,
                    alRawData = alRawData,
                    sca = sca,
                    begTime = begTime,
                    endTime = endTime,
                    aText = aText,
                )
            }
        }
    }

    override fun addGraphicItem(
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
    ): Int {
        var axisIndex = aAxisIndex

        //--- только для основого графика
        if (sca is SensorConfigLiquidLevel) {
            aLine?.alGLD?.let { alGLD ->
                //--- есть ли вообще прописанный датчик расчётного потока жидкости на этом порту
                objectConfig.hmSensorConfig[SensorConfig.SENSOR_LIQUID_FLOW_CALC]?.values?.firstOrNull { sclfc ->
                    sclfc.portNum == sca.portNum
                }?.let { sclfc ->
                    val sclfcOnSamePort = sclfc as SensorConfigAnalogue

                    val xScale = if (viewWidth == 0) {
                        0
                    } else {
                        (endTime - begTime) / (viewWidth / DOT_PER_MM)
                    }
                    val yScale = if (viewHeight == 0) {
                        0.0
                    } else {
                        (sclfcOnSamePort.maxView - sclfcOnSamePort.minView) / (viewHeight / DOT_PER_MM)
                    }

                    //--- расчет скорости расхода жидкости по уровнемеру
                    val aLiquidMin = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, false)
                    val aLiquidMax = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 1, false)
                    val aLiquidFlow = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, axisIndex, 2, false)

                    calcLiquidFlowOverLiquidLevel(
                        alAxisYData = alAxisYData,
                        sca = sca,
                        sclfc = sclfcOnSamePort,
                        begTime = begTime,
                        endTime = endTime,
                        xScale = xScale,
                        yScale = yScale,
                        alGLD = alGLD,
                        aLiquidMin = aLiquidMin,
                        aLiquidMax = aLiquidMax,
                        aLiquidFlow = aLiquidFlow,
                    )

                    alGDC.addAll(listOfNotNull(aText, aLiquidMin, aLiquidMax, aLiquidFlow).filter { it.isNotEmpty() })
                    axisIndex++
                }
            }
        }

        return axisIndex
    }

    private fun calcLiquidFlowOverLiquidUsing(
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        scc: SensorConfigCounter,
        sclfc: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        aLiquidMin: GraphicDataContainer?,
        aLiquidMax: GraphicDataContainer?,
        aLiquidFlow: GraphicDataContainer,
    ) {
        //--- альтернативное (более угрублённое) масштабирования для повышения скорости расчётов
        val altXScale = xScale * 8
        val isLimit = sclfc.maxLimit != sclfc.minLimit
        if (isLimit) {
            aLiquidMin!!.alGLD = listOf(
                GraphicLineData(begTime, sclfc.minLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, sclfc.minLimit, GraphicColorIndex.LINE_LIMIT)
            )
            aLiquidMax!!.alGLD = listOf(
                GraphicLineData(begTime, sclfc.maxLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, sclfc.maxLimit, GraphicColorIndex.LINE_LIMIT)
            )
        }
        //--- х-координата последней усреднённой точки
        var lastAvgTime = 0
        var lastLiquidUsingPerHour = 0.0

        //--- теперь по данным счётчика посчитаем скорость расхода жидкости
        for (pos1 in alRawTime.indices) {
            val time1 = alRawTime[pos1]

            //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
            if (time1 < begTime) {
                continue
            }
            if (time1 > endTime) {
                break
            }

            //--- дополнительное входное огрубления для повышения скорости расчёта
            if (time1 - lastAvgTime <= altXScale) {
                continue
            }

            //--- поиск правой границы диапазона сглаживания
            var pos2 = pos1 + 1
            while (pos2 < alRawTime.size && alRawTime[pos2] <= endTime) {
                //--- умножаем период сглаживания на 2,
                //--- т.к. период сглаживания сам по себе задается как +- радиус от точки,
                //--- а здесь используется "диагональное" расстояние между двумя крайними точками.
                if (alRawTime[pos2] - time1 > sclfc.smoothTime * 2) {
                    break
                }
                pos2++
            }
            //--- если правая граница диапазона сглаживания достигла конца данных,
            //--- то прекращаем расчёты, иначе на концах графиков можем поиметь локальные вылеты из-за недостаточности данных для сглаживания
            if (pos2 >= alRawTime.size || alRawTime[pos2] > endTime) {
                break
            }

            //--- вычисляем среднюю х-координату усреднения
            var sumTime: Long = 0   // суммировать время в Int поле грозит быстрым переполнением
            for (p in pos1 until pos2) {
                sumTime += alRawTime[p]
            }
            val avgTime = (sumTime / (pos2 - pos1)).toInt()
            val sumData = ObjectCalc.calcCounterOrAccumulatedSensor(alRawTime, alRawData, scc, alRawTime[pos1], alRawTime[pos2]).value

            val timeDelta = alRawTime[pos2 - 1] - alRawTime[pos1]
            var liquidUsingPerHour = if (timeDelta == 0) {
                0.0
            } else {
                sumData * 3600 / timeDelta
            }

            //--- отрицательный расход ( т.е. заправка ) нас не интересуют
            if (liquidUsingPerHour < 0) {
                liquidUsingPerHour = 0.0
            }
            //--- уход графика в небо из-за слива жидкости нам тоже не нужен
            if (liquidUsingPerHour > sclfc.maxView) {
                liquidUsingPerHour = sclfc.maxView
            }

            //--- новая средняя точка достаточно далека от предыдущей или отличается от неё цветом
            if (avgTime - lastAvgTime > altXScale || abs(liquidUsingPerHour - lastLiquidUsingPerHour) > yScale) {
                aLiquidFlow.alGLD = aLiquidFlow.alGLD.toMutableList().apply {
                    add(GraphicLineData(avgTime, liquidUsingPerHour, GraphicColorIndex.LINE_NORMAL_1))
                }

                lastAvgTime = avgTime
                lastLiquidUsingPerHour = liquidUsingPerHour
            }
        }
    }

    private fun calcLiquidFlowOverLiquidLevel(
        alAxisYData: MutableList<AxisYData>,
        sca: SensorConfigLiquidLevel,
        sclfc: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        alGLD: List<GraphicLineData>,
        aLiquidMin: GraphicDataContainer,
        aLiquidMax: GraphicDataContainer,
        aLiquidFlow: GraphicDataContainer,
    ) {
        alAxisYData.add(AxisYData("${SensorConfig.hmSensorDescr[sclfc.sensorType]}", sclfc.minView, sclfc.maxView, GraphicColorIndex.AXIS_1, false))

        val isLimit = sclfc.maxLimit != sclfc.minLimit
        if (isLimit) {
            aLiquidMin!!.alGLD = listOf(
                GraphicLineData(begTime, sclfc.minLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, sclfc.minLimit, GraphicColorIndex.LINE_LIMIT)
            )
            aLiquidMax!!.alGLD = listOf(
                GraphicLineData(begTime, sclfc.maxLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, sclfc.maxLimit, GraphicColorIndex.LINE_LIMIT)
            )
        }
        //--- х-координата последней усреднённой точки
        var lastAvgTime = 0
        var lastLiquidUsingPerHour = 0.0
        //--- теперь по сглаженному графику уровня жидкости можно попробовать рассчитать расход по изменению уровня
        NEXT_POINT@ for (pos1 in alGLD.indices) {
            val gpd = alGLD[pos1]
            //--- период сглаживания нельзя начинать с "ненормальной точки"
            if (gpd.colorIndex != GraphicColorIndex.LINE_NORMAL_0) {
                continue
            }
            //--- поиск правой границы диапазона сглаживания
            var pos2 = pos1 + 1
            while (pos2 < alGLD.size) {
                val gpd2 = alGLD[pos2]
                //--- если в период сглаживания начали попадать "ненормальные" точки,
                //--- прекращаем расширение периода и сразу переходим на поиск следующего периода
                if (gpd2.colorIndex != GraphicColorIndex.LINE_NORMAL_0) {
                    continue@NEXT_POINT
                }
                //--- умножаем период сглаживания на 2,
                //--- т.к. период сглаживания сам по себе задается как +- радиус от точки,
                //--- а здесь используется "диагональное" расстояние между двумя крайними точками.
                //--- тонкий момент - сглаживание датчика скорости расхода жидкости
                //--- не должно быть меньше сглаживания исходного датчика уровня жидкости,
                //--- т.к. "ближайших данных" по уровню жидкости в таком случае может и вовсе не найтись
                if (gpd2.x - gpd.x > max(sca.smoothTime, sclfc.smoothTime) * 2) {
                    break
                }
                pos2++
            }
            //--- если правая граница диапазона сглаживания достигла конца данных,
            //--- то прекращаем расчёты, иначе на концах графиков можем поиметь локальные вылеты из-за недостаточности данных для сглаживания
            if (pos2 >= alGLD.size) {
                break
            }
            //--- вычисляем среднюю х-координату усреднения
            var sumTime: Long = 0   // суммировать время в Int поле грозит быстрым переполнением
            for (p in pos1 until pos2) {
                sumTime += alGLD[p].x
            }
            val avgTime = (sumTime / (pos2 - pos1)).toInt()

            //--- определяем среднюю скорость расхода топлива как первую производную по изменению уровня с обратным знаком
            val timeDelta = alGLD[pos2 - 1].x - alGLD[pos1].x
            var liquidUsingPerHour = if (timeDelta == 0) {
                0.0
            } else {
                -(alGLD[pos2 - 1].y - alGLD[pos1].y) * 3600 / timeDelta
            }

            //--- отрицательный расход ( т.е. заправка ) нас не интересуют
            if (liquidUsingPerHour < 0) {
                liquidUsingPerHour = 0.0
            }
            //--- уход графика в небо из-за слива жидкости нам тоже не нужен
            if (liquidUsingPerHour > sclfc.maxView) {
                liquidUsingPerHour = sclfc.maxView
            }

            //--- новая средняя точка достаточно далека от предыдущей или отличается от неё цветом
            if (avgTime - lastAvgTime > xScale || abs(liquidUsingPerHour - lastLiquidUsingPerHour) > yScale) {
                aLiquidFlow!!.alGLD = aLiquidFlow!!.alGLD.toMutableList().apply {
                    add(GraphicLineData(avgTime, liquidUsingPerHour, GraphicColorIndex.LINE_NORMAL_1))
                }
                lastAvgTime = avgTime
                lastLiquidUsingPerHour = liquidUsingPerHour
            }
        }

    }

}

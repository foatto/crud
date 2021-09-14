package foatto.mms.core_mms.graphic.server.document

import foatto.core.app.graphic.AxisYData
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.app.graphic.GraphicDataContainer
import foatto.core.app.graphic.GraphicLineData
import foatto.core.util.AdvancedByteBuffer
import foatto.mms.core_mms.ObjectConfig
import foatto.mms.core_mms.calc.AbstractObjectStateCalc
import foatto.mms.core_mms.calc.ObjectCalc
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.sensor.config.SensorConfigAnalogue
import foatto.mms.core_mms.sensor.config.SensorConfigCounter
import foatto.mms.core_mms.sensor.config.SensorConfigLiquidLevel
import kotlin.math.abs
import kotlin.math.max

class sdcLiquid : sdcAnalog() {

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
            for (errorCode in SensorConfigLiquidLevel.hmLLErrorCodeDescr.keys)
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
            aText.alGTD = alGTD.toTypedArray()
        }
    }

    private var isLiquidFlow = false
    private var aLiquidMin: GraphicDataContainer? = null
    private var aLiquidMax: GraphicDataContainer? = null
    private var aLiquidFlow: GraphicDataContainer? = null

//    private val alScaep = mutableListOf<SensorConfigA>()
//    private val alEnergoPowerMin = mutableListOf<GraphicDataContainer?>()
//    private val alEnergoPowerMax = mutableListOf<GraphicDataContainer?>()

    //--- точки значений с электросчётчика будут уже совсем лишними
    //private MutableList<CoreGraphicDataContainerPoint> alEnergoPowerPoint = new MutableList<>();
//    private val alEnergoPowerLine = mutableListOf<GraphicDataContainer?>()

    override fun calcGraphic(
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

        super.calcGraphic(
            graphicHandler = graphicHandler,
            alRawTime = alRawTime,
            alRawData = alRawData,
            oc = oc,
            sca = sca,
            begTime = begTime,
            endTime = endTime,
            xScale = xScale,
            yScale = yScale,
            alAxisYData = alAxisYData,
            aMinLimit = aMinLimit,
            aMaxLimit = aMaxLimit,
            aBack = aBack,
            aPoint = aPoint,
            aLine = aLine,
            aText = aText
        )

        //--- постобработка/фильтрация заправок/сливов/расходов
        aLine?.let {
            ObjectCalc.getLiquidStatePeriodData(sca as SensorConfigLiquidLevel, aLine, mutableListOf(), graphicHandler as LiquidGraphicHandler)
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

        //--- расчет скорости расхода жидкости по счётчику топлива или уровнемеру
        isLiquidFlow = false
        aLiquidMin = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 1, 1)
        aLiquidMax = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 1, 1)
        aLiquidFlow = GraphicDataContainer(GraphicDataContainer.ElementType.LINE, 1, 2)

        aLine?.let {

            //--- есть ли вообще прописанные датчики скорости расхода жидкости
            val hmSCLF = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_FLOW_CALC].orEmpty()
            val scafInGroup = hmSCLF.values.map { it as SensorConfigAnalogue }.firstOrNull { it.group == sca.group }

            //--- обработка счётчиков топлива
            val hmSCLU = oc.hmSensorConfig[SensorConfig.SENSOR_LIQUID_USING].orEmpty()
            val alSclu = hmSCLU.values.filter { it.group == sca.group }.map { it as SensorConfigCounter }.toList()

            //--- если есть расходомер и он только один (находим датчик скорости расхода) и считаем по нему
            if (scafInGroup != null && alSclu.size == 1) {
                isLiquidFlow = true
                calcLiquidFlowOverLiquidUsing(alRawTime, alRawData, alAxisYData, alSclu.first(), scafInGroup, begTime, endTime, xScale, yScale)
            }
            //--- расходомеров не нашлось (или их >1), считаем через изменение уровня топлива
            else {
                //--- есть ли ( виртуальный ) датчик скорости расхода жидкости на том же порту, что и текущий уровнемер
                val scafInPort = hmSCLF[sca.portNum] as? SensorConfigAnalogue
                if (scafInPort != null && aLine.alGLD.isNotEmpty()) {
                    isLiquidFlow = true
                    calcLiquidFlowOverLiquidLevel(alAxisYData, sca as SensorConfigLiquidLevel, scafInPort, begTime, endTime, xScale, yScale, aLine.alGLD)
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
        }
    }

    //--- собственно вывод данных для возможности перекрытия наследниками
    override fun outGraphicData(alGDC: MutableList<GraphicDataContainer?>) {
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
        if (isLiquidFlow) {
            alGDC.add(aLiquidMin)
            alGDC.add(aLiquidMax)
            alGDC.add(aLiquidFlow)
        }
    }

    private fun calcLiquidFlowOverLiquidUsing(
        alRawTime: List<Int>,
        alRawData: List<AdvancedByteBuffer>,
        alAxisYData: MutableList<AxisYData>,
        scu: SensorConfigCounter,
        scaf: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double
    ) {
        alAxisYData.add(AxisYData("${SensorConfig.hmSensorDescr[scaf.sensorType]}", scaf.minView, scaf.maxView, GraphicColorIndex.AXIS_1))

        val isLimit = scaf.maxLimit > scaf.minLimit
        if (isLimit) {
            aLiquidMin!!.alGLD = arrayOf(
                GraphicLineData(begTime, scaf.minLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, scaf.minLimit, GraphicColorIndex.LINE_LIMIT)
            )
            aLiquidMax!!.alGLD = arrayOf(
                GraphicLineData(begTime, scaf.maxLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, scaf.maxLimit, GraphicColorIndex.LINE_LIMIT)
            )
        }
        //--- х-координата последней усреднённой точки
        var lastAvgTime = 0
        var lastLiquidUsingPerHour = 0.0

        //--- теперь по данным счётчика посчитаем скорость расхода жидкости
        for (pos1 in alRawTime.indices) {
            val time1 = alRawTime[pos1]

            //--- сразу пропускаем запредельные точки, загруженные для бесшовного сглаживания между соседними диапазонами
            if (time1 < begTime) continue
            if (time1 > endTime) break

            //--- поиск правой границы диапазона сглаживания
            var pos2 = pos1 + 1
            while (pos2 < alRawTime.size && alRawTime[pos2] <= endTime) {
                //--- умножаем период сглаживания на 2,
                //--- т.к. период сглаживания сам по себе задается как +- радиус от точки,
                //--- а здесь используется "диагональное" расстояние между двумя крайними точками.
                if (alRawTime[pos2] - time1 > scaf.smoothTime * 2) {
                    break
                }
                pos2++
            }
            //--- если правая граница диапазона сглаживания достигла конца данных,
            //--- то прекращаем расчёты, иначе на концах графиков можем поиметь локальные вылеты из-за недостаточности данных для сглаживания
            if (pos2 >= alRawTime.size || alRawTime[pos2] > endTime) break

            //--- вычисляем среднюю х-координату усреднения
            var sumTime: Long = 0   // суммировать время в Int поле грозит быстрым переполнением
            var sumSensor = 0.0
            for (p in pos1 until pos2) {
                sumTime += alRawTime[p]

                val rawSensorData = AbstractObjectStateCalc.getSensorData(scu.portNum, alRawData[p]) ?: continue
                val sensorData = when (rawSensorData) {
                    is Int -> {
                        rawSensorData.toDouble()
                    }
                    is Double -> {
                        rawSensorData
                    }
                    else -> {
                        0.0
                    }
                }
                //--- вручную игнорируем заграничные значения
                if (ObjectCalc.isIgnoreSensorData(scu, sensorData)) continue
                sumSensor += sensorData
            }
            val avgTime = (sumTime / (pos2 - pos1)).toInt()
            val sumData = AbstractObjectStateCalc.getSensorValue(scu.alValueSensor, scu.alValueData, sumSensor)

            //--- определяем среднюю скорость расхода топлива как первую производную по изменению уровня с обратным знаком
            val timeDelta = alRawTime[pos2 - 1] - alRawTime[pos1]
            var liquidUsingPerHour = if (timeDelta == 0) 0.0 else sumData * 3600 / timeDelta

            //--- уход графика в небо из-за слива жидкости нам тоже не нужен
            if (liquidUsingPerHour > scaf.maxView) liquidUsingPerHour = scaf.maxView

            //--- новая средняя точка достаточно далека от предыдущей или отличается от неё цветом
            if (avgTime - lastAvgTime > xScale || abs(liquidUsingPerHour - lastLiquidUsingPerHour) > yScale) {
                aLiquidFlow!!.alGLD = aLiquidFlow!!.alGLD.toMutableList().apply {
                    add(GraphicLineData(avgTime, liquidUsingPerHour, GraphicColorIndex.LINE_NORMAL_1))
                }.toTypedArray()

                lastAvgTime = avgTime
                lastLiquidUsingPerHour = liquidUsingPerHour
            }
        }
    }

    private fun calcLiquidFlowOverLiquidLevel(
        alAxisYData: MutableList<AxisYData>,
        sca: SensorConfigLiquidLevel,
        scaf: SensorConfigAnalogue,
        begTime: Int,
        endTime: Int,
        xScale: Int,
        yScale: Double,
        alGLD: Array<GraphicLineData>
    ) {
        alAxisYData.add(AxisYData("${SensorConfig.hmSensorDescr[scaf.sensorType]}", scaf.minView, scaf.maxView, GraphicColorIndex.AXIS_1))

        val isLimit = scaf.maxLimit != scaf.minLimit
        if (isLimit) {
            aLiquidMin!!.alGLD = arrayOf(
                GraphicLineData(begTime, scaf.minLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, scaf.minLimit, GraphicColorIndex.LINE_LIMIT)
            )
            aLiquidMax!!.alGLD = arrayOf(
                GraphicLineData(begTime, scaf.maxLimit, GraphicColorIndex.LINE_LIMIT),
                GraphicLineData(endTime, scaf.maxLimit, GraphicColorIndex.LINE_LIMIT)
            )
        }
        //--- х-координата последней усреднённой точки
        var lastAvgTime = 0
        var lastLiquidUsingPerHour = 0.0
        //--- теперь по сглаженному графику уровня жидкости можно попробовать рассчитать расход по изменению уровня
        NEXT_POINT@ for (pos1 in alGLD.indices) {
            val gpd = alGLD[pos1]
            //--- период сглаживания нельзя начинать с "ненормальной точки"
            if (gpd.colorIndex != GraphicColorIndex.LINE_NORMAL_0) continue
            //--- поиск правой границы диапазона сглаживания
            var pos2 = pos1 + 1
            while (pos2 < alGLD.size) {
                val gpd2 = alGLD[pos2]
                //--- если в период сглаживания начали попадать "ненормальные" точки,
                //--- прекращаем расширение периода и сразу переходим на поиск следующего периода
                if (gpd2.colorIndex != GraphicColorIndex.LINE_NORMAL_0) continue@NEXT_POINT
                //--- умножаем период сглаживания на 2,
                //--- т.к. период сглаживания сам по себе задается как +- радиус от точки,
                //--- а здесь используется "диагональное" расстояние между двумя крайними точками.
                //--- тонкий момент - сглаживание датчика скорости расхода жидкости
                //--- не должно быть меньше сглаживания исходного датчика уровня жидкости,
                //--- т.к. "ближайших данных" по уровню жидкости в таком случае может и вовсе не найтись
                if (gpd2.x - gpd.x > max(sca.smoothTime, scaf.smoothTime) * 2) break
                pos2++
            }
            //--- если правая граница диапазона сглаживания достигла конца данных,
            //--- то прекращаем расчёты, иначе на концах графиков можем поиметь локальные вылеты из-за недостаточности данных для сглаживания
            if (pos2 >= alGLD.size) break
            //--- вычисляем среднюю х-координату усреднения
            var sumTime: Long = 0   // суммировать время в Int поле грозит быстрым переполнением
            for (p in pos1 until pos2) {
                sumTime += alGLD[p].x
            }
            val avgTime = (sumTime / (pos2 - pos1)).toInt()

            //--- определяем среднюю скорость расхода топлива как первую производную по изменению уровня с обратным знаком
            val timeDelta = alGLD[pos2 - 1].x - alGLD[pos1].x
            var liquidUsingPerHour = if (timeDelta == 0) 0.0 else -(alGLD[pos2 - 1].y - alGLD[pos1].y) * 3600 / timeDelta

            //--- отрицательный расход ( т.е. заправка ) нас не интересуют
            if (liquidUsingPerHour < 0) liquidUsingPerHour = 0.0
            //--- уход графика в небо из-за слива жидкости нам тоже не нужен
            if (liquidUsingPerHour > scaf.maxView) liquidUsingPerHour = scaf.maxView

            //--- новая средняя точка достаточно далека от предыдущей или отличается от неё цветом
            if (avgTime - lastAvgTime > xScale || abs(liquidUsingPerHour - lastLiquidUsingPerHour) > yScale) {
                aLiquidFlow!!.alGLD = aLiquidFlow!!.alGLD.toMutableList().apply {
                    add(GraphicLineData(avgTime, liquidUsingPerHour, GraphicColorIndex.LINE_NORMAL_1))
                }.toTypedArray()
                lastAvgTime = avgTime
                lastLiquidUsingPerHour = liquidUsingPerHour
            }
        }

    }

}

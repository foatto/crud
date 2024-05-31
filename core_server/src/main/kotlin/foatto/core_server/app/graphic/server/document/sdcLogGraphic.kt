package foatto.core_server.app.graphic.server.document

import foatto.core.app.chart.*
import foatto.util.loadTextFile
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import java.io.File
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.max

class sdcLogGraphic : sdcAbstractGraphic() {

    override fun doGetElements(chartActionRequest: ChartActionRequest): ChartActionResponse {

        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + chartActionRequest.startParamId] as GraphicStartData

        val x1 = chartActionRequest.graphicCoords!!.first
        val x2 = chartActionRequest.graphicCoords!!.second
        val viewWidth = chartActionRequest.viewSize!!.first
        val viewHeight = chartActionRequest.viewSize!!.second

        //--- подготовка сырых данных для графиков

        val maxWorkers = 16
        var maxHandlers = 0
        var maxMemory = 0

        val tmWorkers = TreeMap<Int, Int>()
        val tmHandlers = TreeMap<Int, Int>()
        val tmMemoryUsed = TreeMap<Int, Int>()
        val tmMemoryTotal = TreeMap<Int, Int>()

        val dirLog = File(application.hmAliasLogDir[sd.fullTitle]!!)
        val alLogWord = ArrayList<String>()

        val arrFile = dirLog.listFiles()
        for (file in arrFile!!) {
            if (!file.isFile) continue

            var st = StringTokenizer(file.name, "-.")
            val logFileTime = ZonedDateTime.of(st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), 0, 0, 0, zoneId).toEpochSecond().toInt()
            if (logFileTime > x2 || logFileTime + 60 * 60 < x1) continue

            val alLogStr = loadTextFile(file.toPath())
            //2017.09.29 16:19:18 [ INFO ] ==================== DataServer started ====================
            //2017.09.29 16:19:18 [ INFO ] --- DataWorker started = 1
            for (logLine in alLogStr) {
                alLogWord.clear()
                st = StringTokenizer(logLine, " ")
                while (st.hasMoreTokens()) alLogWord.add(st.nextToken())
                //2017.09.29 16:36:09 [ INFO ] Workers = 1
                //2017.09.29 16:36:09 [ INFO ] Handlers = 1
                //2017.09.29 16:36:09 [ INFO ] Used = 23
                //2017.09.29 16:36:09 [ INFO ] Total = 248
                //2017.09.29 16:36:09 [ INFO ] Max = 910
                if (alLogWord.size == 6 && alLogWord[2] == "[ INFO ]" && alLogWord[4] == "=") {
                    val stDate = StringTokenizer(alLogWord[0], ".")
                    val stTime = StringTokenizer(alLogWord[1], ":")
                    val logStrTime = ZonedDateTime.of(
                        stDate.nextToken().toInt(), stDate.nextToken().toInt(), stDate.nextToken().toInt(),
                        stTime.nextToken().toInt(), stTime.nextToken().toInt(), stTime.nextToken().toInt(), 0, zoneId
                    ).toEpochSecond().toInt()
                    if (logStrTime < x1 || logStrTime > x2) continue

                    val logValue = Integer.parseInt(alLogWord[5])

                    when (alLogWord[3]) {
                        "Workers" -> tmWorkers[logStrTime] = logValue
                        "Handlers" -> {
                            tmHandlers[logStrTime] = logValue
                            maxHandlers = max(maxHandlers, logValue)
                        }

                        "Used" -> tmMemoryUsed[logStrTime] = logValue
                        "Total" -> tmMemoryTotal[logStrTime] = logValue
                        "Max" -> maxMemory = max(maxMemory, logValue)
                    }
                }
            }
        }
        //AdvancedLogger.error(  "work finish:" + tmHandlers.size()  );

        //--- график Workers/Handlers

        val tmElement = TreeMap<String, ChartGroupDTO>()

        val aWorkers = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 3, false)
        getGraphicData(tmWorkers, ChartColorIndex.LINE_NORMAL_0, aWorkers)

        val aHandlers = ChartElementDTO(ChartElementTypeDTO.LINE, 1, 1, false)
        getGraphicData(tmHandlers, ChartColorIndex.LINE_NORMAL_1, aHandlers)

        var alAxisYData = mutableListOf<ChartAxisYDTO>()
        alAxisYData.add(ChartAxisYDTO("Workers", 0.0f, max(4, maxWorkers).toFloat(), ChartColorIndex.AXIS_0, false))
        alAxisYData.add(ChartAxisYDTO("Handlers", 0.0f, max(4, maxHandlers).toFloat(), ChartColorIndex.AXIS_1, false))

        val gewh = ChartGroupDTO(
            chartTitle = "Workers & Handlers",
            alLegend = emptyList(),
            chartHeight = -1.0f,
            alAxisYData = alAxisYData,
            alGDC = listOf(aHandlers, aWorkers)
        )

        tmElement[gewh.chartTitle] = gewh

        //--- график Memory

        val aMemoryUsed = ChartElementDTO(ChartElementTypeDTO.LINE, 0, 3, false)
        getGraphicData(tmMemoryUsed, ChartColorIndex.LINE_NORMAL_0, aMemoryUsed)

        val aMemoryTotal = ChartElementDTO(ChartElementTypeDTO.LINE, 1, 1, false)
        getGraphicData(tmMemoryTotal, ChartColorIndex.LINE_NORMAL_1, aMemoryTotal)

        alAxisYData = mutableListOf()
        alAxisYData.add(ChartAxisYDTO("Memory Used", 0.0f, max(1, maxMemory).toFloat(), ChartColorIndex.AXIS_0, false))
        alAxisYData.add(ChartAxisYDTO("Memory Total", 0.0f, max(1, maxMemory).toFloat(), ChartColorIndex.AXIS_1, false))

        val gem = ChartGroupDTO(
            chartTitle = "Memory Used & Total",
            alLegend = emptyList(),
            chartHeight = -1.0f,
            alAxisYData = alAxisYData,
            alGDC = listOf(aMemoryTotal, aMemoryUsed)
        )

        tmElement[gem.chartTitle] = gem

        //--- конец графиков ---

        val tmVisibleElement = TreeMap<String, Triple<String, String, Boolean>>()
        tmVisibleElement[gewh.chartTitle] = Triple(gewh.chartTitle, "$UP_GRAPHIC_VISIBLE${gewh.chartTitle}", true)
        tmVisibleElement[gem.chartTitle] = Triple(gem.chartTitle, "$UP_GRAPHIC_VISIBLE${gem.chartTitle}", true)

        return ChartActionResponse(
            hmIndexColor = hmIndexColor,
            alCharts = tmElement.toList(),
            alVisibleElement = tmVisibleElement.values.toList(),
            alLegend = emptyList(),
        )
    }

    private fun getGraphicData(
        tmLogData: TreeMap<Int, Int>,
        colorIndex: ChartColorIndex,
        aLogLine: ChartElementDTO
    ) {
        val alGLD = aLogLine.alGLD.toMutableList()
        for (rawTime in tmLogData.keys) {
            alGLD.add(ChartElementLineDTO(rawTime, tmLogData[rawTime]!!.toFloat(), colorIndex))
        }
        aLogLine.alGLD = alGLD
    }

}

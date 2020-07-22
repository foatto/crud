package foatto.core_server.app.graphic.server.document

import foatto.app.CoreSpringApp
import foatto.core.app.graphic.*
import foatto.core_server.app.AppParameter
import foatto.core.util.loadTextFile
import foatto.core_server.app.graphic.server.GraphicStartData
import java.io.File
import java.time.ZonedDateTime
import java.util.*
import kotlin.math.max

class sdcLogGraphic : sdcAbstractGraphic() {

    override fun doGetElements( graphicActionRequest: GraphicActionRequest ): GraphicActionResponse {

        val sd = chmSession[ AppParameter.GRAPHIC_START_DATA + graphicActionRequest.startParamID ] as GraphicStartData

        val x1 = graphicActionRequest.graphicCoords!!.first
        val x2 = graphicActionRequest.graphicCoords!!.second
        val viewWidth = graphicActionRequest.viewSize!!.first
        val viewHeight = graphicActionRequest.viewSize!!.second

        //--- подготовка сырых данных для графиков

        val maxWorkers = 16
        var maxHandlers = 0
        var maxMemory = 0

        val tmWorkers = TreeMap<Int, Int>()
        val tmHandlers = TreeMap<Int, Int>()
        val tmMemoryUsed = TreeMap<Int, Int>()
        val tmMemoryTotal = TreeMap<Int, Int>()

        val dirLog = File( CoreSpringApp.hmAliasLogDir[ sd.sbTitle.toString() ] )
        val alLogWord = ArrayList<String>()

        val arrFile = dirLog.listFiles()
        for( file in arrFile!! ) {
            if( !file.isFile ) continue

            var st = StringTokenizer( file.name, "-." )
            val logFileTime = ZonedDateTime.of(st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), st.nextToken().toInt(), 0, 0, 0, zoneId).toEpochSecond().toInt()
            if( logFileTime > x2 || logFileTime + 60 * 60 < x1 ) continue

            val alLogStr = loadTextFile(file.toPath())
            //2017.09.29 16:19:18 [ INFO ] ==================== DataServer started ====================
            //2017.09.29 16:19:18 [ INFO ] --- DataWorker started = 1
            for( logLine in alLogStr ) {
                alLogWord.clear()
                st = StringTokenizer( logLine, " " )
                while( st.hasMoreTokens() ) alLogWord.add( st.nextToken() )
                //2017.09.29 16:36:09 [ INFO ] Workers = 1
                //2017.09.29 16:36:09 [ INFO ] Handlers = 1
                //2017.09.29 16:36:09 [ INFO ] Used = 23
                //2017.09.29 16:36:09 [ INFO ] Total = 248
                //2017.09.29 16:36:09 [ INFO ] Max = 910
                if( alLogWord.size == 6 && alLogWord[ 2 ] == "[ INFO ]" && alLogWord[ 4 ] == "=" ) {
                    val stDate = StringTokenizer( alLogWord[ 0 ], "." )
                    val stTime = StringTokenizer( alLogWord[ 1 ], ":" )
                    val logStrTime = ZonedDateTime.of(
                        stDate.nextToken().toInt(), stDate.nextToken().toInt(), stDate.nextToken().toInt(),
                        stTime.nextToken().toInt(), stTime.nextToken().toInt(), stTime.nextToken().toInt(), 0, zoneId).toEpochSecond().toInt()
                    if( logStrTime < x1 || logStrTime > x2 ) continue

                    val logValue = Integer.parseInt( alLogWord[ 5 ] )

                    when( alLogWord[ 3 ] ) {
                        "Workers"  -> tmWorkers[ logStrTime ] = logValue
                        "Handlers" -> {
                            tmHandlers[ logStrTime ] = logValue
                            maxHandlers = max( maxHandlers, logValue )
                        }
                        "Used"     -> tmMemoryUsed[ logStrTime ] = logValue
                        "Total"    -> tmMemoryTotal[ logStrTime ] = logValue
                        "Max"      -> maxMemory = max( maxMemory, logValue )
                    }
                }
            }
        }
        //AdvancedLogger.error(  "work finish:" + tmHandlers.size()  );

        //--- график Workers/Handlers

        val tmElement = TreeMap<String, GraphicElement>()

        val aWorkers = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 3 )
        getGraphicData( tmWorkers, GraphicColorIndex.LINE_NORMAL_0, aWorkers )

        val aHandlers = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 1, 1 )
        getGraphicData( tmHandlers, GraphicColorIndex.LINE_NORMAL_1, aHandlers )

        var alAxisYData = mutableListOf<AxisYData>()
        alAxisYData.add( AxisYData( "Workers", 0.0, max( 4, maxWorkers ).toDouble(), GraphicColorIndex.AXIS_0 ) )
        alAxisYData.add( AxisYData( "Handlers", 0.0, max( 4, maxHandlers ).toDouble(), GraphicColorIndex.AXIS_1 ) )

        val gewh = GraphicElement(
            graphicTitle = "Workers & Handlers",
            alIndexColor = hmIndexColor.toList(),
            graphicHeight = -1.0,
            alAxisYData = alAxisYData,
            alGDC = listOf( aHandlers, aWorkers )
        )

        tmElement[ gewh.graphicTitle ] = gewh

        //--- график Memory

        val aMemoryUsed = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 3 )
        getGraphicData( tmMemoryUsed, GraphicColorIndex.LINE_NORMAL_0, aMemoryUsed )

        val aMemoryTotal = GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 1, 1 )
        getGraphicData( tmMemoryTotal, GraphicColorIndex.LINE_NORMAL_1, aMemoryTotal )

        alAxisYData = mutableListOf()
        alAxisYData.add( AxisYData( "Memory Used", 0.0, max( 1, maxMemory ).toDouble(), GraphicColorIndex.AXIS_0 ) )
        alAxisYData.add( AxisYData( "Memory Total", 0.0, max( 1, maxMemory ).toDouble(), GraphicColorIndex.AXIS_1 ) )

        val gem = GraphicElement(
            graphicTitle = "Memory Used & Total",
            alIndexColor = hmIndexColor.toList(),
            graphicHeight = -1.0,
            alAxisYData = alAxisYData,
            alGDC = listOf( aMemoryTotal, aMemoryUsed )
        )

        tmElement[ gem.graphicTitle ] = gem

        //--- конец графиков ---

        val tmVisibleElement = TreeMap<String,String>()
        tmVisibleElement[ gewh.graphicTitle ] = "$UP_GRAPHIC_VISIBLE${gewh.graphicTitle}"
        tmVisibleElement[ gem.graphicTitle ] = "$UP_GRAPHIC_VISIBLE${gem.graphicTitle}"

        return GraphicActionResponse(
            alElement = tmElement.toList(),
            alVisibleElement = tmVisibleElement.toList()
        )
    }

    private fun getGraphicData( tmLogData: TreeMap<Int, Int>, colorIndex: GraphicColorIndex, aLogLine: GraphicDataContainer ) {
        for( rawTime in tmLogData.keys ) aLogLine.alGLD.add( GraphicLineData( rawTime, tmLogData[ rawTime ]!!.toDouble(), colorIndex ) )
    }

}

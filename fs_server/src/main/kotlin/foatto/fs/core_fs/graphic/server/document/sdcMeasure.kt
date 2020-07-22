package foatto.fs.core_fs.graphic.server.document

import foatto.app.CoreSpringApp
import foatto.core.app.UP_GRAPHIC_SHOW_LINE
import foatto.core.app.UP_GRAPHIC_SHOW_POINT
import foatto.core.app.UP_GRAPHIC_SHOW_TEXT
import foatto.core.app.graphic.*
import foatto.core.app.iCoreAppContainer
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.graphic.server.document.sdcAbstractGraphic
import foatto.fs.core_fs.calc.MeasureCalc
import java.util.*

open class sdcMeasure : sdcAbstractGraphic() {

    companion object {
        //--- в худшем случае у нас как минимум 4 точки на мм ( 100 dpi ),
        //--- нет смысла выводить данные в каждый пиксель, достаточно в каждый мм
        private val DOT_PER_MM = 4
    }

    override fun doGetCoords( startParamID: String ): GraphicActionResponse {
//        val begTime: Long
//        val endTime: Long

        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + startParamID] as GraphicStartData

//        if( sd.rangeType == 0 ) {
//            begTime = Arr_DateTime( timeZone, sd.arrBegDT )
//            endTime = Arr_DateTime( timeZone, sd.arrEndDT )
//        }
//        else {
//            val begDateTime = GregorianCalendar( timeZone )
//            begDateTime.add( GregorianCalendar.SECOND, -sd.rangeType )
//            val endDateTime = GregorianCalendar( timeZone )
//            begTime = begDateTime.timeInMillis
//            endTime = endDateTime.timeInMillis
//        }

        val measure = MeasureCalc( CoreSpringApp.rootDirName, sd.objectID, true )

        for( sensorData in measure.alSensor ) {
            //!!! проверить/устранить причины зависания графиков
//            if( sensorData.minValue >= sensorData.maxValue ) continue
            if( sensorData.minTime == sensorData.maxTime  ) continue

            return GraphicActionResponse( begTime = sensorData.minTime, endTime = sensorData.maxTime )
        }

        //--- если данных нет совсем - берём сегодняшнее время и выравниваем к началу суток
        val toDayStart = getCurrentTimeInt() / 86400 * 86400
        return GraphicActionResponse( begTime = toDayStart, endTime = toDayStart + 86400 )
    }

    override fun doGetElements( graphicActionRequest: GraphicActionRequest): GraphicActionResponse {
        //long begTime = System.currentTimeMillis();

//        val fsgdc = GraphicDocumentConfig.hmConfig[ documentTypeName ] as FSGraphicDocumentConfig
//        val sensorType = fsgdc.sensorType
//        val agh = fsgdc.graphicHandler as AnalogGraphicHandler

        val graphicStartDataID = graphicActionRequest.startParamID
        val sd = chmSession[ AppParameter.GRAPHIC_START_DATA + graphicStartDataID ] as GraphicStartData

        val x1: Int = graphicActionRequest.graphicCoords!!.first
        val x2: Int = graphicActionRequest.graphicCoords!!.second
        val viewWidth = graphicActionRequest.viewSize!!.first
        val viewHeight = graphicActionRequest.viewSize!!.second

        val xScale = if( viewWidth == 0 ) 0 else ( x2 - x1 ) / ( viewWidth / DOT_PER_MM )

        val smPoint = userConfig.getUserProperty( UP_GRAPHIC_SHOW_POINT )
        val smLine = userConfig.getUserProperty( UP_GRAPHIC_SHOW_LINE )
        val smText = userConfig.getUserProperty( UP_GRAPHIC_SHOW_TEXT )

        //--- показ точек по умолчанию выключен, если не указано явно иное
        val isShowPoint = smPoint?.toBoolean() ?: false
        //--- показ линий по умолчанию включен, если не указано явно иное
        val isShowLine = smLine?.toBoolean() ?: true
        //--- показ текстов по умолчанию включен, если не указано явно иное
        val isShowText = smText?.toBoolean() ?: true

        val measure = MeasureCalc( CoreSpringApp.rootDirName, sd.objectID, true )

        val tmElement = TreeMap<String, GraphicElement>()
        val tmElementVisibleConfig = TreeMap<String, String>()

        for( sensorData in measure.alSensor ) {

            //!!! проверить/устранить причины зависания графиков
//            if( sensorData.minValue >= sensorData.maxValue ) continue
//            if( sensorData.alMeasureData.isEmpty() ) continue

            //--- заранее заполняем список опеределений видимости графиков
            val graphicVisibilityKey = "$UP_GRAPHIC_VISIBLE${sd.objectID}_${sensorData.typeID}"
            tmElementVisibleConfig[ sensorData.typeDescr ] = graphicVisibilityKey

            //--- а сейчас уже можно и нужно проверять на видимость графика
            val strGraphicVisible = userConfig.getUserProperty( graphicVisibilityKey )
            val isGraphicVisible = strGraphicVisible?.toBoolean() ?: true
            if( !isGraphicVisible ) continue

            val yScale = if( viewHeight == 0 ) 0.0 else ( sensorData.maxValue - sensorData.minValue ) / ( viewHeight / DOT_PER_MM )

            val alAxisYData = mutableListOf<AxisYData>()

            val aLine = if( isShowLine ) GraphicDataContainer( GraphicDataContainer.ElementType.LINE, 0, 3 ) else null

            alAxisYData.add( AxisYData( "${sensorData.typeDescr}, ${sensorData.dimDescr}", sensorData.minValue, sensorData.maxValue, GraphicColorIndex.AXIS_0 ) )

            var lastTime = 0
            var lastValue = 0.0 // начальное значение не важно, т.к. вначале всегда сработает lastTime
            for( i in 0 until sensorData.alMeasureData.size ) {
                val curTime = sensorData.alMeasureData[ i ].first

                if( curTime < x1 ) continue
                if( curTime > x2 ) break

                val curValue = sensorData.alMeasureData[ i ].second

                if( curTime - lastTime > xScale || Math.abs( curValue - lastValue ) > yScale ) { //|| curColorIndex != gldLast.colorIndex )
                    aLine?.alGLD?.add( GraphicLineData( curTime, curValue, GraphicColorIndex.LINE_NORMAL_0, null ) )

                    lastTime = curTime
                    lastValue = curValue
                }
            }

            val alGDC = mutableListOf( /*aText, aMinLimit, aMaxLimit, aPoint,*/ aLine )

            val ge = GraphicElement(
                graphicTitle = sensorData.typeDescr,
                alIndexColor = sdcAbstractGraphic.hmIndexColor.toList(),
                graphicHeight = -1.0,
                alAxisYData = alAxisYData,
                alGDC = alGDC.filterNotNull().filter { /*it != null &&*/ it.itNotEmpty() }
            )

            tmElement[ ge.graphicTitle ] = ge
        }

        //AdvancedLogger.debug(  "Graphic time [ ms ] = " + (  System.currentTimeMillis() - begTime  )  );
        //AdvancedLogger.debug(  "------------------------------------------------------------"  );

        return GraphicActionResponse(
            alElement = tmElement.toList(),
            alVisibleElement = tmElementVisibleConfig.toList()
        )
    }


}

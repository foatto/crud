package foatto.fs.core_fs.graphic

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.link.AppAction
import foatto.core_server.app.AppParameter
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cGraphicFS : cAbstractForm() {

    override fun isFormAutoClick() = true

    override fun getOkButtonIconName(): String = ICON_NAME_GRAPHIC

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if( returnURL != null ) return returnURL

        val msfd = model as mGraphicFS

        //--- выборка данных параметров для отчета
        val selectMeasure = ( hmColumnData[ msfd.columnMeasure ] as DataInt ).value

        val sd = GraphicStartData()
        sd.objectID = selectMeasure
        sd.rangeType = 0//( hmColumnData[ msfd.columnShowRangeType ] as DataRadioButton ).value

//        if( sd.rangeType == 0 ) {
//            sd.arrBegDT = intArrayOf( ( hmColumnData[ msfd.columnShowBegDate ] as DataDate ).year, ( hmColumnData[ msfd.columnShowBegDate ] as DataDate ).month,
//                                      ( hmColumnData[ msfd.columnShowBegDate ] as DataDate ).day, ( hmColumnData[ msfd.columnShowBegTime ] as DataTime ).hour,
//                                      ( hmColumnData[ msfd.columnShowBegTime ] as DataTime ).minute, ( hmColumnData[ msfd.columnShowBegTime ] as DataTime ).second )
//            sd.arrEndDT = intArrayOf( ( hmColumnData[ msfd.columnShowEndDate ] as DataDate ).year, ( hmColumnData[ msfd.columnShowEndDate ] as DataDate ).month,
//                                      ( hmColumnData[ msfd.columnShowEndDate ] as DataDate ).day, ( hmColumnData[ msfd.columnShowEndTime ] as DataTime ).hour,
//                                      ( hmColumnData[ msfd.columnShowEndTime ] as DataTime ).minute, ( hmColumnData[ msfd.columnShowEndTime ] as DataTime ).second )
//        }
//        //--- обработка динамических диапазонов
//        else {
            val endDT = ZonedDateTime.now(zoneId)
            val begDT = endDT.minus(sd.rangeType.toLong(), ChronoUnit.SECONDS)
            sd.begTime = begDT.toEpochSecond().toInt()
            sd.endTime = endDT.toEpochSecond().toInt()
//        }

        //--- заполнение текста заголовка информацией по объекту
//        val oc = ObjectConfig.getObjectConfig( dataWorker.alStm[ 0 ], userConfig, selectObject )
        sd.shortTitle = aliasConfig.descr
        sd.sbTitle = StringBuilder()
//        sd.sbTitle.append( oc.name )
//        if( !oc.model.isEmpty() ) sd.sbTitle.append( ", " ).append( oc.model )

        //--- заполнение текста заголовка информацией по периоду времени
        if( sd.rangeType != 0 ) {
            sd.sbTitle.append( " за последние " )
            if( sd.rangeType % 3600 == 0 ) sd.sbTitle.append( sd.rangeType / 3600 ).append( " час( а,ов ) " )
            else if( sd.rangeType % 60 == 0 ) sd.sbTitle.append( sd.rangeType / 60 ).append( " минут " )
            else sd.sbTitle.append( sd.rangeType ).append( " секунд " )
        }

        val paramID = getRandomInt()
        hmOut[ AppParameter.GRAPHIC_START_DATA + paramID ] = sd

        return getParamURL( aliasConfig.alias, AppAction.GRAPHIC, null, null, null, null, "&${AppParameter.GRAPHIC_START_DATA}=$paramID" )
    }
}


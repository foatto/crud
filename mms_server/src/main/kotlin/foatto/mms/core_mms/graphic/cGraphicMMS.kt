package foatto.mms.core_mms.graphic

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.DataTime3Int
import foatto.mms.iMMSApplication
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cGraphicMMS : cAbstractForm() {

    override fun getOkButtonIconName(): String = ICON_NAME_GRAPHIC

    override fun isFormAutoClick() =
        if( hmParentData[ "mms_day_work" ] != null || hmParentData[ "mms_work_shift" ] != null || hmParentData[ "mms_waybill" ] != null || hmParentData[ "mms_shift_work" ] != null )
            true
        else super.isFormAutoClick()

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if( returnURL != null ) return returnURL

        val msfd = model as mGraphicMMS

        //--- выборка данных параметров для отчета
        val selectObject = ( hmColumnData[ msfd.columnObject ] as DataInt ).value

        val sd = GraphicStartData()
        sd.objectID = selectObject
        sd.rangeType = ( hmColumnData[ msfd.columnShowRangeType ] as DataRadioButton ).value

        if( sd.rangeType == 0 ) {
            sd.begTime = ZonedDateTime.of(
                ( hmColumnData[ msfd.columnShowBegDate ] as DataDate3Int ).localDate,
                ( hmColumnData[ msfd.columnShowBegTime ] as DataTime3Int ).localTime,
                zoneId
            ).toEpochSecond().toInt()
            sd.endTime = ZonedDateTime.of(
                ( hmColumnData[ msfd.columnShowEndDate ] as DataDate3Int ).localDate,
                ( hmColumnData[ msfd.columnShowEndTime ] as DataTime3Int ).localTime,
                zoneId
            ).toEpochSecond().toInt()
        }
        //--- обработка динамических диапазонов
        else {
            val endDT = ZonedDateTime.now(zoneId)
            val begDT = endDT.minus(sd.rangeType.toLong(), ChronoUnit.SECONDS)
            sd.begTime = begDT.toEpochSecond().toInt()
            sd.endTime = endDT.toEpochSecond().toInt()
        }

        //--- заполнение текста заголовка информацией по объекту
        val oc = (application as iMMSApplication).getObjectConfig(userConfig, selectObject)
        sd.shortTitle = aliasConfig.descr
        sd.sbTitle = StringBuilder()
        sd.sbTitle.append( oc.name )
        if( oc.model.isNotEmpty() ) sd.sbTitle.append( ", " ).append( oc.model )

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


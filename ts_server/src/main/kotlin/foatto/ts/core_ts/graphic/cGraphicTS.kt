package foatto.ts.core_ts.graphic

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
import foatto.ts.iTSApplication
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cGraphicTS : cAbstractForm() {

    override fun getOkButtonIconName(): String = ICON_NAME_GRAPHIC

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val msfd = model as mGraphicTS

        //--- выборка данных параметров для отчета
        val selectObject = (hmColumnData[msfd.columnObject] as DataInt).intValue

        val sd = GraphicStartData()
        sd.objectId = selectObject
        sd.rangeType = (hmColumnData[msfd.columnShowRangeType] as DataRadioButton).intValue

        if (sd.rangeType == 0) {
            sd.begTime = ZonedDateTime.of(
                (hmColumnData[msfd.columnShowBegDate] as DataDate3Int).localDate,
                (hmColumnData[msfd.columnShowBegTime] as DataTime3Int).localTime,
                zoneId
            ).toEpochSecond().toInt()
            sd.endTime = ZonedDateTime.of(
                (hmColumnData[msfd.columnShowEndDate] as DataDate3Int).localDate,
                (hmColumnData[msfd.columnShowEndTime] as DataTime3Int).localTime,
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
        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObject)

        sd.shortTitle = aliasConfig.descr + "\n${oc.name}"
        if (oc.model.isNotEmpty()) {
            sd.shortTitle += ", ${oc.model}"
        }

        sd.fullTitle = oc.name
        if (oc.model.isNotEmpty()) {
            sd.fullTitle += "\nМодель: ${oc.model}"
        }

        //--- fill header text by info of time period
//--- commented at the request of the customer
//        if (sd.rangeType != 0) {
//            sd.title += " за последние " +
//                if (sd.rangeType % 3600 == 0) {
//                    "${sd.rangeType / 3600} час(а,ов) "
//                } else if (sd.rangeType % 60 == 0) {
//                    "${sd.rangeType / 60} минут "
//                } else {
//                    "${sd.rangeType} секунд "
//                }
//        }

        val paramID = getRandomInt()
        hmOut[AppParameter.GRAPHIC_START_DATA + paramID] = sd

        return getParamURL(aliasConfig.alias, AppAction.GRAPHIC, null, null, null, null, "&${AppParameter.GRAPHIC_START_DATA}=$paramID")
    }
}


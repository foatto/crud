package foatto.ts.core_ts.custom

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.custom.server.CustomStartData
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.ts.iTSApplication
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cCustomTS : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_GRAPHIC

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val msfd = model as mCustomTS

        val selectObjectId = (hmColumnData[msfd.columnObject] as DataInt).intValue

        val csd = CustomStartData()
        csd.objectId = selectObjectId
        csd.rangeType = (hmColumnData[msfd.columnShowRangeType] as DataRadioButton).intValue

//        if (sd.rangeType == 0) {
//            sd.begTime = ZonedDateTime.of(
//                (hmColumnData[msfd.columnShowBegDate] as DataDate3Int).localDate,
//                (hmColumnData[msfd.columnShowBegTime] as DataTime3Int).localTime,
//                zoneId
//            ).toEpochSecond().toInt()
//            sd.endTime = ZonedDateTime.of(
//                (hmColumnData[msfd.columnShowEndDate] as DataDate3Int).localDate,
//                (hmColumnData[msfd.columnShowEndTime] as DataTime3Int).localTime,
//                zoneId
//            ).toEpochSecond().toInt()
//        }
//        //--- обработка динамических диапазонов
//        else {
        val endDT = ZonedDateTime.now(zoneId)
        val begDT = endDT.minus(csd.rangeType.toLong(), ChronoUnit.SECONDS)
        csd.begTime = begDT.toEpochSecond().toInt()
        csd.endTime = endDT.toEpochSecond().toInt()
//        }

        //--- заполнение текста заголовка информацией по объекту
        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObjectId)
        csd.shortTitle = aliasConfig.descr
        csd.title = oc.name
        if (oc.model.isNotEmpty()) {
            csd.title += ", ${oc.model}"
        }

        //--- заполнение текста заголовка информацией по периоду времени
        if (csd.rangeType != 0) {
            csd.title += " за последние " +
                if (csd.rangeType % 3600 == 0) {
                    "${csd.rangeType / 3600} час(а,ов) "
                } else if (csd.rangeType % 60 == 0) {
                    "${csd.rangeType / 60} минут "
                } else {
                    "${csd.rangeType} секунд "
                }
        }

        //--- XY-part

        val xysd = XyStartData()
        xysd.shortTitle = csd.shortTitle
        xysd.title = csd.title
        xysd.alStartObjectData.add(XyStartObjectData(csd.objectId, "ts_object", true, false, true))

        val xyParamId = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + xyParamId] = xysd

        //--- Graphic-part

        val grsd = GraphicStartData()
        grsd.objectId = csd.objectId
        grsd.rangeType = csd.rangeType
        grsd.begTime = csd.begTime
        grsd.endTime = csd.endTime
        grsd.shortTitle = csd.shortTitle
        grsd.title = csd.title

        val grParamId = getRandomInt()
        hmOut[AppParameter.GRAPHIC_START_DATA + grParamId] = grsd

        //--- Custom end

        csd.xyStartDataId = xyParamId.toString()
        csd.graphicStartDataId = grParamId.toString()

        val customParamID = getRandomInt()
        hmOut[AppParameter.CUSTOM_START_DATA + customParamID] = csd

        return getParamURL(aliasConfig.alias, AppAction.CUSTOM, null, null, null, null, "&${AppParameter.CUSTOM_START_DATA}=$customParamID")
    }
}

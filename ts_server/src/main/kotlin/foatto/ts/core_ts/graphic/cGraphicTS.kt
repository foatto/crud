package foatto.ts.core_ts.graphic

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.ChartStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.server.data.DataTime3Int
import foatto.ts.iTSApplication
import java.time.ZonedDateTime

class cGraphicTS : cAbstractForm() {

    override fun getOkButtonIconName(): String = ICON_NAME_GRAPHIC

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val msfd = model as mGraphicTS

        //--- выборка данных параметров для отчета
        val selectObject = (hmColumnData[msfd.columnObject] as DataInt).intValue

        val sd = ChartStartData()
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

        //--- заполнение текста заголовка информацией по объекту
        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObject)

        sd.title = aliasConfig.descr + "\n${oc.name}"
        if (oc.model.isNotEmpty()) {
            sd.title += ", ${oc.model}"
        }

        sd.tooltip = oc.name
        if (oc.model.isNotEmpty()) {
            sd.tooltip += "\nМодель: ${oc.model}"
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
        hmOut[AppParameter.CHART_START_DATA + paramID] = sd

        return getParamURL(aliasConfig.name, AppAction.GRAPHIC, null, null, null, null, "&${AppParameter.CHART_START_DATA}=$paramID")
    }
}


package foatto.ts.core_ts.composite

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.link.XyServerActionButton
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.composite.server.CompositeStartData
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.ts.iTSApplication
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class cCompositeTS : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_GRAPHIC

    override fun isFormAutoClick() = if (getParentId("ts_object") != null) {
        true
    } else {
        super.isFormAutoClick()
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val msfd = model as mCompositeTS

        val selectObjectId = (hmColumnData[msfd.columnObject] as DataInt).intValue

        val csd = CompositeStartData()
        csd.objectId = selectObjectId
        csd.rangeType = (hmColumnData[msfd.columnShowRangeType] as DataRadioButton).intValue

        //--- заполнение текста заголовка информацией по объекту
        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObjectId)

        csd.shortTitle = aliasConfig.descr + "\n${oc.name}"
        if (oc.model.isNotEmpty()) {
            csd.shortTitle += ", ${oc.model}"
        }

        csd.fullTitle = oc.name
        if (oc.model.isNotEmpty()) {
            csd.fullTitle += "\nМодель: ${oc.model}"
        }

        hmAliasConfig["ts_setup"]?.let { setupAliasConfig ->
            csd.alServerActionButton +=
                XyServerActionButton(
                    caption = setupAliasConfig.descr.replace(' ', '\n'),
                    tooltip = setupAliasConfig.descr,
                    icon = "",
                    url = getParamURL(
                        aAlias = "ts_setup",
                        aAction = AppAction.FORM,
                        aRefererId = null,
                        aId = 0,
                        aParentData = hmParentData.apply {
                            put("ts_object", selectObjectId)
                        },
                        aParentUserId = null,
                        aAltParams = ""
                    ),
                    isForWideScreenOnly = true,
                )
        }

        //--- fill header text by info of time period
//--- commented at the request of the customer
//        if (csd.rangeType != 0) {
//            csd.title += " за последние " +
//                if (csd.rangeType % 3600 == 0) {
//                    "${csd.rangeType / 3600} час(а,ов) "
//                } else if (csd.rangeType % 60 == 0) {
//                    "${csd.rangeType / 60} минут "
//                } else {
//                    "${csd.rangeType} секунд "
//                }
//        }

        //--- XY-part

        val xysd = XyStartData()
        xysd.shortTitle = csd.shortTitle
        xysd.fullTitle = csd.fullTitle
        xysd.alStartObjectData +=
            XyStartObjectData(
                objectId = csd.objectId,
                typeName = "ts_object",
                isStart = true,
                isTimed = false,
                isReadOnly = true
            )

        val xyParamId = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + xyParamId] = xysd

        //--- Graphic-part

        val grsd = GraphicStartData()
        grsd.objectId = csd.objectId
        grsd.rangeType = csd.rangeType
        //--- на композите показываем N времени назад
        //grsd.begTime = csd.begTime
        //grsd.endTime = csd.endTime
        grsd.shortTitle = csd.shortTitle
        grsd.fullTitle = csd.fullTitle

        val grParamId = getRandomInt()
        hmOut[AppParameter.GRAPHIC_START_DATA + grParamId] = grsd

        //--- Composite end

        csd.xyStartDataId = xyParamId.toString()
        csd.graphicStartDataId = grParamId.toString()

        val compositeParamID = getRandomInt()
        hmOut[AppParameter.COMPOSITE_START_DATA + compositeParamID] = csd

        return getParamURL(
            aAlias = aliasConfig.name,
            aAction = AppAction.COMPOSITE,
            aRefererId = null,
            aId = null,
            aParentData = null,
            aParentUserId = null,
            aAltParams = "&${AppParameter.COMPOSITE_START_DATA}=$compositeParamID"
        )
    }
}

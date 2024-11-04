package foatto.ts.core_ts.composite

import foatto.core.app.ICON_NAME_GRAPHIC
import foatto.core.app.composite.CompositeBlockDTO
import foatto.core.app.composite.CompositeResponse
import foatto.core.link.AppAction
import foatto.core.link.ChartResponse
import foatto.core.link.FormData
import foatto.core.link.ServerActionButton
import foatto.core.link.XyResponse
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.ChartStartData
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataRadioButton
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.spring.CoreSpringApp
import foatto.ts.iTSApplication

class cCompositeTS : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_GRAPHIC

    override fun isFormAutoClick() = if (getParentId("ts_object") != null) {
        true
    } else {
        super.isFormAutoClick()
    }

    override fun doSave(action: String, hmFormData: Map<String, FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, hmFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val msfd = model as mCompositeTS

        val selectObjectId = (hmColumnData[msfd.columnObject] as DataInt).intValue
        val rangeType = (hmColumnData[msfd.columnShowRangeType] as DataRadioButton).intValue

        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObjectId)

        var title = aliasConfig.descr + "\n${oc.name}"
        if (oc.model.isNotEmpty()) {
            title += ", ${oc.model}"
        }

        var tooltip = oc.name
        if (oc.model.isNotEmpty()) {
            tooltip += "\nМодель: ${oc.model}"
        }

        val alServerActionButton = mutableListOf<ServerActionButton>()
        hmAliasConfig["ts_setup"]?.let { setupAliasConfig ->
            alServerActionButton +=
                ServerActionButton(
                    icon = "",
                    text = setupAliasConfig.descr.replace(' ', '\n'),
                    tooltip = setupAliasConfig.descr,
                    action = getParamURL(
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
                    inNewWindow = true,
                    isForWideScreenOnly = true,
                )
        }

        //--- XY-part

        val xysd = XyStartData(
            title = title,
            tooltip = tooltip,
            rangeType = rangeType,
            alStartObjectData = mutableListOf(
                XyStartObjectData(
                    objectId = selectObjectId,
                    typeName = "ts_object",
                    isStart = true,
                    isTimed = false,
                    isReadOnly = true
                ),
            ),
        )

        val xyParamId = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + xyParamId] = xysd

        //--- Chart-part

        val grsd = ChartStartData()
        grsd.objectId = selectObjectId
        grsd.rangeType = rangeType
        //--- на композите показываем N времени назад
        //grsd.begTime = csd.begTime
        //grsd.endTime = csd.endTime
        grsd.title = title
        grsd.tooltip = tooltip

        val grParamId = getRandomInt()
        hmOut[AppParameter.CHART_START_DATA + grParamId] = grsd

        val compositeResponseId = getRandomInt()
        hmOut[AppParameter.COMPOSITE_RESPONSE_ID + compositeResponseId] = CompositeResponse(
            title = title,
            tooltip = tooltip,
            alServerActionButton = alServerActionButton,
            cols = 1,
            rows = 2,
            blocks = listOf(
                CompositeBlockDTO(
                    x = 0,
                    y = 0,
                    w = 1,
                    h = 1,
                    xyResponse = XyResponse(
                        documentConfig = CoreSpringApp.hmXyDocumentConfig["ts_state"]!!,
                        startParamId = xyParamId.toString(),
                        title = title,
                        tooltip = tooltip,
                        alServerActionButton = null,
                    ),
                ),
                CompositeBlockDTO(
                    x = 0,
                    y = 1,
                    w = 1,
                    h = 1,
                    chartResponse = ChartResponse(
                        documentTypeName = "ts_graphic_dsltt",
                        startParamId = grParamId.toString(),
                        title = title,
                        tooltip = tooltip,
                        rangeType = rangeType,
                        begTime = 0,
                        endTime = 0,
                    ),
                ),
//                CompositeBlockDTO(
//                    x = 0,
//                    y = 2,
//                    w = 1,
//                    h = 1,
//                    chartResponse = ChartResponse(
//                        documentTypeName = "ts_graphic_tt",
//                        startParamId = grParamId.toString(),
//                        title = title,
//                        tooltip = tooltip,
//                        rangeType = rangeType,
//                        begTime = 0,
//                        endTime = 0,
//                    ),
//                ),
            ),
        )

        return getParamURL(
            aAlias = aliasConfig.name,  // не имеет значения
            aAction = AppAction.COMPOSITE,
            aRefererId = null,
            aId = null,
            aParentData = null,
            aParentUserId = null,
            aAltParams = "&${AppParameter.COMPOSITE_RESPONSE_ID}=$compositeResponseId"
        )
    }
}

package foatto.ts.core_ts.xy

import foatto.core.app.ICON_NAME_STATE
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.ts.iTSApplication

class cShowState : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_STATE

    override fun isFormAutoClick() = if (hmParentData["ts_object"] != null) {
        true
    } else {
        super.isFormAutoClick()
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val mss = model as mShowState

        val selectObjectId = (hmColumnData[mss.columnObject] as DataInt).intValue

        val oc = (application as iTSApplication).getObjectConfig(userConfig, selectObjectId)

        val sd = XyStartData()

        sd.shortTitle = aliasConfig.descr
        sd.sbTitle = StringBuilder(oc.name)
        if (oc.model.isNotEmpty()) {
            sd.sbTitle.append(", ").append(oc.model)
        }

        sd.alStartObjectData.add(XyStartObjectData(selectObjectId, "ts_object", true, false, true))

        val paramID = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + paramID] = sd

        //--- для команды XY alias обозначает xy_document_type_name
        //--- в отличие от графиков на картах/схемах может быть несколько классов-запускателей
        //--- (траектории объектов, зоны, схема оборудования/датчиков и т.п.)
        return getParamURL("ts_state", AppAction.XY, null, null, null, null, "&${AppParameter.XY_START_DATA}=$paramID")
    }

}

package foatto.mms.core_mms.xy

import foatto.core.app.ICON_NAME_MAP
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.mms.core_mms.ZoneData

class cShowZone : cAbstractForm() {

    override fun getOkButtonIconName() = ICON_NAME_MAP

    override fun isFormAutoClick(): Boolean = true

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val msz = model as mShowZone

        //--- выборка данных параметров для показа
        val showZoneID = (hmColumnData[msz.columnZone] as DataInt).intValue

        val hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)
        val zoneData = hmZoneData[showZoneID]!!

        val sd = XyStartData()
        sd.shortTitle = aliasConfig.descr
        sd.title = "Геозона: ${zoneData.name} [${zoneData.descr}]"
        //--- информация по зоне
        sd.alStartObjectData.add(XyStartObjectData(showZoneID, "mms_zone", true, false, zoneData.hmUserRO[userConfig.userId]!!))

        val hmAllZone = mutableMapOf<Int, Boolean>()
        for ((zoneID, zd) in hmZoneData) {
            hmAllZone[zoneID] = zd.hmUserRO[userConfig.userId]!!
        }

        for (zoneID in hmAllZone.keys) {
            if (zoneID != showZoneID) {
                sd.alStartObjectData.add(XyStartObjectData(zoneID, "mms_zone", false, false, hmAllZone[zoneID]!!))
            }
        }

        val paramID = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + paramID] = sd

        //--- для команды XY alias обозначает xy_document_type_name
        //--- в отличие от графиков на картах может быть несколько классов-запускателей
        //--- (траектории объектов, зоны, т.п.), но тип документа-карты пока только один
        return getParamURL("mms_map", AppAction.XY, null, null, null, null, "&${AppParameter.XY_START_DATA}=$paramID")
    }
}

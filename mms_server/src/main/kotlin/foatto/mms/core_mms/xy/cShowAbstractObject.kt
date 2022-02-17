package foatto.mms.core_mms.xy

import foatto.core.app.ICON_NAME_MAP
import foatto.core.link.AppAction
import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getRandomInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.server.cAbstractForm
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.mms.core_mms.ZoneData
import foatto.mms.core_mms.ZoneLimitData
import foatto.mms.iMMSApplication

abstract class cShowAbstractObject : cAbstractForm() {

    //--- нельзя в enum, т.к. это значения из combo-box'a
    companion object {
        const val ZONE_SHOW_NONE = 0    // не показывать никаких зон
        const val ZONE_SHOW_ACTUAL = 1    // показывать только актуальные/используемые
        const val ZONE_SHOW_ALL = 2    // показывать все зоны
    }

    //    protected int waybillID = 0;    // для выбора маршрутов и зон по конкретному маршруту

    protected var showZoneType = ZONE_SHOW_NONE

    override fun getOkButtonIconName() = ICON_NAME_MAP

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val sd = loadShowParam()

        return getShowURL(hmOut, sd)
    }

    protected abstract fun loadShowParam(): XyStartData

    protected fun getShowURL(hmOut: MutableMap<String, Any>, sd: XyStartData): String {

        sd.shortTitle = aliasConfig.descr
        sd.fullTitle = ""

        //--- заполнение стартовой/фильтровой информации по объектам
        for (objectIndex in sd.alStartObjectData.indices) {
            val sod = sd.alStartObjectData[objectIndex]
            sod.typeName = "mms_object"
            sod.isStart = true
            sod.isTimed = true

            //--- заполнение текста заголовка информацией по а/м
            if (objectIndex > 0) {
                sd.fullTitle += ", "
            }
            val oc = (application as iMMSApplication).getObjectConfig(userConfig, sod.objectId)
            sd.fullTitle += oc.name
            if (oc.model.isNotEmpty()) {
                sd.fullTitle += ", ${oc.model}"
            }
        }

        //--- заполнение текста заголовка информацией по периоду времени
        if (sd.rangeType == -1) {
            sd.fullTitle += " за период с ${DateTime_DMYHMS(zoneId, sd.begTime)} по ${DateTime_DMYHMS(zoneId, sd.endTime)}"
        } else if (sd.rangeType > 0) {
            sd.fullTitle += " за последние " +
                if (sd.rangeType % 3600 == 0) {
                    "${sd.rangeType / 3600} час(а,ов) "
                } else if (sd.rangeType % 60 == 0) {
                    "${sd.rangeType / 60} минут "
                } else {
                    "${sd.rangeType} секунд "
                }
        }

        //--- заполнение фильтровой информации по зонам
        var hmZoneData: Map<Int, ZoneData>? = null
        val hmActualZone = mutableMapOf<Int, Boolean>()
        val hmAllZone = mutableMapOf<Int, Boolean>()
        if (showZoneType != ZONE_SHOW_NONE) {
            hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)
            for (objectIndex in sd.alStartObjectData.indices) {
                val objectId = sd.alStartObjectData[objectIndex].objectId
                //--- загрузить список зон, непосредственно-привязанных и привязанных через путевые листы - маршруты
                val hmZoneLimit = ZoneLimitData.getZoneLimit(
                    stm = stm,
                    userConfig = userConfig,
                    objectConfig = (application as iMMSApplication).getObjectConfig(userConfig, objectId),
                    hmZoneData = hmZoneData,
                    zoneType = 0
                )
                for (alZoneLimit in hmZoneLimit.values) {
                    for (zld in alZoneLimit) {
                        hmActualZone[zld.zoneData!!.id] = zld.zoneData!!.hmUserRO[userConfig.userId]!!
                    }
                }
            }
        }
        //--- загрузить список всех доступных по правам доступа зон
        if (showZoneType == ZONE_SHOW_ALL) {
            for (zoneID in hmZoneData!!.keys) {
                hmAllZone[zoneID] = hmZoneData[zoneID]!!.hmUserRO[userConfig.userId]!!
            }
        }

        for (zoneID in hmActualZone.keys) {
            sd.alStartObjectData.add(
                XyStartObjectData(
                    objectId = zoneID,
                    typeName = "mms_zone",
                    isStart = true,
                    isTimed = false,
                    isReadOnly = hmActualZone[zoneID]!!
                )
            )
        }
        for (zoneID in hmAllZone.keys) {
            if (!hmActualZone.containsKey(zoneID)) {
                sd.alStartObjectData.add(
                    XyStartObjectData(
                        objectId = zoneID,
                        typeName = "mms_zone",
                        isStart = false,
                        isTimed = false,
                        isReadOnly = hmAllZone[zoneID]!!
                    )
                )
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

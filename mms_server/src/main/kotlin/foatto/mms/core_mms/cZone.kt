package foatto.mms.core_mms

import foatto.core.app.ICON_NAME_ADD_ITEM
import foatto.core.link.AddActionButton
import foatto.core.link.AppAction
import foatto.core_server.app.AppParameter
import foatto.core.util.getRandomInt
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.app.xy.XyStartData
import foatto.core_server.app.xy.XyStartObjectData
import foatto.core_server.app.xy.server.XyProperty
import foatto.mms.core_mms.xy.server.document.sdcMMSMap
import foatto.mms.core_mms.xy.server.document.sdcMMSMap.Companion.getZoneTooltip

class cZone : cStandart() {

    override fun getNextID(hmColumnData: Map<iColumn, iData>) = stm.getNextID(arrayOf("MMS_object", "MMS_zone"), arrayOf("id", "id"))

    override fun getAddButtonURL(refererID: String, hmOut: MutableMap<String, Any>): MutableList<AddActionButton> {
        val alAddButtonList = super.getAddButtonURL(refererID, hmOut)

        val sd = XyStartData()

        //--- загрузить список всех доступных по правам доступа зон
        val hmZoneData = ZoneData.getZoneData(stm, userConfig, 0)

        for(zoneID in hmZoneData.keys) sd.alStartObjectData.add(XyStartObjectData(zoneID, "mms_zone", false, false, hmZoneData[zoneID]!!.hmUserRO[userConfig.userId]!!))

        sd.shortTitle = "Создание геозоны"
        sd.sbTitle = StringBuilder("Создание геозоны")

        val paramID = getRandomInt()
        hmOut[AppParameter.XY_START_DATA + paramID] = sd

        //--- для команды XY alias обозначает xy_document_type_name
        alAddButtonList.add(AddActionButton(
                caption = "Добавить",
                tooltip = "Добавить",
                icon = ICON_NAME_ADD_ITEM,
                url = getParamURL("mms_map", AppAction.XY, null, null, null, null, StringBuilder().append('&').append(AppParameter.XY_START_DATA).append('=').append(paramID).toString())
        ))

        return alAddButtonList
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val mz = model as mZone
        updateZoneInfo(
            id, getZoneTooltip(
                (hmColumnData[mz.columnZoneName] as DataString).text, (hmColumnData[mz.columnZoneDescr] as DataString).text
            )
        )
        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)

        stm.executeUpdate( " DELETE FROM XY_point WHERE element_id IN ( SELECT id FROM XY_element WHERE object_id = $id ) " )
        stm.executeUpdate( " DELETE FROM XY_property WHERE element_id IN ( SELECT id FROM XY_element WHERE object_id = $id ) ")
        stm.executeUpdate( " DELETE FROM XY_element WHERE object_id = $id " )
    }

    private fun updateZoneInfo(id: Int, zoneInfo: String) {
        var elementId = 0

        //--- найдем соответствующий elementId
        val rs = stm.executeQuery(
            " SELECT id FROM XY_element " +
            " WHERE type_name = '${sdcMMSMap.ELEMENT_TYPE_ZONE}' AND object_id = $id " )
        if(rs.next()) elementId = rs.getInt(1)
        rs.close()
        //--- графическая зона существует, надо обновить информацию
        if(elementId != 0) {
            if(stm.executeUpdate(
                " UPDATE XY_property SET property_value = '$zoneInfo' WHERE element_id = $elementId AND property_name = '${XyProperty.TOOL_TIP_TEXT}' " ) == 0
            ) {

                stm.executeUpdate(
                    " INSERT INTO XY_property ( element_id , property_name , property_value ) VALUES ( " +
                    " $elementId , '${XyProperty.TOOL_TIP_TEXT}' , '$zoneInfo' ) " )
            }
        }
    }
}

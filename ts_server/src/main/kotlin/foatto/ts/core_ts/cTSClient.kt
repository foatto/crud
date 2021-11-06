package foatto.ts.core_ts

import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.iData
import foatto.core_server.app.system.cClient
import foatto.core_server.app.system.cUser
import foatto.ts.iTSApplication

class cTSClient : cClient() {

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateColumnDataAfterFilter(hmColumnData)

        val mc = model as mTSClient

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved((hmColumnData[mc.columnID] as DataInt).intValue)
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.generateFormColumnData(id, hmColumnData)

        val mc = model as mTSClient

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved((hmColumnData[mc.columnID] as DataInt).intValue)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        //--- автосоздание привязки пользователя/клиента и роли управления приборами
        changeControlEnabledRoleLink(model as mTSClient, id, hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name
        cUser.refreshUserConfig(conn, userConfig.userId, hmOut)

        //--- обновление привязки пользователя/клиента и роли управления приборами
        changeControlEnabledRoleLink(model as mTSClient, id, hmColumnData)

        return postURL
    }

    private fun controlEnableSaved(id: Int): Boolean {
        val controlRoleId = (application as iTSApplication).controlEnabledRoleId

        val stm = conn.createStatement()
        val rs = stm.executeQuery(
            """
                SELECT id FROM SYSTEM_user_role 
                WHERE role_id = $controlRoleId  
                AND user_id = $id
            """
        )
        val result = rs.next()
        rs.close()
        stm.close()

        return result
    }

    private fun changeControlEnabledRoleLink(mc: mTSClient, id: Int, hmColumnData: Map<iColumn, iData>) {
        val controlRoleId = (application as iTSApplication).controlEnabledRoleId
        val isControlEnabled = (hmColumnData[mc.columnControlEnabled] as DataBoolean).value

        if (isControlEnabled) {
            if (!controlEnableSaved(id)) {
                val nextId = stm.getNextIntId("SYSTEM_user_role", "id")
                stm.executeUpdate(
                    """
                        INSERT INTO SYSTEM_user_role( id , role_id , user_id ) 
                        VALUES ( $nextId , $controlRoleId , $id )
                    """
                )
            }
        } else {
            stm.executeUpdate(
                """
                    DELETE FROM SYSTEM_user_role
                    WHERE role_id = $controlRoleId  
                    AND user_id = $id
                """
            )
        }
    }
}
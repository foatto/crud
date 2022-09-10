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
    override fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateTableColumnDataAfterFilter(hmColumnData)

        val mc = model as mTSClient

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved((hmColumnData[mc.columnId] as DataInt).intValue)
    }

    override fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.getCalculatedFormColumnData(id, hmColumnData)

        val mc = model as mTSClient

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved(id)
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
        cUser.refreshUserConfig(application, conn, userConfig.userId, hmOut)

        //--- обновление привязки пользователя/клиента и роли управления приборами
        changeControlEnabledRoleLink(model as mTSClient, id, hmColumnData)

        return postURL
    }

    private fun controlEnableSaved(id: Int): Boolean {
        val controlRoleId = (application as iTSApplication).controlEnabledRoleId

        val rs = conn.executeQuery(
            """
                SELECT id FROM SYSTEM_user_role 
                WHERE role_id = $controlRoleId  
                AND user_id = $id
            """
        )
        val result = rs.next()
        rs.close()

        return result
    }

    private fun changeControlEnabledRoleLink(mc: mTSClient, id: Int, hmColumnData: Map<iColumn, iData>) {
        val controlRoleId = (application as iTSApplication).controlEnabledRoleId
        val isControlEnabled = (hmColumnData[mc.columnControlEnabled] as DataBoolean).value

        if (isControlEnabled) {
            if (!controlEnableSaved(id)) {
                val nextId = conn.getNextIntId("SYSTEM_user_role", "id")
                conn.executeUpdate(
                    """
                        INSERT INTO SYSTEM_user_role( id , role_id , user_id ) 
                        VALUES ( $nextId , $controlRoleId , $id )
                    """
                )
            }
        } else {
            conn.executeUpdate(
                """
                    DELETE FROM SYSTEM_user_role
                    WHERE role_id = $controlRoleId  
                    AND user_id = $id
                """
            )
        }
    }
}
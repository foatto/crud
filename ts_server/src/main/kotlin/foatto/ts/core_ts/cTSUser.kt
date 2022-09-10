package foatto.ts.core_ts

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.app.system.cUser
import foatto.ts.iTSApplication

class cTSUser : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val tn = renameTableName(hsTableRenameList, model.modelTableName)
        val mc = model as mTSUser
        val companiesParentId = (application as iTSApplication).companiesParentId.toIntOrNull() ?: 0

        return super.addSQLWhere(hsTableRenameList) +
            """    
                AND $tn.${mc.columnId.getFieldName()} > 0 
                AND $tn.${mc.columnRecordType.getFieldName()} = ${OrgType.ORG_TYPE_BOSS} 
            """ +
            if (getParentId("ts_company") == null) {
                """
                    AND $tn.${mc.columnParent.getFieldName()} IN (
                        SELECT id FROM SYSTEM_users WHERE parent_id = $companiesParentId 
                    )
                """
            } else {
                ""
            }
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateTableColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        super.generateTableColumnDataAfterFilter(hmColumnData)

        val mc = model as mTSUser

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved((hmColumnData[mc.columnId] as DataInt).intValue)
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mc = model as mTSUser

        if (column == mc.columnRecordFullName) {
            val isDisabled = (hmColumnData[mc.columnDisabled] as DataBoolean).value
            val lastLogonTime = (hmColumnData[mc.columnUserLastLoginAttemptDate] as DataDate3Int).localDate.atStartOfDay(zoneId).toEpochSecond().toInt()

            //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = cUser.getUserNameColor(isDisabled, lastLogonTime)
        }
    }

    override fun getCalculatedFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        super.getCalculatedFormColumnData(id, hmColumnData)

        val mc = model as mTSUser

        (hmColumnData[mc.columnControlEnabled] as DataBoolean).value = controlEnableSaved(id)
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        cUser.checkAndSetNewPassword(conn, id, hmColumnData[(model as mTSUser).columnUserPassword] as? DataString)
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        cUser.refreshUserConfig(application, conn, userConfig.userId, hmOut)

        //--- создать запись индивидуального сдвига часового пояса
        cUser.addTimeZone(conn, id)

        //--- автосоздание привязки пользователя/клиента и его типовых ролей
        (application as iTSApplication).alTSUserRoleId.forEach { clientRoleId ->
            val nextId = conn.getNextIntId("SYSTEM_user_role", "id")
            conn.executeUpdate(
                """
                    INSERT INTO SYSTEM_user_role( id , role_id , user_id ) 
                    VALUES ( $nextId , $clientRoleId , $id )
                """
            )
        }

        //--- автосоздание привязки пользователя/клиента и роли управления приборами
        changeControlEnabledRoleLink(model as mTSUser, id, hmColumnData)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name
        cUser.refreshUserConfig(application, conn, userConfig.userId, hmOut)

        //--- обновление привязки пользователя/клиента и роли управления приборами
        changeControlEnabledRoleLink(model as mTSUser, id, hmColumnData)

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

    private fun changeControlEnabledRoleLink(mc: mTSUser, id: Int, hmColumnData: Map<iColumn, iData>) {
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

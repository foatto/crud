package foatto.core_server.app.system

import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData

open class cClient : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val tn = renameTableName(hsTableRenameList, model.modelTableName)
        val mc = model as mClient

        return super.addSQLWhere(hsTableRenameList) +
            """    
                AND $tn.${mc.columnId.getFieldName()} > 0 
                AND $tn.${mc.columnRecordType.getFieldName()} = ${OrgType.ORG_TYPE_WORKER} 
                AND $tn.${mc.columnParent.getFieldName()} = ${mc.getClientParentId(application, aliasConfig.alias)} 
            """
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mc = model as mClient
        if (column == mc.columnRecordFullName) {
            val isDisabled = (hmColumnData[mc.columnDisabled] as DataBoolean).value
            val lastLogonTime = (hmColumnData[mc.columnUserLastLoginAttemptDate] as DataDate3Int).localDate.atStartOfDay(zoneId).toEpochSecond().toInt()

            //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = cUser.getUserNameColor(isDisabled, lastLogonTime)
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        cUser.checkAndSetNewPassword(stm, id, hmColumnData[(model as mClient).columnUserPassword] as? DataString)
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val mc = model as mClient

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        cUser.refreshUserConfig(conn, userConfig.userId, hmOut)

        //--- создать запись индивидуального сдвига часового пояса
        cUser.addTimeZone(stm, id)

        //--- автосоздание привязки пользователя/клиента и его типовых ролей
        mc.getClientRoleIds(application, aliasConfig.alias).forEach { clientRoleId ->
            val nextId = stm.getNextIntId("SYSTEM_user_role", "id")
            stm.executeUpdate(
                """
                    INSERT INTO SYSTEM_user_role( id , role_id , user_id ) 
                    VALUES ( $nextId , $clientRoleId , $id )
                """
            )
        }

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name
        cUser.refreshUserConfig(conn, userConfig.userId, hmOut)

        return postURL
    }
}
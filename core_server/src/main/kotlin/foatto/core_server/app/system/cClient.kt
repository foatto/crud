package foatto.core_server.app.system

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import java.util.*

open class cClient : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val tn = renameTableName(hsTableRenameList, model.modelTableName)
        val mc = model as mClient

        return super.addSQLWhere(hsTableRenameList) +
            """    
                AND $tn.${mc.columnId.getFieldName()} > 0 
                AND $tn.${mc.columnRecordType.getFieldName()} = ${OrgType.ORG_TYPE_WORKER} 
                AND $tn.${mc.columnParent.getFieldName()} = ${mc.getClientParentId(application, aliasConfig.name)} 
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
            tci.foreColor = application.getUserNameColor(isDisabled, lastLogonTime)
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        application.checkAndSetNewPassword(conn, id, hmColumnData[(model as mClient).columnUserPassword] as? DataString)
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val mc = model as mClient

        //--- создать запись индивидуального сдвига часового пояса
        application.saveUserProperty(
            conn = conn,
            userId = id,
            userConfig = null,
            upName = UP_TIME_OFFSET,
            upValue = (TimeZone.getDefault().rawOffset / 1000).toString()
        )

        //--- автосоздание привязки пользователя/клиента и его типовых ролей
        mc.getClientRoleIds(application, aliasConfig.name).forEach { clientRoleId ->
            val nextId = conn.getNextIntId("SYSTEM_user_role", "id")
            conn.executeUpdate(
                """
                    INSERT INTO SYSTEM_user_role( id , role_id , user_id ) 
                    VALUES ( $nextId , $clientRoleId , $id )
                """
            )
        }

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        hmOut[iApplication.USER_CONFIG] = application.getUserConfig(conn, userConfig.userId)

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        hmOut[iApplication.USER_CONFIG] = application.getUserConfig(conn, userConfig.userId)

        return postURL
    }
}
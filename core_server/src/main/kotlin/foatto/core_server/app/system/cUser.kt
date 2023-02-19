package foatto.core_server.app.system

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import java.util.*

class cUser : cAbstractHierarchy() {

    override fun doExpand(pid: Int): Set<Int> {
        val hsUser = mutableSetOf<Int>()
        hsUser += pid

        val alDivisionList = mutableListOf(pid)

        //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
        var i = 0
        while (i < alDivisionList.size) {
            val pID = alDivisionList[i]
            hsUser += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_DIVISION)
            hsUser += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_BOSS)
            hsUser += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_WORKER)

            alDivisionList += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_DIVISION)
            i++
        }
        return hsUser
    }

    override fun getTableColumnStyle(isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(isNewRow, hmColumnData, column, tci)

        val mu = model as mUser
        if (column == mu.columnRecordFullName && (hmColumnData[mu.columnRecordType] as DataComboBox).intValue != OrgType.ORG_TYPE_DIVISION) {
            val isDisabled = (hmColumnData[mu.columnDisabled] as DataBoolean).value
            val lastLogonTime = (hmColumnData[mu.columnUserLastLoginAttemptDate] as DataDate3Int).localDate.atStartOfDay(zoneId).toEpochSecond().toInt()

            //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = application.getUserNameColor(isDisabled, lastLogonTime)
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        application.checkAndSetNewPassword(conn, id, hmColumnData[(model as mUser).columnUserPassword] as? DataString)
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        var postURL = super.postAdd(id, hmColumnData, hmOut)

        //--- создать запись индивидуального сдвига часового пояса
        application.saveUserProperty(
            conn = conn,
            userId = id,
            userConfig = null,
            upName = UP_TIME_OFFSET,
            upValue = (TimeZone.getDefault().rawOffset / 1000).toString()
        )

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        hmOut[iApplication.USER_CONFIG] = application.getUserConfig(conn, userConfig.userId)

        if ((hmColumnData[(model as mUser).columnRecordType] as DataComboBox).intValue != OrgType.ORG_TYPE_DIVISION) {
            val refererID = hmParam[AppParameter.REFERER]
            val hmUserRoleParentData = mutableMapOf<String, Int>()
            hmUserRoleParentData["system_user"] = id
            hmUserRoleParentData["system_user_division"] = id
            hmUserRoleParentData["system_user_people"] = id
            postURL = getParamURL("system_user_role", AppAction.FORM, refererID, 0, hmUserRoleParentData, null, null)
        }
        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        hmOut[iApplication.USER_CONFIG] = application.getUserConfig(conn, userConfig.userId)

        return postURL
    }
}

package foatto.core_server.app.system

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.util.encodePassword
import foatto.core.util.getCurrentTimeInt
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
import foatto.sql.CoreAdvancedConnection
import java.util.*

class cUser : cAbstractHierarchy() {

    companion object {

        //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
        fun getUserNameColor(isDisabled: Boolean, lastLogonTime: Int): Int {
            val curTime = getCurrentTimeInt()

            return if (isDisabled) {
                TABLE_CELL_FORE_COLOR_DISABLED
            } else if (curTime - lastLogonTime > 7 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_CRITICAL
            } else if (curTime - lastLogonTime > 1 * 24 * 60 * 60) {
                TABLE_CELL_FORE_COLOR_WARNING
            } else {
                TABLE_CELL_FORE_COLOR_NORMAL
            }
        }

        fun checkAndSetNewPassword(conn: CoreAdvancedConnection, id: Int, pwd: DataString?) {
            var oldPassword = ""
            //--- запомнить старое значение пароля для возможной шифрации нового заданного пароля
            //--- на всякий случай проверка
            if (id != 0) {
                val rs = conn.executeQuery(" SELECT pwd FROM SYSTEM_users WHERE id = $id ")
                if (rs.next()) {
                    oldPassword = rs.getString(1)
                }
                rs.close()
            }
            pwd?.let {
                val newPassword = pwd.text
                //--- если пароль не менялся, лишний раз перешифровывать не будем
                if (newPassword != oldPassword) {
                    pwd.text = encodePassword(newPassword)
                }
            }
        }

        fun refreshUserConfig(application: iApplication, conn: CoreAdvancedConnection, userId: Int, hmOut: MutableMap<String, Any>) {
            //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
            application.reloadUserNames(conn)
            hmOut[iApplication.USER_CONFIG] = application.getUserConfig(conn, userId)
        }

        fun addTimeZone(conn: CoreAdvancedConnection, id: Int) {
            val gc = GregorianCalendar()
            conn.executeUpdate(
                " INSERT INTO SYSTEM_user_property( user_id , property_name , property_value ) VALUES ( " +
                    " $id , '$UP_TIME_OFFSET' , '${gc.timeZone.getOffset(gc.timeInMillis) / 1000}' ) "
            )
        }
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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
            tci.foreColor = getUserNameColor(isDisabled, lastLogonTime)
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        checkAndSetNewPassword(conn, id, hmColumnData[(model as mUser).columnUserPassword] as? DataString)
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        var postURL = super.postAdd(id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        refreshUserConfig(application, conn, userConfig.userId, hmOut)

        //--- создать запись индивидуального сдвига часового пояса
        addTimeZone(conn, id)

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

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name
        refreshUserConfig(application, conn, userConfig.userId, hmOut)

        return postURL
    }
}

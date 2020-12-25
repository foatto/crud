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
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataBoolean
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import java.util.*

class cUser : cAbstractHierarchy() {

    override fun doExpand( pid: Int ) = userConfig.loadFullSubUserList( stm, pid )

    override fun getTableColumnStyle( rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell ) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val mu = model as mUser
        if(column == mu.columnRecordFullName && (hmColumnData[mu.columnRecordType] as DataComboBox).value != OrgType.ORG_TYPE_DIVISION) {

            tci.foreColorType = TableCellForeColorType.DEFINED

            val isDisabled = (hmColumnData[mu.columnDisabled] as DataBoolean).value
            val lastLogonTime = (hmColumnData[mu.columnUserLastLoginAttemptDate] as DataDate3Int).localDate.atStartOfDay(zoneId).toEpochSecond().toInt()

            //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
            val curTime = getCurrentTimeInt()

            if(isDisabled)
                tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
            else if(curTime - lastLogonTime > 7 * 24 * 60 * 60)
                tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            else if(curTime - lastLogonTime > 1 * 24 * 60 * 60)
                tci.foreColor = TABLE_CELL_FORE_COLOR_WARNING
            else
                tci.foreColor = TABLE_CELL_FORE_COLOR_NORMAL
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        var oldPassword = ""
        //--- запомнить старое значение пароля для возможной шифрации нового заданного пароля
        //--- на всякий случай проверка
        if(id != 0) {
            val rs = stm.executeQuery( " SELECT pwd FROM SYSTEM_users WHERE id = $id " )
            if(rs.next()) oldPassword = rs.getString(1)
            rs.close()
        }
        val pwd = hmColumnData[ ( model as mUser ).columnUserPassword ] as? DataString
        pwd?.let {
            val newPassword = pwd.text
            //--- если пароль не менялся, лишний раз перешифровывать не будем
            if(newPassword != oldPassword) pwd.text = encodePassword( newPassword )
        }
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        var postURL = super.postAdd(id, hmColumnData, hmOut)

        //--- обновим конфигурацию текущего пользователя (более всего необходимо обновление списка пользователей id=name)
        hmOut[iApplication.USER_CONFIG] = UserConfig.getConfig(stm, userConfig.userID)

        //--- создать запись индивидуального сдвига часового пояса (позор, не могу определить текущий offset в jaba-8-time, пока сделаю через GregorianCalendar)
        val gc = GregorianCalendar()
        stm.executeUpdate(
            " INSERT INTO SYSTEM_user_property( user_id , property_name , property_value ) VALUES ( " +
            " $id , '$UP_TIME_OFFSET' , '${gc.timeZone.getOffset(gc.timeInMillis) / 1000}' ) " )

        if((hmColumnData[(model as mUser).columnRecordType] as DataComboBox).value != OrgType.ORG_TYPE_DIVISION) {
            val refererID = hmParam[AppParameter.REFERER]
            val hmUserRoleParentData = HashMap<String, Int>()
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
        hmOut[iApplication.USER_CONFIG] = UserConfig.getConfig(stm, userConfig.userID)

        return postURL
    }
}

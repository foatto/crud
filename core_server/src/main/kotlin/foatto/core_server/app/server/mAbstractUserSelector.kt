package foatto.core_server.app.server

import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString

abstract class mAbstractUserSelector : mAbstract() {

    fun addUserSelector(userConfig: UserConfig): ColumnString {
        //--- получить данные по правам доступа (при добавлении модуля в систему прав доступа к нему ещё нет)
        val isPeopleSelectable = userConfig.userPermission["system_user_people"]?.contains(cStandart.PERM_ACCESS) ?: false

        val columnUserID = ColumnInt("SYSTEM_users", "id")
        columnUser = ColumnInt(modelTableName, "user_id", columnUserID, userConfig.userId)
        val columnUserName = ColumnString("SYSTEM_users", "full_name", "Владелец", STRING_COLUMN_WIDTH).apply {
            if (isPeopleSelectable) {
                selectorAlias = "system_user_people"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        return columnUserName
    }

}

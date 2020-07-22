package foatto.core_server.app.server

import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

class AliasConfig private constructor(rs: CoreAdvancedResultSet) {

    var id = 0
        private set
    var alias: String
        private set
    var controlClassName: String
        private set
    var modelClassName: String
        private set
    var descr: String
        private set
    var isAuthorization = false     // класс требует обязательной аутентификации
        private set
    var isShowRowNo = false         // показывать ли столбец с номером строки в таблице
        private set
    var isShowUserColumn = false    // показывать ли столбец с именем пользователя
        private set
    var pageSize = 0                // размер страницы при просмотре
        private set
    var isNewable = false           // есть понятие "новая/прочитанная запись"
        private set
    var isNewAutoRead = false       // автопрочитка новых записей при просмотре в таблице (без просмотра в форме)
        private set
    var isDefaultParentUser = false // если нет парента, то по умолчанию parent == user
        private set

    init {
        id = rs.getInt(1)
        alias = rs.getString(2)

        controlClassName = rs.getString(3)
        modelClassName = rs.getString(4)
        descr = rs.getString(5)

        isAuthorization = rs.getInt(6) == 1
        isShowRowNo = rs.getInt(7) == 1
        isShowUserColumn = rs.getInt(8) == 1
        pageSize = rs.getInt(9)

        isNewable = rs.getInt(10) == 1
        isNewAutoRead = rs.getInt(11) == 1
        isDefaultParentUser = rs.getInt(12) == 1
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        fun getConfig(stm: CoreAdvancedStatement, aliasID: Int): AliasConfig? {
            val hmResult = getConfig(stm, aliasID, null)
            //--- возвращаем первый и единственный конфиг
            for(aliasConfig in hmResult.values) return aliasConfig
            return null
        }

        fun getConfig(stm: CoreAdvancedStatement, aliasName: String): AliasConfig? = getConfig(stm, null, aliasName)[aliasName]

        fun getConfig(stm: CoreAdvancedStatement, aliasID: Int? = null, aliasName: String? = null): Map<String, AliasConfig> {
            val hmResult = mutableMapOf<String, AliasConfig>()

            var sSQL =
                " SELECT id, name, control_name, model_name, descr, authorization_need, " +
                " show_row_no, show_user_column, table_page_size, newable, new_auto_read, default_parent_user " +
                " FROM SYSTEM_alias " +
                " WHERE "

            if(aliasID != null) sSQL += " id = $aliasID "
            else if(aliasName != null) sSQL += " name = '$aliasName' "
            else sSQL += " id <> 0 "

            val rs = stm.executeQuery(sSQL)
            while(rs.next()) {
                val ac = AliasConfig(rs)
                hmResult[ac.alias] = ac
            }
            rs.close()

            return hmResult
        }
    }

}

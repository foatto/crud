@file:JvmName("mDowntime")
package foatto.mms.core_mms

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDowntime : mAbstract() {

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_downtime"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnDowntimeDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")
        val columnDowntimeReason = ColumnString(tableName, "reason", "Причина", STRING_COLUMN_WIDTH)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)

        alTableGroupColumn.add(columnDowntimeDate)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnUser!!)

        //----------------------------------------------------------------------------------------------------------------------

        val os = ObjectSelector()
        os.fillColumns( this, true, false, alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, false, -1 )

        //----------------------------------------------------------------------------------------------------------------------

        addTableColumn(columnDowntimeReason)

        alFormColumn.add(columnDowntimeDate)
        alFormColumn.add(columnDowntimeReason)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDowntimeDate)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(os.columnObjectName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
    }
}

package foatto.mms.core_mms.device

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mServiceOrder : mAbstract() {

    lateinit var columnOrderCompleted: ColumnBoolean
        private set

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "MMS_service_order"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")
        columnUser = ColumnInt(tableName, "user_id", userConfig.userID)

        //----------------------------------------------------------------------------------------------------------------------

        val columnOrderTime = ColumnDateTimeInt(tableName, "order_time", "Дата и время заявки", false, zoneId)
        columnOrderTime.isEditable = false

        val columnOrderText = ColumnString(tableName, "order_text", "Текст заявки", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnOrderText.isEditable = id == 0
        columnOrderText.formPinMode = FormPinMode.OFF

        columnOrderCompleted = ColumnBoolean(tableName, "order_completed", "Заявка выполнена", false)
        columnOrderCompleted.isEditable = id != 0
        columnOrderCompleted.formPinMode = FormPinMode.OFF

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)

        addTableColumn(columnOrderTime)
        addTableColumn(columnOrderText)
        addTableColumn(columnOrderCompleted)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnUser!!)

        alFormColumn.add(columnOrderTime)
        alFormColumn.add(columnOrderText)
        alFormColumn.add(columnOrderCompleted)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnOrderTime)
        alTableSortDirect.add("ASC")
    }
}

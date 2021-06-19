package foatto.shop

import foatto.core.link.AppAction
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mClient : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_client"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClientName = ColumnString(tableName, "name", "Контрагент", STRING_COLUMN_WIDTH)
        columnClientName.isRequired = true
        columnClientName.setUnique(true, null)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)

        addTableColumn(columnClientName)

        alFormHiddenColumn.add(columnID)

        alFormColumn.add(columnClientName)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnClientName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        DocumentTypeConfig.fillDocChild(alChildData, columnID)
        alChildData.add(ChildData("Отчёты", "shop_report_doc_content", columnID, AppAction.FORM, true))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc", "client_id"))
    }

}
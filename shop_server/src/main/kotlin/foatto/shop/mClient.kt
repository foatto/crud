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

        modelTableName = "SHOP_client"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnClientName = ColumnString(modelTableName, "name", "Контрагент", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnClientName)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnClientName)

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnClientName)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        addTableSort(columnClientName, true)

        //----------------------------------------------------------------------------------------------------------------------

        DocumentTypeConfig.fillDocChild(alChildData, columnId)
        alChildData.add(ChildData("Отчёты", "shop_report_doc_content", columnId, AppAction.FORM, true))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc", "client_id"))
    }

}
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

class mWarehouse : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_warehouse"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnWarehouseName = ColumnString(tableName, "name", "Склад / Магазин", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            setUnique(true, null)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)

        addTableColumn(columnWarehouseName)

        alFormHiddenColumn.add(columnID!!)

        alFormColumn.add(columnWarehouseName)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnWarehouseName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------------------------------------

        DocumentTypeConfig.fillDocChild(alChildData, columnID!!)
        alChildData.add(ChildData("shop_cash", columnID!!, true))
        alChildData.add(ChildData("Отчёты", "shop_report_warehouse_state", columnID!!, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты", "shop_report_operation_summary", columnID!!, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "shop_report_cash_history", columnID!!, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc", "sour_id"))
        alDependData.add(DependData("SHOP_doc", "dest_id"))
        alDependData.add(DependData("SHOP_cash", "warehouse_id"))
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        fun fillWarehouseList(stm: CoreAdvancedStatement): List<Pair<Int, String>> {
            val alWarehouse = mutableListOf<Pair<Int, String>>()
            val rs = stm.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while (rs.next()) {
                alWarehouse += Pair(rs.getInt(1), rs.getString(2))
            }
            rs.close()

            return alWarehouse
        }

        fun fillWarehouseMap(stm: CoreAdvancedStatement): Map<Int, String> {
            val hmWarehouseName = mutableMapOf<Int, String>()
            //--- пустое имя для warehouseID == 0 тоже может пригодиться
            hmWarehouseName[0] = "(все склады / магазины)"

            val rs = stm.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while (rs.next()) {
                hmWarehouseName[rs.getInt(1)] = rs.getString(2)
            }
            rs.close()

            return hmWarehouseName
        }
    }

}

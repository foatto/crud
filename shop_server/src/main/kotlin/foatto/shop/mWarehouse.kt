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
import foatto.sql.CoreAdvancedConnection

class mWarehouse : mAbstract() {

    override fun init(application: iApplication, aConn: CoreAdvancedConnection, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_warehouse"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnWarehouseName = ColumnString(modelTableName, "name", "Склад / Магазин", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnWarehouseName)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnWarehouseName)

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnWarehouseName)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnWarehouseName, true)

        //----------------------------------------------------------------------------------------------------------------------

        DocumentTypeConfig.fillDocChild(alChildData, columnId)
        alChildData.add(ChildData("shop_cash", columnId, true))
        alChildData.add(ChildData("Отчёты", "shop_report_warehouse_state", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("Отчёты", "shop_report_operation_summary", columnId, AppAction.FORM))
        alChildData.add(ChildData("Отчёты", "shop_report_cash_history", columnId, AppAction.FORM))

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc", "sour_id"))
        alDependData.add(DependData("SHOP_doc", "dest_id"))
        alDependData.add(DependData("SHOP_cash", "warehouse_id"))
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        fun fillWarehouseList(conn: CoreAdvancedConnection): List<Pair<Int, String>> {
            val alWarehouse = mutableListOf<Pair<Int, String>>()

            val rs = conn.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while (rs.next()) {
                alWarehouse += Pair(rs.getInt(1), rs.getString(2))
            }
            rs.close()

            return alWarehouse
        }

        fun fillWarehouseMap(conn: CoreAdvancedConnection): Map<Int, String> {
            val hmWarehouseName = mutableMapOf<Int, String>()
            //--- пустое имя для warehouseID == 0 тоже может пригодиться
            hmWarehouseName[0] = "(все склады / магазины)"

            val rs = conn.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while (rs.next()) {
                hmWarehouseName[rs.getInt(1)] = rs.getString(2)
            }
            rs.close()

            return hmWarehouseName
        }
    }

}

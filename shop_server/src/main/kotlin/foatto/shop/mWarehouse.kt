package foatto.shop

import foatto.app.CoreSpringController
import foatto.core.link.AppAction
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import java.util.*

class mWarehouse : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_warehouse"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnWarehouseName = ColumnString(tableName, "name", "Склад / Магазин", STRING_COLUMN_WIDTH)
        columnWarehouseName.isRequired = true
        columnWarehouseName.setUnique(true, null)

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

        @JvmStatic
        fun fillWarehouseList(stm: CoreAdvancedStatement): Pair<ArrayList<Int>, ArrayList<String>> {
            val alWarehouseID = ArrayList<Int>()
            val alWarehouseName = ArrayList<String>()
            val rs = stm.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while(rs.next()) {
                alWarehouseID.add(rs.getInt(1))
                alWarehouseName.add(rs.getString(2))
            }
            rs.close()

            return Pair(alWarehouseID, alWarehouseName)
        }

        @JvmStatic
        fun fillWarehouseMap(stm: CoreAdvancedStatement): HashMap<Int, String> {
            val hmWarehouseName = HashMap<Int, String>()
            //--- пустое имя для warehouseID == 0 тоже может пригодиться
            hmWarehouseName[0] = "(все склады / магазины)"

            val rs = stm.executeQuery(" SELECT id , name FROM SHOP_warehouse WHERE id <> 0 ORDER BY name ")
            while(rs.next()) hmWarehouseName[rs.getInt(1)] = rs.getString(2)
            rs.close()

            return hmWarehouseName
        }
    }

}

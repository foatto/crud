package foatto.shop

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mCash : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val alWarehouse = mWarehouse.fillWarehouseList(stm)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_cash"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnCashWH = ColumnComboBox(tableName, "warehouse_id", "Склад / магазин").apply {
            alWarehouse.forEach { wh ->
                addChoice(wh.first, wh.second)
            }
            defaultValue = alWarehouse[0].first
        }

        val columnCashDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата").apply {
            setUnique(true, null)
        }

        val columnCashPut = ColumnDouble(tableName, "cash_put", "Сдано", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnCashUsed = ColumnDouble(tableName, "cash_used", "Истрачено", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDebtOut = ColumnDouble(tableName, "debt_out", "Дано в долг", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDebtIn = ColumnDouble(tableName, "debt_in", "Возвращено долгов", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnCashRest = ColumnDouble(tableName, "cash_rest", "Остаток на конец дня", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDocumentDescr = ColumnString(tableName, "descr", "Примечание", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)

        addTableColumn(columnCashWH)
        addTableColumn(columnCashDate)
        addTableColumn(columnCashPut)
        addTableColumn(columnCashUsed)
        addTableColumn(columnDebtOut)
        addTableColumn(columnDebtIn)
        addTableColumn(columnCashRest)
        addTableColumn(columnDocumentDescr)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)

        alFormColumn.add(columnCashWH)
        alFormColumn.add(columnCashDate)
        alFormColumn.add(columnCashPut)
        alFormColumn.add(columnCashUsed)
        alFormColumn.add(columnDebtOut)
        alFormColumn.add(columnDebtIn)
        alFormColumn.add(columnCashRest)
        alFormColumn.add(columnDocumentDescr)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnCashDate)
        alTableSortDirect.add("DESC")

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["shop_warehouse"] = columnCashWH

    }
}

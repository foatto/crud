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

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val alWarehouse = mWarehouse.fillWarehouseList(stm)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_cash"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnCashWH = ColumnComboBox(modelTableName, "warehouse_id", "Склад / магазин").apply {
            alWarehouse.forEach { wh ->
                addChoice(wh.first, wh.second)
            }
            defaultValue = alWarehouse[0].first
        }

        val columnCashDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Дата")

        val columnCashPut = ColumnDouble(modelTableName, "cash_put", "Сдано", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnCashUsed = ColumnDouble(modelTableName, "cash_used", "Истрачено", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDebtOut = ColumnDouble(modelTableName, "debt_out", "Дано в долг", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDebtIn = ColumnDouble(modelTableName, "debt_in", "Возвращено долгов", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnCashRest = ColumnDouble(modelTableName, "cash_rest", "Остаток на конец дня", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnDocumentDescr = ColumnString(modelTableName, "descr", "Примечание", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnCashDate)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)

        addTableColumn(columnCashWH)
        addTableColumn(columnCashDate)
        addTableColumn(columnCashPut)
        addTableColumn(columnCashUsed)
        addTableColumn(columnDebtOut)
        addTableColumn(columnDebtIn)
        addTableColumn(columnCashRest)
        addTableColumn(columnDocumentDescr)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)

        alFormColumn.add(columnCashWH)
        alFormColumn.add(columnCashDate)
        alFormColumn.add(columnCashPut)
        alFormColumn.add(columnCashUsed)
        alFormColumn.add(columnDebtOut)
        alFormColumn.add(columnDebtIn)
        alFormColumn.add(columnCashRest)
        alFormColumn.add(columnDocumentDescr)

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnCashDate, false)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["shop_warehouse"] = columnCashWH

    }
}

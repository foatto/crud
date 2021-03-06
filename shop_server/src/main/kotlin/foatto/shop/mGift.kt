package foatto.shop

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate

class mGift : mAbstract() {

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {
        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_gift"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnOutDate = ColumnDate3Int(tableName, "out_ye", "out_mo", "out_da", "Дата выдачи")

        val columnNo = ColumnString(tableName, "no", "Номер", STRING_COLUMN_WIDTH)

        val columnPrice = ColumnDouble(tableName, "price", "Стоимость", 10, 2).apply {
            setEmptyData(0.0, "-")
            tableAlign = TableCellAlign.RIGHT
        }

        val columnIsUsed = ColumnBoolean(tableName, "is_used", "Использован", false)

        val columnUseDate = ColumnDate3Int(tableName, "use_ye", "use_mo", "use_da", "Дата использования").apply {
            default = LocalDate.of(2000, 1, 1)
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)

        addTableColumn(columnOutDate)
        addTableColumn(columnNo)
        addTableColumn(columnPrice)
        addTableColumn(columnIsUsed)
        addTableColumn(columnUseDate)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID)

        alFormColumn.add(columnOutDate)
        alFormColumn.add(columnNo)
        alFormColumn.add(columnPrice)
        alFormColumn.add(columnIsUsed)
        alFormColumn.add(columnUseDate)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnOutDate)
        alTableSortDirect.add("DESC")

    }
}

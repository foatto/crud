package foatto.shop

import foatto.core.link.TableCellAlign
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mCash : mAbstract() {

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val ( alWarehouseID, alWarehouseName ) = mWarehouse.fillWarehouseList( stm )

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_cash"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        val columnCashWH = ColumnComboBox( tableName, "warehouse_id", "Склад / магазин" )
            for( i in alWarehouseID.indices ) columnCashWH.addChoice( alWarehouseID[ i ], alWarehouseName[ i ] )

        val columnCashDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")
            columnCashDate.setUnique( true, null )

        val columnCashPut = ColumnDouble( tableName, "cash_put", "Сдано", 10, 2 )
            columnCashPut.setEmptyData( 0.0, "-" )
            columnCashPut.tableAlign = TableCellAlign.RIGHT

        val columnCashUsed = ColumnDouble( tableName, "cash_used", "Истрачено", 10, 2 )
            columnCashUsed.setEmptyData( 0.0, "-" )
            columnCashUsed.tableAlign = TableCellAlign.RIGHT

        val columnDebtOut = ColumnDouble( tableName, "debt_out", "Дано в долг", 10, 2 )
            columnDebtOut.setEmptyData( 0.0, "-" )
            columnDebtOut.tableAlign = TableCellAlign.RIGHT

        val columnDebtIn = ColumnDouble( tableName, "debt_in", "Возвращено долгов", 10, 2 )
            columnDebtIn.setEmptyData( 0.0, "-" )
            columnDebtIn.tableAlign = TableCellAlign.RIGHT

        val columnCashRest = ColumnDouble( tableName, "cash_rest", "Остаток на конец дня", 10, 2 )
            columnCashRest.setEmptyData( 0.0, "-" )
            columnCashRest.tableAlign = TableCellAlign.RIGHT

        val columnDocumentDescr = ColumnString( tableName, "descr", "Примечание", 12, STRING_COLUMN_WIDTH, textFieldMaxSize )

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID!! )

        addTableColumn( columnCashWH )
        addTableColumn( columnCashDate )
        addTableColumn( columnCashPut )
        addTableColumn( columnCashUsed )
        addTableColumn( columnDebtOut )
        addTableColumn( columnDebtIn )
        addTableColumn( columnCashRest )
        addTableColumn( columnDocumentDescr )

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )

        alFormColumn.add( columnCashWH )
        alFormColumn.add( columnCashDate )
        alFormColumn.add( columnCashPut )
        alFormColumn.add( columnCashUsed )
        alFormColumn.add( columnDebtOut )
        alFormColumn.add( columnDebtIn )
        alFormColumn.add( columnCashRest )
        alFormColumn.add( columnDocumentDescr )

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add( columnCashDate )
        alTableSortDirect.add( "DESC" )

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn[ "shop_warehouse" ] = columnCashWH

    }
}

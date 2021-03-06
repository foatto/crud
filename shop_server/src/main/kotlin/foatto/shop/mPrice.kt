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

class mPrice : mAbstract() {

    companion object {

        val PRICE_TYPE_IN = 0
        val PRICE_TYPE_OUT = 1
    }

    lateinit var columnPriceType: ColumnComboBox

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_price"

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        val columnCatalogID = ColumnInt( "SHOP_catalog", "id" )
        val columnCatalog = ColumnInt( tableName, "catalog_id", columnCatalogID )
        val columnCatalogName = ColumnString( "SHOP_catalog", "name", "Товар", 3, STRING_COLUMN_WIDTH, textFieldMaxSize )
            columnCatalogName.isRequired = true
            columnCatalogName.selectorAlias = "shop_catalog_item"
            columnCatalogName.addSelectorColumn( columnCatalog, columnCatalogID )
            columnCatalogName.addSelectorColumn( columnCatalogName)   //, columnCatalogName );

        columnPriceType = ColumnComboBox( tableName, "price_type", "Тип цены", if( aliasConfig.alias == "shop_price_in" ) PRICE_TYPE_IN else PRICE_TYPE_OUT )
            columnPriceType.addChoice( PRICE_TYPE_IN, "Закупочная цена" )
            columnPriceType.addChoice( PRICE_TYPE_OUT, "Розничная цена" )

        val columnPriceDate = ColumnDate3Int(tableName, "ye", "mo", "da", "Дата")

        val columnPriceValue = ColumnDouble( tableName, "price_value", "Цена", 10, 2 )
            columnPriceValue.tableAlign = TableCellAlign.RIGHT

        val columnPriceNote = ColumnString( tableName, "price_note", "Примечание", STRING_COLUMN_WIDTH )

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add( columnID )
        alTableHiddenColumn.add( columnCatalog )

        alTableGroupColumn.add( columnPriceDate )

        addTableColumn( columnCatalogName )
        addTableColumn( columnPriceType )
        addTableColumn( columnPriceValue )
        addTableColumn( columnPriceNote )

        alFormHiddenColumn.add( columnID )
        alFormHiddenColumn.add( columnCatalog )

        alFormColumn.add( columnCatalogName )
        alFormColumn.add( columnPriceType )
        alFormColumn.add( columnPriceDate )
        alFormColumn.add( columnPriceValue )
        alFormColumn.add( columnPriceNote )

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        //alTableSortColumn.add( columnCatalogName ); - нет мсмысла в этой сортировке, цены всегда показываются в разрезе одной единицы товара
        //    alTableSortDirect.add( "ASC" );
        alTableSortColumn.add( columnPriceDate )
        alTableSortDirect.add( "DESC" )
        alTableSortColumn.add( columnCatalogName )
        alTableSortDirect.add( "ASC" )
        alTableSortColumn.add( columnPriceType )
        alTableSortDirect.add( "ASC" )

        //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        hmParentColumn[ "shop_catalog" ] = columnCatalog
    }
}

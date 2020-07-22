package foatto.shop

import foatto.core_server.app.server.cStandart

class cPrice : cStandart() {

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {

        var sSQL = ""
        if( aliasConfig.alias != "shop_price" ) {
            val mp = model as mPrice
            val tableName = renameTableName( hsTableRenameList, model.tableName )
            val typeFieldName = mp.columnPriceType.getFieldName( 0 )
            val typeValue = if( aliasConfig.alias == "shop_price_in" ) mPrice.PRICE_TYPE_IN else mPrice.PRICE_TYPE_OUT

            sSQL = " AND $tableName.$typeFieldName = $typeValue "
        }
        return super.addSQLWhere( hsTableRenameList ) + sSQL
    }


}
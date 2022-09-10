package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.sql.CoreAdvancedConnection

class mPriceTag : mAbstractReport() {

    companion object {

        val ROWS = 7
        val COLS = 3
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val alColumnCatalog = ArrayList<ArrayList<ColumnInt>>(ROWS)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val alColumnCatalogName = ArrayList<ArrayList<ColumnString>>(ROWS)

        val columnCatalogName = ColumnString("SHOP_catalog", "name", "aaa", 1)

        for(i in 0 until ROWS) {
            alColumnCatalog.add(ArrayList(COLS))
            alColumnCatalogName.add(ArrayList(COLS))
            for(j in 0 until COLS) {
                val selfLinkDestTableName = "SHOP_catalog_$i$j"

                val columnCatalogDestID = ColumnInt(selfLinkDestTableName, "id")
                columnCatalogDestID.selfLinkTableName = "SHOP_catalog"

                alColumnCatalog[i].add(ColumnInt(modelTableName, "catalog_id_$i$j", columnCatalogDestID))
                alColumnCatalog[i][j].isVirtual = true

                alColumnCatalogName[i].add(ColumnString(selfLinkDestTableName, "name_$i$j", "Товар", 3, STRING_COLUMN_WIDTH, textFieldMaxSize))
                alColumnCatalogName[i][j].selfLinkTableName = "SHOP_catalog"  // для правильной работы селектора с подстановочной таблицей
                //columnCatalogDestName.setRequired(  false  );
                alColumnCatalogName[i][j].isVirtual = true
                alColumnCatalogName[i][j].selectorAlias = "shop_catalog_item"
                alColumnCatalogName[i][j].addSelectorColumn(alColumnCatalog[i][j], columnCatalogDestID)
                alColumnCatalogName[i][j].addSelectorColumn(alColumnCatalogName[i][j], columnCatalogName)
                //arrColumnCatalogName[  i  ][  j  ].addSelectorColumn(  arrColumnCatalogName[  i  ][  j  ]  );   //, columnCatalogName  );
            }
        }

        val columnTMP = ColumnInt(modelTableName, "dest_catalog_id")

        //----------------------------------------------------------------------------------------------------------------------

        //        initReportCapAndSignature(  aliasConfig, userConfig  );

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnTMP)

        for(i in 0 until ROWS)
            for(j in 0 until COLS) {
                alFormHiddenColumn.add(alColumnCatalog[i][j])

                alFormColumn.add(alColumnCatalogName[i][j])
            }
    }

    fun getColumnCatalog(row: Int, col: Int): ColumnInt {
        return alColumnCatalog[row][col]
    }
}
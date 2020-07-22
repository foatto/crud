package foatto.shop.report

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.shop.DocumentTypeConfig
import foatto.shop.mWarehouse
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate

abstract class mSHOPReport : mAbstractReport() {

    //--- стандартные ширины столбцов
    // NNN                  = "N п/п"               = 5
    // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 16
    // dd.mm.yyyy hh:mm     = "начало/окончание"    = 14
    // hhhh:mm:ss           = "длитель-ность"       = 9
    // A999AA116RUS         = "объект/скважина     >= 20 (нельзя уменьшить менее 20?)
    // A999AA116RUS         = "датчик/оборуд."     <= 20 (можно уменьшить до 15?)
    // 9999.9               = "время работы"        = 7
    // dd.mm.yyyy           = "дата"                = 9
    // АИ-95 (осн.)(изм.)   = "наим. жидкости"      = 15
    // 9999.9               = расход жидкости       = 7

    protected var isReportWarehouse = false
    protected var isUseNullWarehouse = false   // использовать ли "все склады/магазины"
    protected var isReportDocument = false
    protected var isReportClient = false
    protected var isReportDocumentType = false
    protected var isReportCatalog = false
    protected var catalogSelectorAlias: String? = null
    protected var isReportBegDate = false
    protected var isReportEndDate = false
    //    protected String periodCaption = null;

    protected var isUseCapAndSignature = false

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var columnWarehouseDest: ColumnComboBox
        private set

    lateinit var columnDocument: ColumnInt
        private set
    lateinit var columnClient: ColumnInt
        private set
    lateinit var columnDocumentType: ColumnComboBox
        private set

    lateinit var columnCatalogDest: ColumnInt
        private set

    lateinit var columnBegDate: ColumnDate3Int
        private set
    lateinit var columnEndDate: ColumnDate3Int
        private set

    //    private ColumnInt columnPeriod = null;

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val hmAliasConfig = AliasConfig.getConfig( stm )

        //----------------------------------------------------------------------------------------------------------------------

        val ( alWarehouseID, alWarehouseName ) = mWarehouse.fillWarehouseList( stm )

        //----------------------------------------------------------------------------------------------------------------------

        var parentDocID: Int? = null
        var parentDocType = DocumentTypeConfig.TYPE_ALL
        var parentClient: Int? = null
        for( ( dt, an ) in DocumentTypeConfig.hmDocTypeAlias ) {
            parentDocID = hmParentData[ an ]
            if( parentDocID != null ) {
                parentDocType = dt
                val rs = stm.executeQuery( " SELECT client_id FROM SHOP_doc WHERE id = $parentDocID " )
                if( rs.next() ) parentClient = rs.getInt( 1 )
                rs.close()

                break
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt( tableName, "id" )

        //----------------------------------------------------------------------------------------------------------------------

        columnWarehouseDest = ColumnComboBox( tableName, "dest_warehouse_id", "На склад / магазин" )
        if( isUseNullWarehouse ) columnWarehouseDest.addChoice( 0, "(все склады / магазины)" )
            for( i in alWarehouseID.indices )
                columnWarehouseDest.addChoice( alWarehouseID[ i ], alWarehouseName[ i ] )
            columnWarehouseDest.defaultValue = if( isUseNullWarehouse ) 0 else alWarehouseID[ 0 ]

        //----------------------------------------------------------------------------------------------------------------------

        val columnDocumentID = ColumnInt( "SHOP_doc", "id" )
        columnDocument = ColumnInt( tableName, "doc_id", columnDocumentID, parentDocID )
        val columnDocumentNo = ColumnString( "SHOP_doc", "doc_no", "Номер накладной", STRING_COLUMN_WIDTH )
        val columnDocumentDate = ColumnDate3Int("SHOP_doc", "doc_ye", "doc_mo", "doc_da", "Дата")

        val columnClientID = ColumnInt( "SHOP_client", "id" )
        columnClient = ColumnInt( tableName, "client_id", columnClientID, parentClient )
        val columnClientName = ColumnString( "SHOP_client", "name", "Контрагент", STRING_COLUMN_WIDTH )
        if( isReportClient ) {
            columnClientName.selectorAlias = "shop_client"
            columnClientName.addSelectorColumn( columnClient, columnClientID )
            columnClientName.addSelectorColumn( columnClientName )
        }

        val columnDocumentDescr = ColumnString( "SHOP_doc", "descr", "Примечание", STRING_COLUMN_WIDTH )

        columnDocumentType = ColumnComboBox( tableName, "doc_type", "Тип накладной", parentDocType )
            columnDocumentType.isVirtual = true
            columnDocumentType.isEditable = isReportDocumentType
            for( ( dt, an ) in DocumentTypeConfig.hmDocTypeAlias )
                columnDocumentType.addChoice( dt, hmAliasConfig[ an ]!!.descr )
        //--- спецполя для одновременной установки клиента и типа накладной, чтобы отдельно его не хватать
        val columnClient_ = ColumnInt( "SHOP_doc", "client_id" )
        val columnDocumentType_ = ColumnComboBox( "SHOP_doc", "doc_type", "", DocumentTypeConfig.TYPE_ALL )

        //--- опишем ниже columnDocumentType, чтобы при выборе накладной тип тоже устанавливался автоматически
        columnDocumentNo.isRequired = false
        columnDocumentNo.selectorAlias = "shop_doc_all"
        columnDocumentNo.addSelectorColumn( columnDocument, columnDocumentID )
        columnDocumentNo.addSelectorColumn( columnDocumentNo )
        columnDocumentNo.addSelectorColumn( columnDocumentDate )
        columnDocumentNo.addSelectorColumn( columnClient, columnClient_ )
        columnDocumentNo.addSelectorColumn( columnClientName )
        columnDocumentNo.addSelectorColumn( columnDocumentDescr )
        columnDocumentNo.addSelectorColumn( columnDocumentType, columnDocumentType_ )

        //----------------------------------------------------------------------------------------------------------------------

        val selfLinkDestTableName = "SHOP_catalog_2"
        val columnCatalogDestID = ColumnInt( selfLinkDestTableName, "id" )
            columnCatalogDestID.selfLinkTableName = "SHOP_catalog"
        columnCatalogDest = ColumnInt( tableName, "dest_catalog_id", columnCatalogDestID )
        val columnCatalogDestName = ColumnString( selfLinkDestTableName, "name", "Товар", 3, STRING_COLUMN_WIDTH, textFieldMaxSize )
            columnCatalogDestName.selfLinkTableName = "SHOP_catalog"  // для правильной работы селектора с подстановочной таблицей
            columnCatalogDestName.isRequired = false
            columnCatalogDestName.selectorAlias = catalogSelectorAlias
            columnCatalogDestName.addSelectorColumn( columnCatalogDest, columnCatalogDestID )
            columnCatalogDestName.addSelectorColumn( columnCatalogDestName)   //, columnCatalogName );

        columnBegDate = ColumnDate3Int(tableName, "beg_ye", "beg_mo", "beg_da", "Начало периода")
            columnBegDate.isVirtual = true
            columnBegDate.default = LocalDate.now(zoneId).withDayOfMonth(1)
        columnEndDate = ColumnDate3Int(tableName, "end_ye", "end_mo", "end_da", if( isReportBegDate ) "Конец периода" else "Дата")
            columnEndDate.isVirtual = true

        //        if( periodCaption != null ) {
        //            columnPeriod = new ColumnInt( tableName, "period", periodCaption, 10 );
        //                columnPeriod.setVirtual( true );
        //                columnPeriod.setSavedDefault( userConfig );
        //        }

        //----------------------------------------------------------------------------------------------------------------------

        initReportCapAndSignature( aliasConfig, userConfig )

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add( columnID!! )
        alFormHiddenColumn.add( columnDocument )
        alFormHiddenColumn.add( columnClient )
        alFormHiddenColumn.add( columnCatalogDest )

        ( if( isReportWarehouse ) alFormColumn else alFormHiddenColumn ).add( columnWarehouseDest )

        ( if( isReportDocument ) alFormColumn else alFormHiddenColumn ).add( columnDocumentNo )
        ( if( isReportDocument ) alFormColumn else alFormHiddenColumn ).add( columnDocumentDate )
        ( if( isReportDocument ) alFormColumn else alFormHiddenColumn ).add( columnDocumentDescr )
        ( if( ( isReportDocument || isReportClient ) && DocumentTypeConfig.hsUseClient.contains( parentDocType ) ) alFormColumn else alFormHiddenColumn ).add( columnClientName )
        ( if( isReportDocument || isReportDocumentType ) alFormColumn else alFormHiddenColumn ).add( columnDocumentType )

        ( if( isReportCatalog ) alFormColumn else alFormHiddenColumn ).add( columnCatalogDestName )

        ( if( isReportBegDate ) alFormColumn else alFormHiddenColumn ).add( columnBegDate )
        ( if( isReportEndDate ) alFormColumn else alFormHiddenColumn ).add( columnEndDate )

        //        ( periodCaption != null ? alFormColumn : alFormHiddenColumn ).add( columnPeriod );

        addCapAndSignatureColumns()

        //----------------------------------------------------------------------------------------------------------------------

        //--- перебираем вручную в начале метода
        //        for( Integer dt : DocumentTypeConfig.hmDocTypeAlias.keySet() )
        //            hmParentColumn.put( DocumentTypeConfig.hmDocTypeAlias.get( dt ), columnDocument );
        hmParentColumn[ "shop_client" ] = columnClient
        hmParentColumn[ "shop_catalog" ] = columnCatalogDest
    }
    //    public ColumnInt getColumnPeriod() { return columnPeriod; }
}

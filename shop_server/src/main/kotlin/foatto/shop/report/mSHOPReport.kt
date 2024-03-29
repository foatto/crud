package foatto.shop.report

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractReport
import foatto.shop.DocumentTypeConfig
import foatto.shop.mWarehouse
import foatto.sql.CoreAdvancedConnection
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

        val hmAliasConfigs = application.getAliasConfig(conn)

        //----------------------------------------------------------------------------------------------------------------------

        val alWarehouse = mWarehouse.fillWarehouseList(conn)

        //----------------------------------------------------------------------------------------------------------------------

        var parentDocID: Int? = null
        var parentDocType = DocumentTypeConfig.TYPE_ALL
        var parentClient: Int? = null
        for ((dt, an) in DocumentTypeConfig.hmDocTypeAlias) {
            parentDocID = hmParentData[an]
            if (parentDocID != null) {
                parentDocType = dt
                val rs = conn.executeQuery(" SELECT client_id FROM SHOP_doc WHERE id = $parentDocID ")
                if (rs.next()) {
                    parentClient = rs.getInt(1)
                }
                rs.close()

                break
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_report"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        columnWarehouseDest = ColumnComboBox(modelTableName, "dest_warehouse_id", "На склад / магазин").apply {
            if (isUseNullWarehouse) {
                addChoice(0, "(все склады / магазины)")
            }
            alWarehouse.forEach { wh ->
                addChoice(wh.first, wh.second)
            }
            defaultValue = if (isUseNullWarehouse) 0 else alWarehouse[0].first
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnDocumentID = ColumnInt("SHOP_doc", "id")
        columnDocument = ColumnInt(modelTableName, "doc_id", columnDocumentID, parentDocID)
        val columnDocumentNo = ColumnString("SHOP_doc", "doc_no", "Номер накладной", STRING_COLUMN_WIDTH)
        val columnDocumentDate = ColumnDate3Int("SHOP_doc", "doc_ye", "doc_mo", "doc_da", "Дата")

        val columnClientID = ColumnInt("SHOP_client", "id")
        columnClient = ColumnInt(modelTableName, "client_id", columnClientID, parentClient)
        val columnClientName = ColumnString("SHOP_client", "name", "Контрагент", STRING_COLUMN_WIDTH).apply {
            if (isReportClient) {
                selectorAlias = "shop_client"
                addSelectorColumn(columnClient, columnClientID)
                addSelectorColumn(this)
            }
        }
        val columnDocumentDescr = ColumnString("SHOP_doc", "descr", "Примечание", STRING_COLUMN_WIDTH)

        columnDocumentType = ColumnComboBox(modelTableName, "doc_type", "Тип накладной", parentDocType).apply {
            isVirtual = true
            isEditable = isReportDocumentType
            for ((dt, an) in DocumentTypeConfig.hmDocTypeAlias) {
                addChoice(dt, hmAliasConfigs[an]?.descr ?: "(неизвестный тип накладной = $dt : '$an')")
            }
        }
        //--- спецполя для одновременной установки клиента и типа накладной, чтобы отдельно его не хватать
        val columnClient_ = ColumnInt("SHOP_doc", "client_id")
        val columnDocumentType_ = ColumnComboBox("SHOP_doc", "doc_type", "", DocumentTypeConfig.TYPE_ALL)

        //--- опишем ниже columnDocumentType, чтобы при выборе накладной тип тоже устанавливался автоматически
        columnDocumentNo.apply {
            isRequired = false
            selectorAlias = "shop_doc_all"
            addSelectorColumn(columnDocument, columnDocumentID)
            addSelectorColumn(this)
            addSelectorColumn(columnDocumentDate)
            addSelectorColumn(columnClient, columnClient_)
            addSelectorColumn(columnClientName)
            addSelectorColumn(columnDocumentDescr)
            addSelectorColumn(columnDocumentType, columnDocumentType_)
        }
        //----------------------------------------------------------------------------------------------------------------------

        val selfLinkDestTableName = "SHOP_catalog_2"
        val columnCatalogDestID = ColumnInt(selfLinkDestTableName, "id").apply {
            selfLinkTableName = "SHOP_catalog"
        }
        columnCatalogDest = ColumnInt(modelTableName, "dest_catalog_id", columnCatalogDestID)
        val columnCatalogDestName = ColumnString(selfLinkDestTableName, "name", "Товар", 3, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            selfLinkTableName = "SHOP_catalog"  // для правильной работы селектора с подстановочной таблицей
            isRequired = false
            selectorAlias = catalogSelectorAlias
            addSelectorColumn(columnCatalogDest, columnCatalogDestID)
            addSelectorColumn(this)   //, columnCatalogName );
        }
        columnBegDate = ColumnDate3Int(modelTableName, "beg_ye", "beg_mo", "beg_da", "Начало периода").apply {
            isVirtual = true
            default = LocalDate.now(zoneId).withDayOfMonth(1)
        }
        columnEndDate = ColumnDate3Int(modelTableName, "end_ye", "end_mo", "end_da", if (isReportBegDate) "Конец периода" else "Дата").apply {
            isVirtual = true
        }

        //        if( periodCaption != null ) {
        //            columnPeriod = new ColumnInt( tableName, "period", periodCaption, 10 );
        //                columnPeriod.setVirtual( true );
        //                columnPeriod.setSavedDefault( userConfig );
        //        }

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnDocument)
        alFormHiddenColumn.add(columnClient)
        alFormHiddenColumn.add(columnCatalogDest)

        (if (isReportWarehouse) alFormColumn else alFormHiddenColumn).add(columnWarehouseDest)

        (if (isReportDocument) alFormColumn else alFormHiddenColumn).add(columnDocumentNo)
        (if (isReportDocument) alFormColumn else alFormHiddenColumn).add(columnDocumentDate)
        (if (isReportDocument) alFormColumn else alFormHiddenColumn).add(columnDocumentDescr)
        (if ((isReportDocument || isReportClient) && DocumentTypeConfig.hsUseClient.contains(parentDocType)) alFormColumn else alFormHiddenColumn).add(columnClientName)
        (if (isReportDocument || isReportDocumentType) alFormColumn else alFormHiddenColumn).add(columnDocumentType)

        (if (isReportCatalog) alFormColumn else alFormHiddenColumn).add(columnCatalogDestName)

        (if (isReportBegDate) alFormColumn else alFormHiddenColumn).add(columnBegDate)
        (if (isReportEndDate) alFormColumn else alFormHiddenColumn).add(columnEndDate)

        //        ( periodCaption != null ? alFormColumn : alFormHiddenColumn ).add( columnPeriod );

        //----------------------------------------------------------------------------------------------------------------------

        //--- перебираем вручную в начале метода
        //        for( Integer dt : DocumentTypeConfig.hmDocTypeAlias.keySet() )
        //            hmParentColumn.put( DocumentTypeConfig.hmDocTypeAlias.get( dt ), columnDocument );
        hmParentColumn["shop_client"] = columnClient
        hmParentColumn["shop_catalog"] = columnCatalogDest
    }

//    public ColumnInt getColumnPeriod() { return columnPeriod; }
}

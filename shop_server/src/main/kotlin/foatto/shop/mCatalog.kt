package foatto.shop

import foatto.core.app.ICON_NAME_ADD_FOLDER
import foatto.core.app.ICON_NAME_ADD_ITEM
import foatto.core.app.ICON_NAME_FOLDER
import foatto.core.link.AddActionButton
import foatto.core.link.AppAction
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.sql.CoreAdvancedConnection
import java.time.LocalDate

class mCatalog : mAbstractHierarchy() {

    companion object {
        const val MARK_CODE_LEN = 31
    }

    lateinit var columnCatalogPriceDate: ColumnDate3Int
        private set
    lateinit var columnCatalogPriceIn: ColumnDouble
        private set
    lateinit var columnCatalogPriceOut: ColumnDouble
        private set
    lateinit var columnCatalogRowCount: ColumnString
        private set  // т.к. у элементов номенклатуры - пустое
    lateinit var alColumnCatalogCount: MutableList<ColumnDouble>
        private set
    lateinit var columnCatalogAllCount: ColumnDouble
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        commonAliasName = "shop_catalog"
        folderAliasName = "shop_catalog_folder"
        itemAliasName = "shop_catalog_item"

        modelTableName = "SHOP_catalog"

        alAddButtomParam.add(AddActionButton("Добавить раздел", "Добавить раздел", ICON_NAME_ADD_FOLDER, "$RECORD_TYPE_PARAM=$RECORD_TYPE_FOLDER"))
        alAddButtomParam.add(AddActionButton("Добавить товар", "Добавить товар", ICON_NAME_ADD_ITEM, "$RECORD_TYPE_PARAM=$RECORD_TYPE_ITEM"))
    }

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

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission[aliasConfig.name]
        //--- при добавлении модуля в систему прав доступа к нему ещё нет
        val isMerchant = hsPermission?.contains(cCatalog.PERM_MERCHANT) ?: false

        val alWarehouse = mWarehouse.fillWarehouseList(conn)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        columnActive = ColumnBoolean(modelTableName, "in_active", "", true)
        columnArchive = ColumnBoolean(modelTableName, "in_archive", "", false)

        val recordType = getRecordType(id, "record_type", RECORD_TYPE_ITEM)

        columnRecordType = ColumnComboBox(modelTableName, "record_type", "", recordType).apply {
            addChoice(RECORD_TYPE_FOLDER, ">>>", "Подраздел", ICON_NAME_FOLDER)
            addChoice(RECORD_TYPE_ITEM, "", "Товар")
            tableAlign = TableCellAlign.CENTER
        }

        columnParentFullName = ColumnString(selfLinkParentTableName, "name", "Раздел", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = modelTableName
            selectorAlias = folderAliasName
            addSelectorColumn(columnParent, columnParentID)
            addSelectorColumn(this)
        }

        columnRecordFullName = ColumnString(modelTableName, "name", "-", 3, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            isRequired = true
            addFormCaption(columnRecordType, "Подраздел", setOf(RECORD_TYPE_FOLDER))
            addFormCaption(columnRecordType, "Наименование товара", setOf(RECORD_TYPE_ITEM))
        }

        val columnCatalogIsMark = ColumnBoolean(modelTableName, "is_mark", "Маркировка", false).apply {
            tableCaption = "Марк."
        }

        val columnIsProduction = ColumnBoolean(modelTableName, "is_production", "Собственное производство").apply {
            addFormVisible(columnRecordType, true, setOf(RECORD_TYPE_ITEM))
            tableCaption = "Собс. пр-во"
        }

        val columnProfitAdd = ColumnInt(modelTableName, "profit_add", "Добавочная наценка", 10).apply {
            tableCaption = "Добав. наценка"
        }

        columnCatalogPriceDate = ColumnDate3Int(modelTableName, "_price_ye", "_price_mo", "_price_da", "Дата установки цены").apply {
            isVirtual = true
            tableCaption = "Дата устан. цены"
            addFormVisible(columnRecordType, true, setOf(RECORD_TYPE_ITEM))
            default = LocalDate.now(zoneId)
            //gc.add( GregorianCalendar.DAY_OF_MONTH, 1 ) - оказалось неудобно, т.к. цена завтрашняя и сегодня она не отображается, что вводит в заблуждение
        }

        columnCatalogPriceIn = ColumnDouble(modelTableName, "_price_in", "Закупочная цена", 10, 2).apply {
            isVirtual = true
            addFormVisible(columnRecordType, true, setOf(RECORD_TYPE_ITEM))
            tableCaption = "Закуп. цена"
            tableAlign = TableCellAlign.RIGHT
        }

        columnCatalogPriceOut = ColumnDouble(modelTableName, "_price_out", "Розничная цена", 10, 2).apply {
            isVirtual = true
            addFormVisible(columnRecordType, true, setOf(RECORD_TYPE_ITEM))
            tableCaption = "Розн. цена"
            tableAlign = TableCellAlign.RIGHT
        }

        columnCatalogRowCount = ColumnString(modelTableName, "_row_count", "Кол-во наименований", 10).apply {
            isVirtual = true
            emptyValueString = ""
            tableCaption = "Кол-во наим."
            tableAlign = TableCellAlign.RIGHT
        }

        alColumnCatalogCount = mutableListOf()
        for (i in alWarehouse.indices) {
            val cd = ColumnDouble(modelTableName, "_$i", alWarehouse[i].second, 10, -1).apply {
                isVirtual = true
                tableAlign = TableCellAlign.CENTER
            }
            alColumnCatalogCount.add(cd)
        }
        columnCatalogAllCount = ColumnDouble(modelTableName, "_all_count", "ВСЕГО", 10, -1).apply {
            isVirtual = true
            tableAlign = TableCellAlign.CENTER
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnRecordFullName)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnActive!!)
        alTableHiddenColumn.add(columnArchive!!)

        addTableColumn(columnRecordType)
        addTableColumn(columnRecordFullName)
        addTableColumn(columnCatalogIsMark)
        addTableColumn(columnCatalogPriceOut)
        addTableColumn(columnCatalogRowCount)
        alColumnCatalogCount.forEach {
            addTableColumn(it)
        }
        addTableColumn(columnCatalogAllCount)
        if (isMerchant) {
            addTableColumn(columnIsProduction)
            addTableColumn(columnProfitAdd)
            addTableColumn(columnCatalogPriceDate)
            addTableColumn(columnCatalogPriceIn)
        } else {
            alTableHiddenColumn.add(columnIsProduction)
            alTableHiddenColumn.add(columnProfitAdd)
            alTableHiddenColumn.add(columnCatalogPriceDate)
            alTableHiddenColumn.add(columnCatalogPriceIn)
        }

        //--- порядок полей оптимизирован для быстрого добавления данных в справочник
        alFormHiddenColumn.add(columnActive!!)
        alFormHiddenColumn.add(columnArchive!!)
        alFormHiddenColumn.add(columnRecordType)

        alFormColumn.add(columnRecordFullName)
        alFormColumn.add(columnCatalogIsMark)
        if (isMerchant) {
            alFormColumn.add(columnIsProduction)
            alFormColumn.add(columnProfitAdd)
            alFormColumn.add(columnCatalogPriceDate)
            alFormColumn.add(columnCatalogPriceIn)
            alFormColumn.add(columnCatalogPriceOut)
        }
        alFormColumn.add(columnParentFullName)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        addTableSort(columnRecordType, true)
        addTableSort(columnRecordFullName, true)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- определён в предке
        //alChildData.add( new ChildData( aliasConfig.getAlias(), columnID, true, true ) );
        alChildData.add(ChildData("shop_price", columnId, true))
        alChildData.add(ChildData("shop_price_in", columnId))
        alChildData.add(ChildData("shop_price_out", columnId))
        alChildData.add(ChildData("shop_report_warehouse_state", columnId, AppAction.FORM, true))
        alChildData.add(ChildData("shop_report_operation_summary", columnId, AppAction.FORM))
        alChildData.add(ChildData("shop_report_doc_content", columnId, AppAction.FORM))
        alChildData.add(ChildData("shop_report_operation_history", columnId, AppAction.FORM))
        DocumentTypeConfig.fillDocContentChild(alChildData, columnId)

        //--- определён в предке
        //alDependData.add( new DependData( tableName, columnParent.getFieldName() ) );
        alDependData.add(DependData("SHOP_doc_content", "sour_id"))//, DependData.DELETE ) );
        alDependData.add(DependData("SHOP_doc_content", "dest_id"))//, DependData.DELETE ) );
        alDependData.add(DependData("SHOP_price", "catalog_id", DependData.DELETE))

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        //--- определён в предке
        //expandParentIDColumn = columnParent;
        expandParentNameColumn = columnRecordFullName
    }
}

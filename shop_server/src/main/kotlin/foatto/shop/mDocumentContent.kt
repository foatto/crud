package foatto.shop

import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection

class mDocumentContent : mAbstract() {

    companion object {
        const val ADD_OVER_MARK_CODE = "add_over_mark_code"
    }

    lateinit var columnEditTime: ColumnDateTimeInt
        private set

    lateinit var columnDocumentContentIsDeleted: ColumnBoolean
        private set

    lateinit var columnDocument: ColumnInt
        private set
    lateinit var columnDocumentType: ColumnComboBox
        private set
    lateinit var columnDocumentDate: ColumnDate3Int
        private set

    lateinit var columnSourCatalog: ColumnInt
        private set
    lateinit var columnSourCatalogName: ColumnString
        private set
    lateinit var columnSourCatalogPriceOut: ColumnDouble
        private set

    lateinit var columnDestCatalog: ColumnInt
        private set
    lateinit var columnDestCatalogName: ColumnString
        private set
    lateinit var columnDestCatalogPriceOut: ColumnDouble
        private set

    lateinit var columnSourNum: ColumnDouble
        private set
    lateinit var columnDestNum: ColumnDouble
        private set
    lateinit var columnCostOut: ColumnDouble
        private set

    lateinit var columnSkipMark: ColumnBoolean
        private set
    lateinit var columnMarkCode: ColumnString
        private set

    lateinit var columnToArchive: ColumnBoolean
        private set

    lateinit var columnResort2Reprice: ColumnBoolean
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?,
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        var docID: Int? = null
        for (name in DocumentTypeConfig.hmAliasDocType.keys) {
            docID = hmParentData[name]
            if (docID != null) break
        }

        val hmAliasConfigs = application.getAliasConfig(conn)

        val alWarehouse = mWarehouse.fillWarehouseList(conn)

        val docType = DocumentTypeConfig.hmAliasDocType[aliasConfig.name]

        val isUseSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains(docType)
        val isUseDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains(docType)
        val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(docType)
        val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(docType)
        val isUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(docType)
        val isUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(docType)

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission[aliasConfig.name]
        //--- при добавлении модуля в систему прав доступа к нему ещё нет
        val isAuditMode = hsPermission?.contains(cDocumentContent.PERM_AUDIT_MODE) ?: false

        val isAddOverMarkCode = hmParam[ADD_OVER_MARK_CODE]?.toIntOrNull() == 1

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_doc_content"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnCreateTime = ColumnDateTimeInt(modelTableName, "create_time", "Создание", true, zoneId).apply {
            isEditable = false
        }
        columnEditTime = ColumnDateTimeInt(modelTableName, "edit_time", "Изменение", true, zoneId).apply {
            isEditable = false
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDocumentContentIsDeleted = ColumnBoolean(modelTableName, "is_deleted", "Не считать", false)

        val columnDocumentID = ColumnInt("SHOP_doc", "id")
        columnDocument = ColumnInt(modelTableName, "doc_id", columnDocumentID, docID)

        columnDocumentType = ColumnComboBox("SHOP_doc", "doc_type", "Тип накладной").apply {
            for ((dt, an) in DocumentTypeConfig.hmDocTypeAlias) {
                addChoice(dt, hmAliasConfigs[an]?.descr ?: "(неизвестный тип накладной = $dt : '$an')")
            }
        }
        columnDocumentDate = ColumnDate3Int("SHOP_doc", "doc_ye", "doc_mo", "doc_da", "Дата")

        val columnWarehouseSour = if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType)) {
            ColumnComboBox("SHOP_doc", "sour_id", if (docType == DocumentTypeConfig.TYPE_RESORT) "Склад / магазин" else "Со склада / магазина").apply {
                addChoice(0, "")
                alWarehouse.forEach { wh ->
                    addChoice(wh.first, wh.second)
                }
            }
        } else {
            ColumnInt("SHOP_doc", "sour_id", 0)
        }

        val columnWarehouseDest = if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType)) {
            ColumnComboBox("SHOP_doc", "dest_id", "На склад / магазин").apply {
                addChoice(0, "")
                alWarehouse.forEach { wh ->
                    addChoice(wh.first, wh.second)
                }
            }
        } else {
            ColumnInt("SHOP_doc", "dest_id", 0)
        }

        val columnDocumentNo = ColumnString("SHOP_doc", "doc_no", "№ накладной", STRING_COLUMN_WIDTH)
        val columnClientID = ColumnInt("SHOP_client", "id")
        val columnClient = ColumnInt("SHOP_doc", "client_id", columnClientID)
        val columnClientName = ColumnString("SHOP_client", "name", "Контрагент", STRING_COLUMN_WIDTH)
        val columnDocumentDescr = ColumnString("SHOP_doc", "descr", "Примечание", STRING_COLUMN_WIDTH)

        val selfLinkSourTableName = "SHOP_catalog_1"
        val columnSourCatalogID = ColumnInt(selfLinkSourTableName, "id").apply {
            selfLinkTableName = "SHOP_catalog"
        }
        columnSourCatalog = ColumnInt(modelTableName, "sour_id", columnSourCatalogID)
        columnSourCatalogName = ColumnString(
            aTableName = selfLinkSourTableName,
            aFieldName = "name",
            aCaption = if (docType == DocumentTypeConfig.TYPE_ALL || docType == DocumentTypeConfig.TYPE_RESORT) "Исх. наименование" else "Наименование",
            aRows = 3,
            aCols = STRING_COLUMN_WIDTH,
            aMaxSize = textFieldMaxSize
        ).apply {
            selfLinkTableName = "SHOP_catalog"  // для правильной работы селектора с подстановочной таблицей
            isRequired = if (isAddOverMarkCode) {
                false
            } else {
                isUseSourCatalog
            }
            selectorAlias = "shop_catalog_item"
            addSelectorColumn(columnSourCatalog, columnSourCatalogID)
            addSelectorColumn(this)//, columnCatalogName );
            isAutoStartSelector = !isAddOverMarkCode
        }

        columnSourCatalogPriceOut = ColumnDouble(modelTableName, "_price_out_sour", "Цена", 10, 2).apply {
            isVirtual = true
            tableAlign = TableCellAlign.RIGHT
            isEditable = false
        }
        val selfLinkDestTableName = "SHOP_catalog_2"
        val columnDestCatalogID = ColumnInt(selfLinkDestTableName, "id").apply {
            selfLinkTableName = "SHOP_catalog"
        }
        columnDestCatalog = ColumnInt(modelTableName, "dest_id", columnDestCatalogID)
        columnDestCatalogName = ColumnString(
            aTableName = selfLinkDestTableName,
            aFieldName = "name",
            aCaption = if (docType == DocumentTypeConfig.TYPE_ALL || docType == DocumentTypeConfig.TYPE_RESORT) "Вх. наименование" else "Наименование",
            aRows = 3,
            aCols = STRING_COLUMN_WIDTH,
            aMaxSize = textFieldMaxSize
        ).apply {
            selfLinkTableName = "SHOP_catalog"  // для правильной работы селектора с подстановочной таблицей
            isRequired = if (isAddOverMarkCode) false else isUseDestCatalog

            selectorAlias = "shop_catalog_item"
            addSelectorColumn(columnDestCatalog, columnDestCatalogID)
            addSelectorColumn(this)//, columnCatalogName );
            isAutoStartSelector = !isAddOverMarkCode
        }
        columnDestCatalogPriceOut = ColumnDouble(modelTableName, "_price_out_dest", "Цена", 10, 2).apply {
            isVirtual = true
            tableAlign = TableCellAlign.RIGHT
            isEditable = false
        }

        columnSourNum = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "sour_num",
            aCaption = if (docType == DocumentTypeConfig.TYPE_ALL) "Исх. кол-во" else "Кол-во",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = if (isUseSourNum) 1.0 else 0.0
        ).apply {
            tableAlign = TableCellAlign.CENTER
            if (isUseSourNum /*&& docType != DocumentTypeConfig.TYPE_RESORT*/) {
                minValue = 0.1
            }
        }

        columnDestNum = ColumnDouble(
            aTableName = modelTableName,
            aFieldName = "dest_num",
            aCaption = if (docType == DocumentTypeConfig.TYPE_ALL) "Вх. кол-во" else "Кол-во",
            aCols = 10,
            aPrecision = -1,
            aDefaultValue = if (isUseDestNum) 1.0 else 0.0
        ).apply {
            tableAlign = TableCellAlign.CENTER
            if (isUseDestNum /*&& docType != DocumentTypeConfig.TYPE_RESORT*/) {
                minValue = 0.1
            }
        }

        columnCostOut = ColumnDouble(modelTableName, "_doc_cost_out", "Сумма [руб.]", 10, 2).apply {
            isVirtual = true
            tableAlign = TableCellAlign.RIGHT
        }

        columnSkipMark = ColumnBoolean(modelTableName, "_skip_mark", "Пропустить маркировку", false).apply {
            isVirtual = true
        }

        columnMarkCode = ColumnString(
            aTableName = modelTableName,
            aFieldName = "mark_code",
            aCaption = "Код маркировки",
            aRows = 3,
            aCols = STRING_COLUMN_WIDTH,
            aMaxSize = textFieldMaxSize
        ).apply {
            colSpan = 4
        }

        columnToArchive = ColumnBoolean(modelTableName, "_to_archive", "Перенести исх. товар в архив", false).apply {
            isVirtual = true
//            setSavedDefault( userConfig )
        }

        columnResort2Reprice = ColumnBoolean(modelTableName, "_resort_2_reprice", "Пересортицу в переоценку", false).apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnDocumentContentIsDeleted)
        alTableHiddenColumn.add(columnDocument)
        alTableHiddenColumn.add(columnClient)
        alTableHiddenColumn.add(columnSourCatalog)
        alTableHiddenColumn.add(columnDestCatalog)
        alTableHiddenColumn.add(columnToArchive)
        alTableHiddenColumn.add(columnResort2Reprice)

        if (isAuditMode) {
            addTableColumn(columnCreateTime)
            addTableColumn(columnEditTime)
        } else {
            alTableHiddenColumn.add(columnCreateTime)
            alTableHiddenColumn.add(columnEditTime)
        }

        //--- различный порядок показа полей в зависимости от того, откуда пришли
        //--- переход от каталога по какому-либо элементу номенклатуры
        if (docID == null && docType == DocumentTypeConfig.TYPE_ALL) {
            addTableColumn(columnDocumentType)
        } else {
            alTableHiddenColumn.add(columnDocumentType)
        }
        if (docID == null) {
            if (isUseSourWarehouse) {
                addTableColumn(columnWarehouseSour)
            } else {
                alTableHiddenColumn.add(columnWarehouseSour)
            }
            if (isUseDestWarehouse) {
                addTableColumn(columnWarehouseDest)
            } else {
                alTableHiddenColumn.add(columnWarehouseDest)
            }
            addTableColumn(columnDocumentNo)
            addTableColumn(columnDocumentDate)
            if (DocumentTypeConfig.hsUseClient.contains(docType)) {
                addTableColumn(columnClientName)
            } else {
                alTableHiddenColumn.add(columnClientName)
            }
            addTableColumn(columnDocumentDescr)
        } else {
            alTableHiddenColumn.add(columnWarehouseSour)
            alTableHiddenColumn.add(columnWarehouseDest)
            alTableHiddenColumn.add(columnDocumentNo)
            alTableHiddenColumn.add(columnDocumentDate)
            alTableHiddenColumn.add(columnClientName)
            alTableHiddenColumn.add(columnDocumentDescr)
        }
        if (isUseSourCatalog) {
            addTableColumn(columnSourCatalogName)
            addTableColumn(columnSourCatalogPriceOut)
        } else {
            alTableHiddenColumn.add(columnSourCatalogName)
            alTableHiddenColumn.add(columnSourCatalogPriceOut)
        }

        if (isUseSourNum) {
            addTableColumn(columnSourNum)
        } else {
            alTableHiddenColumn.add(columnSourNum)
        }

        if (isUseDestCatalog) {
            addTableColumn(columnDestCatalogName)
            addTableColumn(columnDestCatalogPriceOut)
        } else {
            alTableHiddenColumn.add(columnDestCatalogName)
            alTableHiddenColumn.add(columnDestCatalogPriceOut)
        }
        if (isUseDestNum) {
            addTableColumn(columnDestNum)
        } else {
            alTableHiddenColumn.add(columnDestNum)
        }

        addTableColumn(columnCostOut)

        addTableColumnHorizNew(columnMarkCode)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnDocument)
        alFormHiddenColumn.add(columnClient)
        alFormHiddenColumn.add(columnSourCatalog)
        alFormHiddenColumn.add(columnDestCatalog)

        if (isAuditMode && !isAddOverMarkCode) {
            alFormColumn.add(columnCreateTime)
            alFormColumn.add(columnEditTime)
        } else {
            alFormHiddenColumn.add(columnCreateTime)
            alFormHiddenColumn.add(columnEditTime)
        }
        //--- если это не состав конкретной накладной, то показывать необходимые подробности,
        //--- которых не будет в заголовке
        if (docID == null) {
            alFormColumn.add(columnDocumentType)
            (if (isUseSourWarehouse) alFormColumn else alFormHiddenColumn).add(columnWarehouseSour)
            (if (isUseDestWarehouse) alFormColumn else alFormHiddenColumn).add(columnWarehouseDest)
            alFormColumn.add(columnDocumentNo)
            alFormColumn.add(columnDocumentDate)
            (if (DocumentTypeConfig.hsUseClient.contains(docType)) alFormColumn else alFormHiddenColumn).add(columnClientName)
            alFormColumn.add(columnDocumentDescr)
        }
        //--- в отличие от таблицы даже "ненужные" поля используются в form-методах,
        else {
            alFormHiddenColumn.add(columnDocumentType)
            alFormHiddenColumn.add(columnWarehouseSour)
            alFormHiddenColumn.add(columnWarehouseDest)
            alFormHiddenColumn.add(columnDocumentNo)
            alFormHiddenColumn.add(columnDocumentDate)
            alFormHiddenColumn.add(columnClientName)
            alFormHiddenColumn.add(columnDocumentDescr)
        }

        //--- так что лучше их оставить в невидимой части
        (if (isUseSourCatalog && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnSourCatalogName)
        (if (isUseSourCatalog && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnSourCatalogPriceOut)
        (if (isUseDestCatalog && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnDestCatalogName)
        (if (isUseDestCatalog && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnDestCatalogPriceOut)
        (if (isUseSourNum && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnSourNum)
        (if (isUseDestNum && !isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnDestNum)

        (if (!isAddOverMarkCode && docType != DocumentTypeConfig.TYPE_MOVE) alFormColumn else alFormHiddenColumn).add(columnSkipMark)

        (if (isAddOverMarkCode || docType != DocumentTypeConfig.TYPE_MOVE) alFormColumn else alFormHiddenColumn).add(columnMarkCode)

        //--- перенос в архив - только при добавлении пересортице
        (if (docType == DocumentTypeConfig.TYPE_RESORT && id == 0) alFormColumn else alFormHiddenColumn).add(columnToArchive)
        //--- преобразование пересортицы в переоценку - только при пересохранении переоценки
        (if (docType == DocumentTypeConfig.TYPE_RESORT && id != 0) alFormColumn else alFormHiddenColumn).add(columnResort2Reprice)

        (if (!isAddOverMarkCode) alFormColumn else alFormHiddenColumn).add(columnDocumentContentIsDeleted)

        //----------------------------------------------------------------------------------------------------------------------

        //--- переход от каталога
        if (docID == null) {
            addTableSort(columnDocumentDate, false)
            addTableSort(columnDocumentNo, false)
        }
        //--- переход от документа
        else {
            if (isUseSourCatalog) {
                addTableSort(columnSourCatalogName, true)
            }
            if (isUseDestCatalog) {
                addTableSort(columnDestCatalogName, true)
            }
        }

        //----------------------------------------------------------------------------------------

        //hmParentColumn.put(  "shop_client", columnClient  ); - накуа?
        for (an in DocumentTypeConfig.hmDocTypeAlias.values) {
            hmParentColumn[an] = columnDocument
        }

        //--- из-за нестандартной обработки shop_doc_content_all/move/_resort паренты от shop_catalog будут обрабатываться отдельно в addSQLWhere
        // hmParentColumn.put( "shop_catalog", columnSourCatalog );
        // hmParentColumn.put( "shop_catalog_folder", columnSourCatalog );
        // hmParentColumn.put( "shop_catalog_item", columnSourCatalog );
    }
}

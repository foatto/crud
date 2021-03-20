package foatto.shop

import foatto.core.link.AppAction
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mDocument : mAbstract() {

    lateinit var columnEditTime: ColumnDateTimeInt
        private set

    lateinit var columnDocumentIsDeleted: ColumnBoolean
        private set

    lateinit var columnDocumentNo: ColumnString
        private set
    lateinit var columnDocumentType: ColumnComboBox
        private set
    lateinit var columnWarehouseSour: iColumn
        private set
    lateinit var columnWarehouseDest: iColumn
        private set
    lateinit var columnDocumentDate: ColumnDate3Int
        private set

    lateinit var columnDocumentDiscount: ColumnDouble
        private set

    lateinit var columnDocumentRowCount: ColumnInt
        private set
    lateinit var columnDocumentCostOut: ColumnDouble
        private set

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val docType = DocumentTypeConfig.hmAliasDocType[aliasConfig.alias]!!
        val hmAliasConfig = AliasConfig.getConfig(stm)
        val alWarehouse = mWarehouse.fillWarehouseList(stm)
        val parentWarehouseId = hmParentData["shop_warehouse"]

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission[aliasConfig.alias]
        //--- при добавлении модуля в систему прав доступа к нему ещё нет
        val isAuditMode = hsPermission?.contains(cDocument.PERM_AUDIT_MODE) ?: false

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "SHOP_doc"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnCreateTime = ColumnDateTimeInt(tableName, "create_time", "Создание", true, zoneId).apply {
            isEditable = false
        }
        columnEditTime = ColumnDateTimeInt(tableName, "edit_time", "Изменение", true, zoneId).apply {
            isEditable = false
        }
        val columnContentEditTime = ColumnDateTimeInt(tableName, "content_edit_time", "Изменение состава", true, zoneId).apply {
            isEditable = false
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDocumentIsDeleted = ColumnBoolean(tableName, "is_deleted", "Не считать", false)

        columnDocumentType = ColumnComboBox(tableName, "doc_type", "Тип накладной", docType).apply {
            isEditable = false    // это информационное поле, значение устанавливается программно
            for ((dt, an) in DocumentTypeConfig.hmDocTypeAlias) {
                addChoice(dt, hmAliasConfig[an]!!.descr)
            }
        }

        columnWarehouseSour = if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType) && parentWarehouseId == null) {
            ColumnComboBox(tableName, "sour_id", if (docType == DocumentTypeConfig.TYPE_RESORT) "Склад / магазин" else "Со склада / магазина").apply {
                if (docType == DocumentTypeConfig.TYPE_ALL) {
                    addChoice(0, "")
                }
                alWarehouse.forEach { wh ->
                    addChoice(wh.first, wh.second)
                }
                defaultValue = if (docType == DocumentTypeConfig.TYPE_ALL) {
                    0
                } else {
                    alWarehouse[0].first
                }
            }
        } else {
            ColumnInt(tableName, "sour_id", parentWarehouseId ?: 0)
        }

        columnWarehouseDest = if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType) && parentWarehouseId == null) {
            ColumnComboBox(tableName, "dest_id", "На склад / магазин").apply {
                if (docType == DocumentTypeConfig.TYPE_ALL) {
                    addChoice(0, "")
                }
                alWarehouse.forEach { wh ->
                    addChoice(wh.first, wh.second)
                }
                defaultValue = if (docType == DocumentTypeConfig.TYPE_ALL) {
                    0
                } else {
                    alWarehouse[0].first
                }
            }
        } else {
            ColumnInt(tableName, "dest_id", parentWarehouseId ?: 0)
        }

        columnDocumentNo = ColumnString(tableName, "doc_no", "№ накладной", STRING_COLUMN_WIDTH).apply {
            tableCaption = "№ накл."
        }

        columnDocumentDate = ColumnDate3Int(tableName, "doc_ye", "doc_mo", "doc_da", "Дата")

        val columnClientID = ColumnInt("SHOP_client", "id")
        val columnClient = ColumnInt(tableName, "client_id", columnClientID)
        val columnClientName = ColumnString("SHOP_client", "name", "Клиент", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "shop_client"
            addSelectorColumn(columnClient, columnClientID)
            addSelectorColumn(this)
        }

        val columnDocumentDescr = ColumnString(tableName, "descr", "Примечание", STRING_COLUMN_WIDTH).apply {
            tableCaption = "Прим."
        }

        columnDocumentDiscount = ColumnDouble(tableName, "discount", "Скидка [%]", 10, 1, 0.0).apply {
            setEmptyData(0.0, "-")
            minValue = 0.0
            maxValue = 100.0
            tableCaption = "[%]"
            tableAlign = TableCellAlign.CENTER
        }

        columnDocumentRowCount = ColumnInt(tableName, "_row_count", "Кол-во наименований", 10).apply {
            isVirtual = true
            tableCaption = "Кол-во наим."
            tableAlign = TableCellAlign.CENTER
        }
        columnDocumentCostOut = ColumnDouble(tableName, "_doc_cost_out", "Сумма [руб.]", 10, 2).apply {
            isVirtual = true
            tableCaption = "[руб.]"
            tableAlign = TableCellAlign.RIGHT
        }

        val columnIsFiscaled = ColumnBoolean(tableName, "is_fiscaled", "", false)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnClient)
        alTableHiddenColumn.add(columnDocumentIsDeleted)

        alTableGroupColumn.add(columnDocumentDate)

        if (isAuditMode) {
            addTableColumn(columnCreateTime)
            addTableColumn(columnEditTime)
            addTableColumn(columnContentEditTime)
        } else {
            alTableHiddenColumn.add(columnCreateTime)
            alTableHiddenColumn.add(columnEditTime)
            alTableHiddenColumn.add(columnContentEditTime)
        }

        if (docType == DocumentTypeConfig.TYPE_ALL) {
            addTableColumn(columnDocumentType)
        } else {
            alTableHiddenColumn.add(columnDocumentType)
        }

        if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType) && parentWarehouseId == null) {
            addTableColumn(columnWarehouseSour)
        } else {
            alTableHiddenColumn.add(columnWarehouseSour)
        }

        if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType) && parentWarehouseId == null) {
            addTableColumn(columnWarehouseDest)
        } else {
            alTableHiddenColumn.add(columnWarehouseDest)
        }

        addTableColumn(columnDocumentNo)

        if (DocumentTypeConfig.hsUseClient.contains(docType)) {
            addTableColumn(columnClientName)
        } else {
            alTableHiddenColumn.add(columnClientName)
        }

        addTableColumn(columnDocumentDescr)
        addTableColumn(columnDocumentRowCount)

        if (docType == DocumentTypeConfig.TYPE_OUT || docType == DocumentTypeConfig.TYPE_ALL) {
            addTableColumn(columnDocumentDiscount)
        } else {
            alTableHiddenColumn.add(columnDocumentDiscount)
        }

        addTableColumn(columnDocumentCostOut)

        //----------------------------------------------------------------------------------------------------------------------

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnClient)
        alFormHiddenColumn.add(columnIsFiscaled)

        if (isAuditMode) {
            alFormColumn.add(columnCreateTime)
            alFormColumn.add(columnEditTime)
            alFormColumn.add(columnContentEditTime)
        } else {
            alFormHiddenColumn.add(columnCreateTime)
            alFormHiddenColumn.add(columnEditTime)
            alFormHiddenColumn.add(columnContentEditTime)
        }

        //--- тип накладной обычно написан в заголовке
        (if (docType == DocumentTypeConfig.TYPE_ALL) alFormColumn else alFormHiddenColumn).add(columnDocumentType)

        (if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType) && parentWarehouseId == null) alFormColumn else alFormHiddenColumn).add(columnWarehouseSour)
        (if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType) && parentWarehouseId == null) alFormColumn else alFormHiddenColumn).add(columnWarehouseDest)
        alFormColumn.add(columnDocumentNo)
        alFormColumn.add(columnDocumentDate)
        (if (DocumentTypeConfig.hsUseClient.contains(docType)) alFormColumn else alFormHiddenColumn).add(columnClientName)
        alFormColumn.add(columnDocumentDescr)
        (if (docType == DocumentTypeConfig.TYPE_OUT || docType == DocumentTypeConfig.TYPE_RETURN_OUT || docType == DocumentTypeConfig.TYPE_ALL)
            alFormColumn else alFormHiddenColumn).add(columnDocumentDiscount)
        alFormColumn.add(columnDocumentIsDeleted)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDocumentDate)
        alTableSortDirect.add("DESC")
        alTableSortColumn.add(columnDocumentNo)
        alTableSortDirect.add("DESC")

        //----------------------------------------------------------------------------------------

        //--- из-за нестандартной обработки shop_doc_all/move/_resort паренты от shop_warehouse будут обрабатываться отдельно в addSQLWhere
        // hmParentColumn.put( "shop_warehouse", ... );
        hmParentColumn["shop_client"] = columnClient

        //----------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData(DocumentTypeConfig.hmAliasChild[aliasConfig.alias]!!, columnID!!, true, true))
        alChildData.add(ChildData("Отчёты", "shop_report_doc_content", columnID!!, AppAction.FORM, true))

        //----------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc_content", "doc_id"))
    }
}
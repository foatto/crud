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
import foatto.sql.CoreAdvancedConnection

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
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        val docType = DocumentTypeConfig.hmAliasDocType[aliasConfig.name]!!
        val hmAliasConfigs = application.getAliasConfig(conn)
        val alWarehouse = mWarehouse.fillWarehouseList(conn)
        val parentWarehouseId = hmParentData["shop_warehouse"]

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission[aliasConfig.name]
        //--- при добавлении модуля в систему прав доступа к нему ещё нет
        val isAuditMode = hsPermission?.contains(cDocument.PERM_AUDIT_MODE) ?: false

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SHOP_doc"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        val columnCreateTime = ColumnDateTimeInt(modelTableName, "create_time", "Создание", true, zoneId).apply {
            isEditable = false
        }
        columnEditTime = ColumnDateTimeInt(modelTableName, "edit_time", "Изменение", true, zoneId).apply {
            isEditable = false
        }
        val columnContentEditTime = ColumnDateTimeInt(modelTableName, "content_edit_time", "Изменение состава", true, zoneId).apply {
            isEditable = false
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDocumentIsDeleted = ColumnBoolean(modelTableName, "is_deleted", "Не считать", false)

        columnDocumentType = ColumnComboBox(modelTableName, "doc_type", "Тип накладной", docType).apply {
            isEditable = false    // это информационное поле, значение устанавливается программно
            for ((dt, an) in DocumentTypeConfig.hmDocTypeAlias) {
                addChoice(dt, hmAliasConfigs[an]?.descr ?: "(неизвестный тип накладной = $dt : '$an')")
            }
        }

        columnWarehouseSour = if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType) && parentWarehouseId == null) {
            ColumnComboBox(modelTableName, "sour_id", if (docType == DocumentTypeConfig.TYPE_RESORT) "Склад / магазин" else "Со склада / магазина").apply {
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
            ColumnInt(modelTableName, "sour_id", parentWarehouseId ?: 0)
        }

        columnWarehouseDest = if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType) && parentWarehouseId == null) {
            ColumnComboBox(modelTableName, "dest_id", "На склад / магазин").apply {
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
            ColumnInt(modelTableName, "dest_id", parentWarehouseId ?: 0)
        }

        columnDocumentNo = ColumnString(modelTableName, "doc_no", "№ накладной", STRING_COLUMN_WIDTH).apply {
            tableCaption = "№ накл."
        }

        columnDocumentDate = ColumnDate3Int(modelTableName, "doc_ye", "doc_mo", "doc_da", "Дата")

        val columnClientID = ColumnInt("SHOP_client", "id")
        val columnClient = ColumnInt(modelTableName, "client_id", columnClientID)
        val columnClientName = ColumnString("SHOP_client", "name", "Клиент", STRING_COLUMN_WIDTH).apply {
            selectorAlias = "shop_client"
            addSelectorColumn(columnClient, columnClientID)
            addSelectorColumn(this)
        }

        val columnDocumentDescr = ColumnString(modelTableName, "descr", "Примечание", STRING_COLUMN_WIDTH).apply {
            tableCaption = "Прим."
        }

        columnDocumentDiscount = ColumnDouble(modelTableName, "discount", "Скидка [%]", 10, 1, 0.0).apply {
            setEmptyData(0.0, "-")
            minValue = 0.0
            maxValue = 100.0
            tableCaption = "[%]"
            tableAlign = TableCellAlign.CENTER
        }

        columnDocumentRowCount = ColumnInt(modelTableName, "_row_count", "Кол-во наименований", 10).apply {
            isVirtual = true
            tableCaption = "Кол-во наим."
            tableAlign = TableCellAlign.CENTER
        }
        columnDocumentCostOut = ColumnDouble(modelTableName, "_doc_cost_out", "Сумма [руб.]", 10, 2).apply {
            isVirtual = true
            tableCaption = "[руб.]"
            tableAlign = TableCellAlign.RIGHT
        }

        val columnIsFiscaled = ColumnBoolean(modelTableName, "is_fiscaled", "", false)

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
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

        alFormHiddenColumn.add(columnId)
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

        addTableSort(columnDocumentDate, false)
        addTableSort(columnDocumentNo, false)

        //----------------------------------------------------------------------------------------

        //--- из-за нестандартной обработки shop_doc_all/move/_resort паренты от shop_warehouse будут обрабатываться отдельно в addSQLWhere
        // hmParentColumn.put( "shop_warehouse", ... );
        hmParentColumn["shop_client"] = columnClient

        //----------------------------------------------------------------------------------------------------------------------

        alChildData += ChildData(
            aAlias = DocumentTypeConfig.hmAliasChild[aliasConfig.name]!!,
            aColumn = columnId,
            aNewGroup = true,
            aDefaultOperation = true
        )
        alChildData += ChildData(
            aGroup = "Отчёты",
            aAlias = "shop_report_doc_content",
            aColumn = columnId,
            aAction = AppAction.FORM,
            aNewGroup = true
        )

        //----------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData("SHOP_doc_content", "doc_id"))
    }
}
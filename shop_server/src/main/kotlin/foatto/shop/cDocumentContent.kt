package foatto.shop

import foatto.core.app.ICON_NAME_ADD_ITEM
import foatto.core.app.ICON_NAME_PRINT
import foatto.core.link.*
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_DMY
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.*
import foatto.shop.mDocumentContent.Companion.ADD_OVER_MARK_CODE
import foatto.shop_core.app.ICON_NAME_ADD_MARKED_ITEM
import foatto.shop_core.app.ICON_NAME_CALC
import foatto.shop_core.app.ICON_NAME_FISCAL
import foatto.sql.CoreAdvancedStatement
import java.util.concurrent.ConcurrentHashMap

class cDocumentContent : cStandart() {

    companion object {
        const val PERM_AUDIT_MODE = "audit_mode"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var docId: Int? = null
    private var docType = DocumentTypeConfig.TYPE_ALL
    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

    private var docCost = 0.0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        for (name in DocumentTypeConfig.hmAliasDocType.keys) {
            docId = hmParentData[name]
            if (docId != null) break
        }
        docType = DocumentTypeConfig.hmAliasDocType[aliasConfig.alias]!!
        hmPrice = PriceData.loadPrice(
            stm, /*if( docType == DocumentTypeConfig.TYPE_IN ||
                                                                  docType == DocumentTypeConfig.TYPE_RETURN_IN )
                                                                   mPrice.PRICE_TYPE_IN
                                                              else*/ mPrice.PRICE_TYPE_OUT
        )
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа как флаг работы в режиме аудита
        alPermission.add(Pair(PERM_AUDIT_MODE, "20 Audit mode"))
    }

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        //--- а нет ли парентов от иерархической номенклатуры?
        var pid = getParentID("shop_catalog")
        if (pid == null) pid = getParentID("shop_catalog_folder")
        if (pid == null) pid = getParentID("shop_catalog_item")
        //--- если есть, то вероятно раскроем parentID группы номенклатуры в список parentID входящих элементов
        val parentWhere = StringBuilder()
        if (pid != null) {
            val hsID = expandParent("shop_catalog", pid)
            for (tmpID in hsID)
                parentWhere.append(if (parentWhere.isEmpty()) "" else " , ").append(tmpID)
            parentWhere.insert(0, if (hsID.size == 1) " = " else " IN ( ")
            if (hsID.size > 1) parentWhere.append(" ) ")
        }

        val mdc = model as mDocumentContent
        val tableName = renameTableName(hsTableRenameList, model.tableName)

        var sSQL = ""

        if (docType != DocumentTypeConfig.TYPE_ALL) {
            val docTableName = renameTableName(hsTableRenameList, mdc.columnDocumentType.tableName)
            sSQL += " AND $docTableName.${mdc.columnDocumentType.getFieldName()} = $docType "
        }

        //--- добавить свои ограничения по parent-данным от shop_catalog (из-за нестандартной обработки shop_doc_all/move/_resort)
        if (pid != null) {
            val sourFieldName = mdc.columnSourCatalog.getFieldName()
            val destFieldName = mdc.columnDestCatalog.getFieldName()
            val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(docType)
            val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(docType)

            sSQL += " AND "
            //--- используются оба номенклатурных поля
            if (isUseSourCatalog && isUseDestCatalog) sSQL += " ( $tableName.$sourFieldName $parentWhere OR $tableName.$destFieldName $parentWhere ) "
            else if (isUseSourCatalog) sSQL += " $tableName.$sourFieldName $parentWhere "
            else if (isUseDestCatalog) sSQL += " $tableName.$destFieldName $parentWhere "
        }
        return super.addSQLWhere(hsTableRenameList) + sSQL
    }

    override fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        var sHeader = aliasConfig.descr
        if (docId != null) {
            var docYe = 0
            var docMo = 0
            var docDa = 0
            var discount = 0.0
            var localDocType = 0
            val hmWarehouse = mWarehouse.fillWarehouseMap(stm)
            val rs = stm.executeQuery(
                " SELECT SHOP_doc.doc_no , SHOP_doc.doc_type , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
                    " SHOP_doc.sour_id , SHOP_doc.dest_id , SHOP_client.name , SHOP_doc.descr , SHOP_doc.discount " +
                    " FROM SHOP_doc , SHOP_client " +
                    " WHERE SHOP_doc.client_id = SHOP_client.id " +
                    " AND SHOP_doc.id = $docId "
            )
            if (rs.next()) {
                val docNo = rs.getString(1)
                localDocType = rs.getInt(2)
                docYe = rs.getInt(3)
                docMo = rs.getInt(4)
                docDa = rs.getInt(5)
                sHeader += ", № $docNo от ${DateTime_DMY(intArrayOf(docYe, docMo, docDa, 0, 0, 0))}"
                if (DocumentTypeConfig.hsUseSourWarehouse.contains(docType)) sHeader += ", со склада ${hmWarehouse[rs.getInt(6)]}"
                if (DocumentTypeConfig.hsUseDestWarehouse.contains(docType)) sHeader += ", на склад ${hmWarehouse[rs.getInt(7)]}"
                val clientName = rs.getString(8)
                if (clientName.isNotEmpty()) sHeader += ", $clientName"
                val descr = rs.getString(9)
                if (descr.isNotEmpty()) sHeader += ", $descr"
                discount = rs.getDouble(10)
                /*if (discount != 0.0) */sHeader += ", скидка $discount % "
            }
            rs.close()
            //--- подсчёт стоимости накладной
            docCost = cDocument.calcDocCountAndCost(stm, hmPrice, docId!!, localDocType, zoneId, docYe, docMo, docDa, discount).second
            sHeader += ", общая стоимость: ${getSplittedDouble(docCost, 2)}"
        }
        alPath.add(Pair("", sHeader))
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mdc = model as mDocumentContent

        //--- может быть вывод состава по нескольким накладным
        val rowDocDate = (hmColumnData[mdc.columnDocumentDate] as DataDate3Int).localDate
        val rowDocType = (hmColumnData[mdc.columnDocumentType] as DataComboBox).intValue
        val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(rowDocType)
        val isUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(rowDocType)
        val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(rowDocType)
        val isUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(rowDocType)

        if (docType == DocumentTypeConfig.TYPE_ALL) {
            hmColumnData[mdc.columnSourCatalogName]!!.isShowEmptyTableCell = !isUseSourCatalog
            hmColumnData[mdc.columnSourCatalogPriceOut]!!.isShowEmptyTableCell = !isUseSourCatalog
            hmColumnData[mdc.columnSourNum]!!.isShowEmptyTableCell = !isUseSourNum

            hmColumnData[mdc.columnDestCatalogName]!!.isShowEmptyTableCell = !isUseDestCatalog
            hmColumnData[mdc.columnDestCatalogPriceOut]!!.isShowEmptyTableCell = !isUseDestCatalog
            hmColumnData[mdc.columnDestNum]!!.isShowEmptyTableCell = !isUseDestNum
        }

        val price = PriceData.getPrice(
            hmPrice, (hmColumnData[if (isUseDestCatalog) mdc.columnDestCatalog else mdc.columnSourCatalog] as DataInt).intValue,
            zoneId, rowDocDate.year, rowDocDate.monthValue, rowDocDate.dayOfMonth
        )
        (hmColumnData[if (isUseDestCatalog) mdc.columnDestCatalogPriceOut else mdc.columnSourCatalogPriceOut] as DataDouble).doubleValue = price

        (hmColumnData[mdc.columnCostOut] as DataDouble).doubleValue = (hmColumnData[if (isUseDestNum) mdc.columnDestNum else mdc.columnSourNum] as DataDouble).doubleValue * price
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val mdc = model as mDocumentContent

        if ((hmColumnData[mdc.columnDocumentContentIsDeleted] as DataBoolean).value) {
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
        }
    }

    override fun getAddButtonURL(refererID: String, hmOut: MutableMap<String, Any>): MutableList<AddActionButton> {
        val alAddButtonList = mutableListOf<AddActionButton>()

        alAddButtonList.add(
            AddActionButton(
                caption = "Добавить",
                tooltip = "Добавить",
                icon = ICON_NAME_ADD_ITEM,
                url = getParamURL(aliasConfig.alias, AppAction.FORM, refererID, 0, hmParentData, parentUserID, null)
            )
        )

        if (docType in setOf(
                DocumentTypeConfig.TYPE_OUT,
                DocumentTypeConfig.TYPE_DESTROY,
                DocumentTypeConfig.TYPE_RETURN_IN,
                DocumentTypeConfig.TYPE_MOVE,
            )
        ) {
            alAddButtonList.add(
                AddActionButton(
                    caption = "Добавить маркированный товар",
                    tooltip = "Добавить маркированный товар",
                    icon = ICON_NAME_ADD_MARKED_ITEM,
                    url = getParamURL(aliasConfig.alias, AppAction.FORM, refererID, 0, hmParentData, parentUserID, "&$ADD_OVER_MARK_CODE=1")
                )
            )
        }
        return alAddButtonList
    }

    override fun getServerAction(): MutableList<ServerActionButton> {
        val alSAB = super.getServerAction()

        //--- проверяем на возможность печати чека
        //var isFiscable = true - for fiscal testing
        val isFiscable = docId?.let {
            docType == DocumentTypeConfig.TYPE_OUT &&
                it != 0 &&
                (application as iShopApplication).isDocumentFiscable(it)
        } ?: false

        //--- для накладных на реализацию добавим работу с онлайн-кассой
        if (isFiscable) {
            alSAB.add(
                ServerActionButton(
                    caption = "Кассовый чек",
                    tooltip = "Кассовый чек",
                    icon = ICON_NAME_FISCAL,
                    url = getParamURL("shop_fiscal_doc_content", AppAction.FORM, null, 0, hmParentData, null, ""),
                    inNewWindow = true
                )
            )
        }
        alSAB.add(
            ServerActionButton(
                caption = "Товарный чек",
                tooltip = "Товарный чек",
                icon = ICON_NAME_PRINT,
                url = getParamURL("shop_report_doc_content", AppAction.FORM, null, 0, hmParentData, null, ""),
                inNewWindow = true
            )
        )

        return alSAB
    }

    override fun getClientAction(): MutableList<ClientActionButton> {
        val alCAB = super.getClientAction()

        //--- для накладных на реализацию добавим клиентскую кнопку расчёта сдачи
        if (docType == DocumentTypeConfig.TYPE_OUT) {
            alCAB.add(
                ClientActionButton(
                    caption = "Рассчитать",
                    tooltip = "Рассчитать сдачу",
                    icon = ICON_NAME_CALC,
                    className = "foatto.shop.CashCalculator",
                    param = docCost.toString()
                )
            )
        }

        return alCAB
    }

    //--- при входе через каталог запрещаются все операции изменения (и удаления на всякий случай тоже) ---

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        val mdc = model as mDocumentContent
        val rowDocType = (hmColumnData[mdc.columnDocumentType] as DataComboBox).intValue
        val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(rowDocType)
        val catalogID = (hmColumnData[if (isUseDestCatalog) mdc.columnDestCatalog else mdc.columnSourCatalog] as DataInt).intValue

        if (catalogID != 0) {
            //--- может быть вывод состава по нескольким накладным
            val rowDocDate = (hmColumnData[mdc.columnDocumentDate] as DataDate3Int).localDate

            val price = PriceData.getPrice(hmPrice, catalogID, zoneId, rowDocDate.year, rowDocDate.monthValue, rowDocDate.dayOfMonth)
            (hmColumnData[if (isUseDestCatalog) mdc.columnDestCatalogPriceOut else mdc.columnSourCatalogPriceOut] as DataDouble).doubleValue = price
        }
    }

    override fun isAddEnabled(): Boolean = super.isAddEnabled() && docId != null

    override fun isEditEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean = super.isEditEnabled(hmColumnData, id) && docId != null

    override fun isDeleteEnabled(hmColumnData: Map<iColumn, iData>, id: Int): Boolean = super.isDeleteEnabled(hmColumnData, id) && docId != null

    override fun getSaveButtonParams(formParam: String): String {
        var saveParams = super.getSaveButtonParams(formParam)
        if(hmParam[ADD_OVER_MARK_CODE]?.toIntOrNull() == 1) {
            saveParams = AppParameter.setParam(saveParams, ADD_OVER_MARK_CODE, "1")
        }
        return saveParams
    }

    override fun getFormValues(id: Int, alFormData: List<FormData>, alColumnList: List<iColumn>, hmColumnData: MutableMap<iColumn, iData>): Boolean {
        if (super.getFormValues(id, alFormData, alColumnList, hmColumnData)) {
            //--- additive checking for markable products
            val mdc = model as mDocumentContent

            val skipMarkData = hmColumnData[mdc.columnSkipMark] as DataBoolean
            val isSkipMark = skipMarkData.value

            val markData = hmColumnData[mdc.columnMarkCode] as DataString
            val markCode = markData.text

            if (isSkipMark) {
                if (markCode.isNotEmpty()) {
                    //--- clear mark data for avoid errorprone duplicate him
                    markData.setError(markCode, "Удалите код, чтобы пропустить маркировку!")
                    return false
                }
            } else {
                val sourCatalogData = hmColumnData[mdc.columnSourCatalog] as DataInt
                val destCatalogData = hmColumnData[mdc.columnDestCatalog] as DataInt

                val sourNumData = hmColumnData[mdc.columnSourNum] as DataDouble
                val destNumData = hmColumnData[mdc.columnDestNum] as DataDouble

                when (docType) {
                    //--- income or return of saled product
                    DocumentTypeConfig.TYPE_IN,
                    DocumentTypeConfig.TYPE_RETURN_OUT
                    -> {
                        val catalogId = destCatalogData.intValue
                        val isMarkable = (application as iShopApplication).checkCatalogMarkable(catalogId)
                        if (isMarkable) {
                            if (markCode.length >= mCatalog.MARK_CODE_LEN) {
                                val alreadyMarkedCatalogId = (application as iShopApplication).findIncomeCatalogIdByMark(markCode)
                                if (alreadyMarkedCatalogId == null) {
                                    if (destNumData.doubleValue != 1.0) {
                                        destNumData.setError(destNumData.doubleValue.toString(), "Товар с маркировкой должен быть в единичном количестве!")
                                        return false
                                    }
                                } else {
                                    markData.setError(markCode, "Товар с такой маркировкой уже был!")
                                    return false
                                }
                            } else {
                                markData.setError(markCode, "Код маркировки неполон или отсутствует!")
                                return false
                            }
                        }
                    }
                    DocumentTypeConfig.TYPE_OUT,
                    DocumentTypeConfig.TYPE_DESTROY,
                    DocumentTypeConfig.TYPE_RETURN_IN,
                    DocumentTypeConfig.TYPE_MOVE,
                    -> {
                        val isAddOverMarkCode = hmParam[ADD_OVER_MARK_CODE]?.toIntOrNull() == 1

                        if (isAddOverMarkCode ||    // always isMarkable
                            (docType != DocumentTypeConfig.TYPE_MOVE && (application as iShopApplication).checkCatalogMarkable(sourCatalogData.intValue)) // check isMarkable
                        ) {
                            if (markCode.length >= mCatalog.MARK_CODE_LEN) {    // full mark code
                                val incomedCatalogId = (application as iShopApplication).findIncomeCatalogIdByMark(markCode)
                                if (incomedCatalogId != null) {
                                    if(isAddOverMarkCode || incomedCatalogId == sourCatalogData.intValue) {
                                        val outcomedCatalogId = (application as iShopApplication).findOutcomeCatalogIdByMark(markCode)
                                        if (outcomedCatalogId == null) {
                                            //--- automatically setup required values
                                            if (isAddOverMarkCode) {
                                                if (docType in DocumentTypeConfig.hsUseSourCatalog) {
                                                    sourCatalogData.intValue = incomedCatalogId
                                                }
                                                //--- избыточно, т.к. default value уже = 1.0, но лучше перебдеть
                                                if (docType in DocumentTypeConfig.hsUseSourNum) {
                                                    sourNumData.doubleValue = 1.0
                                                }
                                                if (docType in DocumentTypeConfig.hsUseDestCatalog) {
                                                    destCatalogData.intValue = incomedCatalogId
                                                }
                                                //--- избыточно, т.к. default value уже = 1.0, но лучше перебдеть
                                                if (docType in DocumentTypeConfig.hsUseDestNum) {
                                                    destNumData.doubleValue = 1.0
                                                }
                                                //--- mark code using in move operation for item search only
                                                if(docType == DocumentTypeConfig.TYPE_MOVE) {
                                                    markData.text = ""
                                                }
                                            } else {
                                                //--- check selected item catalogId with finded over markCode
                                                if (sourCatalogData.intValue == incomedCatalogId) {
                                                    val numData = if (docType in DocumentTypeConfig.hsUseSourCatalog) {
                                                        sourNumData
                                                    } else {
                                                        destNumData
                                                    }
                                                    if (numData.doubleValue != 1.0) {
                                                        numData.setError(numData.doubleValue.toString(), "Товар с маркировкой должен быть в единичном количестве!")
                                                        return false
                                                    }
                                                } else {
                                                    markData.setError(markCode, "Код маркировки не соответствует выбранному товару!")
                                                    return false
                                                }
                                            }
                                        } else {
                                            markData.setError(markCode, "Товар с такой маркировкой уже выведен из оборота!")
                                            return false
                                        }
                                    } else {
                                        markData.setError(markCode, "Эта маркировка принадлежит другому товару!")
                                        return false
                                    }
                                } else {
                                    markData.setError(markCode, "Товар с такой маркировкой не найден!")
                                    return false
                                }
                            } else {
                                markData.setError(markCode, "Код маркировки неполон или отсутствует!")
                                return false
                            }
                        }
                    }
                    DocumentTypeConfig.TYPE_RESORT -> {
                        val sourCatalogId = sourCatalogData.intValue
                        val isSourMarkable = (application as iShopApplication).checkCatalogMarkable(sourCatalogId)
                        if (isSourMarkable) {
                            if (markCode.length >= mCatalog.MARK_CODE_LEN) {    // full mark code
                                val incomedCatalogId = (application as iShopApplication).findIncomeCatalogIdByMark(markCode)
                                if (incomedCatalogId != null) {
                                    if(incomedCatalogId == sourCatalogData.intValue) {
                                        val outcomedCatalogId = (application as iShopApplication).findOutcomeCatalogIdByMark(markCode)
                                        if (outcomedCatalogId == null) {
                                            if (sourNumData.doubleValue != 1.0) {
                                                sourNumData.setError(sourNumData.doubleValue.toString(), "Товар с маркировкой должен быть в единичном количестве!")
                                                return false
                                            }
                                        } else {
                                            markData.setError(markCode, "Товар с такой маркировкой уже выведен из оборота!")
                                            return false
                                        }

                                    } else {
                                        markData.setError(markCode, "Эта маркировка принадлежит другому товару!")
                                        return false
                                    }
                                } else {
                                    markData.setError(markCode, "Товар с такой маркировкой не найден!")
                                    return false
                                }
                            } else {
                                markData.setError(markCode, "Код маркировки неполон или отсутствует!")
                                return false
                            }
                        }
                    }
                }
            }
            return true
        } else {
            return false
        }
    }

    override fun getInvalidFormDataUrl(id: Int, formDataID: String): String {
        var invalidParams = super.getInvalidFormDataUrl(id, formDataID)
        if(hmParam[ADD_OVER_MARK_CODE]?.toIntOrNull() == 1) {
            invalidParams = AppParameter.setParam(invalidParams, ADD_OVER_MARK_CODE, "1")
        }
        return invalidParams
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        val mdc = model as mDocumentContent

        //--- явно менять поле последнего изменения только при повторном сохранении,
        //--- при первом сохранении при создании оставлять значение по умолчанию, равное времени создания
        if (id != 0) (hmColumnData[mdc.columnEditTime] as DataDateTimeInt).setDateTime(getCurrentTimeInt())
        updateDocumentContentEditTime(hmColumnData, false)

        val sourNum = hmColumnData[mdc.columnSourNum] as DataDouble

        //--- если исходящее кол-во отрицательное, то подставляем кол-во остатка товара на конец этого дня
        //--- пока выключим этот неиспользуемый функционал
        //        if( sourNum.getStaticValue() < 0 ) {
        //            GregorianCalendar gcEnd = ( (DataDate) hmColumnData.get( mdc.getColumnDocumentDate() ) ).getStaticValue();
        //
        //            int sourCatalogID = ( (DataInt) hmColumnData.get( mdc.getColumnSourCatalog() ) ).getStaticValue();
        //
        //            int sourWHID = ( (DataAbstractValue) hmColumnData.get( mdc.getColumnWarehouseSour() ) ).getStaticValue();
        //
        //            HashMap<Integer,HashMap<Integer,Double>> hmDestCount = new HashMap<>();
        //            HashMap<Integer,HashMap<Integer,Double>> hmSourCount = new HashMap<>();
        //            cCatalog.loadCatalogCount( dataWorker.alStm.get( 0 ), null, DocumentTypeConfig.TYPE_ALL,
        //                    null, StringFunction.DateTime_Arr( gcEnd ), hmDestCount, hmSourCount );
        //
        //            sourNum.setValue( cCatalog.calcCatalogCount( hmDestCount, hmSourCount, sourCatalogID, sourWHID ) );
        //        }

        //--- при перемещении между складами программно выставляем товар назначения в такой же
        if (docType == DocumentTypeConfig.TYPE_MOVE) {
            val sourCatalog = hmColumnData[mdc.columnSourCatalog] as DataInt
            val destCatalog = hmColumnData[mdc.columnDestCatalog] as DataInt
            destCatalog.intValue = sourCatalog.intValue
        }

        //--- при перемещении между складами или при пересортице программно выставляем вх.кол-во равным исх. кол-ву
        if (docType == DocumentTypeConfig.TYPE_MOVE || docType == DocumentTypeConfig.TYPE_RESORT) {
            val destNum = hmColumnData[mdc.columnDestNum] as DataDouble
            destNum.doubleValue = sourNum.doubleValue
        }

        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        val mdc = model as mDocumentContent
        if (docType == DocumentTypeConfig.TYPE_RESORT && (hmColumnData[mdc.columnToArchive] as DataBoolean).value) {
            // старый вариант: cCatalog.moveToArchive( stm, (hmColumnData[ mdc.columnSourCatalog ] as DataInt).value )
            cAbstractHierarchy.setActiveAndArchive(
                AppAction.ARCHIVE,
                (hmColumnData[mdc.columnSourCatalog] as DataInt).intValue,
                "SHOP_catalog",
                "id",
                "in_active",
                "in_archive",
                "parent_id",
                false,
                stm
            )
        }
        autoUpdateDiscount()

        return postURL
    }

    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        val mdc = model as mDocumentContent
        //--- преобразуем пересортицу в переоценку
        if (docType == DocumentTypeConfig.TYPE_RESORT && (hmColumnData[mdc.columnResort2Reprice] as DataBoolean).value) {
            val dataDate = (hmColumnData[mdc.columnDocumentDate] as DataDate3Int).localDate
            val sourID = (hmColumnData[mdc.columnSourCatalog] as DataInt).intValue
            val destID = (hmColumnData[mdc.columnDestCatalog] as DataInt).intValue
            var destName = (hmColumnData[mdc.columnDestCatalogName] as DataString).text

            //--- убираем цену из названия
            val p1 = destName.lastIndexOf('(')
            val p2 = destName.lastIndexOf(')')
            if (p1 != -1 && p2 != -1 && p1 < p2) {
                val priceStr = destName.substring(p1 + 1, p2)
                try {
                    //--- если преобразование в число получилось, значит там была цена и её можно убрать из названия
                    priceStr.toDouble()
                    destName = (destName.substring(0, p1) + destName.substring(p2 + 1)).trim()
                    stm.executeUpdate(" UPDATE SHOP_catalog SET name = '$destName' WHERE id = $destID ")
                } catch (t: Throwable) {
                    AdvancedLogger.error(t)
                }
            }
            //--- меняем дату цены по умолчанию на дату документа
            stm.executeUpdate(
                " UPDATE SHOP_price SET ye = ${dataDate.year} , mo = ${dataDate.monthValue} , da = ${dataDate.dayOfMonth} , price_note = 'Из пересортицы' " +
                    " WHERE catalog_id = $destID AND price_type = ${mPrice.PRICE_TYPE_OUT} AND ye = 2000 AND mo = 1 AND da = 1 "
            )
            //--- переносим розничные цены стартого товара к новому
            stm.executeUpdate(" UPDATE SHOP_price SET catalog_id = $destID WHERE catalog_id = $sourID AND price_type = ${mPrice.PRICE_TYPE_OUT} ")
            //--- меняем ссылки в операциях со старого товара на новый
            stm.executeUpdate(" UPDATE SHOP_doc_content SET sour_id = $destID WHERE sour_id = $sourID ")
            stm.executeUpdate(" UPDATE SHOP_doc_content SET dest_id = $destID WHERE dest_id = $sourID ")
            //--- удаляем текущую запись с пересортицей
            stm.executeUpdate(" DELETE FROM SHOP_doc_content WHERE id = $id ")
            //--- удаляем оставшиеся (закупочные) цены стартого товара
            stm.executeUpdate(" DELETE FROM SHOP_price WHERE catalog_id = $sourID ")
            //--- удаляем старый товар
            stm.executeUpdate(" DELETE FROM SHOP_catalog WHERE id = $sourID ")
        }
        autoUpdateDiscount()

        return postURL
    }

    override fun postDelete(id: Int, hmColumnData: Map<iColumn, iData>) {
        super.postDelete(id, hmColumnData)
        updateDocumentContentEditTime(hmColumnData, true)
        autoUpdateDiscount()
    }

    private fun updateDocumentContentEditTime(hmColumnData: Map<iColumn, iData>, isCurTime: Boolean) {
        val mdc = model as mDocumentContent

        stm.executeUpdate(
            StringBuilder(
                " UPDATE SHOP_doc SET content_edit_time = "
            )
                .append(if (isCurTime) getCurrentTimeInt() else (hmColumnData[mdc.columnEditTime] as DataDateTimeInt).zonedDateTime.toEpochSecond().toInt())
                .append(" WHERE id = ").append((hmColumnData[mdc.columnDocument] as DataInt).intValue)
        )

    }

    private fun autoUpdateDiscount() {
        docId?.let { docId ->
            if(docType == DocumentTypeConfig.TYPE_OUT) {
                val (docYe, docMo, docDa) = (application as iShopApplication).getDocumentDate(docId)
                docCost = cDocument.calcDocCountAndCost(stm, hmPrice, docId, docType, zoneId, docYe, docMo, docDa, 0.0).second

                val discountLimits = (application as iShopApplication).discountLimits.map { it.toDouble() }
                val discountValues = (application as iShopApplication).discountValues.map { it.toDouble() }

                var discount = 0.0
                discountLimits.forEachIndexed DL@{ index, limit ->
                    if(docCost <= limit) {
                        return@DL
                    }
                    discount = discountValues[index]
                }
                (application as iShopApplication).setDocumentDiscount(docId, discount)
            }
        }
    }
}

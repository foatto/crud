package foatto.shop

import foatto.core.link.TableCell
import foatto.core.link.TableCellBackColorType
import foatto.core.link.TableCellForeColorType
import foatto.core.link.TableResponse
import foatto.core.link.XyDocumentConfig
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeArray
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataDouble
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.data.DataString
import foatto.core_server.app.server.data.iData
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

class cCatalog : cAbstractHierarchy() {

    private lateinit var hmPriceIn: Map<Int, List<Pair<Int, Double>>>
    private lateinit var hmPriceOut: Map<Int, List<Pair<Int, Double>>>
    private var today = 0

    private var isMerchant: Boolean = false

    private lateinit var alWarehouse: List<Pair<Int, String>>

    private val hmDestCount = mutableMapOf<Int, MutableMap<Int, Double>>()
    private val hmSourCount = mutableMapOf<Int, MutableMap<Int, Double>>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(
        aApplication: iApplication, aConn: CoreAdvancedConnection, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig
    ) {
        super.init(aApplication, aConn, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPriceIn = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_IN)
        hmPriceOut = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
        today = getCurrentTimeInt()

        //--- получить данные по правам доступа
        val hsPermission = userConfig.userPermission[aliasConfig.alias]
        //--- при добавлении модуля в систему прав доступа к нему ещё нет
        isMerchant = hsPermission?.contains(PERM_MERCHANT) ?: false
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа как флаг работы товароведа - доступ/видимость отдельных полей
        alPermission.add(Pair(PERM_MERCHANT, "20 Merchant"))
    }

    override fun getTable(hmOut: MutableMap<String, Any>): TableResponse {
        alWarehouse = mWarehouse.fillWarehouseList(stm)

        hmDestCount.clear()
        hmSourCount.clear()
        loadCatalogCount(stm, null, null, hmDestCount, hmSourCount)/*DocumentTypeConfig.TYPE_ALL, null,*/

        return super.getTable(hmOut)
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val mc = model as mCatalog

        val catalogID = (hmColumnData[mc.columnID] as DataInt).intValue
        val recordType = (hmColumnData[mc.columnRecordType] as DataComboBox).intValue

        val hsID: Set<Int>
        if (recordType == mAbstractHierarchy.RECORD_TYPE_FOLDER) {
            //--- возвращаем ID только от items
            hsID = expandCatalog(stm, model.tableName, catalogID, true)
            //--- если в результате только один элемент и тот равен ID группы элементов, значит группа пустая
            (hmColumnData[mc.columnCatalogRowCount] as DataString).text = (if (hsID.size == 1 && hsID.contains(catalogID)) 0 else hsID.size).toString()
        } else {
            hsID = mutableSetOf()
            hsID.add(catalogID)
            (hmColumnData[mc.columnCatalogPriceIn] as DataDouble).doubleValue = PriceData.getPrice(hmPriceIn, catalogID, today)
            (hmColumnData[mc.columnCatalogPriceOut] as DataDouble).doubleValue = PriceData.getPrice(hmPriceOut, catalogID, today)
        }

        var countAll = 0.0
        for (i in 0 until mc.alColumnCatalogCount.size) {
            val wc = calcCatalogCount(hmDestCount, hmSourCount, hsID, alWarehouse[i].first)
            (hmColumnData[mc.alColumnCatalogCount[i]] as DataDouble).doubleValue = wc
            countAll += wc
        }
        (hmColumnData[mc.columnCatalogAllCount] as DataDouble).doubleValue = countAll
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        if (column is ColumnInt && (hmColumnData[column] as DataInt).intValue < 0 || column is ColumnDouble && (hmColumnData[column] as DataDouble).doubleValue < 0) {

            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = TABLE_CELL_FORE_COLOR_CRITICAL
            tci.backColorType = TableCellBackColorType.DEFINED
            tci.backColor = TABLE_CELL_BACK_COLOR_CRITICAL
            tci.fontStyle = 1
        }
    }

    override fun generateFormColumnData(id: Int, hmColumnData: MutableMap<iColumn, iData>) {
        if (id != 0 && isMerchant) {
            val mc = model as mCatalog
            val recordType = (hmColumnData[mc.columnRecordType] as DataComboBox).intValue

            if (recordType == mAbstractHierarchy.RECORD_TYPE_ITEM) {
                (hmColumnData[mc.columnCatalogPriceIn] as DataDouble).doubleValue = PriceData.getPrice(hmPriceIn, id, today)
                (hmColumnData[mc.columnCatalogPriceOut] as DataDouble).doubleValue = PriceData.getPrice(hmPriceOut, id, today)
            }
        }
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postAdd(id, hmColumnData, hmOut)

        if (isMerchant) {
            val mc = model as mCatalog
            val recordType = (hmColumnData[mc.columnRecordType] as DataComboBox).intValue

            if (recordType == mAbstractHierarchy.RECORD_TYPE_ITEM) {
                val priceDate = (hmColumnData[mc.columnCatalogPriceDate] as DataDate3Int).localDate.atStartOfDay(zoneId)
                val priceIn = (hmColumnData[mc.columnCatalogPriceIn] as DataDouble).doubleValue
                val priceOut = (hmColumnData[mc.columnCatalogPriceOut] as DataDouble).doubleValue

                updateOrInsertPrice(id, priceDate, mPrice.PRICE_TYPE_IN, priceIn)
                updateOrInsertPrice(id, priceDate, mPrice.PRICE_TYPE_OUT, priceOut)
            }
        }
        return postURL
    }

    //--- для классов-наследников - пост-обработка после редактирования
    override fun postEdit(action: String, id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        val postURL = super.postEdit(action, id, hmColumnData, hmOut)

        if (isMerchant) {
            val mc = model as mCatalog

            //--- теперь это единообразно делается в cAbstractHierarchy
            //if((hmColumnData[mc.columnToArchive] as DataBoolean).value) moveToArchive(stm, id)

            val recordType = (hmColumnData[mc.columnRecordType] as DataComboBox).intValue

            if (recordType == mAbstractHierarchy.RECORD_TYPE_ITEM) {
                val priceDate = (hmColumnData[mc.columnCatalogPriceDate] as DataDate3Int).localDate.atStartOfDay(zoneId)
                val priceIn = (hmColumnData[mc.columnCatalogPriceIn] as DataDouble).doubleValue
                val priceOut = (hmColumnData[mc.columnCatalogPriceOut] as DataDouble).doubleValue

                updateOrInsertPrice(id, priceDate, mPrice.PRICE_TYPE_IN, priceIn)
                updateOrInsertPrice(id, priceDate, mPrice.PRICE_TYPE_OUT, priceOut)
            }
        }
        return postURL
    }

    private fun updateOrInsertPrice(catalogID: Int, priceDate: ZonedDateTime, priceType: Int, priceValue: Double) {
//                " UPDATE SHOP_price SET ye = ${dataDate.year} , mo = ${dataDate.month} , da = ${dataDate.day} , price_note = 'Из пересортицы' " +
//                " WHERE catalog_id = $destID AND price_type = ${mPrice.PRICE_TYPE_OUT} AND ye = 2000 AND mo = 1 AND da = 1 " )

        //--- если цена нулевая - то это ошибка или цену менять не надо - пропускаем
        if (priceValue == 0.0) return

        val arrPriceDate = getDateTimeArray(priceDate)

        //--- если новая цена равна старой - пропускаем
        val rs = stm.executeQuery(
            " SELECT price_value FROM SHOP_price WHERE catalog_id = $catalogID AND price_type = $priceType ORDER BY ye DESC , mo DESC , da DESC "
        )
        val lastPrice = if (rs.next()) rs.getDouble(1) else 0.0
        rs.close()
        if (lastPrice == priceValue) return

        //--- сначала попробуем просто поменять сегодняшнюю цену (вдруг её сегодня уже меняли), чтобы не получилось ДВЕ сегодняшних цены
        if (stm.executeUpdate(
                " UPDATE SHOP_price SET price_value = $priceValue " +
                    " WHERE catalog_id = $catalogID " +
                    " AND price_type  = $priceType " +
                    " AND ye = ${arrPriceDate[0]} AND mo = ${arrPriceDate[1]} AND da = ${arrPriceDate[2]} "
            ) == 0
        ) {

            //--- такой цены не нашлось - просто добавляем
            val nextID = stm.getNextID("SHOP_price", "id")
            stm.executeUpdate(
                " INSERT INTO SHOP_price ( id, catalog_id, price_type, ye, mo, da, price_value, price_note ) VALUES ( " +
                    " $nextID , $catalogID , $priceType , " +
                    " ${arrPriceDate[0]} , ${arrPriceDate[1]} , ${arrPriceDate[2]} , " +
                    " $priceValue , '' ) "
            )
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {

        const val PERM_MERCHANT = "merchant"

        fun loadCatalogCount(
            stm: CoreAdvancedStatement, aClientID: Int?, //int aDocType, int[] arrBegDT,
            arrEndDT: IntArray?, hmDest: MutableMap<Int, MutableMap<Int, Double>>, hmSour: MutableMap<Int, MutableMap<Int, Double>>
        ) {

            val whereClient = if (aClientID == null) "" else " AND SHOP_doc.client_id = $aClientID"

            val whereDocType = ""
            //        if( aDocType != DocumentTypeConfig.TYPE_ALL )
            //            sbDocType.append( " AND SHOP_doc.doc_type = " ).append( aDocType );

            var whereDT = ""
            //        if( arrBegDT != null ) {
            //            sbDT.append( " AND ( " )
            //               .append( " SHOP_doc.doc_ye > " ).append( arrBegDT[ 0 ] )
            //               .append( " OR SHOP_doc.doc_ye = " ).append( arrBegDT[ 0 ] )
            //                    .append( " AND SHOP_doc.doc_mo > " ).append( arrBegDT[ 1 ] )
            //               .append( " OR SHOP_doc.doc_ye = " ).append( arrBegDT[ 0 ] )
            //                    .append( " AND SHOP_doc.doc_mo = " ).append( arrBegDT[ 1 ] )
            //                    .append( " AND SHOP_doc.doc_da >= " ).append( arrBegDT[ 2 ] )
            //            .append( " ) " );
            //        }
            if (arrEndDT != null)
                whereDT += " AND ( SHOP_doc.doc_ye < ${arrEndDT[0]} OR SHOP_doc.doc_ye = ${arrEndDT[0]} AND SHOP_doc.doc_mo < ${arrEndDT[1]} OR " +
                    " SHOP_doc.doc_ye = ${arrEndDT[0]} AND SHOP_doc.doc_mo = ${arrEndDT[1]} AND SHOP_doc.doc_da <= ${arrEndDT[2]} ) "

            //--- суммирование всяческих приходов
            var rs = stm.executeQuery(
                " SELECT SHOP_doc_content.dest_id , SHOP_doc.dest_id , SUM( SHOP_doc_content.dest_num ) " +
                    " FROM SHOP_doc_content , SHOP_doc " +
                    " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
                    " AND SHOP_doc.is_deleted = 0 " +
                    " AND SHOP_doc_content.is_deleted = 0 " +
                    " $whereClient $whereDocType $whereDT " +
                    " GROUP BY SHOP_doc_content.dest_id , SHOP_doc.dest_id "
            )
            while (rs.next()) {
                val cID = rs.getInt(1)
                val wID = rs.getInt(2)
                val num = rs.getDouble(3)
                var hmDestCatalog: MutableMap<Int, Double>? = hmDest[cID]
                if (hmDestCatalog == null) {
                    hmDestCatalog = mutableMapOf()
                    hmDest[cID] = hmDestCatalog
                }
                hmDestCatalog[wID] = num
            }
            rs.close()

            //--- вычитание всяческих расходов
            rs = stm.executeQuery(
                " SELECT SHOP_doc_content.sour_id , SHOP_doc.sour_id , SUM( SHOP_doc_content.sour_num ) " +
                    " FROM SHOP_doc_content , SHOP_doc " +
                    " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
                    " AND SHOP_doc.is_deleted = 0 " +
                    " AND SHOP_doc_content.is_deleted = 0 " +
                    " $whereClient $whereDocType $whereDT " +
                    " GROUP BY SHOP_doc_content.sour_id , SHOP_doc.sour_id "
            )
            while (rs.next()) {
                val cID = rs.getInt(1)
                val wID = rs.getInt(2)
                val num = rs.getDouble(3)
                var hmSourCatalog: MutableMap<Int, Double>? = hmSour[cID]
                if (hmSourCatalog == null) {
                    hmSourCatalog = mutableMapOf()
                    hmSour[cID] = hmSourCatalog
                }
                hmSourCatalog[wID] = num
            }
            rs.close()
        }

        fun calcCatalogCount(hmDest: Map<Int, Map<Int, Double>>, hmSour: Map<Int, Map<Int, Double>>, hsID: Set<Int>, warehouseID: Int): Double {
            var result = 0.0
            for (cid in hsID) result += calcCatalogCount(hmDest, hmSour, cid, warehouseID)

            return result
        }

        fun calcCatalogCount(hmDest: Map<Int, Map<Int, Double>>?, hmSour: Map<Int, Map<Int, Double>>?, catalogID: Int, warehouseID: Int): Double {
            var result = 0.0

            if (hmDest != null) {
                val hmDestCatalog = hmDest[catalogID]
                if (hmDestCatalog != null) {
                    val n = hmDestCatalog[warehouseID]
                    result += n ?: 0.0
                }
            }

            if (hmSour != null) {
                val hmSourCatalog = hmSour[catalogID]
                if (hmSourCatalog != null) {
                    val n = hmSourCatalog[warehouseID]
                    result -= n ?: 0.0
                }
            }

            return result
        }

//--- старая ненужная версия, пусть полежит.
//--- теперь это единообразно делается в cAbstractHierarchy
//        fun moveToArchive(stm: CoreAdvancedStatement, catalogID: Int) {
//            var archiveID = 0
//            //--- определяем ID папки с архивом:
//            //--- текущий вариант: это должна быть самая первая по алфавиту корневая папка
//            var rs = stm.executeQuery(" SELECT id FROM SHOP_catalog WHERE id <> 0 AND parent_id = 0 ORDER BY name ")
//            if(rs.next()) archiveID = rs.getInt(1)
//            rs.close()
//
//            //--- составляем список предков
//            val alParentID = mutableListOf<Int>()
//            val alParentName = mutableListOf<String>()
//            //--- стартовое значение
//            rs = stm.executeQuery(" SELECT parent_id FROM SHOP_catalog WHERE id = $catalogID ")
//            if(rs.next()) alParentID.add(rs.getInt(1))
//            rs.close()
//
//            while(true) {
//                val pID = alParentID[0]
//                if(pID == 0) break
//
//                rs = stm.executeQuery(" SELECT parent_id , name FROM SHOP_catalog WHERE id = $pID ")
//                if(rs.next()) {
//                    alParentID.add(0, rs.getInt(1))
//                    alParentName.add(0, "${rs.getString(2)} (архив)")
//                }
//                rs.close()
//            }
//
//            //--- строим аналогичную структуру папок, но в архиве (с проверкой существования таких папок в архиве)
//            var parentID = archiveID
//            for(parentName in alParentName) {
//                //--- проверка существования такой папки в архиве
//                var existID = 0
//                rs = stm.executeQuery(" SELECT id FROM SHOP_catalog WHERE parent_id = $parentID AND name = '$parentName$' ")
//                if(rs.next()) existID = rs.getInt(1)
//                rs.close()
//                //--- такой папки нет - создаём сами
//                if(existID == 0) {
//                    existID = stm.getNextID("SHOP_catalog", "id")
//                    stm.executeUpdate(
//                        " INSERT INTO SHOP_catalog ( id , parent_id , record_type , name , is_production, profit_add ) VALUES ( " +
//                            " $existID , $parentID , ${mAbstractHierarchy.RECORD_TYPE_FOLDER} , '$parentName' , 0 , 0 ) "
//                    )
//                }
//                //--- идём на следующий уровень
//                parentID = existID
//            }
//            //--- меняем ссылку на родительскую папку
//            stm.executeUpdate(" UPDATE SHOP_catalog SET parent_id = $parentID WHERE id = $catalogID ")
//        }
    }

}

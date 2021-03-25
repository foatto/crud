package foatto.shop

import foatto.core.link.AppAction
import foatto.core.link.TableCell
import foatto.core.link.TableCellForeColorType
import foatto.core.link.XyDocumentConfig
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.app.AppParameter
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.iColumn
import foatto.core_server.app.server.data.*
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor
import kotlin.math.max

class cDocument : cStandart() {

    companion object {

        val PERM_AUDIT_MODE = "audit_mode"

        //--- расчёт всех параметров накладной: кол-во строк, стоимость с учётом скидки
        fun calcDocCountAndCost(
            stm: CoreAdvancedStatement,
            hmPrice: Map<Int, List<Pair<Int, Double>>>,
            docID: Int,
            docType: Int,
            zoneId: ZoneId,
            ye: Int,
            mo: Int,
            da: Int,
            discount: Double
        ): Pair<Int, Double> {
            val zdt = ZonedDateTime.of(ye, mo, da, 0, 0, 0, 0, zoneId)
            return calcDocCountAndCost(stm, hmPrice, docID, docType, zdt.toEpochSecond().toInt(), discount)
        }

        fun calcDocCountAndCost(
            stm: CoreAdvancedStatement,
            hmPrice: Map<Int, List<Pair<Int, Double>>>,
            docID: Int,
            docType: Int,
            docTime: Int,
            discount: Double
        ): Pair<Int, Double> {
            val fnNum = if(DocumentTypeConfig.hsUseDestNum.contains(docType)) "dest_num" else "sour_num"
            val fnCatalog = if(DocumentTypeConfig.hsUseDestCatalog.contains(docType)) "dest_id" else "sour_id"

            val rs = stm.executeQuery(" SELECT $fnNum , $fnCatalog FROM SHOP_doc_content WHERE doc_id = $docID AND is_deleted = 0 ")
            var rowCount = 0
            var costSum = 0.0
            while(rs.next()) {
                rowCount++
                costSum += rs.getDouble(1) * PriceData.getPrice(hmPrice, rs.getInt(2), docTime)
            }
            rs.close()

            return Pair(rowCount, floor(costSum * (1 - discount / 100)))
        }
    }

    private var docType = DocumentTypeConfig.TYPE_ALL
    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aConn, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        docType = DocumentTypeConfig.hmAliasDocType[aliasConfig.alias]!!
        hmPrice = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
    }

    override fun definePermission() {
        super.definePermission()
        //--- права доступа как флаг работы в режиме аудита
        alPermission.add(Pair(PERM_AUDIT_MODE, "20 Audit mode"))
    }

    override fun fillHeader(selectorID: String?, withAnchors: Boolean, alPath: MutableList<Pair<String, String>>, hmOut: MutableMap<String, Any>) {
        super.fillHeader(selectorID, withAnchors, alPath, hmOut)

        getParentID("shop_warehouse")?.let { pid ->
            alPath.add(Pair("", "[" + (mWarehouse.fillWarehouseMap(stm)[pid] ?: "") + "]"))
        }
    }

    override fun addSQLWhere(hsTableRenameList: Set<String>): String {
        val pid = getParentID("shop_warehouse")

        val md = model as mDocument
        val tableName = renameTableName(hsTableRenameList, model.tableName)
        val typeFieldName = md.columnDocumentType.getFieldName(0)

        var sSQL = ""

        if(docType != DocumentTypeConfig.TYPE_ALL) sSQL += " AND $tableName.$typeFieldName = $docType "

        //--- добавить свои ограничения по parent-данным от shop_warehouse (из-за нестандартной обработки shop_doc_all/move/_resort)
        if(pid != null) {
            val sourFieldName = md.columnWarehouseSour.getFieldName(0)
            val destFieldName = md.columnWarehouseDest.getFieldName(0)
            val useSour = DocumentTypeConfig.hsUseSourWarehouse.contains(docType)
            val useDest = DocumentTypeConfig.hsUseDestWarehouse.contains(docType)

            sSQL += " AND "
            //--- используются оба складских поля
            if(useSour && useDest) sSQL += " ( $tableName.$sourFieldName = $pid OR $tableName.$destFieldName = $pid ) "
            else if(useSour) sSQL += " $tableName.$sourFieldName = $pid "
            else if(useDest) sSQL += " $tableName.$destFieldName = $pid "
        }
        return super.addSQLWhere(hsTableRenameList) + sSQL
    }

    //--- перекрывается наследниками для генерации данных в момент загрузки записей ПОСЛЕ фильтров поиска и страничной разбивки
    override fun generateColumnDataAfterFilter(hmColumnData: MutableMap<iColumn, iData>) {
        val md = model as mDocument

        val docID = (hmColumnData[md.columnID!!] as DataInt).intValue
        //--- именно тип документа из строки, а не общий (т.к. м.б. == TYPE_ALL)
        val rowDocType = (hmColumnData[md.columnDocumentType] as DataComboBox).intValue
        val docTime = (hmColumnData[md.columnDocumentDate] as DataDate3Int).localDate.atStartOfDay(zoneId).toEpochSecond().toInt()
        val discount = (hmColumnData[md.columnDocumentDiscount] as DataDouble).doubleValue

        val (rowCount, docCost) = calcDocCountAndCost(stm, hmPrice, docID, rowDocType, docTime, discount)
        (hmColumnData[md.columnDocumentRowCount] as DataInt).intValue = rowCount
        (hmColumnData[md.columnDocumentCostOut] as DataDouble).doubleValue = docCost
    }

    override fun getTableColumnStyle(rowNo: Int, isNewRow: Boolean, hmColumnData: Map<iColumn, iData>, column: iColumn, tci: TableCell) {
        super.getTableColumnStyle(rowNo, isNewRow, hmColumnData, column, tci)

        val md = model as mDocument

        if ((hmColumnData[md.columnDocumentIsDeleted] as DataBoolean).value) {
            tci.foreColorType = TableCellForeColorType.DEFINED
            tci.foreColor = TABLE_CELL_FORE_COLOR_DISABLED
        }
    }

    override fun preSave(id: Int, hmColumnData: Map<iColumn, iData>) {
        val md = model as mDocument

        //--- если номер документа не заполнен - заполняем его автоматически
        //--- поиск максимального номера накладной за текущий год
        val dataDocNo = (hmColumnData[md.columnDocumentNo] as DataString)
        if(id == 0 && dataDocNo.text.isBlank()) {
            var maxDocNo = 0
            val rs = stm.executeQuery(
                " SELECT doc_no FROM SHOP_doc WHERE doc_type = $docType AND doc_ye = ${ZonedDateTime.now().year} "
            )
            while(rs.next()) {
                try {
                    //--- номер накладной может быть в свободной строкой форме,
                    //--- поэтому учитываем только цифровые значения
                    maxDocNo = max(maxDocNo, rs.getString(1).toInt())
                } catch(nfe: Throwable) {
                }
            }
            //--- дополняем номер нулями спереди, чтобы сортировка не сбивалась
            //--- (стандартный padStart( 5, '0' ) не пойдёт, т.к. он обрезает более длинную строку до 5 символов
            val sb = StringBuilder().append(maxDocNo + 1)
            while(sb.length < 5) sb.insert(0, '0')
            dataDocNo.text = sb.toString()
        }

        //--- явно менять поле последнего изменения только при повторном сохранении,
        //--- при первом сохранении при создании оставлять значение по умолчанию, равное времени создания
        if(id != 0) (hmColumnData[md.columnEditTime] as DataDateTimeInt).setDateTime(getCurrentTimeInt())

        //--- при пересортице программно выставляем "На склад" = "Со склада"
        if(docType == DocumentTypeConfig.TYPE_RESORT) {
            val sourWarehouse = hmColumnData[md.columnWarehouseSour] as DataComboBox
            val destWarehouse = hmColumnData[md.columnWarehouseDest] as DataInt
            destWarehouse.intValue = sourWarehouse.intValue
        }
        super.preSave(id, hmColumnData)
    }

    override fun postAdd(id: Int, hmColumnData: Map<iColumn, iData>, hmOut: MutableMap<String, Any>): String? {
        var postURL = super.postAdd(id, hmColumnData, hmOut)

        val refererID = hmParam[AppParameter.REFERER]
        //--- если добавление не из формы, а из главного меню - то переходим на состав накладной
        if(refererID == null) {
            val hmDocContentParentData = HashMap<String, Int>()
            hmDocContentParentData[aliasConfig.alias] = id
            postURL = getParamURL(DocumentTypeConfig.hmAliasChild[aliasConfig.alias]!!, AppAction.TABLE, refererID, 0, hmDocContentParentData, null, null)
        }

        return postURL
    }

}

//--- пока неизвестно, понадобится ли функция общего вида, а частная реализация есть в report.cCashHistory
//    //--- расчёт суммы нескольких документов
//    public static double calcDocSum( CoreAdvancedStatement stm, int aWarehouseID, int aDocType,
//                                     int[] arrBegDT, int[] arrEndDT ) throws Throwable {
//        boolean useSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains( aDocType );
//        boolean useDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains( aDocType );
//
//        //--- получаем список документов
//        ArrayList<Integer> alDocID = new ArrayList<>();
//        ArrayList<Double> alDocDiscount = new ArrayList<>();
//
//        StringBuilder sbWarehouse = new StringBuilder();
//        if( aWarehouseID != 0 ) {
//            sbWarehouse.append( " AND " );
//            //--- используются оба складских поля
//            if( useSourWarehouse && useDestWarehouse )
//                sbWarehouse.append( " ( sour_id = " ).append( aWarehouseID )
//                           .append( " OR dest_id = " ).append( aWarehouseID )
//                           .append( " ) " );
//            else if( useSourWarehouse )
//                sbWarehouse.append( " sour_id = " ).append( aWarehouseID );
//            else if( useDestWarehouse )
//                sbWarehouse.append( " dest_id = " ).append( aWarehouseID );
//        }
//
//        StringBuilder sbDocType = new StringBuilder();
//        if( aDocType != 0 )
//            sbDocType.append( " AND doc_type = " ).append( aDocType );
//
//        StringBuilder sbDT = new StringBuilder();
//        if( arrBegDT != null ) {
//            sbDT.append( " AND ( " )
//               .append( " doc_ye > " ).append( arrBegDT[ 0 ] )
//               .append( " OR doc_ye = " ).append( arrBegDT[ 0 ] )
//                    .append( " AND doc_mo > " ).append( arrBegDT[ 1 ] )
//               .append( " OR doc_ye = " ).append( arrBegDT[ 0 ] )
//                    .append( " AND doc_mo = " ).append( arrBegDT[ 1 ] )
//                    .append( " AND doc_da >= " ).append( arrBegDT[ 2 ] )
//            .append( " ) " );
//        }
//        if( arrEndDT != null ) {
//            sbDT.append( " AND ( " )
//               .append( " doc_ye < " ).append( arrEndDT[ 0 ] )
//               .append( " OR doc_ye = " ).append( arrEndDT[ 0 ] )
//                    .append( " AND doc_mo < " ).append( arrEndDT[ 1 ] )
//               .append( " OR doc_ye = " ).append( arrEndDT[ 0 ] )
//                    .append( " AND doc_mo = " ).append( arrEndDT[ 1 ] )
//                    .append( " AND doc_da < " ).append( arrEndDT[ 2 ] )
//            .append( " ) " );
//        }
//
//        StringBuilder sbSQL = new StringBuilder(
//                     " SELECT id , discount FROM SHOP_doc " )
//            .append( " WHERE id <> 0 " )
//            .append( sbWarehouse )
//            .append( sbDocType )
//            .append( sbDT );
//        CoreAdvancedResultSet rs = stm.executeQuery( sbSQL.toString() );
//        while( rs.next() ) {
//            alDocID.add( rs.getInt( 1 ) );
//            alDocDiscount.add( rs.getDouble( 2 ) );
//        }
//        rs.close();
//
//        //--- теперь для каждого документа
//        double result = 0;
//        for( int i = 0; i < alDocID.size(); i++ ) {
//            int docID = alDocID.get( i );
//            double discount = alDocDiscount.get( i );
//
//            rs = stm.executeQuery( getDocCostSQL( docID, aDocType ) );
//            if( rs.next() )
//                result += rs.getDouble( 2 ) * ( 1 - discount / 100 );
//            rs.close();
//        }
//        return result;
//    }

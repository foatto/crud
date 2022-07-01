package foatto.shop.report

import foatto.core.link.XyDocumentConfig
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractReport
import foatto.shop.DocumentTypeConfig
import foatto.shop.PriceData
import foatto.shop.cDocument
import foatto.shop.mPrice
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.util.concurrent.ConcurrentHashMap

abstract class cAbstractShopReport : cAbstractReport() {

    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

    override fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aConn, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPrice = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
    }

    //--- расчёт суммы нескольких документов
    protected fun calcOut(aWarehouseID: Int, arrDT: Array<Int>): Double {
        val stmCalc = conn.createStatement()

        //--- получаем список документов
        val alDocID = mutableListOf<Int>()
        val alDocType = mutableListOf<Int>()
        val alDocDiscount = mutableListOf<Double>()

        //--- для всех продаж
        var rs = stmCalc.executeQuery(
            """
                SELECT id , discount 
                FROM SHOP_doc 
                WHERE is_deleted = 0 
                AND sour_id = $aWarehouseID 
                AND doc_type = ${DocumentTypeConfig.TYPE_OUT} 
                AND doc_ye = ${arrDT[0]} 
                AND doc_mo = ${arrDT[1]} 
                AND doc_da = ${arrDT[2]}
            """
        )
        while (rs.next()) {
            alDocID.add(rs.getInt(1))
            alDocType.add(DocumentTypeConfig.TYPE_OUT)
            alDocDiscount.add(rs.getDouble(2))
        }
        rs.close()

        //--- для всех возвратов от покупателя
        rs = stmCalc.executeQuery(
            """
                SELECT id , discount 
                FROM SHOP_doc 
                WHERE is_deleted = 0 
                AND dest_id = $aWarehouseID 
                AND doc_type = ${DocumentTypeConfig.TYPE_RETURN_OUT} 
                AND doc_ye = ${arrDT[0]} 
                AND doc_mo = ${arrDT[1]} 
                AND doc_da = ${arrDT[2]} 
            """
        )
        while (rs.next()) {
            alDocID.add(rs.getInt(1))
            alDocType.add(DocumentTypeConfig.TYPE_RETURN_OUT)
            alDocDiscount.add(rs.getDouble(2))
        }
        rs.close()

        //--- теперь для каждого документа
        var result = 0.0
        for (i in alDocID.indices) {
            val docID = alDocID[i]
            val docType = alDocType[i]
            val discount = alDocDiscount[i]

            result += (if (docType == DocumentTypeConfig.TYPE_OUT) 1 else -1) *
                cDocument.calcDocCountAndCost(stmCalc, hmPrice, docID, docType, zoneId, arrDT[0], arrDT[1], arrDT[2], discount).second
        }

        stmCalc.close()

        return result
    }
}
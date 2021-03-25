package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.link.XyDocumentConfig
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataDate3Int
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.shop.PriceData
import foatto.shop.mPrice
import foatto.shop.mWarehouse
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import jxl.format.PageOrientation
import jxl.format.PaperSize
import java.util.*
import java.util.concurrent.ConcurrentHashMap

abstract class cAbstractCatalogReport : cAbstractReport() {

    companion object {
        const val ROOT_ITEM_NAME = "ВСЕГО"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var hmWarehouseName: Map<Int, String>
    protected lateinit var tmWarehouseID: TreeMap<String, Int>

    protected lateinit var hmCatalogParent: Map<Int, List<Int>>
    protected lateinit var hmItemInfo: MutableMap<Int, ItemInfo>

    protected lateinit var tmItem: TreeMap<String, CalcItem>

    protected lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

    protected var countNN = 1

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aConn, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPrice = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) return returnURL

        val m = model as mSHOPReport

        //--- выборка данных параметров для отчета
        hmReportParam["report_warehouse_dest"] = (hmColumnData[m.columnWarehouseDest] as DataComboBox).intValue
        hmReportParam["report_document_client"] = (hmColumnData[m.columnClient] as DataInt).intValue
        hmReportParam["report_document_type"] = (hmColumnData[m.columnDocumentType] as DataComboBox).intValue
        hmReportParam["report_catalog_dest"] = (hmColumnData[m.columnCatalogDest] as DataInt).intValue

        val begDate = (hmColumnData[m.columnBegDate] as DataDate3Int).localDate
        val endDate = (hmColumnData[m.columnEndDate] as DataDate3Int).localDate

        hmReportParam["report_beg_year"] = begDate.year
        hmReportParam["report_beg_month"] = begDate.monthValue
        hmReportParam["report_beg_day"] = begDate.dayOfMonth

        hmReportParam["report_end_year"] = endDate.year
        hmReportParam["report_end_month"] = endDate.monthValue
        hmReportParam["report_end_day"] = endDate.dayOfMonth

        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = PageOrientation.PORTRAIT

        printMarginLeft = 20
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun collectWarehouseInfo() {
        hmWarehouseName = mWarehouse.fillWarehouseMap(stm)
        //--- понадобится еще и отсортированный список наименований магазинов
        tmWarehouseID = TreeMap()
        // "все склады" нам не нужны, сами напишем
        for((wID, wName) in hmWarehouseName)
            if(wID != 0)
                tmWarehouseID[wName] = wID
    }

    protected fun collectItemInfo() {
        //--- соберём инфу по элементам каталога
        hmItemInfo = mutableMapOf()
        hmItemInfo[0] = ItemInfo(0, ROOT_ITEM_NAME, true)
        val rs = stm.executeQuery(" SELECT id , name , record_type FROM SHOP_catalog WHERE id <> 0 ")
        while(rs.next()) {
            val id = rs.getInt(1)
            hmItemInfo[id] = ItemInfo(id, rs.getString(2), rs.getInt(3) == mAbstractHierarchy.RECORD_TYPE_FOLDER)
        }
        rs.close()
    }

    protected class ItemInfo(val id: Int, val name: String, val isFolder: Boolean)

    protected class CalcItem(val ii: ItemInfo, val parentName: String?) {
        //--- список подэлементов (если == null, значит это элемент, а не папка)
        var tmSubItem: TreeMap<String, CalcItem>? = if(ii.isFolder) TreeMap() else null

        //--- кол-во наименований - только для папок
        var subItemCount = 0

        var tmWHCount = TreeMap<String, Double>()
        var tmWHPrice = TreeMap<String, Double>()
    }

}

package foatto.shop.report

import foatto.core.util.DateTime_DMYHMS
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.shop.DocumentTypeConfig
import foatto.shop.PriceData
import foatto.shop.cCatalog
import jxl.CellView
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cWarehouseState : cAbstractOperationState() {

    private var reportWarehouseDest = 0
    private var reportCatalogDest = 0
    private var reportEndYear: Int? = null
    private var reportEndMonth: Int? = null
    private var reportEndDay: Int? = null

    private var arrEndDT: IntArray? = null

    private lateinit var hmDestCount: MutableMap<Int, MutableMap<Int, Double>>
    private lateinit var hmSourCount: MutableMap<Int, MutableMap<Int, Double>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun postReport(sheet: WritableSheet) {

        reportWarehouseDest = hmReportParam["report_warehouse_dest"] as Int
        reportCatalogDest = hmReportParam["report_catalog_dest"] as Int
        reportEndYear = hmReportParam["report_end_year"] as Int
        reportEndMonth = hmReportParam["report_end_month"] as Int
        reportEndDay = hmReportParam["report_end_day"] as Int

        arrEndDT = if(reportEndYear == null) null else intArrayOf(reportEndYear!!, reportEndMonth!!, reportEndDay!!)

        collectWarehouseInfo()
        collectItemInfo()
        hmCatalogParent = cAbstractHierarchy.getCatalogParent(stm, "SHOP_catalog")

        hmDestCount = mutableMapOf()
        hmSourCount = mutableMapOf()
        cCatalog.loadCatalogCount(stm, null, arrEndDT, hmDestCount, hmSourCount)

        tmItem = TreeMap()
        //--- рекурсивный расчет
        val ciRoot = CalcItem(hmItemInfo[reportCatalogDest]!!, null)
        tmItem[ciRoot.ii.name] = ciRoot
        calcItem(ciRoot)

        defineFormats(8, 2, 0)

        var offsY = 0

        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        sheet.addCell(Label(1, offsY++, "на ${if(reportEndDay!! < 10) "0" else ""}$reportEndDay.${if(reportEndMonth!! < 10) "0" else ""}$reportEndMonth.$reportEndYear", wcfTitleL))
        offsY++

        //--- установка размеров заголовков ( общая ширина = 90/140 для А4 портрет/ландшафт, левые/правые поля по 10 мм )
        //--- общая формула столбцов: W = 5 + N + 7 + (  wh_count + 1  ) * 7 + 11 = 90,
        //--- причем в hmWarehouseName уже входит 0-й элемент для столбца "ВСЕ"
        val alDim = ArrayList<Int>()
        val nameColumnWidth = 90 - 5 - 7 - 11 - (if(reportWarehouseDest == 0) tmWarehouseID!!.size + 1 else 1) * 7
        alDim.add(5)                 // "N п/п"
        alDim.add(nameColumnWidth)   // "Наименование"
        alDim.add(7)                 // "Кол-во наименований"
        if(reportWarehouseDest == 0) {
            for(wName in tmWarehouseID.keys) alDim.add(7)         // кол-во на каждом складе
            alDim.add(7)             // общее кол-во
        } else alDim.add(7)             // кол-во на указанном складе
        alDim.add(11)                // "Стоимость"

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовков
        var offsX = 0  // счётчик позиций из-за переменного кол-ва заголовков

        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Наименование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Кол-во наименований", wcfCaptionHC))

        if(reportWarehouseDest == 0) {
            for(wName in tmWarehouseID.keys) sheet.addCell(Label(offsX++, offsY, wName, wcfCaptionHC))
            sheet.addCell(Label(offsX++, offsY, "ВСЕГО", wcfCaptionHC))
        } else sheet.addCell(Label(offsX++, offsY, hmWarehouseName[reportWarehouseDest], wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Стоимость", wcfCaptionHC))
        offsY++

        //--- рекурсивный вывод
        offsY = outItem(sheet, offsY, ciRoot, reportWarehouseDest)

        offsY++
        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
    }

    //--- рекурсивный расчёт
    private fun calcItem(ci: CalcItem) {
        //--- если это папка, пройдёмся рекурсивно по подпапкам и элементам
        if(ci.ii.isFolder) {
            ci.tmSubItem = TreeMap()
            val alSubID = hmCatalogParent[ci.ii.id]
            if(alSubID != null)
                for(subID in alSubID) {
                    val ciNew = CalcItem(hmItemInfo[subID]!!, ci.ii.name)
                    ci.tmSubItem!![ciNew.ii.name] = ciNew
                    tmItem[ciNew.ii.name] = ciNew
                    calcItem(ciNew)
                }
        }
        //--- посчитаем для самого элемента и просуммируемся по всем родителям
        else {
            val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(DocumentTypeConfig.TYPE_ALL)
            val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(DocumentTypeConfig.TYPE_ALL)
            for((name, wid) in tmWarehouseID) {
                //--- специфика подсчёта отличается в зависимости от типа накладной
                //--- ( иначе сумма перемещённых товаров всегда будет == 0, т.к. там всегда сколько ушло, столько и пришло )
                val wcCount = cCatalog.calcCatalogCount(if(isUseDestCatalog) hmDestCount else null, if(isUseSourCatalog) hmSourCount else null, ci.ii.id, wid)
                //--- если тип накладной не указан, то это чистое суммирование ( т.е. состояние склада ),
                //--- иначе сумма по накладным/операциям
                //--- в отчёте по состоянию склада считаются все типы накладных
                //if(  reportDocumentType != DocumentTypeConfig.TYPE_ALL  ) wcCount = Math.abs(  wcCount  );
                ci.tmWHCount[name] = wcCount
                ci.tmWHPrice[name] = wcCount * PriceData.getPrice(hmPrice, ci.ii.id, zoneId, arrEndDT!![0], arrEndDT!![1], arrEndDT!![2])
            }
            //--- просуммируем для всех родителей
            var parentName = ci.parentName
            while(parentName != null) {
                val parentItem = tmItem[parentName]
                var itemCount = 0
                for(name in ci.tmWHCount.keys) {
                    val parentCount = parentItem!!.tmWHCount[name]
                    parentItem.tmWHCount[name] = (parentCount ?: 0.0) + ci.tmWHCount[name]!!
                    //--- отдельно отслеживаем общее операционное кол-во по всем складам
                    itemCount += ci.tmWHCount[name]!!.toInt()
                }
                for(name in ci.tmWHPrice.keys) {
                    val parentPrice = parentItem!!.tmWHPrice[name]
                    parentItem.tmWHPrice[name] = (parentPrice ?: 0.0) + ci.tmWHPrice[name]!!
                }
                //--- если кол-во отлично от 0 - увеличиваем счётчик кол-ва наименований
                if(itemCount != 0) parentItem!!.subItemCount++
                parentName = parentItem!!.parentName
            }
        }
    }

}

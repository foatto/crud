package foatto.shop.report

import foatto.core_server.app.server.cAbstractHierarchy
import foatto.shop.DocumentTypeConfig
import foatto.shop.PriceData
import jxl.CellView
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cOperationSummary : cAbstractOperationState() {

    private var reportWarehouseDest = 0

    //    private var reportDocumentClient: Int? = null    // отдельное значение == 0 - по всем розничным контрагентам
    private var reportCatalogDest = 0
    private var reportBegYear: Int? = null
    private var reportBegMonth: Int? = null
    private var reportBegDay: Int? = null
    private var reportEndYear: Int? = null
    private var reportEndMonth: Int? = null
    private var reportEndDay: Int? = null

    private var arrBegDT: IntArray? = null
    private var arrEndDT: IntArray? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun postReport(sheet: WritableSheet) {

        reportWarehouseDest = hmReportParam["report_warehouse_dest"] as Int
//        reportDocumentClient = 0;   //(Integer) hmReportParam.get( "report_document_client" ); - пока по всем клиентам
        reportCatalogDest = hmReportParam["report_catalog_dest"] as Int
        reportBegYear = hmReportParam["report_beg_year"] as Int
        reportBegMonth = hmReportParam["report_beg_month"] as Int
        reportBegDay = hmReportParam["report_beg_day"] as Int
        reportEndYear = hmReportParam["report_end_year"] as Int
        reportEndMonth = hmReportParam["report_end_month"] as Int
        reportEndDay = hmReportParam["report_end_day"] as Int

        arrBegDT = if(reportBegYear == null) null else intArrayOf(reportBegYear!!, reportBegMonth!!, reportBegDay!!)
        arrEndDT = if(reportEndYear == null) null else intArrayOf(reportEndYear!!, reportEndMonth!!, reportEndDay!!)

        collectWarehouseInfo()
        collectItemInfo()
        hmCatalogParent = cAbstractHierarchy.getCatalogParent(stm, "SHOP_catalog")

        tmItem = TreeMap()
        //--- рекурсивный расчет
        val ciRoot = CalcItem(hmItemInfo[reportCatalogDest]!!, null)
        tmItem[ciRoot.ii.name] = ciRoot
        calcItem(ciRoot)

        defineFormats(8, 2, 0)

        var offsY = 0

        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        sheet.addCell(
            Label(
                1, offsY++,
                "с ${if(reportBegDay!! < 10) "0" else ""}$reportBegDay.${if(reportBegMonth!! < 10) "0" else ""}$reportBegMonth.$reportBegYear", wcfTitleL
            )
        )
        sheet.addCell(
            Label(
                1, offsY++, if(arrBegDT == null) "на " else "по " +
                    "${if(reportEndDay!! < 10) "0" else ""}$reportEndDay.${if(reportEndMonth!! < 10) "0" else ""}$reportEndMonth.$reportEndYear", wcfTitleL
            )
        )
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
            for((name, wid) in tmWarehouseID) {
                var wcCount = 0.0
                var wcPrice = 0.0

                //--- учёт продаж
                val sSQL =
                    " SELECT SHOP_doc_content.sour_num , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , SHOP_doc.discount " +
                        " FROM SHOP_doc_content , SHOP_doc " +
                        " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
                        " AND SHOP_doc.is_deleted = 0 " +
                        " AND SHOP_doc_content.is_deleted = 0 " +
                        " AND SHOP_doc.doc_type = ${DocumentTypeConfig.TYPE_OUT} " +
                        " AND SHOP_doc.sour_id = $wid " +
                        " AND SHOP_doc_content.sour_id = ${ci.ii.id} " +
                        //--- оставим на будущее
                        //.append(  " AND SHOP_doc.client_id = "  ).append(  aClientID  );
                        " AND ( SHOP_doc.doc_ye > ${arrBegDT!![0]} OR " +
                        " SHOP_doc.doc_ye = ${arrBegDT!![0]} AND SHOP_doc.doc_mo > ${arrBegDT!![1]} OR " +
                        " SHOP_doc.doc_ye = ${arrBegDT!![0]} AND SHOP_doc.doc_mo = ${arrBegDT!![1]} AND SHOP_doc.doc_da >= ${arrBegDT!![2]} ) " +
                        " AND ( SHOP_doc.doc_ye < ${arrEndDT!![0]} OR " +
                        " SHOP_doc.doc_ye = ${arrEndDT!![0]} AND SHOP_doc.doc_mo < ${arrEndDT!![1]} OR " +
                        " SHOP_doc.doc_ye = ${arrEndDT!![0]} AND SHOP_doc.doc_mo = ${arrEndDT!![1]} AND SHOP_doc.doc_da <= ${arrEndDT!![2]} ) "

                val rs = stm.executeQuery(sSQL)
                while(rs.next()) {
                    val num = rs.getDouble(1)
                    val docYe = rs.getInt(2)
                    val docMo = rs.getInt(3)
                    val docDa = rs.getInt(4)
                    val discount = rs.getDouble(5)

                    wcCount += num
                    wcPrice += num * PriceData.getPrice(hmPrice, ci.ii.id, zoneId, docYe, docMo, docDa) * (1 - discount / 100)
                }
                rs.close()

                ci.tmWHCount[name] = wcCount
                ci.tmWHPrice[name] = wcPrice  //Math.floor(  wcPrice  )  );
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

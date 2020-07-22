package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.data.DataInt
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.shop.DocumentTypeConfig
import foatto.shop.cCatalog
import foatto.shop.mPrice
import foatto.shop.mWarehouse
import jxl.CellView
import jxl.format.Alignment
import jxl.format.Colour
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.format.VerticalAlignment
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cOperationHistory : cAbstractReport() {

    private lateinit var hmWarehouseName: HashMap<Int, String>
    private lateinit var tmWarehouseID: TreeMap<String, Int>
    private lateinit var hmCatalogName: HashMap<Int, String>

    //----------------------------------------------------------------------------------------------

    override fun isFormAutoClick(): Boolean = if(hmParentData["shop_catalog"] != null) true else super.isFormAutoClick()

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        val moh = model as mOperationHistory

        //--- выборка данных параметров для отчета
        hmReportParam["report_catalog_dest"] = (hmColumnData[moh.columnCatalogDest] as DataInt).value

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

    override fun postReport(sheet: WritableSheet) {

        hmWarehouseName = mWarehouse.fillWarehouseMap(stm)
        //--- понадобится еще и отсортированный список наименований магазинов
        tmWarehouseID = TreeMap()
        for((wID, wName) in hmWarehouseName)
            if(wID != 0)  // "все склады" нам не нужны, сами напишем
                tmWarehouseID[wName] = wID
        //--- соберём инфу по элементам каталога
        hmCatalogName = HashMap()
        val rs = stm.executeQuery(" SELECT id , name FROM SHOP_catalog WHERE id <> 0 AND record_type = ${mAbstractHierarchy.RECORD_TYPE_ITEM} ")
        while(rs.next()) hmCatalogName[rs.getInt(1)] = rs.getString(2)
        rs.close()

        //--- загрузка стартовых параметров
        val reportCatalogDest = hmReportParam["report_catalog_dest"] as Int

        val alResult = calcReport()

        defineFormats(8, 2, 0)

        var offsY = 0
        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++

        sheet.addCell(Label(1, offsY, "Наименование:", wcfTitleName))
        sheet.addCell(Label(2, offsY, hmCatalogName[reportCatalogDest], wcfTitleValue))
        offsY++

        offsY++

        //--- установка размеров и наименований столбцов в зависимости от параметров отчёта
        val alCaption = ArrayList<String>()
        val alDim = ArrayList<Int>()

        alCaption.add("№ п/п")
        alDim.add(5)
        alCaption.add("Накладная")
        alDim.add(-1)
        alCaption.add("Операционное кол-во")
        alDim.add(7)
        for(wName in tmWarehouseID.keys) {
            alCaption.add(wName)
            alDim.add(7)         // кол-во на каждом складе
        }
        alCaption.add("ВСЕГО")
        alDim.add(7)             // общее кол-во

        defineRelWidth(alDim, 90)

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        //--- вывод заголовков
        var offsX = 0  // счётчик позиций из-за переменного кол-ва заголовков
        for(caption in alCaption) sheet.addCell(Label(offsX++, offsY, caption, wcfCaptionHC))

        offsY++

        var countNN = 1
        for(ohr in alResult) {
            offsX = 3

            var whSum = 0.0
            for((_, wID) in tmWarehouseID) {
                val num = ohr.hmHWState[wID]!!

                sheet.addCell(Label(offsX++, offsY, getSplittedDouble(num, -1).toString(), if(num < 0) wcfCellCBStdRed else wcfCellC))
                whSum += num
            }
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(whSum, -1).toString(), if(whSum < 0) wcfCellCBStdRed else wcfCellCBStdYellow))
            offsY++

            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))

            val docColour = if(ohr.type == DocumentTypeConfig.TYPE_IN) Colour.LIGHT_GREEN
            else if(ohr.type == DocumentTypeConfig.TYPE_OUT) Colour.WHITE
            else if(ohr.type == DocumentTypeConfig.TYPE_MOVE) Colour.VERY_LIGHT_YELLOW
            else if(ohr.type == DocumentTypeConfig.TYPE_RESORT) Colour.ROSE
            else if(ohr.type == -1) Colour.SKY_BLUE   // переоценка
            else Colour.LIGHT_ORANGE

            val wcfDoc = getWCF(8, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
            wcfDoc.setBackground(docColour)
            val wcfNum = getWCF(8, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
            wcfNum.setBackground(docColour)

            sheet.addCell(Label(offsX++, offsY, "${hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[ohr.type]]?.descr ?: "Переоценка"}    ${ohr.date}\n${ohr.doc}", wcfDoc))
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(ohr.operNum, -1).toString(), wcfNum))

            offsY++
        }
        offsY++

        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells(  1, offsY, 3, offsY  );
    }

    private fun calcReport(): ArrayList<OperationHistoryResult> {
        //--- загрузка стартовых параметров
        val reportCatalogDest = hmReportParam["report_catalog_dest"] as Int

        val hsMerchantPermission = userConfig.userPermission["shop_catalog"]!!
        val isMerchant = hsMerchantPermission.contains(cCatalog.PERM_MERCHANT)

        //--- загрузка инфы по переоценкам
        val alPriceData = ArrayList<PriceData>()
        var rs = stm.executeQuery(
            " SELECT ye , mo , da , price_type , price_value FROM SHOP_price " +
                " WHERE catalog_id = $reportCatalogDest " +
                (if(isMerchant) "" else " AND price_type = ${mPrice.PRICE_TYPE_OUT} ") +
                " ORDER BY ye , mo , da "
        )
        while(rs.next()) alPriceData.add(PriceData(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getDouble(5)))
        rs.close()

        val alResult = ArrayList<OperationHistoryResult>()

        val sbSQL =
            " SELECT SHOP_doc.doc_type , SHOP_doc_content.sour_id , SHOP_doc_content.dest_id , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
                " SHOP_doc.sour_id , SHOP_doc.dest_id , SHOP_doc.doc_no , SHOP_client.name , SHOP_doc.descr , SHOP_doc_content.sour_num , SHOP_doc_content.dest_num " +
                " FROM SHOP_doc_content , SHOP_doc , SHOP_client " +
                " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
                " AND SHOP_doc.client_id = SHOP_client.id " +
                " AND SHOP_doc.is_deleted = 0 " +
                " AND SHOP_doc_content.is_deleted = 0 " +
                " AND ( SHOP_doc_content.sour_id = $reportCatalogDest OR SHOP_doc_content.dest_id = $reportCatalogDest ) " +
                " ORDER BY SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , SHOP_doc.doc_no "

        //--- первичное состояние - по нулям ( используется именно tmWarehouseID,
        //--- т.к. у hmWarehouseName есть ненужный элемент с 0-ым id )
        val hmCurrentHWState = HashMap<Int, Double>()
        for((_, wID) in tmWarehouseID) hmCurrentHWState[wID] = 0.0

        rs = stm.executeQuery(sbSQL)
        while(rs.next()) {
            val rowDocType = rs.getInt(1)

            val isRowUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(rowDocType)
            val isRowUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(rowDocType)
            val isRowUseSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains(rowDocType)
            val isRowUseDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains(rowDocType)
            val isRowUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(rowDocType)
            val isRowUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(rowDocType)

            val sbDoc = StringBuilder()
            //--- указать что во что превратилось ( для пересортицы )
            val sourCatalogID = rs.getInt(2)
            val destCatalogID = rs.getInt(3)
            if(isRowUseSourCatalog && isRowUseDestCatalog)
                sbDoc.append("\nиз: ").append(hmCatalogName[sourCatalogID]).append("\n в: ").append(hmCatalogName[destCatalogID])
            //--- дата накладной
            val docYe = rs.getInt(4)
            val docMo = rs.getInt(5)
            val docDa = rs.getInt(6)
            val docDate = DateTime_DMY(intArrayOf(docYe, docMo, docDa, 0, 0, 0))

            //--- делаем врезку для вставки предыдущих переоценок
            while(!alPriceData.isEmpty()) {
                val priceData = alPriceData[0]
                if(priceData.ye < docYe ||
                    priceData.ye == docYe && priceData.mo < docMo ||
                    priceData.ye == docYe && priceData.mo == docMo && priceData.da <= docDa
                ) {   // здесть именно <=, т.к. переоценку считаем началом дня ДО всех документов

                    val priceDate = DateTime_DMY(intArrayOf(priceData.ye, priceData.mo, priceData.da, 0, 0, 0))
                    alResult.add(
                        0, OperationHistoryResult(
                            -1, priceDate,
                            "${if(priceData.priceType == mPrice.PRICE_TYPE_IN) "Закупочная" else "Розничная"} цена = ${getSplittedDouble(priceData.priceValue, 2)}",
                            0.0, hmCurrentHWState
                        )
                    )

                    alPriceData.removeAt(0)
                } else break
            }

            //--- указать откуда/куда
            var sourWH = rs.getInt(7)
            var destWH = rs.getInt(8)
            if(isRowUseSourWarehouse || isRowUseDestWarehouse) sbDoc.append('\n')
            if(isRowUseSourWarehouse) sbDoc.append(hmWarehouseName[sourWH])
            if(isRowUseSourWarehouse && isRowUseDestWarehouse) sbDoc.append(" -> ")
            if(isRowUseDestWarehouse) sbDoc.append(hmWarehouseName[destWH])
            //--- прочие реквизиты накладной
            val docNo = rs.getString(9)
            if(!docNo.isEmpty()) sbDoc.append("\nНомер накладной: ").append(docNo)
            val clientName = rs.getString(10)
            if(!clientName.isEmpty()) sbDoc.append("\nПокупатель: ").append(clientName)
            val docDescr = rs.getString(11)
            if(!docDescr.isEmpty()) sbDoc.append("\nПримечание: ").append(docDescr)

            //--- определяем операционное кол-во
            val sourNum = rs.getDouble(12)
            val destNum = rs.getDouble(13)

            //--- первый частный случай: "производство" - разные вх./исх. кол-ва
            val operNum = if(isRowUseSourNum && isRowUseDestNum)
                if(reportCatalogDest == sourCatalogID) sourNum else destNum
            else if(isRowUseSourNum) sourNum else destNum
            //--- второй частный случай: "пересортица" - разные исх./вх. наименования,
            //--- соответственно, м.б. или вычитание из одного склада или прибавление к другому
            sourWH = if(isRowUseSourCatalog && isRowUseDestCatalog) if(reportCatalogDest == sourCatalogID) sourWH else 0
            else if(isRowUseSourWarehouse) sourWH else 0
            destWH = if(isRowUseSourCatalog && isRowUseDestCatalog) if(reportCatalogDest == destCatalogID) destWH else 0
            else if(isRowUseDestWarehouse) destWH else 0

            if(sourWH != 0) hmCurrentHWState[sourWH] = hmCurrentHWState[sourWH]!! - operNum
            if(destWH != 0) hmCurrentHWState[destWH] = hmCurrentHWState[destWH]!! + operNum

            alResult.add(0, OperationHistoryResult(rowDocType, docDate, sbDoc.toString(), operNum, hmCurrentHWState))
        }
        rs.close()

        //--- дописываем оставшиеся переоценки
        for(priceData in alPriceData) {
            val priceDate = DateTime_DMY(intArrayOf(priceData.ye, priceData.mo, priceData.da, 0, 0, 0))
            alResult.add(
                0, OperationHistoryResult(
                    -1, priceDate,
                    "${if(priceData.priceType == mPrice.PRICE_TYPE_IN) "Закупочная" else "Розничная"} цена = ${getSplittedDouble(priceData.priceValue, 2)}",
                    0.0, hmCurrentHWState
                )
            )
        }

        return alResult
    }

    private class PriceData(val ye: Int, val mo: Int, val da: Int, val priceType: Int, val priceValue: Double)

    private class OperationHistoryResult(val type: Int, val date: String, val doc: String, val operNum: Double, aHmHWState: Map<Int, Double>) {
        val hmHWState = mutableMapOf<Int, Double>()

        init {
            hmHWState.putAll(aHmHWState)
        }
    }

}

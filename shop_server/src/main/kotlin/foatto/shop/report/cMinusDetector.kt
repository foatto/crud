package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.shop.DocumentTypeConfig
import foatto.shop.mWarehouse
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cMinusDetector : cAbstractReport() {

    private lateinit var hmWarehouseName: Map<Int, String>
    private lateinit var tmWarehouseID: TreeMap<String, Int>
    private lateinit var tmCatalogID: TreeMap<String, Int>

    override fun isFormAutoClick() = true

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        return returnURL ?: getReport()

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
        for (wID in hmWarehouseName.keys)
            if (wID != 0)
            // "все склады" нам не нужны, сами напишем
                tmWarehouseID[hmWarehouseName[wID]!!] = wID
        //--- соберём инфу по элементам каталога
        tmCatalogID = TreeMap()
        val rs = stm.executeQuery(" SELECT name , id FROM SHOP_catalog WHERE id <> 0 AND record_type = ${mAbstractHierarchy.RECORD_TYPE_ITEM} ")
        while (rs.next()) tmCatalogID[rs.getString(1)] = rs.getInt(2)
        rs.close()

        try {
            defineFormats(8, 2, 0)
        } catch (aThrowable: Throwable) {
            aThrowable.printStackTrace()
        }

        var offsY = 0
        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        offsY += 2

        //--- установка размеров и наименований столбцов в зависимости от параметров отчёта
        val alCaption = ArrayList<String>()
        val alDim = ArrayList<Int>()

        alCaption.add("№ п/п")
        alDim.add(5)
        alCaption.add("Наименование")
        alDim.add(-1)
        alCaption.add("Дата")
        alDim.add(9)
        for (wName in tmWarehouseID.keys) {
            alCaption.add(wName)
            alDim.add(7)         // кол-во на каждом складе
        }
        defineRelWidth(alDim, 90)

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        //--- вывод заголовков
        var offsX = 0  // счётчик позиций из-за переменного кол-ва заголовков
        for (caption in alCaption)
            sheet.addCell(Label(offsX++, offsY, caption, wcfCaptionHC))

        offsY++

        var countNN = 1
        for ((catalogName, catalogID) in tmCatalogID) {
            val mdr = detectMinus(catalogID) ?: continue

            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(offsX++, offsY, catalogName, wcfCellL))
            sheet.addCell(
                Label(
                    offsX++, offsY,
                    DateTime_DMY(mdr.arrDT), wcfCellC
                )
            )

            for ((_, wID) in tmWarehouseID) {
                val num = mdr.hmHWState[wID]!!
                sheet.addCell(
                    Label(
                        offsX++,
                        offsY,
                        getSplittedDouble(num, -1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                        if (num < 0) wcfCellCBRedStd else wcfCellCGrayStd
                    )
                )
            }
            offsY++
        }
        offsY++

        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
        //sheet.mergeCells( 1, offsY, 3, offsY );
    }

    private fun detectMinus(catalogID: Int): MinusDetectorResult? {

        val sSQL = " SELECT SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
            " SHOP_doc.doc_type , SHOP_doc_content.sour_id , SHOP_doc_content.dest_id , " +
            " SHOP_doc.sour_id , SHOP_doc.dest_id , " +
            " SHOP_doc_content.sour_num , SHOP_doc_content.dest_num " +
            " FROM SHOP_doc_content , SHOP_doc " +
            " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
            " AND SHOP_doc.is_deleted = 0 " +
            " AND ( SHOP_doc_content.sour_id = $catalogID " +
            "    OR SHOP_doc_content.dest_id = $catalogID " +
            " ) " +
            " ORDER BY SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da "

        val arrLastDate = arrayOf(0, 0, 0, 0, 0, 0)
        //--- первичное состояние - по нулям (используется именно tmWarehouseID,
        //--- т.к. у hmWarehouseName есть ненужный элемент с 0-ым id)
        val hmCurrentHWState = mutableMapOf<Int, Double>()
        for (wName in tmWarehouseID.keys) {
            hmCurrentHWState[tmWarehouseID[wName]!!] = 0.0
        }

        val rs = stm.executeQuery(sSQL)
        while (rs.next()) {
            val arrCurDate = arrayOf(rs.getInt(1), rs.getInt(2), rs.getInt(3))

            //--- если дата сменилась - проверим текущее состояние на минусовость
            if (arrCurDate[0] != arrLastDate[0] || arrCurDate[1] != arrLastDate[1] || arrCurDate[2] != arrLastDate[2]) {

                for (whValue in hmCurrentHWState.values) {
                    if (whValue < 0) {
                        return MinusDetectorResult(arrLastDate, hmCurrentHWState)
                    }
                }

                arrLastDate[0] = arrCurDate[0]
                arrLastDate[1] = arrCurDate[1]
                arrLastDate[2] = arrCurDate[2]
            }

            val rowDocType = rs.getInt(4)

            val isRowUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(rowDocType)
            val isRowUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(rowDocType)
            val isRowUseSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains(rowDocType)
            val isRowUseDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains(rowDocType)
            val isRowUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(rowDocType)
            val isRowUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(rowDocType)

            //--- указать что во что превратилось (для операций пересортице)
            val sourCatalogID = rs.getInt(5)
            val destCatalogID = rs.getInt(6)
            //--- указать откуда/куда
            var sourWH = rs.getInt(7)
            var destWH = rs.getInt(8)

            //--- определяем операционное кол-во
            val sourNum = rs.getDouble(9)
            val destNum = rs.getDouble(10)

            //--- первый частный случай: "производство" - разные вх./исх. кол-ва
            val operNum = if (isRowUseSourNum && isRowUseDestNum)
                if (catalogID == sourCatalogID) sourNum else destNum
            else
                if (isRowUseSourNum) sourNum else destNum
            //--- второй частный случай: пересортице - разные исх./вх. наименования,
            //--- соответственно, м.б. или вычитание из одного склада или прибавление к другому
            sourWH = if (isRowUseSourCatalog && isRowUseDestCatalog)
                if (catalogID == sourCatalogID) sourWH else 0
            else
                if (isRowUseSourWarehouse) sourWH else 0
            destWH = if (isRowUseSourCatalog && isRowUseDestCatalog)
                if (catalogID == destCatalogID) destWH else 0
            else
                if (isRowUseDestWarehouse) destWH else 0

            if (sourWH != 0)
                hmCurrentHWState[sourWH] = hmCurrentHWState[sourWH]!! - operNum
            if (destWH != 0)
                hmCurrentHWState[destWH] = hmCurrentHWState[destWH]!! + operNum
        }
        rs.close()

        //--- проверка окончательного состояния
        for (whValue in hmCurrentHWState.values)
            if (whValue < 0)
                return MinusDetectorResult(arrLastDate, hmCurrentHWState)
        //--- не нашли минусов
        return null
    }

    private class MinusDetectorResult(val arrDT: Array<Int>, val hmHWState: MutableMap<Int, Double>)

}

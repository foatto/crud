package foatto.shop.report

import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.cAbstractHierarchy
import foatto.shop.DocumentTypeConfig
import foatto.shop.PriceData
import foatto.shop.cCatalog
import jxl.CellView
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cPriceList : cAbstractCatalogReport() {

    private lateinit var hmDestCount: MutableMap<Int, MutableMap<Int, Double>>
    private lateinit var hmSourCount: MutableMap<Int, MutableMap<Int, Double>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isFormAutoClick(): Boolean = true

    override fun postReport(sheet: WritableSheet) {

        collectWarehouseInfo()
        collectItemInfo()
        hmCatalogParent = cAbstractHierarchy.getCatalogParent(stm, "SHOP_catalog")

        hmDestCount = mutableMapOf()
        hmSourCount = mutableMapOf()
        cCatalog.loadCatalogCount(stm, null, null, hmDestCount, hmSourCount)

        tmItem = TreeMap()
        //--- рекурсивный расчет
        val ciRoot = CalcItem(hmItemInfo[0]!!, null)
        tmItem[ciRoot.ii.name] = ciRoot
        calcItem(ciRoot)

        defineFormats(8, 2, 0)

        var offsY = 0

        sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleL))
        offsY++

        //--- установка размеров заголовков ( общая ширина = 90/140 для А4 портрет/ландшафт, левые/правые поля по 10 мм )
        //--- общая формула столбцов: W = 5 + N + 7 + (  wh_count + 1  ) * 7 + 11 = 90,
        //--- причем в hmWarehouseName уже входит 0-й элемент для столбца "ВСЕ"
        val alDim = ArrayList<Int>()
        alDim.add(5)     // "N п/п"
        alDim.add(76)    // "Наименование"
        alDim.add(9)     // "Цена"

        for (i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- вывод заголовков
        var offsX = 0  // счётчик позиций из-за переменного кол-ва заголовков

        sheet.addCell(Label(offsX++, offsY, "№ п/п", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Наименование", wcfCaptionHC))
        sheet.addCell(Label(offsX++, offsY, "Цена", wcfCaptionHC))
        offsY++

        //--- рекурсивный вывод
        offsY = outItem(sheet, offsY, ciRoot)

        offsY++
        sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
    }

    //--- рекурсивный расчёт
    private fun calcItem(ci: CalcItem) {
        //--- если это папка, пройдёмся рекурсивно по подпапкам и элементам
        if (ci.ii.isFolder) {
            ci.tmSubItem = TreeMap()
            val alSubID = hmCatalogParent[ci.ii.id]
            if (alSubID != null)
                for (subID in alSubID) {
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
            for ((name, wid) in tmWarehouseID) {
                //--- специфика подсчёта отличается в зависимости от типа накладной
                //--- ( иначе сумма перемещённых товаров всегда будет == 0, т.к. там всегда сколько ушло, столько и пришло )
                val wcCount = cCatalog.calcCatalogCount(if (isUseDestCatalog) hmDestCount else null, if (isUseSourCatalog) hmSourCount else null, ci.ii.id, wid)
                //--- если тип накладной не указан, то это чистое суммирование ( т.е. состояние склада ),
                //--- иначе сумма по накладным/операциям
                //--- в отчёте по состоянию склада считаются все типы накладных
                //if(  reportDocumentType != DocumentTypeConfig.TYPE_ALL  ) wcCount = Math.abs(  wcCount  );
                ci.tmWHCount[name] = wcCount
            }
            //--- просуммируем для всех родителей
            var parentName = ci.parentName
            while (parentName != null) {
                val parentItem = tmItem[parentName]
                var itemCount = 0
                for (name in ci.tmWHCount.keys) {
                    val parentCount = parentItem!!.tmWHCount[name]
                    parentItem.tmWHCount[name] = (parentCount ?: 0.0) + ci.tmWHCount[name]!!
                    //--- отдельно отслеживаем общее операционное кол-во по всем складам
                    itemCount += ci.tmWHCount[name]!!.toInt()
                }
                //--- если кол-во отлично от 0 - увеличиваем счётчик кол-ва наименований
                if (itemCount != 0) parentItem!!.subItemCount++
                parentName = parentItem!!.parentName
            }
        }
    }

    //--- рекурсивный вывод
    private fun outItem(sheet: WritableSheet, aOffsY: Int, ci: CalcItem): Int {
        var offsY = aOffsY
        //--- пропускаем строки с полностью нулевыми количествами
        var isExist = false
        for (wName in tmWarehouseID.keys) {
            val count = ci.tmWHCount[wName]
            if (count != null && count != 0.0) {
                isExist = true
                break
            }
        }
        if (!isExist) return offsY

        //--- при выводе прайс-листа первую строку "ВСЕГО" не выводим
        if (ci.ii.name == ROOT_ITEM_NAME) {
        } else {
            //--- пишем номер строки только для товара
            if (!ci.ii.isFolder) sheet.addCell(Label(0, offsY, (countNN++).toString(), wcfNN))

            val appendStr = if (ci.ii.isFolder && (ci.parentName == null || ci.parentName == ROOT_ITEM_NAME)) "\n" else ""
            sheet.addCell(Label(1, offsY, "$appendStr${ci.ii.name}$appendStr", if (ci.ii.isFolder) wcfCellLBStdYellow else wcfCellL))

            if (!ci.ii.isFolder) {
                sheet.addCell(
                    Label(
                        2,
                        offsY,
                        getSplittedDouble(PriceData.getPrice(hmPrice, ci.ii.id, getCurrentTimeInt()), 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                        wcfCellR
                    )
                )
            }
            offsY++
        }

        if (ci.tmSubItem != null) {
            //--- сначала выводим элементы из своей папки, только потом подпапки
            for (name in ci.tmSubItem!!.keys) {
                val ciSub = ci.tmSubItem!![name]!!
                if (ciSub.tmSubItem == null) offsY = outItem(sheet, offsY, ciSub)
            }
            for (name in ci.tmSubItem!!.keys) {
                val ciSub = ci.tmSubItem!![name]!!
                if (ciSub.tmSubItem != null) offsY = outItem(sheet, offsY, ciSub)
            }
        }
        return offsY
    }
}
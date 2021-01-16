package foatto.shop.report

//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.JSON
//import kotlinx.serialization.stringify
import foatto.core.link.FormData
import foatto.core.link.XyDocumentConfig
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_DMY
import foatto.core.util.getSplittedDouble
import foatto.core.util.getWordOfCount
import foatto.core.util.getWordOfMoney
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.data.DataComboBox
import foatto.core_server.app.server.data.DataInt
import foatto.shop.DocumentTypeConfig
import foatto.shop.PriceData
import foatto.shop.iShopApplication
import foatto.shop.mPrice
import foatto.shop.mWarehouse
import foatto.shop.toJson
import foatto.sql.CoreAdvancedStatement
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

//@kotlinx.serialization.ImplicitReflectionSerializer
class cDocContent : cAbstractReport() {

    private val arrDocTitle = arrayOf("ИП Карипова Гульнара Дамировна", "ИНН 165007039790  ОГРН 318169000001873")

    //--- заранее определяем формат отчёта
    private var isWideReport = false
    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPrice = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
    }

    override fun isFormAutoClick(): Boolean {
        for (an in DocumentTypeConfig.hmDocTypeAlias.values)
            if (hmParentData[an] != null) return true

        return super.isFormAutoClick()
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if(returnURL != null) return returnURL

        val mdc = model as mDocContent

        val docID = (hmColumnData[mdc.columnDocument] as DataInt).value
        val docType = (hmColumnData[mdc.columnDocumentType] as DataComboBox).value

        //--- выборка данных параметров для отчета
        hmReportParam["report_document"] = docID
        hmReportParam["report_document_client"] = (hmColumnData[mdc.columnClient] as DataInt).value
        hmReportParam["report_document_type"] = docType
        hmReportParam["report_catalog_dest"] = (hmColumnData[mdc.columnCatalogDest] as DataInt).value

        if(aliasConfig.alias == "shop_fiscal_doc_content") {
            //--- дополнительная проверка (вдруг ещё нажимали на кнопку в том же окне)
            var isFiscable = false
            if(docType == DocumentTypeConfig.TYPE_OUT && docID != 0) {
                val rs = stm.executeQuery(" SELECT is_fiscaled FROM SHOP_doc WHERE id = $docID ")
                isFiscable = rs.next() && rs.getInt(1) == 0
                rs.close()
            }
            if(isFiscable) printFiscal(docID)
            return "#"
        } else return getReport()
    }

    override fun getReport(): String {
        val reportDocumentType = hmReportParam["report_document_type"] as Int

        isWideReport = reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT

        return super.getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = if(isWideReport) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT

        printMarginLeft = if(isWideReport) 10 else 20
        printMarginRight = 10
        printMarginTop = if(isWideReport) 20 else 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 1.0
        printKeyH = 2.0
    }

    override fun postReport(sheet: WritableSheet) {

        //--- загрузка стартовых параметров
        val reportDocument = hmReportParam["report_document"] as Int
        val reportDocumentClient = hmReportParam["report_document_client"] as Int
        val reportDocumentType = hmReportParam["report_document_type"] as Int
        val reportCatalogDest = hmReportParam["report_catalog_dest"] as Int

        val hmWarehouseName = mWarehouse.fillWarehouseMap(stm)
        var discount = 0.0

        defineFormats(8, 2, 0)

        var offsY = 0

        if (reportDocument != 0 && reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
            for (docTitle in arrDocTitle) {
                sheet.addCell(Label(1, offsY++, docTitle, wcfTitleValue))
            }
        } else {
            sheet.addCell(Label(1, offsY++, aliasConfig.descr, wcfTitleC))
        }
        offsY++

        //--- заголовок отчёта зависит от указанных параметров
        if(reportDocument != 0) {
            val rs = stm.executeQuery(
                " SELECT SHOP_doc.sour_id , SHOP_doc.dest_id , SHOP_doc.doc_no , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
                    " SHOP_client.name , SHOP_doc.descr , SHOP_doc.discount " +
                    " FROM SHOP_doc , SHOP_client " +
                    " WHERE SHOP_doc.client_id = SHOP_client.id AND SHOP_doc.id = $reportDocument "
            )
            if(rs.next()) {
                //--- товарный чек будем оформлять официально
                if(reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
                    sheet.addCell(
                        Label(
                            1, offsY++, "Товарный чек № ${rs.getString(3)} от " +
                                DateTime_DMY(intArrayOf(rs.getInt(4), rs.getInt(5), rs.getInt(6), 0, 0, 0)), wcfTitleC
                        )
                    )
                    offsY++

                    sheet.addCell(Label(1, offsY++, "Покупатель: ${rs.getString(7)}", wcfTitleValue))
                } else {
                    sheet.addCell(Label(1, offsY, "Тип накладной:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[reportDocumentType]]!!.descr, wcfTitleValue))
                    offsY++

                    //                //--- своя реализация шапки отчёта
                    //                if(  reportDocumentType == DocumentTypeConfig.TYPE_OUT  ) {
                    //                    sheet.addCell(  new Label(  1, offsY, "Поставщик:", wcfTitleName  )  );
                    //
                    //                    mAbstractReport mar = ( mAbstractReport ) model;
                    //                    String reportCap = ( String ) hmReportParam.get(  mar.getReportCapFieldName()  );
                    //                    if(  reportCap != null  ) {
                    //                        StringTokenizer st = new StringTokenizer(  reportCap, "\n"  );
                    //                        if(  st.hasMoreTokens()  )
                    //                            sheet.addCell(  new Label(  2, offsY, st.nextToken(), wcfTitleValue  )  );
                    //                    }
                    //                    offsY++;
                    //                }

                    if(DocumentTypeConfig.hsUseSourWarehouse.contains(reportDocumentType)) {
                        sheet.addCell(Label(1, offsY, "Со склада/магазина:", wcfTitleName))
                        sheet.addCell(Label(2, offsY, hmWarehouseName[rs.getInt(1)], wcfTitleValue))
                        offsY++
                    }

                    if(DocumentTypeConfig.hsUseDestWarehouse.contains(reportDocumentType)) {
                        sheet.addCell(Label(1, offsY, "На склад/магазин:", wcfTitleName))
                        sheet.addCell(Label(2, offsY, hmWarehouseName[rs.getInt(2)], wcfTitleValue))
                        offsY++
                    }

                    sheet.addCell(Label(1, offsY, "Номер накладной:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(3), wcfTitleValue))
                    offsY++

                    sheet.addCell(Label(1, offsY, "Дата:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, DateTime_DMY(intArrayOf(rs.getInt(4), rs.getInt(5), rs.getInt(6), 0, 0, 0)), wcfTitleValue))
                    offsY++

                    sheet.addCell(Label(1, offsY, "Контрагент:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(7), wcfTitleValue))
                    offsY++

                    sheet.addCell(Label(1, offsY, "Примечание:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(8), wcfTitleValue))
                    offsY++
                }
                //--- пригодится в конце отчёта
                discount = rs.getDouble(9)
            }
            rs.close()
        } else {
            if(reportDocumentClient != 0) {
                val rs = stm.executeQuery(" SELECT name FROM SHOP_client WHERE id = $reportDocumentClient ")
                if(rs.next()) {
                    sheet.addCell(Label(1, offsY, "Контрагент:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(1), wcfTitleValue))
                    offsY++
                }
                rs.close()
            }
            if(reportDocumentType != 0) {
                sheet.addCell(Label(1, offsY, "Тип накладной:", wcfTitleName))
                sheet.addCell(Label(2, offsY, hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[reportDocumentType]]!!.descr, wcfTitleValue))
                offsY++
            }
            if(reportCatalogDest != 0) {
                val rs = stm.executeQuery(" SELECT name FROM SHOP_catalog WHERE id = $reportCatalogDest ")
                if(rs.next()) {
                    sheet.addCell(Label(1, offsY, "Наименование:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(1), wcfTitleValue))
                    offsY++
                }
                rs.close()
            }
        }
        offsY++

        //boolean isUseSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains(  reportDocumentType  );
        //boolean isUseDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains(  reportDocumentType  );
        val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(reportDocumentType)
        val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(reportDocumentType)
        val isUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(reportDocumentType)
        val isUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(reportDocumentType)

        //--- установка размеров и наименований столбцов в зависимости от параметров отчёта
        val alCaption = ArrayList<String>()
        val alDim = ArrayList<Int>()

        alCaption.add("№ п/п")
        alDim.add(5)

        if(reportDocument == 0) {
            alCaption.add("Накладная")
            alDim.add(30)
        }
        if(isUseSourCatalog) {
            alCaption.add(if(reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Исх. наименование" else "Наименование")
            alDim.add(-1)    // переменная/относительная ширина
            alCaption.add("Цена")
            alDim.add(7)
        }
        if(isUseSourNum) {
            alCaption.add(
                if(reportDocumentType == DocumentTypeConfig.TYPE_ALL) "Исх. кол-во"
                else "Кол-во"
            )
            alDim.add(7)
        }
        if(isUseDestCatalog) {
            alCaption.add(if(reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Вх. наименование" else "Наименование")
            alDim.add(-1)    // переменная/относительная ширина
            alCaption.add("Цена")
            alDim.add(7)
        }
        if(isUseDestNum) {
            alCaption.add(if(reportDocumentType == DocumentTypeConfig.TYPE_ALL) "Вх. кол-во" else "Кол-во")
            alDim.add(7)
        }
        alCaption.add("Стоимость")
        alDim.add(11)
        //--- суммируем постоянные ширины, вычисляем остаток, раскидываем по столбцам переменной ширины
        var captionConstWidthSum = 0
        var captionRelWidthSum = 0
        for(w in alDim)
            if(w > 0) captionConstWidthSum += w
            else captionRelWidthSum += w
        //--- получаем минусовую ширину на одну относительную ед.ширины
        val captionRelWidth = ((if(isWideReport) 140 else 90) - captionConstWidthSum) / captionRelWidthSum
        //--- устанавливаем полученные остатки ширины ( минус на минус как раз даёт плюс )
        for(i in alDim.indices)
            if(alDim[i] < 0) alDim[i] = alDim[i] * captionRelWidth

        for(i in alDim.indices) {
            val cvNN = CellView()
            cvNN.size = alDim[i] * 256
            sheet.setColumnView(i, cvNN)
        }
        //--- вывод заголовков
        var offsX = 0  // счётчик позиций из-за переменного кол-ва заголовков
        for(caption in alCaption)
            sheet.addCell(Label(offsX++, offsY, caption, wcfCaptionHC))

        offsY++

        //--- составим мегазапрос на вывод состава накладной( ых )
        var sWhereAND = ""
        if(reportDocument != 0) sWhereAND += " AND SHOP_doc.id = $reportDocument "
        else {
            if(reportDocumentClient != 0) sWhereAND += " AND SHOP_doc.client_id = $reportDocumentClient "
            if(reportDocumentType != 0) sWhereAND += " AND SHOP_doc.doc_type = $reportDocumentType "
            if(reportCatalogDest != 0) {
                sWhereAND += " AND "
                //--- используются оба номенклатурных поля
                if(isUseSourCatalog && isUseDestCatalog) sWhereAND += " ( SHOP_doc_content.sour_id = $reportCatalogDest OR SHOP_doc_content.dest_id = $reportCatalogDest ) "
                else if(isUseSourCatalog) sWhereAND += " SHOP_doc_content.sour_id = $reportCatalogDest "
                else if(isUseDestCatalog) sWhereAND += " SHOP_doc_content.dest_id = $reportCatalogDest "
            }
        }

        var sOrderBy = ""
        if(reportDocument == 0) sOrderBy += " SHOP_doc.doc_ye DESC , SHOP_doc.doc_mo DESC , SHOP_doc.doc_da DESC , SHOP_doc.doc_no DESC "
        else {
            if(isUseSourCatalog) sOrderBy += (if(sOrderBy.isEmpty()) "" else " , ") + " SHOP_catalog_1.name ASC "
            if(isUseDestCatalog) sOrderBy += (if(sOrderBy.isEmpty()) "" else " , ") + " SHOP_catalog_2.name ASC "
        }

        val sSQL =
            " SELECT SHOP_doc.doc_type , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
                " SHOP_doc.sour_id , SHOP_doc.dest_id , SHOP_doc.doc_no , SHOP_client.name , SHOP_doc.descr , " +
                " SHOP_catalog_1.id , SHOP_catalog_1.name , SHOP_doc_content.sour_num , " +
                " SHOP_catalog_2.id , SHOP_catalog_2.name , SHOP_doc_content.dest_num " +
                " FROM SHOP_doc_content , SHOP_doc , SHOP_client , SHOP_catalog SHOP_catalog_1 , SHOP_catalog SHOP_catalog_2 " +
                " WHERE SHOP_doc_content.doc_id = SHOP_doc.id " +
                " AND SHOP_doc_content.is_deleted = 0 " +
                " AND SHOP_doc_content.sour_id = SHOP_catalog_1.id " + "" +
                " AND SHOP_doc_content.dest_id = SHOP_catalog_2.id " +
                " AND SHOP_doc.client_id = SHOP_client.id " +
                sWhereAND +
                " ORDER BY $sOrderBy "

        val rs = stm.executeQuery(sSQL)
        var countNN = 1
        var sumNum = 0.0
        var sumCostOut = 0.0
        while(rs.next()) {
            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))

            val rowDocType = rs.getInt(1)

            val isRowUseSourWarehouse = DocumentTypeConfig.hsUseSourWarehouse.contains(rowDocType)
            val isRowUseDestWarehouse = DocumentTypeConfig.hsUseDestWarehouse.contains(rowDocType)
            val isRowUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(rowDocType)
            val isRowUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(rowDocType)
            val isRowUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(rowDocType)
            val isRowUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(rowDocType)

            val docYe = rs.getInt(2)
            val docMo = rs.getInt(3)
            val docDa = rs.getInt(4)

            if(reportDocument == 0) {
                val sb = StringBuilder()
                if(reportDocumentType == DocumentTypeConfig.TYPE_ALL)
                    sb.append(hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[rowDocType]]!!.descr).append('\n')
                sb.append(DateTime_DMY(intArrayOf(docYe, docMo, docDa, 0, 0, 0))).append('\n')
                if(isRowUseSourWarehouse)
                    sb.append(if(reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Склад / магазин: " else "Со склада / магазина: ")
                        .append(hmWarehouseName[rs.getInt(5)]).append('\n')
                if(isRowUseDestWarehouse) sb.append("На склад / магазин: ").append(hmWarehouseName[rs.getInt(6)]).append('\n')
                //--- прочие реквизиты накладной
                val docNo = rs.getString(7)
                if (docNo.isNotEmpty()) sb.append("Номер накладной: ").append(docNo).append('\n')
                val clientName = rs.getString(8)
                if (clientName.isNotEmpty()) sb.append("Покупатель: ").append(clientName).append('\n')
                val docDescr = rs.getString(9)
                if (docDescr.isNotEmpty()) sb.append("Примечание: ").append(docDescr).append('\n')

                sheet.addCell(Label(offsX++, offsY, sb.toString(), wcfCellC))
            }

            var num = 0.0
            var priceOut = 0.0
            if(isUseSourCatalog) {
                if(isRowUseSourCatalog) priceOut = PriceData.getPrice(hmPrice, rs.getInt(10), zoneId, docYe, docMo, docDa)

                sheet.addCell(Label(offsX++, offsY, if(isRowUseSourCatalog) rs.getString(11) else "", wcfCellL))
                sheet.addCell(Label(offsX++, offsY, if(isRowUseSourCatalog) getSplittedDouble(priceOut, 2).toString() else "", wcfCellR))
            }
            if(isUseSourNum) {
                if(isRowUseSourNum) num = rs.getDouble(12)
                sheet.addCell(Label(offsX++, offsY, if(isRowUseSourNum) getSplittedDouble(num, -1).toString() else "", wcfCellC))
            }
            if(isUseDestCatalog) {
                if(isRowUseDestCatalog) priceOut = PriceData.getPrice(hmPrice, rs.getInt(13), zoneId, docYe, docMo, docDa)

                sheet.addCell(Label(offsX++, offsY, if(isRowUseDestCatalog) rs.getString(14) else "", wcfCellL))
                sheet.addCell(Label(offsX++, offsY, if(isRowUseDestCatalog) getSplittedDouble(priceOut, 2).toString() else "", wcfCellR))
            }
            if(isUseDestNum) {
                if(isRowUseDestNum) num = rs.getDouble(15)
                sheet.addCell(Label(offsX++, offsY, if(isRowUseDestNum) getSplittedDouble(num, -1).toString() else "", wcfCellC))
            }
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(priceOut * num, 2).toString(), wcfCellR))

            offsY++

            sumNum += num
            sumCostOut += priceOut * num
        }
        rs.close()
        offsY++

        //--- при выводе состава всех типов накладных сумма бессмысленна
        var discountedCost = 0.0
        if(reportDocumentType != DocumentTypeConfig.TYPE_ALL) {
            sheet.addCell(Label(1, offsY, "ИТОГО:", wcfTextRB))
            sheet.addCell(Label(alCaption.size - 2, offsY, getSplittedDouble(sumNum, -1).toString(), wcfTextCB))
            sheet.addCell(Label(alCaption.size - 1, offsY, getSplittedDouble(sumCostOut, 2).toString(), wcfTextRB))
            offsY++
            if(reportDocument != 0 && reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
                sheet.addCell(Label(1, offsY, "Скидка:", wcfTextRB))

                sheet.addCell(Label(alCaption.size - 2, offsY, getSplittedDouble(discount, 1) + " %", wcfTextCB))
                sheet.addCell(Label(alCaption.size - 1, offsY, getSplittedDouble(Math.ceil(sumCostOut * discount / 100), 2).toString(), wcfTextRB))
                offsY++

                //--- пригодится для вывода текстовой суммы
                discountedCost = Math.floor(sumCostOut * (1 - discount / 100))

                sheet.addCell(Label(1, offsY, "Стоимость со скидкой:", wcfTextRB))
                sheet.addCell(Label(alCaption.size - 1, offsY, getSplittedDouble(discountedCost, 2).toString(), wcfTextRB))
                offsY++
            }
            offsY++
        }

        //--- для накладной на реализацию своя подпись отчёта
        if(reportDocument != 0 && reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
            sheet.addCell(Label(1, offsY, "Всего ${getWordOfCount(countNN - 1, true)} наименований", wcfTextLB))
            sheet.mergeCells(1, offsY, 4, offsY)
            offsY++

            sheet.addCell(Label(1, offsY, "на сумму ${getWordOfMoney(discountedCost)}", wcfTextLB))
            sheet.mergeCells(1, offsY, 4, offsY)
            offsY++

            offsY++
            sheet.addCell(Label(1, offsY, "Отпустил ________________________________________ Получил ________________________________________", wcfTextLB))
            sheet.mergeCells(1, offsY, 4, offsY)
            offsY++
        } else sheet.addCell(Label(1, offsY, getPreparedAt(), wcfCellL))
    }

    fun printFiscal(docID: Int) {
        val shopApplication = application as iShopApplication
        val fiscalURL = shopApplication.fiscalURL ?: return
        val fiscalClient = shopApplication.fiscalClient ?: return
        val fiscalLineCutter = shopApplication.fiscalLineCutter?.toInt() ?: return
        val fiscalTaxMode = shopApplication.fiscalTaxMode?.toInt() ?: return
        val fiscalPlace = shopApplication.fiscalPlace ?: return

        var docYe = 0
        var docMo = 0
        var docDa = 0
        var discount = 0.0

        var rs = stm.executeQuery(" SELECT doc_ye , doc_mo , doc_da , discount FROM SHOP_doc WHERE id = $docID ")
        if (rs.next()) {
            docYe = rs.getInt(1)
            docMo = rs.getInt(2)
            docDa = rs.getInt(3)
            discount = rs.getDouble(4)
        }
        rs.close()

        //--- составим мегазапрос на вывод состава накладной
        val sSQL = " SELECT SHOP_catalog.id , SHOP_catalog.name , SHOP_doc_content.sour_num  " +
            " FROM SHOP_doc_content , SHOP_catalog " +
            " WHERE SHOP_doc_content.doc_id = $docID " +
            " AND SHOP_doc_content.is_deleted = 0 " +
            " AND SHOP_doc_content.sour_id = SHOP_catalog.id "

        rs = stm.executeQuery(sSQL)
        var sumCostOut = 0L
        val alLine = mutableListOf<FiscalLine>()
        while(rs.next()) {
            val price = (PriceData.getPrice(hmPrice, rs.getInt(1), zoneId, docYe, docMo, docDa) * (1 - discount / 100) * 100).toInt()
            val name = rs.getString(2).trim()
            val count = (rs.getDouble(3) * 1000).toInt()

            alLine.add(FiscalLine(Qty = count, Price = price, Description = name.substring(0, min(name.length, fiscalLineCutter))))

            //--- avoid int value overflow to negative values
            sumCostOut += price.toLong() * count.toLong()
        }
        rs.close()

        val query = FiscalQuery(
            Password = 1,
            ClientId = fiscalClient,
            RequestId = docID.toString(),

            Lines = alLine.toTypedArray(),
            Cash = sumCostOut / 1000,
            NonCash = arrayOf(0),
            TaxMode = fiscalTaxMode,
            Place = fiscalPlace,
        )
        sendFiscal(fiscalURL, query)

        stm.executeUpdate(" UPDATE SHOP_doc SET is_fiscaled = 1 WHERE id = $docID ")

        AdvancedLogger.debug(query.toJson())
    }

    fun sendFiscal(fiscalURL: String, query: FiscalQuery) {

        //--- создание подключения
        val url = URL(fiscalURL)
        val urlConn = url.openConnection() as HttpURLConnection

        //--- настройка подключения
        urlConn.requestMethod = "POST"
        urlConn.allowUserInteraction = true
        urlConn.doOutput = true
        urlConn.doInput = true
        urlConn.useCaches = false
        urlConn.setRequestProperty("Content-type", "application/json")

        urlConn.connect()

        val os = urlConn.outputStream
        os.write(query.toJson().toByteArray())
//        os.write( JSON.stringify( query ).toByteArray())
        os.flush()
        os.close()

        val responseCode = urlConn.responseCode
        AdvancedLogger.error(responseCode.toString())
        AdvancedLogger.error(urlConn.responseMessage)

        val br = BufferedReader(InputStreamReader(if(responseCode >= 400) urlConn.errorStream else urlConn.inputStream))
        while(true) {
            val sReturned = br.readLine()
            if(sReturned == null) break
            else AdvancedLogger.error(sReturned)
        }
        br.close()
        urlConn.disconnect()
    }
}

//@Serializable
data class FiscalQuery(
    val Device: String = "auto",
    val Password: Int,
    val ClientId: String,
    val RequestId: String,  // обязательно уникально

    val DocumentType: Int = 0,      // 0 - приход, 1 - расход, 2 - возврат прихода, 3 - возврат расхода
    val Lines: Array<FiscalLine>,

    val Cash: Long,             // сумма наличными
    //--- оплачено карточкой (разделение по типам карточек), если только наличкой, писать [ 0 ],
    //--- иначе писать [ xxx, 0, 0 ]
    val NonCash: Array<Long> = arrayOf(0L),
//    val AdvancePayment: Long,   // предоплата
//    val Credit: Long,           // кредит
//    val Consideration: Long,    // Сумма оплаты встречным предоставлением
    val TaxMode: Int,               // 2 = УСН доход 6%, 32 = ПСН (современный?)
//    val PhoneOrEmail: String,
//    val MaxDocumentsInTurn: Int,  // макс. кол-во документов в одной смене - накуа?
    val FullResponse: Boolean = true,    // иначе чек печататься не будет
    val Place: String,
//    val TaxCalculationMethod: Int = 0,  // метод расчёта налогов в чеке (берётся из настроек кассы, д.б. == 0)

//    val UserRequisite: пользовательские реквизиты
)

//@Serializable
data class FiscalLine(
    val Qty: Int,               // количество * 1000
    val Price: Int,             // цена в копейках, т.е. * 100
//    val SubTotal: Long,         // итог по строке
    val PayAttribute: Int = 1,  // c 2021 года = 1 (Полная предварительная оплата до момента передачи предмета расчёта)
    val TaxId: Int = 4,             // НДС: 4 - "Без налога" или 3 - "НДС 0%"
    val Description: String
)

//@Serializable
data class FiscalDate(
    val Day: Int,
    val Month: Int,
    val Year: Int       // последние две цифры
)

//@Serializable
data class FiscalTime(
    val Hour: Int,
    val Minute: Int,
    val Second: Int       // последние две цифры
)

//@Serializable
data class FiscalDateTime(
    val Date: FiscalDate,
    val Time: FiscalTime
)

fun FiscalQuery.toJson(): String {
    var json = "{"

    json += Device.toJson("Device") + ","
    json += Password.toJson("Password") + ","
    json += ClientId.toJson("ClientId") + ","
    json += RequestId.toJson("RequestId") + ","
    json += DocumentType.toJson("DocumentType") + ","

    json += "\"Lines\":["

    for (formData in Lines)
        json += "${formData.toJson()},"

    if (Lines.isNotEmpty())
        json = json.substring(0, json.length - 1)

    json += "],"

    json += Cash.toJson("Cash") + ","
    json += NonCash.toJson("NonCash") + ","

    json += TaxMode.toJson("TaxMode") + ","
    json += FullResponse.toJson("FullResponse") + ","
    json += Place.toJson("Place")

    return "$json}"
}

fun FiscalLine.toJson(): String {
    var json = "{"

    json += Qty.toJson("Qty") + ","
    json += Price.toJson("Price") + ","
    json += PayAttribute.toJson("PayAttribute") + ","
    json += TaxId.toJson("TaxId") + ","
    json += Description.toJson("Description")

    return "$json}"
}
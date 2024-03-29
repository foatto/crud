package foatto.shop.report

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import foatto.core.link.FormData
import foatto.core.link.XyDocumentConfig
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
import foatto.shop.report.fiscal.Atol
import foatto.shop.report.fiscal.iFiscal
import foatto.sql.CoreAdvancedConnection
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.logging.*
import io.ktor.http.*
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

class cDocContent : cAbstractReport() {

    private val arrDocTitle = arrayOf("ИП Карипова Гульнара Дамировна", "ИНН 165007039790  ОГРН 318169000001873")

    private val objectMapper = jacksonObjectMapper()

    private val httpClient = HttpClient(Apache).config {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
//                when {
//                    logOptions.contains("debug") -> LogLevel.ALL
//                    logOptions.contains("error") -> LogLevel.HEADERS
//                    logOptions.contains("info") -> LogLevel.INFO
//                    else -> LogLevel.NONE
//                }
        }
        install(HttpTimeout)
        defaultRequest {
            url.protocol = URLProtocol.HTTP //URLProtocol.HTTPS
        }
    }

    //--- заранее определяем формат отчёта
    private var isWideReport = false
    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aConn, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPrice = PriceData.loadPrice(conn, mPrice.PRICE_TYPE_OUT)
    }

    override fun isFormAutoClick(): Boolean {
        for (an in DocumentTypeConfig.hmDocTypeAlias.values) {
            if (getParentId(an) != null) {
                return true
            }
        }

        return super.isFormAutoClick()
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {
        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val mdc = model as mDocContent

        val docID = (hmColumnData[mdc.columnDocument] as DataInt).intValue
        val docType = (hmColumnData[mdc.columnDocumentType] as DataComboBox).intValue

        //--- выборка данных параметров для отчета
        hmReportParam["report_document"] = docID
        hmReportParam["report_document_client"] = (hmColumnData[mdc.columnClient] as DataInt).intValue
        hmReportParam["report_document_type"] = docType
        hmReportParam["report_catalog_dest"] = (hmColumnData[mdc.columnCatalogDest] as DataInt).intValue

        return if (aliasConfig.name == "shop_fiscal_doc_content") {
            //--- дополнительная проверка (вдруг ещё нажимали на кнопку в том же окне)
            var isFiscable = false
            if (docType == DocumentTypeConfig.TYPE_OUT && docID != 0) {
                val rs = conn.executeQuery(" SELECT is_fiscaled FROM SHOP_doc WHERE id = $docID ")
                isFiscable = rs.next() && rs.getInt(1) == 0
                rs.close()
            }
            if (isFiscable) {
                printFiscal(docID)
            }
            "#"
        } else {
            getReport()
        }
    }

    override fun getReport(): String {
        val reportDocumentType = hmReportParam["report_document_type"] as Int

        isWideReport = reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT

        return super.getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = if (isWideReport) PageOrientation.LANDSCAPE else PageOrientation.PORTRAIT

        printMarginLeft = if (isWideReport) 10 else 20
        printMarginRight = 10
        printMarginTop = if (isWideReport) 20 else 10
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

        val hmWarehouseName = mWarehouse.fillWarehouseMap(conn)
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
        if (reportDocument != 0) {
            val rs = conn.executeQuery(
                " SELECT SHOP_doc.sour_id , SHOP_doc.dest_id , SHOP_doc.doc_no , SHOP_doc.doc_ye , SHOP_doc.doc_mo , SHOP_doc.doc_da , " +
                    " SHOP_client.name , SHOP_doc.descr , SHOP_doc.discount " +
                    " FROM SHOP_doc , SHOP_client " +
                    " WHERE SHOP_doc.client_id = SHOP_client.id AND SHOP_doc.id = $reportDocument "
            )
            if (rs.next()) {
                //--- товарный чек будем оформлять официально
                if (reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
                    sheet.addCell(
                        Label(
                            1, offsY++, "Товарный чек № ${rs.getString(3)} от " +
                                DateTime_DMY(arrayOf(rs.getInt(4), rs.getInt(5), rs.getInt(6), 0, 0, 0)), wcfTitleC
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

                    if (DocumentTypeConfig.hsUseSourWarehouse.contains(reportDocumentType)) {
                        sheet.addCell(Label(1, offsY, "Со склада/магазина:", wcfTitleName))
                        sheet.addCell(Label(2, offsY, hmWarehouseName[rs.getInt(1)], wcfTitleValue))
                        offsY++
                    }

                    if (DocumentTypeConfig.hsUseDestWarehouse.contains(reportDocumentType)) {
                        sheet.addCell(Label(1, offsY, "На склад/магазин:", wcfTitleName))
                        sheet.addCell(Label(2, offsY, hmWarehouseName[rs.getInt(2)], wcfTitleValue))
                        offsY++
                    }

                    sheet.addCell(Label(1, offsY, "Номер накладной:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(3), wcfTitleValue))
                    offsY++

                    sheet.addCell(Label(1, offsY, "Дата:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, DateTime_DMY(arrayOf(rs.getInt(4), rs.getInt(5), rs.getInt(6), 0, 0, 0)), wcfTitleValue))
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
            if (reportDocumentClient != 0) {
                val rs = conn.executeQuery(" SELECT name FROM SHOP_client WHERE id = $reportDocumentClient ")
                if (rs.next()) {
                    sheet.addCell(Label(1, offsY, "Контрагент:", wcfTitleName))
                    sheet.addCell(Label(2, offsY, rs.getString(1), wcfTitleValue))
                    offsY++
                }
                rs.close()
            }
            if (reportDocumentType != 0) {
                sheet.addCell(Label(1, offsY, "Тип накладной:", wcfTitleName))
                sheet.addCell(Label(2, offsY, hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[reportDocumentType]]!!.descr, wcfTitleValue))
                offsY++
            }
            if (reportCatalogDest != 0) {
                val rs = conn.executeQuery(" SELECT name FROM SHOP_catalog WHERE id = $reportCatalogDest ")
                if (rs.next()) {
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

        if (reportDocument == 0) {
            alCaption.add("Накладная")
            alDim.add(30)
        }
        if (isUseSourCatalog) {
            alCaption.add(if (reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Исх. наименование" else "Наименование")
            alDim.add(-1)    // переменная/относительная ширина
            alCaption.add("Цена")
            alDim.add(7)
        }
        if (isUseSourNum) {
            alCaption.add(
                if (reportDocumentType == DocumentTypeConfig.TYPE_ALL) "Исх. кол-во"
                else "Кол-во"
            )
            alDim.add(7)
        }
        if (isUseDestCatalog) {
            alCaption.add(if (reportDocumentType == DocumentTypeConfig.TYPE_ALL || reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Вх. наименование" else "Наименование")
            alDim.add(-1)    // переменная/относительная ширина
            alCaption.add("Цена")
            alDim.add(7)
        }
        if (isUseDestNum) {
            alCaption.add(if (reportDocumentType == DocumentTypeConfig.TYPE_ALL) "Вх. кол-во" else "Кол-во")
            alDim.add(7)
        }
        alCaption.add("Стоимость")
        alDim.add(11)
        //--- суммируем постоянные ширины, вычисляем остаток, раскидываем по столбцам переменной ширины
        var captionConstWidthSum = 0
        var captionRelWidthSum = 0
        for (w in alDim)
            if (w > 0) captionConstWidthSum += w
            else captionRelWidthSum += w
        //--- получаем минусовую ширину на одну относительную ед.ширины
        val captionRelWidth = ((if (isWideReport) 140 else 90) - captionConstWidthSum) / captionRelWidthSum
        //--- устанавливаем полученные остатки ширины ( минус на минус как раз даёт плюс )
        for (i in alDim.indices)
            if (alDim[i] < 0) alDim[i] = alDim[i] * captionRelWidth

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

        //--- составим мегазапрос на вывод состава накладной( ых )
        var sWhereAND = ""
        if (reportDocument != 0) sWhereAND += " AND SHOP_doc.id = $reportDocument "
        else {
            if (reportDocumentClient != 0) sWhereAND += " AND SHOP_doc.client_id = $reportDocumentClient "
            if (reportDocumentType != 0) sWhereAND += " AND SHOP_doc.doc_type = $reportDocumentType "
            if (reportCatalogDest != 0) {
                sWhereAND += " AND "
                //--- используются оба номенклатурных поля
                if (isUseSourCatalog && isUseDestCatalog) sWhereAND += " ( SHOP_doc_content.sour_id = $reportCatalogDest OR SHOP_doc_content.dest_id = $reportCatalogDest ) "
                else if (isUseSourCatalog) sWhereAND += " SHOP_doc_content.sour_id = $reportCatalogDest "
                else if (isUseDestCatalog) sWhereAND += " SHOP_doc_content.dest_id = $reportCatalogDest "
            }
        }

        var sOrderBy = ""
        if (reportDocument == 0) sOrderBy += " SHOP_doc.doc_ye DESC , SHOP_doc.doc_mo DESC , SHOP_doc.doc_da DESC , SHOP_doc.doc_no DESC "
        else {
            if (isUseSourCatalog) sOrderBy += (if (sOrderBy.isEmpty()) "" else " , ") + " SHOP_catalog_1.name ASC "
            if (isUseDestCatalog) sOrderBy += (if (sOrderBy.isEmpty()) "" else " , ") + " SHOP_catalog_2.name ASC "
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

        val rs = conn.executeQuery(sSQL)
        var countNN = 1
        var sumNum = 0.0
        var sumCostOut = 0.0
        while (rs.next()) {
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

            if (reportDocument == 0) {
                val sb = StringBuilder()
                if (reportDocumentType == DocumentTypeConfig.TYPE_ALL)
                    sb.append(hmAliasConfig[DocumentTypeConfig.hmDocTypeAlias[rowDocType]]!!.descr).append('\n')
                sb.append(DateTime_DMY(arrayOf(docYe, docMo, docDa, 0, 0, 0))).append('\n')
                if (isRowUseSourWarehouse)
                    sb.append(if (reportDocumentType == DocumentTypeConfig.TYPE_RESORT) "Склад / магазин: " else "Со склада / магазина: ")
                        .append(hmWarehouseName[rs.getInt(5)]).append('\n')
                if (isRowUseDestWarehouse) sb.append("На склад / магазин: ").append(hmWarehouseName[rs.getInt(6)]).append('\n')
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
            if (isUseSourCatalog) {
                if (isRowUseSourCatalog) priceOut = PriceData.getPrice(hmPrice, rs.getInt(10), zoneId, docYe, docMo, docDa)

                sheet.addCell(Label(offsX++, offsY, if (isRowUseSourCatalog) rs.getString(11) else "", wcfCellL))
                sheet.addCell(Label(offsX++, offsY, if (isRowUseSourCatalog) getSplittedDouble(priceOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) else "", wcfCellR))
            }
            if (isUseSourNum) {
                if (isRowUseSourNum) num = rs.getDouble(12)
                sheet.addCell(Label(offsX++, offsY, if (isRowUseSourNum) getSplittedDouble(num, -1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) else "", wcfCellC))
            }
            if (isUseDestCatalog) {
                if (isRowUseDestCatalog) priceOut = PriceData.getPrice(hmPrice, rs.getInt(13), zoneId, docYe, docMo, docDa)

                sheet.addCell(Label(offsX++, offsY, if (isRowUseDestCatalog) rs.getString(14) else "", wcfCellL))
                sheet.addCell(Label(offsX++, offsY, if (isRowUseDestCatalog) getSplittedDouble(priceOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) else "", wcfCellR))
            }
            if (isUseDestNum) {
                if (isRowUseDestNum) num = rs.getDouble(15)
                sheet.addCell(
                    Label(
                        offsX++,
                        offsY,
                        if (isRowUseDestNum) getSplittedDouble(num, -1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) else "",
                        wcfCellC
                    )
                )
            }
            sheet.addCell(Label(offsX++, offsY, getSplittedDouble(priceOut * num, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfCellR))

            offsY++

            sumNum += num
            sumCostOut += priceOut * num
        }
        rs.close()

        offsY++

        //--- при выводе состава всех типов накладных сумма бессмысленна
        var discountedCost = 0.0
        if (reportDocumentType != DocumentTypeConfig.TYPE_ALL) {
            sheet.addCell(Label(1, offsY, "ИТОГО:", wcfTextRB))
            sheet.addCell(Label(alCaption.size - 2, offsY, getSplittedDouble(sumNum, -1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfTextCB))
            sheet.addCell(Label(alCaption.size - 1, offsY, getSplittedDouble(sumCostOut, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfTextRB))
            offsY++
            if (reportDocument != 0 && reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
                sheet.addCell(Label(1, offsY, "Скидка:", wcfTextRB))

                sheet.addCell(
                    Label(
                        alCaption.size - 2,
                        offsY,
                        getSplittedDouble(discount, 1, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider) + " %",
                        wcfTextCB
                    )
                )
                sheet.addCell(
                    Label(
                        alCaption.size - 1,
                        offsY,
                        getSplittedDouble(ceil(sumCostOut * discount / 100), 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                        wcfTextRB
                    )
                )
                offsY++

                //--- пригодится для вывода текстовой суммы
                discountedCost = floor(sumCostOut * (1 - discount / 100))

                sheet.addCell(Label(1, offsY, "Стоимость со скидкой:", wcfTextRB))
                sheet.addCell(
                    Label(
                        alCaption.size - 1,
                        offsY,
                        getSplittedDouble(discountedCost, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider),
                        wcfTextRB
                    )
                )
                offsY++
            }
            offsY++
        }

        //--- для накладной на реализацию своя подпись отчёта
        if (reportDocument != 0 && reportDocumentType == DocumentTypeConfig.TYPE_OUT) {
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

    private fun printFiscal(docId: Int) {
        val shopApplication = application as iShopApplication

        val fiscalIndex = shopApplication.fiscalIndex?.toIntOrNull() ?: 0

        val fiscalUrl = shopApplication.fiscalUrls[fiscalIndex]
        val fiscalLineCutters = shopApplication.fiscalLineCutters[fiscalIndex]
        val fiscalCashier = shopApplication.fiscalCashiers[fiscalIndex]
        val fiscalTaxMode = shopApplication.fiscalTaxModes[fiscalIndex]
        val fiscalPlace = shopApplication.fiscalPlace ?: "Магазин"

        val fiscalOnceOnly = shopApplication.fiscalOnceOnly?.toBoolean() ?: false

        val fiscal: iFiscal = when (fiscalIndex) {
            0 -> Atol()
            else -> throw Exception("Wrong index for fiscal = $fiscalIndex")
        }

        var docYe = 0
        var docMo = 0
        var docDa = 0
        var discount = 0.0

        var rs = conn.executeQuery(" SELECT doc_ye , doc_mo , doc_da , discount FROM SHOP_doc WHERE id = $docId ")
        if (rs.next()) {
            docYe = rs.getInt(1)
            docMo = rs.getInt(2)
            docDa = rs.getInt(3)
            discount = rs.getDouble(4)
        }
        rs.close()

        //--- составим мегазапрос на вывод состава накладной
        val sSQL = " SELECT SHOP_catalog.id , SHOP_catalog.name , SHOP_doc_content.sour_num , SHOP_doc_content.mark_code  " +
            " FROM SHOP_doc_content , SHOP_catalog " +
            " WHERE SHOP_doc_content.doc_id = $docId " +
            " AND SHOP_doc_content.is_deleted = 0 " +
            " AND SHOP_doc_content.sour_id = SHOP_catalog.id "

        rs = conn.executeQuery(sSQL)
        while (rs.next()) {
            val price = PriceData.getPrice(hmPrice, rs.getInt(1), zoneId, docYe, docMo, docDa) * (1 - discount / 100)
            val name = rs.getString(2).trim().apply {
                substring(0, min(length, fiscalLineCutters[fiscalIndex].toInt()))
            }
            val count = rs.getDouble(3)
            val markCode = rs.getString(4)

            fiscal.addLine(
                name = name,
                price = price,
                count = count,
                markingCode = markCode.ifBlank {
                    null
                }
            )
        }
        rs.close()

        fiscal.sendFiscal(
            objectMapper = objectMapper,
            httpClient = httpClient,
            fiscalUrl = fiscalUrl,
            fiscalCashier = fiscalCashier,
            docId = docId.toString(),
            fiscalTaxMode = fiscalTaxMode,
            fiscalPlace = fiscalPlace,
        )
        if (fiscalOnceOnly) {
            conn.executeUpdate(" UPDATE SHOP_doc SET is_fiscaled = 1 WHERE id = $docId ")
        }
    }

}


package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.link.XyDocumentConfig
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cAbstractReport
import foatto.core_server.app.server.data.DataInt
import foatto.shop.PriceData
import foatto.shop.mPrice
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import jxl.CellView
import jxl.format.*
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableSheet
import java.util.concurrent.ConcurrentHashMap

class cPriceTag : cAbstractReport() {

    private val TAG_HEIGHT = 9

    private lateinit var hmPrice: Map<Int, List<Pair<Int, Double>>>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aHmParam: Map<String, String>, aHmAliasConfig: Map<String, AliasConfig>, aAliasConfig: AliasConfig, aHmXyDocumentConfig: Map<String, XyDocumentConfig>, aUserConfig: UserConfig) {
        super.init(aApplication, aConn, aStm, aChmSession, aHmParam, aHmAliasConfig, aAliasConfig, aHmXyDocumentConfig, aUserConfig)

        hmPrice = PriceData.loadPrice(stm, mPrice.PRICE_TYPE_OUT)
    }

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        if (returnURL != null) {
            return returnURL
        }

        val mpt = model as mPriceTag

        //--- выборка данных параметров для отчета
        for (i in 0 until mPriceTag.ROWS) {
            for (j in 0 until mPriceTag.COLS) {
                val paramName = "report_catalog_$i$j"
                hmReportParam[paramName] = (hmColumnData[mpt.getColumnCatalog(i, j)] as DataInt).intValue
            }
        }
        return getReport()
    }

    override fun setPrintOptions() {
        printPaperSize = PaperSize.A4
        printPageOrientation = PageOrientation.PORTRAIT

        printMarginLeft = 10
        printMarginRight = 10
        printMarginTop = 10
        printMarginBottom = 10

        printKeyX = 0.0
        printKeyY = 0.0
        printKeyW = 0.0
        printKeyH = 0.0
    }

    override fun postReport(sheet: WritableSheet) {

//        val priceTagHeader = ShopSpringApp.arrDocTitle[ 0 ]

        val wcfTitle = WritableCellFormat(WritableFont(WritableFont.ARIAL, 8, WritableFont.NO_BOLD, true, UnderlineStyle.NO_UNDERLINE, Colour.BLACK))
        wcfTitle.alignment = Alignment.CENTRE
        wcfTitle.verticalAlignment = VerticalAlignment.CENTRE
        wcfTitle.wrap = true
        //wcfTitle.setBorder(  Border.NONE, BorderLineStyle.NONE  );
        wcfTitle.setBorder(Border.ALL, BorderLineStyle.THIN)
        wcfTitle.setBackground(Colour.VERY_LIGHT_YELLOW)

        val wcfName = WritableCellFormat(WritableFont(WritableFont.ARIAL, 14, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK))
        wcfName.alignment = Alignment.CENTRE
        wcfName.verticalAlignment = VerticalAlignment.CENTRE
        wcfName.wrap = true
        wcfName.setBorder(Border.ALL, BorderLineStyle.THIN)

        val wcfPrice = WritableCellFormat(WritableFont(WritableFont.ARIAL, 28, WritableFont.BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK))
        wcfPrice.alignment = Alignment.CENTRE
        wcfPrice.verticalAlignment = VerticalAlignment.CENTRE
        wcfPrice.wrap = true
        wcfPrice.setBorder(Border.ALL, BorderLineStyle.THIN)

        val wcfDim = WritableCellFormat(WritableFont(WritableFont.ARIAL, 8, WritableFont.NO_BOLD, false, UnderlineStyle.NO_UNDERLINE, Colour.BLACK))
        wcfDim.alignment = Alignment.CENTRE
        wcfDim.verticalAlignment = VerticalAlignment.CENTRE
        wcfDim.wrap = true
        wcfDim.setBorder(Border.NONE, BorderLineStyle.NONE)

        //        WritableCellFormat wcfSignL = new WritableCellFormat(  new WritableFont( 
        //                                                            WritableFont.ARIAL, 8, WritableFont.NO_BOLD, false,
        //                                                            UnderlineStyle.NO_UNDERLINE, Colour.BLACK  )  );
        //        wcfSignL.setAlignment(  Alignment.LEFT  );
        //        wcfSignL.setVerticalAlignment(  VerticalAlignment.CENTRE  );
        //        wcfSignL.setWrap(  true  );
        //        wcfSignL.setBorder(  Border.RIGHT, BorderLineStyle.THIN  );

        //        WritableCellFormat wcfSignR = new WritableCellFormat(  new WritableFont( 
        //                                                            WritableFont.ARIAL, 8, WritableFont.NO_BOLD, false,
        //                                                            UnderlineStyle.NO_UNDERLINE, Colour.BLACK  )  );
        //        wcfSignR.setAlignment(  Alignment.RIGHT  );
        //        wcfSignR.setVerticalAlignment(  VerticalAlignment.CENTRE  );
        //        wcfSignR.setWrap(  true  );
        //        wcfSignR.setBorder(  Border.RIGHT, BorderLineStyle.THIN  );

        for (i in 0 until mPriceTag.COLS) {
            val cvNN = CellView()
            cvNN.size = 96 / mPriceTag.COLS * 256
            sheet.setColumnView(i, cvNN)
        }

        //--- загрузка стартовых параметров
        for (i in 0 until mPriceTag.ROWS) {
            for (j in 0 until mPriceTag.COLS) {
                val paramName = "report_catalog_$i$j"
                val catalogID = hmReportParam[paramName] as Int
                var catalogName = ""
                var catalogPrice = 0.0

                if (catalogID != 0) {
                    val rs = stm.executeQuery(" SELECT name FROM SHOP_catalog WHERE id = $catalogID ")
                    if (rs.next()) {
                        catalogName = rs.getString(1)

                        //--- убираем из названия цену - последнее слово в скобках
                        val pos = catalogName.lastIndexOf('(')
                        if (pos != -1) {
                            catalogName = catalogName.substring(0, pos).trim()
                        }
                    }
                    rs.close()
                    //--- могут быть пустые ячейки ценников, устанавливаем здесь, когда ясно, что catalogID != 0
                    catalogPrice = PriceData.getPrice(hmPrice, catalogID, getCurrentTimeInt())
                }

//                sheet.addCell( Label( j, i * TAG_HEIGHT, priceTagHeader, wcfTitle ) )

                sheet.addCell(Label(j, i * TAG_HEIGHT, catalogName, wcfName))
                sheet.mergeCells(j, i * TAG_HEIGHT, j, i * TAG_HEIGHT + 5)

                sheet.addCell(Label(j, i * TAG_HEIGHT + 6, getSplittedDouble(catalogPrice, 2, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider), wcfPrice))
                sheet.mergeCells(j, i * TAG_HEIGHT + 6, j, i * TAG_HEIGHT + 8)

//                sheet.addCell( Label( j, i * TAG_HEIGHT + 13, "( 1 шт. )", wcfDim ) )

                //                sheet.addCell(  new Label(  j, i * TAG_HEIGHT + 13, "Подпись ответственного лица:", wcfSignL  )  );
                //                sheet.addCell(  new Label(  j, i * TAG_HEIGHT + 14, new StringBuilder(
                //                    StringFunction.DateTime_DMY(  new GregorianCalendar()  )  )
                //                    .append(  " _________________________"  ).toString(), wcfSignR  )  );
            }
        }
    }
}

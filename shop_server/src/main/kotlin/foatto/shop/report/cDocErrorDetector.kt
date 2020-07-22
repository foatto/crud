package foatto.shop.report

import foatto.core.link.FormData
import foatto.core.util.DateTime_DMY
import foatto.core_server.app.server.cAbstractReport
import foatto.shop.DocumentTypeConfig
import jxl.CellView
import jxl.format.PageOrientation
import jxl.format.PaperSize
import jxl.write.Label
import jxl.write.WritableSheet
import java.util.*

class cDocErrorDetector : cAbstractReport() {

    override fun isFormAutoClick() = true

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        val returnURL = super.doSave(action, alFormData, hmOut)
        return returnURL ?: getReport()

        //        mSHOPReport m = (mSHOPReport) model;
        //
        //        //--- выборка данных параметров для отчета
        //        hmReportParam.put( "report_period", ( (DataInt) hmColumnData.get( m.getColumnPeriod() ) ).getStaticValue() );

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

    override fun postReport( sheet: WritableSheet ) {

        val alDER = calcReport()

        try {
            defineFormats(8, 2, 0)
        }
        catch(aThrowable: Throwable) {
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
        alCaption.add("Описание ошибки")
        alDim.add(85)

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

        var countNN = 1
        for(errorDescr in alDER) {
            offsX = 0

            sheet.addCell(Label(offsX++, offsY, (countNN++).toString(), wcfNN))
            sheet.addCell(Label(offsX++, offsY, errorDescr, wcfCellL))

            offsY++
        }
        offsY++

        sheet.addCell( Label( 1, offsY, getPreparedAt(), wcfCellL ) )
        //sheet.mergeCells( 1, offsY, 3, offsY );
    }

    private fun calcReport(): List<String> {
        //        int reportPeriod = (Integer) hmReportParam.get( "report_period" );

        val alDER = mutableListOf<String>()

        val alDD = DocumentData.loadDocumentData( stm )

        //--- ищем пустые накладные и накладные с дублирующимися строками внутри себя
        for(dd in alDD) {
            val alDCD = dd.alDCD

            if(alDCD.isEmpty()) {
                alDER.add(
                    StringBuilder("Пустая накладная № ")
                        .append(dd.docNo).append(" от ")
                        .append(DateTime_DMY(dd.arrDT)).toString()
                )
                continue
            }

            val isUseSour = DocumentTypeConfig.hsUseSourCatalog.contains(dd.type)
            val isUseDest = DocumentTypeConfig.hsUseDestCatalog.contains(dd.type)

            for(i in 0 until alDCD.size - 1) {
                val dcd0 = alDCD[i]
                for(j in i + 1 until alDCD.size) {
                    val dcd1 = alDCD[j]

                    if(isUseSour && dcd0.sourID == dcd1.sourID || isUseDest && dcd0.destID == dcd1.destID)

                        alDER.add(
                            StringBuilder("Совпадающие строки в накладной № ")
                                .append(dd.docNo).append(" от ")
                                .append(DateTime_DMY(dd.arrDT)).toString()
                        )
                }
            }
        }

        //--- ищем накладные, отличающиеся друг от друга не более чем на одну строку
        for(i in 0 until alDD.size - 1) {
            val dd0 = alDD[i]
            val alDCD0 = dd0.alDCD

            //--- пустые накладные сравнивать бессмысленно
            if(alDCD0.isEmpty()) continue

            for(j in i + 1 until alDD.size) {
                val dd1 = alDD[j]
                val alDCD1 = dd1.alDCD

                //--- пустые накладные сравнивать бессмысленно
                if(alDCD1.isEmpty()) continue

                //--- накладные разных типов не сравниваем и сразу пропускаем
                if(dd0.type != dd1.type) continue

                val isUseSourCatalog = DocumentTypeConfig.hsUseSourCatalog.contains(dd0.type)
                val isUseDestCatalog = DocumentTypeConfig.hsUseDestCatalog.contains(dd0.type)
                val isUseSourNum = DocumentTypeConfig.hsUseSourNum.contains(dd0.type)
                val isUseDestNum = DocumentTypeConfig.hsUseDestNum.contains(dd0.type)

                var diffCount = 0
                OUT_K@ for(k0 in alDCD0.indices)
                    for(k1 in alDCD1.indices)

                        if(isUseSourCatalog && alDCD0[k0].sourID != alDCD1[k1].sourID) {
                            diffCount++
                            break@OUT_K
                        }
                        else if(isUseDestCatalog && alDCD0[k0].destID != alDCD1[k1].destID) {
                            diffCount++
                            break@OUT_K
                        }
                        else if(isUseSourNum && alDCD0[k0].sourNum != alDCD1[k1].sourNum) {
                            diffCount++
                            break@OUT_K
                        }
                        else if(isUseDestNum && alDCD0[k0].destNum != alDCD1[k1].destNum) {
                            diffCount++
                            break@OUT_K
                        }

                if(diffCount == 0)
                    alDER.add(
                        StringBuilder("Совпадающие накладные № ")
                            .append(dd0.docNo).append(" от ").append(DateTime_DMY(dd0.arrDT))
                            .append(" и ")
                            .append(dd1.docNo).append(" от ").append(DateTime_DMY(dd1.arrDT))
                            .toString()
                    )
            }
        }

        return alDER
    }

}

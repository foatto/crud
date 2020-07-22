package foatto.core_server.app.server

import foatto.app.CoreSpringApp
import foatto.core.app.ICON_NAME_PRINT
import foatto.core.app.iCoreAppContainer
import foatto.core.link.FormData
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getFreeFile
import foatto.core.util.getRandomInt
import foatto.core_server.app.server.data.DataString
import jxl.Workbook
import jxl.format.*
import jxl.format.Alignment
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import jxl.format.VerticalAlignment
import jxl.write.*
import java.io.File
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList

abstract class cAbstractReport : cAbstractForm() {

    companion object {
        val REPORT_FILES_BASE = "reports"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- для передачи параметров отчёта (в общем случае взятых из hmColumnData) между doSave и getReport
    protected var hmReportParam = HashMap<String, Any>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var printPaperSize: PaperSize
    protected lateinit var printPageOrientation: PageOrientation
    protected var printMarginLeft = 0
    protected var printMarginRight = 0
    protected var printMarginTop = 0
    protected var printMarginBottom = 0

    protected var printKeyX = 0.0
    protected var printKeyY = 0.0
    protected var printKeyW = 0.0
    protected var printKeyH = 0.0

//--- cell format part ----------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var wcfTitleL: WritableCellFormat
    protected lateinit var wcfTitleC: WritableCellFormat
    protected lateinit var wcfTitleR: WritableCellFormat

    protected lateinit var wcfTitleName: WritableCellFormat
    protected lateinit var wcfTitleValue: WritableCellFormat

    protected lateinit var wcfCap: WritableCellFormat
    protected lateinit var wcfSignature: WritableCellFormat

    protected lateinit var wcfTextL: WritableCellFormat
    protected lateinit var wcfTextC: WritableCellFormat
    protected lateinit var wcfTextR: WritableCellFormat

    protected lateinit var wcfTextLB: WritableCellFormat
    protected lateinit var wcfTextCB: WritableCellFormat
    protected lateinit var wcfTextRB: WritableCellFormat

    protected lateinit var wcfCaptionHC: WritableCellFormat
    //    protected WritableCellFormat wcfCaptionHCB = null;
    //    protected WritableCellFormat wcfCaptionHT = null;
    protected lateinit var wcfCaptionVC: WritableCellFormat

    protected lateinit var wcfNN: WritableCellFormat

    protected lateinit var wcfCellL: WritableCellFormat
    protected lateinit var wcfCellC: WritableCellFormat
    protected lateinit var wcfCellR: WritableCellFormat

    protected lateinit var wcfCellLB: WritableCellFormat
    protected lateinit var wcfCellCB: WritableCellFormat
    protected lateinit var wcfCellRB: WritableCellFormat

    protected lateinit var wcfCellLStdYellow: WritableCellFormat
    protected lateinit var wcfCellCStdYellow: WritableCellFormat
    protected lateinit var wcfCellRStdYellow: WritableCellFormat

    protected lateinit var wcfCellLStdRed: WritableCellFormat
    protected lateinit var wcfCellCStdRed: WritableCellFormat
    protected lateinit var wcfCellRStdRed: WritableCellFormat

    protected lateinit var wcfCellLBStdYellow: WritableCellFormat
    protected lateinit var wcfCellCBStdYellow: WritableCellFormat
    protected lateinit var wcfCellRBStdYellow: WritableCellFormat

    protected lateinit var wcfCellLBStdRed: WritableCellFormat
    protected lateinit var wcfCellCBStdRed: WritableCellFormat
    protected lateinit var wcfCellRBStdRed: WritableCellFormat

    //    protected WritableCellFormat wcfCellLBI = null;
    //    protected WritableCellFormat wcfCellCBI = null;
    //    protected WritableCellFormat wcfCellRBI = null;

    //    protected WritableCellFormat wcfCellRSmall = null;
    protected lateinit var wcfCellRBSmall: WritableCellFormat

    protected lateinit var wcfCellCRedStd: WritableCellFormat
    protected lateinit var wcfCellRRedStd: WritableCellFormat

    protected lateinit var wcfCellCBRedStd: WritableCellFormat
    protected lateinit var wcfCellRBRedStd: WritableCellFormat

    protected lateinit var wcfCellCGrayStd: WritableCellFormat
    protected lateinit var wcfCellRGrayStd: WritableCellFormat
    protected lateinit var wcfCellRBGrayStd: WritableCellFormat

    //    protected WritableCellFormat wcfCellCRedBack = null;

//-----------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun getBegEndTimeFromParam(): Pair<Int,Int> {
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int
        val reportBegHour = hmReportParam["report_beg_hour"] as Int
        val reportBegMinute = hmReportParam["report_beg_minute"] as Int

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int
        val reportEndHour = hmReportParam["report_end_hour"] as Int
        val reportEndMinute = hmReportParam["report_end_minute"] as Int

        val zdtBeg = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, reportBegHour, reportBegMinute, 0, 0, zoneId)
        val zdtEnd = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, reportEndHour, reportEndMinute, 0, 0, zoneId)
        val begTime = zdtBeg.toEpochSecond().toInt()
        val endTime = zdtEnd.toEpochSecond().toInt()

        return Pair(begTime, endTime)
    }

    protected fun getBegEndDayFromParam(): Pair<ZonedDateTime,ZonedDateTime> {
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        val zdtBeg = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0, 0, zoneId)
        val zdtEnd = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0, 0, zoneId)

        return Pair(zdtBeg, zdtEnd)
    }

    protected fun getBegNextDayFromParam(): Pair<Int,Int> {
        val reportBegYear = hmReportParam["report_beg_year"] as Int
        val reportBegMonth = hmReportParam["report_beg_month"] as Int
        val reportBegDay = hmReportParam["report_beg_day"] as Int

        val reportEndYear = hmReportParam["report_end_year"] as Int
        val reportEndMonth = hmReportParam["report_end_month"] as Int
        val reportEndDay = hmReportParam["report_end_day"] as Int

        val zdtBeg = ZonedDateTime.of(reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0, 0, zoneId)
        val zdtEnd = ZonedDateTime.of(reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0, 0, zoneId).plus(1, ChronoUnit.DAYS)
        val begTime = zdtBeg.toEpochSecond().toInt()
        val endTime = zdtEnd.toEpochSecond().toInt()

        return Pair(begTime, endTime)
    }

    protected fun getPreparedAt() = "Подготовлено: ${DateTime_DMYHMS(ZonedDateTime.now(zoneId))}"

//--- report part ---------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun getReport(): String {

        val newFileName = getFreeFile( "${CoreSpringApp.rootDirName}/$REPORT_FILES_BASE", arrayOf( "xls" ) )
        val fullExcelName = "${CoreSpringApp.rootDirName}/$REPORT_FILES_BASE/$newFileName.xls"

        val fileExcel = File( fullExcelName )
        val workbook = Workbook.createWorkbook( fileExcel )
        val sheet = workbook.createSheet( " ", 0 )

        setPrintOptions()
        val ss = sheet.settings
        ss.paperSize = printPaperSize
        ss.orientation = printPageOrientation
        ss.leftMargin = printMarginLeft / 25.4
        ss.rightMargin = printMarginRight / 25.4
        ss.topMargin = printMarginTop / 25.4
        ss.bottomMargin = printMarginBottom / 25.4
//--- можно задать шаблон
//            ss.setHorizontalCentre( true );
//            ss.setVerticalCentre( true );
//        //ss.setPrintHeaders( true ); - и без этого хорошо
//        ss.setHeaderMargin( outMarginTop / 25.4 / 2 );
//        HeaderFooter hf = ss.fillHeader();
//        hf.getLeft().appendDate();
//        hf.getLeft().append( ' ' );
//        hf.getLeft().appendTime();
//        hf.getCentre().append( outHeader );
//        hf.getRight().append( "" + ( v + 1 ) + " - " + ( h + 1 ) );

        //--- пост-обработка отчета в классах-наследниках
        postReport( sheet )

        //--- если есть картинка-водяной знак, тогда имеет смысл защищать отчет
        val printKeyImage = File( "${CoreSpringApp.rootDirName}/logo.png" )
        if( printKeyW > 0 && printKeyH > 0 && printKeyImage.exists() ) {
            ss.password = getRandomInt().toString()
            ss.isProtected = true
            sheet.addImage( WritableImage( printKeyX, printKeyY, printKeyW, printKeyH, printKeyImage ) )
        }

        workbook.write()
        workbook.close()
        fileExcel.deleteOnExit()

        //--- (пока) одному важному пользователю не понравилось автозакрытие формы запуска отчёта после его генерации
        val isDisableReportAutoclose = userConfig.getUserProperty( iCoreAppContainer.UP_DISABLE_REPORT_AUTOCLOSE )?.toBoolean() ?: false

        val startChar = if( isDisableReportAutoclose ) "" else "#"

        //--- вернуть имя отчета (с отметкой автозакрытия закладки с формой отчёта)
        return "$startChar/$REPORT_FILES_BASE/$newFileName.xls"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getOkButtonIconName(): String = ICON_NAME_PRINT

    override fun doSave(action: String, alFormData: List<FormData>, hmOut: MutableMap<String, Any>): String? {

        //--- ошибки ввода в форме
        val returnURL = super.doSave(action, alFormData, hmOut)

        //--- если нет ошибок, то сохраним параметры подписей отчётов
        if( returnURL == null ) {
            val mar = model as mAbstractReport
            //--- графики запускаются как отчёты, но у них нет заголовков/подписей
            if( mar.columnReportCap != null ) {
                val reportCapValue = ( hmColumnData[ mar.columnReportCap!! ] as DataString ).text
                userConfig.saveUserProperty( stm, mar.getReportCapPropertyName( aliasConfig ), reportCapValue )
                hmReportParam[ mAbstractReport.REPORT_CAP_FIELD_NAME ] = reportCapValue

                for( i in 0 until mAbstractReport.MAX_REPORT_SIGNATURE_ROWS )
                    for( j in 0 until mAbstractReport.MAX_REPORT_SIGNATURE_COLS ) {
                        val reportSignatureValue = ( hmColumnData[ mar.alColumnReportSignature[ i ][ j ] ] as DataString ).text
                        userConfig.saveUserProperty( stm, mar.getReportSignaturePropertyName( i, j, aliasConfig ), reportSignatureValue )
                        hmReportParam[ mar.getReportSignatureFieldName( i, j ) ] = reportSignatureValue
                    }
            }
        }
        return returnURL
    }

    //--- обязательный метод установки параметров печати/бумаги
    protected abstract fun setPrintOptions()

    protected abstract fun postReport( sheet: WritableSheet )

    protected fun defineFormats( fontSize: Int, titleFontSizeInc: Int, titleNVFontSizeInc: Int ) {

        wcfTitleL = getWCF( fontSize + titleFontSizeInc, true, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)
        wcfTitleC = getWCF( fontSize + titleFontSizeInc, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, false, false, Colour.BLACK)
        wcfTitleR = getWCF( fontSize + titleFontSizeInc, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)

        wcfTitleName = getWCF(fontSize + titleNVFontSizeInc, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)
        wcfTitleValue = getWCF(fontSize + titleNVFontSizeInc, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)

        wcfCap = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)
        wcfSignature = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)

        wcfTextL = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, true, Colour.BLACK)
        wcfTextC = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, false, true, Colour.BLACK)
        wcfTextR = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, false, true, Colour.BLACK)

        wcfTextLB = getWCF(fontSize, true, false, Alignment.LEFT, VerticalAlignment.CENTRE, false, true, Colour.BLACK)
        wcfTextCB = getWCF(fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, false, true, Colour.BLACK)
        wcfTextRB = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, false, true, Colour.BLACK)

        wcfCaptionHC = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCaptionHC.setBackground(Colour.VERY_LIGHT_YELLOW)
        //        wcfCaptionHCB = getWCF( fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK );
        //        wcfCaptionHT = getWCF( fontSize, false, false, Alignment.CENTRE, VerticalAlignment.TOP, true, true, Colour.BLACK );
        wcfCaptionVC = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCaptionVC.setBackground(Colour.VERY_LIGHT_YELLOW)
        wcfCaptionVC.orientation = Orientation.PLUS_90

        wcfNN = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, false, Colour.BLACK)

        wcfCellL = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellC = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellR = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)

        wcfCellLB = getWCF(fontSize, true, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellCB = getWCF(fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellRB = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)

        wcfCellLStdYellow = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellLStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)
        wcfCellCStdYellow = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellCStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)
        wcfCellRStdYellow = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellRStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)

        wcfCellLStdRed = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellLStdRed.setBackground(Colour.CORAL)
        wcfCellCStdRed = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellCStdRed.setBackground(Colour.CORAL)
        wcfCellRStdRed = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellRStdRed.setBackground(Colour.CORAL)

        wcfCellLBStdYellow = getWCF(fontSize, true, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellLBStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)
        wcfCellCBStdYellow = getWCF(fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellCBStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)
        wcfCellRBStdYellow = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellRBStdYellow.setBackground(Colour.VERY_LIGHT_YELLOW)

        wcfCellLBStdRed = getWCF(fontSize, true, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellLBStdRed.setBackground(Colour.CORAL)
        wcfCellCBStdRed = getWCF(fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellCBStdRed.setBackground(Colour.CORAL)
        wcfCellRBStdRed = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        wcfCellRBStdRed.setBackground(Colour.CORAL)

        //        wcfCellLBI = getWCF( fontSize, true, true, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK );
        //        wcfCellCBI = getWCF( fontSize, true, true, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.BLACK );
        //        wcfCellRBI = getWCF( fontSize, true, true, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK );

        //        wcfCellRSmall = getWCF( fontSize - 1, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true );
        wcfCellRBSmall = getWCF(fontSize - 1, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)

        wcfCellCRedStd = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.RED)
        wcfCellRRedStd = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.RED)

        wcfCellCBRedStd = getWCF(fontSize, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.RED)
        wcfCellRBRedStd = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.RED)

        wcfCellCGrayStd = getWCF(fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true, Colour.GRAY_50)
        wcfCellRGrayStd = getWCF(fontSize, false, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.GRAY_50)
        wcfCellRBGrayStd = getWCF(fontSize, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, true, true, Colour.GRAY_50)

        //        wcfCellCRedBack = getWCF( fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true );
        //        wcfCellCRedBack.setBackground( Colour.PINK );
    }

    protected fun getWCF( fontSize: Int, isBold: Boolean, isItalic: Boolean, hAlign: Alignment, vAlign: VerticalAlignment,
                          isBorder: Boolean, isWrap: Boolean, fontColor: Colour ): WritableCellFormat {
        val wcf = WritableCellFormat( WritableFont( WritableFont.ARIAL, fontSize, if( isBold ) WritableFont.BOLD else WritableFont.NO_BOLD,
                                                    isItalic, UnderlineStyle.NO_UNDERLINE, fontColor ) )
        wcf.alignment = hAlign
        wcf.verticalAlignment = vAlign
        wcf.setBorder( if( isBorder ) Border.ALL else Border.NONE, if( isBorder ) BorderLineStyle.THIN else BorderLineStyle.NONE )
        wcf.wrap = isWrap

        return wcf
    }

    //--- распределяет ширину по столбцам с динамической шириной
    protected fun defineRelWidth( alDim: MutableList<Int>, totalWidth: Int ) {
        var captionConstWidthSum = 0
        var captionRelWidthSum = 0
        for( w in alDim )
            if( w > 0 ) captionConstWidthSum += w
            else captionRelWidthSum += w
        //--- получаем минусовую ширину на одну относительную ед.ширины
        val captionRelWidth = ( totalWidth - captionConstWidthSum ) / captionRelWidthSum
        //--- устанавливаем полученные остатки ширины (минус на минус как раз даёт плюс)
        for( i in alDim.indices ) if( alDim[ i ] < 0 ) alDim[ i ] = alDim[ i ] * captionRelWidth
    }

    protected fun outReportCap( sheet: WritableSheet, aCapX: Int, aCapY: Int ): Int {
        var capY = aCapY
        val reportCap = hmReportParam[ mAbstractReport.REPORT_CAP_FIELD_NAME ] as? String
        if( reportCap != null ) {
            val st = StringTokenizer( reportCap, "\n" )
            while( st.hasMoreTokens() )
                sheet.addCell( Label( aCapX, capY++, st.nextToken(), wcfCap ) )
        }
        return capY
    }

    protected fun outReportSignature( sheet: WritableSheet, arrSignatureX: IntArray, aSignatureY: Int ) {
        var signatureY = aSignatureY
        val mar = model as mAbstractReport
        for( i in 0 until mAbstractReport.MAX_REPORT_SIGNATURE_ROWS ) {
            var maxSignatureHeight = 0
            for( j in 0 until mAbstractReport.MAX_REPORT_SIGNATURE_COLS ) {
                val signature = hmReportParam[ mar.getReportSignatureFieldName( i, j ) ] as? String
                if( signature != null ) {
                    val st = StringTokenizer( signature, "\n" )
                    val alSignature = ArrayList<String>()
                    while( st.hasMoreTokens() ) alSignature.add( st.nextToken() )
                    for( k in 0 until alSignature.size )
                        sheet.addCell( Label( arrSignatureX[ j ], signatureY + k, alSignature[ k ], wcfSignature ) )
                    maxSignatureHeight = Math.max( maxSignatureHeight, alSignature.size )
                }
            }
            signatureY += maxSignatureHeight + 1
        }
    }
}

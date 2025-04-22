package foatto.server.service

import foatto.core.ActionType
import foatto.core.IconName
import foatto.core.model.AppAction
import foatto.core.model.request.FormActionData
import foatto.core.model.response.FormActionResponse
import foatto.core.model.response.ResponseCode
import foatto.core.model.response.form.FormButton
import foatto.core.model.response.form.FormButtonKey
import foatto.core.model.response.table.TableRowData
import foatto.core.model.response.table.cell.TableBaseCell
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeDMYHMSString
import foatto.core.util.getRandomInt
import foatto.core.util.getSplittedDouble
import foatto.server.checkFormAddPermission
import foatto.server.checkReportUnlockPermission
import foatto.server.model.AppModuleConfig
import foatto.server.model.ServerUserConfig
import foatto.server.util.getFreeFile
import jxl.Workbook
import jxl.format.*
import jxl.write.Label
import jxl.write.NumberFormat
import jxl.write.WritableCell
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.write.WritableImage
import jxl.write.WritableSheet
import java.io.File

abstract class ReportService(
    private val fileStoreService: FileStoreService,
) : ApplicationService(
    fileStoreService = fileStoreService,
) {

    companion object {
        const val REPORT_FILES_BASE: String = "reports"
    }

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

    protected lateinit var wcfComment: WritableCellFormat

    //    protected WritableCellFormat wcfCellCRedBack = null;

//--- report part ---------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun formActionSave(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, formActionData: Map<String, FormActionData>): FormActionResponse {

        val newFileName = getFreeFile("$rootDirName/$REPORT_FILES_BASE", arrayOf("xls"))
        val fullExcelName = "$rootDirName/$REPORT_FILES_BASE/$newFileName.xls"

        val fileExcel = File(fullExcelName)
        val workbook = Workbook.createWorkbook(fileExcel)
        val sheet = workbook.createSheet(" ", 0)

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
        getReport(userConfig, moduleConfig, formActionData, sheet)

        //--- если есть картинка-водяной знак, тогда имеет смысл защищать отчет
        val isReportUnlock = checkReportUnlockPermission(action.module, userConfig.roles)

        if (!isReportUnlock) {
            val printKeyImage = File("$rootDirName/logo.png")
            if (printKeyW > 0 && printKeyH > 0 && printKeyImage.exists()) {
                ss.password = getRandomInt().toString()
                ss.isProtected = true
                sheet.addImage(WritableImage(printKeyX, printKeyY, printKeyW, printKeyH, printKeyImage))
            }
        }

        workbook.write()
        workbook.close()
        fileExcel.deleteOnExit()

        return FormActionResponse(
            responseCode = ResponseCode.OK,
            nextAction = AppAction(
                type = ActionType.FILE,
                isAutoClose = true,
                url = "/$REPORT_FILES_BASE/$newFileName.xls",
            ),
        )
    }

    //--- обязательный метод установки параметров печати/бумаги
    protected abstract fun setPrintOptions()

    protected abstract fun getReport(
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        formActionData: Map<String, FormActionData>,
        sheet: WritableSheet,
    )

    protected fun defineFormats(fontSize: Int, titleFontSizeInc: Int, titleNVFontSizeInc: Int) {

        wcfTitleL = getWCF(
            fontSize = fontSize + titleFontSizeInc,
            isBold = true,
            isItalic = false,
            hAlign = Alignment.LEFT,
            vAlign = VerticalAlignment.CENTRE,
            isBorder = false,
            isWrap = false,
            fontColor = Colour.BLACK
        )
        wcfTitleC = getWCF(fontSize + titleFontSizeInc, true, false, Alignment.CENTRE, VerticalAlignment.CENTRE, false, false, Colour.BLACK)
        wcfTitleR = getWCF(fontSize + titleFontSizeInc, true, false, Alignment.RIGHT, VerticalAlignment.CENTRE, false, false, Colour.BLACK)

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

        wcfCellL = getWCF(
            fontSize = fontSize,
            isBold = false,
            isItalic = false,
            hAlign = Alignment.LEFT,
            vAlign = VerticalAlignment.CENTRE,
            isBorder = true,
            isWrap = true,
            fontColor = Colour.BLACK
        )
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

        wcfComment = getWCF(fontSize, false, false, Alignment.LEFT, VerticalAlignment.CENTRE, true, true, Colour.BLACK)
        //        wcfCellCRedBack = getWCF( fontSize, false, false, Alignment.CENTRE, VerticalAlignment.CENTRE, true, true );
        //        wcfCellCRedBack.setBackground( Colour.PINK );
    }

    protected fun getWCF(
        fontSize: Int,
        isBold: Boolean,
        isItalic: Boolean,
        hAlign: Alignment,
        vAlign: VerticalAlignment,
        isBorder: Boolean,
        isWrap: Boolean,
        fontColor: Colour
    ): WritableCellFormat {
        val wcf = WritableCellFormat(
            WritableFont(
                WritableFont.ARIAL,
                fontSize,
                if (isBold) {
                    WritableFont.BOLD
                } else {
                    WritableFont.NO_BOLD
                },
                isItalic,
                UnderlineStyle.NO_UNDERLINE,
                fontColor
            )
        )
        wcf.alignment = hAlign
        wcf.verticalAlignment = vAlign
        wcf.setBorder(if (isBorder) Border.ALL else Border.NONE, if (isBorder) BorderLineStyle.THIN else BorderLineStyle.NONE)
        wcf.wrap = isWrap

        return wcf
    }

    //--- распределяет ширину по столбцам с динамической шириной
    protected fun defineRelWidth(alDim: MutableList<Int>, totalWidth: Int) {
        var captionConstWidthSum = 0
        var captionRelWidthSum = 0
        for (w in alDim) {
            if (w > 0) {
                captionConstWidthSum += w
            } else {
                captionRelWidthSum += w
            }
        }
        //--- получаем минусовую ширину на одну относительную ед.ширины
        val captionRelWidth = (totalWidth - captionConstWidthSum) / captionRelWidthSum
        //--- устанавливаем полученные остатки ширины (минус на минус как раз даёт плюс)
        for (i in alDim.indices) {
            if (alDim[i] < 0) {
                alDim[i] = alDim[i] * captionRelWidth
            }
        }
    }

    //--- число в различном формате: в виде нередактируемой строки или в виде редактируемого числа
    protected fun getNumberCell(col: Int, row: Int, value: Number, precision: Int, wcf: WritableCellFormat, reportUnlocked: Boolean = false): WritableCell {
        return if (reportUnlocked) {
            val wcfNum = WritableCellFormat(
                WritableFont(
                    WritableFont.ARIAL,
                    wcf.font.pointSize,
                    if (wcf.font.boldWeight == 0x2bc) {
                        WritableFont.BOLD
                    } else {
                        WritableFont.NO_BOLD
                    },
                    wcf.font.isItalic,
                    UnderlineStyle.NO_UNDERLINE,
                    wcf.font.colour,
                ),
                NumberFormat(
                    if (precision > 0) {
                        "0." + "#".repeat(precision)
                    } else {
                        "0"
                    }
                ),
            )
            wcfNum.alignment = wcf.alignment
            wcfNum.verticalAlignment = wcf.verticalAlignment
            wcfNum.setBorder(if (wcf.hasBorders()) Border.ALL else Border.NONE, if (wcf.hasBorders()) BorderLineStyle.THIN else BorderLineStyle.NONE)
            wcfNum.wrap = wcf.wrap
            wcfNum.setBackground(wcf.backgroundColour)
            wcfNum.orientation = wcf.orientation

            jxl.write.Number(col, row, value.toDouble(), wcfNum)
        } else {
            Label(col, row, getSplittedDouble(value.toDouble(), precision/* !!!, userConfig.upIsUseThousandsDivider, userConfig.upDecimalDivider*/), wcf)
        }
    }

    protected fun getPreparedAt(userConfig: ServerUserConfig) = "Подготовлено: ${getDateTimeDMYHMSString(userConfig.timeOffset, getCurrentTimeInt())}"

//--- common app part --------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun getTableColumnCaptions(action: AppAction, userConfig: ServerUserConfig): List<Pair<AppAction?, String>> = emptyList()
    override fun fillTableGridData(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig, alTableCell: MutableList<TableBaseCell>, alTableRowData: MutableList<TableRowData>, alPageButton: MutableList<Pair<AppAction?, String>>): Int? = null

    override fun getFormButtons(
        action: AppAction,
        userConfig: ServerUserConfig,
        moduleConfig: AppModuleConfig,
        addEnabled: Boolean,
        editEnabled: Boolean,
        deleteEnabled: Boolean,
    ): List<FormButton> {

        val alFormButton = mutableListOf<FormButton>()

        if (action.id == null && addEnabled) {
            alFormButton += FormButton(
                actionType = ActionType.FORM_ADD,
                withNewData = true,
                name = IconName.PRINT_FORM,
                key = FormButtonKey.SAVE,
            )
        }

        return alFormButton
    }

    override fun getFormActionPermissions(action: AppAction, userConfig: ServerUserConfig, moduleConfig: AppModuleConfig): Triple<Boolean, Boolean, Boolean> {
        val addEnabled = checkFormAddPermission(action.module, userConfig.roles)

        return Triple(addEnabled, false, false)
    }

    override fun formActionDelete(userId: Int, id: Int): FormActionResponse = FormActionResponse(ResponseCode.ERROR)

}
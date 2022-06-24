package foatto.office.report

import foatto.core.util.DateTime_DMY
import foatto.core.util.DateTime_DMYHMS
import foatto.core_server.app.server.cAbstractReport
import jxl.write.Label
import jxl.write.WritableCellFormat
import jxl.write.WritableSheet

abstract class cOfficeReport : cAbstractReport() {
    companion object {

        //--- стандартные ширины столбцов
        // NNN                  = "N п/п"               = 5
        // dd.mm.yyyy hh:mm:ss  = "начало/окончание"    = 16
        // dd.mm.yyyy hh:mm     = "начало/окончание"    = 14
        // hhhh:mm:ss           = "длитель-ность"       = 9
        // A999AA116RUS         = "объект/скважина     >= 20 (нельзя уменьшить менее 20?)
        // A999AA116RUS         = "датчик/оборуд."     <= 20 (можно уменьшить до 15?)
        // 9999.9               = "время работы"        = 7
        // dd.mm.yyyy           = "дата"                = 9
        // АИ-95 (осн.)(изм.)   = "наим. жидкости"      = 15
        // 9999.9               = расход жидкости       = 7

        fun fillReportTitle(
            reportTitle: String,
            reportBegYear: Int, reportBegMonth: Int, reportBegDay: Int,
            reportEndYear: Int, reportEndMonth: Int, reportEndDay: Int,
            sheet: WritableSheet,
            wcfTitle: WritableCellFormat,
            offsX: Int, aOffsY: Int
        ): Int {
            var offsY = aOffsY

            sheet.addCell(Label(offsX, offsY++, reportTitle, wcfTitle))
            sheet.addCell(Label(
                    offsX, offsY++,
                    "за период с ${DateTime_DMY( arrayOf( reportBegYear, reportBegMonth, reportBegDay, 0, 0, 0 ) )}" +
                    " по ${DateTime_DMY( arrayOf( reportEndYear, reportEndMonth, reportEndDay, 0, 0, 0 ) )}",
                    wcfTitle
                )
            )
            return offsY
        }
    }

    //    //--- универсальное заполнение заголовка отчета
    //    public static int fillReportHeader( Connection conn, int reportDepartment, int reportGroup,
    //                                        WritableSheet sheet, WritableCellFormat wcfTitleName, WritableCellFormat wcfTitleValue,
    //                                        int offsX, int offsY ) throws Throwable {
    //        Statement stm = DBFunction.createStatement( conn );
    //        if( reportDepartment != 0 ) {
    //            String name = "(неизвестно)";
    //            CoreAdvancedResultSet rs = stm.executeQuery( new StringBuilder( " SELECT name FROM MMS_department WHERE id = " )
    //                                                       .append( reportDepartment ).toString() );
    //            if( rs.next() ) name = rs.getString( 1 );
    //            rs.close();
    //
    //            sheet.addCell( new jxl.write.Label( offsX, offsY, "Подразделение:", wcfTitleName ) );
    //            sheet.addCell( new jxl.write.Label( offsX + 1, offsY, name, wcfTitleValue ) );
    //            offsY++;
    //        }
    //        if( reportGroup != 0 ) {
    //            String name = "(неизвестно)";
    //            CoreAdvancedResultSet rs = stm.executeQuery( new StringBuilder( " SELECT name FROM MMS_group WHERE id = " )
    //                                                       .append( reportGroup ).toString() );
    //            if( rs.next() ) name = rs.getString( 1 );
    //            rs.close();
    //
    //            sheet.addCell( new jxl.write.Label( offsX, offsY, "Группа:", wcfTitleName ) );
    //            sheet.addCell( new jxl.write.Label( offsX + 1, offsY, name, wcfTitleValue ) );
    //            offsY++;
    //        }
    //        stm.close();
    //
    //        return offsY;
    //    }

}

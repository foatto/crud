package foatto.shop.report

import foatto.core.util.getSplittedDouble
import foatto.core.util.getSplittedLong
import jxl.write.Label
import jxl.write.WritableSheet

abstract class cAbstractOperationState : cAbstractCatalogReport() {

    //--- рекурсивный вывод
    protected fun outItem(sheet: WritableSheet, aOffsY: Int, ci: CalcItem, whDest: Int ): Int {
        var offsY = aOffsY
        //--- пропускаем строки с полностью нулевыми количествами
        var isExist = false
        for( wName in tmWarehouseID.keys ) {
            val count = ci.tmWHCount[ wName ]
            if( count != null && count != 0.0 ) {
                isExist = true
                break
            }
        }
        if( !isExist ) return offsY

        //--- пишем номер строки только для товара
        if( !ci.ii.isFolder ) sheet.addCell( Label( 0, offsY, (countNN++).toString(), wcfNN ) )

        val appendStr = if( ci.ii.isFolder && ( ci.parentName == null || ci.parentName == ROOT_ITEM_NAME ) ) "\n" else ""
        sheet.addCell( Label( 1, offsY, "$appendStr${ci.ii.name}$appendStr", if( ci.ii.isFolder ) wcfCellLBStdYellow else wcfCellL ) )

        //--- кол-во наименований пишется только для папки
        if( ci.ii.isFolder ) sheet.addCell( Label( 2, offsY, getSplittedLong( ci.subItemCount.toLong() ).toString(), wcfCellCBStdYellow ) )

        //--- далее применяем счётчик позиций из-за переменного кол-ва заголовков
        var offsX = 3

        var whSum = 0.0
        var priceSum = 0.0
        for( ( wName, wID ) in tmWarehouseID ) {
            //--- если задан конкретный склад, то пропускаем "не наши" склады
            if( whDest != 0 && whDest != wID ) continue

            val count = ci.tmWHCount[ wName ] ?: 0.0
            whSum += count
            sheet.addCell( Label( offsX++, offsY, getSplittedDouble( count, -1 ).toString(), if( ci.ii.isFolder ) if( count < 0 ) wcfCellCBStdRed else wcfCellCBStdYellow
                                                                                             else if( count < 0 ) wcfCellCBStdRed else wcfCellC ) )
            val price = ci.tmWHPrice[ wName ]
            priceSum += price ?: 0.0
        }
        if( whDest == 0 )
            sheet.addCell( Label( offsX++, offsY, getSplittedDouble( whSum, -1 ).toString(), if( ci.ii.isFolder ) if( whSum < 0 ) wcfCellCBStdRed else wcfCellCBStdYellow
        else if( whSum < 0 ) wcfCellCBStdRed else wcfCellC ) )
        sheet.addCell( Label( offsX++, offsY, getSplittedDouble( priceSum, 2 ).toString(), if( ci.ii.isFolder ) if( priceSum < 0 ) wcfCellRBStdRed else wcfCellRBStdYellow
                                                                                           else if( priceSum < 0 ) wcfCellRBStdRed else wcfCellR ) )
        offsY++

        if( ci.tmSubItem != null ) {
            //--- сначала выводим элементы из своей папки, только потом подпапки
            for( name in ci.tmSubItem!!.keys ) {
                val ciSub = ci.tmSubItem!![ name ]!!
                if( ciSub.tmSubItem == null ) offsY = outItem( sheet, offsY, ciSub, whDest )
            }
            for( name in ci.tmSubItem!!.keys ) {
                val ciSub = ci.tmSubItem!![ name ]!!
                if( ciSub.tmSubItem != null ) offsY = outItem( sheet, offsY, ciSub, whDest )
            }
        }
        return offsY
    }
}

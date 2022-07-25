package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.DateTime_DMY
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate

abstract class DataAbstractDate(aColumn: iColumn) : DataAbstract(aColumn) {

    protected val NULL_DATE = LocalDate.of(1970, 1, 1)

    var localDate: LocalDate = NULL_DATE
        protected set

    protected var arrErrorValue: Array<String>? = null

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        val sDa = formData.alDateTimeValue!![0]
        val sMo = formData.alDateTimeValue!![1]
        val sYe = formData.alDateTimeValue!![2]

        //--- сначала проверка на правильность ввода цифр как таковых (для полей простой даты, где цифры вводятся вручную)
        try {
            localDate = LocalDate.of(sYe.toInt(), sMo.toInt(), sDa.toInt())
            arrErrorValue = null
            errorText = null
            return true
        } catch (t: Throwable) {
            arrErrorValue = arrayOf(sYe, sMo, sDa)
            errorText = "Ошибка ввода даты"
            return false
        }
    }

    override fun getTableCell(
        rootDirName: String,
        conn: CoreAdvancedConnection,
        row: Int,
        col: Int,
        dataRowNo: Int,
        isUseThousandsDivider: Boolean,
        decimalDivider: Char
    ): TableCell =
        if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan, dataRowNo)
        }
        else {
            TableCell(
                aRow = row,
                aCol = col,
                aRowSpan = column.rowSpan,
                aColSpan = column.colSpan,
                aDataRow = dataRowNo,

                aAlign = column.tableAlign,
                aMinWidth = column.minWidth,
                aIsWordWrap = column.isWordWrap,
                aTooltip = column.caption,

                aText = DateTime_DMY(localDate)
            )
        }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val fci = FormCell(FormCellType.DATE)

        //--- данные ye/mo/da для пользователя выводим как da/mo/ye
        fci.alDateTimeField = fci.alDateTimeField.toMutableList().apply {
            add(Pair(getFieldCellName(2), if (errorText == null) localDate.dayOfMonth.toString() else arrErrorValue!![2]))
            add(Pair(getFieldCellName(1), if (errorText == null) localDate.monthValue.toString() else arrErrorValue!![1]))
            add(Pair(getFieldCellName(0), if (errorText == null) localDate.year.toString() else arrErrorValue!![0]))
        }.toTypedArray()
        return fci
    }

    override fun setData(data: iData) {
        val dd = data as DataAbstractDate
        localDate = LocalDate.from(dd.localDate)
        arrErrorValue = null
        errorText = null
    }

}

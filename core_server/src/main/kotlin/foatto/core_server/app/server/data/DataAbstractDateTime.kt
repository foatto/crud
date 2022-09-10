package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.DateTime_DMYHM
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getDateTime
import foatto.core_server.app.server.column.ColumnAbstractDateTime
import foatto.core_server.app.server.column.ColumnDateTimeInt
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import java.time.ZonedDateTime

abstract class DataAbstractDateTime(aColumn: iColumn) : DataAbstract(aColumn) {

    protected val zoneId = (aColumn as ColumnAbstractDateTime).zoneId
    protected val NULL_DATE_TIME = getDateTime(zoneId, 0)

    var zonedDateTime = NULL_DATE_TIME
        protected set

    protected var arrErrorValue: Array<String>? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDefault() {
        val cdt = column as ColumnAbstractDateTime

        zonedDateTime = ZonedDateTime.from(cdt.default)
        arrErrorValue = null
        errorText = null
    }

    override fun loadFromForm(conn: CoreAdvancedConnection, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        val cdt = column as ColumnAbstractDateTime

        val sDa = formData.alDateTimeValue!![0]
        val sMo = formData.alDateTimeValue!![1]
        val sYe = formData.alDateTimeValue!![2]
        val sHo = formData.alDateTimeValue!![3]
        val sMi = formData.alDateTimeValue!![4]
        val sSe = if (cdt.withSecond) formData.alDateTimeValue!![5] else "0"

        //--- сначала проверка на правильность ввода цифр как таковых (для полей простой даты, где цифры вводятся вручную)
        try {
            zonedDateTime = ZonedDateTime.of(sYe.toInt(), sMo.toInt(), sDa.toInt(), sHo.toInt(), sMi.toInt(), sSe.toInt(), 0, zoneId)
            arrErrorValue = null
            errorText = null
        } catch (t: Throwable) {
            arrErrorValue = if (cdt.withSecond) arrayOf(sYe, sMo, sDa, sHo, sMi, sSe) else arrayOf(sYe, sMo, sDa, sHo, sMi)
            errorText = "Ошибка ввода даты/времени"
            return false
        }
        return true
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, dataRowNo: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        val cdt = column as ColumnAbstractDateTime

        return if (isShowEmptyTableCell) {
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

                aText = if (cdt.withSecond) DateTime_DMYHMS(zonedDateTime) else DateTime_DMYHM(zonedDateTime)
            )
        }
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val cdt = column as ColumnDateTimeInt

        val fci = FormCell(FormCellType.DATE_TIME)

        fci.withSecond = cdt.withSecond

        fci.alDateTimeField = fci.alDateTimeField.toMutableList().apply {
            add(Pair(getFieldCellName(2), if (errorText == null) zonedDateTime.dayOfMonth.toString() else arrErrorValue!![2]))
            add(Pair(getFieldCellName(1), if (errorText == null) zonedDateTime.monthValue.toString() else arrErrorValue!![1]))
            add(Pair(getFieldCellName(0), if (errorText == null) zonedDateTime.year.toString() else arrErrorValue!![0]))
            add(Pair(getFieldCellName(3), if (errorText == null) zonedDateTime.hour.toString() else arrErrorValue!![3]))
            add(
                Pair(
                    getFieldCellName(4), if (errorText == null) (if (zonedDateTime.minute < 10) "0" else "") + zonedDateTime.minute.toString()
                    else arrErrorValue!![4]
                )
            )
            if (cdt.withSecond)
                add(
                    Pair(
                        getFieldCellName(5), if (errorText == null) (if (zonedDateTime.second < 10) "0" else "") + zonedDateTime.second.toString()
                        else arrErrorValue!![5]
                    )
                )
        }.toTypedArray()
        return fci
    }

    override fun setData(data: iData) {
        val ddt = data as DataAbstractDateTime
        zonedDateTime = ZonedDateTime.from(ddt.zonedDateTime)
        arrErrorValue = null
        errorText = null
    }

    override fun getFieldCellName(index: Int): String = "${getFieldName(0)}_$index".replace(".", "___")

}
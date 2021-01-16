package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.Time_HM
import foatto.core.util.Time_HMS
import foatto.core_server.app.server.column.ColumnTimeDT
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import java.time.LocalTime

class DataTimeDT(aColumn: iColumn) : DataAbstractTime(aColumn) {

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        localTime = rs.getTime(posRS++) ?: NULL_TIME
        return posRS
    }

    override fun loadFromDefault() {
        val cd = column as ColumnTimeDT

        localTime = LocalTime.from(cd.default)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val ct = column as ColumnTimeDT

        val sHo = formData.alDateTimeValue!![0]
        val sMi = formData.alDateTimeValue!![1]
        val sSe = if (ct.withSecond) formData.alDateTimeValue!![2] else "0"

        try {
            localTime = LocalTime.of(sHo.toInt(), sMi.toInt(), sSe.toInt())
            arrErrorValue = null
            errorText = null
            return true
        } catch (t: Throwable) {
            arrErrorValue = if (ct.withSecond) arrayOf(sHo, sMi, sSe) else arrayOf(sHo, sMi)
            errorText = "Ошибка ввода времени"
            return false
        }
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell {
        val ct = column as ColumnTimeDT

        return if (isShowEmptyTableCell) TableCell(row, col, column.rowSpan, column.colSpan)
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = column.caption,

            aText = if (ct.withSecond) Time_HMS(localTime) else Time_HM(localTime)
        )
    }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement): FormCell {
        val ct = column as ColumnTimeDT

        val fci = FormCell(FormCellType.TIME)

        fci.withSecond = ct.withSecond
        fci.alDateTimeField = getDateTimeField(fci.alDateTimeField, ct.withSecond)

        return fci
    }

    override fun getFieldCellName(index: Int): String = "${getFieldName(0)}_$index".replace(".", "___")

    override fun getFieldSQLValue(index: Int): String {
        return "'${localTime.hour}:${localTime.minute}:${localTime.second}'"
    }
}
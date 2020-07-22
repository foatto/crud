package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.Time_HM
import foatto.core.util.Time_HMS
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import java.time.LocalTime

class DataTime3Int(aColumn: iColumn) : DataAbstractTime(aColumn) {

    override val fieldSQLCount: Int
        get() = column.alFieldName.size

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        val ct = column as ColumnTime3Int

        var posRS = aPosRS
        localTime = LocalTime.of(rs.getInt(posRS++), rs.getInt(posRS++), if(ct.withSecond) rs.getInt(posRS++) else 0)
        return posRS
    }

    override fun loadFromDefault() {
        val ct = column as ColumnTime3Int

        localTime = LocalTime.from(ct.default)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val ct = column as ColumnTime3Int

        val sHo = formData.alDateTimeValue!![0]
        val sMi = formData.alDateTimeValue!![1]
        val sSe = if(ct.withSecond) formData.alDateTimeValue!![2] else "0"

        try {
            localTime = LocalTime.of(sHo.toInt(), sMi.toInt(), sSe.toInt())
            arrErrorValue = null
            errorText = null
            return true
        } catch(t: Throwable) {
            arrErrorValue = if(ct.withSecond) arrayOf(sHo, sMi, sSe) else arrayOf(sHo, sMi)
            errorText = "Ошибка ввода времени"
            return false
        }
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell {
        val ct = column as ColumnTime3Int

        return if(isShowEmptyTableCell) TableCell(row, col, column.rowSpan, column.colSpan)
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = column.caption,

            aText = if(ct.withSecond) Time_HMS(localTime) else Time_HM(localTime)
        )
    }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement): FormCell {
        val ct = column as ColumnTime3Int

        val fci = FormCell(FormCellType.TIME)

        fci.withSecond = ct.withSecond

        fci.alDateTimeField.add(Pair(getFieldCellName(0), if(errorText == null) localTime.hour.toString() else arrErrorValue!![0]))
        fci.alDateTimeField.add(Pair(getFieldCellName(1), if(errorText == null) (if(localTime.minute < 10) "0" else "") + localTime.minute else arrErrorValue!![1]))
        if(ct.withSecond)
            fci.alDateTimeField.add(Pair(getFieldCellName(2), if(errorText == null) (if(localTime.second < 10) "0" else "") + localTime.second else arrErrorValue!![2]))

        return fci
    }

    override fun getFieldSQLValue(index: Int): String {
        return when(index) {
            0 -> "${localTime.hour}"
            1 -> "${localTime.minute}"
            2 -> "${localTime.second}"
            else -> throw Throwable("DataTime3Int.getFieldSQLValue: wrong index = $index")
        }
    }
}

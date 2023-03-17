package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.Time_HM
import foatto.core.util.Time_HMS
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import java.time.LocalTime

class DataTime3Int(aColumn: iColumn) : DataAbstractTime(aColumn) {

    override val fieldSQLCount: Int
        get() = column.alFieldName.size

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        val ct = column as ColumnTime3Int

        var posRS = aPosRS

        localTime = LocalTime.of(rs.getInt(posRS++), rs.getInt(posRS++), if (ct.withSecond) rs.getInt(posRS++) else 0)
        arrErrorValue = null
        errorText = null

        return posRS
    }

    override fun loadFromDefault() {
        val ct = column as ColumnTime3Int

        localTime = LocalTime.from(ct.default)
        arrErrorValue = null
        errorText = null
    }

    override fun loadFromForm(conn: CoreAdvancedConnection, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        val ct = column as ColumnTime3Int

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

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, dataRowNo: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        val ct = column as ColumnTime3Int

        return if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan, dataRowNo)
        } else {
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

                aText = if (ct.withSecond) {
                    Time_HMS(localTime)
                } else {
                    Time_HM(localTime)
                }
            )
        }
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val ct = column as ColumnTime3Int

        val fci = FormCell(FormCellType.TIME)

        fci.withSecond = ct.withSecond
        fci.alDateTimeField = getDateTimeField(fci.alDateTimeField, ct.withSecond)

        return fci
    }

    override fun getFieldSQLValue(index: Int): String {
        return when (index) {
            0 -> "${localTime.hour}"
            1 -> "${localTime.minute}"
            2 -> "${localTime.second}"
            else -> throw Throwable("DataTime3Int.getFieldSQLValue: wrong index = $index")
        }
    }
}

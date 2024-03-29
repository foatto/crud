package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.prepareForSQL
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import kotlin.math.min

class DataString(aColumn: iColumn) : DataAbstract(aColumn) {

    private val cs = column as ColumnString

    var text: String = ""
        set(value) {
            field = validate(value)
            clearError()
        }

    private var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        text = validate(rs.getString(posRS++))
        clearError()

        return posRS
    }

    override fun loadFromDefault() {
        text = validate(cs.defaultValue)
        clearError()
    }

    override fun loadFromForm(conn: CoreAdvancedConnection, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        text = if (cs.rows == 0) {
            formData.stringValue!!
        } else {
            formData.textValue!!
        }

        if (cs.isUseTrim) {
            text = text.trim()
        }
        text = text.substring(0, min(text.length, cs.maxSize))
        clearError()

        if (cs.isRequired && text.isEmpty()) {
            setError(text, "Обязательно для заполнения")
            return false
        }
        return true
    }

    override fun getUniqueCheckValue(index: Int): Any = text

    override fun setUniqueCheckingError(message: String) {
        setError(text, message)
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, dataRowNo: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell {
        if (isShowEmptyTableCell) {
            return TableCell(row, col, column.rowSpan, column.colSpan, dataRowNo)
        }

        return TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aDataRow = dataRowNo,

            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = column.caption,

            aText = if (cs.isPassword) {
                "********"
            } else if (text.isEmpty()) {
                cs.emptyValueString
            } else {
                text
            }
        )
    }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell {
        val fci: FormCell
        if (cs.rows == 0) {
            fci = FormCell(FormCellType.STRING)
            fci.name = getFieldCellName(0)
            fci.value = if (errorText == null) text else errorValue!!
            fci.column = cs.cols
            fci.isPassword = cs.isPassword
            fci.alComboString = cs.alCombo
        } else {
            fci = FormCell(FormCellType.TEXT)
            fci.textName = getFieldCellName(0)
            fci.textValue = text
            fci.textRow = cs.rows
            fci.textColumn = cs.cols
        }
        return fci
    }

    override fun getFieldSQLValue(index: Int): String = " '${prepareForSQL(text)}' "

    override fun setData(data: iData) {
        text = (data as DataString).text
        clearError()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun setError(aErrorValue: String?, aErrorText: String?) {
        errorValue = aErrorValue
        errorText = aErrorText
    }

    fun clearError() {
        setError(null, null)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun validate(obj: Any?): String {
        var tmp = obj?.toString() ?: ""
        if (cs.isUseTrim) {
            tmp = tmp.trim()
        }
        return tmp
    }
}

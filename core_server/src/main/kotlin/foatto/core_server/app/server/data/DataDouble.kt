package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet

class DataDouble(aColumn: iColumn) : DataAbstract(aColumn) {

    private val cd = column as ColumnDouble

    var doubleValue = 0.0

    private var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS

        doubleValue = rs.getDouble(posRS++)
        clearError()

        return posRS
    }

    override fun loadFromDefault() {
        doubleValue = cd.defaultValue ?: 0.0
        clearError()
    }

    override fun loadFromForm(conn: CoreAdvancedConnection, formData: FormData, fieldNameId: String?, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if (cd.isRequired && strValue.isBlank()) {
            setError(strValue, "Обязательно для заполнения")
            return false
        }
        try {
            doubleValue = strValue.replace(',', '.').replace(" ", "").toDouble()
            clearError()
        } catch (t: Throwable) {
            doubleValue = 0.0
            setError(strValue, "Ошибка ввода")
            return false
        }

        //--- проверка на минимум
        if (cd.minValue != null && doubleValue < cd.minValue!!) {
            setError(strValue, "Значение должно быть не меньше, чем ${cd.minValue}")
            return false
        }
        //--- проверка на максимум
        if (cd.maxValue != null && doubleValue > cd.maxValue!!) {
            setError(strValue, "Значение должно быть не больше, чем ${cd.maxValue}")
            return false
        }

        return true
    }

    override fun getUniqueCheckValue(index: Int): Any = doubleValue

    override fun setUniqueCheckingError(message: String) {
        setError(doubleValue.toString(), message)
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, dataRowNo: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell =
        if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan, dataRowNo)
        }
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aDataRow = dataRowNo,

            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = column.caption,

            aText = if (cd.emptyValue != null && cd.emptyValue == doubleValue) {
                cd.emptyText!!
            } else {
                getSplittedDouble(doubleValue, cd.precision, isUseThousandsDivider, decimalDivider)
            }
        )

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char): FormCell =
        FormCell(FormCellType.DOUBLE).apply {
            name = getFieldCellName(0)
            value = if (errorText == null) {
                getSplittedDouble(doubleValue, cd.precision, isUseThousandsDivider, decimalDivider)
            } else {
                errorValue!!
            }
            column = cd.cols
            alComboString = cd.alCombo.toTypedArray()
        }

    override fun getFieldSQLValue(index: Int): String = "$doubleValue"

    override fun setData(data: iData) {
        doubleValue = (data as DataDouble).doubleValue
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
}

package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getSplittedDouble
import foatto.core_server.app.server.column.ColumnDouble
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement

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
        errorValue = null
        errorText = null

        return posRS
    }

    override fun loadFromDefault() {
        doubleValue = validate(cd.defaultValue)
        errorValue = null
        errorText = null
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if (cd.isRequired && strValue.isBlank()) {
            errorValue = strValue
            errorText = "Обязательно для заполнения"
            return false
        }
        try {
            doubleValue = strValue.replace(',', '.').replace(" ", "").toDouble()
            errorValue = null
            errorText = null
        } catch (t: Throwable) {
            doubleValue = 0.0
            errorValue = strValue
            errorText = "Ошибка ввода"
            return false
        }

        //--- проверка на минимум
        if (cd.minValue != null && doubleValue < cd.minValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не меньше, чем ${cd.minValue}"
            return false
        }
        //--- проверка на максимум
        if (cd.maxValue != null && doubleValue > cd.maxValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не больше, чем ${cd.maxValue}"
            return false
        }
        if (column.isUnique &&
            (column.uniqueIgnore == null || column.uniqueIgnore != doubleValue) &&
            stm.checkExist(column.tableName, column.alFieldName[0], doubleValue, fieldNameID, id)
        ) {
            errorValue = strValue
            errorText = "Это значение уже существует"
            return false
        }

        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell =
        if (isShowEmptyTableCell) TableCell(row, col, column.rowSpan, column.colSpan)
        else TableCell(
            aRow = row,
            aCol = col,
            aRowSpan = column.rowSpan,
            aColSpan = column.colSpan,
            aAlign = column.tableAlign,
            aMinWidth = column.minWidth,
            aIsWordWrap = column.isWordWrap,
            aTooltip = column.caption,

            aText = getReportString()
        )

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement): FormCell =
        FormCell(FormCellType.DOUBLE).apply {
            name = getFieldCellName(0)
            value = if (errorText == null) {
                getSplittedDouble(doubleValue, cd.precision)
            } else {
                errorValue!!
            }
            column = cd.cols
            alComboString = cd.alCombo.toTypedArray()
        }

    override fun getFieldSQLValue(index: Int): String = "$doubleValue"

    override fun setData(data: iData) {
        doubleValue = (data as DataDouble).doubleValue
        errorValue = null
        errorText = null
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getReportString(): String =
        if (cd.emptyValue != null && cd.emptyValue == doubleValue) {
            cd.emptyText!!
        }
        else {
            getSplittedDouble(doubleValue, cd.precision)
        }

    private fun validate(obj: Double?) = obj ?: 0.0
}

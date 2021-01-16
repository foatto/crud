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

    var value = 0.0

    private var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val fieldSQLCount: Int
        get() = 1

    override fun loadFromDB(rs: CoreAdvancedResultSet, aPosRS: Int): Int {
        var posRS = aPosRS
        value = rs.getDouble(posRS++)
        return posRS
    }

    override fun loadFromDefault() {
        value = validate(cd.defaultValue)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if (cd.isRequired && strValue.isBlank()) {
            errorValue = strValue
            errorText = "Обязательно для заполнения"
            return false
        }
        try {
            value = strValue.replace(',', '.').replace(" ", "").toDouble()
        } catch (t: Throwable) {
            value = 0.0
            errorValue = strValue
            errorText = "Ошибка ввода"
            return false
        }

        //--- проверка на минимум
        if (cd.minValue != null && value < cd.minValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не меньше, чем ${cd.minValue}"
            return false
        }
        //--- проверка на максимум
        if (cd.maxValue != null && value > cd.maxValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не больше, чем ${cd.maxValue}"
            return false
        }
        if (column.isUnique &&
            (column.uniqueIgnore == null || column.uniqueIgnore != value) &&
            stm.checkExist(column.tableName, column.alFieldName[0], value, fieldNameID, id)
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

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement): FormCell {
        val fci = FormCell(FormCellType.DOUBLE)
        fci.name = getFieldCellName(0)
        fci.value = if (errorText == null) getSplittedDouble(value, cd.precision) else errorValue!!
        fci.column = cd.cols
        fci.alComboString = cd.alCombo.toTypedArray()
        return fci
    }

    override fun getFieldSQLValue(index: Int): String = "$value"

    override fun setData(data: iData) {
        value = (data as DataDouble).value
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getReportString(): String {
        return if (cd.emptyValue != null && cd.emptyValue == value) cd.emptyText!!
        else getSplittedDouble(value, cd.precision)
    }

    private fun validate(obj: Double?) = obj ?: 0.0
}

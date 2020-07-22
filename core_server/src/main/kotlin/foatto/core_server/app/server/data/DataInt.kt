package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getSplittedLong
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedStatement

class DataInt(aColumn: iColumn) : DataAbstractValue(aColumn) {

    private val ci = column as ColumnInt

    private var errorValue: String? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromDefault() {
        value = validate((column as ColumnInt).defaultValue)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if(ci.isRequired && strValue.isBlank()) {
            errorValue = strValue
            errorText = "Обязательно для заполнения"
            return false
        }
        try {
            value = strValue.replace(" ", "").toInt(ci.radix)
        } catch(t: Throwable) {
            value = 0
            errorValue = strValue
            errorText = "Ошибка ввода"
            return false
        }

        //--- проверка на минимум
        if(ci.minValue != null && value < ci.minValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не меньше, чем ${ci.minValue}"
            return false
        }
        //--- проверка на максимум
        if(ci.maxValue != null && value > ci.maxValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не больше, чем ${ci.maxValue}"
            return false
        }

        if(column.isUnique &&
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
        if(isShowEmptyTableCell) TableCell(row, col, column.rowSpan, column.colSpan)
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
        val fci = FormCell(FormCellType.INT)
        fci.name = getFieldCellName(0)
        fci.value = if(errorText == null) getSplittedLong(value.toLong(), ci.radix).toString() else errorValue!!
        fci.column = ci.cols
        fci.alComboString.addAll(ci.alCombo)
        return fci
    }

    override fun setData(data: iData) {
        value = (data as DataInt).value
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getReportString(): String {
        return if(ci.emptyValue != null && ci.emptyValue == value) ci.emptyText!!
        else getSplittedLong(value.toLong(), ci.radix).toString()
    }
}

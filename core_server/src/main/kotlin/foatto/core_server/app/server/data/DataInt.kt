package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getSplittedLong
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedStatement

class DataInt(aColumn: iColumn) : DataAbstractIntValue(aColumn) {

    private val ci = column as ColumnInt

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromDefault() {
        intValue = validate((column as ColumnInt).defaultValue)
        errorValue = null
        errorText = null
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if (ci.isRequired && strValue.isBlank()) {
            errorValue = strValue
            errorText = "Обязательно для заполнения"
            return false
        }
        try {
            intValue = strValue.replace(" ", "").toInt(ci.radix)
            errorValue = null
            errorText = null
        } catch (t: Throwable) {
            intValue = 0
            errorValue = strValue
            errorText = "Ошибка ввода"
            return false
        }

        //--- проверка на минимум
        if (ci.minValue != null && intValue < ci.minValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не меньше, чем ${ci.minValue}"
            return false
        }
        //--- проверка на максимум
        if (ci.maxValue != null && intValue > ci.maxValue!!) {
            errorValue = strValue
            errorText = "Значение должно быть не больше, чем ${ci.maxValue}"
            return false
        }

        if (column.isUnique &&
            (column.uniqueIgnore == null || column.uniqueIgnore != intValue) &&
            stm.checkExist(column.tableName, column.alFieldName[0], intValue, fieldNameID, id)
        ) {
            errorValue = strValue
            errorText = "Это значение уже существует"
            return false
        }

        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell =
        if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan)
        } else {
            TableCell(
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
        }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement) =
        FormCell(FormCellType.INT).apply {
            name = getFieldCellName(0)
            value = if (errorText == null) {
                getSplittedLong(intValue.toLong(), ci.radix)
            } else {
                errorValue!!
            }
            column = ci.cols
            alComboString = ci.alCombo.toTypedArray()
        }

    override fun setData(data: iData) {
        intValue = (data as DataInt).intValue
        errorValue = null
        errorText = null
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun getReportString() =
        if (ci.emptyValue != null && ci.emptyValue == intValue) {
            ci.emptyText!!
        } else {
            getSplittedLong(intValue.toLong(), ci.radix)
        }

}
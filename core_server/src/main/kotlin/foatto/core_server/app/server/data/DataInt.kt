package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core.util.getSplittedLong
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement

class DataInt(aColumn: iColumn) : DataAbstractIntValue(aColumn) {

    private val ci = column as ColumnInt

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromDefault() {
        intValue = validate((column as ColumnInt).defaultValue)
        clearError()
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String?, id: Int): Boolean {
        val strValue = formData.stringValue!!

        if (ci.isRequired && strValue.isBlank()) {
            setError(strValue, "Обязательно для заполнения")
            return false
        }
        try {
            intValue = strValue.replace(" ", "").toInt(ci.radix)
            clearError()
        } catch (t: Throwable) {
            intValue = 0
            setError(strValue, "Ошибка ввода")
            return false
        }

        //--- проверка на минимум
        if (ci.minValue != null && intValue < ci.minValue!!) {
            setError(strValue, "Значение должно быть не меньше, чем ${ci.minValue}")
            return false
        }
        //--- проверка на максимум
        if (ci.maxValue != null && intValue > ci.maxValue!!) {
            setError(strValue, "Значение должно быть не больше, чем ${ci.maxValue}")
            return false
        }

        if (column.isUnique &&
            (column.uniqueIgnore == null || column.uniqueIgnore != intValue) &&
            stm.checkExist(column.tableName, column.alFieldName[0], intValue, fieldNameID, id)
        ) {
            setError(strValue, "Это значение уже существует")
            return false
        }

        return true
    }

    override fun getTableCell(rootDirName: String, conn: CoreAdvancedConnection, row: Int, col: Int, isUseThousandsDivider: Boolean, decimalDivider: Char): TableCell =
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

                aText =
                if (ci.emptyValue != null && ci.emptyValue == intValue) {
                    ci.emptyText!!
                } else {
                    if(isUseThousandsDivider) {
                        getSplittedLong(intValue.toLong(), ci.radix)
                    }
                    else {
                        intValue.toString(ci.radix)
                    }
                }
            )
        }

    override fun getFormCell(rootDirName: String, conn: CoreAdvancedConnection, isUseThousandsDivider: Boolean, decimalDivider: Char) =
        FormCell(FormCellType.INT).apply {
            name = getFieldCellName(0)
            value = if (errorText == null) {
                if(isUseThousandsDivider) {
                    getSplittedLong(intValue.toLong(), ci.radix)
                }
                else {
                    intValue.toString(ci.radix)
                }
            } else {
                errorValue!!
            }
            column = ci.cols
            alComboString = ci.alCombo.toTypedArray()
        }

    override fun setData(data: iData) {
        intValue = (data as DataInt).intValue
        clearError()
    }
}
package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.server.column.ColumnAbstractSelector
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedStatement

abstract class DataAbstractSelector(aColumn: iColumn, aFormCellType: FormCellType) : DataAbstractIntValue(aColumn) {

    private var formCellType = aFormCellType

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromDefault() {
        intValue = validate((column as ColumnAbstractSelector).defaultValue)
        errorValue = null
        errorText = null
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val cas = column as ColumnAbstractSelector
        cas.requiredExcept?.let {
            if (formData.comboValue!! == cas.requiredExcept) {
                errorValue = formData.comboValue!!.toString()
                errorText = "Обязательно для выбора"
                return false
            }
        }

        intValue = formData.comboValue!!
        errorValue = null
        errorText = null

        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell =
        if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan)
        } else {
            val cas = column as ColumnAbstractSelector
            val icon = cas.findChoiceIcon(intValue)
            val descr = cas.findChoiceTableDescr(intValue)
            TableCell(
                aRow = row,
                aCol = col,
                aRowSpan = column.rowSpan,
                aColSpan = column.colSpan,
                aAlign = column.tableAlign,
                aMinWidth = column.minWidth,
                aIsWordWrap = column.isWordWrap,
                aTooltip = if (icon.isBlank()) column.caption else descr,

                aIcon = icon,
                aText = descr
            )
        }

    override fun getFormCell(rootDirName: String, stm: CoreAdvancedStatement): FormCell {
        val cas = column as ColumnAbstractSelector

        return FormCell(formCellType).apply {
            comboName = getFieldCellName(0)
            comboValue = if (errorText == null) {
                intValue
            } else {
                errorValue!!.toInt()
            }
            alComboData = cas.alSelectorData.map { Pair(it.value, it.formDescr) }.toTypedArray()
        }
    }

    override fun setData(data: iData) {
        intValue = (data as DataAbstractSelector).intValue
        errorValue = null
        errorText = null
    }
}

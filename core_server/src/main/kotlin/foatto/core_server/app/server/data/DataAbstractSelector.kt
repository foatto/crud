package foatto.core_server.app.server.data

import foatto.core.link.FormCell
import foatto.core.link.FormCellType
import foatto.core.link.FormData
import foatto.core.link.TableCell
import foatto.core_server.app.server.column.ColumnAbstractSelector
import foatto.core_server.app.server.column.iColumn
import foatto.sql.CoreAdvancedStatement

abstract class DataAbstractSelector(aColumn: iColumn, aFormCellType: FormCellType) : DataAbstractValue(aColumn) {

    private var formCellType = aFormCellType

    private var errorValue: Int? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun loadFromDefault() {
        value = validate((column as ColumnAbstractSelector).defaultValue)
    }

    override fun loadFromForm(stm: CoreAdvancedStatement, formData: FormData, fieldNameID: String, id: Int): Boolean {
        val cas = column as ColumnAbstractSelector
        cas.requiredExcept?.let {
            if (formData.comboValue!! == cas.requiredExcept) {
                errorValue = formData.comboValue!!
                errorText = "Обязательно для выбора"
                return false
            }
        }

        value = formData.comboValue!!
        return true
    }

    override fun getTableCell(rootDirName: String, stm: CoreAdvancedStatement, row: Int, col: Int): TableCell =
        if (isShowEmptyTableCell) {
            TableCell(row, col, column.rowSpan, column.colSpan)
        } else {
            val cas = column as ColumnAbstractSelector
            val icon = cas.findChoiceIcon(value)
            val descr = cas.findChoiceTableDescr(value)
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

        val fci = FormCell(formCellType)
        fci.comboName = getFieldCellName(0)
        fci.comboValue = if (errorText == null) value else errorValue!!
        fci.alComboData = cas.alSelectorData.map { Pair(it.value, it.formDescr) }.toTypedArray()
        return fci
    }

    override fun setData(data: iData) {
        value = (data as DataAbstractSelector).value
    }
}

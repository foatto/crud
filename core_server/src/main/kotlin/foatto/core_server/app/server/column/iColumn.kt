package foatto.core_server.app.server.column

import foatto.core.link.FormPinMode
import foatto.core.link.TableCellAlign
import foatto.core_server.app.server.FormColumnCaptionData
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

interface iColumn {

    var columnTableName: String

    //--- больше трёх полей ещё не было
    val alFieldName: MutableList<String>

    val caption: String

    val rowSpan: Int
    val colSpan: Int

    val minWidth: Int

    //--- разрешается ли переносить содержимое текста по словам
    var isWordWrap: Boolean

    val linkColumn: iColumn?
    val selfLinkTableName: String?

    val selectorAlias: String?
    val alSelectTo: MutableList<iColumn>
    val alSelectFrom: MutableList<iColumn>
    val isAutoStartSelector: Boolean
    val isNotUseParentId: Boolean

    val isVirtual: Boolean
    val isSearchable: Boolean
    val isEditable: Boolean
    var isUnique: Boolean
    var uniqueIgnore: Any?

    val isSavedDefault: Boolean

    var tableCaption: String
    var tableAlign: TableCellAlign

    val formPinMode: FormPinMode

    val alFCVD: MutableList<FormColumnVisibleData>
    val alFCCD: MutableList<FormColumnCaptionData>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun isSortable(): Boolean
    fun getSortFieldName(index: Int): String

    fun addSelectorColumn(columnToAndFrom: iColumn)
    fun addSelectorColumn(columnTo: iColumn, columnFrom: iColumn)

    fun setUnique(aIsUnique: Boolean, aUniqueIgnore: Any? = null)

    fun setSavedDefault(userConfig: UserConfig)
    fun saveDefault(conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getData(): iData

//!!! для совместимости со старым Java-кодом, чтобы не менять в куче мест в старых рабочих исходниках ----------------------------------------------------------------------------------

    fun getFieldCount(): Int
    fun getFieldName(): String
    fun getFieldName(index: Int): String

    fun getSelectTo(): List<iColumn>
    fun getSelectFrom(): List<iColumn>

    fun getFormVisibleCount(): Int
    fun getFormVisible(index: Int): FormColumnVisibleData
    fun addFormVisible(columnMaster: iColumn, state: Boolean, values: Set<Int>)

    fun getFormCaptionCount(): Int
    fun getFormCaption(index: Int): FormColumnCaptionData
    fun addFormCaption(columnMaster: iColumn, caption: String, values: Set<Int>)
}

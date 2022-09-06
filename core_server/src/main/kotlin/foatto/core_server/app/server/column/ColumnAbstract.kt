package foatto.core_server.app.server.column

import foatto.core.link.FormPinMode
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.FormColumnCaptionData
import foatto.core_server.app.server.FormColumnVisibleData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.iData
import foatto.sql.CoreAdvancedConnection

abstract class ColumnAbstract : iColumn {

    override lateinit var columnTableName: String

    override val alFieldName = mutableListOf<String>()

    override lateinit var caption: String

    override var rowSpan: Int = 1
    override var colSpan: Int = 1

    override var minWidth: Int = 0

    override var isWordWrap = true

    override var linkColumn: iColumn? = null
    override var selfLinkTableName: String? = null    // реальное имя таблицы для самосвязанных таблиц

    override var selectorAlias: String? = null

    //--- в какие поля копировать
    override var alSelectTo = mutableListOf<iColumn>()

    //--- из каких полей копировать
    override var alSelectFrom = mutableListOf<iColumn>()

    //--- будет ли автоматически запускаться селектор у данного поля
    override var isAutoStartSelector: Boolean = false

    override var isNotUseParentId: Boolean = false

    //--- виртуальное поле
    override var isVirtual: Boolean = false

    //--- возможность/разрешение искать в этом поле
    override var isSearchable: Boolean = true

    //--- редактируемость данных
    override var isEditable: Boolean = true

    //--- поле с сохраняемым default-значением
    override var isSavedDefault: Boolean = false

    //--- отдельный caption для табличного режима
    override var tableCaption = ""
    override var tableAlign = TableCellAlign.LEFT

    override var formPinMode = FormPinMode.AUTO
    override var isAutoFocus = false

    override val alFCVD = mutableListOf<FormColumnVisibleData>()
    override val alFCCD = mutableListOf<FormColumnCaptionData>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun addSelectorColumn(columnToAndFrom: iColumn) {
        addSelectorColumn(columnToAndFrom, columnToAndFrom)
    }

    override fun addSelectorColumn(columnTo: iColumn, columnFrom: iColumn) {
        alSelectTo.add(columnTo)
        alSelectFrom.add(columnFrom)
    }

    override fun isSortable() = !isVirtual

    //--- основная часть полей не умеет сохранять своё default-значение, ибо незачем
    override fun setSavedDefault(userConfig: UserConfig) {}

    override fun saveDefault(application: iApplication, conn: CoreAdvancedConnection, userConfig: UserConfig, hmColumnData: Map<iColumn, iData>) {}

//--- перекрыто для нормальной работы HashMap.get при передаче описаний полей/столбцов между модулями ---------------------------------------------------------------------------------

    override fun hashCode(): Int {
        var h: Long = 0
        h += columnTableName.hashCode().toLong()
        for (fieldName in alFieldName) h += fieldName.hashCode().toLong()
        return h.toInt()
    }

    override fun equals(other: Any?): Boolean {
        if (super.equals(other)) return true  // if( this == other ) return true;
        if (other == null) return false
        if (other !is ColumnAbstract) return false

        if (columnTableName != other.columnTableName) return false
        if (alFieldName.size != other.alFieldName.size) return false
        for (i in 0 until alFieldName.size)
            if (alFieldName[i] != other.alFieldName[i]) return false

        return true
    }

//!!! для совместимости со старым Java-кодом, чтобы всё не менять потом ---------------------------------------------------------------------------------------------------------------

    override fun getFieldCount() = alFieldName.size
    override fun getFieldName() = alFieldName.first()
    override fun getFieldName(index: Int) = alFieldName[index]

    override fun getSelectTo(): List<iColumn> = alSelectTo
    override fun getSelectFrom(): List<iColumn> = alSelectFrom

    override fun getFormVisibleCount() = alFCVD.size
    override fun getFormVisible(index: Int) = alFCVD[index]
    override fun addFormVisible(columnMaster: iColumn, state: Boolean, values: Set<Int>) {
        alFCVD.add(FormColumnVisibleData(columnMaster, state, values))
    }

    override fun getFormCaptionCount() = alFCCD.size
    override fun getFormCaption(index: Int) = alFCCD[index]
    override fun addFormCaption(columnMaster: iColumn, caption: String, values: Set<Int>) {
        alFCCD.add(FormColumnCaptionData(columnMaster, caption, values))
    }

}

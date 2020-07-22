package foatto.core_server.app.server

import foatto.app.CoreSpringController
import foatto.core.link.AddActionButton
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedStatement

open class mAbstractHierarchy : mAbstract() {

    companion object {
        const val RECORD_TYPE_PARAM = "record_type"

        const val RECORD_TYPE_FOLDER = 0
        const val RECORD_TYPE_ITEM = 1
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var commonAliasName: String
        protected set
    lateinit var folderAliasName: String
        protected set
    lateinit var itemAliasName: String
        protected set

    protected lateinit var columnParentID: ColumnInt
    lateinit var columnParent: ColumnInt
        protected set

    lateinit var columnRecordType: ColumnComboBox
        protected set   //--- тип строки - папка или элемент списка
    lateinit var columnRecordFullName: ColumnString
        protected set
    lateinit var columnParentFullName: ColumnString
        protected set

    var isSelectableFolder = false
        protected set
    var isSelectableItem = false
        protected set

    val alAddButtomParam = mutableListOf<AddActionButton>()

    protected lateinit var selfLinkTableName: String

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isExpandable(): Boolean = true

    override fun init(
        appController: CoreSpringController,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int
    ) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        commonAliasName += (if(isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")
        folderAliasName += (if(isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")
        itemAliasName += (if(isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")

        isSelectableFolder = aliasConfig.alias == commonAliasName || aliasConfig.alias == folderAliasName
        isSelectableItem = aliasConfig.alias == commonAliasName || aliasConfig.alias == itemAliasName
        selfLinkTableName = "${tableName}_"

        columnID = ColumnInt(tableName, "id")

        columnParentID = ColumnInt(selfLinkTableName, "id")
        columnParentID.selfLinkTableName = tableName
        columnParent = ColumnInt(tableName, "parent_id", columnParentID)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)

        alFormHiddenColumn.add(columnID!!)
        alFormHiddenColumn.add(columnParent)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        hmParentColumn[commonAliasName] = columnParent
        hmParentColumn[folderAliasName] = columnParent
        hmParentColumn[itemAliasName] = columnParent

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData(aliasConfig.alias, columnID!!, aNewGroup = true, aDefaultOperation = true))

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData(tableName, columnParent.getFieldName()))

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        expandParentIDColumn = columnParent
    }

    protected fun getRecordType(id: Int, recordTypeFieldName: String, defaultRecordType: Int): Int {
        return if(id == 0) {
            hmParam[RECORD_TYPE_PARAM]?.toIntOrNull() ?: defaultRecordType
        } else {
            val rs = stm.executeQuery(" SELECT $recordTypeFieldName FROM $tableName WHERE id = $id ")
            val result = if(rs.next()) rs.getInt(1) else defaultRecordType
            rs.close()
            result
        }
    }
}


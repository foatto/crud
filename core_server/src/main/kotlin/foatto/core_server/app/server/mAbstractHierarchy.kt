package foatto.core_server.app.server

import foatto.core.link.AddActionButton
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.sql.CoreAdvancedConnection

open class mAbstractHierarchy : mAbstract() {

    companion object {
        const val RECORD_TYPE_PARAM = "record_type"

        const val RECORD_TYPE_FOLDER = 0
        const val RECORD_TYPE_ITEM = 1
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var commonAliasName: String
    lateinit var folderAliasName: String
    lateinit var itemAliasName: String

    protected lateinit var columnParentID: ColumnInt
    lateinit var columnParent: ColumnInt

    //--- тип строки - папка или элемент списка
    lateinit var columnRecordType: ColumnComboBox
    lateinit var columnRecordFullName: ColumnString
    lateinit var columnParentFullName: ColumnString

    var isSelectableFolder = false
    var isSelectableItem = false

    val alAddButtomParam = mutableListOf<AddActionButton>()

    protected lateinit var selfLinkParentTableName: String

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun isExpandable(): Boolean = true

    override fun init(
        application: iApplication,
        aConn: CoreAdvancedConnection,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aConn, aliasConfig, userConfig, aHmParam, hmParentData, id)

        commonAliasName += (if (isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")
        folderAliasName += (if (isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")
        itemAliasName += (if (isArchiveAlias) ALIAS_NAME_ARCHIVE_POSTFIX else "")

        isSelectableFolder = aliasConfig.name == commonAliasName || aliasConfig.name == folderAliasName
        isSelectableItem = aliasConfig.name == commonAliasName || aliasConfig.name == itemAliasName
        selfLinkParentTableName = "${modelTableName}__PARENT"

        columnId = ColumnInt(modelTableName, "id")

        columnParentID = ColumnInt(selfLinkParentTableName, "id").apply {
            selfLinkTableName = modelTableName
        }
        columnParent = ColumnInt(modelTableName, "parent_id", columnParentID)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnParent

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        hmParentColumn[commonAliasName] = columnParent
        hmParentColumn[folderAliasName] = columnParent
        hmParentColumn[itemAliasName] = columnParent

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alChildData.add(ChildData(aliasConfig.name, columnId, aNewGroup = true, aDefaultOperation = true))

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        alDependData.add(DependData(modelTableName, columnParent.getFieldName()))

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        expandParentIDColumn = columnParent
    }

    protected fun getRecordType(id: Int?, recordTypeFieldName: String, defaultRecordType: Int): Int {
        return if (id == null || id == 0) {
            hmParam[RECORD_TYPE_PARAM]?.toIntOrNull() ?: defaultRecordType
        } else {
            val rs = conn.executeQuery(" SELECT $recordTypeFieldName FROM $modelTableName WHERE id = $id ")
            val result = if (rs.next()) {
                rs.getInt(1)
            } else {
                defaultRecordType
            }
            rs.close()

            result
        }
    }
}


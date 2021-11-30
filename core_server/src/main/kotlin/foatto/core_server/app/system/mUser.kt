package foatto.core_server.app.system

import foatto.core.app.ICON_NAME_ADD_FOLDER
import foatto.core.app.ICON_NAME_ADD_ITEM
import foatto.core.app.ICON_NAME_BOSS
import foatto.core.app.ICON_NAME_DIVISION
import foatto.core.app.ICON_NAME_WORKER
import foatto.core.link.AddActionButton
import foatto.core.link.TableCellAlign
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstractHierarchy
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.ZonedDateTime

class mUser : mAbstractHierarchy() {

    companion object {
        val alExtendChildData = mutableListOf<ChildData>()
        val alExtendDependData = mutableListOf<DependData>()
    }
    //----------------------------------------------------------------------------------------------------------------------

    lateinit var columnDisabled: ColumnBoolean
        private set
    lateinit var columnUserPassword: ColumnString
        private set
    lateinit var columnUserLastLoginAttemptDate: ColumnDate3Int
        private set
    //----------------------------------------------------------------------------------------------------------------------

    init {
        commonAliasName = "system_user"
        folderAliasName = "system_user_division"
        itemAliasName = "system_user_people"

        modelTableName = "SYSTEM_users"

        alAddButtomParam.add(AddActionButton("Добавить подразделение", "Добавить подразделение", ICON_NAME_ADD_FOLDER, "$RECORD_TYPE_PARAM=${OrgType.ORG_TYPE_DIVISION}"))
        alAddButtomParam.add(AddActionButton("Добавить руководителя", "Добавить руководителя", ICON_NAME_ADD_ITEM, "$RECORD_TYPE_PARAM=${OrgType.ORG_TYPE_BOSS}"))
        alAddButtomParam.add(AddActionButton("Добавить работника", "Добавить работника", ICON_NAME_ADD_ITEM, "$RECORD_TYPE_PARAM=${OrgType.ORG_TYPE_WORKER}"))
    }

    //----------------------------------------------------------------------------------------------------------------------

    override fun init(
        application: iApplication,
        aStm: CoreAdvancedStatement,
        aliasConfig: AliasConfig,
        userConfig: UserConfig,
        aHmParam: Map<String, String>,
        hmParentData: MutableMap<String, Int>,
        id: Int?
    ) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- получить данные по правам доступа (при добавлении модуля в систему прав доступа к нему ещё нет)
        val isPeopleSelectable = userConfig.userPermission["system_user_people"]?.contains(cStandart.PERM_ACCESS) ?: false

        val selfLinkUserTableName = "${modelTableName}__USER"

        val columnUserID = ColumnInt(selfLinkUserTableName, "id").apply {
            selfLinkTableName = modelTableName
        }
        columnUser = ColumnInt(modelTableName, "user_id", columnUserID, userConfig.userId)
        val columnUserName = ColumnString(selfLinkUserTableName, "full_name", "Владелец", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = modelTableName
            if (isPeopleSelectable) {
                selectorAlias = "system_user_people"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        //----------------------------------------------------------------------------------------------------------------------

        val recordType = getRecordType(id, "org_type", OrgType.ORG_TYPE_WORKER)

        columnRecordType = ColumnComboBox(modelTableName, "org_type", "", recordType).apply {
            addChoice(OrgType.ORG_TYPE_DIVISION, "Подразделение", "Подразделение", ICON_NAME_DIVISION)
            addChoice(OrgType.ORG_TYPE_BOSS, "Руководитель", "Руководитель", ICON_NAME_BOSS)
            addChoice(OrgType.ORG_TYPE_WORKER, "Работник", "Работник", ICON_NAME_WORKER)
            tableAlign = TableCellAlign.CENTER
        }

        columnParentFullName = ColumnString(selfLinkParentTableName, "full_name", "Вышестоящее подразделение", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = modelTableName

            selectorAlias = folderAliasName
            addSelectorColumn(columnParent, columnParentID)
            addSelectorColumn(this)
        }

        columnRecordFullName = ColumnString(modelTableName, "full_name", "-", STRING_COLUMN_WIDTH).apply {
            isRequired = true
            addFormCaption(columnRecordType, "Наименование", setOf(OrgType.ORG_TYPE_DIVISION))
            addFormCaption(columnRecordType, "Полное имя", setOf(OrgType.ORG_TYPE_BOSS, OrgType.ORG_TYPE_WORKER))
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserShortName = ColumnString(modelTableName, "short_name", "Краткое имя", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        columnDisabled = ColumnBoolean(modelTableName, "is_disabled", "Отключен", false).apply {
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        val columnUserLogin = ColumnString(modelTableName, "login", "Логин", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
            setUnique(true, "")
        }

        columnUserPassword = ColumnString(modelTableName, "pwd", "Пароль", STRING_COLUMN_WIDTH).apply {
            isPassword = true
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        val columnUserLoginAttemptCount = ColumnInt(modelTableName, "at_count", "Счетчик попыток входа", 10).apply {
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        columnUserLastLoginAttemptDate = ColumnDate3Int(modelTableName, "at_ye", "at_mo", "at_da", "Дата последней попытки входа").apply {
            isEditable = false
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        val columnUserLastLoginAttemptTime = ColumnTime3Int(modelTableName, "at_ho", "at_mi", null, "Время последней попытки входа").apply {
            isEditable = false
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        val columnUserLastPasswordChangeDate = ColumnDate3Int(modelTableName, "pwd_ye", "pwd_mo", "pwd_da", "Дата последнего изменения пароля").apply {
            val toDay = ZonedDateTime.now(zoneId)
            default = LocalDate.of(toDay.year + 1, toDay.monthValue, toDay.dayOfMonth) // через год
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        val columnUserEmail = ColumnString(modelTableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }
        val columnUserContactInfo = ColumnString(modelTableName, "contact_info", "Контактная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
//        val columnFile = ColumnFile(tableName, "file_id", "Файлы")

        val columnUserLastIP = ColumnString(modelTableName, "last_ip", "Last IP", STRING_COLUMN_WIDTH).apply {
            isEditable = false
            addFormVisible(columnRecordType, false, setOf(OrgType.ORG_TYPE_DIVISION))
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnUser!!
        alTableHiddenColumn += columnDisabled

        addTableColumn(columnRecordType)
        addTableColumn(columnUserShortName)
        addTableColumn(columnRecordFullName)
        addTableColumn(columnUserEmail)
        addTableColumn(columnUserContactInfo)
        if (userConfig.isAdmin) {
            addTableColumn(columnUserLastLoginAttemptDate)
            addTableColumn(columnUserLastLoginAttemptTime)
            addTableColumn(columnUserLoginAttemptCount)
            addTableColumn(columnUserLastIP)
        } else {
            alTableHiddenColumn += columnUserLastLoginAttemptDate
            alTableHiddenColumn += columnUserLastLoginAttemptTime
            alTableHiddenColumn += columnUserLoginAttemptCount
            alTableHiddenColumn += columnUserLastIP
        }
        //        addTableColumn( columnFile );

        alFormHiddenColumn += columnUser!!
        alFormHiddenColumn += columnRecordType

        alFormColumn += columnUserName
        alFormColumn += columnParentFullName
        alFormColumn += columnDisabled
        alFormColumn += columnRecordFullName

        if (userConfig.isAdmin) {
            alFormColumn += columnUserLogin
            alFormColumn += columnUserPassword
        }
        alFormColumn += columnUserShortName
        alFormColumn += columnUserEmail
        alFormColumn += columnUserContactInfo
        if (userConfig.isAdmin) {
            alFormColumn += columnUserLastPasswordChangeDate
            alFormColumn += columnUserLoginAttemptCount
            alFormColumn += columnUserLastLoginAttemptDate
            alFormColumn += columnUserLastLoginAttemptTime
            alFormColumn += columnUserLastIP
        }

//        alFormColumn.add(columnFile)

        //----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn += columnRecordType
        alTableSortDirect += "ASC"
        alTableSortColumn += columnRecordFullName
        alTableSortDirect += "ASC"

        //----------------------------------------------------------------------------------------

        alChildData.add(ChildData("system_user_role", columnID))
        alChildData.add(ChildData("system_log_user", columnID))

        for (cd in alExtendChildData) {
            alChildData += ChildData(cd.alias, columnID, cd.isNewGroup)
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_user_role", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_user_property", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_new", "user_id", DependData.DELETE))

        for (dd in alExtendDependData) {
            alDependData += DependData(dd.destTableName, dd.destFieldName, dd.type)
        }

        //----------------------------------------------------------------------------------------------------------------------

        expandParentNameColumn = columnRecordFullName
    }
}

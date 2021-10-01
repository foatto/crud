package foatto.core_server.app.system

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import java.time.LocalDate
import java.time.ZonedDateTime

open class mClient : mAbstract() {

    lateinit var columnParent: ColumnInt
        private set
    lateinit var columnRecordType: ColumnInt
        private set
    lateinit var columnRecordFullName: ColumnString
        private set
    lateinit var columnDisabled: ColumnBoolean
        private set
    lateinit var columnUserPassword: ColumnString
        private set
    lateinit var columnUserLastLoginAttemptDate: ColumnDate3Int
        private set

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

        tableName = "SYSTEM_users"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        columnParent = ColumnInt(tableName, "parent_id", getClientParentId(application, aliasConfig.alias))

        columnRecordType = ColumnInt(
            aTableName = tableName,
            aFieldName = "org_type",
            aCaption = "",
            aDefaultValue = OrgType.ORG_TYPE_WORKER
        )

        columnRecordFullName = ColumnString(tableName, "full_name", "Полное имя", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserShortName = ColumnString(tableName, "short_name", "Краткое имя", STRING_COLUMN_WIDTH)

        columnDisabled = ColumnBoolean(tableName, "is_disabled", "Отключен", false)

        val columnUserLogin = ColumnString(tableName, "login", "Логин", STRING_COLUMN_WIDTH).apply {
            setUnique(true, "")
        }

        columnUserPassword = ColumnString(tableName, "pwd", "Пароль", STRING_COLUMN_WIDTH).apply {
            isPassword = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserLoginAttemptCount = ColumnInt(tableName, "at_count", "Счетчик попыток входа", 10)
        columnUserLastLoginAttemptDate = ColumnDate3Int(tableName, "at_ye", "at_mo", "at_da", "Дата последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastLoginAttemptTime = ColumnTime3Int(tableName, "at_ho", "at_mi", null, "Время последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastPasswordChangeDate = ColumnDate3Int(tableName, "pwd_ye", "pwd_mo", "pwd_da", "Дата последнего изменения пароля").apply {
            val toDay = ZonedDateTime.now(zoneId)
            default = LocalDate.of(toDay.year + 1, toDay.monthValue, toDay.dayOfMonth) // через год
        }

        val columnUserEmail = ColumnString(tableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH)
        val columnUserContactInfo = ColumnString(tableName, "contact_info", "Контактная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------

        addColumnDefinitions()

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnParent)
        alTableHiddenColumn.add(columnRecordType)
        alTableHiddenColumn.add(columnDisabled)

        addTableColumn(columnUserShortName)
        addTableColumn(columnRecordFullName)
        addTableColumn(columnUserEmail)
        addTableColumn(columnUserContactInfo)
        addTableColumn(columnUserLastLoginAttemptDate)
        addTableColumn(columnUserLastLoginAttemptTime)
        addTableColumn(columnUserLoginAttemptCount)

        alFormHiddenColumn.add(columnID)
        alFormHiddenColumn.add(columnParent)
        alFormHiddenColumn.add(columnRecordType)

        alFormColumn.add(columnDisabled)
        alFormColumn.add(columnRecordFullName)
        alFormColumn.add(columnUserLogin)
        alFormColumn.add(columnUserPassword)
        alFormColumn.add(columnUserShortName)
        alFormColumn.add(columnUserEmail)
        alFormColumn.add(columnUserContactInfo)
        alFormColumn.add(columnUserLastPasswordChangeDate)
        alFormColumn.add(columnUserLoginAttemptCount)
        alFormColumn.add(columnUserLastLoginAttemptDate)
        alFormColumn.add(columnUserLastLoginAttemptTime)

        //----------------------------------------------------------------------------------------------------------------------

        addColumnsToTableAndForm()

        //----------------------------------------------------------------------------------------------------------------------

        alTableSortColumn.add(columnRecordFullName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        for (cd in mUser.alExtendChildData) {
            alChildData.add(ChildData(cd.alias, columnID, cd.isNewGroup))
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_user_role", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_user_property", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_new", "user_id", DependData.DELETE))

        for (dd in mUser.alExtendDependData) {
            alDependData.add(DependData(dd.destTableName, dd.destFieldName, dd.type))
        }

    }

    open fun addColumnDefinitions() {}

    open fun addColumnsToTableAndForm() {}

    fun getClientParentId(application: iApplication, aliasName: String): Int {
        val idx = application.alClientAlias.indexOf(aliasName)
        return if(idx >= 0) {
            application.alClientParentId[idx].toIntOrNull() ?: 0
        } else {
            0
        }
    }

    fun getClientRoleIds(application: iApplication, aliasName: String): List<Int> {
        val idx = application.alClientAlias.indexOf(aliasName)
        return if(idx >= 0) {
            application.alClientRoleId[idx].split("-").filter(String::isNotBlank).mapNotNull(String::toIntOrNull)
        } else {
            emptyList()
        }
    }
}
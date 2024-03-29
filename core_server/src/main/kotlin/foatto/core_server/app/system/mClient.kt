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
import foatto.sql.CoreAdvancedConnection
import java.time.LocalDate
import java.time.ZonedDateTime

open class mClient : mAbstract() {

    lateinit var columnParent: ColumnInt
    lateinit var columnRecordType: ColumnInt
    lateinit var columnRecordFullName: ColumnString
    lateinit var columnDisabled: ColumnBoolean
    lateinit var columnUserPassword: ColumnString
    lateinit var columnUserLastLoginAttemptDate: ColumnDate3Int

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

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_users"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        columnParent = ColumnInt(modelTableName, "parent_id", getClientParentId(application, aliasConfig.name))

        columnRecordType = ColumnInt(
            aTableName = modelTableName,
            aFieldName = "org_type",
            aCaption = "",
            aDefaultValue = OrgType.ORG_TYPE_WORKER
        )

        columnRecordFullName = ColumnString(modelTableName, "full_name", "Полное имя", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserShortName = ColumnString(modelTableName, "short_name", "Краткое имя", STRING_COLUMN_WIDTH)

        columnDisabled = ColumnBoolean(modelTableName, "is_disabled", "Отключен", false)

        val columnUserLogin = ColumnString(modelTableName, "login", "Логин", STRING_COLUMN_WIDTH)

        columnUserPassword = ColumnString(modelTableName, "pwd", "Пароль", STRING_COLUMN_WIDTH).apply {
            isPassword = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserLoginAttemptCount = ColumnInt(modelTableName, "at_count", "Счетчик попыток входа", 10)
        columnUserLastLoginAttemptDate = ColumnDate3Int(modelTableName, "at_ye", "at_mo", "at_da", "Дата последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastLoginAttemptTime = ColumnTime3Int(modelTableName, "at_ho", "at_mi", null, "Время последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastPasswordChangeDate = ColumnDate3Int(modelTableName, "pwd_ye", "pwd_mo", "pwd_da", "Дата последнего изменения пароля").apply {
            val toDay = ZonedDateTime.now(zoneId)
            default = LocalDate.of(toDay.year + 1, toDay.monthValue, toDay.dayOfMonth) // через год
        }

        val columnUserEmail = ColumnString(modelTableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH)
        val columnUserContactInfo = ColumnString(modelTableName, "contact_info", "Контактная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //----------------------------------------------------------------------------------------------------------------------

        addColumnDefinitions()

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnUserLogin, "")

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
        alTableHiddenColumn += columnParent
        alTableHiddenColumn += columnRecordType
        alTableHiddenColumn += columnDisabled

        addTableColumn(columnUserShortName)
        addTableColumn(columnRecordFullName)
        addTableColumn(columnUserEmail)
        addTableColumn(columnUserContactInfo)
        addTableColumn(columnUserLastLoginAttemptDate)
        addTableColumn(columnUserLastLoginAttemptTime)
        addTableColumn(columnUserLoginAttemptCount)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnParent
        alFormHiddenColumn += columnRecordType

        alFormColumn += columnDisabled
        alFormColumn += columnRecordFullName
        alFormColumn += columnUserLogin
        alFormColumn += columnUserPassword
        alFormColumn += columnUserShortName
        alFormColumn += columnUserEmail
        alFormColumn += columnUserContactInfo
        alFormColumn += columnUserLastPasswordChangeDate
        alFormColumn += columnUserLoginAttemptCount
        alFormColumn += columnUserLastLoginAttemptDate
        alFormColumn += columnUserLastLoginAttemptTime

        //----------------------------------------------------------------------------------------------------------------------

        addColumnsToTableAndForm()

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnRecordFullName, true)

        //----------------------------------------------------------------------------------------

        for (cd in mUser.alExtendChildData) {
            alChildData.add(ChildData(cd.alias, columnId, cd.isNewGroup))
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_users", "user_id", DependData.SET, 0))
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
        return if (idx >= 0) {
            application.alClientParentId[idx].toIntOrNull() ?: 0
        } else {
            0
        }
    }

    fun getClientRoleIds(application: iApplication, aliasName: String): List<Int> {
        val idx = application.alClientAlias.indexOf(aliasName)
        return if (idx >= 0) {
            application.alClientRoleId[idx].split("-").filter(String::isNotBlank).mapNotNull(String::toIntOrNull)
        } else {
            emptyList()
        }
    }
}
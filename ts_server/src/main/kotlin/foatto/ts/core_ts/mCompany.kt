package foatto.ts.core_ts

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement
import foatto.ts.iTSApplication
import java.time.LocalDate
import java.time.ZonedDateTime

class mCompany : mAbstract() {

    lateinit var columnParent: ColumnInt
    lateinit var columnRecordType: ColumnInt
    lateinit var columnRecordFullName: ColumnString
    lateinit var columnDisabled: ColumnBoolean

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

        val companiesParentId = (application as iTSApplication).companiesParentId.toIntOrNull() ?: 0

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "SYSTEM_users"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        columnParent = ColumnInt(modelTableName, "parent_id", companiesParentId)

        columnRecordType = ColumnInt(
            aTableName = modelTableName,
            aFieldName = "org_type",
            aCaption = "",
            aDefaultValue = OrgType.ORG_TYPE_DIVISION
        )

        columnRecordFullName = ColumnString(modelTableName, "full_name", "Наименование", STRING_COLUMN_WIDTH).apply {
            isRequired = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        columnDisabled = ColumnBoolean(modelTableName, "is_disabled", "Отключен", false)

        //----------------------------------------------------------------------------------------------------------------------

        val columnUserEmail = ColumnString(modelTableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH)
        val columnUserContactInfo = ColumnString(modelTableName, "contact_info", "Контактная информация", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)

        //--- невидимые поля для совместимости с полным system_user

        val columnUserLoginAttemptCount = ColumnInt(modelTableName, "at_count", "Счетчик попыток входа", 10)

        val columnUserLastLoginAttemptDate = ColumnDate3Int(modelTableName, "at_ye", "at_mo", "at_da", "Дата последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastLoginAttemptTime = ColumnTime3Int(modelTableName, "at_ho", "at_mi", null, "Время последней попытки входа").apply {
            isEditable = false
        }

        val columnUserLastPasswordChangeDate = ColumnDate3Int(modelTableName, "pwd_ye", "pwd_mo", "pwd_da", "Дата последнего изменения пароля").apply {
            val toDay = ZonedDateTime.now(zoneId)
            default = LocalDate.of(toDay.year + 1, toDay.monthValue, toDay.dayOfMonth) // через год
        }

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
        alTableHiddenColumn += columnParent
        alTableHiddenColumn += columnRecordType

        addTableColumn(columnRecordFullName)
        addTableColumn(columnUserEmail)
        addTableColumn(columnUserContactInfo)
        addTableColumn(columnDisabled)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnParent
        alFormHiddenColumn += columnRecordType
        alFormHiddenColumn += columnUserLoginAttemptCount
        alFormHiddenColumn += columnUserLastLoginAttemptDate
        alFormHiddenColumn += columnUserLastLoginAttemptTime
        alFormHiddenColumn += columnUserLastPasswordChangeDate

        alFormColumn += columnRecordFullName
        alFormColumn += columnUserEmail
        alFormColumn += columnUserContactInfo
        alFormColumn += columnDisabled

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnRecordFullName, true)

        //----------------------------------------------------------------------------------------

        alChildData += ChildData(
            aAlias = "ts_user",
            aColumn = columnId,
            aNewGroup = true,
            aDefaultOperation = true,
        )
        alChildData += ChildData("ts_object", aColumn = columnId)

        //----------------------------------------------------------------------------------------

        //--- кастомные проверки и удаление сделаны вручную в cCompany
        //for (dd in mUser.alExtendDependData) {
        //    alDependData.add(DependData(dd.destTableName, dd.destFieldName, dd.type))
        //}

    }
}
package foatto.ts.core_ts

import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedConnection
import java.time.LocalDate
import java.time.ZonedDateTime

class mTSUser : mAbstract() {

    lateinit var columnParentID: ColumnInt
    lateinit var columnParent: ColumnInt
    lateinit var columnParentFullName: ColumnString

    //--- тип строки - папка или элемент списка
    lateinit var columnRecordType: ColumnComboBox
    lateinit var columnRecordFullName: ColumnString

    lateinit var columnDisabled: ColumnBoolean
    lateinit var columnUserPassword: ColumnString
    lateinit var columnUserLastLoginAttemptDate: ColumnDate3Int

    lateinit var columnControlEnabled: ColumnBoolean

    //----------------------------------------------------------------------------------------------------------------------

    lateinit var selfLinkParentTableName: String

    //----------------------------------------------------------------------------------------------------------------------

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

        selfLinkParentTableName = "${modelTableName}__PARENT"

        //----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

//        columnUser = ColumnInt(modelTableName, "user_id", userConfig.userId)

        //----------------------------------------------------------------------------------------------------------------------

        columnRecordType = ColumnComboBox(modelTableName, "org_type", "", OrgType.ORG_TYPE_BOSS)

        columnParentID = ColumnInt(selfLinkParentTableName, "id").apply {
            selfLinkTableName = modelTableName
        }
        columnParent = ColumnInt(modelTableName, "parent_id", columnParentID)
        columnParentFullName = ColumnString(selfLinkParentTableName, "full_name", "Компания", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = modelTableName

            selectorAlias = "ts_company"
            addSelectorColumn(columnParent, columnParentID)
            addSelectorColumn(this)
        }

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
//        val columnFile = ColumnFile(tableName, "file_id", "Файлы")

        val columnUserLastIP = ColumnString(modelTableName, "last_ip", "Last IP", STRING_COLUMN_WIDTH).apply {
            isEditable = false
        }

        columnControlEnabled = ColumnBoolean(modelTableName, "is_control_enabled", "Управление устройствами", false).apply {
            isVirtual = true
        }

        //----------------------------------------------------------------------------------------------------------------------

        addUniqueColumn(columnUserLogin, "")

        //----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn += columnId
//        alTableHiddenColumn += columnUser!!
        alTableHiddenColumn += columnParent
        alTableHiddenColumn += columnDisabled       // отыгрывается через серый цвет полного имени
        alTableHiddenColumn += columnRecordType

        alTableGroupColumn += columnParentFullName

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
        addTableColumn(columnControlEnabled)

        alFormHiddenColumn += columnId
        alFormHiddenColumn += columnParent
//        alFormHiddenColumn += columnUser!!
        alFormHiddenColumn += columnRecordType

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
        alFormColumn += columnControlEnabled

        //----------------------------------------------------------------------------------------------------------------------

        addTableSort(columnParentFullName, true)
        addTableSort(columnRecordFullName, true)

        //----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["ts_company"] = columnParent

        //----------------------------------------------------------------------------------------

//        alChildData.add(ChildData("system_user_role", columnId))
//        alChildData.add(ChildData("system_log_user", columnId))
//
//        for (cd in alExtendChildData) {
//            alChildData += ChildData(cd.alias, columnId, cd.isNewGroup)
//        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("SYSTEM_users", "user_id", DependData.SET, 0))
        alDependData.add(DependData("SYSTEM_user_role", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_user_property", "user_id", DependData.DELETE))
        alDependData.add(DependData("SYSTEM_new", "user_id", DependData.DELETE))
    }
}

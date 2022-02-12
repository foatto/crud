package foatto.office

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.*
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mReminder : mAbstract() {

    companion object {
        const val ALERT_TAG = "reminder"

        //--- служебный тип для обозначения отсутствия необходимости в напоминании
        const val REMINDER_TYPE_NO_REMINDER = -1
        const val REMINDER_TYPE_OTHER = 0
        const val REMINDER_TYPE_CALL = 1
        const val REMINDER_TYPE_MEET = 2
        const val REMINDER_TYPE_CALL_REMEMBER = 3
        const val REMINDER_TYPE_INPUT_CALL = 4
        const val REMINDER_TYPE_MEETING = 5

        val hmReminderName = mapOf(
            REMINDER_TYPE_NO_REMINDER to "(неизвестный тип)",
            REMINDER_TYPE_CALL to "Позвонить",
            REMINDER_TYPE_MEET to "Назначить встречу",
            REMINDER_TYPE_CALL_REMEMBER to "Напомнить о звонке",
            REMINDER_TYPE_INPUT_CALL to "Входящие звонки",
            REMINDER_TYPE_MEETING to "Совещания",
            REMINDER_TYPE_OTHER to "Прочие напоминания",
        )
    }

//----------------------------------------------------------------------------------------------------------------------

    var type = REMINDER_TYPE_NO_REMINDER
        private set

    lateinit var columnType: ColumnRadioButton
        private set

    lateinit var columnDate: ColumnDate3Int
    lateinit var columnTime: ColumnTime3Int
    private lateinit var columnAlertTime: ColumnComboBox

    lateinit var columnSubj: ColumnString
        private set

//----------------------------------------------------------------------------------------------------------------------

    //--- оповещать при добавлении или изменении напоминания
    override fun getAddAlertTag() = ALERT_TAG
    override fun getEditAlertTag() = ALERT_TAG

    //--- прежнее (устаревшее) оповещение должно быть удалено
    override fun isUniqueAlertRowID() = true

    override fun getDateTimeColumns(): Array<iColumn> {
        return arrayOf(columnAlertTime, columnDate, columnTime)
    }

//----------------------------------------------------------------------------------------------------------------------

    override fun init(application: iApplication, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int?) {

        super.init(application, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

//----------------------------------------------------------------------------------------------------------------------

        when (aliasConfig.alias.removeSuffix("_archive")) {
            "office_reminder" -> type = REMINDER_TYPE_NO_REMINDER // все напоминания одним списком
            "office_reminder_call" -> type = REMINDER_TYPE_CALL
            "office_reminder_meet" -> type = REMINDER_TYPE_MEET
            "office_reminder_call_remember" -> type = REMINDER_TYPE_CALL_REMEMBER
            "office_reminder_input_call" -> type = REMINDER_TYPE_INPUT_CALL
            "office_reminder_meeting" -> type = REMINDER_TYPE_MEETING
            "office_reminder_other" -> type = REMINDER_TYPE_OTHER
        }

//----------------------------------------------------------------------------------------------------------------------

        modelTableName = "OFFICE_reminder"

//----------------------------------------------------------------------------------------------------------------------

        columnId = ColumnInt(modelTableName, "id")

        columnUser = ColumnInt(modelTableName, "user_id", userConfig.userId)

        columnActive = ColumnBoolean(modelTableName, "in_active", "", true)
        columnArchive = ColumnBoolean(modelTableName, "in_archive", "", false)

        columnType = ColumnRadioButton(modelTableName, "type", "Тип", if (type < 0) 0 else type).apply {
            addChoice(REMINDER_TYPE_CALL, hmReminderName[REMINDER_TYPE_CALL]!!)
            addChoice(REMINDER_TYPE_MEET, hmReminderName[REMINDER_TYPE_MEET]!!)
            addChoice(REMINDER_TYPE_CALL_REMEMBER, hmReminderName[REMINDER_TYPE_CALL_REMEMBER]!!)
            addChoice(REMINDER_TYPE_INPUT_CALL, hmReminderName[REMINDER_TYPE_INPUT_CALL]!!)
            addChoice(REMINDER_TYPE_MEETING, hmReminderName[REMINDER_TYPE_MEETING]!!)
            addChoice(REMINDER_TYPE_OTHER, hmReminderName[REMINDER_TYPE_OTHER]!!)
        }

        columnDate = ColumnDate3Int(modelTableName, "ye", "mo", "da", "Дата")

        columnTime = ColumnTime3Int(modelTableName, "ho", "mi", null, "Время")

        columnAlertTime = ColumnComboBox(modelTableName, "alert_time", "Оповещение", -1).apply {
            addChoice(-1, "не оповещать")
            addChoice(0, "в указанное время")
            addChoice(5 * 60, "за 5 минут")
            addChoice(15 * 60, "за 15 минут")
            addChoice(60 * 60, "за час")
        }

        columnSubj = ColumnString(modelTableName, "subj", "Тема", 4, STRING_COLUMN_WIDTH, textFieldMaxSize)

        val columnDescr = ColumnString(modelTableName, "descr", "Примечания", 4, STRING_COLUMN_WIDTH, textFieldMaxSize)

        val columnPeopleID = ColumnInt("OFFICE_people", "id")
        val columnPeople = ColumnInt(modelTableName, "people_id", columnPeopleID)
        val columnPeopleName = ColumnString("OFFICE_people", "name", "Ф.И.О.", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeoplePost = ColumnString("OFFICE_people", "post", "Должность", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleEmail = ColumnString("OFFICE_people", "e_mail", "E-mail", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleCell = ColumnString("OFFICE_people", "cell_no", "Мобильный телефон", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeoplePhone = ColumnString("OFFICE_people", "phone_no", "Рабочий телефон", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleFax = ColumnString("OFFICE_people", "fax_no", "Факс", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleAssistant = ColumnString("OFFICE_people", "assistant_name", "Помощник", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnPeopleAssistantEmail = ColumnString("OFFICE_people", "assistant_mail", "E-mail помощника", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleAssistantCell = ColumnString("OFFICE_people", "assistant_cell", "Телефон помощника", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }
        val columnPeopleContactInfo = ColumnString("OFFICE_people", "contact_info", "Доп. информация по контакту", 6, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnPeopleBirthDate = ColumnDate3Int("OFFICE_people", "birth_ye", "birth_mo", "birth_da", "День рождения").apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnPeopleWorkState = ColumnComboBox("OFFICE_people", "work_state", "Состояние работы с клиентом", mPeople.WORK_STATE_NOT_NEED).apply {
            addChoice(mPeople.WORK_STATE_NOT_NEED, "(нет)")
            addChoice(mPeople.WORK_STATE_IN_WORK, "В работе")
            addChoice(mPeople.WORK_STATE_OUT_WORK, "Отработан")
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }

        //--- хоть здесь и нет применения "предыдущего" SYSTEM_users_1,
        //--- но очень желательно применять псевдоимена таблиц такие же, как в исходном OFFICE_people
        val selfLinkManagerTableName = "SYSTEM_users_2"
        val columnPeopleManagerID = ColumnInt(selfLinkManagerTableName, "id").apply {
            selfLinkTableName = "SYSTEM_users"
        }
        val columnPeopleManager = ColumnInt("OFFICE_people", "manager_id", columnPeopleManagerID, 0)
        val columnPeopleManagerName = ColumnString(selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH).apply {
            selfLinkTableName = "SYSTEM_users"
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        //--- отдельное служебное поле для копирования селектора из mPeople, т.к. там оно в виде SYSTEM_users_2.full_name,
        //--- уже без указания setSelfLinkTableName( "SYSTEM_users" );
        val columnPeopleManagerName_ = ColumnString(selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH)
        val columnCompanyID = ColumnInt("OFFICE_company", "id")
        val columnCompany = ColumnInt("OFFICE_people", "company_id", columnCompanyID)
        val columnCompanyBlackList = ColumnBoolean("OFFICE_company", "in_black_list", "В чёрном списке").apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnCompanyName = ColumnString("OFFICE_company", "name", "Предприятие", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnCompanyAddress = ColumnString("OFFICE_company", "address", "Адрес компании", 6, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnCompanyContactInfo = ColumnString("OFFICE_company", "contact_info", "Доп. информация по компании", 6, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnCityID = ColumnInt("OFFICE_city", "id")
        val columnCity = ColumnInt("OFFICE_company", "city_id", columnCityID)
        val columnCityName = ColumnString("OFFICE_city", "name", "Город", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
            formPinMode = FormPinMode.OFF
        }
        val columnPhoneCode = ColumnString("OFFICE_city", "phone_code", "Код города", STRING_COLUMN_WIDTH).apply {
            addFormVisible(columnType, false, setOf(REMINDER_TYPE_MEETING))
        }

        columnPeopleName.run {
            selectorAlias = "office_people"
            addSelectorColumn(columnPeople, columnPeopleID)
            addSelectorColumn(columnPeopleName)
            addSelectorColumn(columnPeoplePost)
            addSelectorColumn(columnPeopleEmail)
            addSelectorColumn(columnPeopleCell)
            addSelectorColumn(columnPeoplePhone)
            addSelectorColumn(columnPeopleFax)
            addSelectorColumn(columnPeopleAssistant)
            addSelectorColumn(columnPeopleAssistantEmail)
            addSelectorColumn(columnPeopleAssistantCell)
            addSelectorColumn(columnPeopleContactInfo)
            addSelectorColumn(columnPeopleBirthDate)
            addSelectorColumn(columnPeopleWorkState)
            addSelectorColumn(columnPeopleManagerName, columnPeopleManagerName_)
            addSelectorColumn(columnCompanyBlackList)
            addSelectorColumn(columnCompanyName)
            addSelectorColumn(columnCompanyAddress)
            addSelectorColumn(columnCompanyContactInfo)
            addSelectorColumn(columnCityName)
            addSelectorColumn(columnPhoneCode)
        }

//----------------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnId)
        alTableHiddenColumn.add(columnUser!!)
        alTableHiddenColumn.add(columnActive!!)
        alTableHiddenColumn.add(columnArchive!!)
        alTableHiddenColumn.add(columnPeople)
        alTableHiddenColumn.add(columnPeopleManager)
        alTableHiddenColumn.add(columnCompany)
        alTableHiddenColumn.add(columnCity)

        alTableGroupColumn.add(columnDate)
        alTableGroupColumn.add(columnTime)
        if (type < 0) {
            addTableColumn(columnType)
        } else {
            alTableHiddenColumn.add(columnType)
        }
        addTableColumn(columnSubj)
        addTableColumn(columnDescr)
        if (type == REMINDER_TYPE_MEETING) {
            alTableHiddenColumn.add(columnPeopleName)
            alTableHiddenColumn.add(columnPeoplePost)
            alTableHiddenColumn.add(columnCompanyBlackList)
            alTableHiddenColumn.add(columnCompanyName)
            alTableHiddenColumn.add(columnCityName)
            alTableHiddenColumn.add(columnPeopleWorkState)
            alTableHiddenColumn.add(columnPeopleManagerName)
        } else {
            addTableColumn(columnPeopleName)
            addTableColumn(columnPeoplePost)
            addTableColumn(columnCompanyBlackList)
            addTableColumn(columnCompanyName)
            addTableColumn(columnCityName)
            addTableColumn(columnPeopleWorkState)
            addTableColumn(columnPeopleManagerName)
        }

        alFormHiddenColumn.add(columnId)
        alFormHiddenColumn.add(columnUser!!)
        alFormHiddenColumn.add(columnActive!!)
        alFormHiddenColumn.add(columnArchive!!)
        alFormHiddenColumn.add(columnPeople)
        alFormHiddenColumn.add(columnPeopleManager)
        alFormHiddenColumn.add(columnCompany)
        alFormHiddenColumn.add(columnCity)
        alFormHiddenColumn.add(columnAlertTime)

        alFormColumn.add(columnType)
        alFormColumn.add(columnDate)
        alFormColumn.add(columnTime)
        alFormColumn.add(columnSubj)
        alFormColumn.add(columnDescr)
        alFormColumn.add(columnPeopleName)
        alFormColumn.add(columnPeoplePost)
        alFormColumn.add(columnPeopleEmail)
        alFormColumn.add(columnPeopleCell)
        alFormColumn.add(columnPeoplePhone)
        alFormColumn.add(columnPeopleFax)
        alFormColumn.add(columnPeopleAssistant)
        alFormColumn.add(columnPeopleAssistantEmail)
        alFormColumn.add(columnPeopleAssistantCell)
        alFormColumn.add(columnPeopleContactInfo)
        alFormColumn.add(columnPeopleBirthDate)
        alFormColumn.add(columnPeopleWorkState)
        alFormColumn.add(columnPeopleManagerName)
        alFormColumn.add(columnCompanyBlackList)
        alFormColumn.add(columnCompanyName)
        alFormColumn.add(columnCompanyAddress)
        alFormColumn.add(columnCompanyContactInfo)
        alFormColumn.add(columnCityName)
        alFormColumn.add(columnPhoneCode)

//----------------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        alTableSortColumn.add(columnDate)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnTime)
        alTableSortDirect.add("ASC")

//----------------------------------------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!
        hmParentColumn["office_city"] = columnCity
        hmParentColumn["office_company"] = columnCompany
        hmParentColumn["office_people"] = columnPeople
    }
}

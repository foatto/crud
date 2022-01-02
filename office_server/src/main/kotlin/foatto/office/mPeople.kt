package foatto.office

import foatto.core.link.FormPinMode
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.column.ColumnBoolean
import foatto.core_server.app.server.column.ColumnComboBox
import foatto.core_server.app.server.column.ColumnDate3Int
import foatto.core_server.app.server.column.ColumnFile
import foatto.core_server.app.server.column.ColumnInt
import foatto.core_server.app.server.column.ColumnString
import foatto.core_server.app.server.column.ColumnTime3Int
import foatto.core_server.app.server.mAbstract
import foatto.sql.CoreAdvancedStatement

class mPeople : mAbstract() {

    companion object {

        //--- состояние работы с клиентом
        val WORK_STATE_OUT_WORK = -1   // отработан
        val WORK_STATE_NOT_NEED = 0   // не требует работы с ним
        val WORK_STATE_IN_WORK = 1   // в работе
    }

    lateinit var columnPeopleWorkState: ColumnComboBox
        private set
    lateinit var columnClientPlanDate: ColumnDate3Int
        private set
    lateinit var columnClientPlanTime: ColumnTime3Int
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

        //--- это обычный контакт (people) или сопровождаемый клиент (client) ?
        val isPeople = aliasConfig.alias == "office_people"

        //----------------------------------------------------------------------------------------------------------------------

        modelTableName = "OFFICE_people"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(modelTableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        //--- только для формы, в таблице оно показывается само/автоматически
        var columnUserName: ColumnString? = null

        //--- в режиме "контакт": поля для показа текущего менеджера
        var columnPeopleManager: ColumnInt? = null
        var columnPeopleManagerName: ColumnString? = null

        //--- в режиме "клиент": поле userID, всегда == 0 для правильного создания общего контакта/клиента
        var columnUser_: ColumnInt? = null

        //--- админы могут явно выбирать режим работы и менеджера контакта в любое время
        columnPeopleWorkState = ColumnComboBox(modelTableName, "work_state", "Состояние работы с клиентом", if (isPeople) WORK_STATE_NOT_NEED else WORK_STATE_IN_WORK).apply {
            isEditable = !isPeople || userConfig.isAdmin
            addChoice(WORK_STATE_NOT_NEED, "(нет)")
            addChoice(WORK_STATE_IN_WORK, "В работе")
            //--- нет смысла создавать отработанного клиента
            if (id != 0) addChoice(WORK_STATE_OUT_WORK, "Отработан")
            formPinMode = FormPinMode.OFF
        }

        if (isPeople) {
            //--- админы могут явно выбирать владельца контакта в любое время
            if (userConfig.isAdmin) {
                val selfLinkUserTableName = "SYSTEM_users_1"
                val columnUserID = ColumnInt(selfLinkUserTableName, "id").apply {
                    selfLinkTableName = "SYSTEM_users"
                }
                //--- в режиме клиента вместо user_id регулятором прав доступа работает manager_id
                columnUser = ColumnInt(modelTableName, "user_id", columnUserID, 0)
                columnUserName = ColumnString(selfLinkUserTableName, "full_name", "Пользователь", STRING_COLUMN_WIDTH).apply {
                    selfLinkTableName = "SYSTEM_users"
                    //columnUserName.setRequired( true ); - может быть ничья/общая
                    selectorAlias = "system_user_people"
                    addSelectorColumn(columnUser!!, columnUserID)
                    addSelectorColumn(this)
                }
            } else if (id == 0) {
                columnUser = ColumnComboBox(modelTableName, "user_id", "Доступ", 0).apply {
                    addChoice(0, "общий")
                    addChoice(userConfig.userId, "личный")
                }
            } else {
                columnUser = ColumnInt(modelTableName, "user_id", 0)
            }//--- во всех прочих случаях это обычное служебное/невидимое поле
            //--- обычные пользователи могут указать доступ к своему контакту только при его создании

            //--- показ имени менеджера в режиме контакта
            val selfLinkManagerTableName = "SYSTEM_users_2"
            val columnPeopleManagerID = ColumnInt(selfLinkManagerTableName, "id").apply {
                selfLinkTableName = "SYSTEM_users"
            }
            columnPeopleManager = ColumnInt(modelTableName, "manager_id", columnPeopleManagerID, 0).apply {
                columnPeopleManagerName = ColumnString(selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH)
                selfLinkTableName = "SYSTEM_users"
                addFormVisible(columnPeopleWorkState!!, true, setOf(WORK_STATE_IN_WORK))
                formPinMode = FormPinMode.OFF
            }
        }
        //--- в режиме работы с клиентом всегда можно переназначить клиента другому менеджеру
        else {
            columnUser_ = ColumnInt(modelTableName, "user_id", 0)

            val selfLinkUserTableName = "SYSTEM_users_1"
            val columnUserID = ColumnInt(selfLinkUserTableName, "id").apply {
                selfLinkTableName = "SYSTEM_users"
            }
            //--- в режиме клиента вместо user_id регулятором прав доступа работает manager_id
            columnUser = ColumnInt(modelTableName, "manager_id", columnUserID, userConfig.userId)
            columnUserName = ColumnString(selfLinkUserTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH).apply {
                selfLinkTableName = "SYSTEM_users"
                //columnUserName.setRequired( true ); - может быть ничья/общая
                selectorAlias = "system_user_people"
                addSelectorColumn(columnUser!!, columnUserID)
                addSelectorColumn(this)
            }
        }

        //---------------------------------------------------------------------------------------------------------------

        val columnCompanyID = ColumnInt("OFFICE_company", "id")
        val columnCompany = ColumnInt(modelTableName, "company_id", columnCompanyID)

        val columnCompanyBlackList = ColumnBoolean("OFFICE_company", "in_black_list", "В чёрном списке").apply {
            formPinMode = FormPinMode.OFF
        }
        val columnCompanyAddress = ColumnString("OFFICE_company", "address", "Адрес компании", 12, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            formPinMode = FormPinMode.OFF
        }
        val columnCompanyContactInfo = ColumnString("OFFICE_company", "contact_info", "Доп. информация по компании", 10, STRING_COLUMN_WIDTH, textFieldMaxSize).apply {
            formPinMode = FormPinMode.OFF
        }

        val columnCityID = ColumnInt("OFFICE_city", "id")
        val columnCity = ColumnInt("OFFICE_company", "city_id", columnCityID)

        val columnCityName = ColumnString("OFFICE_city", "name", "Город", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
        }
        val columnCityPhoneCode = ColumnString("OFFICE_city", "phone_code", "Код города", STRING_COLUMN_WIDTH)

        val columnCompanyName = ColumnString("OFFICE_company", "name", "Предприятие", STRING_COLUMN_WIDTH).apply {
            formPinMode = FormPinMode.OFF
            selectorAlias = "office_company"
            addSelectorColumn(columnCompany, columnCompanyID)
            addSelectorColumn(columnCompanyBlackList)
            addSelectorColumn(this)
            addSelectorColumn(columnCompanyAddress)
            addSelectorColumn(columnCompanyContactInfo)
            addSelectorColumn(columnCityName)
            addSelectorColumn(columnCityPhoneCode)
        }

        val columnPeopleName = ColumnString(modelTableName, "name", "Ф.И.О.", STRING_COLUMN_WIDTH)
        val columnPeoplePost = ColumnString(modelTableName, "post", "Должность", STRING_COLUMN_WIDTH)
        val columnPeopleEmail = ColumnString(modelTableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH)
        val columnPeopleCell = ColumnString(modelTableName, "cell_no", "Мобильный телефон", STRING_COLUMN_WIDTH)
        val columnPeoplePhone = ColumnString(modelTableName, "phone_no", "Рабочий телефон", STRING_COLUMN_WIDTH)
        val columnPeopleFax = ColumnString(modelTableName, "fax_no", "Факс", STRING_COLUMN_WIDTH)
        val columnPeopleAssistant = ColumnString(modelTableName, "assistant_name", "Помощник", STRING_COLUMN_WIDTH)
        val columnPeopleAssistantEmail = ColumnString(modelTableName, "assistant_mail", "E-mail помощника", STRING_COLUMN_WIDTH)
        val columnPeopleAssistantCell = ColumnString(modelTableName, "assistant_cell", "Телефон помощника", STRING_COLUMN_WIDTH)
        val columnPeopleContactInfo = ColumnString(modelTableName, "contact_info", "Доп. информация по контакту", 10, STRING_COLUMN_WIDTH, textFieldMaxSize)
        val columnPeopleBirthDate = ColumnDate3Int(modelTableName, "birth_ye", "birth_mo", "birth_da", "День рождения")

        val columnFile = ColumnFile(application, modelTableName, "file_id", "Файлы")

        //---------------------------------------------------------------------------------------------------------------

        val columnBusiness = ColumnComboBox(modelTableName, "business_id", "Направление деятельности", 0).apply {
            val rs = stm.executeQuery(" SELECT id , name FROM OFFICE_business ")
            while (rs.next()) addChoice(rs.getInt(1), rs.getString(2))
            rs.close()
        }

        val columnClientActionDate = ColumnDate3Int(modelTableName, "action_ye", "action_mo", "action_da", "Дата последнего действия").apply {
            isEditable = false
        }
        val columnClientActionTime = ColumnTime3Int(modelTableName, "action_ho", "action_mi", null, "Время последнего действия").apply {
            isEditable = false
        }

        columnClientPlanDate = ColumnDate3Int(modelTableName, "plan_ye", "plan_mo", "plan_da", "Дата следующего действия").apply {
            isEditable = false
            formPinMode = FormPinMode.OFF
        }
        columnClientPlanTime = ColumnTime3Int(modelTableName, "plan_ho", "plan_mi", null, "Время следующего действия").apply {
            isEditable = false
        }

        //---------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID)
        alTableHiddenColumn.add(columnUser!!)
        if (!isPeople) alTableHiddenColumn.add(columnUser_!!)
        if (isPeople) alTableHiddenColumn.add(columnPeopleManager!!)
        alTableHiddenColumn.add(columnCompany)
        alTableHiddenColumn.add(columnCity)
        //--- показывать не будем, но для селектора оставим
        alTableHiddenColumn.add(columnPeopleEmail)
        alTableHiddenColumn.add(columnPeopleCell)
        alTableHiddenColumn.add(columnPeoplePhone)
        alTableHiddenColumn.add(columnPeopleFax)
        alTableHiddenColumn.add(columnPeopleAssistant)
        alTableHiddenColumn.add(columnPeopleAssistantEmail)
        alTableHiddenColumn.add(columnPeopleAssistantCell)
        alTableHiddenColumn.add(columnPeopleContactInfo)
        alTableHiddenColumn.add(columnPeopleBirthDate)
        alTableHiddenColumn.add(columnCompanyAddress)
        alTableHiddenColumn.add(columnCompanyContactInfo)
        alTableHiddenColumn.add(columnCityPhoneCode)

        if (aliasConfig.alias == "office_client_in_work") {
            alTableGroupColumn.add(columnClientPlanDate)
            alTableGroupColumn.add(columnClientPlanTime)
        } else {
            alTableHiddenColumn.add(columnClientPlanDate)
            alTableHiddenColumn.add(columnClientPlanTime)
        }

        addTableColumn(columnPeopleName)
        addTableColumn(columnPeoplePost)
        //--- на смартфоне галочку черного списка не показываем
        addTableColumn(columnCompanyBlackList)
        addTableColumn(columnCompanyName)
        addTableColumn(columnCityName)
//        addTableColumn(columnPeopleWorkState)
        if (isPeople) {
//            addTableColumn(columnPeopleManagerName!!)
        }
//        addTableColumn(columnBusiness)
        if (isPeople) {
            alTableHiddenColumn.add(columnClientActionDate)
            alTableHiddenColumn.add(columnClientActionTime)
        } else {
            addTableColumn(columnClientActionDate)
            addTableColumn(columnClientActionTime)
        }
        addTableColumn(columnFile)


        alFormHiddenColumn.add(columnID)
        if (!isPeople) alFormHiddenColumn.add(columnUser_!!)
        if (isPeople) alFormHiddenColumn.add(columnPeopleManager!!)
        alFormHiddenColumn.add(columnCompany)
        alFormHiddenColumn.add(columnCity)

        (if (!isPeople || userConfig.isAdmin || id != 0) alFormHiddenColumn else alFormColumn).add(columnUser!!)
        if (!isPeople || userConfig.isAdmin) {
            alFormColumn.add(columnUserName!!)
        }
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
        alFormColumn.add(columnCompanyBlackList)
        alFormColumn.add(columnCompanyName)
        alFormColumn.add(columnCompanyAddress)
        alFormColumn.add(columnCompanyContactInfo)
        alFormColumn.add(columnCityName)
        alFormColumn.add(columnCityPhoneCode)
//        alFormColumn.add(columnPeopleWorkState)
//        if(isPeople) alFormColumn.add(columnPeopleManagerName!!)
//        alFormColumn.add(columnBusiness)
        (if (isPeople) alFormHiddenColumn else alFormColumn).add(columnClientActionDate)
        (if (isPeople) alFormHiddenColumn else alFormColumn).add(columnClientActionTime)
        (if (isPeople) alFormHiddenColumn else alFormColumn).add(columnClientPlanDate)
        (if (isPeople) alFormHiddenColumn else alFormColumn).add(columnClientPlanTime)
        alFormColumn.add(columnFile)

        //---------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        if (!isPeople) {
            alTableSortColumn.add(columnClientPlanDate)
            alTableSortDirect.add("ASC")
            alTableSortColumn.add(columnClientPlanTime)
            alTableSortDirect.add("ASC")
        }
        alTableSortColumn.add(columnPeopleName)
        alTableSortDirect.add("ASC")
        alTableSortColumn.add(columnCompanyName)
        alTableSortDirect.add("ASC")

        //----------------------------------------------------------------------------------------

        hmParentColumn["system_user"] = columnUser!!

        hmParentColumn["office_city"] = columnCity
        hmParentColumn["office_company"] = columnCompany
        hmParentColumn["office_business"] = columnBusiness

        //----------------------------------------------------------------------------------------

        if (isPeople) {
            alChildData.add(ChildData("office_reminder", columnID, true))
            alChildData.add(ChildData("office_reminder_call", columnID))
            alChildData.add(ChildData("office_reminder_meet", columnID))
            alChildData.add(ChildData("office_reminder_call_remember", columnID))
            alChildData.add(ChildData("office_reminder_input_call", columnID))
            //alChildData.add( new ChildData( "office_reminder_meeting" , columnID ) ); - неприменимо для совещаний
            alChildData.add(ChildData("office_reminder_other", columnID))
            alChildData.add(ChildData("office_client_work_view", columnID, true))
        } else {
            alChildData.add(ChildData("office_client_work_history", columnID, true, true))
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_reminder", "people_id"))
        alDependData.add(DependData("OFFICE_client_work", "client_id"))
    }
}

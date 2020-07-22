package foatto.office

import foatto.core.link.FormPinMode
import foatto.core_server.app.server.*
import foatto.core_server.app.server.column.*
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

    override fun init(appController: CoreSpringController, aStm: CoreAdvancedStatement, aliasConfig: AliasConfig, userConfig: UserConfig, aHmParam: Map<String, String>, hmParentData: MutableMap<String, Int>, id: Int) {

        super.init(appController, aStm, aliasConfig, userConfig, aHmParam, hmParentData, id)

        //----------------------------------------------------------------------------------------------------------------------

        //--- это обычный контакт (people) или сопровождаемый клиент (client) ?
        val isPeople = aliasConfig.alias == "office_people"

        //----------------------------------------------------------------------------------------------------------------------

        tableName = "OFFICE_people"

        //----------------------------------------------------------------------------------------------------------------------

        columnID = ColumnInt(tableName, "id")

        //----------------------------------------------------------------------------------------------------------------------

        //--- только для формы, в таблице оно показывается само/автоматически
        var columnUserName: ColumnString? = null

        //--- в режиме "контакт": поля для показа текущего менеджера
        var columnPeopleManager: ColumnInt? = null
        var columnPeopleManagerName: ColumnString? = null

        //--- в режиме "клиент": поле userID, всегда == 0 для правильного создания общего контакта/клиента
        var columnUser_: ColumnInt? = null

        //--- админы могут явно выбирать режим работы и менеджера контакта в любое время
        columnPeopleWorkState = ColumnComboBox(tableName, "work_state", "Состояние работы с клиентом", if(isPeople) WORK_STATE_NOT_NEED else WORK_STATE_IN_WORK)
        columnPeopleWorkState.isEditable = !isPeople || userConfig.isAdmin
        columnPeopleWorkState.addChoice(WORK_STATE_NOT_NEED, "(нет)")
        columnPeopleWorkState.addChoice(WORK_STATE_IN_WORK, "В работе")
        //--- нет смысла создавать отработанного клиента
        if(id != 0) columnPeopleWorkState.addChoice(WORK_STATE_OUT_WORK, "Отработан")
        columnPeopleWorkState.formPinMode = FormPinMode.OFF

        if(isPeople) {
            //--- админы могут явно выбирать владельца контакта в любое время
            if(userConfig.isAdmin) {
                val selfLinkUserTableName = "SYSTEM_users_1"
                val columnUserID = ColumnInt(selfLinkUserTableName, "id")
                columnUserID.selfLinkTableName = "SYSTEM_users"
                //--- в режиме клиента вместо user_id регулятором прав доступа работает manager_id
                columnUser = ColumnInt(tableName, "user_id", columnUserID, 0)
                columnUserName = ColumnString(selfLinkUserTableName, "full_name", "Пользователь", STRING_COLUMN_WIDTH)
                columnUserName.selfLinkTableName = "SYSTEM_users"
                //columnUserName.setRequired( true ); - может быть ничья/общая
                columnUserName.selectorAlias = "system_user_people"
                columnUserName.addSelectorColumn(columnUser!!, columnUserID)
                columnUserName.addSelectorColumn(columnUserName)
            } else if(id == 0) {
                columnUser = ColumnComboBox(tableName, "user_id", "Доступ", 0)
                (columnUser as ColumnComboBox).addChoice(0, "общий")
                (columnUser as ColumnComboBox).addChoice(userConfig.userID, "личный")
            } else columnUser = ColumnInt(tableName, "user_id", 0)//--- во всех прочих случаях это обычное служебное/невидимое поле
            //--- обычные пользователи могут указать доступ к своему контакту только при его создании

            //--- показ имени менеджера в режиме контакта
            val selfLinkManagerTableName = "SYSTEM_users_2"
            val columnPeopleManagerID = ColumnInt(selfLinkManagerTableName, "id")
            columnPeopleManagerID.selfLinkTableName = "SYSTEM_users"
            columnPeopleManager = ColumnInt(tableName, "manager_id", columnPeopleManagerID, 0)
            columnPeopleManagerName = ColumnString(selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH)
            columnPeopleManagerName.selfLinkTableName = "SYSTEM_users"
            columnPeopleManagerName.addFormVisible(FormColumnVisibleData(columnPeopleWorkState!!, true, intArrayOf(WORK_STATE_IN_WORK)))
            columnPeopleManagerName.formPinMode = FormPinMode.OFF
        }
        //--- в режиме работы с клиентом всегда можно переназначить клиента другому менеджеру
        else {
            columnUser_ = ColumnInt(tableName, "user_id", 0)

            val selfLinkUserTableName = "SYSTEM_users_1"
            val columnUserID = ColumnInt(selfLinkUserTableName, "id")
            columnUserID.selfLinkTableName = "SYSTEM_users"
            //--- в режиме клиента вместо user_id регулятором прав доступа работает manager_id
            columnUser = ColumnInt(tableName, "manager_id", columnUserID, userConfig.userID)
            columnUserName = ColumnString(selfLinkUserTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH)
            columnUserName.selfLinkTableName = "SYSTEM_users"
            //columnUserName.setRequired( true ); - может быть ничья/общая
            columnUserName.selectorAlias = "system_user_people"
            columnUserName.addSelectorColumn(columnUser!!, columnUserID)
            columnUserName.addSelectorColumn(columnUserName)
        }

        //---------------------------------------------------------------------------------------------------------------

        val columnCompanyID = ColumnInt("OFFICE_company", "id")
        val columnCompany = ColumnInt(tableName, "company_id", columnCompanyID)

        val columnCompanyBlackList = ColumnBoolean("OFFICE_company", "in_black_list", "В чёрном списке")
        columnCompanyBlackList.formPinMode = FormPinMode.OFF
        val columnCompanyName = ColumnString("OFFICE_company", "name", "Предприятие", STRING_COLUMN_WIDTH)
        columnCompanyName.formPinMode = FormPinMode.OFF
        val columnCompanyAddress = ColumnString("OFFICE_company", "address", "Адрес компании", 12, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnCompanyAddress.formPinMode = FormPinMode.OFF
        val columnCompanyContactInfo = ColumnString("OFFICE_company", "contact_info", "Доп. информация по компании", 10, STRING_COLUMN_WIDTH, textFieldMaxSize)
        columnCompanyContactInfo.formPinMode = FormPinMode.OFF

        val columnCityID = ColumnInt("OFFICE_city", "id")
        val columnCity = ColumnInt("OFFICE_company", "city_id", columnCityID)

        val columnCityName = ColumnString("OFFICE_city", "name", "Город", STRING_COLUMN_WIDTH)
        columnCityName.formPinMode = FormPinMode.OFF
        val columnCityPhoneCode = ColumnString("OFFICE_city", "phone_code", "Код города", STRING_COLUMN_WIDTH)

        columnCompanyName.selectorAlias = "office_company"
        columnCompanyName.addSelectorColumn(columnCompany, columnCompanyID)
        columnCompanyName.addSelectorColumn(columnCompanyBlackList)
        columnCompanyName.addSelectorColumn(columnCompanyName)
        columnCompanyName.addSelectorColumn(columnCompanyAddress)
        columnCompanyName.addSelectorColumn(columnCompanyContactInfo)
        columnCompanyName.addSelectorColumn(columnCityName)
        columnCompanyName.addSelectorColumn(columnCityPhoneCode)

        val columnPeopleName = ColumnString(tableName, "name", "Ф.И.О.", STRING_COLUMN_WIDTH)
        val columnPeoplePost = ColumnString(tableName, "post", "Должность", STRING_COLUMN_WIDTH)
        val columnPeopleEmail = ColumnString(tableName, "e_mail", "E-mail", STRING_COLUMN_WIDTH)
        val columnPeopleCell = ColumnString(tableName, "cell_no", "Мобильный телефон", STRING_COLUMN_WIDTH)
        val columnPeoplePhone = ColumnString(tableName, "phone_no", "Рабочий телефон", STRING_COLUMN_WIDTH)
        val columnPeopleFax = ColumnString(tableName, "fax_no", "Факс", STRING_COLUMN_WIDTH)
        val columnPeopleAssistant = ColumnString(tableName, "assistant_name", "Помощник", STRING_COLUMN_WIDTH)
        val columnPeopleAssistantEmail = ColumnString(tableName, "assistant_mail", "E-mail помощника", STRING_COLUMN_WIDTH)
        val columnPeopleAssistantCell = ColumnString(tableName, "assistant_cell", "Телефон помощника", STRING_COLUMN_WIDTH)
        val columnPeopleContactInfo = ColumnString(tableName, "contact_info", "Доп. информация по контакту", 10, STRING_COLUMN_WIDTH, textFieldMaxSize)
        val columnPeopleBirthDate = ColumnDate3Int(tableName, "birth_ye", "birth_mo", "birth_da", "День рождения")

        val columnFile = ColumnFile(tableName, "file_id", "Файлы")

        //---------------------------------------------------------------------------------------------------------------

        val columnBusiness = ColumnComboBox(tableName, "business_id", "Направление деятельности", 0)
        val rs = stm.executeQuery(" SELECT id , name FROM OFFICE_business ")
        while(rs.next()) columnBusiness.addChoice(rs.getInt(1), rs.getString(2))
        rs.close()

        val columnClientActionDate = ColumnDate3Int(tableName, "action_ye", "action_mo", "action_da", "Дата последнего действия")
        columnClientActionDate.isEditable = false
        val columnClientActionTime = ColumnTime3Int(tableName, "action_ho", "action_mi", null, "Время последнего действия")
        columnClientActionTime.isEditable = false

        columnClientPlanDate = ColumnDate3Int(tableName, "plan_ye", "plan_mo", "plan_da", "Дата следующего действия")
        columnClientPlanDate.isEditable = false
        columnClientPlanDate.formPinMode = FormPinMode.OFF
        columnClientPlanTime = ColumnTime3Int(tableName, "plan_ho", "plan_mi", null, "Время следующего действия")
        columnClientPlanTime.isEditable = false

        //---------------------------------------------------------------------------------------------------------------

        alTableHiddenColumn.add(columnID!!)
        alTableHiddenColumn.add(columnUser!!)
        if(!isPeople) alTableHiddenColumn.add(columnUser_!!)
        if(isPeople) alTableHiddenColumn.add(columnPeopleManager!!)
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

        if(aliasConfig.alias == "office_client_in_work") {
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
        if(isPeople) {
//            addTableColumn(columnPeopleManagerName!!)
        }
//        addTableColumn(columnBusiness)
        if(isPeople) {
            alTableHiddenColumn.add(columnClientActionDate)
            alTableHiddenColumn.add(columnClientActionTime)
        } else {
            addTableColumn(columnClientActionDate)
            addTableColumn(columnClientActionTime)
        }
        addTableColumn(columnFile)


        alFormHiddenColumn.add(columnID!!)
        if(!isPeople) alFormHiddenColumn.add(columnUser_!!)
        if(isPeople) alFormHiddenColumn.add(columnPeopleManager!!)
        alFormHiddenColumn.add(columnCompany)
        alFormHiddenColumn.add(columnCity)

        (if(!isPeople || userConfig.isAdmin || id != 0) alFormHiddenColumn else alFormColumn).add(columnUser!!)
        if(!isPeople || userConfig.isAdmin) {
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
        (if(isPeople) alFormHiddenColumn else alFormColumn).add(columnClientActionDate)
        (if(isPeople) alFormHiddenColumn else alFormColumn).add(columnClientActionTime)
        (if(isPeople) alFormHiddenColumn else alFormColumn).add(columnClientPlanDate)
        (if(isPeople) alFormHiddenColumn else alFormColumn).add(columnClientPlanTime)
        alFormColumn.add(columnFile)

        //---------------------------------------------------------------------------------------------------------------

        //--- поля для сортировки
        if(!isPeople) {
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

        if(isPeople) {
            alChildData.add(ChildData("office_reminder", columnID!!, true))
            alChildData.add(ChildData("office_reminder_call", columnID!!))
            alChildData.add(ChildData("office_reminder_meet", columnID!!))
            alChildData.add(ChildData("office_reminder_call_remember", columnID!!))
            alChildData.add(ChildData("office_reminder_input_call", columnID!!))
            //alChildData.add( new ChildData( "office_reminder_meeting" , columnID ) ); - неприменимо для совещаний
            alChildData.add(ChildData("office_reminder_other", columnID!!))
            alChildData.add(ChildData("office_client_work_view", columnID!!, true))
        } else {
            alChildData.add(ChildData("office_client_work_history", columnID!!, true, true))
        }

        //----------------------------------------------------------------------------------------

        alDependData.add(DependData("OFFICE_reminder", "people_id"))
        alDependData.add(DependData("OFFICE_client_work", "client_id"))
    }
}

//package foatto.office;
//
//import foatto.core_client.app.CoreFormCellInfo;
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.FormColumnVisibleData;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.app.server.column.*;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mReminder extends mAbstract {
//
//    public static final String ALERT_TAG = "reminder";
//
//    //--- служебный тип для обозначения отсутствия необходимости в напоминании
//    public static final int REMINDER_TYPE_NO_REMINDER = -1;
//
//    public static final int REMINDER_TYPE_OTHER = 0;
//    public static final int REMINDER_TYPE_CALL = 1;
//    public static final int REMINDER_TYPE_MEET = 2;
//    public static final int REMINDER_TYPE_CALL_REMEMBER = 3;
//    public static final int REMINDER_TYPE_INPUT_CALL = 4;
//    public static final int REMINDER_TYPE_MEETING = 5;
//
//    public static final HashMap<Integer,String> hmReminderName = new HashMap<>();
//    static {
//        hmReminderName.put( REMINDER_TYPE_NO_REMINDER, "(нет)" );
//
//        hmReminderName.put( REMINDER_TYPE_CALL, "Позвонить" );
//        hmReminderName.put( REMINDER_TYPE_MEET, "Назначить встречу" );
//        hmReminderName.put( REMINDER_TYPE_CALL_REMEMBER, "Напомнить о звонке" );
//        hmReminderName.put( REMINDER_TYPE_INPUT_CALL, "Входящие звонки" );
//        hmReminderName.put( REMINDER_TYPE_MEETING, "Совещания" );
//        hmReminderName.put( REMINDER_TYPE_OTHER, "Прочие напоминания" );
//    }
//
//    private int type = REMINDER_TYPE_NO_REMINDER;
//
//    private ColumnBoolean columnArchive = null;
//    private ColumnRadioButton columnType = null;
//    private ColumnDate columnDate = null;
//    private ColumnTime columnTime = null;
//    private ColumnComboBox columnAlertTime = null;
//    private ColumnString columnSubj = null;
//
////----------------------------------------------------------------------------------------------------------------------
//
//    //--- оповещать при добавлении или изменении напоминания
//    public String getAddAlertTag() { return ALERT_TAG; }
//    public String getEditAlertTag() { return ALERT_TAG; }
//    //--- прежнее (устаревшее) оповещение должно быть удалено
//    public boolean isUniqueAlertRowID() { return true; }
//    public iColumn[] getDateTimeColumns() { return new iColumn[] { columnAlertTime, columnDate, columnTime }; }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        if( aliasConfig.getAlias().equals( "office_reminder" ) ) type = -1;            // все напоминания одним списком
//        else if( aliasConfig.getAlias().equals( "office_reminder_call" ) ) type = REMINDER_TYPE_CALL;
//        else if( aliasConfig.getAlias().equals( "office_reminder_meet" ) ) type = REMINDER_TYPE_MEET;
//        else if( aliasConfig.getAlias().equals( "office_reminder_call_remember" ) ) type = REMINDER_TYPE_CALL_REMEMBER;
//        else if( aliasConfig.getAlias().equals( "office_reminder_input_call" ) ) type = REMINDER_TYPE_INPUT_CALL;
//        else if( aliasConfig.getAlias().equals( "office_reminder_meeting" ) ) type = REMINDER_TYPE_MEETING;
//        else if( aliasConfig.getAlias().equals( "office_reminder_other" ) ) type = REMINDER_TYPE_OTHER;
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_reminder";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//        columnUser = new ColumnInt( tableName, "user_id", userConfig.getUserID() );
//
//        columnArchive = new ColumnBoolean( tableName, "in_archive", "В архиве" );
//
//        columnType = new ColumnRadioButton( tableName, "type", "Тип", type < 0 ? 0 : type );
//            columnType.addChoice( REMINDER_TYPE_CALL, hmReminderName.get( REMINDER_TYPE_CALL ) );
//            columnType.addChoice( REMINDER_TYPE_MEET, hmReminderName.get( REMINDER_TYPE_MEET ) );
//            columnType.addChoice( REMINDER_TYPE_CALL_REMEMBER, hmReminderName.get( REMINDER_TYPE_CALL_REMEMBER ) );
//            columnType.addChoice( REMINDER_TYPE_INPUT_CALL, hmReminderName.get( REMINDER_TYPE_INPUT_CALL ) );
//            columnType.addChoice( REMINDER_TYPE_MEETING, hmReminderName.get( REMINDER_TYPE_MEETING ) );
//            columnType.addChoice( REMINDER_TYPE_OTHER, hmReminderName.get( REMINDER_TYPE_OTHER ) );
//
//        columnDate = new ColumnDate( tableName, "ye", "mo", "da", "Дата", 2000, 2030, timeZone );
//        columnTime = new ColumnTime( tableName, "ho", "mi", "Время", timeZone );
//        columnAlertTime = new ColumnComboBox( tableName, "alert_time", "Оповещение", -1 );
//            columnAlertTime.addChoice(      -1, "не оповещать" );
//            columnAlertTime.addChoice(       0, "в указанное время" );
//            columnAlertTime.addChoice(  5 * 60, "за 5 минут" );
//            columnAlertTime.addChoice( 15 * 60, "за 15 минут" );
//            columnAlertTime.addChoice( 60 * 60, "за час" );
//
//        columnSubj = new ColumnString( tableName, "subj", "Тема", 4, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//        ColumnString columnDescr = new ColumnString( tableName, "descr", "Примечания",
//                                                     4, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//
//            ColumnInt columnPeopleID = new ColumnInt( "OFFICE_people", "id" );
//        ColumnInt columnPeople = new ColumnInt( tableName, "people_id", columnPeopleID );
//
//        ColumnString columnPeopleName = new ColumnString( "OFFICE_people", "name", "Ф.И.О.", STRING_COLUMN_WIDTH );
//            columnPeopleName.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeoplePost = new ColumnString( "OFFICE_people", "post", "Должность", STRING_COLUMN_WIDTH );
//            columnPeoplePost.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleEmail = new ColumnString( "OFFICE_people", "e_mail", "E-mail", STRING_COLUMN_WIDTH );
//            columnPeopleEmail.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleCell = new ColumnString( "OFFICE_people", "cell_no", "Мобильный телефон", STRING_COLUMN_WIDTH );
//            columnPeopleCell.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeoplePhone = new ColumnString( "OFFICE_people", "phone_no", "Рабочий телефон", STRING_COLUMN_WIDTH );
//            columnPeoplePhone.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleFax = new ColumnString( "OFFICE_people", "fax_no", "Факс", STRING_COLUMN_WIDTH );
//            columnPeopleFax.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleAssistant = new ColumnString( "OFFICE_people", "assistant_name", "Помощник", STRING_COLUMN_WIDTH );
//            columnPeopleAssistant.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnPeopleAssistant.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnString columnPeopleAssistantEmail = new ColumnString( "OFFICE_people", "assistant_mail", "E-mail помощника", STRING_COLUMN_WIDTH );
//            columnPeopleAssistantEmail.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleAssistantCell = new ColumnString( "OFFICE_people", "assistant_cell", "Телефон помощника", STRING_COLUMN_WIDTH );
//            columnPeopleAssistantCell.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//        ColumnString columnPeopleContactInfo = new ColumnString( "OFFICE_people", "contact_info", "Доп. информация по контакту",
//                                                                 6, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnPeopleContactInfo.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnPeopleContactInfo.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnDate columnPeopleBirthDate = new ColumnDate( "OFFICE_people", "birth_ye", "birth_mo", "birth_da", "День рождения", 1800, 2030, timeZone );
//            columnPeopleBirthDate.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnPeopleBirthDate.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//
//        ColumnComboBox columnPeopleWorkState = new ColumnComboBox( "OFFICE_people", "work_state", "Состояние работы с клиентом",
//                                                                   mPeople.WORK_STATE_NOT_NEED );
//            columnPeopleWorkState.addChoice( mPeople.WORK_STATE_NOT_NEED, "(нет)" );
//            columnPeopleWorkState.addChoice( mPeople.WORK_STATE_IN_WORK, "В работе" );
//            columnPeopleWorkState.addChoice( mPeople.WORK_STATE_OUT_WORK, "Отработан" );
//            columnPeopleWorkState.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnPeopleWorkState.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//
//        //--- хоть здесь и нет применения "предыдущего" SYSTEM_users_1,
//        //--- но очень желательно применять псевдоимена таблиц такие же, как в исходном OFFICE_people
//        String selfLinkManagerTableName = "SYSTEM_users_2";
//            ColumnInt columnPeopleManagerID = new ColumnInt( selfLinkManagerTableName, "id" );
//                columnPeopleManagerID.setSelfLinkTableName( "SYSTEM_users" );
//        ColumnInt columnPeopleManager = new ColumnInt( "OFFICE_people", "manager_id", columnPeopleManagerID, 0 );
//        ColumnString columnPeopleManagerName = new ColumnString( selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH );
//            columnPeopleManagerName.setSelfLinkTableName( "SYSTEM_users" );
//            columnPeopleManagerName.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnPeopleManagerName.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        //--- отдельное служебное поле для копирования селектора из mPeople, т.к. там оно в виде SYSTEM_users_2.full_name,
//        //--- уже без указания setSelfLinkTableName( "SYSTEM_users" );
//        ColumnString columnPeopleManagerName_ = new ColumnString( selfLinkManagerTableName, "full_name", "Менеджер", STRING_COLUMN_WIDTH );
//
//            ColumnInt columnCompanyID = new ColumnInt( "OFFICE_company", "id" );
//        ColumnInt columnCompany = new ColumnInt( "OFFICE_people", "company_id", columnCompanyID );
//
//        ColumnBoolean columnCompanyBlackList = new ColumnBoolean( "OFFICE_company", "in_black_list", "В чёрном списке" );
//            columnCompanyBlackList.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnCompanyBlackList.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnString columnCompanyName = new ColumnString( "OFFICE_company", "name", "Предприятие", STRING_COLUMN_WIDTH );
//            columnCompanyName.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnCompanyName.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnString columnCompanyAddress = new ColumnString( "OFFICE_company", "address", "Адрес компании",
//                                                              6, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnCompanyAddress.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnCompanyAddress.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnString columnCompanyContactInfo = new ColumnString( "OFFICE_company", "contact_info", "Доп. информация по компании",
//                                                                  6, STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnCompanyContactInfo.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnCompanyContactInfo.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//
//            ColumnInt columnCityID = new ColumnInt( "OFFICE_city", "id" );
//        ColumnInt columnCity = new ColumnInt( "OFFICE_company", "city_id", columnCityID );
//
//        ColumnString columnCityName = new ColumnString( "OFFICE_city", "name", "Город", STRING_COLUMN_WIDTH );
//            columnCityName.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//            columnCityName.setFormPinMode( CoreFormCellInfo.FORM_PIN_OFF );
//        ColumnString columnPhoneCode = new ColumnString( "OFFICE_city", "phone_code", "Код города", STRING_COLUMN_WIDTH );
//            columnPhoneCode.addFormVisible( new FormColumnVisibleData( columnType, false, new int[] { REMINDER_TYPE_MEETING } ) );
//
//            columnPeopleName.setSelectorAlias( "office_people" );
//            columnPeopleName.addSelectorColumn( columnPeople, columnPeopleID );
//            columnPeopleName.addSelectorColumn( columnPeopleName );
//            columnPeopleName.addSelectorColumn( columnPeoplePost );
//            columnPeopleName.addSelectorColumn( columnPeopleEmail );
//            columnPeopleName.addSelectorColumn( columnPeopleCell );
//            columnPeopleName.addSelectorColumn( columnPeoplePhone );
//            columnPeopleName.addSelectorColumn( columnPeopleFax );
//            columnPeopleName.addSelectorColumn( columnPeopleAssistant );
//            columnPeopleName.addSelectorColumn( columnPeopleAssistantEmail );
//            columnPeopleName.addSelectorColumn( columnPeopleAssistantCell );
//            columnPeopleName.addSelectorColumn( columnPeopleContactInfo );
//            columnPeopleName.addSelectorColumn( columnPeopleBirthDate );
//            columnPeopleName.addSelectorColumn( columnPeopleWorkState );
//            columnPeopleName.addSelectorColumn( columnPeopleManagerName, columnPeopleManagerName_ );
//            columnPeopleName.addSelectorColumn( columnCompanyBlackList );
//            columnPeopleName.addSelectorColumn( columnCompanyName );
//            columnPeopleName.addSelectorColumn( columnCompanyAddress );
//            columnPeopleName.addSelectorColumn( columnCompanyContactInfo );
//            columnPeopleName.addSelectorColumn( columnCityName );
//            columnPeopleName.addSelectorColumn( columnPhoneCode );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnUser );
//        alTableHiddenColumn.add( columnPeople );
//        alTableHiddenColumn.add( columnPeopleManager );
//        alTableHiddenColumn.add( columnCompany );
//        alTableHiddenColumn.add( columnCity );
//        alTableHiddenColumn.add( columnArchive );
//
//        alTableGroupColumn.add( columnDate );
//        alTableGroupColumn.add( columnTime );
//
//        if( type < 0 ) addTableColumn( columnType );
//        else alTableHiddenColumn.add( columnType );
//        addTableColumn( columnSubj );
//        addTableColumn( columnDescr );
//
//        if( type == REMINDER_TYPE_MEETING ) {
//            alTableHiddenColumn.add( columnPeopleName );
//            alTableHiddenColumn.add( columnPeoplePost );
//            alTableHiddenColumn.add( columnCompanyBlackList );
//            alTableHiddenColumn.add( columnCompanyName );
//            alTableHiddenColumn.add( columnCityName );
//            alTableHiddenColumn.add( columnPeopleWorkState );
//            alTableHiddenColumn.add( columnPeopleManagerName );
//        }
//        else {
//            addTableColumn( columnPeopleName );
//            addTableColumn( columnPeoplePost );
//            addTableColumn( columnCompanyBlackList );
//            addTableColumn( columnCompanyName );
//            addTableColumn( columnCityName );
//            addTableColumn( columnPeopleWorkState );
//            addTableColumn( columnPeopleManagerName );
//        }
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnUser );
//        alFormHiddenColumn.add( columnPeople );
//        alFormHiddenColumn.add( columnPeopleManager );
//        alFormHiddenColumn.add( columnCompany );
//        alFormHiddenColumn.add( columnCity );
//
//        alFormColumn.add( columnType );
//        alFormColumn.add( columnDate );
//        alFormColumn.add( columnTime );
//        alFormHiddenColumn.add( columnAlertTime );
//        alFormColumn.add( columnSubj );
//        alFormColumn.add( columnDescr );
//        alFormColumn.add( columnPeopleName );
//        alFormColumn.add( columnPeoplePost );
//        alFormColumn.add( columnPeopleEmail );
//        alFormColumn.add( columnPeopleCell );
//        alFormColumn.add( columnPeoplePhone );
//        alFormColumn.add( columnPeopleFax );
//        alFormColumn.add( columnPeopleAssistant );
//        alFormColumn.add( columnPeopleAssistantEmail );
//        alFormColumn.add( columnPeopleAssistantCell );
//        alFormColumn.add( columnPeopleContactInfo );
//        alFormColumn.add( columnPeopleBirthDate );
//        alFormColumn.add( columnPeopleWorkState );
//        alFormColumn.add( columnPeopleManagerName );
//        alFormColumn.add( columnCompanyBlackList );
//        alFormColumn.add( columnCompanyName );
//        alFormColumn.add( columnCompanyAddress );
//        alFormColumn.add( columnCompanyContactInfo );
//        alFormColumn.add( columnCityName );
//        alFormColumn.add( columnPhoneCode );
//        alFormColumn.add( columnArchive );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnArchive );
//            alTableSortDirect.add( "ASC" );
//        alTableSortColumn.add( columnDate );
//            alTableSortDirect.add( "ASC" );
//        alTableSortColumn.add( columnTime );
//            alTableSortDirect.add( "ASC" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        hmParentColumn.put( "system_user", columnUser );
//
//        hmParentColumn.put( "office_city", columnCity );
//        hmParentColumn.put( "office_company", columnCompany );
//        hmParentColumn.put( "office_people", columnPeople );
//    }
//
////----------------------------------------------------------------------------------------------------------------------
//
//    public int getType() { return type; }
//
//    public ColumnBoolean getColumnArchive() { return columnArchive; }
//    public ColumnRadioButton getColumnType() { return columnType; }
//    public ColumnDate getColumnDate() { return columnDate; }
//    public ColumnTime getColumnTime() { return columnTime; }
//    public ColumnString getColumnSubj() { return columnSubj; }
//}

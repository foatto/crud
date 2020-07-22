//package foatto.office;
//
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.app.server.column.*;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//
//import java.util.HashMap;
//
//public class mClientWork extends mAbstract {
//
//    private ColumnInt columnClient = null;
//    private ColumnDate columnActionDate = null;
//    private ColumnTime columnActionTime = null;
//    private ColumnString columnActionDescr = null;
//    private ColumnString columnActionResult = null;
//    private ColumnDate columnPlanDate = null;
//    private ColumnTime columnPlanTime = null;
//    private ColumnString columnPlanAction = null;
//    private ColumnComboBox columnReminderType = null;
//
//    public void init( CoreDataServer dataServer, CoreDataWorker dataWorker, AliasConfig aliasConfig, UserConfig userConfig,
//                      HashMap<String,Integer> hmParentData, int id, boolean isOldVersion ) {
//
//        super.init( dataServer, dataWorker, aliasConfig, userConfig, hmParentData, id, isOldVersion );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        boolean isCompanyParent = hmParentData.get( "office_company" ) != null;
//
//        Integer parentID = hmParentData.get( "office_people" );
//        if( parentID == null ) parentID = hmParentData.get( "office_client_not_need" );
//        if( parentID == null ) parentID = hmParentData.get( "office_client_in_work" );
//        if( parentID == null ) parentID = hmParentData.get( "office_client_out_work" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = "OFFICE_client_work";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//        columnUser = new ColumnInt( tableName, "user_id", userConfig.getUserID() );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        ColumnString columnClientName = null;
//        ColumnString columnClientPost = null;
//        ColumnInt columnCompany = null;
//        //--- если это переход от компании, надо показать историю работы по всему предприятию
//        if( isCompanyParent ) {
//                ColumnInt columnClientID = new ColumnInt( "OFFICE_people", "id" );
//            columnClient = new ColumnInt( tableName, "client_id", columnClientID );
//
//            columnClientName = new ColumnString( "OFFICE_people", "name", "Контактное лицо", STRING_COLUMN_WIDTH );
//            columnClientPost = new ColumnString( "OFFICE_people", "post", "Должность", STRING_COLUMN_WIDTH );
//
//            columnCompany = new ColumnInt( "OFFICE_people", "company_id" );
//        }
//        else columnClient = new ColumnInt( tableName, "client_id", parentID );
//
//        columnActionDate = new ColumnDate( tableName, "action_ye", "action_mo", "action_da",
//                                           "Дата последнего действия", 2010, 2100, timeZone );
//        columnActionTime = new ColumnTime( tableName, "action_ho", "action_mi", "Время последнего действия", timeZone );
//        columnActionDescr = new ColumnString( tableName, "action_descr", "Последнее действие", 12,
//                                                           STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnActionDescr.setRequired( true );
//        columnActionResult = new ColumnString( tableName, "action_result", "Результат", 12,
//                                                            STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnActionResult.setRequired( true );
//
//        columnPlanDate = new ColumnDate( tableName, "plan_ye", "plan_mo", "plan_da",
//                                         "Дата планируемого действия", 2010, 2100, timeZone );
//        columnPlanTime = new ColumnTime( tableName, "plan_ho", "plan_mi", "Время планируемого действия", timeZone );
//        columnPlanAction = new ColumnString( tableName, "plan_action", "Планируемое действие", 12,
//                                                           STRING_COLUMN_WIDTH, false, textFieldMaxSize );
//            columnPlanAction.setRequired( true );
//
//        columnReminderType = new ColumnComboBox( tableName, "_reminder_type", "Установить напоминание", mReminder.REMINDER_TYPE_NO_REMINDER );
//            columnReminderType.setVirtual( true );
//            //--- не все виды напоминаний здесь уместны
//            columnReminderType.addChoice( mReminder.REMINDER_TYPE_NO_REMINDER, mReminder.hmReminderName.get( mReminder.REMINDER_TYPE_NO_REMINDER ) );
//            columnReminderType.addChoice( mReminder.REMINDER_TYPE_CALL, mReminder.hmReminderName.get( mReminder.REMINDER_TYPE_CALL ) );
//            columnReminderType.addChoice( mReminder.REMINDER_TYPE_MEET, mReminder.hmReminderName.get( mReminder.REMINDER_TYPE_MEET ) );
//            columnReminderType.addChoice( mReminder. REMINDER_TYPE_OTHER, mReminder.hmReminderName.get( mReminder.REMINDER_TYPE_OTHER ) );
//
//        ColumnFile columnFile = new ColumnFile( tableName, "file_id", "Файлы" );
//
////----------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnUser );
//        if( columnCompany != null ) alTableHiddenColumn.add( columnCompany );
//        alTableHiddenColumn.add( columnClient );
//
//        addTableColumn( columnActionDate );
//        addTableColumn( columnActionTime );
//        if( columnClientName != null ) addTableColumn( columnClientName );
//        if( columnClientPost != null ) addTableColumn( columnClientPost );
//        addTableColumn( columnActionDescr );
//        addTableColumn( columnActionResult );
//        addTableColumn( columnPlanDate );
//        addTableColumn( columnPlanTime );
//        addTableColumn( columnPlanAction );
//        addTableColumn( columnFile );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnUser );
//        alFormHiddenColumn.add( columnClient );
//        alFormHiddenColumn.add( columnActionDate );
//        alFormHiddenColumn.add( columnActionTime );
//
//        alFormColumn.add( columnActionDescr );
//        alFormColumn.add( columnActionResult );
//        alFormColumn.add( columnPlanDate );
//        alFormColumn.add( columnPlanTime );
//        alFormColumn.add( columnPlanAction );
//        alFormColumn.add( columnReminderType );
//        alFormColumn.add( columnFile );
//
////----------------------------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnActionDate );
//            alTableSortDirect.add( "DESC" );
//        alTableSortColumn.add( columnActionTime );
//            alTableSortDirect.add( "DESC" );
//
////----------------------------------------------------------------------------------------
//
//        hmParentColumn.put( "system_user", columnUser );
//        hmParentColumn.put( "office_people", columnClient );
//        hmParentColumn.put( "office_client_not_need", columnClient );
//        hmParentColumn.put( "office_client_in_work", columnClient );
//        hmParentColumn.put( "office_client_out_work", columnClient );
//        //--- поле columnCompany будет проинициализировано только при переходе с компании
//        if( isCompanyParent )
//            hmParentColumn.put( "office_company", columnCompany );
//    }
//
//    public ColumnInt getColumnClient() { return columnClient; }
//    public ColumnDate getColumnActionDate() { return columnActionDate; }
//    public ColumnTime getColumnActionTime() { return columnActionTime; }
//    public ColumnString getColumnActionDescr() { return columnActionDescr; }
//    public ColumnString getColumnActionResult() { return columnActionResult; }
//    public ColumnDate getColumnPlanDate() { return columnPlanDate; }
//    public ColumnTime getColumnPlanTime() { return columnPlanTime; }
//    public ColumnString getColumnPlanAction() { return columnPlanAction; }
//    public ColumnComboBox getColumnReminderType() { return columnReminderType; }
//}
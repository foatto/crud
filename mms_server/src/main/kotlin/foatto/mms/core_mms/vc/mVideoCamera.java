//package foatto.mms.core_mms.vc;
//
//import foatto.app.CoreSpringController;
//import foatto.core_server.app.server.AliasConfig;
//import foatto.core_server.app.server.FormColumnVisibleData;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.server.column.ColumnBoolean;
//import foatto.core_server.app.server.column.ColumnComboBox;
//import foatto.core_server.app.server.column.ColumnInt;
//import foatto.core_server.app.server.column.ColumnString;
//import foatto.core_server.app.server.mAbstract;
//import foatto.core_server.app.video.server.CameraModelData;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//import foatto.mms.core_mms.ObjectSelector;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.TreeMap;
//
//public class mVideoCamera extends mAbstract {
//
//    private static final String[] arrStreamDescr = { "URL основного видеопотока", "URL дополнительного видеопотока" };
//
//    private ObjectSelector os = null;
//
////    private ColumnComboBox columnOrderNo = null;
//    private ColumnString columnDescr = null;
//    private ColumnString columnLogin = null;
//    private ColumnString columnPassword = null;
//    private ColumnComboBox columnModel = null;
//    private ColumnString columnIP = null;
//    private ColumnString[] arrColumnURL = new ColumnString[ VideoFunction.VIDEO_STREAM_COUNT ];
//    private ColumnString columnURLMJpeg = null;
//    private ColumnString columnURLImage = null;
//    private ColumnString columnURLTime = null;
//    private ColumnComboBox columnVideoCodec = null;
//    private ColumnComboBox columnAudioCodec = null;
//    private ColumnInt columnDuration = null;
//
//    public void init(CoreSpringController appController, AliasConfig aliasConfig, UserConfig userConfig,
//                     Map<String, Integer> hmParentData, int id) {
//
//        super.init(appController, aliasConfig, userConfig, hmParentData, id);
//
////        //--- этот метод может быть запущен из "Модули системы", безо всяких parent data
////        Integer objectId = hmParentData.get( "mms_object" );
////        AutoConfig ac = autoID == null ? null : AutoConfig.getAutoConfig( conn, userConfig, autoID );
//
//        TreeMap<Integer,CameraModelData> tmCMD = VideoFunction.loadCameraModelData( appController.hmConfig );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        tableName = "VC_camera";
//
////----------------------------------------------------------------------------------------------------------------------
//
//        columnID = new ColumnInt( tableName, "id" );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        //--- скрытое поле для автоматической генерации камер для сервера приёма снимков с ImageSender'a
//        ColumnString columnName = new ColumnString( tableName, "name", "name", STRING_COLUMN_WIDTH );
//
////        columnOrderNo = new ColumnComboBox( tableName, "_order_no", "Порядковый номер", 0 );
////            columnModel.setVirtual( true );
////            columnOrderNo.addChoice( 0, "(вручную)" );
////            for( int i = 1; i < 20; i++ )
////                columnOrderNo.addChoice( i, Integer.toString( i ) );
//
//        columnDescr = new ColumnString( tableName, "descr", "Описание", STRING_COLUMN_WIDTH );
//            columnDescr.setRequired( true );
////            //--- если задан номер камеры, то описание будет генерироваться автоматически по шаблону Cam-NNN
////            columnDescr.addFormVisible( new FormColumnVisibleData( columnOrderNo, true, new int[] { 0 } ) );
//
//        columnLogin = new ColumnString( tableName, "login", "Логин", STRING_COLUMN_WIDTH );
//        columnPassword = new ColumnString( tableName, "pwd", "Пароль", STRING_COLUMN_WIDTH );
//
//        columnModel = new ColumnComboBox( tableName, "_model_id", "Модель", -1 );
//            columnModel.setVirtual( true );
//            columnModel.addChoice( -1, "(другая)" );
//            for( Integer mID : tmCMD.keySet() )
//                columnModel.addChoice( mID, tmCMD.get( mID ).name );
//
//        columnIP = new ColumnString( tableName, "_ip", "IP-адрес:", STRING_COLUMN_WIDTH );
//            columnIP.setVirtual( true );
////            columnIP.addFormVisible( new FormColumnVisibleData( columnOrderNo, true, new int[] { 0 } ) );
//            columnIP.addFormVisible( new FormColumnVisibleData( columnModel, false, new int[] { -1 } ) );
//
//        for( int i = 0; i < VideoFunction.VIDEO_STREAM_COUNT; i++ ) {
//            arrColumnURL[ i ] = new ColumnString( tableName, "url_" + i, arrStreamDescr[ i ], STRING_COLUMN_WIDTH );
//            //--- обязателен только основной поток
//            //arrColumnURL[ i ].setRequired( i == 0 ); - пересекается с автозаполнением при выборе "типовой" камеры
//            arrColumnURL[ i ].addFormVisible( new FormColumnVisibleData( columnModel, true, new int[] { -1 } ) );
//        }
//
//        columnURLMJpeg = new ColumnString( tableName, "url_mjpeg", "URL mjpeg-потока", STRING_COLUMN_WIDTH );
//            columnURLMJpeg.addFormVisible( new FormColumnVisibleData( columnModel, true, new int[] { -1 } ) );
//        columnURLImage = new ColumnString( tableName, "url_image", "URL одиночных кадров", STRING_COLUMN_WIDTH );
//            columnURLImage.addFormVisible( new FormColumnVisibleData( columnModel, true, new int[] { -1 } ) );
//        columnURLTime = new ColumnString( tableName, "url_time", "URL установки времени", STRING_COLUMN_WIDTH );
//            columnURLTime.setMaxSize( 1000 );
//            columnURLTime.addFormVisible( new FormColumnVisibleData( columnModel, true, new int[] { -1 } ) );
//
//        columnVideoCodec = new ColumnComboBox( tableName, "video_codec", "Видео-кодек", VideoFunction.VC_COPY );
//            columnVideoCodec.addChoice( VideoFunction.VC_COPY, "(исх.)" );
//            columnVideoCodec.addChoice( VideoFunction.VC_H264, "H.264" );
//
//        columnAudioCodec = new ColumnComboBox( tableName, "audio_codec", "Аудио-кодек", VideoFunction.AC_NONE );
//            columnAudioCodec.addChoice( VideoFunction.AC_NONE, "(выкл.)" );
//            columnAudioCodec.addChoice( VideoFunction.AC_COPY, "(исх.)" );
//            columnAudioCodec.addChoice( VideoFunction.AC_AAC, "AAC" );
//
//        columnDuration = new ColumnInt( tableName, "duration", "Продолжительность нарезки [сек]", 10, 300 );
//            columnDuration.setMinValue( 5 );
//            columnDuration.setMaxValue( 3600 );
//
////---------------------------------------------------------------------------------------------------------------
//
//        alTableHiddenColumn.add( columnID );
//        alTableHiddenColumn.add( columnName );
////        alTableHiddenColumn.add( columnOrderNo );
//        alTableHiddenColumn.add( columnModel );
//        alTableHiddenColumn.add( columnIP );
//
//        addTableColumn( columnDescr );
//        addTableColumn( columnLogin );
//        addTableColumn( columnPassword );
//        for( ColumnString csURL : arrColumnURL ) addTableColumn( csURL );
//        addTableColumn( columnURLMJpeg );
//        addTableColumn( columnURLImage );
//        addTableColumn( columnURLTime );
//        addTableColumn( columnVideoCodec );
//        addTableColumn( columnAudioCodec );
//        addTableColumn( columnDuration );
//
//        alFormHiddenColumn.add( columnID );
//        alFormHiddenColumn.add( columnName );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        os = new ObjectSelector();
//        os.fillColumns( this, true, true,
//                        alTableHiddenColumn, alFormHiddenColumn, alFormColumn, hmParentColumn, true, false, -1 );
//
////----------------------------------------------------------------------------------------------------------------------
//
////        alFormColumn.add( columnOrderNo );
//        alFormColumn.add( columnDescr );
//        alFormColumn.add( columnLogin );
//        alFormColumn.add( columnPassword );
//        alFormColumn.add( columnModel );
//        alFormColumn.add( columnIP );
//        for( ColumnString csURL : arrColumnURL ) alFormColumn.add( csURL );
//        alFormColumn.add( columnURLMJpeg );
//        alFormColumn.add( columnURLImage );
//        alFormColumn.add( columnURLTime );
//        alFormColumn.add( columnVideoCodec );
//        alFormColumn.add( columnAudioCodec );
//        alFormColumn.add( columnDuration );
//
////----------------------------------------------------------------------------------------------------------------------
//
//        //--- поля для сортировки
//        alTableSortColumn.add( columnDescr );
//            alTableSortDirect.add( "ASC" );
//    }
//
//    public ObjectSelector getObjectSelector() { return os; }
////    public ColumnComboBox getColumnOrderNo() { return columnOrderNo; }
//    public ColumnString getColumnDescr() { return columnDescr; }
//    public ColumnString getColumnLogin() { return columnLogin; }
//    public ColumnString getColumnPassword() { return columnPassword; }
//    public ColumnComboBox getColumnModel() { return columnModel; }
//    public ColumnString getColumnIP() { return columnIP; }
//    public ColumnString getColumnURL( int index ) { return arrColumnURL[ index ]; }
//    public ColumnString getColumnURLMJpeg() { return columnURLMJpeg; }
//    public ColumnString getColumnURLImage() { return columnURLImage; }
//    public ColumnString getColumnURLTime() { return columnURLTime; }
//    public ColumnComboBox getColumnVideoCodec() { return columnVideoCodec; }
//    public ColumnComboBox getColumnAudioCodec() { return columnAudioCodec; }
//    public ColumnInt getColumnDuration() { return columnDuration; }
//}

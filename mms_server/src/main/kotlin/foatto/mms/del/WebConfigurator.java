//package foatto.mms.del;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.nio.channels.SelectionKey;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//import foatto.core.link.ResponseCode;
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//import foatto.core_server.app.server.HTTPServer;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.video.server.CameraModelData;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.ds.CoreDataServer;
//import foatto.core_server.ds.CoreDataWorker;
//import foatto.mms.core_mms.vc.cVideoCamera;
//import foatto.util.StringFunctionJVMKt;
//
//public class WebConfigurator extends HTTPServer {
//
//    private static final String CONFIG_SERIAL_NO_INI_FILE_NAME = "serial_no_ini_file";
//    private static final String CONFIG_SERIAL_NO_COMMAND_FILE_NAME = "serial_no_command_file";
//    private static final String CONFIG_DEL_INI_FILE_NAME = "del_ask_ini_file";
//    private static final String CONFIG_STORAGE_STAT_NAME = "stat_name";
//    private static final String CONFIG_STORAGE_STAT_DIR = "stat_dir";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static final String PARAM_ALIAS = "alias";
//    private static final String PARAM_SESSION_ID = "session_id";
//    private static final String PARAM_ID = "id";
//    private static final String PARAM_LOGIN = "login";
//    private static final String PARAM_PASSWORD = "itPassword";
//    private static final String PARAM_YE = "ye";
//    private static final String PARAM_MO = "mo";
//    private static final String PARAM_DA = "da";
//    private static final String PARAM_HO = "ho";
//    private static final String PARAM_MI = "mi";
//    private static final String PARAM_SE = "se";
//    private static final String PARAM_SERIAL_NO = "serial_no";
//    private static final String PARAM_DEL_NO = "del_no";
//    private static final String PARAM_DEL_IP = "del_ip";
//    private static final String PARAM_DEL_PORT = "del_port";
//    private static final String PARAM_TOUCAN_IP_ = "toucan_ip_";
//    private static final String PARAM_TOUCAN_PORT_ = "toucan_port_";
//    private static final String PARAM_CAMERA_MODEL = "camera_model";
//
//    private static final String ALIAS_LOGON = "logon";
//    private static final String ALIAS_MAIN_MENU = "main_menu";
//    private static final String ALIAS_TIME_FORM = "time_form";
//    private static final String ALIAS_TIME_SAVE = "time_save";
//    private static final String ALIAS_VC_FORM = "vc_form";
//    private static final String ALIAS_VC_SAVE = "vc_save";
//    private static final String ALIAS_DEL_FORM = "del_form";
//    private static final String ALIAS_DEL_SAVE = "del_save";
//    private static final String ALIAS_CAMERA_LIST = "camera_list";
//    private static final String ALIAS_CAMERA_FORM = "camera_form";
//    private static final String ALIAS_CAMERA_SAVE = "camera_save";
//    private static final String ALIAS_CAMERA_DELETE = "camera_delete";
//    private static final String ALIAS_REBOOT = "reboot";
//
//    private static String[] arrTimeFieldName = { PARAM_YE, PARAM_MO, PARAM_DA, PARAM_HO, PARAM_MI, PARAM_SE };
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private int timeSystem = VideoFunction.TIME_SYSTEM_LINUX_BUILDROOT;
//    private String serialNoIniFileName = null;
//    private String serialNoCommandFileName = null;
//    private String delIniFileName = null;
//    private String delInfoFileName = null;
//    private ArrayList<String> alStatName = new ArrayList<>();
//    private ArrayList<String> alStatDir = new ArrayList<>();
//
////--- !!! временно на период перехода на Kotlin
//
//    @Override
//    protected int getStartBufSize() { return 1024; }
//    @Override
//    protected void setStartBufSize( int aI ) {}
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public void init( CoreDataServer aDataServer, SelectionKey aSelectionKey ) {
//        super.init( aDataServer, aSelectionKey );
//
//        timeSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TIME_SYSTEM ) );
//        serialNoIniFileName = dataServer.hmConfig.get( CONFIG_SERIAL_NO_INI_FILE_NAME );
//        serialNoCommandFileName = dataServer.hmConfig.get( CONFIG_SERIAL_NO_COMMAND_FILE_NAME );
//        delIniFileName = dataServer.hmConfig.get( CONFIG_DEL_INI_FILE_NAME );
//        delInfoFileName = dataServer.hmConfig.get( CoreDELAsker.CONFIG_DEL_INFO_FILE );
//
//        StringTokenizer st = new StringTokenizer( dataServer.hmConfig.get( CONFIG_STORAGE_STAT_NAME ), ", " );
//        while( st.hasMoreTokens() ) alStatName.add( st.nextToken() );
//
//        st = new StringTokenizer( dataServer.hmConfig.get( CONFIG_STORAGE_STAT_DIR ), ", " );
//        while( st.hasMoreTokens() ) alStatDir.add( st.nextToken() );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    protected StringBuilder prepareQuery( CoreDataWorker dataWorker, HashMap<String,String> hmParam ) throws Throwable {
//        StringBuilder sb = new StringBuilder();
//
//        String alias = hmParam.get( PARAM_ALIAS );
//        if( alias == null ) getLoginPage( ResponseCode.LOGON_NEED, sb );
//        else if( alias.equals( ALIAS_LOGON  ) ) {
//            //--- чтобы не переписывать/не портить стандартную функцию checkLogon (когда нибудь я сделаю правильное наследование),
//            //--- заранее создадим сессию (но пока никуда ее не записываем)
//            chmSession = new ConcurrentHashMap<>();
//            int logonResult = checkLogon( dataWorker, hmParam.get( PARAM_LOGIN ), StringFunctionJVMKt.encodePassword( hmParam.get( PARAM_PASSWORD ) ) );
//            if( logonResult == ResponseCode.LOGON_SUCCESS_BUT_OLD || logonResult == ResponseCode.LOGON_SUCCESS ) {
//                long sessionId = CommonFunction.getRandomLong();
//                dataServer.chmSessionStore.put( sessionId, chmSession );
//                //--- обновляем время обращения к сессии при каждом запросе,
//                //--- т.к. соединение может работать очень долго
//                dataServer.chmSessionTime.put( sessionId, System.currentTimeMillis() );
//
//                //--- после успешного входа грузим полную/рабочую страницу
//                getMainMenu( sessionId, sb );
//            }
//            else getLoginPage( logonResult, sb );
//        }
//        //--- на функционал, выполняемый после входа в систему, всегда требуется сессия
//        else {
//            long sessionId = 0;
//            try {
//                sessionId = Long.parseLong( hmParam.get( PARAM_SESSION_ID ) );
//            }
//            catch( Throwable t ) {}
//            chmSession = dataServer.chmSessionStore.get( sessionId );
//
//            //--- сессия не найдена, требуется вход
//            if( chmSession == null ) getLoginPage( ResponseCode.LOGON_NEED, sb );
//            else if( alias.equals( ALIAS_MAIN_MENU ) ) getMainMenu( sessionId, sb );
//            else if( alias.equals( ALIAS_TIME_FORM ) ) getTimeForm( sessionId, sb );
//            else if( alias.equals( ALIAS_TIME_SAVE ) ) {
//                setTime( dataWorker, hmParam );
//                getMainMenu( sessionId, sb );
//            }
//            else if( alias.equals( ALIAS_VC_FORM ) ) getVCForm( sessionId, sb );
//            else if( alias.equals( ALIAS_VC_SAVE ) ) {
//                setVCParam( hmParam );
//                getMainMenu( sessionId, sb );
//            }
//            else if( alias.equals( ALIAS_DEL_FORM ) ) getDelForm( sessionId, sb );
//            else if( alias.equals( ALIAS_DEL_SAVE ) ) {
//                setDelParam( hmParam );
//                getMainMenu( sessionId, sb );
//            }
//            else if( alias.equals( ALIAS_CAMERA_LIST ) ) getCameraList( dataWorker, sessionId, sb );
//            else if( alias.equals( ALIAS_CAMERA_FORM ) ) getCameraForm( dataWorker, hmParam, sessionId, null, sb );
//            else if( alias.equals( ALIAS_CAMERA_SAVE ) ) saveCamera( dataWorker, hmParam, sessionId, sb );
//            else if( alias.equals( ALIAS_CAMERA_DELETE ) ) deleteCamera( dataWorker, hmParam, sessionId, sb );
//            else if( alias.equals( ALIAS_REBOOT ) ) doReboot();
//
//            //--- обновляем время обращения к сессии при каждом запросе,
//            //--- т.к. соединение может работать очень долго
//            dataServer.chmSessionTime.put( sessionId, System.currentTimeMillis() );
//        }
//
//        return sb;
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void getLoginPage( int msgCode, StringBuilder sb ) {
//        outPageBegin( "Вход", sb );
//
//        //--- текст ошибки
//        String msgText = null;
//        switch( msgCode ) {
//        case ResponseCode.LOGON_FAILED:
//            msgText = "Неправильное имя пользователя или пароль.<br>Попробуйте ввести еще раз.";
//            break;
//        case ResponseCode.LOGON_SYSTEM_BLOCKED:
//            msgText = "Слишком много попыток неправильного входа.<br>Пользователь временно заблокирован.<br>Попробуйте войти в систему попозже.";
//            break;
//        case ResponseCode.LOGON_ADMIN_BLOCKED:
//            msgText = "Пользователь заблокирован администратором";
//            break;
//        }
//
//        outFormBegin( sb, PARAM_ALIAS, ALIAS_LOGON );
//
//        if( msgText != null && ! msgText.isEmpty() ) {
//           sb.append( "<tr><td colspan='2'>" )
//             .append( msgText )
//             .append( "</td></tr>" );
//        }
//        sb.append( "<tr>" )
//            .append( "<td>" )
//                .append( "Имя:" )
//            .append( "</td>" )
//            .append( "<td>" )
//                .append( "<input tabindex='1' name='" ).append( PARAM_LOGIN ).append( "' type='text' size='40' maxlength='250' value=''>" )
//            .append( "</td>" )
//        .append( "</tr>" )
//        .append( "<tr>" )
//            .append( "<td>" )
//            .append( "</td>" )
//            .append( "<td>" )
//                .append( "<input tabindex='2' name='" ).append( PARAM_PASSWORD ).append( "' type='itPassword' size='40' maxlength='250' value=''>" )
//            .append( "</td>" )
//        .append( "</tr>" )
//          .append( "<tr>" )
//            .append( "<td colspan='2'>" )
//                .append( "<input tabindex='5' type='submit' value='Вход'>" )
//          .append( "</td>" )
//        .append( "</tr>" );
//
//        outFormEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void getMainMenu( long sessionId, StringBuilder sb ) {
//        UserConfig userConfig = (UserConfig) chmSession.get( USER_CONFIG );
//
//        outPageBegin( "Главное меню", sb );
//        outTableBegin( sb );
//
//        outMainPageRow( sessionId, ALIAS_TIME_FORM, "Установка системного времени", sb );
//        if( userConfig.isAdmin() ) outMainPageRow( sessionId, ALIAS_VC_FORM, "Настройки регистратора", sb );
//        outMainPageRow( sessionId, ALIAS_DEL_FORM, "Настройки ДЭЛ", sb );
//        outMainPageRow( sessionId, ALIAS_CAMERA_LIST, "Настройки видеокамер", sb );
//        outMainPageRow( sessionId, ALIAS_REBOOT, "Перезагрузить регистратор", sb );
//
//        //--- статистика по дискам
//        int mb = 1024 * 1024;
//        sb.append( "<tr><td>" );
//        for( int i = 0; i < Math.min( alStatName.size(), alStatDir.size() ); i++ ) {
//            File dir = new File( alStatDir.get( i ) );
//            sb.append( alStatName.get( i ) ).append( " = " );
//            if( dir.exists() )
//                sb.append( StringFunction.getSplittedLong( dir.getFreeSpace() / mb ) ).append( " / " )
//                  .append( StringFunction.getSplittedLong( dir.getTotalSpace() / mb ) ).append( " Mb " );
//            else sb.append( "(not found)" );
//            sb.append( "<br>" );
//        }
//        sb.append( "</td></tr>" );
//
//        //--- версия программы
//        sb.append( "<tr><td>Прошивка: " )
//          .append( StringFunction.DateTime_YMDHMS( TimeZone.getDefault(),
//                                                   new File( dataServer.rootDirName, "lib/core_server.jar" ).lastModified() ) )
//          .append( "</td></tr>" );
//
//        outTableEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void getTimeForm( long sessionId, StringBuilder sb ) {
//        String[] arrLabel = { "Год:", "Месяц:", "День:", "Час:", "Минута:", "Секунда:" };
//        int[] arrDT = StringFunction.DateTime_Arr( new GregorianCalendar() );
//
//        outPageBegin( "Установка времени", sb );
//        outFormBegin( sb, PARAM_SESSION_ID, Long.toString( sessionId ), PARAM_ALIAS, ALIAS_TIME_SAVE );
//
//        int tabIndex = 1;
//        for( int i = 0; i < arrLabel.length; i++ ) {
//            sb.append( "<tr>" )
//                .append( "<td>" )
//                    .append( arrLabel[ i ] )
//                .append( "</td>" )
//                .append( "<td>" )
//                    .append( "<input tabindex='" ).append( tabIndex++ ).append( "' name='" )
//                    .append( arrTimeFieldName[ i ] ).append( "' type='text' size='20' maxlength='250' value='" )
//                    .append( arrDT[ i ] ).append( "'>" )
//                .append( "</td>" )
//            .append( "</tr>" );
//        }
//
//        outFormSaveButton( tabIndex, sb );
//        outFormEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void setTime( CoreDataWorker dataWorker, HashMap<String,String> hmParam ) throws Throwable {
//        int[] arrDT = new int[ 6 ];
//        for( int i = 0; i < arrDT.length; i++ ) {
//            try {
//                arrDT[ i ] = Integer.parseInt( hmParam.get( arrTimeFieldName[ i ] ) );
//            }
//            catch( Throwable t ) {}
//        }
//
//        VideoFunction.setTime( dataWorker.alStm.get( 0 ), timeSystem, arrDT );
//    }
//
//    private void getVCForm( long sessionId, StringBuilder sb ) {
//        HashMap<String,String> hmSerialNoConfig = CommonFunction.loadConfig( serialNoIniFileName );
//        String serialNo = hmSerialNoConfig.get( VideoFunction.CONFIG_SERIAL_NO );
//
//        outPageBegin( "Настройки регистратора", sb );
//        outFormBegin( sb, PARAM_SESSION_ID, Long.toString( sessionId ), PARAM_ALIAS, ALIAS_VC_SAVE );
//
//        int tabIndex = 1;
//        //if( error != null ) outFormRowString( tabIndex++, "Ошибка ввода:", "error", error, 250, sb );
//        outFormRowString( tabIndex++, "Серийный номер регистратора:", PARAM_SERIAL_NO, serialNo == null ? "" : serialNo, 250, sb );
//
//        outFormSaveButton( tabIndex, sb );
//        outFormEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void setVCParam( HashMap<String,String> hmParam ) throws Throwable {
//        //--- серийный номер регистратора ---
//
//        String serialNo = hmParam.get( PARAM_SERIAL_NO );
//
//        HashMap<String,String> hmSerialNoConfig = CommonFunction.loadConfig( serialNoIniFileName );
//        hmSerialNoConfig.put( VideoFunction.CONFIG_SERIAL_NO, serialNo );
//        CommonFunction.saveConfig( hmSerialNoConfig, serialNoIniFileName );
//
//        File serialNoCommandFile = new File( serialNoCommandFileName );
//        BufferedWriter bwText = CommonFunction.getFileWriter( serialNoCommandFile, "Cp1251", false );
//        bwText.write( timeSystem == VideoFunction.TIME_SYSTEM_WINDOWS ? "SET SERIAL_NO=" : "SERIAL_NO=" );
//        bwText.write( serialNo );
//        bwText.newLine();
//        bwText.close();
//        serialNoCommandFile.setExecutable( true );
//    }
//
//    private void getDelForm( long sessionId, StringBuilder sb ) {
//        HashMap<String,String> hmDelConfig = CommonFunction.loadConfig( delIniFileName );
//        String staticDel = hmDelConfig.get( CoreDELAsker.CONFIG_STATIC_DEL );
//        String delIP = hmDelConfig.get( CoreDELAsker.CONFIG_DEL_IP );
//        String delPort = hmDelConfig.get( CoreDELAsker.CONFIG_DEL_PORT );
//        HashMap<String,String> hmDelInfo = CommonFunction.loadConfig( delInfoFileName );
//
//        int index = 0;
//        ArrayList<String> alToucanIP = new ArrayList<>();
//        ArrayList<String> alToucanPort = new ArrayList<>();
//        while( true ) {
//            String serverIP = hmDelConfig.get( CoreImageDel.CONFIG_SERVER_IP_ + index );
//            if( serverIP == null ) break;
//
//            alToucanIP.add( serverIP );
//            alToucanPort.add( hmDelConfig.get( CoreImageDel.CONFIG_SERVER_PORT_ + index ) );
//
//            index++;
//        }
//
//        outPageBegin( "Настройки ДЭЛ", sb );
//        outFormBegin( sb, PARAM_SESSION_ID, Long.toString( sessionId ), PARAM_ALIAS, ALIAS_DEL_SAVE );
//
//        int tabIndex = 1;
//        //if( error != null ) outFormRowString( tabIndex++, "Ошибка ввода:", "error", error, 250, sb );
//        outFormRowString( tabIndex++, "Статический номер ДЭЛ<br>(введите 0 для установки по опросу):", PARAM_DEL_NO,
//                          staticDel == null || staticDel.isEmpty() ? "0" : staticDel, 250, sb );
//        outFormRowString( tabIndex++, "Адрес ДЭЛ:", PARAM_DEL_IP, delIP, 250, sb );
//        outFormRowString( tabIndex++, "Порт ДЭЛ:", PARAM_DEL_PORT, delPort, 250, sb );
//        for( int i = 0; i < 2; i++ ) {
//            outFormRowString( tabIndex++, "Адрес Toucan " + i + ':', PARAM_TOUCAN_IP_ + i,
//                              i >= alToucanIP.size() || alToucanIP.get( i ) == null ? "" : alToucanIP.get( i ), 250, sb );
//            outFormRowString( tabIndex++, "Порт Toucan " + i + ':', PARAM_TOUCAN_PORT_ + i,
//                              i >= alToucanPort.size() || alToucanPort.get( i ) == null ? "" : alToucanPort.get( i ), 250, sb );
//        }
//        for( int i = 0; i < CoreDELAsker.alDelParamName.size(); i++ ) {
//            String delParamName = CoreDELAsker.alDelParamName.get( i );
//            String delParamValue = hmDelInfo.get( delParamName );
//            outFormRowString( tabIndex++, CoreDELAsker.alDelParamDescr.get( i ) + ':', delParamName,
//                              delParamValue == null ? "" : delParamValue, 250, sb );
//        }
//
//        outFormSaveButton( tabIndex, sb );
//        outFormEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void setDelParam( HashMap<String,String> hmParam ) {
//        int delNo = StringFunction.parseWithDefault( hmParam.get( PARAM_DEL_NO ), 0 );
//        int delPort = StringFunction.parseWithDefault( hmParam.get( PARAM_DEL_PORT ), 17999 );
//
//        HashMap<String,String> hmDelConfig = CommonFunction.loadConfig( delIniFileName );
//
//        if( delNo == 0 ) hmDelConfig.remove( CoreDELAsker.CONFIG_STATIC_DEL );
//        else hmDelConfig.put( CoreDELAsker.CONFIG_STATIC_DEL, Integer.toString( delNo ) );
//
//        hmDelConfig.put( CoreDELAsker.CONFIG_DEL_IP, hmParam.get( PARAM_DEL_IP ) );
//        hmDelConfig.put( CoreDELAsker.CONFIG_DEL_PORT, Integer.toString( delPort ) );
//        for( int i = 0; i < 2; i++ ) {
//            hmDelConfig.put( CoreImageDel.CONFIG_SERVER_IP_ + i, hmParam.get( PARAM_TOUCAN_IP_ + i ) );
//            hmDelConfig.put( CoreImageDel.CONFIG_SERVER_PORT_ + i, hmParam.get( PARAM_TOUCAN_PORT_ + i ) );
//        }
//        CommonFunction.saveConfig( hmDelConfig, delIniFileName );
//
//        //--- кроме основных del-параметров, там могут быть еще и камерные параметры
//        HashMap<String,String> hmDelInfo = CommonFunction.loadConfig( delInfoFileName );
//        for( int i = 0; i < CoreDELAsker.alDelParamName.size(); i++ ) {
//            String delParamName = CoreDELAsker.alDelParamName.get( i );
//            hmDelInfo.put( delParamName, hmParam.get( delParamName ) );
//        }
//        CommonFunction.saveConfig( hmDelInfo, delInfoFileName );
//    }
//
//    private void getCameraList( CoreDataWorker dataWorker, long sessionId, StringBuilder sb ) throws Throwable {
//        outPageBegin( "Список видеокамер", sb );
//        outTableBegin( sb );
//
//        outMainPageRow( sessionId, ALIAS_MAIN_MENU, "В главное меню", sb );
//
//        outCameraListHeader( sb );
//
//        //--- object_id всегда = 1
//        //--- name здесь тоже не интересен
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT id , descr , login , pwd , url_0 , url_1 , url_mjpeg , url_image , url_time , " )
//            .append( " video_codec , audio_codec , duration FROM VC_camera WHERE id <> 0 ORDER BY descr " ) );
//        while( rs.next() )
//            outCameraListRow( sessionId, rs, sb );
//        rs.close();
//
//        TreeMap<Integer,CameraModelData> tmCMD = VideoFunction.loadCameraModelData( dataServer.hmConfig );
//        for( Integer mID : tmCMD.keySet() )
//            outCameraAddRow( sessionId, mID, tmCMD.get( mID ).name, sb );
//
//        outCameraAddRow( sessionId, -1, null, sb );
//
//        outTableEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void getCameraForm( CoreDataWorker dataWorker, HashMap<String,String> hmParam, long sessionId,
//                                String error, StringBuilder sb )  {
//        String id = hmParam.get( PARAM_ID );
//        String sModelID = hmParam.get( PARAM_CAMERA_MODEL );
//        int modelID = Integer.parseInt( sModelID );
//        int tabIndex = 1;
//        int p = 1;
//
//        outPageBegin( "Настройки камеры", sb );
//        outFormBegin( sb, PARAM_SESSION_ID, Long.toString( sessionId ), PARAM_ALIAS, ALIAS_CAMERA_SAVE, PARAM_ID, id,
//                      PARAM_CAMERA_MODEL, sModelID );
//
//        //--- object_id всегда = 1
//        //--- name здесь тоже не интересен
//        CoreAdvancedResultSet rs = null;
//        if( ! id.equals( "0" ) ) {
//            rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                    " SELECT descr , login , pwd , url_0 , url_1 , url_mjpeg , url_image , url_time , " )
//                    .append( " video_codec , audio_codec , duration FROM VC_camera WHERE id = " ).append( id ) );
//            rs.next();
//        }
//
//        if( error != null ) outFormRowString( tabIndex++, "Ошибка ввода:", "error", error, 250, sb );
//        if( modelID != -1 ) {
//            outFormRowString( tabIndex++, "Порядковый номер:", "order_no", "", 250, sb );
//        }
//        outFormRowString( tabIndex++, "Описание:", "descr", rs == null ? "" : rs.getString( p++ ), 250, sb );
//        outFormRowString( tabIndex++, "Логин:", "login", rs == null ? "" : rs.getString( p++ ), 250, sb );
//        outFormRowString( tabIndex++, "Пароль:", "pwd", rs == null ? "" : rs.getString( p++ ), 250, sb );
//        if( modelID != -1 ) {
//            outFormRowString( tabIndex++, "IP-адрес:", "ip", "", 250, sb );
//        }
//        else {
//            outFormRowString( tabIndex++, "URL основного видеопотока:", "url_0", rs == null ? "" : rs.getString( p++ ), 250, sb );
//            outFormRowString( tabIndex++, "URL дополнительного видеопотока:", "url_1", rs == null ? "" : rs.getString( p++ ), 250, sb );
//            outFormRowString( tabIndex++, "URL mjpeg-потока:", "url_mjpeg", rs == null ? "" : rs.getString( p++ ), 250, sb );
//            outFormRowString( tabIndex++, "URL одиночных кадров:", "url_image", rs == null ? "" : rs.getString( p++ ), 250, sb );
//            outFormRowString( tabIndex++, "URL установки времени:", "url_time", rs == null ? "" : rs.getString( p++ ), 1000, sb );
//        }
//        outFormRowComboBox( tabIndex++, "Видео-кодек:", "video_codec", rs == null ? VideoFunction.VC_COPY : rs.getInt( p++ ),
//                                    new int[] { VideoFunction.VC_COPY, VideoFunction.VC_H264 }, new String[] { "(исх.)", "H.264" }, sb );
//        outFormRowComboBox( tabIndex++, "Аудио-кодек:", "audio_codec", rs == null ? VideoFunction.AC_NONE : rs.getInt( p++ ),
//                                    new int[] { VideoFunction.AC_NONE, VideoFunction.AC_COPY, VideoFunction.AC_AAC },
//                                    new String[] { "(выкл.)", "(исх.)", "AAC" }, sb );
//        outFormRowString( tabIndex++, "Нарезка [сек]:", "duration", rs == null ? "300" : Integer.toString( rs.getInt( p++ ) ), 250, sb );
//
//        if( rs != null ) rs.close();
//
//        //--- свой вариант концовки формы
//        sb.append( "<tr><td>" );
//        if( id.equals( "0" ) )
//            sb.append( "&nbsp;" );
//        else
//            sb.append( "<a href='/" )
//              .append( '?' ).append( PARAM_SESSION_ID ).append( '=' ).append( sessionId )
//              .append( '&' ).append( PARAM_ALIAS ).append( '=' ).append( ALIAS_CAMERA_DELETE )
//              .append( '&' ).append( PARAM_ID ).append( '=' ).append( id )
//              .append( "'>" )
//              .append( "Удалить камеру" )
//              .append( "</a>" );
//        sb.append( "</td><td>" )
//                .append( "<input tabindex='" ).append( tabIndex ).append( "' type='submit' value='Сохранить'>" )
//          .append( "</td>" )
//          .append( "</tr>" );
//
//        outFormEnd( sb );
//        outPageEnd( sb );
//    }
//
//    private void saveCamera( CoreDataWorker dataWorker, HashMap<String,String> hmParam, long sessionId, StringBuilder sb ) throws Throwable {
//        int orderNo = 0;
//        try {
//            orderNo = Integer.parseInt( hmParam.get( "order_no" ) );
//        }
//        catch( Throwable t ) {}
//
//        String sModelID = hmParam.get( PARAM_CAMERA_MODEL );
//        int modelID = Integer.parseInt( sModelID );
//
//        //--- проверка правильности ввода ---
//        String cameraDescr = hmParam.get( "descr" ).trim();
//
//        //--- url_0 - проверяем только в случае если вводится "другая" камера
//        String url0 = hmParam.get( "url_0" );
//        if( modelID == -1 ) {
//            url0 = url0.trim();
//            if( url0.isEmpty() ) {
//                getCameraForm( dataWorker, hmParam, sessionId, "URL основного видеопотока должен быть всегда заполнен!", sb );
//                return;
//            }
//        }
//        else {
//            if( orderNo != 0 && cameraDescr.isEmpty() ) cameraDescr = "Cam-" + orderNo;
//        }
//        //--- duration
//        int duration;
//        try {
//            duration = Integer.parseInt( hmParam.get( "duration" ) );
//            if( duration < 5 || duration > 3600 ) {
//                getCameraForm( dataWorker, hmParam, sessionId, "Продолжительность нарезки должна быть в диапазоне от 5 до 3600 сек!", sb );
//                return;
//            }
//        }
//        catch( NumberFormatException nfe ) {
//            getCameraForm( dataWorker, hmParam, sessionId, "Ошибка ввода продолжительности нарезки!", sb );
//            return;
//        }
//        int id = Integer.parseInt( hmParam.get( PARAM_ID ) );
//        int objectId = 1;
//
//        StringBuilder sbErrorValue = new StringBuilder();
//        StringBuilder sbErrorText = new StringBuilder();
//
//        boolean isValid = cVideoCamera.checkDescr( dataWorker, cameraDescr, id, objectId, sbErrorValue, sbErrorText );
//
//        if( ! isValid ) {
//            getCameraForm( dataWorker, hmParam, sessionId, sbErrorText.toString(), sb );
//            return;
//        }
//
//        //--- собственно сохранение
//        String dirVideoRoot = dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR );
//        String ffmpegPath = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_PATH );
//        String ffmpegMetaData = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_METADATA_FILE );
//        String ffmpegExtraCommand = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_EXTRA_COMMAND );
//        int taskSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_SYSTEM ) );
//        String taskPath = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PATH );
//        String taskManager = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_MANAGER );
//        String taskUser = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_USER );
//        String taskPassword = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PASSWORD );
//        Integer doorGPIO = cVideoCamera.loadDoorButtonGPIO( dataServer.hmConfig );
//        TreeMap<Integer,Integer> tmDisplayButtonGPIO = cVideoCamera.loadDisplayButtonGPIO( dataServer.hmConfig );
//        String playerName = dataServer.hmConfig.get( VideoFunction.CONFIG_PLAYER_NAME );
//        String cameraShowScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME );
//        String displayButtonScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME );
//
//        String login = hmParam.get( "login" ).trim();
//        String pwd = hmParam.get( "pwd" ).trim();
//
//        String url1 = "";
//        String urlMjpeg = "";
//        String urlImage = "";
//        String urlTime = "";
//
//        if( modelID == -1 ) {
//            url1 = hmParam.get( "url_1" ).trim();
//            urlMjpeg = hmParam.get( "url_mjpeg" ).trim();
//            urlImage = hmParam.get( "url_image" ).trim();
//            urlTime = hmParam.get( "url_time" ).trim();
//        }
//        else {
//            String ip = hmParam.get( "ip" ).trim();
//            if( orderNo != 0 && ip.isEmpty() ) ip = "192.168.7." + ( 30 + orderNo - 1 );
//            CameraModelData cmd = VideoFunction.loadCameraModelData( dataServer.hmConfig ).get( modelID );
//
//            if( cmd.login != null       && ! cmd.login.isEmpty() && login.isEmpty() ) login = cmd.login;
//            if( cmd.itPassword != null    && ! cmd.itPassword.isEmpty() && pwd.isEmpty() ) pwd = cmd.itPassword;
//            if( cmd.arrUrl[ 0 ] != null && ! cmd.arrUrl[ 0 ].isEmpty() ) url0 = cmd.arrUrl[ 0 ].replace( "0.0.0.0", ip );
//            if( cmd.arrUrl[ 1 ] != null && ! cmd.arrUrl[ 1 ].isEmpty() ) url1 = cmd.arrUrl[ 1 ].replace( "0.0.0.0", ip );
//            if( cmd.urlMjpeg != null    && ! cmd.urlMjpeg.isEmpty() )    urlMjpeg = cmd.urlMjpeg.replace( "0.0.0.0", ip );
//            if( cmd.urlImage != null    && ! cmd.urlImage.isEmpty() )    urlImage = cmd.urlImage.replace( "0.0.0.0", ip );
//            if( cmd.urlTime != null     && ! cmd.urlTime.isEmpty() )     urlTime = cmd.urlTime.replace( "0.0.0.0", ip );
//        }
//
//        int videoCodec = Integer.parseInt( hmParam.get( "video_codec" ) );
//        int audioCodec = Integer.parseInt( hmParam.get( "audio_codec" ) );
//        String[] arrURL = { url0 == null || url0.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url0, login, pwd, true ),
//                            url1 == null || url1.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url1, login, pwd, true ) };
//
//        if( id == 0 ) {
//            id = dataWorker.alStm.get( 0 ).getNextID( "VC_camera", "id" );
//            StringBuilder sbSQL = new StringBuilder(
//                " INSERT INTO VC_camera ( id , object_id , name , descr , login , pwd , " )
//                .append( " url_0 , url_1 , url_mjpeg , url_image , url_time , video_codec , audio_codec , duration ) VALUES ( " )
//                .append( id ).append( " , 1 , '' , '" ).append( cameraDescr )
//                .append( "' , '" ).append( login ).append( "' , '" ).append( pwd )
//                .append( "' , '" ).append( url0 ).append( "' , '" ).append( url1 )
//                .append( "' , '" ).append( urlMjpeg ).append( "' , '" ).append( urlImage ).append( "' , '" ).append( urlTime )
//                .append( "' , " ).append( videoCodec ).append( " , " ).append( audioCodec )
//                .append( " , " ).append( duration ).append( " ); " );
//            dataWorker.alStm.get( 0 ).executeUpdate( sbSQL );
//
//            cVideoCamera.addCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName, dirVideoRoot,
//                                    ffmpegPath, ffmpegMetaData, ffmpegExtraCommand,
//                                    taskSystem, taskPath, taskManager, taskUser, taskPassword, doorGPIO, tmDisplayButtonGPIO, playerName,
//                                    objectId, cameraDescr, arrURL, duration, videoCodec, audioCodec,
//                                    cameraShowScriptFileName, displayButtonScriptFileName );
//        }
//        else {
//            StringBuilder sbSQL = new StringBuilder(
//                " UPDATE VC_camera SET object_id = 1 , name = '' " )
//                .append( "  , descr = '" ).append( cameraDescr )
//                .append( "' , login = '" ).append( login )
//                .append( "' , pwd = '" ).append( pwd )
//                .append( "' , url_0 = '" ).append( url0 )
//                .append( "' , url_1 = '" ).append( url1 )
//                .append( "' , url_mjpeg = '" ).append( urlMjpeg )
//                .append( "' , url_image = '" ).append( urlImage )
//                .append( "' , url_time = '" ).append( urlTime )
//                .append( "' , video_codec = " ).append( videoCodec )
//                .append( "  , audio_codec = " ).append( audioCodec )
//                .append( "  , duration = " ).append( duration )
//                .append( " WHERE id = " ).append( id );
//            dataWorker.alStm.get( 0 ).executeUpdate( sbSQL );
//
//            cVideoCamera.editCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName, dirVideoRoot,
//                                     ffmpegPath, ffmpegMetaData, ffmpegExtraCommand,
//                                     taskSystem, taskPath, taskManager, doorGPIO, tmDisplayButtonGPIO, playerName,
//                                     objectId, cameraDescr, arrURL, duration, videoCodec, audioCodec,
//                                     cameraShowScriptFileName, displayButtonScriptFileName );
//        }
//        //--- обновить current_del.info
//        HashMap<String,String> hmDelInfo = CommonFunction.loadConfig( delInfoFileName );
//        int cameraNo = 0;
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                     " SELECT descr , login , pwd , url_0 , url_1 , url_mjpeg , url_image " )
//            .append( " FROM VC_camera WHERE id <> 0 ORDER BY descr " ) );
//        while( rs.next() ) {
//            hmDelInfo.put( "camera_descr_" + cameraNo, rs.getString( 1 ) );
//            hmDelInfo.put( "camera_login_" + cameraNo, rs.getString( 2 ) );
//            hmDelInfo.put( "camera_password_" + cameraNo, rs.getString( 3 ) );
//            hmDelInfo.put( "camera_url_0_" + cameraNo, rs.getString( 4 ) );
//            hmDelInfo.put( "camera_url_1_" + cameraNo, rs.getString( 5 ) );
//            hmDelInfo.put( "camera_url_mjpeg_" + cameraNo, rs.getString( 6 ) );
//            hmDelInfo.put( "camera_url_image_" + cameraNo, rs.getString( 7 ) );
//            //--- я бы убрал, но бубенцов развоняется
//            hmDelInfo.put( "is_enabled_"+ cameraNo, "1" );
//
//            cameraNo++;
//        }
//        rs.close();
//        hmDelInfo.put( "camera_count", Integer.toString( cameraNo ) );
//        saveConfigForHuentsov( hmDelInfo, delInfoFileName );
//
//        getCameraList( dataWorker, sessionId, sb );
//    }
//
//    private void deleteCamera( CoreDataWorker dataWorker, HashMap<String,String> hmParam, long sessionId, StringBuilder sb ) throws Throwable {
//        //--- собственно сохранение
//        int taskSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_SYSTEM ) );
//        String taskPath = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PATH );
//        String taskManager = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_MANAGER );
//        Integer doorGPIO = cVideoCamera.loadDoorButtonGPIO( dataServer.hmConfig );
//        TreeMap<Integer,Integer> tmDisplayButtonGPIO = cVideoCamera.loadDisplayButtonGPIO( dataServer.hmConfig );
//        String playerName = dataServer.hmConfig.get( VideoFunction.CONFIG_PLAYER_NAME );
//        String cameraShowScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME );
//        String displayButtonScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME );
//
//        int id = Integer.parseInt( hmParam.get( PARAM_ID ) );
//        int objectId;
//        String cameraDescr;
//
//        CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//            " SELECT object_id, descr FROM VC_camera WHERE id = " ).append( id ).toString() );
//        rs.next();
//        objectId = rs.getInt( 1 );
//        cameraDescr = rs.getString( 2 );
//        rs.close();
//
//        StringBuilder sbSQL = new StringBuilder( " DELETE FROM VC_camera WHERE id = " ).append( id );
//        dataWorker.alStm.get( 0 ).executeUpdate( sbSQL );
//
//        cVideoCamera.deleteCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName,
//                                   taskSystem, taskPath, taskManager,
//                                   doorGPIO, tmDisplayButtonGPIO, playerName,
//                                   objectId, cameraDescr, cameraShowScriptFileName, displayButtonScriptFileName );
//
//        getCameraList( dataWorker, sessionId, sb );
//    }
//
//    private void doReboot() {
//        CommonFunction.runCommand( null, "reboot" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void outPageBegin( String title, StringBuilder sb ) {
//        sb.append( "<html>" );
//        sb.append( "<head>" );
//        sb.append( "<meta http-equiv='Content-Type' content='text/html; charset=windows-1251'>" );
//        //          <meta http-equiv='Content-Type' content='text/html; charset=cp1251'\>
//        sb.append( "<meta http-equiv='Content-Language' content='ru'>" );
//        sb.append( "<meta http-equiv='Pragma' content='no-cache'>" );
//        sb.append( "<title>" ).append( title ).append( "</title>" );
////        sb.append( "<style>" + style.getStyle() + "</style>" );
//        sb.append( "</head>" );
//        sb.append( "<body leftmargin='0' topmargin='0' rightmargin='0' bottommargin='0'>" );
//    }
//
//    private void outPageEnd( StringBuilder sb ) {
//        sb.append( "</body>" );
//        sb.append( "</html>" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void outTableBegin( StringBuilder sb ) {
//        sb.append( "<table border='0' width='100%' height='100%'>" )
//          .append( "<tr><td align='center' valign='middle'>" )
//          .append( "<table border='0' cellpadding='10' cellspacing='0'>" );
//    }
//
//    private void outTableEnd( StringBuilder sb ) {
//        sb.append( "</table>" )
//          .append( "</td></tr>" )
//          .append( "</table>" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void outFormBegin( StringBuilder sb, String... arrHiddenParam ) {
//        sb.append( "<table border='0' width='100%' height='100%'>" )
//          .append( "<tr><td align='center' valign='middle'>" )
//          .append( "<form name='' action='/' method='post'>" );
//
//        for( int i = 0; i < arrHiddenParam.length; i += 2 )
//            sb.append( "<input name='" ).append( arrHiddenParam[ i ] )
//              .append( "' type='hidden' value='" ).append( arrHiddenParam[ i + 1 ] ).append( "'>" );
//
//        sb.append( "<table border='0' cellpadding='10' cellspacing='0'>" );
//    }
//
//    private void outFormRowBoolean( int tabIndex, String caption, String name, boolean value, StringBuilder sb ) {
//        sb.append( "<tr>" )
//            .append( "<td>" )
//                .append( caption )
//            .append( "</td>" )
//            .append( "<td>" )
//                .append( "<input tabindex='" ).append( tabIndex ).append( "' name='" )
//                .append( name ).append( "' type='checkbox'" ).append( value ? " checked" : "" ).append( '>' )
//            .append( "</td>" )
//        .append( "</tr>" );
//    }
//
//    private void outFormRowString( int tabIndex, String caption, String name, String value, int maxLen, StringBuilder sb ) {
//        sb.append( "<tr>" )
//            .append( "<td>" )
//                .append( caption )
//            .append( "</td>" )
//            .append( "<td>" )
//                .append( "<input tabindex='" ).append( tabIndex ).append( "' name='" )
//                .append( name ).append( "' type='text' size='60' maxlength='" ).append( maxLen ).append( "' value='" )
//                .append( value ).append( "'>" )
//            .append( "</td>" )
//        .append( "</tr>" );
//    }
//
//    private void outFormRowComboBox( int tabIndex, String caption, String name, int value, int[] arrValue, String[] arrLabel,
//                                     StringBuilder sb ) {
//        sb.append( "<tr>" )
//            .append( "<td>" )
//                .append( caption )
//            .append( "</td>" )
//            .append( "<td>" )
//                .append( "<select tabindex='" ).append( tabIndex ).append( "' name='" ).append( name ).append( "'>" );
//                for( int i = 0; i < arrValue.length; i++ )
//                    sb.append( "<option value='" ).append( arrValue[ i ] ).append( '\'' )
//                      .append( arrValue[ i ] == value  ? " selected" : "" ).append( '>' )
//                      .append( arrLabel[ i ] ).append( "</option>" );
//                sb.append( "</select>" )
//            .append( "</td>" )
//        .append( "</tr>" );
//    }
//
//    private void outFormSaveButton( int tabIndex, StringBuilder sb ) {
//        sb.append( "<tr>" )
////          .append( "<td colspan='2'>" )
////                .append( "<input tabindex='" ).append( tabIndex ).append( "' type='submit' value='Сохранить'>" )
////          .append( "</td>" )
//          .append( "<td>&nbsp;</td>" )
//          .append( "<td>" )
//                .append( "<input tabindex='" ).append( tabIndex ).append( "' type='submit' value='Сохранить'>" )
//          .append( "</td>" )
//          .append( "</tr>" );
//    }
//
//    private void outFormEnd( StringBuilder sb ) {
//        sb.append( "</table>" )
//          .append( "</form>" )
//          .append( "</td></tr>" )
//          .append( "</table>" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void outMainPageRow( long sessionId, String alias, String caption, StringBuilder sb ) {
////        sb.append( "<tr><td colspan='4'>" )
//        sb.append( "<tr><td>" )
//          .append( "<a href='/" )
//          .append( '?' ).append( PARAM_SESSION_ID ).append( '=' ).append( sessionId )
//          .append( '&' ).append( PARAM_ALIAS ).append( '=' ).append( alias )
//          .append( "'>" )
//          .append( caption )
//          .append( "</a>" )
//          .append( "</td></tr>" );
//    }
//
//    private void outCameraListHeader( StringBuilder sb ) throws Throwable {
//        sb.append( "<tr>" )
//          .append( "<td>" )
//          .append( "Описание" )
//          .append( "</a>" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "Логин" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "Пароль" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "URL:" )
//          .append( "<br>- основного видеопотока" )
//          .append( "<br>- дополнительного видеопотока" )
//          .append( "<br>- mjpeg-потока" )
//          .append( "<br>- одиночных кадров" )
//          .append( "<br>- настройки времени" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "Видео-кодек" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "Аудио-кодек" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "Нарезка [сек]" )
//          .append( "</td>" );
//
//        sb.append( "</tr>" );
//    }
//
//    private void outCameraListRow( long sessionId, CoreAdvancedResultSet rs, StringBuilder sb ) throws Throwable {
//        //--- динамический индекс поля, чтоб каждый раз не менять
//        int p = 1;
//        sb.append( "<tr>" )
//
//          .append( "<td>" )
//          .append( "<a href='/" )
//          .append( '?' ).append( PARAM_SESSION_ID ).append( '=' ).append( sessionId )
//          .append( '&' ).append( PARAM_ALIAS ).append( '=' ).append( ALIAS_CAMERA_FORM )
//          .append( '&' ).append( PARAM_ID ).append( '=' ).append( rs.getInt( p++ ) )
//          .append( '&' ).append( PARAM_CAMERA_MODEL ).append( '=' ).append( -1 )
//          .append( "'>" )
//          .append( rs.getString( p++ ) )
//          .append( "</a>" )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( rs.getString( p++ ) )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( rs.getString( p++ ) )
//          .append( "</td>" )
//
//          .append( "<td>" )
//          .append( "<br>- " ).append( rs.getString( p++ ) )
//          .append( "<br>- " ).append( rs.getString( p++ ) )
//          .append( "<br>- " ).append( rs.getString( p++ ) )
//          .append( "<br>- " ).append( rs.getString( p++ ) )
//          .append( "<br>- " ).append( rs.getString( p++ ) )
//          .append( "</td>" );
//
//        int videoCodec = rs.getInt( p++ );
//
//        sb.append( "<td>" )
//          .append( videoCodec == VideoFunction.VC_COPY ? "(исх.)" :
//                   videoCodec == VideoFunction.VC_H264 ? "H.264" : "(неизвестный кодек)" )
//          .append( "</td>" );
//
//        int audioCodec = rs.getInt( p++ );
//
//        sb.append( "<td>" )
//          .append( audioCodec == VideoFunction.AC_NONE ? "(выкл.)" :
//                   audioCodec == VideoFunction.AC_COPY ? "(исх.)" :
//                   audioCodec == VideoFunction.AC_AAC ? "AAC" : "(неизвестный кодек)" )
//          .append( "</td>" );
//
//        sb.append( "<td>" )
//          .append( rs.getInt( p++ ) )
//          .append( "</td>" );
//
//        sb.append( "</tr>" );
//    }
//
//    private void outCameraAddRow( long sessionId, int cameraModelID, String cameraModelName, StringBuilder sb ) throws Throwable {
//        sb.append( "<tr>" )
//          .append( "<td>" )
//          .append( "&nbsp;" )
//          .append( "</td>" )
//          .append( "<td colspan='4'>" )
//          .append( "<a href='/" )
//          .append( '?' ).append( PARAM_SESSION_ID ).append( '=' ).append( sessionId )
//          .append( '&' ).append( PARAM_ALIAS ).append( '=' ).append( ALIAS_CAMERA_FORM )
//          .append( '&' ).append( PARAM_ID ).append( '=' ).append( 0 )
//          .append( '&' ).append( PARAM_CAMERA_MODEL ).append( '=' ).append( cameraModelID )
//          .append( "'>" )
//          .append( cameraModelID == -1 ? "Добавить другую камеру" : "Добавить камеру " + cameraModelName )
//          .append( "</a>" )
//          .append( "</td>" )
//          .append( "</tr>" );
//    }
//
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static void saveConfigForHuentsov( HashMap<String,String> hmConfig, String fileName ) throws Throwable {
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileName, "Cp1251", false );
//        for( String key : hmConfig.keySet() ) {
//            String value = hmConfig.get( key );
//            bwText.write( key );
//            bwText.write( '=' );
//            bwText.write( value == null ? "" : value );
//            bwText.newLine();
//        }
//        bwText.close();
//    }
//}
//
////            //--- по умолчанию - отправка идёт без контроля отправки
////            //--- (пока нигде не используется)
////            boolean withWriteControl = false;
////            //--- набор для накопления выходных параметров.
////            //--- выходные параметры будут записаны в сессию, только если транзакция пройдет успешно.
////            HashMap<String,Object> hmOut = new HashMap<>();
////            //--- ссылка на файл, удаляемый после завершения транзакции
////            //--- (и, возможно, после успешной контролируемой передачи данных)
////            //--- (пока нигде не применяется)
////            //File fileForDeleteAfterCommit = null;
////
////            else if( action.equals( AppAction.LOGOFF ) ) {
////                chmSession = new ConcurrentHashMap<>();
////                dataServer.chmSessionStore.put( sessionId, chmSession );
////            }
////            //--- команды, для которых требуется вход в систему и данные о пользователе
////            else {
////                UserConfig userConfig = (UserConfig) chmSession.get( USER_CONFIG );
////postData.append( "\nUserID: " ).append( userConfig == null ? "null" : userConfig.getUserID() );
////
////                else {
////                    String aliasName = hmParam.get( AppParameter.ALIAS );
////                    if( aliasName == null ) throw new Throwable( "Не указано имя модуля." );
////
////                    HashMap<String,AliasConfig> hmAliasConfig = getAliasConfig( dataWorker, hmOut );
////                    AliasConfig aliasConfig = hmAliasConfig.get( aliasName );
////                    if( aliasConfig == null )
////                        throw new Throwable( new StringBuilder( "Модуль '" ).append( aliasName ).append( "' не существует." ).toString() );
////
////                    //--- если класс не требует обязательной аутентификации и нет никакого логина,
////                    //--- то подгрузим хотя бы гостевой логин
////                    if( ! aliasConfig.isAuthorization() && userConfig == null ) {
////                        //--- при отсутствии оного загрузим гостевой логин
////                        userConfig = UserConfig.getConfig( dataWorker.alStm.get( 0 ), UserConfig.USER_GUEST );
////                        hmOut.put( USER_CONFIG, userConfig ); // уйдет в сессию
////                    }
////
////                    //--- если класс требует обязательную аутентификацию,
////                    //--- а юзер не залогинен или имеет гостевой логин, то запросим авторизацию
////                    if( aliasConfig.isAuthorization() && ( userConfig == null || userConfig.getUserID() == UserConfig.USER_GUEST ) )
////                        bbOut.putByte( ResponseCode.LOGON_NEED );
////                    else {
////                        //--- проверим права доступа на класс
////                        if( ! userConfig.getUserPermission().get( aliasName ).contains( cStandart.PERM_ACCESS ) )
////                            throw new Throwable( new StringBuilder( "Доступ к модулю '" ).append( aliasName ).append( "' не разрешён." ).toString() );
////
////                        cStandart page = (cStandart) Class.forName( aliasConfig.getControlClassName() ).newInstance();
////                        page.init( dataServer, dataWorker, chmSession, hmParam, hmAliasConfig, aliasConfig, hmXyDocumentConfig, userConfig );
////                        if( action.equals( AppAction.TABLE ) ) {
////                            if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam );
////
////                            bbOut.putByte( ResponseCode.TABLE );
////                            page.getTable( bbOut, hmOut );
////                        }
////                        //--- форма
////                        else if( action.equals( AppAction.FORM ) ) {
////                            if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam );
////
////                            bbOut.putByte( ResponseCode.FORM );
////                            page.getForm( bbOut, hmOut );
////                        }
////                        //--- поиск
////                        else if( action.equals( AppAction.FIND ) ) {
////                            if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam );
////
////                            //--- если сервер вдруг перезагрузили между отдельными командами поиска
////                            //--- (такое бывает редко, только при обновлениях, но тем не менее пользователю обидно),
////                            //--- то сделаем вид что поиска не было :)
////                            String findRedirectURL = page.doFind( bbIn, hmOut );
////                            if( findRedirectURL == null ) {
////                                bbOut.putByte( ResponseCode.TABLE );
////                                page.getTable( bbOut, hmOut );
////                            }
////                            else {
////                                bbOut.putByte( ResponseCode.REDIRECT );
////                                bbOut.putShortString( findRedirectURL );
////                            }
////                        }
////                        //--- сохранение и удаление
////                        else {
////                            //--- пропускаем модули с синими вёдрами
////                            //if( "ru".equals( aliasName ) ) {}
////                            //--- пропускаем логи отчётов и показов картографии
////                            //else
////                            if( checkLogSkipAliasPrefix( aliasName ) ) {
////                                if( userLogMode == SYSTEM_LOG_ALL ) logQuery( hmParam );
////                            }
////                            else if( userLogMode != SYSTEM_LOG_NONE ) logQuery( hmParam );
////
////                            bbOut.putByte( ResponseCode.REDIRECT );
////                            if( action.equals( AppAction.SAVE ) )
////                                bbOut.putShortString( page.doSave( bbIn, hmOut ) );
////                            else if( action.equals( AppAction.DELETE ) )
////                                bbOut.putShortString( page.doDelete( bbIn, hmOut ) );
////                            else throw new Throwable( new StringBuilder( "Unknown action = " )
////                                                                .append( action ).toString() );
////                        }
////                    }
////                }
////            }
//
//
////    private void putUserProperty( UserConfig userConfig, AdvancedByteBuffer bbOut ) throws Throwable {
////        HashMap<String,String> hmUserProperty = userConfig.getUserProperty();
////        bbOut.putShort( (short) hmUserProperty.size() );
////        for( String key : hmUserProperty.keySet() ) {
////            bbOut.putShortString( key );
////            bbOut.putShortString( hmUserProperty.get( key ) );
////        }
////    }

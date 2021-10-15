//package foatto.mms.core_mms.vc;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedByteBuffer;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//import foatto.core_server.app.server.column.iColumn;
//import foatto.core_server.app.server.data.*;
//import foatto.core_server.app.video.server.CameraModelData;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.ds.CoreDataWorker;
//import foatto.mms.core_mms.cMMSOneObjectParent;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.TreeMap;
//
//public class cVideoCamera extends cMMSOneObjectParent {
//
//    private static final String ENABLED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789()-_";
//
//    public static final String CONFIG_DISPLAY_BUTTON_GPIO_ = "relay_display_button_";
//    public static final String CONFIG_DOOR_BUTTON_GPIO = "relay_door_button";
//
//    protected void preSave(int id, Map<iColumn, ? extends iData> hmColumnData ) {
//        mVideoCamera mvc = (mVideoCamera) model;
//
//        int modelID = ( (DataComboBox) hmColumnData.get( mvc.getColumnModel() ) ).getValue();
//        if( modelID != -1 ) {
//            CameraModelData cmd = VideoFunction.loadCameraModelData( dataServer.hmConfig ).get( modelID );
//            String ip = ( (DataString) hmColumnData.get( mvc.getColumnIP() ) ).getText();
//            DataString dLogin = (DataString) hmColumnData.get( mvc.getColumnLogin() );
//            DataString dPassword = (DataString) hmColumnData.get( mvc.getColumnPassword() );
//
//            if( cmd.login != null && ! cmd.login.isEmpty() && dLogin.getText().isEmpty() ) dLogin.setText( cmd.login );
//            if( cmd.itPassword != null && ! cmd.itPassword.isEmpty() && dPassword.getText().isEmpty() ) dPassword.setText( cmd.itPassword );
//            for( int i = 0; i < VideoFunction.VIDEO_STREAM_COUNT; i++ )
//                if( cmd.arrUrl[ i ] != null && ! cmd.arrUrl[ i ].isEmpty() )
//                    ( (DataString) hmColumnData.get( mvc.getColumnURL( i ) ) ).setText( cmd.arrUrl[ i ].replace( "0.0.0.0", ip ) );
//            if( cmd.urlMjpeg != null && ! cmd.urlMjpeg.isEmpty() )
//                ( (DataString) hmColumnData.get( mvc.getColumnURLMJpeg() ) ).setText( cmd.urlMjpeg.replace( "0.0.0.0", ip ) );
//            if( cmd.urlImage != null && ! cmd.urlImage.isEmpty() )
//                ( (DataString) hmColumnData.get( mvc.getColumnURLImage() ) ).setText( cmd.urlImage.replace( "0.0.0.0", ip ) );
//            if( cmd.urlTime != null && ! cmd.urlTime.isEmpty() )
//                ( (DataString) hmColumnData.get( mvc.getColumnURLTime() ) ).setText( cmd.urlTime.replace( "0.0.0.0", ip ) );
//        }
//
//        super.preSave( id, hmColumnData );
//    }
//
//    protected boolean getFormValues( AdvancedByteBuffer bbIn, int id,
//                                     ArrayList<iColumn> alColumnList, HashMap<iColumn,iData> hmColumnData ) {
//        boolean isValid = super.getFormValues( bbIn, id, alColumnList, hmColumnData );
//
//        //--- фильтрация/экранирование символов в descr с последующей проверкой на пустоту и уникальность
//        if( isValid ) {
//            mVideoCamera mvc = (mVideoCamera) model;
//            int objectId = ( (DataInt) hmColumnData.get( mvc.getObjectSelector().getColumnObject() ) ).getValue();
//            DataString dataDescr = (DataString) hmColumnData.get( mvc.getColumnDescr() );
//            String cameraDescr = dataDescr.getText().trim();
//
//            StringBuilder sbErrorValue = new StringBuilder();
//            StringBuilder sbErrorText = new StringBuilder();
//
//            isValid = checkDescr( dataWorker, cameraDescr, id, objectId, sbErrorValue, sbErrorText );
//
//            if( isValid ) dataDescr.setText( cameraDescr );
//            else {
//                dataDescr.setErrorValue( sbErrorValue.toString() );
//                dataDescr.setErrorText( sbErrorText.toString() );
//            }
//        }
//
//        return isValid;
//    }
//
//    protected String postAdd(int id, Map<iColumn, ? extends iData> hmColumnData, Map<String, Object> hmOut ) throws Throwable {
//        String postURL = super.postAdd( id, hmColumnData, hmOut );
//
//        String dirVideoRoot = dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR );
//        String ffmpegPath = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_PATH );
//        String ffmpegMetaData = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_METADATA_FILE );
//        String ffmpegExtraCommand = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_EXTRA_COMMAND );
//        int taskSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_SYSTEM ) );
//        String taskPath = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PATH );
//        String taskManager = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_MANAGER );
//        String taskUser = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_USER );
//        String taskPassword = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PASSWORD );
//        Integer doorGPIO = loadDoorButtonGPIO( dataServer.hmConfig );
//        TreeMap<Integer,Integer> tmDisplayButtonGPIO = loadDisplayButtonGPIO( dataServer.hmConfig );
//        String playerName = dataServer.hmConfig.get( VideoFunction.CONFIG_PLAYER_NAME );
//        String cameraShowScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME );
//        String displayButtonScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME );
//
//        mVideoCamera mvc = (mVideoCamera) model;
//
//        int objectId = ( (DataInt) hmColumnData.get( mvc.getObjectSelector().getColumnObject() ) ).getValue();
//        String cameraDescr = ( (DataString) hmColumnData.get( mvc.getColumnDescr() ) ).getText();
//        //--- ffmpeg нервно реагирует на наличие символа '&' в rstp-url - воспринимает его как следующую команду на выполнение
//        //--- (согласно linux-правилам), поэтому обернём rstp-url в двойные кавычки
//        String url0 = ( ( DataString) hmColumnData.get( mvc.getColumnURL( 0 ) ) ).getText();
//        String url1 = ( ( DataString) hmColumnData.get( mvc.getColumnURL( 1 ) ) ).getText();
//        String[] arrURL = {
//            url0.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url0,
//                                                        ( ( DataString) hmColumnData.get( mvc.getColumnLogin() ) ).getText(),
//                                                        ( ( DataString) hmColumnData.get( mvc.getColumnPassword() ) ).getText(), true ),
//            url1.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url1,
//                                            ( ( DataString) hmColumnData.get( mvc.getColumnLogin() ) ).getText(),
//                                            ( ( DataString) hmColumnData.get( mvc.getColumnPassword() ) ).getText(), true ) };
//        int duration = ( (DataInt) hmColumnData.get( mvc.getColumnDuration() ) ).getValue();
//        int videoCodec = ( ( DataComboBox) hmColumnData.get( mvc.getColumnVideoCodec() ) ).getValue();
//        int audioCodec = ( ( DataComboBox) hmColumnData.get( mvc.getColumnAudioCodec() ) ).getValue();
//
//        addCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName, dirVideoRoot,
//                   ffmpegPath, ffmpegMetaData, ffmpegExtraCommand,
//                   taskSystem, taskPath, taskManager, taskUser, taskPassword,
//                   doorGPIO, tmDisplayButtonGPIO, playerName,
//                   objectId, cameraDescr, arrURL, duration, videoCodec, audioCodec,
//                   cameraShowScriptFileName, displayButtonScriptFileName );
//
//        return postURL;
//    }
//
//    protected String postEdit(int id, Map<iColumn, ? extends iData> hmColumnData, Map<String, Object> hmOut ) throws Throwable {
//        String postURL = super.postEdit( id, hmColumnData, hmOut );
//
//        String dirVideoRoot = dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR );
//        String ffmpegPath = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_PATH );
//        String ffmpegMetaData = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_METADATA_FILE );
//        String ffmpegExtraCommand = dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_EXTRA_COMMAND );
//        int taskSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_SYSTEM ) );
//        String taskPath = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PATH );
//        String taskManager = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_MANAGER );
//        Integer doorGPIO = loadDoorButtonGPIO( dataServer.hmConfig );
//        TreeMap<Integer,Integer> tmDisplayButtonGPIO = loadDisplayButtonGPIO( dataServer.hmConfig );
//        String playerName = dataServer.hmConfig.get( VideoFunction.CONFIG_PLAYER_NAME );
//        String cameraShowScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME );
//        String displayButtonScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME );
//
//        //--- стартуем или останавливаем задание в зависимости от изменения значения isEnabled
//        mVideoCamera mvc = (mVideoCamera) model;
//
//        int objectId = ( (DataInt) hmColumnData.get( mvc.getObjectSelector().getColumnObject() ) ).getValue();
//        String cameraDescr = ( (DataString) hmColumnData.get( mvc.getColumnDescr() ) ).getText();
//        //--- ffmpeg нервно реагирует на наличие символа '&' в rstp-url - воспринимает его как следующую команду на выполнение
//        //--- (согласно linux-правилам), поэтому обернём rstp-url в двойные кавычки
//        String url0 = ( ( DataString) hmColumnData.get( mvc.getColumnURL( 0 ) ) ).getText();
//        String url1 = ( ( DataString) hmColumnData.get( mvc.getColumnURL( 1 ) ) ).getText();
//        String[] arrURL = {
//            url0.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url0,
//                                                        ( ( DataString) hmColumnData.get( mvc.getColumnLogin() ) ).getText(),
//                                                        ( ( DataString) hmColumnData.get( mvc.getColumnPassword() ) ).getText(), true ),
//            url1.isEmpty() ? "" : StringFunction.addLoginAndPasswordToURL( url1,
//                                            ( ( DataString) hmColumnData.get( mvc.getColumnLogin() ) ).getText(),
//                                            ( ( DataString) hmColumnData.get( mvc.getColumnPassword() ) ).getText(), true ) };
//        int duration = ( (DataInt) hmColumnData.get( mvc.getColumnDuration() ) ).getValue();
//        int videoCodec = ( ( DataComboBox) hmColumnData.get( mvc.getColumnVideoCodec() ) ).getValue();
//        int audioCodec = ( ( DataComboBox) hmColumnData.get( mvc.getColumnAudioCodec() ) ).getValue();
//
//        editCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName, dirVideoRoot,
//                    ffmpegPath, ffmpegMetaData, ffmpegExtraCommand,
//                    taskSystem, taskPath, taskManager,
//                    doorGPIO, tmDisplayButtonGPIO, playerName,
//                    objectId, cameraDescr, arrURL, duration, videoCodec, audioCodec,
//                    cameraShowScriptFileName, displayButtonScriptFileName );
//
//        return postURL;
//    }
//
//    protected void postDelete(int id, Map<iColumn, ? extends iData> hmColumnData ) throws Throwable {
//        super.postDelete( id, hmColumnData );
//
//        int taskSystem = Integer.parseInt( dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_SYSTEM ) );
//        String taskPath = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_PATH );
//        String taskManager = dataServer.hmConfig.get( VideoFunction.CONFIG_TASK_MANAGER );
//        Integer doorGPIO = loadDoorButtonGPIO( dataServer.hmConfig );
//        TreeMap<Integer,Integer> tmDisplayButtonGPIO = loadDisplayButtonGPIO( dataServer.hmConfig );
//        String playerName = dataServer.hmConfig.get( VideoFunction.CONFIG_PLAYER_NAME );
//        String cameraShowScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME );
//        String displayButtonScriptFileName = dataServer.hmConfig.get( VideoFunction.CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME );
//
//        mVideoCamera mvc = (mVideoCamera) model;
//
//        int objectId = ( (DataInt) hmColumnData.get( mvc.getObjectSelector().getColumnObject() ) ).getValue();
//        String cameraDescr = ( (DataString) hmColumnData.get( mvc.getColumnDescr() ) ).getText();
//
//        deleteCamera( dataWorker, dataServer.rootDirName, dataServer.tempDirName, taskSystem, taskPath, taskManager, doorGPIO, tmDisplayButtonGPIO, playerName,
//                      objectId, cameraDescr, cameraShowScriptFileName, displayButtonScriptFileName );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static boolean checkDescr( CoreDataWorker aDataWorker, String inDescr, int id, int objectId,
//                                      StringBuilder sbErrorValue, StringBuilder sbErrorText ) {
//        if( inDescr.trim().isEmpty() ) {
//            sbErrorValue.append( inDescr );
//            sbErrorText.append( "Описание камеры не заполнено!" );
//            return false;
//        }
//
//        for( int i = 0; i < inDescr.length(); i++ )
//            if( ENABLED_CHARS.indexOf( inDescr.charAt( i ) ) == -1 ) {
//                sbErrorValue.append( inDescr );
//                sbErrorText.append( "В описании камеры допустимы только символы английского алфавита, цифры, круглые скобки, символы тире и подчеркивания!" );
//                return false;
//            }
//
//        //--- проверка на уникальность описания среди камер этого объекта
//        CoreAdvancedResultSet rs = aDataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                " SELECT id FROM VC_camera WHERE object_id = " ).append( objectId )
//                .append( " AND descr = '" ).append( inDescr ).append( "' " )
//                .append( " AND id <> " ).append( id ) );
//        boolean isExist = rs.next();
//        rs.close();
//
//        if( isExist ) {
//            sbErrorValue.append( inDescr );
//            sbErrorText.append( "Описание камеры совпадает с другими камерами!" );
//            return false;
//        }
//        else return true;
//    }
//
//    public static Integer loadDoorButtonGPIO( HashMap<String,String> hmConfig ) {
//        String sDoorGPIO = hmConfig.get( CONFIG_DOOR_BUTTON_GPIO );
//        return sDoorGPIO == null ? null : Integer.parseInt( sDoorGPIO );
//    }
//
//    public static TreeMap<Integer,Integer> loadDisplayButtonGPIO( HashMap<String,String> hmConfig ) {
//        TreeMap<Integer,Integer> tmGPIO = new TreeMap<>();
//        int index = 0;
//        while( true ) {
//            String sGPIO = hmConfig.get( CONFIG_DISPLAY_BUTTON_GPIO_ + index );
//            if( sGPIO == null ) break;
//            tmGPIO.put( index, Integer.parseInt( sGPIO ) );
//            index++;
//        }
//        return tmGPIO;
//    }
//
//    public static void addCamera( CoreDataWorker aDataWorker, String rootDirName, String tempDirName, String dirVideoRoot,
//                                  String ffmpegPath, String ffmpegMetaData, String ffmpegExtraCommand,
//                                  int taskSystem, String taskPath, String taskManager, String taskUser, String taskPassword,
//                                  Integer doorGPIO, TreeMap<Integer,Integer> tmDisplayButtonGPIO, String playerName,
//                                  int objectId, String cameraDescr, String[] arrURL,
//                                  int duration, int videoCodec, int audioCodec,
//                                  String cameraShowScriptFileName, String displayButtonScriptFileName ) throws Throwable {
//
//        File dirCamera = VideoFunction.getCameraDir( dirVideoRoot, objectId, cameraDescr );
//
//        //--- для каждого потока
//        for( int streamIndex = 0; streamIndex < VideoFunction.VIDEO_STREAM_COUNT; streamIndex++ ) {
//            StringBuilder sbStreamName = VideoFunction.getStreamName( objectId, cameraDescr, streamIndex );
//            String streamName = sbStreamName.toString();
//
//            //--- создаём записывающий скрипт (независимо от указания URL потока)
//            String curURL = arrURL[ streamIndex ];
//            File fileFFmpegScript = rewriteFFmpegScript( rootDirName, ffmpegPath, ffmpegMetaData, ffmpegExtraCommand, taskSystem,
//                                                         dirCamera.getCanonicalPath(), sbStreamName, streamIndex,
//                                                         cameraDescr, duration, curURL, videoCodec, audioCodec );
//            boolean isStreamDefined = ! curURL.isEmpty();
//
//            switch( taskSystem ) {
//            case VideoFunction.TASK_SYSTEM_WINDOWS:
//                //--- создаём xml-файл с описанием задания
//                File fileSheduleXML = writeSheduleXML( rootDirName, taskPath, fileFFmpegScript, sbStreamName, isStreamDefined );
//                //--- прописываем задание на старт компа
//                CommonFunction.runCommand( null, taskManager, "/create", "/f", "/tn", streamName,
//                                            "/xml", fileSheduleXML.getCanonicalPath(),
//                                            "/ru", taskUser, "/rp", taskPassword );
//                //--- больше не нужен
//                fileSheduleXML.delete();
//                //--- стартуем задание
//                if( isStreamDefined ) CommonFunction.runCommand( null, taskManager, "/run", "/tn", streamName );
//                break;
//            case VideoFunction.TASK_SYSTEM_SYSTEMD:
//                writeSheduleUnit( rootDirName, taskPath, fileFFmpegScript, sbStreamName );
//                //--- прописывать не надо - достаточно того, что юнит-файл лежит в systemd-папке
//                //...
//                //--- стартуем задание
//                if( isStreamDefined ) {
//                    CommonFunction.runCommand( null, taskManager, "enable", streamName );
//                    CommonFunction.runCommand( null, taskManager, "start", streamName );
//                }
//                break;
//            case VideoFunction.TASK_SYSTEM_INITD:
//                //--- прописывать не надо - достаточно того, что initd-файл лежит в /etc/init.d/
//                //...
//                //--- стартуем задание
//                if( isStreamDefined ) {
//                    //--- enabled == наличие initd-скрипта в папке /etc/init.d/
//                    File initdScript = getInitdScriptFile( taskPath, sbStreamName );
//                    writeInitdScript( fileFFmpegScript, initdScript );
//                    //--- начиная с добавления второй камеры это подвешивает программу
//                    //CommonFunction.runCommand( null, initdScript.getCanonicalPath(), "start" );
//                }
//                break;
//            }
//        }
//        //--- обновляем скрипты локального показа онлайна с камер
//        rewriteCameraShowScript( aDataWorker, rootDirName, tempDirName, taskSystem, playerName, cameraShowScriptFileName );
//        rewriteDisplayButtonScript( rootDirName, tempDirName, taskSystem, doorGPIO, tmDisplayButtonGPIO, playerName, displayButtonScriptFileName );
//    }
//
//    public static void editCamera( CoreDataWorker aDataWorker, String rootDirName, String tempDirName, String dirVideoRoot,
//                                   String ffmpegPath, String ffmpegMetaData, String ffmpegExtraCommand,
//                                   int taskSystem, String taskPath, String taskManager,
//                                   Integer doorGPIO, TreeMap<Integer,Integer> tmDisplayButtonGPIO, String playerName,
//                                   int objectId, String cameraDescr, String[] arrURL,
//                                   int duration, int videoCodec, int audioCodec,
//                                   String cameraShowScriptFileName, String displayButtonScriptFileName ) throws Throwable {
//
//        File dirCamera = VideoFunction.getCameraDir( dirVideoRoot, objectId, cameraDescr );
//
//        //--- для каждого потока
//        for( int streamIndex = 0; streamIndex < VideoFunction.VIDEO_STREAM_COUNT; streamIndex++ ) {
//            StringBuilder sbStreamName = VideoFunction.getStreamName( objectId, cameraDescr, streamIndex );
//            String streamName = sbStreamName.toString();
//
//            //--- обновим записывающий скрипт
//            String curURL = arrURL[ streamIndex ];
//            File fileFFmpegScript = rewriteFFmpegScript( rootDirName, ffmpegPath, ffmpegMetaData, ffmpegExtraCommand, taskSystem,
//                                                         dirCamera.getCanonicalPath(), sbStreamName, streamIndex,
//                                                         cameraDescr, duration, curURL, videoCodec, audioCodec );
//
//            switch( taskSystem ) {
//            case VideoFunction.TASK_SYSTEM_WINDOWS:
//                if( ! curURL.isEmpty() ) {
//                    CommonFunction.runCommand( null, taskManager, "/change", "/enable", "/tn", streamName );
//                    CommonFunction.runCommand( null, taskManager, "/run", "/tn", streamName );
//                }
//                else {
//                    CommonFunction.runCommand( null, taskManager, "/end", "/tn", streamName );
//                    CommonFunction.runCommand( null, taskManager, "/change", "/disable", "/tn", streamName );
//                }
//                break;
//            case VideoFunction.TASK_SYSTEM_SYSTEMD:
//                if( ! curURL.isEmpty() ) {
//                    CommonFunction.runCommand( null, taskManager, "enable", streamName );
//                    CommonFunction.runCommand( null, taskManager, "start", streamName );
//                }
//                else {
//                    CommonFunction.runCommand( null, taskManager, "stop", streamName );
//                    CommonFunction.runCommand( null, taskManager, "disable", streamName );
//                }
//                break;
//            case VideoFunction.TASK_SYSTEM_INITD:
//                if( ! curURL.isEmpty() ) {
//                    //--- enabled == наличие initd-скрипта в папке /etc/init.d/
//                    File initdScript = getInitdScriptFile( taskPath, sbStreamName );
//                    writeInitdScript( fileFFmpegScript, initdScript );
//                    //--- начиная с добавления второй камеры это подвешивает программу
//                    //CommonFunction.runCommand( null, initdScript.getCanonicalPath(), "start" );
//                }
//                else {
//                    File initdScript = getInitdScriptFile( taskPath, sbStreamName );
//                    //--- когда копируют готовую базу на новый комп, ранее существовавших скриптов там нет,
//                    //--- и редактирование старых записей камер становится невозможным
//                    try {
//                        CommonFunction.runCommand( null, initdScript.getCanonicalPath(), "stop" );
//                    }
//                    catch( Throwable t ) {
//                        AdvancedLogger.error( t );
//                    }
//                    initdScript.delete();
//                }
//                break;
//            }
//        }
//        //--- обновляем скрипты локального показа онлайна с камер
//        rewriteCameraShowScript( aDataWorker, rootDirName, tempDirName, taskSystem, playerName, cameraShowScriptFileName );
//        rewriteDisplayButtonScript( rootDirName, tempDirName, taskSystem, doorGPIO, tmDisplayButtonGPIO, playerName, displayButtonScriptFileName );
//    }
//
//    public static void deleteCamera( CoreDataWorker aDataWorker, String rootDirName, String tempDirName,
//                                     int taskSystem, String taskPath, String taskManager,
//                                     Integer doorGPIO, TreeMap<Integer,Integer> tmDisplayButtonGPIO, String playerName,
//                                     int objectId, String cameraDescr,
//                                     String cameraShowScriptFileName, String displayButtonScriptFileName ) throws Throwable {
//        //--- для каждого потока
//        for( int streamIndex = 0; streamIndex < VideoFunction.VIDEO_STREAM_COUNT; streamIndex++ ) {
//            StringBuilder sbStreamName = VideoFunction.getStreamName( objectId, cameraDescr, streamIndex );
//            String streamName = sbStreamName.toString();
//
//            switch( taskSystem ) {
//            case VideoFunction.TASK_SYSTEM_WINDOWS:
//                //--- останавливаем записывающий скрипт
//                CommonFunction.runCommand( null, taskManager, "/end", "/tn", streamName );
//                //--- удаляем его из списка заданий
//                CommonFunction.runCommand( null, taskManager, "/delete", "/f", "/tn", streamName );
//                break;
//            case VideoFunction.TASK_SYSTEM_SYSTEMD:
//                //--- останавливаем записывающий скрипт
//                CommonFunction.runCommand( null, taskManager, "stop", streamName );
//                //--- выключаем его (удалять не обязательно)
//                CommonFunction.runCommand( null, taskManager, "disable", streamName );
//                break;
//            case VideoFunction.TASK_SYSTEM_INITD:
//                File initdScript = getInitdScriptFile( taskPath, sbStreamName );
//                //--- когда копируют готовую базу на новый комп, ранее существовавших скриптов там нет,
//                //--- и удаление старых записей камер становится невозможным
//                try {
//                    CommonFunction.runCommand( null, initdScript.getCanonicalPath(), "stop" );
//                }
//                catch( Throwable t ) {
//                    AdvancedLogger.error( t );
//                }
//                initdScript.delete();
//                break;
//            }
//            //--- удаляем сам скрипт
//            new File( rootDirName, new StringBuilder( sbStreamName )
//                                        .append( taskSystem == VideoFunction.TASK_SYSTEM_WINDOWS ? ".cmd" : ".sh" ).toString() ).delete();
//        }
//
//        //--- обновляем скрипты локального показа онлайна с камер
//        rewriteCameraShowScript( aDataWorker, rootDirName, tempDirName, taskSystem, playerName, cameraShowScriptFileName );
//        rewriteDisplayButtonScript( rootDirName, tempDirName, taskSystem, doorGPIO, tmDisplayButtonGPIO, playerName, displayButtonScriptFileName );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static File rewriteFFmpegScript( String rootDirName,
//                                             String ffmpegPath, String ffmpegMetaData, String ffmpegExtraCommand, int taskSystem,
//                                             String cameraDir, StringBuilder sbStreamName, int streamIndex,
//                                             String cameraDescr, int duration, String url, int videoCodec, int audioCodec ) throws Throwable {
//
//        File fileFFmpegScript = new File( rootDirName,
//                new StringBuilder( sbStreamName ).append( taskSystem == VideoFunction.TASK_SYSTEM_WINDOWS ? ".cmd" : ".sh" ).toString() );
//
//        StringBuilder sbFFMpeg = new StringBuilder( ffmpegPath )
//            //--- обязательно выключаем вывод логов, иначе не получится запустить ffmpeg напрямую из-под себя (для initd-task-system)
//            .append( " -rtsp_transport tcp -stimeout 100000 -loglevel quiet -t " ).append( duration )
//            //.append( " -rtsp_transport tcp -stimeout 100000 -loglevel quiet -report -t " ).append( duration )
//            .append( " -i " ).append( url );
//        if( ffmpegMetaData != null && ! ffmpegMetaData.isEmpty() )
//            sbFFMpeg.append( " -i " ).append( ffmpegMetaData ).append( " -map_metadata 1 " );
//        sbFFMpeg.append( " -vcodec " ).append( videoCodec == VideoFunction.VC_H264 ? "h264" : "copy" );
//        if( audioCodec == VideoFunction.AC_NONE ) sbFFMpeg.append( " -an " );
//        else sbFFMpeg.append( " -acodec " ).append( audioCodec == VideoFunction.AC_AAC ? "aac" : "copy" );
//        sbFFMpeg.append( " -y -flags +global_header" ).append( ' ' ).append( cameraDir );
//
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileFFmpegScript, "Cp1251", false );
//
//        switch( taskSystem ) {
//        case VideoFunction.TASK_SYSTEM_WINDOWS:
//
//            sbFFMpeg.append( '\\' ).append( streamIndex ).append( "\\%beg_dir_name%\\%beg_file_name%.mp4" );
//
//            bwText.write( "@echo off" );
//                bwText.newLine();
//            //--- текущую/рабочую папку зададим в параметрах задания
//            //bwText.write( "cd /d " );
//            //bwText.write( dirCamera.getCanonicalPath() );
//            //bwText.newLine();
//            bwText.write( ":a" );
//                bwText.newLine();
//            bwText.write( "set beg_date=%DATE: =0%" );
//                bwText.newLine();
//            bwText.write( "set beg_time=%TIME: =0%" );
//                bwText.newLine();
//            bwText.write( "for /f \"tokens=1-3 delims=/-:., \" %%a in ( \"%beg_date%\" ) do set beg_dir_name=%%c-%%b-%%a" );
//                bwText.newLine();
//            bwText.write( "for /f \"tokens=1-3 delims=/-:., \" %%a in ( \"%beg_time%\" ) do set beg_file_name=%%a-%%b-%%c" );
//                bwText.newLine();
//            bwText.write( new StringBuilder( "md " ).append( cameraDir ).append( '\\' ).append( streamIndex )
//                                                                        .append( "\\%beg_dir_name%" ).toString() );
//                bwText.newLine();
//            bwText.write( sbFFMpeg.toString() );
//                bwText.newLine();
//            bwText.write( "if %errorlevel% == 0 goto b" );
//                bwText.newLine();
//            bwText.write( "timeout /t 1" );
//                bwText.newLine();
//            bwText.write( "goto a" );
//                bwText.newLine();
//            bwText.write( ":b" );
//                bwText.newLine();
//            bwText.write( "set end_date=%DATE: =0%" );
//                bwText.newLine();
//            bwText.write( "set end_time=%TIME: =0%" );
//                bwText.newLine();
//            bwText.write( "for /f \"tokens=1-3 delims=/-:., \" %%a in ( \"%end_date%\" ) do set end_dir_name=%%c-%%b-%%a" );
//                bwText.newLine();
//            bwText.write( "for /f \"tokens=1-3 delims=/-:., \" %%a in ( \"%end_time%\" ) do set end_file_name=%%a-%%b-%%c" );
//                bwText.newLine();
//            bwText.write( new StringBuilder( "ren " )
//                                .append( cameraDir ).append( '\\' ).append( streamIndex ).append( "\\%beg_dir_name%\\%beg_file_name%.mp4 " )
//                                .append( "%beg_dir_name%-%beg_file_name%-%end_dir_name%-%end_file_name%.mp4" ).toString() );
//                bwText.newLine();
//            //--- возможная связь/привязка к внешней системе
//            if( ffmpegExtraCommand != null && ! ffmpegExtraCommand.isEmpty() ) {
//                bwText.write( new StringBuilder( "call " ).append( ffmpegExtraCommand )
//                                        .append( ' ' ).append( cameraDescr )
//                                        .append( ' ' ).append( streamIndex )
//                                        .append( ' ' ).append( cameraDir ).toString() );
//                bwText.newLine();
//            }
//            bwText.write( "goto a" );
//                bwText.newLine();
//            break;
//
//        case VideoFunction.TASK_SYSTEM_SYSTEMD:
//        case VideoFunction.TASK_SYSTEM_INITD:
//
//            sbFFMpeg.append( '/' ).append( streamIndex ).append( "/$beg_dir_name/$beg_file_name.mp4" );
//
//            bwText.write( taskSystem == VideoFunction.TASK_SYSTEM_SYSTEMD ? "#!/bin/bash" : "#!/bin/sh" );
//                bwText.newLine();
//            if( taskSystem == VideoFunction.TASK_SYSTEM_INITD ) {
//                bwText.write( "cd " );
//                bwText.write( rootDirName );
//                    bwText.newLine();
//            }
//            bwText.write( "while true" );
//                bwText.newLine();
//            bwText.write( "do" );
//                bwText.newLine();
//            bwText.write( "beg_dir_name=$(date +%Y-%m-%d)" );
//                bwText.newLine();
//            bwText.write( "beg_file_name=$(date +%H-%M-%S)" );
//                bwText.newLine();
//            bwText.write( "export beg_dir_name" );
//                bwText.newLine();
//            bwText.write( "export beg_file_name" );
//                bwText.newLine();
//            bwText.write( new StringBuilder( "mkdir -p " ).append( cameraDir ).append( '/' ).append( streamIndex )
//                                                                           .append( "/$beg_dir_name" ).toString() );
//                bwText.newLine();
//            bwText.write( sbFFMpeg.toString() );
//                bwText.newLine();
//            //--- слишком много разных ненулевых/успешных кодов выхода
//            //bwText.write( "if [ $? -eq 0 ]; then" );
//            //    bwText.newLine();
//            bwText.write( "end_dir_name=$(date +%Y-%m-%d)" );
//                bwText.newLine();
//            bwText.write( "end_file_name=$(date +%H-%M-%S)" );
//                bwText.newLine();
//            bwText.write( "export end_dir_name" );
//                bwText.newLine();
//            bwText.write( "export end_file_name" );
//                bwText.newLine();
//
//            bwText.write( new StringBuilder( "mv -f " )
//                                        .append( cameraDir ).append( '/' ).append( streamIndex )
//                                        .append( "/$beg_dir_name/$beg_file_name.mp4 " )
//                                        .append( cameraDir ).append( '/' ).append( streamIndex )
//                                        .append( "/$beg_dir_name/$beg_dir_name-$beg_file_name-$end_dir_name-$end_file_name.mp4" ).toString() );
//                bwText.newLine();
//            //--- возможная связь/привязка к внешней системе
//            if( ffmpegExtraCommand != null && ! ffmpegExtraCommand.isEmpty() ) {
//                bwText.write( new StringBuilder( ffmpegExtraCommand )
//                                        .append( ' ' ).append( cameraDescr.isEmpty() ? '-' : cameraDescr )
//                                        .append( ' ' ).append( streamIndex )
//                                        .append( ' ' ).append( cameraDir ).toString() );
//                bwText.newLine();
//            }
//            //--- слишком много разных ненулевых/успешных кодов выхода
//            //bwText.write( "else" );
//            //    bwText.newLine();
//            //bwText.write( "sleep 1" );
//            //    bwText.newLine();
//            //bwText.write( "fi" );
//            //    bwText.newLine();
//            bwText.write( "done" );
//                bwText.newLine();
//            bwText.write( "exit 0" );
//                bwText.newLine();
//
//            break;
//        }
//        bwText.close();
//
//        //--- требуемо для линюксов - сделать скрипт исполняемым
//        fileFFmpegScript.setExecutable( true );
//
//        return fileFFmpegScript;
//    }
//
//    private static File writeSheduleXML( String rootDirName, String taskPath, File fileFFmpegScript, StringBuilder sbStreamName,
//                                         boolean isEnabled ) throws Throwable {
//
//        File fileSheduleXML = new File( taskPath, new StringBuilder( sbStreamName ).append( ".xml" ).toString() );
//
//        //--- Суть проблемы: UTF-16 бывает двух видов:
//        //--- little_endian - стандартный режим для windows, его ждёт виндовый шедулер
//        //--- big_endian - в нём пишет Java, он не воспринимается шедулером и я пока не нашёл способа переключить ByteOrder в данном случае
//        //--- Пробовал варианты указать другие encoding: Cp1251, win-1251, ANSI и т.п. - не прокатывают.
//        //--- Решение: файл пишем как обычно в "Cp1251", но в теле XML-файла encoding вообще не указываем.
//        //--- Так всё проходит.
//
//        //BufferedWriter bwText = CommonFunction.getFileWriter( fileSheduleXML, "UTF-16", false );
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileSheduleXML, "Cp1251", false );
//
//        //bwText.write( "<?xml version=\"1.0\" encoding=\"UTF-16\"?>" );
//        bwText.write( "<?xml version=\"1.0\"?>" );
//        bwText.newLine();
//        bwText.write( "<Task version=\"1.3\" xmlns=\"http://schemas.microsoft.com/windows/2004/02/mit/task\">" );
//        bwText.newLine();
//
////    <Date>2014-05-01T21:58:43.3627863</Date>
////    <SecurityDescriptor>D:(A;;FA;;;BA)(A;;FA;;;SY)(A;;FRFX;;;WD)</SecurityDescriptor>
//
//        bwText.write( "<RegistrationInfo>" );
//        bwText.newLine();
//        bwText.write( "<Author>Pulsar Server</Author>" );
//        bwText.newLine();
//        bwText.write( "</RegistrationInfo>" );
//        bwText.newLine();
//
//        bwText.write( "<Triggers>" );
//        bwText.newLine();
//        bwText.write( "<BootTrigger>" );
//        bwText.newLine();
//        bwText.write( "<Enabled>true</Enabled>" );
//        bwText.newLine();
//        bwText.write( "</BootTrigger>" );
//        bwText.newLine();
//        bwText.write( "</Triggers>" );
//        bwText.newLine();
//
//        bwText.write( "<Principals>" );
//        bwText.newLine();
//        bwText.write( "<Principal id=\"Author\">" );
//        bwText.newLine();
////      <UserId>SERVER-14\Администратор</UserId>
////      <LogonType>Password</LogonType>
////        bwText.write( "___" );
////        bwText.newLine();
////        bwText.write( "___" );
////        bwText.newLine();
//        bwText.write( "<RunLevel>HighestAvailable</RunLevel>" );
//        bwText.newLine();
//        bwText.write( "</Principal>" );
//        bwText.newLine();
//        bwText.write( "</Principals>" );
//        bwText.newLine();
//
//        bwText.write( "<Settings>" );
//        bwText.newLine();
//        //--- StopExisting не лучше будет?
//        bwText.write( "<MultipleInstancesPolicy>IgnoreNew</MultipleInstancesPolicy>" );
//        bwText.newLine();
//        bwText.write( "<DisallowStartIfOnBatteries>false</DisallowStartIfOnBatteries>" );
//        bwText.newLine();
//        bwText.write( "<StopIfGoingOnBatteries>false</StopIfGoingOnBatteries>" );
//        bwText.newLine();
//        bwText.write( "<AllowHardTerminate>true</AllowHardTerminate>" );
//        bwText.newLine();
//        bwText.write( "<StartWhenAvailable>false</StartWhenAvailable>" );
//        bwText.newLine();
//        bwText.write( "<RunOnlyIfNetworkAvailable>false</RunOnlyIfNetworkAvailable>" );
//        bwText.newLine();
//        bwText.write( "<IdleSettings>" );
//        bwText.newLine();
//        bwText.write( "<StopOnIdleEnd>false</StopOnIdleEnd>" );
//        bwText.newLine();
//        bwText.write( "<RestartOnIdle>false</RestartOnIdle>" );
//        bwText.newLine();
//        bwText.write( "</IdleSettings>" );
//        bwText.newLine();
//        bwText.write( "<AllowStartOnDemand>true</AllowStartOnDemand>" );
//        bwText.newLine();
//        bwText.write( new StringBuilder( "<Enabled>" ).append( isEnabled ).append( "</Enabled>" ).toString() );
//        bwText.newLine();
//        bwText.write( "<Hidden>false</Hidden>" );
//        bwText.newLine();
//        bwText.write( "<RunOnlyIfIdle>false</RunOnlyIfIdle>" );
//        bwText.newLine();
//        bwText.write( "<DisallowStartOnRemoteAppSession>false</DisallowStartOnRemoteAppSession>" );
//        bwText.newLine();
//        bwText.write( "<UseUnifiedSchedulingEngine>false</UseUnifiedSchedulingEngine>" );   //???
//        bwText.newLine();
//        bwText.write( "<WakeToRun>false</WakeToRun>" );
//        bwText.newLine();
//        bwText.write( "<ExecutionTimeLimit>PT0S</ExecutionTimeLimit>" );
//        bwText.newLine();
//        bwText.write( "<Priority>7</Priority>" );
//        bwText.newLine();
//        bwText.write( "</Settings>" );
//        bwText.newLine();
//
//        //--- dataServer.hmConfig.get( mVideoCamera.CONFIG_TASK_USER ) ?
//        bwText.write( "<Actions Context=\"Author\">" );
//        bwText.newLine();
//        bwText.write( "<Exec>" );
//        bwText.newLine();
//        bwText.write( new StringBuilder( "<Command>" ).append( fileFFmpegScript.getCanonicalPath() )
//                                .append( "</Command>" ).toString() );
//        bwText.newLine();
//        bwText.write( new StringBuilder( "<WorkingDirectory>" ).append( rootDirName )
//                                .append( "</WorkingDirectory>" ).toString() );
//        bwText.newLine();
////      <Arguments> arguments list </Arguments>
//        bwText.write( "</Exec>" );
//        bwText.newLine();
//        bwText.write( "</Actions>" );
//        bwText.newLine();
//        bwText.write( "</Task>" );
//        bwText.newLine();
//
////        bwText.write( "___" );
////        bwText.newLine();
//
//        bwText.close();
//
//        return fileSheduleXML;
//    }
//
//    private static void writeSheduleUnit( String rootDirName, String taskPath, File fileFFmpegScript, StringBuilder sbStreamName ) throws Throwable {
//
//        File fileSheduleUnit = new File( taskPath, new StringBuilder( sbStreamName ).append( ".service" ).toString() );
//
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileSheduleUnit, "Cp1251", false );
//
//        bwText.write( "[Unit]" );
//        bwText.newLine();
//
//        bwText.write( new StringBuilder( "Description=" ).append( sbStreamName ).toString() );
//        bwText.newLine();
//
//        bwText.write( "[Service]" );
//        bwText.newLine();
//
//        bwText.write( "Type=simple" );
//        bwText.newLine();
//
//        //bwText.write( "User=vc или root" );
//        //bwText.newLine();
//        //bwText.write( "Group=myunit - я х.з. какая группа" );
//        //bwText.newLine();
//
//        bwText.write( new StringBuilder( "WorkingDirectory=" ).append( rootDirName ).toString() );
//        bwText.newLine();
//        bwText.write( new StringBuilder( "ExecStart=" ).append( fileFFmpegScript.getCanonicalPath() ).toString() );
//        bwText.newLine();
//
//        bwText.write( "[Install]" );
//        bwText.newLine();
//
//        bwText.write( "WantedBy=multi-user.target" );
//        bwText.newLine();
//
////        bwText.write( "___" );
////        bwText.newLine();
//
//        bwText.close();
//    }
//
//    private static File getInitdScriptFile( String taskPath, StringBuilder sbStreamName ) {
//        return new File( taskPath, new StringBuilder( "S92_" ).append( sbStreamName ).toString() );
//    }
//    private static void writeInitdScript( File fileFFmpegScript, File fileInitdScript ) throws Throwable {
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileInitdScript, "Cp1251", false );
//
//        bwText.write( "#!/bin/sh" );
//        bwText.newLine();
//
//        bwText.write( "case \"$1\" in" );
//        bwText.newLine();
//
//        bwText.write( "start)" );
//        bwText.newLine();
//
//        //bwText.write( "nohup " );
//        bwText.write( fileFFmpegScript.getCanonicalPath() );
//        bwText.write( " &" );
//        bwText.newLine();
//
//        bwText.write( ";;" );
//        bwText.newLine();
//
//        bwText.write( "stop)" );
//        bwText.newLine();
//
//        bwText.write( "killall " );
//        bwText.write( fileFFmpegScript.getName() );
//        bwText.newLine();
//
//        bwText.write( ";;" );
//        bwText.newLine();
//
//        bwText.write( "restart|reload)" );
//        bwText.newLine();
//
//        bwText.write( "\"$0\" stop" );
//        bwText.newLine();
//
//        bwText.write( "\"$0\" start" );
//        bwText.newLine();
//
//        bwText.write( ";;" );
//        bwText.newLine();
//
//        bwText.write( "*)" );
//        bwText.newLine();
//
//        bwText.write( "echo \"Usage: $0 {start|stop|restart}\"" );
//        bwText.newLine();
//
//        bwText.write( "exit 1" );
//        bwText.newLine();
//
//        bwText.write( "esac" );
//        bwText.newLine();
//
//        bwText.write( "exit $?" );
//        bwText.newLine();
//
////        bwText.write( "___" );
////        bwText.newLine();
//
//        bwText.close();
//
//        //--- требуемо для линюксов - сделать скрипт исполняемым
//        fileInitdScript.setExecutable( true );
//    }
//
//    private static void rewriteCameraShowScript( CoreDataWorker aDataWorker, String rootDirName, String tempDirName,
//                                                 int taskSystem, String playerName, String cameraShowScriptFileName ) throws Throwable {
//        //--- если параметр не задан - выходим сразу
//        if( cameraShowScriptFileName == null ) return;
//
//        File fileCameraShowScript = new File( rootDirName, cameraShowScriptFileName );
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileCameraShowScript, "Cp1251", false );
//
//        switch( taskSystem ) {
//        case VideoFunction.TASK_SYSTEM_WINDOWS:
//            //--- скрипты под винду не нужны - там есть GUI
//            break;
//
//        case VideoFunction.TASK_SYSTEM_SYSTEMD:
//            //--- скрипты под убунту не нужны - там есть GUI
//            break;
//
//        case VideoFunction.TASK_SYSTEM_INITD:
//
//            bwText.write( "#!/bin/sh" );
//                bwText.newLine();
//            bwText.write( "cd " );
//            bwText.write( rootDirName );
//                bwText.newLine();
//
//            //--- убираем мигание курсора, если собираемся показывать видео
//            bwText.write( "echo 0 > /sys/class/graphics/fbcon/cursor_blink" );
//                bwText.newLine();
//
//            //--- создаём файл, если его не было
//            bwText.write( "echo 0 > " );
//            bwText.write( tempDirName );
//            bwText.write( "/camera_show.index" );
//                bwText.newLine();
//
//            bwText.write( "while true" );
//                bwText.newLine();
//            bwText.write( "do" );
//                bwText.newLine();
//
//            //--- внутренний цикл взятия значения с проверкой на ошибочное взятие/значение
//            //--- (когда display_button.sh пишет в door_status.index,
//            //--- то camera_show.sh не может взять правильное значение из файла в это время)
//            bwText.write( "while true" );
//                bwText.newLine();
//            bwText.write( "do" );
//                bwText.newLine();
//            bwText.write( "door_status=`cat " );
//            bwText.write( tempDirName );
//            bwText.write( "/door_status.index`" );
//                bwText.newLine();
//            bwText.write( "if [ $door_status -eq 0 ]; then" );
//                bwText.newLine();
//            bwText.write( "break" );
//                bwText.newLine();
//            bwText.write( "elif [ $door_status -ne 0 ]; then" );
//                bwText.newLine();
//            bwText.write( "break" );
//                bwText.newLine();
//            bwText.write( "fi" );
//                bwText.newLine();
//            //--- 0.1 сек между "неправильными" запросами, чтобы зря не мучить систему
//            bwText.write( "usleep 100000" );
//                bwText.newLine();
//            bwText.write( "done" );
//                bwText.newLine();
//
//            bwText.write( "camera_index=`cat " );
//            bwText.write( tempDirName );
//            bwText.write( "/camera_show.index`" );
//                bwText.newLine();
//
//            //--- реакция на дверь
//            bwText.write( "if [ $door_status -ne 0 ]; then" );
//                bwText.newLine();
//            bwText.write( "cat /dev/zero > /dev/fb0" );
//                bwText.newLine();
//            //--- 1 сек - достаточное время реакции на открытие двери
//            bwText.write( "usleep 1000000" );
//                bwText.newLine();
//
//            CoreAdvancedResultSet rs = aDataWorker.alStm.get( 0 ).executeQuery(
//                " SELECT url_0 , login , pwd FROM VC_camera WHERE id <> 0 ORDER BY descr " );
//            int index = 0;
//            while( rs.next() ) {
//                bwText.write( "elif [ $camera_index -eq " );
//                bwText.write( Integer.toString( index ) );
//                bwText.write( " ]; then" );
//                    bwText.newLine();
//
//                bwText.write( playerName );
//                //--- старый вариант с паузой
//                //bwText.write( " playbin uri=" );
//                //bwText.write( StringFunction.addLoginAndPasswordToURL( rs.getString( 1 ), rs.getString( 2 ), rs.getString( 3 ), true ) );
//                bwText.write( " rtspsrc location=" );
//                bwText.write( rs.getString( 1 ) );
//                bwText.write( " user-id=" );
//                bwText.write( rs.getString( 2 ) );
//                bwText.write( " user-pw=" );
//                bwText.write( rs.getString( 3 ) );
//                bwText.write( " latency=10 protocols=0x00000004 tcp-timeout=1000000 do-retransmission=false ! decodebin ! imxg2dvideosink force-aspect-ratio=0 sync=false" );
//                    bwText.newLine();
//
//                index++;
//            }
//            rs.close();
//
//            bwText.write( "fi" );
//                bwText.newLine();
//            bwText.write( "done" );
//                bwText.newLine();
//            bwText.write( "exit 0" );
//                bwText.newLine();
//
//            break;
//        }
//        bwText.close();
//
//        //--- требуемо для линюксов - сделать скрипт исполняемым
//        fileCameraShowScript.setExecutable( true );
//    }
//
//    private static void rewriteDisplayButtonScript( String rootDirName, String tempDirName, int taskSystem,
//                                                    Integer doorGPIO, TreeMap<Integer,Integer> tmDisplayButtonGPIO,
//                                                    String playerName, String displayButtonScriptFileName ) throws Throwable {
//        //--- если параметр не задан - выходим сразу
//        if( displayButtonScriptFileName == null ) return;
//
//        int doorGPIOAbs = doorGPIO == null ? 0 : Math.abs( doorGPIO );
//
//        TreeMap<Integer,Integer> tmGPIOAbs = new TreeMap<>();
//        for( Integer index : tmDisplayButtonGPIO.keySet() )
//            tmGPIOAbs.put( index, Math.abs( tmDisplayButtonGPIO.get( index ) ) );
//
//        File fileDisplayButtonScript = new File( rootDirName, displayButtonScriptFileName );
//        BufferedWriter bwText = CommonFunction.getFileWriter( fileDisplayButtonScript, "Cp1251", false );
//
//        switch( taskSystem ) {
//        case VideoFunction.TASK_SYSTEM_WINDOWS:
//            //--- скрипты под винду не нужны - там есть GUI
//            break;
//
//        case VideoFunction.TASK_SYSTEM_SYSTEMD:
//            //--- скрипты под убунту не нужны - там есть GUI
//            break;
//
//        case VideoFunction.TASK_SYSTEM_INITD:
//
//            bwText.write( "#!/bin/sh" );
//                bwText.newLine();
//            bwText.write( "cd " );
//            bwText.write( rootDirName );
//                bwText.newLine();
//
//            if( doorGPIO != null ) {
//                bwText.write( VideoFunction.GPIO_IN_INIT );
//                bwText.write( " " );
//                bwText.write( Integer.toString( doorGPIOAbs ) );
//                    bwText.newLine();
//            }
//
//            for( Integer index : tmDisplayButtonGPIO.keySet() ) {
//                bwText.write( VideoFunction.GPIO_IN_INIT );
//                bwText.write( " " );
//                bwText.write( tmGPIOAbs.get( index ).toString() );
//                    bwText.newLine();
//            }
//
//            bwText.write( "camera_index=`cat " );
//            bwText.write( tempDirName );
//            bwText.write( "/camera_show.index`" );
//                bwText.newLine();
//
//            bwText.write( "while true" );
//                bwText.newLine();
//            bwText.write( "do" );
//                bwText.newLine();
//
//            if( doorGPIO != null ) {
//                bwText.write( "door_status=`cat /sys/class/gpio/gpio" );
//                bwText.write( Integer.toString( doorGPIOAbs ) );
//                bwText.write( "/value`" );
//                    bwText.newLine();
//
//                bwText.write( "if [ $door_status " );
//                bwText.write( doorGPIO > 0 ? "-ne" : "-eq" );
//                bwText.write( " 0 ]; then" );
//                    bwText.newLine();
//
//                bwText.write( "echo 1 > " );
//                bwText.write( tempDirName );
//                bwText.write( "/door_status.index" );
//                    bwText.newLine();
//
//                bwText.write( "ps | grep " );
//                bwText.write( playerName );
//                bwText.write( " | grep -v grep | awk '{print $1}' | xargs kill -2 $1" );
//                    bwText.newLine();
//
//                bwText.write( "else" );
//                    bwText.newLine();
//
//                bwText.write( "echo 0 > " );
//                bwText.write( tempDirName );
//                bwText.write( "/door_status.index" );
//                    bwText.newLine();
//
//                bwText.write( "fi" );
//                    bwText.newLine();
//            }
//
//            for( Integer index : tmDisplayButtonGPIO.keySet() ) {
//                bwText.write( "button_status=`cat /sys/class/gpio/gpio" );
//                bwText.write( tmGPIOAbs.get( index ).toString() );
//                bwText.write( "/value`" );
//                    bwText.newLine();
//
//                bwText.write( "if [ $button_status " );
//                bwText.write( tmDisplayButtonGPIO.get( index ) > 0 ? "-ne" : "-eq" );
//                bwText.write( " 0 ]; then" );
//                    bwText.newLine();
//
//                bwText.write( "if [ $camera_index -ne " );
//                bwText.write( Integer.toString( index ) );
//                bwText.write( " ]; then" );
//                    bwText.newLine();
//
//                bwText.write( "camera_index=" );
//                bwText.write( Integer.toString( index ) );
//                    bwText.newLine();
//
//                bwText.write( "echo " );
//                bwText.write( Integer.toString( index ) );
//                bwText.write( " > " );
//                bwText.write( tempDirName );
//                bwText.write( "/camera_show.index" );
//                    bwText.newLine();
//
//                bwText.write( "ps | grep " );
//                bwText.write( playerName );
//                bwText.write( " | grep -v grep | awk '{print $1}' | xargs kill -2 $1" );
//                    bwText.newLine();
//
//                bwText.write( "fi" );
//                    bwText.newLine();
//
//                bwText.write( "fi" );
//                    bwText.newLine();
//            }
//
//            //--- 0.1 сек - достаточное время реакции на кнопку выбора показа другой камеры
//            bwText.write( "usleep 100000" );
//                bwText.newLine();
//
//            bwText.write( "done" );
//                bwText.newLine();
//            bwText.write( "exit 0" );
//                bwText.newLine();
//
//            break;
//        }
//        bwText.close();
//
//        //--- требуемо для линюксов - сделать скрипт исполняемым
//        fileDisplayButtonScript.setExecutable( true );
//    }
//
//}
//

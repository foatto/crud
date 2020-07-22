//package foatto.mms.del;
//
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//import foatto.core_server.app.video.server.VideoFunction;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.StringTokenizer;
//
//public abstract class CoreCameraStatus extends CoreRelayWorker {
//
//    private static final String CONFIG_MAX_VIDEO_SIZE = "max_video_size";
//    private static final String CONFIG_CYCLE_PAUSE = "cycle_pause";
//    private static final long CONFIG_RELOAD_INTERVAL = 15 * 60 * 1000L;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private String dirVideoRoot = null;
//    private HashSet<String> hsStorageExt = new HashSet<>();
//    private long maxVideoSize = 0;
//    private long cyclePause = 0;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private long lastConfigReloadTime = 0;
//    private ArrayList<Integer> alCameraObjectID = new ArrayList<>();
//    private ArrayList<String> alCameraDescr = new ArrayList<>();
//    private ArrayList<Long> alCameraDuration = new ArrayList<>();
//
////---------------------------------------------------------------------------------------------------------------
//
//    protected CoreCameraStatus( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        dirVideoRoot = hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR );
//
//        StringTokenizer st = new StringTokenizer( hmConfig.get( VideoFunction.CONFIG_VIDEO_EXT ), ",; " );
//        while( st.hasMoreTokens() ) hsStorageExt.add( st.nextToken().toLowerCase() );
//
//        maxVideoSize = Integer.parseInt( hmConfig.get( CONFIG_MAX_VIDEO_SIZE ) ) * 1_000_000L;
//
//        cyclePause = Integer.parseInt( hmConfig.get( CONFIG_CYCLE_PAUSE ) ) * 1000L;
//
//        //--- стартовое/первоначальное включение камер
//        try {
//            writeSingleCoil( relayCameraPower, true );
//        }
//        //!!! временно на период перехода на Kotlin
//        catch( Throwable t ) {
//            t.printStackTrace();
//            return;
//        }
//    }
//
//    protected boolean isRunOnce() { return false; }
//
//    protected void cycle() throws Throwable {
//
//        //--- периодически перезагружаем/обновляем определения камер и информацию по объекту
//        if( System.currentTimeMillis() - lastConfigReloadTime > CONFIG_RELOAD_INTERVAL ) {
//            alCameraObjectID.clear();
//            alCameraDescr.clear();
//            alCameraDuration.clear();
//
//            CoreAdvancedResultSet rs = alStm.get( 0 ).executeQuery(
//                        " SELECT object_id , descr , duration , url_time , login , pwd  FROM VC_camera WHERE id <> 0 ORDER BY descr " );
//            while( rs.next() ) {
//                int p = 1;
//                alCameraObjectID.add( rs.getInt( p++ ) );
//                alCameraDescr.add( rs.getString( p++ ) );
//                alCameraDuration.add( rs.getInt( p++ ) * 1000L );
//                String urlTime = rs.getString( p++ );
//                String login = rs.getString( p++ );
//                String pwd = rs.getString( p++ );
//
//                //--- ручная установка времени на камере
//                if( urlTime != null && ! urlTime.isEmpty() ) VideoFunction.setCameraTime( urlTime, login, pwd );
//            }
//            rs.close();
//
//            lastConfigReloadTime = System.currentTimeMillis();
//        }
//
//        //--- по каждой папке из списка видеохранилищ
//        for( int cIndex = 0; cIndex < alCameraObjectID.size(); cIndex++ ) {
//            File dirCamera = VideoFunction.getCameraDir( dirVideoRoot, alCameraObjectID.get( cIndex ), alCameraDescr.get( cIndex ) );
//            File dirStream = new File( dirCamera, "0" );
//
//            File lastFile = null;
//            if( dirStream.exists() ) {
//                //--- по каждой папке с датой
//                File[] arrDateDir = dirStream.listFiles();
//                for( File dateDir : arrDateDir )
//                    if( dateDir.isDirectory() ) {
//                        //--- по каждому видеофайлу
//                        File[] arrVideoFile = dateDir.listFiles();
//                        for( File videoFile : arrVideoFile )
//                            if( videoFile.isFile() ) {
//                                String fileName = videoFile.getName();
//                                //--- проверка на проверяемые расширения видеофайлов
//                                int dotPos = fileName.lastIndexOf( '.' );
//                                if( dotPos == -1 ) continue;
//                                String ext = fileName.substring( dotPos + 1 ).toLowerCase();
//                                if( ! hsStorageExt.contains( ext ) ) continue;
//
//                                long lastModified = videoFile.lastModified();
//                                if( lastFile == null || lastModified > lastFile.lastModified() )
//                                    lastFile = videoFile;
//                            }
//                    }
//            }
//            AdvancedLogger.debug( "System.currentTimeMillis() = " + System.currentTimeMillis() );
//            AdvancedLogger.debug( "lastFileTime = " + ( lastFile == null ? 0 : lastFile.lastModified() ) );
//            AdvancedLogger.debug( "alCameraDuration.get( cIndex ) = " + alCameraDuration.get( cIndex ) );
//            AdvancedLogger.debug( "cyclePause = " + cyclePause );
//
//            boolean result = System.currentTimeMillis() - ( lastFile == null ? 0 : lastFile.lastModified() )
//                             < alCameraDuration.get( cIndex ) + cyclePause;
//            AdvancedLogger.info( alCameraDescr.get( cIndex ) + " = " + result );
//            setCameraLED( cIndex, result );
//            //--- прибьём процесс, если он слишком долгий
//            //--- (если relayIP не задан, то работаем на buildroot-linux)
//            if( relayIP == null && lastFile != null && ! result ) {
//                StringTokenizer st = new StringTokenizer( lastFile.getName(), "-." );
//                //--- реагируем только если это имя записываемого сейчас файла (в формате yyyy-mm-dd.mp4)
//                if( st.countTokens() == 4 ) {
//                    CommonFunction.runCommand( null, "ps", "|", "grep", "ffmpeg", "|", "grep", lastFile.getCanonicalPath(), "|",
//                            "grep", "-v", "grep", "|", "awk", "'{print $1}'", "|", "xargs", "kill", "-2", "$1" );
//                    if( maxVideoSize > 0 && lastFile.lastModified() > maxVideoSize )
//                        lastFile.delete();
//                }
//            }
//        }
//
//        Thread.sleep( cyclePause );
//    }
//
////---------------------------------------------------------------------------------------------------------------
//
//    private void setCameraLED( int cameraIndex, boolean status ) {
//        try {
//            writeSingleCoil( hmRelayCameraLed.get( cameraIndex ), status );
//        }
//        catch( Throwable t ) {
//            AdvancedLogger.error( t );
//            //--- надо бы mp3-шку какую проиграть...
//        }
//    }
//
//}

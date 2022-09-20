//package foatto.core_server.app.video.server.document;
//
//import foatto.core.app.iCoreAppContainer;
//import foatto.core.app.video.CameraDef;
//import foatto.core.app.video.StreamDef;
//import foatto.core.app.video.VideoParameter;
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.core.util.*;
//import foatto.core_server.app.server.UserConfig;
//import foatto.core_server.app.video.server.VideoFunction;
//import foatto.core_server.app.video.server.VideoStartData;
//import foatto.core_server.ds.nio.CoreDataServer;
//import foatto.core_server.ds.nio.CoreDataWorker;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class sdcVideo {
//
//    //--- пока используется два видеопотока - основной и дополнительный
//    private CoreDataServer dataServer = null;
//    private CoreDataWorker dataWorker = null;
//    private ConcurrentHashMap<String,Object> chmSession = null;
//    private UserConfig userConfig = null;     // ссылка на конфигурацию загруженного пользователя
//
//    private TimeZone timeZone = null;
//
//    public void init( CoreDataServer aDataServer, CoreDataWorker aDataWorker,
//                      ConcurrentHashMap<String,Object> aChmSession, UserConfig aUserConfig ) throws Throwable {
//        dataServer = aDataServer;
//        dataWorker = aDataWorker;
//        chmSession = aChmSession;
//        //--- получить конфигурацию по подключенному пользователю
//        userConfig = aUserConfig;
//
//        timeZone = StringFunction.getTimeZone( Integer.parseInt( userConfig.getUserProperty( iCoreAppContainer.UP_TIME_OFFSET ) ) );
//    }
//
//    public boolean doAction( AdvancedByteBuffer bbIn, AdvancedByteBuffer bbOut ) throws Throwable {
//        String action = bbIn.getShortString();
//        if( action.equals( VideoParameter.ACTION_GET_COORDS ) ) return getCoords( bbIn, bbOut );
//        else if( action.equals( VideoParameter.ACTION_GET_FILE_LIST ) ) return doGetFileList( bbIn, bbOut );
//        else if( action.equals( VideoParameter.ACTION_LOAD_IMAGE ) ) return doLoadImage( bbIn, bbOut );
//        else throw new Throwable( new StringBuilder( "Unknown video action = " ).append( action ).toString() );
//    }
//
//    protected boolean getCoords( AdvancedByteBuffer bbIn, AdvancedByteBuffer bbOut ) throws Throwable {
//        long begTime;
//        long endTime;
//
//        String videoStartDataID = bbIn.getShortString();
//        VideoStartData sd = (VideoStartData) chmSession.get( VideoParameter.VIDEO_START_DATA + videoStartDataID );
//
//        if( sd.rangeType == 0 ) {
//            begTime = StringFunction.Arr_DateTime( timeZone, sd.arrBegDT );
//            endTime = StringFunction.Arr_DateTime( timeZone, sd.arrEndDT );
//        }
//        else {
//            GregorianCalendar begDateTime = new GregorianCalendar( timeZone );
//            begDateTime.add( GregorianCalendar.SECOND, - sd.rangeType );
//            GregorianCalendar endDateTime = new GregorianCalendar( timeZone );
//            begTime = begDateTime.getTimeInMillis();
//            endTime = endDateTime.getTimeInMillis();
//        }
//        bbOut.putInt( (int) ( begTime / 1000 ) );
//        bbOut.putInt( (int) ( endTime / 1000 ) );
//
//        return true;
//    }
//
//    protected boolean doGetFileList( AdvancedByteBuffer bbIn, AdvancedByteBuffer bbOut ) throws Throwable {
//        String dirVideoRoot = dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR );
//
//        String videoStartDataID = bbIn.getShortString();
//        VideoStartData vsd = (VideoStartData) chmSession.get( VideoParameter.VIDEO_START_DATA + videoStartDataID );
//
//        HashSet<String> hsStorageExt = new HashSet<>();
//        StringTokenizer st = new StringTokenizer( dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_EXT ), ",; " );
//        while( st.hasMoreTokens() ) hsStorageExt.add( st.nextToken().toLowerCase() );
//
//        //--- (редко бывает больше 16 камер одновременно)
//        ArrayList<CameraDef> alCameraDef = new ArrayList<>( 16 );
//
//        //--- online-mode
//        if( vsd.rangeType == -1 ) {
//            for( int cIndex = 0; cIndex < vsd.alCameraDescr.size(); cIndex++ )
//                alCameraDef.add( new CameraDef( dirVideoRoot, vsd.objectId, vsd.alCameraDescr.get( cIndex ),
//                                vsd.alCameraURLImage.get( cIndex ), vsd.alCameraLogin.get( cIndex ), vsd.alCameraPassword.get( cIndex ) ) );
//        }
//        else {
//            long x1 = bbIn.getInt() * 1000L;
//            long x2 = bbIn.getInt() * 1000L;
//
//            File dirObject = VideoFunction.getObjectDir( dirVideoRoot, vsd.objectId );
//            File[] arrDirCamera = dirObject.listFiles();
//            //--- для каждой найденной папки с архивом
//            for( File dirCamera : arrDirCamera ) {
//                if( ! dirCamera.isDirectory() ) continue;
//
//                CameraDef cameraDef = new CameraDef( dirVideoRoot, vsd.objectId, dirCamera.getName(), VideoFunction.VIDEO_STREAM_COUNT );
//                //--- для каждого потока
//                for( int streamIndex = 0; streamIndex < VideoFunction.VIDEO_STREAM_COUNT; streamIndex++ ) {
//                    StreamDef streamDef = new StreamDef();
//
//                    File dirStream = new File( dirCamera, Integer.toString( streamIndex ) );
//
//                    //--- редко когда видео хранится более 30 дней
//                    PriorityQueue<File> pqDir = new PriorityQueue<>( 30, AscendingFileNameComparator.INSTANCE );
//
//                    if( dirStream.exists() ) {
//                        File[] arrDir = dirStream.listFiles();
//                        for( File file : arrDir )
//                            if( file.isDirectory() ) {
//                                long dirTime = StringFunction.getTimeFromDirName( file.getName(), "-", timeZone );
//                                //--- двое суток перед началом периода, т.к. файл может начаться в предыдущем периоде
//                                if( dirTime != 0 && dirTime > x1 - 2 * 86_400_000 && dirTime <= x2 )
//                                    pqDir.offer( file );
//                            }
//                    }
//
//                    //--- список файлов в текущей посуточной папке (300 типовых 5-минутных нарезок)
//                    PriorityQueue<File> pqFile = new PriorityQueue<>( 300, AscendingFileNameComparator.INSTANCE );
//
//                    //--- загрузим список файлов, пропуская ненужные
//                    while( true ) {
//                        File dir = pqDir.poll();
//                        if( dir == null ) break;
//
//                        String dirName = dir.getName();
//
//                        File[] arrFile = dir.listFiles();
//                        for( File file : arrFile )
//                            if( file.isFile() && file.length() > 1024 ) pqFile.offer( file );
//
//                        while( true ) {
//                            File file = pqFile.poll();
//                            if( file == null ) break;
//
//                            String fileName = file.getName();
//
//                            //--- проверка на разрешённые расширения видеофайлов
//                            int dotPos = fileName.indexOf( '.' );
//                            if( dotPos == -1 ) continue;
//                            String ext = fileName.substring( dotPos + 1 ).toLowerCase();
//                            if( ! hsStorageExt.contains( ext ) ) continue;
//
//                            long[] arrFileTime = StringFunction.getTimeFromFileName( fileName, "-.", timeZone );
//
//                            //--- файл с неправильным названием (например, текущий/записываемый ffmpeg'ом файл)
//                            if( arrFileTime == null ) continue;
//
//                            if( arrFileTime[ 0 ] < x2 && arrFileTime[ 1 ] > x1 ) {
//                                streamDef.alFileBeg.add( arrFileTime[ 0 ] );
//                                streamDef.alFileEnd.add( arrFileTime[ 1 ] );
//                                streamDef.alFileName.add( new StringBuilder( dirName )
//                                                      .append( '/' ).append( fileName ).toString() );
//                            }
//                        }
//                    }
//                    cameraDef.alStream.add( streamDef );
//                }
//                alCameraDef.add( cameraDef );
//            }
//        }
//
//        bbOut.putInt( alCameraDef.size() );
//        for( CameraDef cd : alCameraDef ) cd.write( bbOut );
//
//        return true;
//    }
//
//    protected boolean doLoadImage( AdvancedByteBuffer bbIn, AdvancedByteBuffer bbOut ) throws Throwable {
//        String videoStartDataID = bbIn.getShortString();
//        VideoStartData vsd = (VideoStartData) chmSession.get( VideoParameter.VIDEO_START_DATA + videoStartDataID );
//
//        ArrayList<File> alDestFile = new ArrayList<>( 16 );
//
//        boolean isLocalServer = bbIn.getBoolean();
//        for( int cIndex = 0; cIndex < vsd.alCameraDescr.size(); cIndex++ ) {
//            int imageWidth = bbIn.getShort();
//            int imageHeight = bbIn.getShort();
//
//            int objectId = vsd.objectId;
//            String descr = vsd.alCameraDescr.get( cIndex );
//
//            //--- загрузка для этой камеры не требуется
//            if( imageWidth == 0 ) alDestFile.add( null );
//            else {
//                int fileID = CommonFunction.getRandomInt();
//                //--- ВАЖНО: используем корневую папку для видео, поскольку она точно существует и
//                //--- нет необходимости постоянно делать дорогостоящий mkdirs()
//                File fileDest = new File( dataServer.hmConfig.get( CoreDataServer.CONFIG_TEMP_DIR ),
//                                          new StringBuilder().append( fileID ).append( ".jpg" ).toString() );
//
//                alDestFile.add( fileDest );
//
//                //--- если поле camera-name - пустое, значит запись камеры добавлена "вручную",
//                //--- что означает, что камера подключена к этому серверу напрямую
//                if( vsd.alCameraName.get( cIndex ).isEmpty() )
//                    VideoFunction.getImageFromCamera( dataServer.hmConfig.get( VideoFunction.CONFIG_FFMPEG_PATH ), vsd.alCameraURL0.get( cIndex ),
//                            vsd.alCameraURLImage.get( cIndex ), vsd.alCameraLogin.get( cIndex ), vsd.alCameraPassword.get( cIndex ),
//                            imageWidth, imageHeight, fileDest );
//                //--- иначе если поле camera-name - заполненное, значит запись камеры создана автоматически сервером приема снимков,
//                //--- что означает, что камера не подключена к этому серверу напрямую, и необходимо раздать задания на загрузку
//                else {
//                    //--- обновляем (добавляем если не было) актуальное время задания
//                    if( dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                        " UPDATE VC_job_image SET last_time = " ).append( System.currentTimeMillis() / 1000 )
//                        .append( " WHERE object_id = " ).append( objectId )
//                        .append( " AND descr = '" ).append( descr ).append( "' " ) ) == 0 )
//
//                        dataWorker.alStm.get( 0 ).executeUpdate( new StringBuilder(
//                                " INSERT INTO VC_job_image ( object_id , descr , last_time , file_index ) VALUES ( " )
//                                .append( objectId ).append( " , '" ).append( descr ).append( "' , " )
//                                .append( System.currentTimeMillis() / 1000 ).append( " , " ).append( 0 ).append( " ) " ) );
//
//                    //--- берём текущий индекс файла, который можно загружать
//                    //--- загрузим текущий индекс файла
//                    CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery( new StringBuilder(
//                        " SELECT file_index FROM VC_job_image " )
//                        .append( " WHERE object_id = " ).append( objectId )
//                        .append( " AND descr = '" ).append( descr ).append( "' " ) );
//                    rs.next();
//                    int fileIndex = rs.getInt( 1 );
//                    rs.close();
//
//                    File fileSour = new File( VideoFunction.getCameraDir( dataServer.hmConfig.get( VideoFunction.CONFIG_VIDEO_DIR ),
//                                                                          objectId, descr ),
//                                              new StringBuilder().append( fileIndex ).append( ".jpg" ).toString() );
//
//                    if( fileSour.exists() ) {
//                        //--- извратище: вся работа с папками/файлами сделана на старом добром java.io.File,
//                        //--- а для копирования приходится использовать новомодный NIO
//                        Path sourPath = Paths.get( fileSour.getCanonicalPath() );
//                        Path destPath = Paths.get( fileDest.getCanonicalPath() );
//                        Files.copy( sourPath, destPath, StandardCopyOption.REPLACE_EXISTING );
//
//                        //--- дополнительно сжимаем изображение при необходимости
//                        VideoFunction.resizeImage( imageWidth, imageHeight, fileDest );
//                    }
//                }
//            }
//        }
//        //--- отправка файлов-картинок
//        for( File fileDest : alDestFile ) {
//            if( fileDest == null || ! fileDest.exists() ) {
//                if( isLocalServer ) bbOut.putShortString( "" );
//                else bbOut.putInt( 0 );
//            }
//            else {
//                //--- как ни странно, вариант с fileDest.toURI().toString() для javafx.Image не работает,
//                //--- хотя в докции явно написано требование к fileDest:/- подобному урлу
//                if( isLocalServer ) bbOut.putShortString( fileDest.getCanonicalPath() );
//                else {
//                    CommonFunction.readFileToBuffer( fileDest, bbOut, true );
//                    fileDest.delete();
//                }
//            }
//        }
//
//        //--- отдаваемые картинки сжимать смысла нет
//        return false;
//    }
//
//}
//
//

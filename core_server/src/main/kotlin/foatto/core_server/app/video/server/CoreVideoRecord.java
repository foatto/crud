//package foatto.core_server.app.video.server;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.nio.file.StandardCopyOption;
//import java.util.ArrayList;
//import java.util.TimeZone;
//
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//
//public abstract class CoreVideoRecord {
//
//    private static TimeZone timeZone = TimeZone.getDefault();
//
//    public void run( String[] args ) {
//        try {
//            int ai = 0;
//            String videoRoot = args[ ai++ ];
//            String objectID = args[ ai++ ];
//            String cameraDescr = args[ ai++ ];
//            String streamIndex = args[ ai++ ];
//
//            String ffmpegPath = args[ ai++ ];
//            String duration = args[ ai++ ];
//            String url = args[ ai++ ];
//            String ffmpegMetaData = args[ ai++ ];
//            String videoCodec = args[ ai++ ];
//            String audioCodec = args[ ai++ ];
//            String videoFileExt = args[ ai++ ];
//
//            File dirCamera = new File( new File( videoRoot, objectID ), cameraDescr );
//            File dirStream = new File( dirCamera, streamIndex );
//
//            int[] arrBeg = StringFunction.DateTime_Arr( timeZone, System.currentTimeMillis() );
//            StringBuilder sbBegDate = new StringBuilder(
//                                                    arrBeg[ 0 ] ).append( '-' ).append( arrBeg[ 1 ] ).append( '-' ).append( arrBeg[ 2 ] );
//            StringBuilder sbBegTime = new StringBuilder(
//                                                    arrBeg[ 3 ] ).append( '-' ).append( arrBeg[ 4 ] ).append( '-' ).append( arrBeg[ 5 ] );
//
//            File dirDate = new File( dirStream, sbBegDate.toString() );
//            dirDate.mkdirs();
//
//            File fileVideoSour = new File( dirDate, new StringBuilder( sbBegTime ).append( '.' ).append( videoFileExt ).toString() );
//
//            //--- обязательно выключаем (убираем -report) вывод логов,
//            //--- иначе не получится запустить ffmpeg напрямую из-под себя (для initd-task-system)
//            ArrayList<String> alParam = new ArrayList<>();
//            alParam.add( ffmpegPath );
//            alParam.add( "-rtsp_transport" );   alParam.add( "tcp" );
//            alParam.add( "-stimeout" );         alParam.add( "100000" );
//            alParam.add( "-loglevel" );         alParam.add( "quiet" );
//            alParam.add( "-t" );                alParam.add( duration );
//            alParam.add( "-i" );                alParam.add( url );
//            if( ! ffmpegMetaData.equals( "-" ) ) {
//                alParam.add( "-i" );            alParam.add( ffmpegMetaData );
//                alParam.add( "-map_metadata" ); alParam.add( "1" );
//            }
//            alParam.add( "-vcodec" );           alParam.add( videoCodec );
//            if( audioCodec.equals( "-" ) )
//                alParam.add( "-an" );
//            else {
//                alParam.add( "-acodec" );       alParam.add( audioCodec );
//            }
//            alParam.add( "-y" );
//            alParam.add( "-flags" );
//            alParam.add( "+global_header" );
//            alParam.add( fileVideoSour.getCanonicalPath() );
//
//            String[] arrParam = new String[ alParam.size() ];
//            alParam.toArray( arrParam );
//            //--- слишком много разных ненулевых/успешных кодов выхода, пока не будем их учитывать
//            CommonFunction.runCommand( null, arrParam );
//
//            int[] arrEnd = StringFunction.DateTime_Arr( timeZone, System.currentTimeMillis() );
//            StringBuilder sbEndDate = new StringBuilder(
//                                                    arrEnd[ 0 ] ).append( '-' ).append( arrEnd[ 1 ] ).append( '-' ).append( arrEnd[ 2 ] );
//            StringBuilder sbEndTime = new StringBuilder(
//                                                    arrEnd[ 3 ] ).append( '-' ).append( arrEnd[ 4 ] ).append( '-' ).append( arrEnd[ 5 ] );
//
//            File fileVideoDest = new File( dirDate,
//                        new StringBuilder( sbBegDate ).append( '-' ).append( sbBegTime ).append( '-' )
//                                  .append( sbEndDate ).append( '-' ).append( sbEndTime ).append( '.' ).append( videoFileExt ).toString() );
//
//            //--- извратище: вся работа с папками/файлами сделана на старом добром java.io.File,
//            //--- а для переноса приходится использовать новомодный NIO
//            Path sourPath = Paths.get( fileVideoSour.getCanonicalPath() );
//            Path destPath = Paths.get( fileVideoDest.getCanonicalPath() );
//            Files.move( sourPath, destPath, StandardCopyOption.REPLACE_EXISTING
//                                            //--- эти опции не поддерживаются win7
//                                            /*, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.ATOMIC_MOVE*/ );
//
//            //--- дополнительные специфичные операции
//            extraWork();
//        }
//        catch( Throwable t ) {
//            t.printStackTrace();
//            exitProcess( 0 );
//        }
//    }
//
//    protected void extraWork() throws Throwable {}
//
//}

//package foatto.core_server.app.video.server;
//
//import java.awt.geom.AffineTransform;
//import java.awt.image.AffineTransformOp;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.*;
//
//import javax.imageio.ImageIO;
//
//import foatto.core.app.video.CameraDef;
//import foatto.sql.CoreAdvancedResultSet;
//import foatto.sql.CoreAdvancedStatement;
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//
//public class VideoFunction {
//
//    public static final int VIDEO_STREAM_COUNT = 2;
//
//    //--- видео-настройки в mms-server.ini
//    public static final String CONFIG_VIDEO_DIR = "video_dir";
//    public static final String CONFIG_VIDEO_EXT = "video_ext";
//
//    public static final String CONFIG_FFMPEG_PATH = "ffmpeg_path";
//    public static final String CONFIG_FFMPEG_METADATA_FILE = "ffmpeg_metadata";
//    public static final String CONFIG_FFMPEG_EXTRA_COMMAND = "ffmpeg_extra_command";
//
//    public static final String CONFIG_TASK_SYSTEM = "task_system";
//    public static final String CONFIG_TASK_PATH = "task_path";
//    public static final String CONFIG_TASK_MANAGER = "task_manager";
//    public static final String CONFIG_TASK_USER = "task_user";
//    public static final String CONFIG_TASK_PASSWORD = "task_password";
//
//    public static final String CONFIG_TIME_SYSTEM = "time_system";
//
//    public static final String CONFIG_CAMERA_MODEL_NAME_ = "camera_model_name_";
//    public static final String CONFIG_CAMERA_MODEL_LOGIN_ = "camera_model_login_";
//    public static final String CONFIG_CAMERA_MODEL_PASSWORD_ = "camera_model_password_";
//    public static final String CONFIG_CAMERA_MODEL_URL_0_ = "camera_model_url_0_";
//    public static final String CONFIG_CAMERA_MODEL_URL_1_ = "camera_model_url_1_";
//    public static final String CONFIG_CAMERA_MODEL_URL_MJPEG_ = "camera_model_url_mjpeg_";
//    public static final String CONFIG_CAMERA_MODEL_URL_IMAGE_ = "camera_model_url_image_";
//    public static final String CONFIG_CAMERA_MODEL_URL_TIME_ = "camera_model_url_time_";
//
//    public static final String CONFIG_PLAYER_NAME = "player_name";
//    public static final String CONFIG_CAMERA_SHOW_SCRIPT_FILE_NAME = "camera_show_script";
//    public static final String CONFIG_DISPLAY_BUTTON_SCRIPT_FILE_NAME = "display_button_script";
//
//    public static final String CONFIG_SERIAL_NO = "serial_no";
//
//    public static final String GPIO_IN_INIT = "./gpio_in_init.sh";
//    public static final String GPIO_IN = "./gpio_in.sh";
//    public static final String GPIO_OUT_INIT = "./gpio_out_init.sh";
//    public static final String GPIO_OUT = "./gpio_out.sh";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static final int VC_COPY = 1;
//    public static final int VC_H264 = 2;
//
//    public static final int AC_NONE = 0;
//    public static final int AC_COPY = 1;
//    public static final int AC_AAC  = 2;
//
//    public static final int TASK_SYSTEM_WINDOWS = 0;
//    public static final int TASK_SYSTEM_SYSTEMD = 1;
//    public static final int TASK_SYSTEM_INITD = 2;
//
//    public static final int TIME_SYSTEM_WINDOWS = 0;
//    public static final int TIME_SYSTEM_LINUX_UBUNTU = 1;
//    public static final int TIME_SYSTEM_LINUX_BUILDROOT = 2;
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static final ArrayList<String> alTimeTemplate = new ArrayList<>();
//    static {
//        alTimeTemplate.add( "ГГГГ" );
//        alTimeTemplate.add( "ММ" );
//        alTimeTemplate.add( "ДД" );
//        alTimeTemplate.add( "чч" );
//        alTimeTemplate.add( "мм" );
//        alTimeTemplate.add( "сс" );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static File getObjectDir( String dirVideoRoot, int objectId ) {
//        return new File( dirVideoRoot, Integer.toString( objectId ) );
//    }
//
//    public static File getCameraDir( String dirVideoRoot, int objectId, String cameraDescr ) {
//        return new File( getObjectDir( dirVideoRoot, objectId ), cameraDescr );
//    }
//
//    public static StringBuilder getStreamName( int objectId, String cameraDescr, int streamIndex ) {
//        return new StringBuilder().append( objectId ).append( '-' ).append( cameraDescr ).append( '-' ).append( streamIndex );
//    }
//
//    public static void getImageFromCamera( String ffmpegPath, String urlVideo, String urlImage,
//                                           String cameraLogin, String cameraPassword,
//                                           int imageWidth, int imageHeight, File imageFile ) throws Throwable {
//        if( urlImage == null || urlImage.isEmpty() )
//            CommonFunction.runCommand( null, ffmpegPath, "-rtsp_transport", "tcp", "-stimeout", "100000", "-threads", "1",
//                "-i", StringFunction.addLoginAndPasswordToURL( urlVideo, cameraLogin, cameraPassword, true ),
//                "-vframes", "1", "-y", imageFile.getCanonicalPath() );
//        else    // ловим и игнорируем ошибку получения кадра в случае, если камера физически не подключена
//            try {
//                ImageIO.write( ImageIO.read( CameraDef.getCameraImageInputStream( urlImage, cameraLogin, cameraPassword ) ), "jpg",
//                               imageFile );
//            }
//            catch( Throwable t ) {}
//
//        //--- из-за того, что снимок с камеры может быть получен через ffmpeg -
//        //--- - приходится делать пережатие картинки отдельной операцией, опять прочитывая файл
//        if( imageFile.exists() && imageWidth > 0 && imageHeight > 0 )
//            resizeImage( imageWidth, imageHeight, imageFile );
//    }
//
//    public static void resizeImage( int imageWidth, int imageHeight, File imageFile ) throws Throwable {
//        BufferedImage biSour = ImageIO.read( imageFile );
//        double scaleDiv = Math.max( 1.0 * biSour.getWidth() / imageWidth, 1.0 * biSour.getHeight() / imageHeight );
//        //--- пережимаем, если хотя бы одну из сторон нужно уменьшить более чем в 1.4 раза (1.4 ~ SQRT( 2 ),
//        //--- при таком уменьшении сторон объём картинки (и соответственно трафик) уменьшится примерно в 2 раза,
//        //--- что является достаточной причиной для пережатия)
//        if( scaleDiv > 1.4 ) {
//            imageWidth = (int) Math.round( biSour.getWidth() / scaleDiv );
//            imageHeight = (int) Math.round( biSour.getHeight() / scaleDiv );
//            BufferedImage biDest = new BufferedImage( imageWidth, imageHeight, biSour.getType() );
//            AffineTransform at = new AffineTransform();
//            at.scale( 1.0 / scaleDiv, 1.0 / scaleDiv );
//            AffineTransformOp atOp = new AffineTransformOp( at, AffineTransformOp.TYPE_BICUBIC );
//            atOp.filter( biSour, biDest );
//            ImageIO.write( biDest, "jpg", imageFile );
//        }
//    }
//
//    public static void setTime( CoreAdvancedStatement stm, int timeSystem, int[] arrDT ) {
//        StringBuilder sbDT = StringFunction.DateTime_DMYHMS( arrDT );
//
//        switch( timeSystem ) {
//        case TIME_SYSTEM_WINDOWS:
//            CommonFunction.runCommand( null, "cmd", "/C", "date", sbDT.substring( 0, 10 ) );
//            CommonFunction.runCommand( null, "cmd", "/C", "time", sbDT.substring( 11, 19 ) );
//            break;
//        case TIME_SYSTEM_LINUX_UBUNTU:
//            //date 110114312011.00 - 11-месяц-01-день14-час31-минуты-2011-год.00-секунды
//            CommonFunction.runCommand( null, "date", new StringBuilder()
//                .append( arrDT[ 1 ] < 10 ? '0' : "" ).append( arrDT[ 1 ] )
//                .append( arrDT[ 2 ] < 10 ? '0' : "" ).append( arrDT[ 2 ] )
//                .append( arrDT[ 3 ] < 10 ? '0' : "" ).append( arrDT[ 3 ] )
//                .append( arrDT[ 4 ] < 10 ? '0' : "" ).append( arrDT[ 4 ] )
//                .append( arrDT[ 0 ] ).append( '.' )
//                .append( arrDT[ 5 ] < 10 ? '0' : "" ).append( arrDT[ 5 ] ).toString() );
//            break;
//        case TIME_SYSTEM_LINUX_BUILDROOT:
//            //date 2016.09.22-19:30:00
//            CommonFunction.runCommand( null, "date", new StringBuilder()
//                .append( arrDT[ 0 ] ).append( '.' )
//                .append( arrDT[ 1 ] < 10 ? '0' : "" ).append( arrDT[ 1 ] ).append( '.' )
//                .append( arrDT[ 2 ] < 10 ? '0' : "" ).append( arrDT[ 2 ] ).append( '-' )
//                .append( arrDT[ 3 ] < 10 ? '0' : "" ).append( arrDT[ 3 ] ).append( ':' )
//                .append( arrDT[ 4 ] < 10 ? '0' : "" ).append( arrDT[ 4 ] ).append( ':' )
//                .append( arrDT[ 5 ] < 10 ? '0' : "" ).append( arrDT[ 5 ] ).toString() );
//            //--- установить железное RTC-время на плате равным софтовому
//            CommonFunction.runCommand( null, "hwclock", "-w" );
//            break;
//        }
//
//        //--- установка времени на камерах
//        CoreAdvancedResultSet rs = conn.executeQuery( new StringBuilder(
//                                    " SELECT url_time , login , pwd FROM VC_camera WHERE id <> 0 " ) );
//        while( rs.next() ) {
//            int p = 1;
//            String urlTime = rs.getString( p++ );
//            String login = rs.getString( p++ );
//            String pwd = rs.getString( p++ );
//
//            //--- ручная установка времени на камере
//            if( urlTime != null && ! urlTime.isEmpty() ) setCameraTime( urlTime, login, pwd );
//        }
//        rs.close();
//    }
//
//    public static void setCameraTime( String urlTime, String login, String pwd ) {
//        int[] arrDT = StringFunction.DateTime_Arr( new GregorianCalendar() );
//        for( int i = 0; i < alTimeTemplate.size(); i++ ) {
//            //--- если параметр подстановки не нашёлся - пропускаем установку времени
//            if( ! urlTime.contains( alTimeTemplate.get( i ) ) ) {
//                AdvancedLogger.error( "Not found '" + alTimeTemplate.get( i ) + "' parameter for " + urlTime );
//                urlTime = null;
//                break;
//            }
//            String sDest = new StringBuilder( arrDT[ i ] < 10 ? "0" : "" ).append( arrDT[ i ] ).toString();
//            urlTime = urlTime.replace( alTimeTemplate.get( i ), sDest );
//        }
//        if( urlTime != null )
////http://192.168.7.31/cgi-bin/date.cgi?system_hostname=H264+4MP+HDR+IR+compact+bullet+camera&timezone=GMT&dateformate=YYYY-MM-DD&pcdate=ГГГГ%2FММ%2FДД&pcdate2=ДД%2FММ%2FГГГГ&pctime=чч%3Aмм%3Aсс&method=manu&system_date=ГГГГ%2FММ%2FДД&system_date2=ДД%2FММ%2FГГГГ&system_time=чч%3Aмм%3Aсс&ntpaddr=192.168.7.20&updateinterval=hour
//            try {
//                HttpURLConnection urlConn = (HttpURLConnection) new URL( urlTime ).openConnection();
//                //--- настройка подключения
//                urlConn.setRequestMethod( "GET" );
//                urlConn.setAllowUserInteraction( false );
//                urlConn.setConnectTimeout( 1000 );
//                urlConn.setDoOutput( true );
//                urlConn.setDoInput( true );
//                urlConn.setUseCaches( false );
//                urlConn.setRequestProperty( "Authorization", new StringBuilder( "Basic " ).append(
//                    Base64.getEncoder().encodeToString(
//                        new StringBuilder( login ).append( ':' ).append( pwd ).toString().getBytes() ) ).toString() );
//                //--- так и осталось неясным - вызывать ли явно метод connect или он где-то автоматом вызывается:
//                //--- часть примеров с ним, часть без него.
//                //--- но сейчас работает и без его вызова.
//                //urlConn.connect();
//
//                InputStream is = urlConn.getInputStream();
//                while( is.read() != -1 );
//                is.close();
//            }
//            catch( Throwable t ) {
//                AdvancedLogger.error( t );
//            }
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    public static TreeMap<Integer,CameraModelData> loadCameraModelData( HashMap<String,String> hmConfig ) {
//        TreeMap<Integer,CameraModelData> tmCMD = new TreeMap<>();
//        int index = 0;
//        while( true ) {
//            String sName = hmConfig.get( CONFIG_CAMERA_MODEL_NAME_ + index );
//            if( sName == null ) break;
//
//            tmCMD.put( index, new CameraModelData( sName,
//                hmConfig.get( CONFIG_CAMERA_MODEL_LOGIN_ + index ),
//                hmConfig.get( CONFIG_CAMERA_MODEL_PASSWORD_ + index ),
//                new String[] { hmConfig.get( CONFIG_CAMERA_MODEL_URL_0_ + index ),
//                               hmConfig.get( CONFIG_CAMERA_MODEL_URL_1_ + index ) },
//                hmConfig.get( CONFIG_CAMERA_MODEL_URL_MJPEG_ + index ),
//                hmConfig.get( CONFIG_CAMERA_MODEL_URL_IMAGE_ + index ),
//                hmConfig.get( CONFIG_CAMERA_MODEL_URL_TIME_ + index ) ) );
//
//            index++;
//        }
//        return tmCMD;
//    }
//
//}
//

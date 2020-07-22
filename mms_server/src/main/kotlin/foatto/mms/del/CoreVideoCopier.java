//package foatto.mms.del;
//
//import foatto.core.util.*;
//
//import java.io.File;
//import java.nio.file.*;
//import java.util.*;
//
//public abstract class CoreVideoCopier extends CoreRelayWorker {
//
//    private static final String CONFIG_ROOT_COUNT = "root_count";
//    private static final String CONFIG_ROOT_ = "root_";
//
//    private static final String CONFIG_TIME_DATA_FILE = "time_data_file";
//
//    private static final String CONFIG_VIDEO_DIR_COUNT = "video_dir_count";
//    private static final String CONFIG_VIDEO_DIR_ = "video_dir_";
//
//    private static final String CONFIG_VIDEO_EXT = "video_ext";
//
//    private static final String CONFIG_USB_UTILITY = "usb_utility";
//
//    private static final String CONFIG_CYCLE_PAUSE = "cycle_pause";
//
////---------------------------------------------------------------------------------------------------------------
//
//    private HashSet<String> hsBaseRoot = new HashSet<>();
//    private String timeDataFileName = null;
//    private File[] arrVideoDir = null;
//    private HashSet<String> hsStorageExt = new HashSet<>();
//    private String usbUtility = null;
//    private long cyclePause = 0;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private TimeZone timeZone = TimeZone.getDefault();
//
////---------------------------------------------------------------------------------------------------------------
//
//    protected CoreVideoCopier( String aConfigFileName ) throws Throwable {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() throws Throwable {
//        super.loadConfig();
//
//        int rootCount = Integer.parseInt( hmConfig.get( CONFIG_ROOT_COUNT ) );
//        for( int i = 0; i < rootCount; i++ )
//            hsBaseRoot.add( hmConfig.get( CONFIG_ROOT_ + i ) );
//
//        timeDataFileName = hmConfig.get( CONFIG_TIME_DATA_FILE );
//
//        int videoDirCount = Integer.parseInt( hmConfig.get( CONFIG_VIDEO_DIR_COUNT ) );
//        arrVideoDir = new File[ videoDirCount ];
//        for( int i = 0; i < videoDirCount; i++ )
//            arrVideoDir[ i ] = new File( hmConfig.get( CONFIG_VIDEO_DIR_ + i ) );
//
//        StringTokenizer st = new StringTokenizer( hmConfig.get( CONFIG_VIDEO_EXT ), ",; " );
//        while( st.hasMoreTokens() ) hsStorageExt.add( st.nextToken().toLowerCase() );
//
//        usbUtility = hmConfig.get( CONFIG_USB_UTILITY );
//
//        cyclePause = Integer.parseInt( hmConfig.get( CONFIG_CYCLE_PAUSE ) ) * 1000L;
//    }
//
//    protected boolean isRunOnce() { return false; }
//    protected boolean isDailyRestart() { return false; }    // вдруг там целыми сутками копироваться будет
//
//    protected void cycle() throws Throwable {
//        //--- грузим текущий список дисков.
//        //--- если обнаружились новые диски, берём с максимально свободным местом
//        long maxWritableSize = 0;
//        File fileWritableRoot = null;
//        File[] arrRoot = File.listRoots();
//        for( File root : arrRoot ) {
//            String rootName = root.getCanonicalPath();
//            AdvancedLogger.debug( "root = " + rootName );
//            if( ! hsBaseRoot.contains( rootName ) && root.canWrite() ) {
//                long freeSpace = root.getFreeSpace();   // чтобы не пересчитывать лишний раз
//                if( freeSpace > maxWritableSize ) {
//                    maxWritableSize = freeSpace;
//                    fileWritableRoot = root;
//                }
//            }
//        }
//        //--- если нашёлся новый диск (на воткнутой USB-флешке)
//        if( fileWritableRoot != null ) {
//            AdvancedLogger.info( "Найден внешний диск " + fileWritableRoot.getCanonicalPath() );
//            //--- зажигаем старт копирования
//            setCopyLED( true );
//            //--- загружаем список времён последних скопированных данных
//            HashMap<String,String> hmLastTime = CommonFunction.loadConfig( timeDataFileName, "Cp1251", "#", "@", true );
//            //--- составляем очередь видео файлов для копирования, отсортированную по времени начала записи
//            //--- на обычные 4 камеры 30 дней по примерно 300 типовых 5-минутных нарезок для каждого хранилища
//            PriorityQueue<VideoFileData> pqVideoFileData = new PriorityQueue<>( arrVideoDir.length * 30 * 300,
//                                                                                new VideoFileNameComparator() );
//            AdvancedLogger.info( "Подготовка списка файлов..." );
//            //--- по каждой папке из списка видеохранилищ
//            for( File videoDir : arrVideoDir ) {
//                //--- по каждой папке камеры
//                File[] arrDelDir = videoDir.listFiles();
//                for( File delDir : arrDelDir )
//                    if( delDir.isDirectory() ) {
//                        //--- по каждой папке камеры
//                        File[] arrCameraDir = delDir.listFiles();
//                        for( File cameraDir : arrCameraDir )
//                            if( cameraDir.isDirectory() ) {
//                                //--- по каждой папке камеры
//                                File[] arrStreamDir = cameraDir.listFiles();
//                                for( File streamDir : arrStreamDir )
//                                    if( streamDir.isDirectory() ) {
//                                        String timeName = new StringBuilder( delDir.getName() )
//                                                        .append( ':' ).append( cameraDir ).append( ':' ).append( streamDir ).toString();
//                                        String sLastSavedTime = hmLastTime.get( timeName );
//                                        long lastSavedTime = sLastSavedTime == null ? 0 : Long.parseLong( sLastSavedTime );
//                                        //--- по каждой папке с датой
//                                        File[] arrDateDir = streamDir.listFiles();
//                                        for( File dateDir : arrDateDir )
//                                            if( dateDir.isDirectory() ) {
//                                                //--- по каждому видеофайлу
//                                                File[] arrVideoFile = dateDir.listFiles();
//                                                for( File videoFile : arrVideoFile )
//                                                    if( videoFile.isFile() ) {
//                                                        String fileName = videoFile.getName();
//                                                        //--- проверка на разрешённые расширения видеофайлов
//                                                        int dotPos = fileName.lastIndexOf( '.' );
//                                                        if( dotPos == -1 ) continue;
//                                                        String ext = fileName.substring( dotPos + 1 ).toLowerCase();
//                                                        if( ! hsStorageExt.contains( ext ) ) continue;
//
//                                                        long[] arrFileTime = StringFunction.getTimeFromFileName( fileName, "-.", timeZone );
//                                                        //--- файл с неправильным названием (например, текущий/записываемый ffmpeg'ом файл)
//                                                        if( arrFileTime == null ) continue;
//
//                                                        //--- после проверки на правильное название проверяем на глюк ffmpeg'a -
//                                                        //--- неполный файл
//                                                        if( videoFile.length() < 1024 ) {
//                                                            videoFile.delete();
//                                                            continue;
//                                                        }
//
//                                                        //--- эти файлы еще не копировались
//                                                        if( arrFileTime[ 0 ] > lastSavedTime )
//                                                            pqVideoFileData.offer( new VideoFileData(
//                                                                                                delDir.getName(), cameraDir.getName(),
//                                                                                                streamDir.getName(), dateDir.getName(),
//                                                                                                videoFile, arrFileTime[ 0 ] ) );
//                                                    }
//                                            }
//                                    }
//                            }
//                    }
//            }
//
//            AdvancedLogger.info( "Копирование..." );
//            //--- начинаем копирование
//            File destRootDir = new File( fileWritableRoot, "MMSServer_video" );
//            destRootDir.mkdirs();
//            while( true ) {
//                //--- нажата кнопка копирования - прерываем копирование
//                if( getCopyButtonStatus() ) {
//                    AdvancedLogger.info( "Копирование закончено: нажата кнопка прерывания копирования." );
//                    break;
//                }
//
//                VideoFileData sourVFD = pqVideoFileData.poll();
//                //--- нет больше файлов для копирования
//                if( sourVFD == null ) {
//                    AdvancedLogger.info( "Копирование закончено: нет больше файлов для копирования." );
//                    break;
//                }
//                //--- кончилось место на флешке
//                if( destRootDir.getFreeSpace() < sourVFD.videoFile.length() ) {
//                    AdvancedLogger.info( "Копирование закончено: нет свободного место на внешнем диске." );
//                    break;
//                }
//                //--- создаём требуемую структуру папок
//                File destDelDir = new File( destRootDir, sourVFD.delDirName );
//                File destCameraDir = new File( destDelDir, sourVFD.cameraDirName );
//                File destStreamDir = new File( destCameraDir, sourVFD.streamDirName );
//                File destDateDir = new File( destStreamDir, sourVFD.dateDirName );
//                destDateDir.mkdirs();
//                File destVideoFile = new File( destDateDir, sourVFD.videoFile.getName() );
//                //--- извратище: вся работа с папками/файлами сделана на старом добром java.io.File,
//                //--- а для копирования приходится использовать новомодный NIO
//                Path sourPath = Paths.get( sourVFD.videoFile.getCanonicalPath() );
//                Path destPath = Paths.get( destVideoFile.getCanonicalPath() );
//                Files.copy( sourPath, destPath, StandardCopyOption.REPLACE_EXISTING );
//                //--- подготовим обновление инфы по последнему копированию
//                String timeName = new StringBuilder( sourVFD.delDirName )
//                                .append( ':' ).append( sourVFD.cameraDirName ).append( ':' ).append( sourVFD.streamDirName ).toString();
//                hmLastTime.put( timeName, Long.toString( sourVFD.fileTime ) );
//                //AdvancedLogger.info( new StringBuilder( "Осталось " ).append( pqVideoFileData.size() ).append( " файлов." ) );
//            }
//            //--- освобождаем флешку - 5 попыток, потом пофиг
//            AdvancedLogger.info( "Подготовка внешнего диска к безопасному отключению..." );
//            boolean isRemoved = true;
//            for( int i = 0; i < 5; i++ ) {
//                CommonFunction.runCommand( null, usbUtility, "/stop_by_drive", fileWritableRoot.getCanonicalPath() );
//                //--- ждём/проверяем что освободилась
//                Thread.sleep( ( i + 1 ) * 1000L );   // всё равно не сразу освободится
//                isRemoved = true;
//                File[] arrNewRoot = File.listRoots();
//                for( File root : arrNewRoot )
//                    if( root.getCanonicalPath().equals( fileWritableRoot.getCanonicalPath() ) ) {
//                        isRemoved = false;
//                        break;
//                    }
//                if( isRemoved ) break;
//            }
//            AdvancedLogger.info( isRemoved ? "Внешний диск успешно отключён." : "Не удалось отключить внешний диск." );
//            //--- сохраняем значения времени последних скопированных файлов
//            CommonFunction.saveConfig( timeDataFileName, "Cp1251", hmLastTime );
//            //--- и только в самом конце гасим лампочку - потому что связь с реле иногда вылетает по таймауту
//            setCopyLED( false );
//        }
//        else {
//            setCopyLED( false );
//            //--- кнопка
////System.out.println( "readDiscreteInputs( 0x0000 ) = " + Integer.toBinaryString( readDiscreteInputs( 0x0000 ) ) );
//            //--- вкл/выкл камеры - работает
//            //writeSingleCoil( 0x0000, 3, 0xFF00 );
//            //writeSingleCoil( 0x0000, 4, 0xFF00 );
//            //writeSingleCoil( 0x0000, 5, 0xFF00 );
//            //writeSingleCoil( 0x0000, 3, 0x0000 );
//            //writeSingleCoil( 0x0000, 4, 0x0000 );
//            //writeSingleCoil( 0x0000, 5, 0x0000 );
//            //latch on - срабатывает, но потом непонятно как выключить, да и зачем вообще эта функция
//            //writeSingleCoil( 0x0096, 0, 0xFF00 );
//            //System.out.println( "readDiscreteInputs( 0x0000 ) = " + Integer.toBinaryString( readDiscreteInputs( 0x0000 ) ) );
//            //System.out.println( "readDiscreteInputs( 0x0020 ) = " + Integer.toBinaryString( readDiscreteInputs( 0x0020 ) ) );
//            //System.out.println( "readDiscreteInputs( 0x0040 ) = " + Integer.toBinaryString( readDiscreteInputs( 0x0040 ) ) );
//        }
//
//        Thread.sleep( cyclePause );
//    }
//
////---------------------------------------------------------------------------------------------------------------
//
//    private void setCopyLED( boolean status ) {
//        try {
//            writeSingleCoil( 0x0000, relayRackLed, status ? 0xFF00 : 0x0000 );
//        }
//        catch( Throwable t ) {
//            AdvancedLogger.error( t );
//            //--- надо бы mp3-шку какую проиграть...
//        }
//    }
//
//    private boolean getCopyButtonStatus() {
//        boolean status = false;
//        try {
//            status = readDiscreteInputs( 0x0000 + relayRackButton ) != 0;
//        }
//        catch( Throwable t ) {
//            AdvancedLogger.error( t );
//            //--- надо бы mp3-шку какую проиграть...
//        }
//        return status;
//    }
//
////---------------------------------------------------------------------------------------------------------------
//
//    private static class VideoFileData {
//        public String delDirName = null;
//        public String cameraDirName = null;
//        public String streamDirName = null;
//        public String dateDirName = null;
//        public File videoFile = null;
//        public long fileTime = 0;
//
//        VideoFileData( String aDelDirName, String aCameraDirName, String aStreamDirName, String aDateDirName,
//                       File aVideoFile, long aFileTime ) {
//            delDirName = aDelDirName;
//            cameraDirName = aCameraDirName;
//            streamDirName = aStreamDirName;
//            dateDirName = aDateDirName;
//            videoFile = aVideoFile;
//            fileTime = aFileTime;
//        }
//    }
//
////---------------------------------------------------------------------------------------------------------------
//
//    private static class VideoFileNameComparator implements Comparator {
//        public int compare( Object o1, Object o2 ) {
//            return ( (VideoFileData) o1 ).videoFile.getName().compareTo( ( (VideoFileData) o2 ).videoFile.getName() );
//        }
//    }
//
//}

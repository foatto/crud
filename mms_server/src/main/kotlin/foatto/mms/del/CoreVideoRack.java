//package foatto.mms.del;
//
//import foatto.core.util.AdvancedLogger;
//import foatto.core.util.CommonFunction;
//import foatto.core.util.StringFunction;
//import kotlin.Pair;
//
//import java.io.File;
//import java.nio.file.*;
//import java.util.*;
//
//public abstract class CoreVideoRack extends CoreRelayWorker {
//
//    private static final String CONFIG_HARD_DEST = "hard_dest";
//
//    private static final String CONFIG_MOUNT_CMD = "mount_cmd";
//
//    private static final String CONFIG_DEST_ROOT = "dest_root";
//
//    private static final String CONFIG_ROOT_COUNT = "root_count";
//    private static final String CONFIG_ROOT_ = "root_";
//
//    private static final String CONFIG_STORAGE_ROOT = "storage_root";
//    private static final String CONFIG_VIDEO_EXT = "video_ext";
//
//    private static final String CONFIG_INDEXER_CONFIG_FILE = "indexer_config_file";
//    private static final String CONFIG_INDEXER_CONFIG_DEFAULT_ROOT = "indexer_default_root";
//
//    private static final String CONFIG_HTTP_SERVER_TYPE = "http_server_type";
//    private static final String CONFIG_HTTP_SERVER_CONFIG_FILE = "http_server_config_file";
//    private static final String CONFIG_HTTP_SERVER_CONFIG_DEFAULT_ROOT = "http_server_default_root";
//    private static final String CONFIG_HTTP_SERVER_FILE = "http_server_file";
//
//    private static final String CONFIG_DEL_INFO_FILE = "del_info_file";
//
//    private static final String CONFIG_USB_UTILITY = "usb_utility";
//    private static final String CONFIG_UNMOUNT_CMD = "unmount_cmd";
//
//    private static final String CONFIG_CYCLE_PAUSE = "cycle_pause";
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static final int HTTP_SERVER_TYPE_LIGHTTPD = 0;
//    private static final int HTTP_SERVER_TYPE_NGINX = 1;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private File fileHardDest = null;
//    private String[] arrMountCmd = null;
//    private File fileDestRoot = null;
//    private HashSet<String> hsBaseRoot = new HashSet<>();
//    private TreeSet<File> tsStorageRoot = new TreeSet<>();
//    private HashSet<String> hsStorageExt = new HashSet<>();
//    private String indexerConfigFileName = null;
//    private String indexerConfigDefaultRoot = null;
//    private int httpServerType = HTTP_SERVER_TYPE_LIGHTTPD;
//    private String httpServerConfigFileName = null;
//    private String httpServerConfigDefaultRoot = null;
//    private String httpServerFileName = null;
//    private String delInfoFileName = null;
//    private String usbUtility = null;
//    private String[] arrUnmountCmd = null;
//    private long cyclePause = 0;
//
////---------------------------------------------------------------------------------------------------------------
//
//    private TimeZone timeZone = TimeZone.getDefault();
//
////---------------------------------------------------------------------------------------------------------------
//
//    protected CoreVideoRack( String aConfigFileName ) {
//        super( aConfigFileName );
//    }
//
//    public void loadConfig() {
//        super.loadConfig();
//
//        String sHardDest = hmConfig.get( CONFIG_HARD_DEST );
//        if( sHardDest != null && ! sHardDest.isEmpty() ) fileHardDest = new File( sHardDest );
//
//        String mountCmd = hmConfig.get( CONFIG_MOUNT_CMD );
//        if( mountCmd != null ) arrMountCmd = StringFunction.getStringArray( mountCmd, " " );
//
//        String sDestRoot = hmConfig.get( CONFIG_DEST_ROOT );
//        if( sDestRoot != null && ! sDestRoot.isEmpty() ) fileDestRoot = new File( sDestRoot );
//
//        String sRootCount = hmConfig.get( CONFIG_ROOT_COUNT );
//        if( sRootCount != null && ! sRootCount.isEmpty() ) {
//            int rootCount = Integer.parseInt( sRootCount );
//            for( int i = 0; i < rootCount; i++ )
//                hsBaseRoot.add( hmConfig.get( CONFIG_ROOT_ + i ) );
//        }
//
//        StringTokenizer st = new StringTokenizer( hmConfig.get( CONFIG_STORAGE_ROOT ), ",;" );
//        while( st.hasMoreTokens() ) tsStorageRoot.add( new File( st.nextToken().trim() ) );
//
//        st = new StringTokenizer( hmConfig.get( CONFIG_VIDEO_EXT ), ",; " );
//        while( st.hasMoreTokens() ) hsStorageExt.add( st.nextToken().toLowerCase() );
//
//        indexerConfigFileName = hmConfig.get( CONFIG_INDEXER_CONFIG_FILE );
//        indexerConfigDefaultRoot = hmConfig.get( CONFIG_INDEXER_CONFIG_DEFAULT_ROOT );
//
//        httpServerType = Integer.parseInt( hmConfig.get( CONFIG_HTTP_SERVER_TYPE ) );
//        httpServerConfigFileName = hmConfig.get( CONFIG_HTTP_SERVER_CONFIG_FILE );
//        httpServerConfigDefaultRoot = hmConfig.get( CONFIG_HTTP_SERVER_CONFIG_DEFAULT_ROOT );
//        httpServerFileName = hmConfig.get( CONFIG_HTTP_SERVER_FILE );
//
//        delInfoFileName = hmConfig.get( CONFIG_DEL_INFO_FILE );
//
//        usbUtility = hmConfig.get( CONFIG_USB_UTILITY );
//
//        String unmountCmd = hmConfig.get( CONFIG_UNMOUNT_CMD );
//        if( unmountCmd != null ) arrUnmountCmd = StringFunction.getStringArray( unmountCmd, " " );
//
//        cyclePause = Integer.parseInt( hmConfig.get( CONFIG_CYCLE_PAUSE ) ) * 1000L;
//    }
//
//    protected boolean isRunOnce() { return false; }
//
//    protected void cycle() throws Throwable {
//
//        if( getRackButtonStatus() ) {
//            File fileWritableRoot = null;
//
//            //--- если задана команда безусловного монтирования при нажатии кнопки
//            if( arrMountCmd != null && fileHardDest != null ) {
//                //--- включаем питание USB
//                setUSBPower( true );
//                //--- монтируем диск с проверкой результата
//                if( CommonFunction.runCommand( null, arrMountCmd ) == 0 )
//                    fileWritableRoot = fileHardDest;
//                //--- если не получилось - попробуем его размонтировать, вдруг он завис в недоразмонтированном состоянии
//                else if( arrUnmountCmd != null ) {
//                    CommonFunction.runCommand( null, arrUnmountCmd );
//                    setUSBPower( false );
//                }
//            }
//            //--- если просто указан жёстко заданный путь, то проверяем его наличие и доступность
//            else if( fileHardDest != null ) {
//                if( fileHardDest.exists() && fileHardDest.canWrite() ) fileWritableRoot = fileHardDest;
//            }
//            else {
//                //--- грузим текущий список дисков или примонтированных папок
//                //--- если обнаружились новые диски, берём с максимально свободным местом
//                long maxWritableSize = 0;
//                File[] arrRoot = fileDestRoot == null ? File.listRoots() : fileDestRoot.listFiles();
//                for( File root : arrRoot ) {
//                    String rootName = root.getCanonicalPath();
//                    AdvancedLogger.debug( "root = " + rootName );
//                    if( ! hsBaseRoot.contains( rootName ) && root.canWrite() ) {
//                        long freeSpace = root.getFreeSpace();   // чтобы не пересчитывать лишний раз
//                        AdvancedLogger.debug( "freeSpace = " + freeSpace );
//                        if( freeSpace > maxWritableSize ) {
//                            maxWritableSize = freeSpace;
//                            fileWritableRoot = root;
//                        }
//                    }
//                }
//            }
//            //--- если определились, куда переносить - запускаем цикл переноса
//            if( fileWritableRoot != null ) {
//                //--- зажигаем старт копирования
//                setRackLED( true );
//                doMoveCycle( fileWritableRoot );
//
//                //--- пытаемся освободить накопитель
//                AdvancedLogger.info( "Preparing the external drive to a safe shutdown..." );
//                if( usbUtility != null )
//                    CommonFunction.runCommand( null, usbUtility, "/stop_by_drive", fileWritableRoot.getCanonicalPath() );
//                if( arrUnmountCmd != null ) {
//                    CommonFunction.runCommand( null, arrUnmountCmd );
//                    setUSBPower( false );
//                }
//            }
//        }
//        setRackLED( false );
//
//        Thread.sleep( cyclePause );
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void doMoveCycle( File fileWritableRoot ) {
//        File newDelInfoFile = null;
//        try {
//            AdvancedLogger.info( "Found an external drive: " + fileWritableRoot.getCanonicalPath() );
//
//            //--- меняем конфиг file-indexer'a на внешний накопитель, если требуется
//            File newIndexerRoot = CommonFunction.replaceFileRoot( indexerConfigDefaultRoot, fileWritableRoot ).getFirst();
//            newIndexerRoot.mkdirs();
//            changeIndexerConfig( newIndexerRoot );
//
//            //--- меняем конфиг http-сервера на внешний накопитель и перезагружаем его, если требуется
//            File newHttpServerRoot = CommonFunction.replaceFileRoot( httpServerConfigDefaultRoot, fileWritableRoot ).getFirst();
//            newHttpServerRoot.mkdirs();
//            changeHttpServerConfig( newHttpServerRoot );
//
//            //--- готовим имена файлов del-info-file для периодического копирования
//            File oldDelInfoFile = new File( delInfoFileName );
//            Path oldDelInfoPath = Paths.get( delInfoFileName );
//            newDelInfoFile = CommonFunction.replaceFileRoot( delInfoFileName, fileWritableRoot ).getFirst();
//            Path newDelInfoPath = Paths.get( newDelInfoFile.getCanonicalPath() );
//
//            //--- составляем очередь видео файлов для переноса, отсортированную по времени начала записи
//            PriorityQueue<File> pqVideoFile = new PriorityQueue<>( 1024, new FileNameComparator() );
//
//            AdvancedLogger.info( "Start of transfer of files to an external drive..." );
//            //--- начинаем копирование
//            while( true ) {
//                //--- кнопка выключена из "рабочего" положения
//                if( ! getRackButtonStatus() ) {
//                    AdvancedLogger.info( "Working with an external drive is finished: shutdown button is pressed the drive." );
//                    break;
//                }
//
//                File sourFile = pqVideoFile.poll();
//
//                //--- в очереди нет файлов для копирования - надо перезаполнить очередь
//                if( sourFile == null ) {
//                    AdvancedLogger.debug( "Prepare a list of files..." );
//                    CommonFunction.collectFileQueue( tsStorageRoot, hsStorageExt, false, pqVideoFile );
//                    //--- если нет файлов для копирования - подождём
//                    if( pqVideoFile.isEmpty() ) {
//                        AdvancedLogger.debug( "No files to transfer. Wait " + ( cyclePause / 1000 ) + " seconds..." );
//                        Thread.sleep( cyclePause );
//                    }
//                    continue;
//                }
//
//                //--- Если файл нулевой длины - это или глюк ffmpeg'a - неполный файл - или текущий копируемый файл.
//                //--- Пропускаем без удаления, mms_data_clean потом его снесёт.
//                if( sourFile.length() == 0 ) continue;
//
//                //--- проверяем имя видео-файла на правильность
//                long[] arrFileTime = StringFunction.getTimeFromFileName( sourFile.getName(), "-.", timeZone );
//                //--- файл с неправильным названием (например, текущий/записываемый ffmpeg'ом файл)
//                if( arrFileTime == null ) continue;
//
//                //--- кончилось место на накопителе - проверяем 16-кратный запас (с учётом накруток на размере кластера и т.п.)
//                //--- или не менее 15% свободного места (NTFS начинает сильно фрагментироваться, если места остаётся меньше 13%)
//                if( fileWritableRoot.getFreeSpace() < sourFile.length() * 16 ||
//                    fileWritableRoot.getFreeSpace() < fileWritableRoot.getTotalSpace() * 15 / 100 ) {
//
//                    AdvancedLogger.debug( "Freeing up the external drive..." );
//                    //--- чистим 16-кратный запас, чтобы не дергать очистку каждый раз
//                    CommonFunction.clearStorage( fileWritableRoot, hsStorageExt, sourFile.length() * 16, 15, true );
//                }
//
//                AdvancedLogger.info( "Transferring to an external drive: " + sourFile.getCanonicalPath() );
//
//                Pair<File,File> arrFile = CommonFunction.replaceFileRoot( sourFile.getCanonicalPath(), fileWritableRoot );
//                arrFile.getFirst().getParentFile().mkdirs();
//
//                //--- извратище: вся работа с папками/файлами сделана на старом добром java.io.File,
//                //--- а для переноса приходится использовать новомодный NIO
//                Path sourPath = Paths.get( sourFile.getCanonicalPath() );
//                Path destPath = Paths.get( arrFile.getFirst().getCanonicalPath() );
//                //            try {
//                Files.move( sourPath, destPath, StandardCopyOption.REPLACE_EXISTING
//                        //--- эти опции не поддерживаются win7
//                                                    /*, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.ATOMIC_MOVE*/ );
//                //            }
//                //            //--- иногда мы хватаем ещё не освободившийся (например, не докопировавшийся) файл
//                //            catch( FileSystemException fse ) {
//                //                AdvancedLogger.error( "File skipped: " );
//                //                AdvancedLogger.error( fse );
//                //            }
//
//                //--- периодически копируем del-info-file (если есть)
//                if( oldDelInfoFile.exists() )
//                    Files.copy( oldDelInfoPath, newDelInfoPath, StandardCopyOption.REPLACE_EXISTING );
//            }
//        }
//        catch( Throwable t ) { AdvancedLogger.error( t ); }
//
//        //--- откатываем изменения ---
//
//        //--- удаляем del-info-file (если был)
//        try {
//            if( newDelInfoFile != null && newDelInfoFile.exists() )
//                newDelInfoFile.delete();
//        }
//        catch( Throwable t ) { AdvancedLogger.error( t ); }
//
//        //--- меняем конфиг file-indexer'a обратно на внутренний накопитель, если требуется
//        try {
//            changeIndexerConfig( new File( indexerConfigDefaultRoot ) );
//        }
//        catch( Throwable t ) { AdvancedLogger.error( t ); }
//
//        //--- меняем конфиг lighttpd/nginx'а обратно на внутренний накопитель и перезагружаем его, если требуется
//        try {
//            changeHttpServerConfig( new File( httpServerConfigDefaultRoot ) );
//        }
//        catch( Throwable t ) { AdvancedLogger.error( t ); }
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void changeIndexerConfig( File newPath ) throws Throwable {
//        //--- готовим требуемый конфиг из read-only копии
//        String copyFileName = new StringBuilder( indexerConfigFileName ).append( '-' ).toString();
//        ArrayList<String> alIndexerConfigCopy = CommonFunction.loadTextFile( copyFileName, "Cp1251", null, false );
//        //--- ищем строку с указанием корневой папки веб-сервера вида "index_root = c:\mmsserver\http_root\del_root"
//        for( int i = 0; i < alIndexerConfigCopy.size(); i++ )
//            if( alIndexerConfigCopy.get( i ).contains( CoreFileIndexer.CONFIG_INDEX_ROOT ) )
//                alIndexerConfigCopy.set( i, new StringBuilder( CoreFileIndexer.CONFIG_INDEX_ROOT ).append( " = " )
//                                                  .append( newPath.getCanonicalPath() ).toString() );
//
//        //--- сравниваем с текущим состоянием
//        boolean isEqual = false;
//        if( new File( indexerConfigFileName ).exists() ) {
//            ArrayList<String> alIndexerConfig = CommonFunction.loadTextFile( indexerConfigFileName, "Cp1251", null, false );
//            if( alIndexerConfigCopy.size() == alIndexerConfig.size() ) {
//                isEqual = true;
//                for( int i = 0; i < alIndexerConfigCopy.size(); i++ )
//                    if( ! alIndexerConfigCopy.get( i ).equals( alIndexerConfig.get( i ) ) ) {
//                        isEqual = false;
//                        break;
//                    }
//            }
//        }
//
//        if( ! isEqual ) CommonFunction.saveTextFile( indexerConfigFileName, "Cp1251", alIndexerConfigCopy );
//    }
//
//    private void changeHttpServerConfig( File newPath ) throws Throwable {
//        //--- готовим требуемый конфиг из read-only копии
//        String copyFileName = new StringBuilder( httpServerConfigFileName ).append( '-' ).toString();
//        ArrayList<String> alHTTPServerConfigCopy = CommonFunction.loadTextFile( copyFileName, "Cp1251", null, false );
//        //--- ищем строку с указанием корневой папки веб-сервера
//        for( int i = 0; i < alHTTPServerConfigCopy.size(); i++ )
//            switch( httpServerType ) {
//            case HTTP_SERVER_TYPE_LIGHTTPD:
//                // var.server_root = "/home/vc/MMSServerVideoLinuxARM/http_root"
//                if( alHTTPServerConfigCopy.get( i ).contains( "var.server_root" ) )
//                    alHTTPServerConfigCopy.set( i, new StringBuilder( "var.server_root = \"" )
//                                                .append( newPath.getCanonicalPath().replace( '\\', '/' ) ).append( '\"' ).toString() );
//                break;
//            case HTTP_SERVER_TYPE_NGINX:
//                // root /home/vc/MMSServerVideoLinuxARM/http_root;
//                if( alHTTPServerConfigCopy.get( i ).contains( "root" ) )
//                    alHTTPServerConfigCopy.set( i, new StringBuilder( "root " )
//                                                .append( newPath.getCanonicalPath().replace( '\\', '/' ) ).append( ';' ).toString() );
//                break;
//            }
//
//        //--- сравниваем с текущим состоянием
//        boolean isEqual = false;
//        if( new File( httpServerConfigFileName ).exists() ) {
//            ArrayList<String> alNginxConfig = CommonFunction.loadTextFile( httpServerConfigFileName, "Cp1251", null, false );
//            if( alHTTPServerConfigCopy.size() == alNginxConfig.size() ) {
//                isEqual = true;
//                for( int i = 0; i < alHTTPServerConfigCopy.size(); i++ )
//                    if( ! alHTTPServerConfigCopy.get( i ).equals( alNginxConfig.get( i ) ) ) {
//                        isEqual = false;
//                        break;
//                    }
//            }
//        }
//
//        if( ! isEqual ) {
//            CommonFunction.saveTextFile( httpServerConfigFileName, "Cp1251", alHTTPServerConfigCopy );
//            switch( httpServerType ) {
//            case HTTP_SERVER_TYPE_LIGHTTPD:
//                CommonFunction.runCommand( new File( httpServerFileName ).getParentFile(), httpServerFileName, "restart" );
//                break;
//            case HTTP_SERVER_TYPE_NGINX:
//                CommonFunction.runCommand( new File( httpServerFileName ).getParentFile(), httpServerFileName, "-s", "reload" );
//                break;
//            }
//        }
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private void setUSBPower( boolean status ) {
//        for( Integer usbNo : hmRelayUSBPower.keySet() )
//            try {
//                writeSingleCoil( hmRelayUSBPower.get( usbNo ), status );
//            }
//            catch( Throwable t ) {
//                AdvancedLogger.error( t );
//            }
//    }
//
//    private void setRackLED( boolean status ) {
//        try {
//            writeSingleCoil( relayRackLed, status );
//        }
//        catch( Throwable t ) {
//            AdvancedLogger.error( t );
//        }
//    }
//
//    private boolean getRackButtonStatus() {
//        boolean status = false;
//        try {
//            status = readDiscreteInputs( relayRackButton );
//        }
//        catch( Throwable t ) {
//            AdvancedLogger.error( t );
//            //--- надо бы mp3-шку какую проиграть...
//        }
//        return status;
//    }
//
////----------------------------------------------------------------------------------------------------------------------------------------
//
//    private static class FileNameComparator implements Comparator {
//        public int compare( Object o1, Object o2 ) {
//            return ( (File) o1 ).getName().compareTo( ( (File) o2 ).getName() );
//        }
//    }
//
//}
//
////1. Отслеживаем факт подключения USB устройства
////
////
////Для выполнения данной задачи я использовал свойство udev, которое позволяет выполнять скрипт при наступлении какого-либо события. Создадим правило, которое будет отвечать за подключение и отключение usb устройств:
////touch /etc/udev/rules.d/usb.rules
////
////Содержимое файла usb.rules:
////
////ACTION=="add", SUBSYSTEM=="block", ENV{ID_BUS}=="usb|mmc|memstick|ieee1394", RUN+="/bin/bash /etc/udev/usb_on.sh %E{ID_SERIAL_SHORT} %E{ID_MODEL} %E{ID_VENDOR}"
////ACTION=="remove", SUBSYSTEM=="block", ENV{ID_BUS}=="usb|mmc|memstick|ieee1394", RUN+="/bin/bash /etc/udev/usb_off.sh %E{ID_SERIAL_SHORT} %E{ID_MODEL} %E{ID_VENDOR}"
////
////Где:
////ACTION – отслеживаемое действие, add – подключение устройств, remove – отключение;
////ENV – перечень отслеживаемых устройств по типу;
////RUN – исполняемое действие. В данном случае, в зависимости от события, запускаются скрипты usb_on.sh и usb_off.sh.
////
////Скриптам usb_on.sh и usb_off.sh udev передает следующие данные:
////%E{ID_SERIAL_SHORT} – серийный номер USB устройства;
////%E{ID_MODEL} – модель USB устройства;
////%E{ID_VENDOR} – производитель USB устройства.

//package foatto.mms.core_mms.vc
//
//import foatto.core.util.AdvancedByteBuffer
//import foatto.core.util.AdvancedLogger
//import foatto.core_server.app.video.server.VideoFunction
//import foatto.core_server.ds.AbstractHandler
//import foatto.core_server.ds.CoreDataServer
//import foatto.core_server.ds.CoreDataWorker
//import foatto.mms.core_mms.ds.MMSHandler
//import java.io.File
//import java.io.FileOutputStream
//import java.nio.ByteOrder
//import java.nio.channels.SelectionKey
//import java.nio.channels.SocketChannel
//import java.util.HashMap
//import java.util.TreeSet
//
//class VideoServer : AbstractHandler() {
//
//    companion object {
//
//        const val MAGIC_WORD = 20100515  // 0x0132B5A3
//
//        const val TAG_ASK_COMMAND = 0
//        const val TAG_OBJECT_AND_CAMERA_INFO = 1
//        const val TAG_CAMERA_SHOT = 2
//        //    public static final int TAG_ = ___;
//        //    public static final int TAG_ = ___;
//        //    public static final int TAG_ = ___;
//
//        //--- максимальное время жизни/актуальности задания на загрузку картинки с камеры [сек]
//        //--- пока = 5 мин для случая, если картинки будет запрашивать и загружать через EDGE очень неторопливый клиент
//        private val IMAGE_JOB_LIFETIME = 5 * 60
//
//        //--- 1 млн. миллисекунд = примерно 16-17 мин
//        private val DEVICE_CONFIG_OUT_PERIOD: Long = 1000000
//    }
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    override var startBufSize: Int = 512 * 1024
//    override val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private var dirVideoRoot: String? = null
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private var magicWord = 0
//    private var protocolVersion: Byte = 0
//    private var inBufSize: Int = 0
//
//    private var deviceID: String? = null // выведено наружу исключительно для prepareErrorCommand
//    private var objectId = 0
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    private val sbCameraDescr = StringBuilder()
//    private val hmCameraDir = HashMap<String, File>()
//
//    private val tsJobDescr = TreeSet<String>()
//    private var lastDeviceConfigOutTime = System.currentTimeMillis()
//
////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    override fun init( aDataServer: CoreDataServer, aSelectionKey: SelectionKey ) {
//        super.init( aDataServer, aSelectionKey )
//
//        dirVideoRoot = dataServer.hmConfig[ VideoFunction.CONFIG_VIDEO_DIR ]
//    }
//
//    override fun preWork() {}
//
//    override fun prepareErrorCommand( dataWorker: CoreDataWorker ) {
//        AdvancedLogger.error( StringBuilder( " Disconnect from device ID = " ).append( deviceID ).toString() )
//    }
//
//    override fun oneWork( dataWorker: CoreDataWorker ): Boolean {
//
//        //--- заголовок, считывается только один раз (актуально для постоянного соединения) ---
//
//        if( magicWord == 0 ) {
//            if( bbIn.remaining() < 4 + 1 ) {
//                bbIn.compact()
//                return true
//            }
//
//            magicWord = bbIn.getInt()
//            if( magicWord != MAGIC_WORD ) {
//                AdvancedLogger.error( "Wrong magic word = $magicWord" )
//                return false
//            }
//
//            protocolVersion = bbIn.getByte()
//            if( protocolVersion < 0 || protocolVersion > 0 ) {
//                AdvancedLogger.error( "Wrong protocol version = ${protocolVersion.toInt()}" )
//                return false
//            }
//        }
//
//        //--- конец заголовка ---
//
//        //--- ждём получения размера ожидаемых данных
//        if( inBufSize == 0 ) {
//            if( bbIn.remaining() < 4 ) {
//                bbIn.compact()
//                return true
//            }
//            inBufSize = bbIn.getInt()
//        }
//        //--- ждём получения ожидаемых данных
//        if( bbIn.remaining() < inBufSize ) {
//            bbIn.compact()
//            return true
//        }
//
//        val tag = bbIn.getByte().toInt()
//        when( tag ) {
//
//            TAG_ASK_COMMAND -> {
//
//                val serialNo = bbIn.getInt()
//
//                val ( cmdID, cmdStr ) = MMSHandler.getCommand( dataWorker.alStm[ 0 ], serialNo )
//
//                //--- команда есть
//                var dataSize = 0
//                if( cmdStr != null ) {
//                    //--- и она не пустая
//                    if( !cmdStr.isEmpty() ) dataSize = 2 + cmdStr.length * 2    // putShortString
//                    //--- отметим успешную отправку команды
//                    MMSHandler.setCommandSended( dataWorker.alStm[ 0 ], cmdID )
//                }
//                val bbOut = AdvancedByteBuffer( 4 + dataSize )
//                bbOut.putInt( dataSize )
//                if( cmdStr != null && !cmdStr.isEmpty() ) bbOut.putShortString( cmdStr )
//
//                outBuf( bbOut )
//
//                //--- после всего
//                dataWorker.alConn[ 0 ].commit()
//            }
//
//            TAG_OBJECT_AND_CAMERA_INFO -> {
//
//                val hmObjectInfo = HashMap<String, String>()
//                val objectInfoCount = bbIn.getShort().toInt()
//                for( i in 0 until objectInfoCount ) hmObjectInfo.put( bbIn.getShortString(), bbIn.getShortString() )
//
//                //--- во избежание многократной перезагрузки конфигурации прибора.
//                //--- и раз в 16-17 мин перезагружаем конфигурацию контроллера на случай его перепривязки к другому объекту
//                if( deviceID == null || System.currentTimeMillis() - lastDeviceConfigOutTime > DEVICE_CONFIG_OUT_PERIOD ) {
//
//                    deviceID = hmObjectInfo[ VideoFunction.CONFIG_SERIAL_NO ]
//
//                    //--- определем объект по device_id
//                    val rs = dataWorker.alStm[ 0 ].executeQuery( " SELECT object_id FROM MMS_device WHERE device_id = $deviceID" )
//                    objectId = if( rs.next() ) rs.getInt( 1 ) else 0
//                    rs.close()
//
//                    //--- неизвестный прибор
//                    if( objectId == 0 ) {
//                        AdvancedLogger.error( "Unknown device ID = $deviceID" )
//                        return false
//                    }
//
//                    sbCameraDescr.setLength( 0 )
//                    hmCameraDir.clear()
//
//                    //--- обновляем (пересоздаём) описание _только_ своих камер
//                    dataWorker.alStm[ 0 ].executeUpdate( " DELETE FROM VC_camera WHERE name = '$deviceID' AND object_id = $objectId" )
//
//                    var cIndex = 0
//                    while( true ) {
//                        val descr = hmObjectInfo[ CoreImageSender.CONFIG_CAMERA_DESCR_ + cIndex ] ?: break
//
//                        dataWorker.alStm[ 0 ].executeUpdate(
//                            " INSERT INTO VC_camera ( id , object_id , name , descr , login , pwd , url_0 , url_1 , url_mjpeg , url_image , url_time , video_codec , audio_codec , duration ) VALUES ( " +
//                            dataWorker.alStm[ 0 ].getNextID( "VC_camera", "id" ) + " , $objectId , '$deviceID' , '$descr' , '' , '' , '' , '' , '' , '' , '' , 1 , 0 , 0 ) " )
//
//                        sbCameraDescr.append( if( sbCameraDescr.isEmpty() ) "" else " , " ).append( '\'' ).append( descr ).append( '\'' )
//
//                        //--- создадим папки под онлайн-кадры и видео
//                        val dirCamera = VideoFunction.getCameraDir( dirVideoRoot, objectId, descr )
//                        //dirCamera.mkdirs(); - достаточно последующего dirStream.mkdirs()
//                        hmCameraDir.put( descr, dirCamera )
//                        //--- для каждого потока
//                        for( streamIndex in 0 until VideoFunction.VIDEO_STREAM_COUNT ) {
//                            //--- создаём папку под поток с камеры
//                            val dirStream = File( dirCamera, Integer.toString( streamIndex ) )
//                            dirStream.mkdirs()
//                        }
//                        cIndex++
//                    }
//                    dataWorker.alConn[ 0 ].commit()
//                    lastDeviceConfigOutTime = System.currentTimeMillis()
//                }
//                //--- в качестве ответа ImageSender'у - отправка запроса на снимки
//                sendCameraShotQuery( dataWorker )
//            }
//
//            TAG_CAMERA_SHOT -> {
//
//                //--- грузим снимки и раздаём страждущим
//                for( descr in tsJobDescr ) {
//                    val fileSize = bbIn.getInt()
//                    if( fileSize > 0 ) {
//                        //--- загрузим текущий индекс файла
//                        val rs = dataWorker.alStm[ 0 ].executeQuery( " SELECT file_index FROM VC_job_image WHERE object_id = $objectId AND descr = '$descr' " )
//                        //--- переключаем индекс файла - если был 0, станет 1 и наоборот
//                        rs.next()
//                        val newFileIndex = 1 - rs.getInt( 1 )
//                        rs.close()
//
//                        val file = File( hmCameraDir[ descr ], "$newFileIndex.jpg" )
//                        //--- вот так по-дурацки, по-байтово, а всё потому,
//                        //--- что в файловый канал никак не записать только ЧАСТЬ буфера
//                        //FileChannel fileChannel = new FileOutputStream( file ).getChannel();
//                        val fos = FileOutputStream( file, false )
//                        for( c in 0 until fileSize ) fos.write( bbIn.getByte().toInt() )
//                        fos.close()
//
//                        //--- переключаем индекс файла на новый
//                        dataWorker.alStm[ 0 ].executeUpdate( " UPDATE VC_job_image SET file_index = $newFileIndex WHERE object_id = $objectId AND descr = '$descr' " )
//                    }
//                }
//                dataWorker.alConn[ 0 ].commit()
//                //--- в качестве ответа ImageSender'у - отправка запроса на снимки
//                sendCameraShotQuery( dataWorker )
//            }
//
//            else -> AdvancedLogger.error( "Unknown tag = $tag" )
//        }
//
//        //--- для возможного режима постоянного/длительного соединения
//        bbIn.compact()
//        inBufSize = 0
//
//        return true
//    }
//
//    private fun sendCameraShotQuery( dataWorker: CoreDataWorker ) {
//        //--- загружаем только актуальные задания
//        tsJobDescr.clear()
//        if( !sbCameraDescr.isEmpty() ) {
//            val rs = dataWorker.alStm[ 0 ].executeQuery(
//                " SELECT descr FROM VC_job_image WHERE object_id = $objectId AND descr IN ( $sbCameraDescr )  AND last_time > ${System.currentTimeMillis() / 1000 - IMAGE_JOB_LIFETIME}" )
//            while( rs.next() ) tsJobDescr.add( rs.getString( 1 ) )
//            rs.close()
//        }
//        var dataSize = 2   // alJobDescr.size();
//        for( descr in tsJobDescr ) dataSize += 2 + descr.length * 2    // putShortString
//
//        val bbOut = AdvancedByteBuffer( 4 + dataSize )
//        bbOut.putInt( dataSize )
//        bbOut.putShort( tsJobDescr.size )
//        for( descr in tsJobDescr ) bbOut.putShortString( descr )
//
//        outBuf( bbOut )
//    }
//
//
//}

package foatto.fs.core_fs.ds

import foatto.core.util.AdvancedByteBuffer
import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getZoneId
import foatto.core_server.ds.AbstractHandler
import foatto.core_server.ds.CoreDataServer
import foatto.core_server.ds.CoreDataWorker
import foatto.core_server.ds.DataMessage
import foatto.fs.core_fs.calc.getMeasureFile
import java.io.FileOutputStream
import java.nio.ByteOrder

//-------------------------------------------------------------------------------------------------------

class FotonHandler : AbstractHandler() {

    private val SEC_TO_2000 = 946684800
    private val zoneId = getZoneId( 0 )

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- конфигурация блока управления/модема
    private val CMD_GET_CONTROLLER_CONFIG = 1
    private val TAG_CONTROLLER_CONFIG = 1

    //--- список измерений
    private val CMD_GET_MEASURE_LIST = 2
    private val TAG_MEASURE_LIST = 2

   //--- получить пакет с данными
    private val CMD_GET_DATA = 3
    //--- пакет с данными
    private val TAG_DATA = 3

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override var startBufSize: Int = 4096
    override val byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var inBufSize: Int = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var controllerSerialNo: Int = 0
    //--- пользователь через привязку к контроллеру
    private var userID = 0
    //--- measure list
    private val alMeasureList = mutableListOf<FotonMeasure>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun preWork() {
        sendGetControllerConfig()
    }

    override fun prepareErrorCommand( dataWorker: CoreDataWorker ) {
        AdvancedLogger.error( " Disconnect from controller = $controllerSerialNo" )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork( dataWorker: CoreDataWorker ): Boolean {
        //--- ждём получения размера ожидаемых данных
        if( inBufSize == 0 ) {
            if( bbIn.remaining() < 4 ) {
                bbIn.compact()
                return true
            }
            inBufSize = bbIn.getInt()
AdvancedLogger.debug( "inBufSize = $inBufSize" )
        }

        //--- ждём основные данные
        if( bbIn.remaining() < inBufSize ) {
AdvancedLogger.debug( "remaining = ${bbIn.remaining()}" )
            bbIn.compact()
            return true
        }

        while( bbIn.remaining() > 0 ) {
//AdvancedLogger.debug( "data[ ${pos++} ] = " + Integer.toHexString( bbIn.getByte().toInt() and 0xFF ).toUpperCase() )
            
            //AdvancedLogger.debug( "remaining = " + bbIn.remaining() );
            //--- тег данных
            val tag = bbIn.getInt()
AdvancedLogger.debug( "tag = $tag" )
            when( tag ) {

                TAG_CONTROLLER_CONFIG -> {
                    val protocolVersion = bbIn.getInt()
                    val controllerVersion = bbIn.getInt()
                    controllerSerialNo = bbIn.getInt()
                    val controllerStatus = bbIn.getInt()
AdvancedLogger.debug( "protocolVersion = $protocolVersion" )
AdvancedLogger.debug( "controllerVersion = $controllerVersion" )
AdvancedLogger.debug( "controllerSerialNo = $controllerSerialNo" )
AdvancedLogger.debug( "controllerStatus = $controllerStatus" )

                    if( protocolVersion < 1 || protocolVersion > 1 ) {
                        AdvancedLogger.error( "Wrong protocol version = $protocolVersion" )
                        return false
                    }

                    //--- пытаемся определить пользователя по контроллеру
                    val rsUser = dataWorker.alStm[ 0 ].executeQuery( " SELECT user_id FROM FS_controller WHERE serial_no = '$controllerSerialNo' " )
                    val tmpUserID = if( rsUser.next() ) rsUser.getInt( 1 ) else null
                    rsUser.close()

                    //--- если такого контроллера нет - создаём его
                    if( tmpUserID == null ) {
                        val id = dataWorker.alStm[ 0 ].getNextID( "FS_controller", "id" )
                        dataWorker.alStm[ 0 ].executeUpdate(
                            " INSERT INTO FS_controller ( id , user_id , serial_no , version , status ) VALUES ( " +
                            " $id , 0 , $controllerSerialNo , $controllerVersion , $controllerStatus ) " )

                        userID = 0
                    }
                    //--- иначе просто обновляем статус
                    else {
                        dataWorker.alStm[ 0 ].executeUpdate( " UPDATE FS_controller SET status = $controllerStatus WHERE serial_no = '$controllerSerialNo' " )
                        userID = tmpUserID
                    }
                    dataWorker.alConn[ 0 ].commit()

                    //--- запрос на список измерений
                    sendGetMeasureList()
                }

                TAG_MEASURE_LIST -> {
                    alMeasureList.clear()

                    val measureCount = bbIn.getInt()
AdvancedLogger.debug( "measureCount = $measureCount" )

                    //--- по каждому измерению
                    for( measureNo in 0 until measureCount ) {
AdvancedLogger.debug( "measureNo = $measureNo" )
                        val measure = FotonMeasure( bbIn.getInt(), bbIn.getInt(), bbIn.getInt(), bbIn.getInt() + SEC_TO_2000, bbIn.getInt() )
AdvancedLogger.debug( "measure.deviceType = ${measure.deviceType}" )
AdvancedLogger.debug( "measure.deviceNo = ${measure.deviceNo}" )
AdvancedLogger.debug( "measure.handle = ${measure.handle}" )
AdvancedLogger.debug( "measure.onTime = ${DateTime_YMDHMS( zoneId, measure.onTime)}" )
AdvancedLogger.debug( "measure.size = ${measure.size}" )

                        alMeasureList.add( measure )
                    }
                    //--- если нет заданий/команд на срочное скачивание конкретного измерения (заложить в будущем) -
                    //--- то будем скачивать всё по порядку
                    var isNewData = false
                    for( measure in alMeasureList ) {
                        val curSize = checkMeasure( dataWorker, measure )
                        if( curSize < measure.size ) {
                            sendGetData( measure.handle, curSize )
                            isNewData = true
                            break
                        }
                    }
                    if( !isNewData ) return false
                }

                TAG_DATA -> {
                    val handle = bbIn.getInt()
                    val data = bbIn.get( inBufSize - 8 )

                    val measure = alMeasureList.find { it.handle == handle }!!

                    val curSize = saveData( dataServer, dataWorker, measure, data )

                    //--- продолжаем загрузку данных
                    if( curSize < measure.size ) sendGetData( measure.handle, curSize )
                    //--- перезапрос на новый список измерений
                    else sendGetMeasureList()
                }
            }
        }

        //--- для возможного режима постоянного/длительного соединения
        bbIn.compact()
        inBufSize = 0
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun sendGetControllerConfig() {
        val bbOut = AdvancedByteBuffer( 8, byteOrder ) // хватит и 6 байт, но для кеширования сделаем 8

        bbOut.putInt( CMD_GET_CONTROLLER_CONFIG )

        send( bbOut )
    }

    private fun sendGetMeasureList() {
        val bbOut = AdvancedByteBuffer( 8, byteOrder ) // хватит и 6 байт, но для кеширования сделаем 8

        bbOut.putInt( CMD_GET_MEASURE_LIST )

        send( bbOut )
    }

    private fun sendGetData( handle: Int, offset: Int ) {
        val bbOut = AdvancedByteBuffer( 8, byteOrder ) // хватит и 6 байт, но для кеширования сделаем 8

        bbOut.putInt( CMD_GET_DATA )
        bbOut.putInt( handle )
        bbOut.putInt( offset )
        bbOut.putInt( 0 )   // с размером страницы прибор сам разберётся

        send( bbOut )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun send( bbOut: AdvancedByteBuffer ) {
        bbOut.flip()
        clqOut.offer( DataMessage( byteBuffer = bbOut ) )
        dataServer.putForWrite( this )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun checkMeasure( dataWorker: CoreDataWorker, measure: FotonMeasure ): Int {
        val stm = dataWorker.alStm[ 0 ]
        val result: Int

        //--- ищем существующую запись по прибору
        var rs = stm.executeQuery(
            " SELECT id FROM FS_device " +
            " WHERE type = ${measure.deviceType} " +
            " AND serial_no = '${measure.deviceNo}' ")
        val deviceID =
            if( rs.next() ) {
                val id = rs.getInt( 1 )
                rs.close()

                id
            }
            else {
                rs.close()

                //--- новый прибор
                val id = stm.getNextID( "FS_device", "id" )
                stm.executeUpdate(
                    " INSERT INTO FS_device ( id , user_id , type , serial_no , version , spec ) VALUES ( " +
                    " $id , $userID , ${measure.deviceType} , '${measure.deviceNo}' , '0' , '0' ) " )

                id
            }

        //--- ищем существующее измерение
        rs = stm.executeQuery(
            " SELECT id , all_size FROM FS_measure " +
            " WHERE device_id = $deviceID " +
            " AND beg_time = ${measure.onTime} " )

        if( rs.next() ) {
            var p = 1
            measure.id = rs.getInt( p++ )
            val oldAllSize = rs.getInt( p++ )
            rs.close()

            //--- если изменился размер измерения
            if( measure.size != oldAllSize )
                dataWorker.alStm[ 0 ].executeUpdate( " UPDATE FS_measure SET all_size = ${measure.size} WHERE id = ${measure.id} " )

            val curSize = getMeasureFile( dataServer.rootDirName, measure.id ).length().toInt()

            result = curSize
        }
        else {
            rs.close()

            //--- новое измерение - создаём необходимое
            val id = stm.getNextID( "FS_measure", "id" )
            stm.executeUpdate(
                " INSERT INTO FS_measure ( id , user_id , object_id , device_id , beg_time , cur_size , all_size ) VALUES ( " +
                " $id , $userID , 0 , $deviceID , ${measure.onTime} , 0 , ${measure.size} ) " )

            measure.id = id
            result = 0
        }

        dataWorker.alConn[ 0 ].commit()

        return result
    }

    private fun saveData( dataServer: CoreDataServer, dataWorker: CoreDataWorker, measure: FotonMeasure, data: ByteArray ): Int {
        val file = getMeasureFile( dataServer.rootDirName, measure.id )
        val out = FileOutputStream( file, true )
        out.write( data )
        out.close()

        val curSize = file.length().toInt()

        dataWorker.alStm[ 0 ].executeUpdate( " UPDATE FS_measure SET cur_size = $curSize WHERE id = ${measure.id} " )
        dataWorker.alConn[ 0 ].commit()

        return curSize
    }

}

class FotonMeasure( val deviceType: Int, val deviceNo: Int, val handle: Int, val onTime: Int, var size: Int ) {
    var id: Int = 0
}

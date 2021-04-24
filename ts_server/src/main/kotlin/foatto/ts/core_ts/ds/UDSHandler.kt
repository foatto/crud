package foatto.ts.core_ts.ds

import foatto.core.util.AdvancedLogger
import foatto.core_server.ds.CoreDataWorker
import java.nio.ByteOrder

class UDSHandler : TSHandler() {

    private val sbData = StringBuilder()

    //--- в общем-то всё равно, т.к. принимаются/передаются передаются строки,
    //--- но при передаче упакованных данных может пригодится
    override val byteOrder: ByteOrder = ByteOrder.LITTLE_ENDIAN

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun oneWork(dataWorker: CoreDataWorker): Boolean {
        //        //--- данных нет - ждём
        //        if( ! bbIn.hasRemaining() ) {
        //            bbIn.compact();
        //            return true;
        //        }

        val arrByte = ByteArray(bbIn.remaining())
        bbIn.get(arrByte)
        sbData.append(String(arrByte))

//        //--- первый символ - д.б. #
//        if( sbData[ 0 ] != '#' ) {
//            AdvancedLogger.error( "Wrong data = " + sbData )
//            return false
//        }
//
//        //--- минимальный размер пакета = 5 символов/байт
//        if( sbData.length < 5 ) {
//            bbIn.compact()
//            return true
//        }

        //--- хотя бы один пакет собрался в общей строке данных?
        val packetEndPos = sbData.indexOf("\r\n")
        if (packetEndPos < 0) {
            bbIn.compact()
            return true
        }
//        val packetData = sbData.substring( secondDelimiterPos + 1, packetEndPos )

        AdvancedLogger.debug("data = ${sbData.substring(0, packetEndPos)}")

        //--- убираем разобранный пакет из начала общей строки данных
        sbData.delete(0, packetEndPos + 2)

//        sbStatus.append("DataRead;")

//        for( stm in dataWorker.alStm ) sqlBatchData.execute( stm )

//        //--- отправка ответа
//        val bbOut = AdvancedByteBuffer( answer.length, byteOrder )
//        bbOut.put( answer.toByteArray() )
//        outBuf( bbOut )

//        //--- данные успешно переданы - теперь можно завершить транзакцию
//        sbStatus.append("Ok;")
//        errorText = null
//        writeSession(dataWorker.alConn, dataWorker.alStm[0], true)

        //--- для возможного режима постоянного/длительного соединения
        bbIn.compact()     // нельзя .clear(), т.к. копятся данные следующего пакета

        begTime = 0
        sbStatus.setLength(0)
        dataCount = 0
        dataCountAll = 0
        firstPointTime = 0
        lastPointTime = 0

        //--- это накопительная строка данных, очищается в процессе разбора
        //sbData.setLength( 0 );
        return true
    }
}

package foatto.server.ds

import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import java.nio.ByteOrder
import java.nio.channels.SelectionKey
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentLinkedQueue

abstract class AbstractNioHandler {

    //--- время последней обработки/укладки в очередь
    @Volatile
    var lastWorkTime = 0

    //--- очередь входных данных
    val clqIn = ConcurrentLinkedQueue<DataMessage>()

    //--- очередь выходных данных
    val clqOut = ConcurrentLinkedQueue<DataMessage>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected abstract var startBufSize: Int
    protected abstract val byteOrder: ByteOrder

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var dataServer: CoreNioServer
    var selectionKey: SelectionKey? = null

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- основной/рабочий буфер входных данных
    protected lateinit var bbIn: AdvancedByteBuffer

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    open fun init(aDataServer: CoreNioServer, aSelectionKey: SelectionKey) {
        dataServer = aDataServer
        selectionKey = aSelectionKey

        bbIn = AdvancedByteBuffer(startBufSize, byteOrder)
    }

    //--- всякие освобождающие ресурсы/память процедуры
    fun free() {
        selectionKey?.let { sk ->
            //--- закрыть/удалить мёртвое/ошибочное соединение
            try {
                val socketChannel = sk.channel() as SocketChannel
                AdvancedLogger.debug("Connection closed = ${socketChannel.remoteAddress}")
                socketChannel.close()
            } catch (t: Throwable) {
                AdvancedLogger.error(t)
            }

            sk.attach(null)    // отвяжем себя от него
            sk.cancel()
            //--- именно selectionKey, а не локальный sk
            selectionKey = null            // отвяжем его от себя
        }
    }

    open fun work(dataWorker: CoreNioWorker): Boolean {
        //--- Намеренно не будем проверять:
        //--- - на null - т.к. других забирателей данных из очереди нет
        //--- - нулевой размер буфера в очереди для CMD_DATA - таких там просто не должно быть,
        //--- а если так получилось - ищем ошибку в соответствующих местах
        //if( bb != null && bb.remaining() > 0 )

        val dataMessage = clqIn.poll()

        when (dataMessage.cmd) {
            DataMessage.CMD_PRE_WORK -> {
                preWork()
                return true
            }

            DataMessage.CMD_DATA -> {
                bbIn.put(dataMessage.byteBuffer!!.buffer).flip()
                return oneWork(dataWorker)
            }

            DataMessage.CMD_CLOSE ->
                //--- это не ошибка, а нормальное закрытие канала клиентом
                //writeError( dataWorker.alConn.get( 0 ), dataWorker.alStm.get( 0 ), new StringBuilder( " Disconnect from controller ID = " ).append( getControllerID() ).toString() );
                return false

            DataMessage.CMD_ERROR -> {
                prepareErrorCommand(dataWorker)
                return false
            }
        }
        return true
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected abstract fun preWork()
    protected abstract fun oneWork(dataWorker: CoreNioWorker): Boolean
    protected abstract fun prepareErrorCommand(dataWorker: CoreNioWorker)

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun outBuf(bbOut: AdvancedByteBuffer) {
        bbOut.flip()
        val socketChannel = selectionKey!!.channel() as SocketChannel
        while (bbOut.hasRemaining()) {
            socketChannel.write(bbOut.buffer)
        }
    }
}

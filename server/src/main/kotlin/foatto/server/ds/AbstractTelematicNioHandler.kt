package foatto.server.ds

import foatto.core.util.getCurrentTimeInt
import java.io.File
import java.nio.channels.SelectionKey
import java.time.ZoneId

abstract class AbstractTelematicNioHandler : AbstractNioHandler() {

    protected val zoneId: ZoneId = ZoneId.systemDefault()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override var startBufSize: Int = 1024

    protected abstract val configSessionLogPath: String
    protected abstract val configJournalLogPath: String

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var dirSessionLog: File
    protected lateinit var dirJournalLog: File

    //--- тип прибора - должен переопределяться в наследниках
    protected var deviceType = -1

    //--- время начала сессии
    protected var begTime = 0

    //--- запись состояния сессии
    protected var status = ""

    //--- текст ошибки
    protected var errorText = ""

    //--- количество записанных блоков данных (например, точек)
    protected var dataCount = 0

    //--- количество считанных блоков данных (например, точек)
    protected var dataCountAll = 0

    //--- время первого и последнего блока данных (например, точки)
    protected var firstPointTime = 0
    protected var lastPointTime = 0

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun init(aDataServer: CoreNioServer, aSelectionKey: SelectionKey) {
        super.init(aDataServer, aSelectionKey)

        dirSessionLog = File(dataServer.hmConfig[configSessionLogPath]!!)
        dirJournalLog = File(dataServer.hmConfig[configJournalLogPath]!!)

        begTime = getCurrentTimeInt()
        status += " Init;"
    }

    override fun work(dataWorker: CoreNioWorker): Boolean {
        if (begTime == 0) {
            begTime = getCurrentTimeInt()
        }

        return super.work(dataWorker)
    }

    override fun preWork() {
        status += " Start;"
    }

}
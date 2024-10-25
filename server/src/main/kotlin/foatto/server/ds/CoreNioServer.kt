package foatto.server.ds

import foatto.core.util.getCurrentTimeInt
import foatto.server.util.AdvancedByteBuffer
import foatto.server.util.AdvancedLogger
import foatto.server.util.AsyncFileSaver
import foatto.server.util.loadConfig
import foatto.server.sql.DBConfig
import java.io.File
import java.net.InetSocketAddress
import java.nio.channels.CancelledKeyException
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min

abstract class CoreNioServer protected constructor(private val configFileName: String) {

    companion object {

        private val CONFIG_ROOT_DIR = "root_dir"
        const val CONFIG_TEMP_DIR = "temp_dir"
        private val CONFIG_LOG_PATH = "log_path"
        private val CONFIG_LOG_OPTIONS = "log_options"
        private val CONFIG_SERVER_PORT_ = "server_port_"
        private val CONFIG_SERVER_CLASS_ = "server_class_"
        const val CONFIG_MAX_WORKER_COUNT = "max_worker_count"
        private val CONFIG_MAX_HANDLER_INACTIVE_TIME = "max_handler_inactive_time"
        private val CONFIG_MAX_SESSION_INACTIVE_TIME = "max_session_inactive_time"

        private val CONFIG_DB_PING_INTERVAL = "db_ping_interval"
        private val CONFIG_DB_PING_QUERY = "db_ping_query"

        //--- 1000 секунд = примерно 16-17 мин
        protected val STATISTIC_OUT_PERIOD = 1000

        //--- проверяем флаг рестарта сервера не чаще чем раз в 10 секунд
        private val RESTART_CHECK_PERIOD = 10
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    lateinit var hmConfig: MutableMap<String, String>

    //--- путь к корневой папке сервера
    lateinit var rootDirName: String

    //--- путь к временной папке
    lateinit var tempDirName: String

    //--- для перекрытия и переопределения в android-версии
    lateinit private var logPath: String
    lateinit private var logOptions: String

    private val hmServerPortClass = mutableMapOf<Int, String>()

    //--- максимальное кол-во worker'ов
    private var maxWorkerCount = 0

    //--- максимальное время жизни задания/обработчика в бездействующем состоянии [мин]
    private var maxHandlerInactiveTime = 0

    //--- максимальное время жизни сессии в бездействующем состоянии [мин]
    private var maxSessionInactiveTime = 0

    //--- параметры SQL-базы
    lateinit var dbConfig: DBConfig

    //--- интервал проверки SQL-соединения [мин]
    var dbPingInterval = 0

    //--- запрос на проверку SQL-соединения
    lateinit var dbPingQuery: String

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //!!! у котлиновского Any нет функций wait/notify, заменить в будущем на более идиоматичный вариант
    val lock = Object()

    @Volatile
    var workerCount = 0

    @Volatile
    var successResultCount = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private var lastSessionCheckTime = getCurrentTimeInt()

    val chmSessionTime = ConcurrentHashMap<Long, Int>()
    val chmSessionStore = ConcurrentHashMap<Long, ConcurrentHashMap<String, Any>>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- очередь Handler'ов для обработки входной информации: препроцессинга или чтения/обработки данных
    private var clqIn = ConcurrentLinkedQueue<AbstractNioHandler>()

    //--- очередь Handler'ов для обработки выходной информации: записи или закрытия
    private var clqOut: ConcurrentLinkedQueue<AbstractNioHandler> = ConcurrentLinkedQueue()

    //--- очередь Handler'ов на закрытие
    private var clqClose = ConcurrentLinkedQueue<AbstractNioHandler>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private lateinit var selector: Selector
    private var startBufSize = 1024
    private var lastStatisticOutTime = getCurrentTimeInt()

    private var begWorkTime = getCurrentTimeInt()

    @Volatile
    var workTime = 0

    private var runtime = Runtime.getRuntime()
    private var lastRestartCheckTime = getCurrentTimeInt()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadConfig() {
        hmConfig = loadConfig(configFileName)

        rootDirName = hmConfig[CONFIG_ROOT_DIR]!!
        tempDirName = hmConfig[CONFIG_TEMP_DIR]!!

        logPath = hmConfig[CONFIG_LOG_PATH]!!
        logOptions = hmConfig[CONFIG_LOG_OPTIONS]!!

        var index = 0
        while (hmConfig[CONFIG_SERVER_CLASS_ + index] != null) {
            hmServerPortClass[hmConfig[CONFIG_SERVER_PORT_ + index]!!.toInt()] = hmConfig[CONFIG_SERVER_CLASS_ + index]!!
            index++
        }

        maxWorkerCount = Integer.parseInt(hmConfig[CONFIG_MAX_WORKER_COUNT])
        maxHandlerInactiveTime = 60 * hmConfig[CONFIG_MAX_HANDLER_INACTIVE_TIME]!!.toInt()
        maxSessionInactiveTime = 60 * hmConfig[CONFIG_MAX_SESSION_INACTIVE_TIME]!!.toInt()

        dbConfig = DBConfig.loadConfig(hmConfig).first()

        dbPingInterval = 60 * hmConfig[CONFIG_DB_PING_INTERVAL]!!.toInt()
        dbPingQuery = hmConfig[CONFIG_DB_PING_QUERY]!!
    }

    fun run() {
        try {
            loadConfig()

            AdvancedLogger.init(logPath, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
            AdvancedLogger.info("==================== DataServer started ====================")

            //--- запуск асинхронного записывателя файлов
            AsyncFileSaver.init(rootDirName)

            val fileRestartFlag = File("${configFileName}_")

            //--- на старте всегда запускаем первого обработчика, чтобы сразу получить возможные ошибки подключения к базе
            runNewDataWorker()

            //--- внутри содержит SelectorProvider.provider().openSelector();
            selector = Selector.open()

            //--- инициализация слушателей портов
            val hmServerSocketChannelClass = mutableMapOf<ServerSocketChannel, Class<*>>()
            for (serverPort in hmServerPortClass.keys) {
                val serverSocketChannel = ServerSocketChannel.open()
                serverSocketChannel.configureBlocking(false)
                //--- особой необходимости здесь нет - стандартного буфера для _приёма_соединений_ вполне хватает
                //serverSocketChannel.setOption( StandardSocketOptions.SO_RCVBUF, 64 * 1024 );
                serverSocketChannel.socket().reuseAddress = true   // обязательно ДО bind()
                serverSocketChannel.socket().bind(InetSocketAddress(serverPort))
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT)
                //--- припишем для каждого серверного канала - своё имя класса-обработчика
                hmServerSocketChannelClass[serverSocketChannel] = Class.forName(hmServerPortClass[serverPort])
            }

            //--- тайм-аут селектора должен быть равен (или даже меньше) самому маленькому из остальных таймаутов/периодов
            val selectorTimeOut = calcMinSelectorTimeOut()

            while (true) {
                //--- периодическая проверка на наличие файла-флага перезагрузки
                if (getCurrentTimeInt() - lastRestartCheckTime > RESTART_CHECK_PERIOD) {
                    //--- рестарт по обнаружению рестарт-файла
                    if (fileRestartFlag.exists()) {
                        fileRestartFlag.delete()
                        AdvancedLogger.info("==================== restart by flag-file ====================")
                        break
                    }
                    lastRestartCheckTime = getCurrentTimeInt()
                }

                //--- если обработчиков совсем не осталось (например, позакрывались из-за пропажи коннекта к базе),
                //--- или это первый цикл работы сервера, то запустим новый обработчик
                if (workerCount <= 0) {
                    runNewDataWorker()
                }

                val keyCount = selector.select(selectorTimeOut * 1000L)
                if (keyCount == 0) {
                    //--- здесь можно поставить sleep или запись забуферизованных данных или ещё какую хрень - не придумал ещё
                    //...
                    //--- эта ситуация - не ошибка, т.к. бывает wakeup() при пустом селекторе - например, для установки OP_WRITE или для закрытия каналов/объектов
                    //dsLog.debug( "keyCount == 0" );
                    //continue;
                }

                val keys = selector.selectedKeys()
                for (key in keys) {
                    if (!key.isValid) {
                        AdvancedLogger.error("Key not valid: $key")
                        continue
                    }

                    //--- Принимаем входящее соединение
                    if (key.isAcceptable) {
                        val ssc = key.channel() as ServerSocketChannel
                        val socketChannel = ssc.accept()
                        socketChannel.configureBlocking(false)
                        //--- При большой необходимости можно увеличить буфферы.
                        //--- Однако, стандартного значения (по умолчанию равного размеру системного буфера) должно хватать всегда
                        //socketChannel.socket().setReceiveBufferSize( 512 * 1024 );
                        //socketChannel.socket().setSendBufferSize( 512 * 1024 );
                        //--- привязка сокетного канала и его обработчика
                        val handler = hmServerSocketChannelClass[ssc]!!.getConstructor().newInstance() as AbstractNioHandler
                        handler.init(this, socketChannel.register(selector, SelectionKey.OP_READ, handler))

                        //--- добавить в список обрабатываемых объектов
                        putHandler(handler)
                        //--- предварительная обработка соединения (например, посылка первой команды ModBus-подобному устройству)
                        putMessage(handler, DataMessage(DataMessage.CMD_PRE_WORK))
                        //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
                        //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
                        AdvancedLogger.debug("Connection opened = ${socketChannel.socket().inetAddress}")
                        AdvancedLogger.debug("Handler [create] = ${clqIn.size}")
                    } else if (key.isReadable) {
                        val handler = key.attachment() as AbstractNioHandler
                        val socketChannel = key.channel() as SocketChannel
                        //--- собственно чтение
                        try {
                            //--- в данном случае порядок байт значения не имеет, т.к. буфер будет побайтово скопирован в большой/рабочий буфер
                            val bb = AdvancedByteBuffer(startBufSize)
                            val num = socketChannel.read(bb.buffer)
                            //--- канал нормально закрыт клиентом
                            if (num == -1) {
                                putMessage(handler, DataMessage(DataMessage.CMD_CLOSE))
                            }
                            //--- волшебный случай, однако стоит предусмотреть
                            else if (num == 0) {
                                AdvancedLogger.error("num == 0")
                            } else {
                                bb.flip()
                                putMessage(handler, DataMessage(byteBuffer = bb))

                                //--- если буфер был полностью заполнен и памяти хватит, чтобы его увеличить, то вероятно, его не хватает и его надо/можно увеличить
                                if (num == startBufSize && startBufSize * 2 < (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) / 2) {
                                    startBufSize *= 2
                                }
                                //--- если буфер заполнен менее чем наполовину и его ещё можно уменьшить, то его стоит/можно уменьшить
                                else if (num < startBufSize / 2 && startBufSize > 8) {
                                    startBufSize /= 2
                                }
                            }
                        } catch (t: Throwable) {
                            closeOnError(key, handler, socketChannel, t)
                        }
                    } else if (key.isWritable) {
                        val handler = key.attachment() as AbstractNioHandler
                        val socketChannel = key.channel() as SocketChannel
                        try {
                            //--- только один разочек, чтобы главный поток селектора не "зависал" на отправке больших данных клиентам на медленных каналах
                            if (!handler.clqOut.isEmpty()) {
                                //--- берём очередной буфер для записи, но пока не удаляем его - возможно, он не запишется полностью
                                val dataMessage = handler.clqOut.peek()
                                //--- попытаемся записать буфер в сокет
                                socketChannel.write(dataMessage.byteBuffer!!.buffer)
                                //--- если записался полностью, удаляем из очереди
                                if (dataMessage.byteBuffer.remaining() == 0) {
                                    handler.clqOut.poll()
                                }
                            }
                            //--- если запись закончена, переходим в режим чтения/ожидания входящих данных
                            if (handler.clqOut.isEmpty()) {
                                key.interestOps(SelectionKey.OP_READ)
                            }
                        } catch (t: Throwable) {
                            closeOnError(key, handler, socketChannel, t)
                        }
                    }
                }
                keys.clear()

                //--- перевести соответствующие селекторы в режим записи
                //--- Намеренно не будем проверять:
                //--- - на null - т.к. нет других забирателей/удалятелей данных из очереди, и !isEmpty должно явно указывать на наличие данных
                //--- - нулевой размер очереди для записи - т.к. это может быть "инициация записи для удаления"
                while (!clqOut.isEmpty()) {
                    val handler = clqOut.poll()
                    try {
                        //--- получаем ключ селектора из очереди и пытаемся перевести канал в режим записи/отправки данных.
                        //--- и если клиент уже отвалился, то такая ситуация даёт CancelledKeyException, который надо использовать для закрытия обработчика
                        handler.selectionKey!!.interestOps(SelectionKey.OP_WRITE)
                    } catch (cke: CancelledKeyException) {
                        clqClose.offer(handler)
                    }
                }

                //--- закрытие/удаление объектов
                while (!clqClose.isEmpty()) {
                    val handler = clqClose.poll()
                    //--- освобождаем ресурсы
                    handler.free()

                    //--- проверка: этот цикл не должен сработать ни разу - т.к. закрываемый объект вместо clqIn должен попасть в clqClose
                    while (clqIn.remove(handler)) {
                        AdvancedLogger.error("Handler in clqIn at closing time !")
                    }

                    //--- проверка: этот цикл не должен сработать ни разу - т.к. закрываемый объект не может попасть в clqOut,
                    //--- т.к. в clqClose он попадает не в режиме работы, а сам clqOut уже очищен в предыдущем блоке
                    while (clqOut.remove(handler)) {
                        AdvancedLogger.error("Handler in clqOut at closing time !")
                    }

                    AdvancedLogger.debug("Handler [close] = ${clqIn.size}")
                }
                //--- периодическая очистка залежавшихся сессий
                if (getCurrentTimeInt() - lastSessionCheckTime > maxSessionInactiveTime) {
                    val alsessionId = ArrayList<Long>(chmSessionTime.size)
                    //--- чтобы не получить ошибку при удалении во время перебора -
                    //--- просто запишем sessionId, а удалять будем позже по списку
                    for (sessionId in chmSessionTime.keys) {
                        if (getCurrentTimeInt() - chmSessionTime[sessionId]!! > maxSessionInactiveTime) {
                            alsessionId.add(sessionId)
                        }
                    }
                    for (sessionId in alsessionId) {
                        chmSessionTime.remove(sessionId)
                        chmSessionStore.remove(sessionId)
                    }
                    lastSessionCheckTime = getCurrentTimeInt()
                }
                //--- всяческая статистика
                if (AdvancedLogger.isInfoEnabled && getCurrentTimeInt() - lastStatisticOutTime > STATISTIC_OUT_PERIOD) {
                    AdvancedLogger.info("======== Busy Statistic =======")
                    AdvancedLogger.info(" Workers = $workerCount")
                    AdvancedLogger.info(" Handlers = ${clqIn.size}")
                    AdvancedLogger.info(" Busy % = ${100 * workTime / (getCurrentTimeInt() - begWorkTime)}")
                    AdvancedLogger.info("======= Memory Statistic ======")
                    AdvancedLogger.info(" Buffer Size = $startBufSize")
                    AdvancedLogger.info(" Used = ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}")
                    AdvancedLogger.info(" Free = ${runtime.freeMemory() / 1024 / 1024}")
                    AdvancedLogger.info(" Total = ${runtime.totalMemory() / 1024 / 1024}")
                    AdvancedLogger.info(" Max = ${runtime.maxMemory() / 1024 / 1024}")
                    AdvancedLogger.info("===============================")
                    lastStatisticOutTime = getCurrentTimeInt()
                }
            }
        } catch (t: Throwable) {
            //--- для сложных случаев отладки в cmd-окне
            t.printStackTrace()
            AdvancedLogger.error(t)
        } finally {
            AsyncFileSaver.close()

            AdvancedLogger.info("==================== DataServer stopped ====================")
            AdvancedLogger.close()
        }
    }

    private fun closeOnError(key: SelectionKey, handler: AbstractNioHandler, socketChannel: SocketChannel, t: Throwable) {
        //--- надо сразу же отменить регистрацию ключа в селекторе,
        //--- чтобы до реального закрытия обработчика не нагенерировалась куча входящих ошибок
        //--- (в close этот ключ ещё раз закроется, но это не критично)
        key.cancel()
        //handler.selectionKey.cancel();
        //--- непредвиденное закрытие канала
        putMessage(handler, DataMessage(DataMessage.CMD_ERROR))
        try {
            //--- SocketChannel.getRemoteAddress(), который есть в Oracle Java, не существует в Android Java,
            //--- поэтому используем более общий метод SocketChannel.socket().getInetAddress()
            AdvancedLogger.debug("Connection closed for Exception = ${socketChannel.socket().inetAddress}")
        } catch (t2: Throwable) {
        }

        AdvancedLogger.error(t)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getHandler(): AbstractNioHandler? {
        var result: AbstractNioHandler? = null
        //--- size() слишком дорогая операция, чтобы крутить её в цикле
        val count = clqIn.size
        //--- очередь заданий достаточно прокрутить только один раз за один запрос
        for (i in 0 until count) {
            val handler = clqIn.poll()
            //--- если очередь внезапно/досрочно закончилась (в случае нескольких одновременных запросов)
            if (handler == null) {
                result = null
                break
            }
            //--- пустое/неактивное задание
            else if (handler.clqIn.isEmpty()) {
                //--- проверяем на время последней обработки
                //--- (иногда каналы не срабатывают на внезапный обрыв с той стороны и обработчик навсегда остаётся в clqIn с пустой входящей очередью)
                if (getCurrentTimeInt() - handler.lastWorkTime > maxHandlerInactiveTime) {
                    //--- кладём сообщение о закрытие и отдаём на обработку (делать CMD_ERROR нельзя, т.к. данное состояние - не всегда ошибка)
                    handler.clqIn.offer(DataMessage(DataMessage.CMD_CLOSE))
                    result = handler
                    break
                }
                //--- кладём обратно в очередь пустое задание
                else {
                    clqIn.offer(handler)
                }
            }
            //--- ура, мы нашли задание с непустой входной очередью
            else {
                result = handler
                break
            }
        }
        //--- сколько успешных запросов подряд получилось?
        successResultCount = if (result == null) {
            0
        } else {
            successResultCount + 1
        }
        try {
            //--- не пора ли запустить ещё один worker?
            if (successResultCount > workerCount && workerCount < maxWorkerCount) {
                //--- во избежание почти одновременного запуска нескольких обработчиков обнуляем счётчик
                successResultCount = 0
                runNewDataWorker()
            }
        } catch (t: Throwable) {
            AdvancedLogger.error(t)
        }

        return result
    }

    private fun putMessage(handler: AbstractNioHandler, dataMessage: DataMessage) {
        handler.clqIn.offer(dataMessage)
        synchronized(lock) {
            //--- notifyAll() не нужен - на одно событие одного обработчика достаточно разбудить один процесс-worker
            lock.notify()
        }
    }

    fun putHandler(handler: AbstractNioHandler) {
        handler.lastWorkTime = getCurrentTimeInt()
        clqIn.offer(handler)
    }

    fun putForWrite(handler: AbstractNioHandler) {
        handler.lastWorkTime = getCurrentTimeInt()
        clqOut.offer(handler)
        selector.wakeup()
    }

    fun putForClose(handler: AbstractNioHandler) {
        clqClose.offer(handler)
        selector.wakeup()
    }

    //--- вычисление минимального тайм-аута селектора соединений
    private fun calcMinSelectorTimeOut(): Int {
        var selectorTimeOut = min(RESTART_CHECK_PERIOD, STATISTIC_OUT_PERIOD)
        selectorTimeOut = min(selectorTimeOut, maxHandlerInactiveTime)
        selectorTimeOut = min(selectorTimeOut, maxSessionInactiveTime)
        selectorTimeOut = min(selectorTimeOut, dbPingInterval)
        return selectorTimeOut
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- запуск своей специфичной для платформы версии обработчика
    protected abstract fun runNewDataWorker()

}

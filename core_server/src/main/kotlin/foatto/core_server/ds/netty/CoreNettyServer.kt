package foatto.core_server.ds.netty

import foatto.core.util.AdvancedLogger
import foatto.core.util.loadConfig
import foatto.sql.DBConfig
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.io.File

abstract class CoreNettyServer protected constructor(private val configFileName: String) {

    companion object {

        private val CONFIG_ROOT_DIR = "root_dir"
        const val CONFIG_TEMP_DIR = "temp_dir"
        private val CONFIG_LOG_PATH = "log_path"
        private val CONFIG_LOG_OPTIONS = "log_options"
        private val CONFIG_PORT = "port"
        private val CONFIG_DECODER_CLASS = "decoder_class"
        private val CONFIG_ENCODER_CLASS = "encoder_class"
        private val CONFIG_HANDLER_CLASS = "handler_class"

        private val CONFIG_SESSION_LOG_PATH = "log_session"
        private val CONFIG_JOURNAL_LOG_PATH = "log_journal"

//        const val CONFIG_MAX_WORKER_COUNT = "max_worker_count"
//        private val CONFIG_MAX_HANDLER_INACTIVE_TIME = "max_handler_inactive_time"
//        private val CONFIG_MAX_SESSION_INACTIVE_TIME = "max_session_inactive_time"
//
//        private val CONFIG_DB_PING_INTERVAL = "db_ping_interval"
//        private val CONFIG_DB_PING_QUERY = "db_ping_query"

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
    private lateinit var logPath: String
    private lateinit var logOptions: String

    private var port = 0

    //--- декодеров/енкодеров может и не быть, если чтение/запись идёт сразу в сыром/бинарном виде
    private var decoderClassName: String? = null
    private var encoderClassName: String? = null
    private lateinit var handlerClassName: String

    //--- параметры SQL-базы
    private lateinit var dbConfig: DBConfig

    private lateinit var dirSessionLog: File
    private lateinit var dirJournalLog: File

//    //--- максимальное кол-во worker'ов
//    private var maxWorkerCount = 0
//
//    //--- максимальное время жизни задания/обработчика в бездействующем состоянии [мин]
//    private var maxHandlerInactiveTime = 0
//
//    //--- максимальное время жизни сессии в бездействующем состоянии [мин]
//    private var maxSessionInactiveTime = 0

//    @Volatile
//    var workerCount = 0

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    private var lastSessionCheckTime = getCurrentTimeInt()
//
//    val chmSessionTime = ConcurrentHashMap<Long, Int>()
//    val chmSessionStore = ConcurrentHashMap<Long, ConcurrentHashMap<String, Any>>()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    private var lastStatisticOutTime = getCurrentTimeInt()
//
//    private var begWorkTime = getCurrentTimeInt()
//
//    @Volatile
//    var workTime = 0
//
//    private var runtime = Runtime.getRuntime()
//    private var lastRestartCheckTime = getCurrentTimeInt()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadConfig() {
        hmConfig = loadConfig(configFileName)

        rootDirName = hmConfig[CONFIG_ROOT_DIR]!!
        tempDirName = hmConfig[CONFIG_TEMP_DIR]!!

        logPath = hmConfig[CONFIG_LOG_PATH]!!
        logOptions = hmConfig[CONFIG_LOG_OPTIONS]!!

        port = hmConfig[CONFIG_PORT]!!.toInt()

        decoderClassName = hmConfig[CONFIG_DECODER_CLASS]
        encoderClassName = hmConfig[CONFIG_ENCODER_CLASS]
        handlerClassName = hmConfig[CONFIG_HANDLER_CLASS]!!

        dbConfig = DBConfig.loadConfig(hmConfig).first()

        dirSessionLog = File(hmConfig[CONFIG_SESSION_LOG_PATH]!!)
        dirJournalLog = File(hmConfig[CONFIG_JOURNAL_LOG_PATH]!!)

//        maxWorkerCount = Integer.parseInt(hmConfig[CONFIG_MAX_WORKER_COUNT])
//        maxHandlerInactiveTime = 60 * hmConfig[CONFIG_MAX_HANDLER_INACTIVE_TIME]!!.toInt()
//        maxSessionInactiveTime = 60 * hmConfig[CONFIG_MAX_SESSION_INACTIVE_TIME]!!.toInt()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun run() {
        lateinit var bossGroup: NioEventLoopGroup
        lateinit var workerGroup: NioEventLoopGroup

        try {
            loadConfig()

            AdvancedLogger.init(logPath, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
            AdvancedLogger.info("==================== NettyServer started ====================")

//            val fileRestartFlag = File("${configFileName}_")

            //!!! попробовать другие варианты, типа EpollEventLoopGroup или других наследников MultithreadEventExecutorGroup
            bossGroup = NioEventLoopGroup()
            workerGroup = NioEventLoopGroup()

            val serverBootstrap = ServerBootstrap()

            serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<SocketChannel>() {
                    override fun initChannel(sc: SocketChannel) {
                        decoderClassName?.let {
                            sc.pipeline().addLast(Class.forName(it).getConstructor().newInstance() as ChannelHandler)
                        }
                        encoderClassName?.let {
                            sc.pipeline().addLast(Class.forName(it).getConstructor().newInstance() as ChannelHandler)
                        }
                        sc.pipeline().addLast(
                            Class.forName(handlerClassName).getConstructor(
                                dbConfig.javaClass,
                                dirSessionLog.javaClass,
                                dirJournalLog.javaClass,
                            ).newInstance(
                                dbConfig,
                                dirSessionLog,
                                dirJournalLog,
                            ) as ChannelHandler
                        )
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channelFuture = serverBootstrap.bind(port).sync()
            channelFuture.channel().closeFuture().sync()

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//            while (true) {
//                //--- периодическая проверка на наличие файла-флага перезагрузки
//                if (getCurrentTimeInt() - lastRestartCheckTime > RESTART_CHECK_PERIOD) {
//                    //--- рестарт по обнаружению рестарт-файла
//                    if (fileRestartFlag.exists()) {
//                        fileRestartFlag.delete()
//                        AdvancedLogger.info("==================== restart by flag-file ====================")
//                        break
//                    }
//                    lastRestartCheckTime = getCurrentTimeInt()
//                }
//
//
//                //--- всяческая статистика
//                if (AdvancedLogger.isInfoEnabled && getCurrentTimeInt() - lastStatisticOutTime > STATISTIC_OUT_PERIOD) {
//                    AdvancedLogger.info("======== Busy Statistic =======")
//                    AdvancedLogger.info(" Workers = $workerCount")
//                    AdvancedLogger.info(" Handlers = ${clqIn.size}")
//                    AdvancedLogger.info(" Busy % = ${100 * workTime / (getCurrentTimeInt() - begWorkTime)}")
//                    AdvancedLogger.info("======= Memory Statistic ======")
//                    AdvancedLogger.info(" Buffer Size = $startBufSize")
//                    AdvancedLogger.info(" Used = ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}")
//                    AdvancedLogger.info(" Free = ${runtime.freeMemory() / 1024 / 1024}")
//                    AdvancedLogger.info(" Total = ${runtime.totalMemory() / 1024 / 1024}")
//                    AdvancedLogger.info(" Max = ${runtime.maxMemory() / 1024 / 1024}")
//                    AdvancedLogger.info("===============================")
//                    lastStatisticOutTime = getCurrentTimeInt()
//                }
//            }
        } catch (t: Throwable) {
            //--- для сложных случаев отладки в cmd-окне
            t.printStackTrace()
            AdvancedLogger.error(t)
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()

            AdvancedLogger.info("==================== DataServer stopped ====================")
            AdvancedLogger.close()
        }
    }

}

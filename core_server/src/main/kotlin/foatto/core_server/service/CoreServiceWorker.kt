package foatto.core_server.service

import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.loadConfig
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import foatto.sql.DBConfig
import java.io.File

abstract class CoreServiceWorker(private val configFileName: String) {

    companion object {

        private val CONFIG_ROOT_DIR = "root_dir"
        private val CONFIG_TEMP_DIR = "temp_dir"
        private val CONFIG_LOG_PATH = "log_path"
        private val CONFIG_LOG_OPTIONS = "log_options"

        lateinit var serviceWorkerName: String

        //--- Статистика

        //--- 1000 секунд = примерно 16-17 мин
        protected val STATISTIC_OUT_PERIOD = 1000
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var hmConfig: Map<String, String>

    //--- путь к веб-серверу (для доступа к общим файлам)
    protected lateinit var rootDirName: String

    //--- путь к временной папке
    protected lateinit var tempDirName: String

    //--- для перекрытия и переопределения в android-версии
    protected lateinit var logPath: String
    protected lateinit var logOptions: String

    //--- параметры баз
    protected lateinit var alDBConfig: List<DBConfig>

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected var alConn = mutableListOf<CoreAdvancedConnection>()
    protected var alStm = mutableListOf<CoreAdvancedStatement>()
    protected var lastStatisticOutTime = getCurrentTimeInt()

    protected var runtime = Runtime.getRuntime()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- вызывать отдельно явным образом блять после конструктора,
    //--- иначе ловить буду удивительнейшие баги недоинициализации,
    //--- когда в конструкторе вызывается метод, перекрываемый в наследниках
    open fun loadConfig() {
        hmConfig = loadConfig(configFileName)

        rootDirName = hmConfig[CONFIG_ROOT_DIR]!!
        tempDirName = hmConfig[CONFIG_TEMP_DIR]!!

        logPath = hmConfig[CONFIG_LOG_PATH]!!
        logOptions = hmConfig[CONFIG_LOG_OPTIONS]!!

        alDBConfig = DBConfig.loadConfig(hmConfig)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun run() {
        //--- загрузка параметров в отдельном блоке,
        //--- т.к. AdvancedLogger к этому времени ещё не готов выводить ошибки
        try {
            loadConfig()
        } catch (t: Throwable) {
            t.printStackTrace()
            return
        }

        try {
            AdvancedLogger.init(logPath, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
            AdvancedLogger.info("==================== $serviceWorkerName started ====================")

            val fileRestartFlag = File("${configFileName}_")

            initDB()

            //--- разовый запуск
            if (isRunOnce) cycle()
            else {
                while (true) {
                    //--- рестарт по обнаружению рестарт-файла
                    if (fileRestartFlag.exists()) {
                        fileRestartFlag.delete()
                        AdvancedLogger.info("==================== $serviceWorkerName restart by flag-file ====================")
                        break
                    }
                    //--- собственно цикл обработки
                    cycle()
                    //--- всяческая статистика
                    if (AdvancedLogger.isInfoEnabled && getCurrentTimeInt() - lastStatisticOutTime > STATISTIC_OUT_PERIOD) {
                        outStat()
                        lastStatisticOutTime = getCurrentTimeInt()
                    }
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            AdvancedLogger.error(t)
            for (conn in alConn) {
                try {
                    conn.rollback()
                } catch (re: Throwable) {
                    re.printStackTrace()
                    AdvancedLogger.error(re)
                }

            }
        } finally {
            for (conn in alConn) {
                try {
                    conn.close()
                } catch (re: Throwable) {
                    re.printStackTrace()
                    AdvancedLogger.error(re)
                }

            }
            AdvancedLogger.info("==================== $serviceWorkerName close ====================")
            AdvancedLogger.close()
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- сделал изначально абстрактными, чтобы не забывать переопределять в наследниках
    protected abstract val isRunOnce: Boolean

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected abstract fun initDB()

    protected abstract fun cycle()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun outStat() {
        AdvancedLogger.info("======= Memory Statistic ======")
        AdvancedLogger.info("Used = ${(runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024}")
        AdvancedLogger.info("Free = ${runtime.freeMemory() / 1024 / 1024}")
        AdvancedLogger.info("Total = ${runtime.totalMemory() / 1024 / 1024}")
        AdvancedLogger.info("Max = ${runtime.maxMemory() / 1024 / 1024}")
        AdvancedLogger.info("===============================")

    }

}

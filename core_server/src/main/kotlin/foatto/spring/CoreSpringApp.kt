package foatto.spring

import foatto.core.link.XyDocumentConfig
import foatto.core.link.XyElementClientType
import foatto.core.link.XyElementConfig
import foatto.core.util.AdvancedLogger
import foatto.core.util.AsyncFileSaver
import foatto.core_server.app.AppParameter
import foatto.core_server.app.xy.server.document.sdcXyAbstract
import foatto.sql.DBConfig
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

//--- добавлять у каждого наследника
//@SpringBootApplication  // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
//@EnableWebMvc
open class CoreSpringApp {
//    companion object {
//        @JvmStatic
//        fun main(args: Array<String>) {
//            //--- новый способ запуска
//            runApplication<SpringXXXApp>(*args)
//        }
//    }

    companion object {
        //--- общие константы
        val MAX_LOGON_ATTEMPT = 3     // кол-во попыток ввода неправильного пароля
        val LOGON_LOCK_TIMEOUT = 15   // таймаут [мин] после N-кратного ввода неправильного пароля
        val PASSWORD_LIFE_TIME = 12   // период смены пароля [мес.]

        //--- выявляемая продолжительность запроса
        val MAX_TIME_PER_REQUEST = 1 * 60

        //--- режимы логгирования действий пользователей
        val SYSTEM_LOG_NONE = 0
        val SYSTEM_LOG_ACTION = 1
        val SYSTEM_LOG_ALL = 2

        //--- параметры, живущие только внутри сессии
        val ALIAS_CONFIG = "alias_config"

        val hsSkipKeyWords = hashSetOf(AppParameter.FORM_DATA, AppParameter.FORM_SELECTOR, AppParameter.REFERER, AppParameter.SELECTOR)

        val zoneId = ZoneId.systemDefault()

        val chmSessionStore = ConcurrentHashMap<Long, ConcurrentHashMap<String, Any>>()

        //--- инициируются снаружи после чтения конфигов
        lateinit var dirUserLog: File
        var userLogMode: Int = SYSTEM_LOG_NONE
        lateinit var dbConfig: DBConfig
        internal val hmAliasLogDir = mutableMapOf<String, String>()
        val hmXyDocumentConfig = mutableMapOf<String, XyDocumentConfig>()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @Value("\${db_name}")
    val dbName: String = ""

    @Value("\${db_url}")
    val dataSourceURL: String = ""

    @Value("\${db_login}")
    val dataSourceUser: String = ""

    @Value("\${db_password}")
    val dataSourcePassword: String = ""

    @Value("\${db_replication_name}")
    val dbReplicationName: String = ""

    @Value("\${db_replication_filter}")
    val dbReplicatiionFilter: String = ""

    @Value("\${db_replication_path}")
    val dbReplicationPath: String = ""

    @Value("\${root_dir}")
    private val rootDirName: String = ""

    @Value("\${log_dir}")
    val logDir: String = ""

    @Value("\${log_options}")
    val logOptions: String = ""

    @Value("\${user_log_path}")
    val userLogPath: String = ""

    @Value("\${user_log_mode}")
    val sUserLogMode: String = ""

    @Value("\${log_show_aliases}")
    private val logShowAliases: Array<String> = emptyArray()

    @Value("\${log_show_dirs}")
    private val logShowDirs: Array<String> = emptyArray()

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- добавлять у каждого наследника
//    @EventListener(ApplicationReadyEvent::class)
    open fun init() {
        AdvancedLogger.init(logDir, logOptions.contains("error"), logOptions.contains("info"), logOptions.contains("debug"))
        AdvancedLogger.info("==================== Spring App started ====================")

        //--- запуск асинхронного записывателя файлов
        AsyncFileSaver.init(rootDirName)

        dirUserLog = File(userLogPath)
        userLogMode = sUserLogMode.toIntOrNull() ?: SYSTEM_LOG_NONE
        dbConfig = DBConfig(dbName, dataSourceURL, dataSourceUser, dataSourcePassword, dbReplicationName, dbReplicatiionFilter, dbReplicationPath)

        for (i in 0 until min(logShowAliases.size, logShowDirs.size)) {
            hmAliasLogDir[logShowAliases[i]] = logShowDirs[i]
        }

        initXyConfig()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun initXyConfig() {
        addXyDocumentConfig()
    }

    //--- должно быть переопределено прикладными серверами, использующими XY-модули
    protected open fun addXyDocumentConfig() {}

    //--- предположительно/пока его не надо перекрывать
    protected fun initXyElementConfig(level: Int, minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {
        val hmElementConfig = mutableMapOf<String, XyElementConfig>()

        hmElementConfig[sdcXyAbstract.BITMAP] = XyElementConfig(
            sdcXyAbstract.BITMAP,
            XyElementClientType.BITMAP,
            //"foatto.app_client.xy.element.XyBitmap", "foatto.app.xy.element.fxBitmap",
            0, minScale, maxScale, "",
            itRotatable = false, itMoveable = true, itEditablePoint = false
        )

        hmElementConfig[sdcXyAbstract.ICON] = XyElementConfig(
            sdcXyAbstract.ICON,
            XyElementClientType.ICON,
            //"foatto.app_client.xy.element.XyIcon", "foatto.app.xy.element.fxIcon",
            level, minScale, maxScale, "",
            itRotatable = true, itMoveable = true, itEditablePoint = false
        )

        hmElementConfig[sdcXyAbstract.MARKER] = XyElementConfig(
            sdcXyAbstract.MARKER,
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            level, minScale, maxScale, "",
            itRotatable = true, itMoveable = true, itEditablePoint = false
        )

        hmElementConfig[sdcXyAbstract.POLY] = XyElementConfig(
            sdcXyAbstract.POLY,
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            level, minScale, maxScale, "",
            itRotatable = false, itMoveable = true, itEditablePoint = true
        )

        hmElementConfig[sdcXyAbstract.TEXT] = XyElementConfig(
            sdcXyAbstract.TEXT,
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            level, minScale, maxScale, "",
            itRotatable = true, itMoveable = true, itEditablePoint = false
        )

        hmElementConfig[sdcXyAbstract.TRACE] = XyElementConfig(
            sdcXyAbstract.TRACE,
            XyElementClientType.TRACE,
            //"foatto.app_client.xy.element.XyTrace", "foatto.app.xy.element.fxTrace",
            level, minScale, maxScale, "",
            itRotatable = false, itMoveable = true, itEditablePoint = true
        )

        return hmElementConfig
    }

}

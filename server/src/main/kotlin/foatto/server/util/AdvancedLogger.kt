package foatto.server.util

import foatto.core.util.getCurrentTimeInt
import foatto.core.util.getDateTimeYMDHMSString
import kotlinx.datetime.TimeZone
import java.io.BufferedWriter
import java.io.File
import java.util.concurrent.LinkedBlockingQueue

//--- фоновый процесс записи логов ----------------------------------------------------------------------------------------------------------------------------------------------------

class AdvancedLogger : Thread() {

    companion object {
        //--- конфигурация логгера ----------------------------------------------------------------------------------------------------------------------------------------------------

        private lateinit var logPath: File

        var isErrorEnabled = true
            private set
        var isInfoEnabled = true
            private set
        var isDebugEnabled = true
            private set

        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private val lbqLog = LinkedBlockingQueue<LoggerData>()

        @Volatile
        private var inWork = true
        private val timeZone = TimeZone.currentSystemDefault()

        private class LoggerData(val subDir: String = "", val prefix: String, val message: String) {
            val time = getCurrentTimeInt()
        }

        //--- внешнее статическое API -------------------------------------------------------------------------------------------------------------------------------------------------

        fun init(aLogPath: String, aErrorEnabled: Boolean, aInfoEnabled: Boolean, aDebugEnabled: Boolean) {
            logPath = File(aLogPath)

            isErrorEnabled = aErrorEnabled
            isInfoEnabled = aInfoEnabled
            isDebugEnabled = aDebugEnabled

            logPath.mkdirs()
            AdvancedLogger().start()
        }

        fun error(t: Throwable, subDir: String = "") {
            if (isErrorEnabled) {
                log(subDir, "[ERROR]", t)
            }
        }

        fun info(t: Throwable, subDir: String = "") {
            if (isInfoEnabled) {
                log(subDir, "[INFO]", t)
            }
        }

        fun debug(t: Throwable, subDir: String = "") {
            if (isDebugEnabled) {
                log(subDir, "[DEBUG]", t)
            }
        }

        fun error(message: String, subDir: String = "") {
            if (isErrorEnabled) {
                log(subDir, "[ERROR]", message)
            }
        }

        fun info(message: String, subDir: String = "") {
            if (isInfoEnabled) {
                log(subDir, "[INFO]", message)
            }
        }

        fun debug(message: String, subDir: String = "") {
            if (isDebugEnabled) {
                log(subDir, "[DEBUG]", message)
            }
        }

        fun close(isWait: Boolean = true) {
            while (isWait && lbqLog.isNotEmpty()) {
                sleep(1000L)
            }
            inWork = false
        }

        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private fun log(subDir: String, prefix: String, t: Throwable) {
            lbqLog.put(LoggerData(subDir, prefix, t.stackTraceToString()))
        }

        private fun log(subDir: String, prefix: String, message: String) {
            lbqLog.put(LoggerData(subDir, prefix, message))
        }
    }

    private var hmBufferedWriter = mutableMapOf<String, BufferedWriter>()
    private var hmCurrentName = mutableMapOf<String, String>()

    override fun run() {
        while (inWork) {
            try {
                val logData = lbqLog.take()

                val subDir = logData.subDir
                val logTime = getDateTimeYMDHMSString(timeZone, logData.time)
//                val logTime = DateTime_YMDHMS(zoneId, logData.time)
                //--- какое д.б. имя лог-файла для текущего дня и часа
                val logFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

                //--- открытие (другого) файла для записи при необходимости
                var out = hmBufferedWriter[subDir]
                val curFileName = hmCurrentName[subDir]
                if (out == null || curFileName != logFileName) {
                    hmCurrentName[subDir] = logFileName

                    out?.close()
                    val logFile =
                        if (subDir != "") {
                            val logDir = File(logPath, subDir)
                            logDir.mkdirs()

                            File(logDir, "$logFileName.log")
                        } else {
                            File(logPath, "$logFileName.log")
                        }

                    out = getFileWriter(logFile, true)
                    hmBufferedWriter[subDir] = out
                }

                //--- собственно вывод
                out.apply {
                    write("$logTime ${logData.prefix} ${logData.message}")
                    newLine()
                    flush()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                //--- обнулим список лог-файлов, для последующего переоткрытия
                hmBufferedWriter.clear()
                hmCurrentName.clear()
                //--- небольшая пауза, чтобы не загрузить систему в случае циклической ошибки
                try {
                    sleep(100_000)
                } catch (t2: Throwable) {
                }
            }
        }
        hmBufferedWriter.forEach { (_, out) ->
            try {
                out.close()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}


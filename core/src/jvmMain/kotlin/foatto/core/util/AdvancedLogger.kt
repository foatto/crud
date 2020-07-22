package foatto.core.util

import java.io.BufferedWriter
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZoneId
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
        private val zoneId = ZoneId.systemDefault()

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
            if(isErrorEnabled) log(subDir, "[ERROR]", t)
        }

        fun info(t: Throwable, subDir: String = "") {
            if(isInfoEnabled) log(subDir, "[INFO]", t)
        }

        fun debug(t: Throwable, subDir: String = "") {
            if(isDebugEnabled) log(subDir, "[DEBUG]", t)
        }

        fun error(message: String, subDir: String = "") {
            if(isErrorEnabled) log(subDir, "[ERROR]", message)
        }

        fun info(message: String, subDir: String = "") {
            if(isInfoEnabled) log(subDir, "[INFO]", message)
        }

        fun debug(message: String, subDir: String = "") {
            if(isDebugEnabled) log(subDir, "[DEBUG]", message)
        }

        fun close() {
            inWork = false
        }

        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private fun log(subDir: String, prefix: String, t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            lbqLog.put(LoggerData(subDir, prefix, sw.toString()))
        }

        private fun log(subDir: String, prefix: String, message: String) {
            lbqLog.put(LoggerData(subDir, prefix, message))
        }
    }

    private var hmBufferedWriter = mutableMapOf<String, BufferedWriter>()
    private var hmCurrentName = mutableMapOf<String, String>()

    override fun run() {
        while(inWork) {
            try {
                val logData = lbqLog.take()

                val subDir = logData.subDir
                val logTime = DateTime_YMDHMS(zoneId, logData.time)
                //--- какое д.б. имя лог-файла для текущего дня и часа
                val logFileName = logTime.substring(0, 13).replace('.', '-').replace(' ', '-')

                //--- открытие (другого) файла для записи при необходимости
                var out = hmBufferedWriter[subDir]
                val curFileName = hmCurrentName[subDir]
                if(out == null || curFileName != logFileName) {
                    hmCurrentName[subDir] = logFileName

                    out?.close()
                    val logFile =
                        if(subDir != "") {
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
            } catch(t: Throwable) {
                t.printStackTrace()
                //--- обнулим список лог-файлов, для последующего переоткрытия
                hmBufferedWriter.clear()
                hmCurrentName.clear()
                //--- небольшая пауза, чтобы не загрузить систему в случае циклической ошибки
                try {
                    sleep(100_000)
                } catch(t2: Throwable) {
                }
            }
        }
        hmBufferedWriter.forEach { (_, out) ->
            try {
                out.close()
            } catch(t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}

//import java.io.BufferedWriter
//import java.io.File
//import java.io.PrintWriter
//import java.io.StringWriter
//import java.time.ZoneId
//import java.util.concurrent.ConcurrentLinkedQueue
//
////--- фоновый процесс записи логов ----------------------------------------------------------------------------------------------------------------------------------------------------
//
//class AdvancedLogger : Thread() {
//
//    companion object {
//        //--- конфигурация логгера ----------------------------------------------------------------------------------------------------------------------------------------------------
//
//        private lateinit var logPath: File
//        private var encoding: String = ""
//
//        var isErrorEnabled = true
//            private set
//        var isInfoEnabled = true
//            private set
//        var isDebugEnabled = true
//            private set
//
//        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//        private val clqLog = ConcurrentLinkedQueue<LoggerData>()
//        //!!! у котлиновского Any нет функций wait/notify, заменить в будущем на более идиоматичный вариант
//        private val lock = java.lang.Object()
//        @Volatile private var inWork = true
//        private val zoneId = ZoneId.systemDefault()
//
//        private class LoggerData( val time: Int, val prefix: CharSequence, val message: CharSequence )
//
//        //--- внешнее статическое API -------------------------------------------------------------------------------------------------------------------------------------------------
//
//        fun init( aLogPath: String, aEncoding: String, aErrorEnabled: Boolean, aInfoEnabled: Boolean, aDebugEnabled: Boolean ) {
//            logPath = File( aLogPath )
//
//            encoding = aEncoding
//
//            isErrorEnabled = aErrorEnabled
//            isInfoEnabled = aInfoEnabled
//            isDebugEnabled = aDebugEnabled
//
//            AdvancedLogger().start()
//        }
//
//        fun error( t: Throwable ) { if( isErrorEnabled ) log( "[ERROR]", t ) }
//        fun info( t: Throwable ) { if( isInfoEnabled ) log( "[INFO]", t ) }
//        fun debug( t: Throwable ) { if( isDebugEnabled ) log( "[DEBUG]", t ) }
//
//        fun error( message: CharSequence ) { if( isErrorEnabled ) log( "[ERROR]", message ) }
//        fun info( message: CharSequence ) { if( isInfoEnabled ) log( "[INFO]", message ) }
//        fun debug( message: CharSequence ) { if( isDebugEnabled ) log( "[DEBUG]", message ) }
//
//        //--- специально для записи клиентских (android) логов
//        fun client( time: Int, message: CharSequence ) { log( time, "[CLIENT]", message ) }
//
//        fun close() { inWork = false }
//
//        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//        private fun log( prefix: CharSequence, t: Throwable ) {
//            val sw = StringWriter()
//            t.printStackTrace( PrintWriter( sw ) )
//            log( prefix, sw.toString() )
//        }
//
//        private fun log( prefix: CharSequence, message: CharSequence ) {
//            log( getCurrentTimeInt(), prefix, message )
//        }
//
//        private fun log( time: Int, prefix: CharSequence, message: CharSequence ) {
//            clqLog.offer( LoggerData( time, prefix, message ) )
//            synchronized( lock ) {
//                //--- notifyAll() не нужен - на одно событие одного обработчика достаточно разбудить один процесс-worker
//                lock.notify()
//            }
//        }
//    }
//
//    private var out: BufferedWriter? = null
//    private var currentName: String = ""
//
//    override fun run() {
//        var sleepTime = 0
//
//        while( inWork ) {
//            try {
//                //--- забираем, но пока не удаляем лог из очереди
//                val loggerData = clqLog.peek()
//                if( loggerData == null ) synchronized( lock ) {
//                    try {
//                        lock.wait( (++sleepTime) * 1000L )
//                    }
//                    catch( e: InterruptedException ) {}
//                }
//                else {
//                    sleepTime = 0
//                    //--- собственно обработка
//                    val logTime = DateTime_YMDHMS( zoneId, loggerData.time )
//                    //--- какое д.б. имя лог-файла для текущего дня и часа
//                    val curLogName = logTime.substring( 0, 13 ).replace( '.', '-' ).replace( ' ', '-' )
//
//                    //--- открытие (другого) файла для записи при необходимости
//                    if( out == null || curLogName != currentName ) {
//                        currentName = curLogName
//                        out?.close()
//                        out = getFileWriter( File( logPath, StringBuilder( currentName ).append( ".log" ).toString() ), encoding, true )
//                    }
//
//                    //--- собственно вывод
//                    out?.write( "$logTime ${loggerData.prefix} ${loggerData.message}" )
//                    out?.newLine()
//                    out?.flush()
//
//                    //--- после успешного сохранения можно удалить лог из очереди
//                    clqLog.poll()
//                }
//            }
//            catch( t: Throwable ) {
//                t.printStackTrace()
//                //--- на всякий случай закроем лог-файл, для последующего переоткрытия
//                out = null
//                //--- небольшая пауза, чтобы не загрузить систему в случае циклической ошибки
//                try {
//                    sleep( 100_000 )
//                }
//                catch( t2: Throwable ) {}
//            }
//        }
//        try {
//            out?.close()
//        }
//        catch( t: Throwable ) {
//            t.printStackTrace()
//        }
//    }
//}

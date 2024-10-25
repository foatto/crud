package foatto.server.util

import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue

//--- фоновый процесс сохранения файлов -----------------------------------------------------------------------------------------------------------------------------------------------

class AsyncFileSaver : Thread() {

    companion object {
        //--- конфигурация сохраняльщика ----------------------------------------------------------------------------------------------------------------------------------------------

        private lateinit var rootDir: File

        //-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        private val clqFileData = ConcurrentLinkedQueue<FileData>()

        //!!! у котлиновского Any нет функций wait/notify, заменить в будущем на более идиоматичный вариант
        private val lock = Object()
        @Volatile
        private var inWork = true

        private class FileData(val fileName: CharSequence, val bbFileData: AdvancedByteBuffer)

        //--- внешнее статическое API -------------------------------------------------------------------------------------------------------------------------------------------------

        fun init(rootDirName: String) {
            rootDir = File(rootDirName)
            AsyncFileSaver().start()
        }

        fun put(fileName: CharSequence, bbFileData: AdvancedByteBuffer) {
            clqFileData.offer(FileData(fileName, bbFileData))
            synchronized(lock) {
                //--- notifyAll() не нужен - на одно событие одного обработчика достаточно разбудить один процесс-worker
                lock.notify()
            }
        }

        fun close() {
            inWork = false
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun run() {
        var sleepTime = 0

        while (inWork) {
            try {
                val fileData = clqFileData.poll()
                if (fileData == null)
                    synchronized(lock) {
                        try {
                            lock.wait((++sleepTime) * 1000L)
                        } catch (e: InterruptedException) {
                        }
                    }
                else {
                    sleepTime = 0

                    val file = File(rootDir, fileData.fileName.toString())
                    val fileChannel = FileOutputStream(file).channel
                    fileChannel.write(fileData.bbFileData.buffer)
                    fileChannel.close()
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                //--- небольшая пауза, чтобы не загрузить систему в случае циклической ошибки
                try {
                    sleep(100_000)
                } catch (t2: Throwable) {
                }
            }
        }
    }
}

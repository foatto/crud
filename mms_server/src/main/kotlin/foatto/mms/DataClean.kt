package foatto.mms

import foatto.mms.core_mms.CoreDataClean
import foatto.sql.AdvancedConnection
import kotlin.system.exitProcess

class DataClean(aConfigFileName: String) : CoreDataClean(aConfigFileName) {

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "DataClean"
                if (args.size == 1) {
                    DataClean(args[0]).run()
                    exitCode = 1
                } else {
                    println("Usage: $serviceWorkerName <ini-file-name>")
                }
            }
            catch(t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }
}

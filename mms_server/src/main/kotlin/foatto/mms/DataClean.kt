package foatto.mms

import foatto.core_server.service.CoreServiceWorker
import foatto.mms.core_mms.CoreDataClean
import foatto.sql.AdvancedConnection

class DataClean(aConfigFileName: String) : CoreDataClean(aConfigFileName) {

    override fun initDB() {
        for(i in alDBConfig.indices) {
            alConn.add(AdvancedConnection(alDBConfig[i]))
            alStm.add(alConn[i].createStatement())
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                CoreServiceWorker.serviceWorkerName = "DataClean"
                if(args.size == 1) {
                    DataClean(args[0]).run()
                    exitCode = 1
                }
                else println("Usage: ${CoreServiceWorker.serviceWorkerName} <ini-file-name>")
            }
            catch(t: Throwable) {
                t.printStackTrace()
            }

            System.exit(exitCode)
        }
    }
}

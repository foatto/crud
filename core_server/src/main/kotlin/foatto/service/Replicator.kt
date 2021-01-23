package foatto.service

import foatto.core_server.service.CoreReplicator
import foatto.sql.AdvancedConnection
import kotlin.system.exitProcess

class Replicator(aConfigFileName: String) : CoreReplicator(aConfigFileName) {

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "Replicator"
                if (args.size == 1) {
                    Replicator(args[0]).run()
                    exitCode = 1
                } else println("Usage: $serviceWorkerName <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

}

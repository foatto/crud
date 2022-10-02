package foatto.ds

import foatto.core_server.ds.netty.CoreNettyServer
import kotlin.system.exitProcess

class NettyServer(aConfigFileName: String) : CoreNettyServer(aConfigFileName) {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0
            try {
                if (args.size == 1) {
                    NettyServer(args[0]).run()
                    //--- выход для перезапуска
                    exitCode = 1
                }
                //--- обычный выход с прерыванием цикла запусков
                else println("Usage: NettyServer <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            exitProcess(exitCode)
        }
    }

}

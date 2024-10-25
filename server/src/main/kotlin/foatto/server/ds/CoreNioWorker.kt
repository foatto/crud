package foatto.server.ds

import foatto.core.util.getCurrentTimeInt
import foatto.server.util.AdvancedLogger
import foatto.server.sql.CoreAdvancedConnection

abstract class CoreNioWorker protected constructor(val dataServer: CoreNioServer) : Thread() {

    lateinit var conn: CoreAdvancedConnection

    init {
        openDB()
        //--- нельзя увеличивать счётчик до открытия базы - т.к. открытие базы может обломиться (SQLite),
        //--- а счётчик уже увеличен
        dataServer.workerCount++
        AdvancedLogger.info("--- DataWorker started = ${dataServer.workerCount}")
    }

    override fun run() {
        try {
            var sleepTime = 0
            var lastSQLCheck = getCurrentTimeInt()

            while (true) {
                //--- проверка SQL-соединения на живость
                lastSQLCheck = checkDB(lastSQLCheck)

                val handler = dataServer.getHandler()

                if (handler == null)
                    synchronized(dataServer.lock) {
                        try {
                            dataServer.lock.wait((++sleepTime) * 1000L)
                        } catch (e: InterruptedException) {
                        }
                    }
                else {
                    sleepTime = 0
                    //--- собственно обработка
                    try {
                        var isOk = true

                        val begTime = getCurrentTimeInt()
                        while (isOk && !handler.clqIn.isEmpty()) {
                            isOk = handler.work(this)
                        }
                        dataServer.workTime += getCurrentTimeInt() - begTime

                        if (isOk) {
                            dataServer.putHandler(handler)
                        } else {
                            dataServer.putForClose(handler)
                        }
                    } catch (t: Throwable) {
                        AdvancedLogger.error(t)
                        dataServer.putForClose(handler)
                        //--- выходим из цикла обработки
                        break
                    }
                }
            }
        } catch (t: Throwable) {
            AdvancedLogger.error(t)
        }

        //--- закрываем соединения
        try {
            closeDB()
        } catch (t: Throwable) {
            AdvancedLogger.error(t)
        }

        dataServer.workerCount--
        AdvancedLogger.info("--- DataWorker stopped = ${dataServer.workerCount}")
    }

    //--- проверка живости соединения с базой -
    //--- особенно актуально для работы с сетевыми базами данных
    private fun checkDB(aLastSQLCheck: Int): Int {
        var lastSQLCheck = aLastSQLCheck
        if (getCurrentTimeInt() - lastSQLCheck > dataServer.dbPingInterval) {
            try {
                val rs = conn.executeQuery(dataServer.dbPingQuery)
                rs.next()
                rs.close()
            } catch (t: Throwable) {
                AdvancedLogger.error(t)
                //--- переоткрыть соединение к базе, при ошибке - закрываем обработчик, т.к. база недоступна
                closeDB()
                openDB()
            }
            lastSQLCheck = getCurrentTimeInt()
        }
        return lastSQLCheck
    }

    private fun openDB() {
        conn = openConnection()
    }

    //--- открытие коннектов к базе - имеет свои особенности на разных платформах
    protected abstract fun openConnection(): CoreAdvancedConnection

    //--- закрытие баз - своих особенностей на разных платформах не обнаружилось
    private fun closeDB() {
        try {
            conn.close()
        } catch (re: Throwable) {
            AdvancedLogger.error(re)
        }
    }
}

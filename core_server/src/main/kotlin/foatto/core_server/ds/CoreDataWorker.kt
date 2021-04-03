package foatto.core_server.ds

import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.util.*

abstract class CoreDataWorker protected constructor(val dataServer: CoreDataServer) : Thread() {

    val alConn = ArrayList<CoreAdvancedConnection>()
    val alStm = ArrayList<CoreAdvancedStatement>()

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
                        while (isOk && !handler.clqIn.isEmpty()) isOk = handler.work(this)
                        dataServer.workTime += getCurrentTimeInt() - begTime

                        if (isOk) dataServer.putHandler(handler)
                        else dataServer.putForClose(handler)
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
                val rs = alStm[0].executeQuery(dataServer.dbPingQuery)
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
        //--- на случай переоткрытия баз - надёжнее именно здесь
        alConn.clear()
        alStm.clear()

        for (i in dataServer.alDBConfig.indices) {
            val conn = openConnection(i)

            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

    //--- открытие коннектов к базе - имеет свои особенности на разных платформах
    protected abstract fun openConnection(dbIndex: Int): CoreAdvancedConnection

    //--- закрытие баз - своих особенностей на разных платформах не обнаружилось
    private fun closeDB() {
        for (stm in alStm)
            try {
                stm.close()
            } catch (re: Throwable) {
                AdvancedLogger.error(re)
            }

        for (conn in alConn)
            try {
                conn.close()
            } catch (re: Throwable) {
                AdvancedLogger.error(re)
            }
    }
}

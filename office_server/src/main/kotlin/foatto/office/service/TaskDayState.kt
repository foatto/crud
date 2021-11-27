package foatto.office.service

import foatto.core.util.getDateTimeArray
import foatto.core_server.app.server.UserConfig
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.AdvancedConnection
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.system.exitProcess

class TaskDayState(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        val toDay = ZonedDateTime.now()

        //----------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0 // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "TaskDayState"
                if (args.size == 1) {
                    TaskDayState(args[0]).run()
                    exitCode = 1
                } else {
                    println("Usage: $serviceWorkerName <ini-file-name>")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            exitProcess(exitCode)
        }
    }

    //----------------------------------------------------------------------------------------------------------------------

    override val isRunOnce = true

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

    override fun cycle() {
        //--- загрузка двух отдельных конфигурации пользователей,
        //--- на всякий случай, чтобы итераторы во вложенных циклах не перемешались
        val hmUserConfigOut = UserConfig.getConfig(alConn[0])
        val hmUserConfigIn = UserConfig.getConfig(alConn[0])
        val arrToday = getDateTimeArray(toDay)

        hmUserConfigOut.keys.forEach { outUserID ->
            hmUserConfigIn.keys.forEach { inUserID ->
                val taskState = getTaskState(outUserID, inUserID)
                if (taskState.first > 0) {
                    //--- запишем (обновим или добавим) статистику за сегодня
                    val sqlUpdate =
                        """ 
                            UPDATE OFFICE_task_day_state SET 
                            count_red = ${taskState.second} , 
                            count_all = ${taskState.first}
                            WHERE out_user_id = $outUserID
                            AND in_user_id = $inUserID
                            AND ye = ${arrToday[0]}
                            AND mo = ${arrToday[1]}
                            AND da = ${arrToday[2]}
                        """
                    //--- попробуем обновить статистику
                    if (alStm[0].executeUpdate(sqlUpdate) == 0) //--- если не обновилось - добавляем запись
                        alStm[0].executeUpdate(
                            """
                                INSERT INTO OFFICE_task_day_state ( id , out_user_id , in_user_id , ye , mo , da , count_red , count_all ) VALUES ( 
                                ${alStm[0].getNextIntId("OFFICE_task_day_state", "id")} , 
                                $outUserID , 
                                $inUserID , 
                                ${arrToday[0]} ,
                                ${arrToday[1]} ,
                                ${arrToday[2]} ,
                                ${taskState.second} ,
                                ${taskState.first}
                                )
                            """
                        )
                }
            }
        }
        alConn[0].commit()
    }

    fun getTaskState(outUserID: Int, inUserID: Int): Pair<Int, Int> {
        var countAll = 0
        var countRed = 0
        //--- загрузим список поручений по данной паре автор/исполнитель
        val rs = alStm[0].executeQuery(
            """
                SELECT ye , mo , da
                FROM OFFICE_task 
                WHERE out_user_id = $outUserID 
                AND in_user_id = $inUserID 
                AND in_active = 1 
                AND in_archive = 0 
            """
        )
        while (rs.next()) {
            val date = ZonedDateTime.of(rs.getInt(1), rs.getInt(2), rs.getInt(3), 0, 0, 0, 0, ZoneId.systemDefault())
            if (date.isBefore(toDay)) {
                countRed++
            }
            countAll++
        }
        rs.close()

        return Pair(countAll, countRed)
    }

}

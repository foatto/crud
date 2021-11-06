package foatto.office.service

import foatto.core_server.app.server.UserConfig
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import java.util.*
import kotlin.system.exitProcess

class Tasker(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {
        private const val CONFIG_BOSS_USER_ID = "boss_user_id"
        private const val CONFIG_RECEPTION_USER_ID = "reception_user_id"
        private const val CONFIG_ALLOWED_TASK_DELAY = "allowed_task_delay"

        private val toDay = GregorianCalendar()

        //----------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0 // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "Tasker"
                if (args.size == 1) {
                    Tasker(args[0]).run()
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

    private var bossUserID = 0
    private var receptionUserID = 0
    private var allowedTaskDelay = 0

    //----------------------------------------------------------------------------------------------------------------------

    override val isRunOnce = true

    override fun loadConfig() {
        super.loadConfig()

        bossUserID = hmConfig[CONFIG_BOSS_USER_ID]!!.toInt()
        receptionUserID = hmConfig[CONFIG_RECEPTION_USER_ID]!!.toInt()
        allowedTaskDelay = hmConfig[CONFIG_ALLOWED_TASK_DELAY]!!.toInt()
    }

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

    override fun cycle() {
        val hmUserConfig = UserConfig.getConfig(alConn[0])

        for (userID in hmUserConfig.keys) {
            //--- босс сам себя наказывать не будет (наверное)
            if (userID == bossUserID) {
                continue
            }
            //--- приёмную наказывать тоже не за что - она инструмент наказания
            if (userID == receptionUserID) {
                continue
            }

            //--- ищем самое старое просроченное поручение с последним ответом от босса

            //--- загрузим список просроченных поручений
            val alTaskID = mutableListOf<Int>()
            val alTaskSubj = mutableListOf<String>()
            var rs: CoreAdvancedResultSet = alStm[0].executeQuery(
                """
                    SELECT ye , mo , da , id , subj 
                    FROM OFFICE_task 
                    WHERE out_user_id = $bossUserID 
                    AND in_user_id = $userID
                    AND in_active = 1 
                    AND in_archive = 0 
                    ORDER BY ye , mo , da 
                """
            )
            while (rs.next()) {
                val gc = GregorianCalendar(rs.getInt(1), rs.getInt(2) - 1, rs.getInt(3))
                //--- наказания за не очень просроченное поручение не будет
                gc.add(GregorianCalendar.DAY_OF_MONTH, allowedTaskDelay)
                //--- уже пошли поздние даты, можно выходить
                if (gc.after(toDay)) {
                    break
                }
                alTaskID.add(rs.getInt(4))
                alTaskSubj.add(rs.getString(5))
            }
            rs.close()

            //--- среди просроченных ищем поручение с последним сообщением от босса
            //--- (т.е. поручение висит в просроченных не по вине босса)
            for (taskIndex in alTaskID.indices) {
                val taskID = alTaskID[taskIndex]
                //--- если переписка пуста, значит подчинённый должен был ответить
                var messageUserID = bossUserID
                rs = alStm[0].executeQuery(
                    """
                        SELECT user_id 
                        FROM OFFICE_task_thread 
                        WHERE task_id = $taskID
                        ORDER BY ye DESC , mo DESC , da DESC , ho DESC , mi DESC
                    """
                )
                if (rs.next()) {
                    messageUserID = rs.getInt(1)
                }
                rs.close()
                //--- если последнее сообщение было от босса,
                //--- то сейчас последует наказание
                if (messageUserID == bossUserID) {
                    //--- перенести поручение на приёмную, вставить в тему поручения "! Пригласить" и фамилию ответственного
                    val sqlTask =
                        """
                            UPDATE OFFICE_task SET 
                            in_user_id = $receptionUserID ,
                            ye = ${toDay[GregorianCalendar.YEAR]} ,
                            mo = ${toDay[GregorianCalendar.MONTH] + 1} ,
                            da = ${toDay[GregorianCalendar.DAY_OF_MONTH]} ,
                            subj = '! Пригласить: ${UserConfig.hmUserFullNames[userID]}\n по поручению: ${alTaskSubj[taskIndex]}'
                            WHERE id = $taskID
                        """
                    //--- в переписку добавить "!" и userID ответственного для последующих отчетов
                    val rowID = alStm[0].getNextIntId("OFFICE_task_thread", "id")
                    val sqlTaskThread =
                        """
                            INSERT INTO OFFICE_task_thread ( id , user_id , task_id , ye , mo , da , ho , mi , message ) VALUES ( 
                            $rowID , $bossUserID , $taskID , 
                            ${toDay[GregorianCalendar.YEAR]} , 
                            ${toDay[GregorianCalendar.MONTH] + 1} , 
                            ${toDay[GregorianCalendar.DAY_OF_MONTH]} , 0 , 0 , '!$userID' ) 
                        """
                    alStm[0].executeUpdate(sqlTask)
                    alStm[0].executeUpdate(sqlTaskThread)

                    //--- одного наказания за один раз достаточно
                    break
                }
            }
        }
        alConn[0].commit()
    }

}

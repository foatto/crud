package foatto.office.service

import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_DMYHMS
import foatto.core.util.getDateTimeArray
import foatto.core.util.prepareForSQL
import foatto.core_server.app.server.UserConfig
import foatto.core_server.service.CoreServiceWorker
import foatto.office.mTask
import foatto.office.mTaskThread
import foatto.sql.AdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.system.exitProcess

class Alerter(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {
        private const val CONFIG_SMTP_SERVER = "smtp_server"
        private const val CONFIG_SMTP_PORT = "smtp_port"
        private const val CONFIG_SMTP_LOGIN = "smtp_login"
        private const val CONFIG_SMTP_PASSWORD = "smtp_password"
        private const val CONFIG_SMTP_OPTION_NAME_ = "smtp_option_name_"
        private const val CONFIG_SMTP_OPTION_VALUE_ = "smtp_option_value_"

        private const val CONFIG_POP3_SERVER = "pop3_server"
        private const val CONFIG_POP3_LOGIN = "pop3_login"
        private const val CONFIG_POP3_PASSWORD = "pop3_password"
        private const val CONFIG_POP3_OPTION_NAME_ = "pop3_option_name_"
        private const val CONFIG_POP3_OPTION_VALUE_ = "pop3_option_value_"

        private const val CONFIG_CYCLE_PAUSE = "cycle_pause"

        private const val ACTION_EMPTY = 0
        private const val ACTION_DELETE = 1
        private const val ACTION_CHANGE_USER = 2
        private const val ACTION_TIME_SHIFT = 3
        private const val ACTION_TIME_SET = 4

        //----------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0 // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "Alerter"
                if (args.size == 1) {
                    Alerter(args[0]).run()
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

    private lateinit var smtpServer: String
    private var smtpPort = 0
    private lateinit var smtpLogin: String
    private lateinit var smtpPassword: String
    private val hmSmtpOption = mutableMapOf<String, String>()

    private lateinit var pop3Server: String
    private lateinit var pop3Login: String
    private lateinit var pop3Password: String
    private val hmPop3Option = mutableMapOf<String, String>()

    private var cyclePause: Long = 0L

    private lateinit var hmUserConfig: Map<Int, UserConfig>

//----------------------------------------------------------------------------------------------------------------------

    override val isRunOnce = false

    override fun loadConfig() {
        super.loadConfig()

        smtpServer = hmConfig[CONFIG_SMTP_SERVER]!!
        smtpPort = hmConfig[CONFIG_SMTP_PORT]!!.toInt()
        smtpLogin = hmConfig[CONFIG_SMTP_LOGIN]!!
        smtpPassword = hmConfig[CONFIG_SMTP_PASSWORD]!!

        var index = 0
        while (true) {
            val optionName = hmConfig[CONFIG_SMTP_OPTION_NAME_ + index] ?: break

            hmSmtpOption[optionName] = hmConfig[CONFIG_SMTP_OPTION_VALUE_ + index]!!

            index++
        }

        pop3Server = hmConfig[CONFIG_POP3_SERVER]!!
        pop3Login = hmConfig[CONFIG_POP3_LOGIN]!!
        pop3Password = hmConfig[CONFIG_POP3_PASSWORD]!!

        index = 0
        while (true) {
            val optionName = hmConfig[CONFIG_POP3_OPTION_NAME_ + index] ?: break

            hmPop3Option[optionName] = hmConfig[CONFIG_POP3_OPTION_VALUE_ + index]!!

            index++
        }

        cyclePause = hmConfig[CONFIG_CYCLE_PAUSE]!!.toLong() * 1000L
    }

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
            alStm.add(conn.createStatement())
        }
    }

    override fun cycle() {

        //--- между циклами/проходами могут появиться новые данные по адресам электронной почты,
        //--- лучше их перезагружать каждый раз, не дожидаясь очередной перезагрузки
        /*if( hmUserConfig == null )*/
        hmUserConfig = UserConfig.getConfig(alConn[0])

        //--- загрузка оповещений на отправку
        val rs = alStm[0].executeQuery(
            """
                SELECT id , tag , row_id 
                FROM SYSTEM_alert
                WHERE alert_time <= ${System.currentTimeMillis() / 1000}
                ORDER BY alert_time 
            """
        )
        if (rs.next()) {
            val alertID = rs.getInt(1)
            val tag = rs.getString(2)
            val rowID = rs.getInt(3)
            rs.close()

            when (tag) {
                mTask.ALERT_TAG -> {
                    sendTask(rowID)
                    AdvancedLogger.info("New Task Sended.")
                }
                mTaskThread.ALERT_TAG -> {
                    sendTaskThread(rowID)
                    AdvancedLogger.info("New TaskThread Sended.")
                }
                "reminder" /*mReminder.ALERT_TAG*/ -> {
                    //sendReminder(rowID)
                    //AdvancedLogger.info("Reminder Sended.")
                    AdvancedLogger.error("Reminder not supported yet.")
                }
                else -> {
                    AdvancedLogger.error("Unknown tag = $tag")
                }
            }
            alStm[0].executeUpdate(" DELETE FROM SYSTEM_alert WHERE id = $alertID")
            alConn[0].commit()
        } else {
            rs.close()

            AdvancedLogger.info("No outgoing alerts. Incoming mail checking...")
            receiveMail()?.let { receivedMailData ->
                //--- парсим тему письма, ищем TAG и ROW_ID
//                "Office" ).append( '#' ).append( mTaskThread.ALERT_TAG )
//                                 .append( '#' ).append( taskID ).append( '#' ).append( arrUserID[ i ] ).append( '#' )
                val tokens = receivedMailData.subject.split('#').filter(String::isNotBlank)
                try {
                    tokens[1] // пропускаем начальные "Re:" и/или "Office"
                } catch (t: Throwable) {
                    AdvancedLogger.error("Not found 'tokens[1]' for subj = '${receivedMailData.subject}'.")
                    null
                }?.let { tag ->
                    //--- обрабатываем подходящий тег
                    //--- ответ на создание задачи ложится в виде сообщения в переписку
                    if (tag == mTask.ALERT_TAG || tag == mTaskThread.ALERT_TAG) {
                        try {
                            val (taskID, userID) = Pair(tokens[2].toInt(), tokens[3].toInt())
                            //--- если получены параметры поручения
                            if (taskID != 0 && userID != 0) {
                                receiveTaskThread(taskID, userID, receivedMailData.sentDate, receivedMailData.content)
                                AdvancedLogger.info("New TaskThread Received.")
                            }
                        } catch (t: Throwable) {
                            AdvancedLogger.error("Not found correct '(taskID, userID)' for subj = '${receivedMailData.subject}'.")
                            Pair(null, null)
                        }
                    }
                }
            } ?: run {
                //--- если входящей почты нет, сделаем паузу
                AdvancedLogger.info("No incoming mail. Pause ${cyclePause / 1000} sec.")
                Thread.sleep(cyclePause)
            }
        }
    }

    private fun sendTask(rowID: Int) {
        //--- загрузим поручение
        var rs: CoreAdvancedResultSet = alStm[0].executeQuery(
            " SELECT out_user_id , in_user_id , subj , ye , mo , da FROM OFFICE_task WHERE id = $rowID"
        )
        if (!rs.next()) {
            rs.close()
            AdvancedLogger.error("Task not found for ID = $rowID")
            return
        }
        val outUserID = rs.getInt(1)
        val inUserID = rs.getInt(2)
        val taskSubj = rs.getString(3)
        val arrDT = intArrayOf(rs.getInt(4), rs.getInt(5), rs.getInt(6), 0, 0, 0)
        rs.close()

        AdvancedLogger.debug("--- send new task ---")
        AdvancedLogger.debug("from UserID = $outUserID")
        AdvancedLogger.debug("to UserID = $inUserID")
        AdvancedLogger.debug("Task ID = $rowID")
        AdvancedLogger.debug("Task Subj = '$taskSubj'")
        AdvancedLogger.debug("Task Day = ${arrDT[2]}")
        AdvancedLogger.debug("Task Month = ${arrDT[1]}")
        AdvancedLogger.debug("Task Year = ${arrDT[0]}")

        //--- если этого пользователя не существует или нет e-mail для оповещений, пропускаем из обработки
        val eMail = hmUserConfig[inUserID]?.eMail
        AdvancedLogger.debug("to User e-mail: '$eMail'")
        if (!eMail.isNullOrBlank() && eMail.contains("@")) {
            //--- проверим, а не прочитано ли уже оппонентом это сообщение
            rs = alStm[0].executeQuery(
                """
                     SELECT * FROM SYSTEM_new 
                     WHERE table_name = 'OFFICE_task' 
                     AND row_id = $rowID 
                     AND user_id = $inUserID
                """
            )
            val isReaded = rs.next()
            rs.close()

            //--- если ещё не прочитано, то стоит оповестить
            if (!isReaded) {
                //--- готовим тему письма
                val sbMailSubj = "Office#${mTask.ALERT_TAG}#$rowID#$inUserID#"
                //--- готовим текст самого письма
                val sbMailBody = "Новое поручение:" +
                    "\nПоручитель: ${UserConfig.hmUserFullNames[outUserID]}" +
                    "\nТема поручения: $taskSubj" +
                    "\nСрок исполнения: ${DateTime_DMYHMS(arrDT)}"
                //--- отправляем письмо
                sendMail(eMail, sbMailSubj, sbMailBody)
            } else {
                AdvancedLogger.debug("Task not sended: task already readed by user.")
            }
        }
        AdvancedLogger.debug("-".repeat(20))
    }

    private fun sendTaskThread(rowID: Int) {
        //--- загрузим новое сообщение из переписки
        var rs = alStm[0].executeQuery(
            " SELECT user_id , task_id FROM OFFICE_task_thread WHERE id = $rowID"
        )
        if (!rs.next()) {
            rs.close()
            AdvancedLogger.error("Task thread not found for ID = $rowID")
            return
        }
        val curUserID = rs.getInt(1)
        val taskID = rs.getInt(2)
        rs.close()

        //--- загрузим само поручение
        rs = alStm[0].executeQuery(
            " SELECT out_user_id , in_user_id , subj FROM OFFICE_task WHERE id = $taskID"
        )
        if (!rs.next()) {
            rs.close()
            AdvancedLogger.error("Task not found for ID = $taskID")
            return
        }
        val arrUserID = arrayOf(rs.getInt(1), rs.getInt(2))
        val taskSubj = rs.getString(3)
        rs.close()

        AdvancedLogger.debug("--- send new task thread ---")
        AdvancedLogger.debug("Current UserID = $curUserID")
        AdvancedLogger.debug("Task ID = $taskID")
        AdvancedLogger.debug("Task Subj = $taskSubj")

        //--- для каждой стороны поручения
        for (i in arrUserID.indices) {
            //--- автора последнего сообщения пропускаем из рассмотрения
            if (arrUserID[i] == curUserID) {
                continue
            }
            //--- если у этого пользователя нет e-mail для оповещений, пропускаем из обработки
            val eMail = hmUserConfig[arrUserID[i]]?.eMail
            AdvancedLogger.debug("to User e-mail: $eMail")
            if (eMail.isNullOrBlank() || !eMail.contains("@")) {
                continue
            }

            //--- проверим, а не прочитано ли уже оппонентом это сообщение
            rs = alStm[0].executeQuery(
                """
                    SELECT * FROM SYSTEM_new 
                    WHERE table_name = 'OFFICE_task_thread' 
                    AND row_id = $rowID
                    AND user_id = ${arrUserID[i]}
                """
            )
            val isReaded = rs.next()
            rs.close()
            //--- если ещё не прочитано, то стоит оповестить
            if (!isReaded) {
                var sbThread = ""
                var isOriginalMessagePosted = false
                //--- загрузим последние сообщения из переписки (в обратном порядке)
                rs = alStm[0].executeQuery(
                    """
                        SELECT id , user_id , ye , mo , da , ho , mi , message
                        FROM OFFICE_task_thread 
                        WHERE task_id = $taskID
                        ORDER BY ye DESC , mo DESC , da DESC , ho DESC , mi DESC
                    """
                )
                while (rs.next()) {
                    val id = rs.getInt(1)
                    val userID = rs.getInt(2)
                    val arrDT = intArrayOf(rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), 0)
                    val msg = rs.getString(8)
                    val userFullName = if (userID == 0) "" else UserConfig.hmUserFullNames[userID]
                    val sbTmp = userFullName +
                        " [ " + DateTime_DMYHMS(arrDT) + "]:\n" +
                        msg + "\n\n"
                    //--- сообщения в тело письма складываем в прямом порядке
                    sbThread = sbTmp + sbThread
                    //--- собственно оригинальное сообщение, ради которого весь шум, уже добавлено в письмо?
                    if (id == rowID) {
                        isOriginalMessagePosted = true
                    }
                    //--- если оригинальное сообщение уже добавлено в письмо,
                    //--- то складываем вплоть до предыдущего сообщения от оппонента
                    //--- (чтобы оппонент не потерял нити обсуждения)
                    if (isOriginalMessagePosted && userID != curUserID) {
                        break
                    }
                }
                rs.close()
                //--- готовим тему письма
                val sbMailSubj = "Office#${mTaskThread.ALERT_TAG}#$taskID#${arrUserID[i]}#"
                //--- готовим текст самого письма
                val sbMailBody = "Обсуждение по поручению:\n$taskSubj\n${"-".repeat(20)}\n$sbThread"
                //--- отправляем письмо
                sendMail(eMail, sbMailSubj, sbMailBody)
            } else {
                AdvancedLogger.debug("Task thread not sended: task already readed by user.")
            }
        }
        AdvancedLogger.debug("-".repeat(20))
    }

//    private fun sendReminder(rowID: Int) {
//        //--- загрузим напоминание
////  people_id         INT,
//        val rs: CoreAdvancedResultSet = alStm[0].executeQuery(
//            StringBuilder(
//                " SELECT user_id , type , ye , mo , da , ho , mi , subj , descr FROM OFFICE_reminder WHERE id = "
//            )
//                .append(rowID).toString()
//        )
//        if (!rs.next()) {
//            rs.close()
//            AdvancedLogger.error(StringBuilder("Reminder not found for ID = ").append(rowID).toString())
//            return
//        }
//        val userID: Int = rs.getInt(1)
//        val type: Int = rs.getInt(2)
//        val arrDT = intArrayOf(rs.getInt(3), rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), 0)
//        val subj: String = rs.getString(8)
//        val descr: String = rs.getString(9)
//        rs.close()
//        AdvancedLogger.debug("--- send new reminder ---")
//        AdvancedLogger.debug(StringBuilder("Reminder ID = ").append(rowID).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Type = ").append(type).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Day = ").append(arrDT[2]).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Month = ").append(arrDT[1]).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Year = ").append(arrDT[0]).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Hour = ").append(arrDT[3]).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Minute = ").append(arrDT[4]).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Subj = ").append(subj).toString())
//        AdvancedLogger.debug(StringBuilder("Reminder Descr = ").append(descr).toString())
//
//        //--- если этого пользователя не существует или нет e-mail для оповещений, пропускаем из обработки
//        val eMail = if (hmUserConfig!![userID] == null) null else hmUserConfig!![userID]!!.eMail
//        AdvancedLogger.debug(StringBuilder("to User e-mail: ").append(eMail).toString())
//        if (eMail != null && !eMail.trim { it <= ' ' }.isEmpty() && eMail.contains("@")) {
//            //--- готовим тему письма
//            val sbMailSubj: StringBuilder = StringBuilder("Office").append('#').append(mReminder.ALERT_TAG)
//            //.append( '#' ).append( rowID ).append( '#' ).append( userID ).append( '#' ); - без нужды
//            //--- готовим текст самого письма
//            val sbMailBody: StringBuilder = StringBuilder("Напоминание:").append(mReminder.hmReminderName.get(type))
//                .append("\nВремя: ").append(StringFunction.DateTime_DMYHMS(arrDT))
//                .append("\nТема: ").append(subj)
//                .append("\nПримечания: ").append(descr)
//            //--- отправляем письмо
//            sendMail(eMail, sbMailSubj.toString(), sbMailBody.toString())
//        }
//        AdvancedLogger.debug("---------------------")
//    }

    //--- отправка письма
    private fun sendMail(eMail: String, subj: String, body: String) {
        val props = System.getProperties()
        hmSmtpOption.keys.forEach { key ->
            props[key] = hmSmtpOption[key]
        }

        val session = Session.getDefaultInstance(props, null)
        val transport = session.getTransport("smtp")
        transport.connect(smtpServer, smtpPort, smtpLogin, smtpPassword)

        val msg: Message = MimeMessage(session)
        msg.sentDate = Date()
        msg.setFrom(InternetAddress(smtpLogin))
        msg.setRecipient(Message.RecipientType.TO, InternetAddress(eMail))
        msg.subject = subj
        msg.setText(body)
        msg.saveChanges()

        transport.sendMessage(msg, arrayOf(InternetAddress(eMail)))
        transport.close()
    }

    //--- получение письма с удалением его из почтового ящика
    private fun receiveMail(): ReceivedMailData? {
        var result: ReceivedMailData? = null

        val props = Properties()
        hmPop3Option.keys.forEach { key ->
            props[key] = hmPop3Option[key]
        }
        //--- по совету на одном из форумов - одно из многочисленнейших условий для корректной работы с MS Exchange
        //Session session = Session.getDefaultInstance( props, null );
        val session = Session.getInstance(props, null)
        val store = session.getStore("pop3")
        store.connect(pop3Server, pop3Login, pop3Password)
        val folder = store.getFolder("INBOX")
        folder.open(Folder.READ_WRITE)

        //--- вариант получения списка писем с одновременным удалением их из ящика:
        //--- Message[] arrMessage = folder.expunge();
        //--- но:
        //--- 1. не самый удачный вариант, т.к. в процессе разбора писем программа может вылететь с ошибкой и
        //--- недообработать письма, а они уже удалены
        //--- 2. якобы поддерживается не всеми серверами, лучше использовать каноничный
        //--- Message.setFlag( Flags.Flag.DELETED, true );
        folder.messages.firstOrNull()?.let { message ->
            result = ReceivedMailData(
                message.sentDate,
                message.subject,
                message.content.toString()
            )
            //--- пометим на удаление
            message.setFlag(Flags.Flag.DELETED, true)
        }
        //--- Закрыть соединение с удалением помеченных записей
        folder.close(true)
        store.close()

        return result
    }

    private fun receiveTaskThread(taskID: Int, userID: Int, date: Date, mailBody: String) {
        var arrDT = getDateTimeArray(ZoneId.systemDefault(), (date.time / 1000).toInt())
        AdvancedLogger.debug("--- MAIL BODY start ---")
        AdvancedLogger.debug(mailBody)
        AdvancedLogger.debug("--- MAIL BODY end ---")
        val stMailBody = StringTokenizer(mailBody, "\n")
        AdvancedLogger.debug("--- New Task Thread Message ---")
        //--- вырезаем и нормализуем первую строку сообщения - возможную команду/данные
        var cmd = ""
        var sbHtmlMessage: String? = null
        if (stMailBody.hasMoreElements()) {
            cmd = stMailBody.nextToken().trim().uppercase(Locale.getDefault())
            AdvancedLogger.debug("First row = '$cmd'")
            //--- пошла новая неотключаемая мода у Apple - письма исключительно в html-формате
            if (cmd.contains("<HTML>")) {
                sbHtmlMessage = ""
                //--- перематываем до <BODY
                while (stMailBody.hasMoreTokens()) {
                    cmd = stMailBody.nextToken().trim().uppercase(Locale.getDefault())
                    if (cmd.contains("<BODY")) {
                        break
                    }
                }
                //--- теперь сама команда
                cmd = stMailBody.nextToken().trim().uppercase(Locale.getDefault()).replace("<BR>", "").replace("&NBSP;", "")
                //--- собираем инфу до <DIV
                while (stMailBody.hasMoreTokens()) {
                    val msg = stMailBody.nextToken().uppercase(Locale.getDefault())
                    if (msg.contains("<DIV")) {
                        break
                    }
                    sbHtmlMessage += msg.replace("<BR>", " ").replace("&NBSP;", " ")
                }
                AdvancedLogger.debug("First row from HTML = '$cmd'")
                AdvancedLogger.debug("Rest HTML message= '$sbHtmlMessage'")
            }
        }
        var action = ACTION_EMPTY
        var actionDescr: String? = null
        var newUserID = 0
        var dayShift = 0
        var newDa = 0
        var newMo = 0
        var newYe = 0
        //--- удалить (а точнее, перенести задачу в архив)
        if (cmd == "УДАЛИТЬ" || cmd == "АРХИВ") {
            action = ACTION_DELETE
            actionDescr = "# Поручение перемещено в архив #"
        } else {
            //--- если есть однозначное совпадение (содержание в полном имени) пользователя, то переназначить
            UserConfig.hmUserFullNames.keys.forEach { uID ->
                val userName = UserConfig.hmUserFullNames[uID]?.uppercase(Locale.getDefault())
                //--- есть такой пользователь
                if (userName != null && userName.contains(cmd)) {
                    //--- если это в первый (единственный) раз, то запомним этого пользователя
                    if (newUserID == 0) {
                        newUserID = uID
                    } else {
                        newUserID = 0
                        return@forEach
                    }
                }
            }
            if (newUserID != 0) {
                action = ACTION_CHANGE_USER
                actionDescr = "# Поручение перенаправлено пользователю: ${UserConfig.hmUserFullNames[newUserID]} #"
            } else {
                //--- проверям на наличии числа - сдвига срока или даты, на которое перенести поручение
                val st = StringTokenizer(cmd, ".")
                when (st.countTokens()) {
                    1 -> try {
                        //--- попытаемся его преобразовать в число
                        dayShift = cmd.toInt()
                        action = ACTION_TIME_SHIFT
                        actionDescr = "# Поручение продлено на $dayShift дней #"
                    } catch (t: Throwable) {
                        AdvancedLogger.debug("Wrong cmd = '$cmd'")
                    }
//                    2 -> try {
//                        //--- попытаемся преобразовать числа даты\месяца
//                        newDa = st.nextToken().toInt()
//                        newMo = st.nextToken().toInt()
//                        //--- достаточно пока простой проверки
//                        if (newDa in 1..31 && newMo in 1..12) {
//                            newYe = arrDT[0] // берём текущий год
//                            action = ACTION_TIME_SET
//                            actionDescr = "# Поручение продлено до $newDa.$newMo.$newYe #"
//                        }
//                    } catch (t: Throwable) {
//                        AdvancedLogger.debug("Wrong cmd = '$cmd'")
//                    }
                    3 -> try {
                        //--- попытаемся преобразовать числа даты\месяца
                        newDa = st.nextToken().toInt()
                        newMo = st.nextToken().toInt()
                        newYe = st.nextToken().toInt()
                        //--- достаточно пока простой проверки
                        if (newDa in 1..31 && newMo in 1..12 && newYe > 0) {
                            //--- если там двузначное число года, то добавим 2000
                            if (newYe <= 99) {
                                newYe += 2000
                            }
                            action = ACTION_TIME_SET
                            actionDescr = "# Поручение продлено до $newDa.$newMo.$newYe #"
                        }
                    } catch (t: Throwable) {
                        AdvancedLogger.debug("Wrong cmd = '$cmd'")
                    }
                    else -> {
                        AdvancedLogger.debug("Wrong tokens count in cmd = '$cmd'")
                    }
                }
            }
        }
        //--- если никакая команда не распознана, то просто сдвигаем срок на один день
        if (action == ACTION_EMPTY) {
            dayShift = 1
            action = ACTION_TIME_SHIFT
            actionDescr = "$cmd\n# Поручение продлено на $dayShift день #"
        }

        AdvancedLogger.debug("Action = '$action'")
        AdvancedLogger.debug("Action Descr = '$actionDescr'")
        AdvancedLogger.debug("New User ID = $newUserID")
        AdvancedLogger.debug("Day Shift = $dayShift")
        AdvancedLogger.debug("New Day = $newDa")
        AdvancedLogger.debug("New Month = $newMo")
        AdvancedLogger.debug("New Year = $newYe")

        //--- составляем сообщение в систему
        var sbMailBody = actionDescr + "\n"
        //--- если это обычное письмо в обычном текстовом формате - пропускаем строки-цитаты из письма
        if (sbHtmlMessage == null) {
            while (stMailBody.hasMoreTokens()) {
                val s = stMailBody.nextToken() //.trim()
                if (!s.startsWith(">")) {
                    sbMailBody += if (sbMailBody.isEmpty()) {
                        ""
                    } else {
                        "\n"
                    } + s
                }
            }
        } else {
            sbMailBody += sbHtmlMessage
            //--- остальное игнорируем нафиг
        }
        val maxTextFieldSize = alConn[0].dialect.textFieldMaxSize / 2
        sbMailBody = if (sbMailBody.length > maxTextFieldSize) {
            sbMailBody.substring(0, maxTextFieldSize)
        } else {
            sbMailBody
        }

        //--- в любом случае пишем сообщение
        val rowID = alStm[0].getNextID("OFFICE_task_thread", "id")
        alStm[0].executeUpdate(
            """
                INSERT INTO OFFICE_task_thread ( id , user_id , task_id , ye , mo , da , ho , mi , message ) VALUES (
                    $rowID , $userID , $taskID , 
                    ${arrDT[0]} , ${arrDT[1]} , ${arrDT[2]} , ${arrDT[3]} , ${arrDT[4]} ,
                    '${prepareForSQL(sbMailBody)}' 
                ) 
            """
        )
        //--- и создаём оповещение по этому сообщению
        alStm[0].executeUpdate(
            """
                INSERT INTO SYSTEM_alert ( id , alert_time , tag , row_id ) VALUES ( 
                    ${alStm[0].getNextID("SYSTEM_alert", "id")} , -1 , '${mTaskThread.ALERT_TAG}' , $rowID 
                ) 
            """
        )

        //--- если отправитель сообщения является автором поручения, то выполним его команды
        when (action) {
            ACTION_DELETE -> alStm[0].executeUpdate(
                """
                    UPDATE OFFICE_task 
                    SET in_active = 0 , in_archive = 1 
                    WHERE id = $taskID
                    AND out_user_id = $userID
                """
            )
            ACTION_CHANGE_USER -> {
                //--- сменим исполнителя и продлим поручение на 1 день позже даты отправления его сообщения
                val gcNextDay = ZonedDateTime.of(arrDT[0], arrDT[1], arrDT[2], 0, 0, 0, 0, ZoneId.systemDefault())
                arrDT = getDateTimeArray(gcNextDay.plusDays(1))
                alStm[0].executeUpdate(
                    """
                        UPDATE OFFICE_task 
                        SET in_user_id = $newUserID , 
                            ye = ${arrDT[0]} , 
                            mo = ${arrDT[1]} , 
                            da = ${arrDT[2]}
                        WHERE id = $taskID
                        AND out_user_id = $userID
                    """
                )
            }
            ACTION_TIME_SHIFT -> {
                //--- продлим поручение на N дней позже даты отправления его сообщения
                val gcNextDay = ZonedDateTime.of(arrDT[0], arrDT[1], arrDT[2], 0, 0, 0, 0, ZoneId.systemDefault())
                arrDT = getDateTimeArray(gcNextDay.plusDays(dayShift.toLong()))
                alStm[0].executeUpdate(
                    """
                        UPDATE OFFICE_task 
                        SET ye = ${arrDT[0]} , 
                            mo = ${arrDT[1]} , 
                            da = ${arrDT[2]}
                        WHERE id = $taskID
                        AND out_user_id = $userID
                    """
                )
            }
            ACTION_TIME_SET ->             //--- продлим поручение до указанного срока
                alStm[0].executeUpdate(
                    """
                        UPDATE OFFICE_task 
                        SET ye = $newYe , 
                            mo = $newMo , 
                            da = $newDa
                        WHERE id = $taskID
                        AND out_user_id = $userID
                    """
                )
        }
        alConn[0].commit()
        AdvancedLogger.debug("-".repeat(20))
    }

    private class ReceivedMailData(
        val sentDate: Date,
        val subject: String,
        val content: String,
    )
}

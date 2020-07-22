package foatto.mms

import foatto.core.util.AdvancedLogger
import foatto.core.util.getCurrentTimeInt
import foatto.core.util.tokenize
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.AdvancedConnection
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ObjectMonitor(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private val NO_DATA_CHECK_PERIOD = "no_data_check_period"

        private val DEFAULT_EMAIL = "default_email"

        private val CONFIG_SMTP_SERVER = "smtp_server"
        private val CONFIG_SMTP_PORT = "smtp_port"
        private val CONFIG_SMTP_LOGIN = "smtp_login"
        private val CONFIG_SMTP_PASSWORD = "smtp_password"
        private val CONFIG_SMTP_OPTION_NAME_ = "smtp_option_name_"
        private val CONFIG_SMTP_OPTION_VALUE_ = "smtp_option_value_"

        //--- повторяем/дублируем алерты - не ранее чем через месяц
        private val NO_REPEAT_ALERT_PERIOD = 30 * 24 * 60 * 60

        //----------------------------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "ObjectMonitor"
                if(args.size == 1) {
                    ObjectMonitor(args[0]).run()
                    exitCode = 1
                }
                else println("Usage: $serviceWorkerName <ini-file-name>")
            }
            catch(t: Throwable) {
                t.printStackTrace()
            }

            System.exit(exitCode)
        }
    }

    private var noDataCheckPeriod = 1 * 60 * 60     // по умолчанию - через час начнётся паника

    private lateinit var alDefaultEmail: List<String>

    private lateinit var smtpServer: String
    private var smtpPort = 0
    private lateinit var smtpLogin: String
    private lateinit var smtpPassword: String
    private val hmSmtpOption = mutableMapOf<String, String>()

    //----------------------------------------------------------------------------------------------------------------------------------------

    override val isRunOnce: Boolean
        get() = true

    override fun loadConfig() {
        super.loadConfig()

        noDataCheckPeriod = (hmConfig[NO_DATA_CHECK_PERIOD]?.toIntOrNull() ?: 1 ) * 60 * 60

        alDefaultEmail = hmConfig[DEFAULT_EMAIL]?.tokenize( " ,;" ) ?: emptyList()

        smtpServer = hmConfig[CONFIG_SMTP_SERVER]!!
        smtpPort = Integer.parseInt(hmConfig[CONFIG_SMTP_PORT])

        smtpLogin = hmConfig[CONFIG_SMTP_LOGIN]!!
        smtpPassword = hmConfig[CONFIG_SMTP_PASSWORD]!!

        var index = 0
        while(true) {
            val optionName = hmConfig[CONFIG_SMTP_OPTION_NAME_ + index] ?: break

            hmSmtpOption[optionName] = hmConfig[CONFIG_SMTP_OPTION_VALUE_ + index]!!

            index++
        }
    }

    override fun initDB() {
        for(i in alDBConfig.indices) {
            alConn.add(AdvancedConnection(alDBConfig[i]))
            alStm.add(alConn[i].createStatement())
        }
    }

    override fun cycle() {

        val hmObjectEmail = mutableMapOf<Int,List<String>>()
        var rs = alStm[ 0 ].executeQuery( " SELECT e_mail, id FROM MMS_object WHERE id <> 0 " )
        while( rs.next() ) {
            val email = rs.getString( 1 )
            if( email.isNotBlank() )
                hmObjectEmail[ rs.getInt( 2 ) ] = email.tokenize( " ,;" )
        }
        rs.close()

        val hmUserEmail = mutableMapOf<Int,List<String>>()
        rs = alStm[ 0 ].executeQuery( " SELECT e_mail, id FROM SYSTEM_users WHERE id <> 0 " )
        while( rs.next() ) {
            val email = rs.getString( 1 )
            if( email.isNotBlank() )
                hmUserEmail[ rs.getInt( 2 ) ] = email.tokenize( " ,;" )
        }
        rs.close()

        val alObject = mutableListOf<ObjectData>()
        rs = alStm[ 0 ].executeQuery( " SELECT id , user_id , name , last_alert FROM MMS_object WHERE id <> 0 AND is_disabled = 0 " )
        while( rs.next() ) {
            alObject.add( ObjectData( rs.getInt( 1 ), rs.getInt( 2 ), rs.getString( 3 ), rs.getInt( 4 ) ) )
        }
        rs.close()

        alObject.forEach {
            var message = ""

            rs = alStm[ 0 ].executeQuery( " SELECT ontime FROM MMS_data_${it.objectId} ORDER BY ontime DESC " )
            if( rs.next() ) {
                val lastDataTime = rs.getInt(1)

                if( getCurrentTimeInt() - lastDataTime > noDataCheckPeriod )
                    message = "Нет данных от объекта '${it.name}'"
            }
            else {
                message = "Нет данных от объекта '${it.name}'"
            }
            rs.close()

            //--- свежие данные есть - снимаем время последнего алерта
            if( message.isBlank() ) {
                alStm[ 0 ].executeUpdate( " UPDATE MMS_object SET last_alert = 0 WHERE id = ${it.objectId} " )
            }
            //--- свежих данных нет и после последнего алерта прошло достаточное время
            else if( getCurrentTimeInt() - it.lastAlertTime > NO_REPEAT_ALERT_PERIOD ) {
                val alEmail = hmObjectEmail[ it.objectId ] ?: hmUserEmail[ it.userId ] ?: alDefaultEmail
                sendMail( alEmail, "Система контроля технологического оборудования и транспорта \"Пульсар\"", message )

                //--- обновляем время последнего алерта, чтобы не беспокоить ещё месяц
                alStm[ 0 ].executeUpdate( " UPDATE MMS_object SET last_alert = ${getCurrentTimeInt()} WHERE id = ${it.objectId} " )

                //--- чтоб нас в спамеры не засчитали :)
                Thread.sleep( 60_000 )
            }
            alConn[ 0 ].commit()
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- отправка письма
    private fun sendMail(alEmail: List<String>, subj: String, body: String) {
        val props = System.getProperties()
        for(key in hmSmtpOption.keys) props[key] = hmSmtpOption[key]

        val session = Session.getDefaultInstance(props, null)
        val transport = session.getTransport("smtp")

        transport.connect(smtpServer, smtpPort, smtpLogin, smtpPassword)

        val arrRecipient = alEmail.map { InternetAddress(it) }.toTypedArray()

        val msg = MimeMessage(session)
        msg.sentDate = Date()
        msg.setFrom(InternetAddress(smtpLogin))
        msg.setRecipients(Message.RecipientType.TO, arrRecipient )
        msg.subject = subj
        msg.setText(body)
        msg.saveChanges()

        transport.sendMessage(msg, arrRecipient)
        transport.close()

        AdvancedLogger.debug( "Mail sended.\nSubj: $subj\nBody: $body" )
    }

    private class ObjectData(
        val objectId: Int,
        val userId: Int,
        val name: String,
        val lastAlertTime: Int
    )
}

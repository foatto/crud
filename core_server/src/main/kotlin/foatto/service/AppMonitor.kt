package foatto.service

import foatto.core.util.AdvancedLogger
import foatto.core.util.DateTime_YMDHMS
import foatto.core.util.getCurrentTimeInt
import foatto.core_server.service.CoreServiceWorker
import foatto.sql.AdvancedConnection
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.time.ZoneId
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

class AppMonitor(aConfigFileName: String) : CoreServiceWorker(aConfigFileName) {

    companion object {

        private val CONFIG_CHECK_DESCR_ = "check_descr_"
        private val CONFIG_CHECK_TYPE_ = "check_type_"
        private val CONFIG_CHECK_DATA_ = "check_data_"
        private val CONFIG_CHECK_LAG_ = "check_lag_"

        private val CONFIG_EMAIL = "email"

        private val CONFIG_SMTP_SERVER = "smtp_server"
        private val CONFIG_SMTP_PORT = "smtp_port"
        private val CONFIG_SMTP_LOGIN = "smtp_login"
        private val CONFIG_SMTP_PASSWORD = "smtp_password"
        private val CONFIG_SMTP_OPTION_NAME_ = "smtp_option_name_"
        private val CONFIG_SMTP_OPTION_VALUE_ = "smtp_option_value_"

        //----------------------------------------------------------------------------------------------------------------------------------------

        @JvmStatic
        fun main(args: Array<String>) {
            var exitCode = 0   // нормальный выход с прерыванием цикла запусков
            try {
                serviceWorkerName = "AppMonitor"
                if (args.size == 1) {
                    AppMonitor(args[0]).run()
                    exitCode = 1
                } else println("Usage: ${serviceWorkerName} <ini-file-name>")
            } catch (t: Throwable) {
                t.printStackTrace()
            }

            exitProcess(exitCode)
        }
    }

    private val alCheckDescr = mutableListOf<String>()
    private val alCheckType = mutableListOf<Int>()
    private val alCheckData = mutableListOf<String>()
    private val alCheckLag = mutableListOf<Int>()

    private var email: String? = null

    private var smtpServer: String? = null
    private var smtpPort = 0
    private var smtpLogin: String? = null
    private var smtpPassword: String? = null
    private val hmSmtpOption = mutableMapOf<String, String>()

    //----------------------------------------------------------------------------------------------------------------------------------------

    private val zoneId = ZoneId.systemDefault()

    override val isRunOnce: Boolean
        get() = true

    override fun loadConfig() {
        super.loadConfig()

        var index = 0
        while (true) {
            val descr = hmConfig[CONFIG_CHECK_DESCR_ + index] ?: break

            alCheckDescr.add(descr)
            alCheckType.add(Integer.parseInt(hmConfig[CONFIG_CHECK_TYPE_ + index]))
            alCheckData.add(hmConfig[CONFIG_CHECK_DATA_ + index]!!)
            alCheckLag.add(hmConfig[CONFIG_CHECK_LAG_ + index]!!.toInt() * 60)

            index++
        }

        email = hmConfig[CONFIG_EMAIL]

        smtpServer = hmConfig[CONFIG_SMTP_SERVER]
        smtpPort = Integer.parseInt(hmConfig[CONFIG_SMTP_PORT])

        smtpLogin = hmConfig[CONFIG_SMTP_LOGIN]
        smtpPassword = hmConfig[CONFIG_SMTP_PASSWORD]

        index = 0
        while (true) {
            val optionName = hmConfig[CONFIG_SMTP_OPTION_NAME_ + index] ?: break

            hmSmtpOption[optionName] = hmConfig[CONFIG_SMTP_OPTION_VALUE_ + index]!!

            index++
        }
    }

    override fun initDB() {
        alDBConfig.forEach {
            val conn = AdvancedConnection(it)
            alConn.add(conn)
        }
    }

    override fun cycle() {

        for (checkIndex in alCheckType.indices) {
            when (alCheckType[checkIndex]) {
                //--- проверка наличия/даты последних файлов в папке (обычно логов)
                0 -> {
                    val lftv = LastFileTimeVisitor()
                    Files.walkFileTree(Paths.get(alCheckData[checkIndex]), lftv)
                    if (lftv.lastFileTime < getCurrentTimeInt() - alCheckLag[checkIndex]) {
                        sendMail(email, alCheckDescr[checkIndex], DateTime_YMDHMS(zoneId, lftv.lastFileTime))
                    }
                }
                //--- проверка наличия/даты первых файлов в папке (обычно файлов репликации)
                1 -> {
                    var firstFileTime = Int.MAX_VALUE
                    val arrFile = File(alCheckData[checkIndex]).listFiles()
                    for (file in arrFile!!) {
                        if (file.exists()) {
                            firstFileTime = min(firstFileTime, (file.lastModified() / 1000).toInt())
                        }
                    }
                    if (firstFileTime < getCurrentTimeInt() - alCheckLag[checkIndex]) {
                        sendMail(email, alCheckDescr[checkIndex], DateTime_YMDHMS(zoneId, firstFileTime))
                    }
                }
            }//                FirstFileTimeVisitor fftv = new FirstFileTimeVisitor();
            //                Files.walkFileTree( Paths.get( arrCheckData[ checkIndex ] ), fftv );
            //                if( fftv.firstFileTime < System.currentTimeMillis() - arrCheckLag[ checkIndex ] )
            //                    sendMail( email, arrCheckDescr[ checkIndex ],
            //                              StringFunction.DateTime_YMDHMS( timeZone, fftv.firstFileTime ).toString() );
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    //--- отправка письма
    private fun sendMail(eMail: String?, subj: String, body: String) {
        val props = System.getProperties()
        for (key in hmSmtpOption.keys) props[key] = hmSmtpOption[key]

        val session = Session.getDefaultInstance(props, null)
        val transport = session.getTransport("smtp")

        transport.connect(smtpServer, smtpPort, smtpLogin, smtpPassword)

        val msg = MimeMessage(session)
        msg.sentDate = Date()
        msg.setFrom(InternetAddress(smtpLogin!!))
        msg.setRecipient(Message.RecipientType.TO, InternetAddress(eMail!!))
        msg.subject = subj
        msg.setText(body)
        msg.saveChanges()

        transport.sendMessage(msg, arrayOf(InternetAddress(eMail)))
        transport.close()

        AdvancedLogger.debug("Mail sended.\nSubj: $subj\nBody: $body")
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

    private class LastFileTimeVisitor : SimpleFileVisitor<Path>() {

        var lastFileTime = 0

        override fun visitFile(path: Path, fileAttributes: BasicFileAttributes): FileVisitResult {

            if (fileAttributes.isRegularFile) lastFileTime = max(lastFileTime, (fileAttributes.lastModifiedTime().toMillis() / 1000).toInt())

            return FileVisitResult.CONTINUE
        }
    }

    //----------------------------------------------------------------------------------------------------------------------------------------

//    private class FirstFileTimeVisitor() : SimpleFileVisitor<Path>() {
//
//        var firstFileTime = java.lang.Long.MAX_VALUE
//
//        override fun visitFile(path: Path, fileAttributes: BasicFileAttributes): FileVisitResult {
//
//            if(fileAttributes.isRegularFile) firstFileTime = Math.min(firstFileTime, fileAttributes.lastModifiedTime().toMillis())
//
//            return FileVisitResult.CONTINUE
//        }
//    }
}

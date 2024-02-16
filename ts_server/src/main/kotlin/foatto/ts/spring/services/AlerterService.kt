package foatto.ts.spring.services

import foatto.spring.services.UserService
import foatto.sql.AdvancedConnection
import foatto.sql.DBConfig
import foatto.ts.core_ts.calc.ObjectState
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.sensor.config.SensorConfigState
import foatto.util.getDateTimeArray
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@Service
class AlerterService(
    private val userService: UserService,
    private val objectService: ObjectService,
) {

    private val hsErrorStatusCodes = setOf(
        SensorConfigState.STATE_UNPASS_DOWN,
        SensorConfigState.STATE_UNPASS_UP,
        SensorConfigState.STATE_WIRE_RUNOUT,
        SensorConfigState.STATE_DRIVE_PROTECT,
        SensorConfigState.STATE_STOPPED_BY_SERVER,
        SensorConfigState.STATE_BLOCKED_BY_SERVER,
    )

    @Value("\${db_name}")
    val dbName: String = ""

    @Value("\${db_url}")
    val dataSourceURL: String = ""

    @Value("\${db_login}")
    val dataSourceUser: String = ""

    @Value("\${db_password}")
    val dataSourcePassword: String = ""

    @Value("\${db_replication_name}")
    val dbReplicationName: String = ""

    @Value("\${db_replication_filter}")
    val dbReplicationFilter: String = ""

    @Value("\${db_replication_path}")
    val dbReplicationPath: String = ""

    @Value("\${smtp_server}")
    val smtpServer: String = ""

    @Value("\${smtp_port}")
    val smtpPort: String = ""

    @Value("\${smtp_login}")
    val smtpLogin: String = ""

    @Value("\${smtp_password}")
    val smtpPassword: String = ""

    @Value("\${smtp_option_names}")
    private val smtpOptionNames: Array<String> = emptyArray()

    @Value("\${smtp_option_values}")
    private val smtpOptionValues: Array<String> = emptyArray()

    @Value("\${admins_folder_id}")
    private val adminsFolderId: String = ""

    @Scheduled(
        initialDelay = 5_000,
        fixedDelay = 300_000,
    )
    fun alerter() {
        val dbConfig = DBConfig(dbName, dataSourceURL, dataSourceUser, dataSourcePassword, dbReplicationName, dbReplicationFilter, dbReplicationPath)
        val conn = AdvancedConnection(dbConfig)

        val adminEmails = mutableListOf<String>()
        val rs = conn.executeQuery(
            """
                SELECT e_mail
                FROM SYSTEM_users
                WHERE parent_id = $adminsFolderId
                AND is_disabled = 0 
            """
        )
        while (rs.next()) {
            val email = rs.getString(1)
            val emails = getEmailsFromString(email)
            adminEmails.addAll(emails)
        }
        rs.close()

        val hmUserEmail = mutableMapOf<Int, Set<String>>()

        userService.getAllWorkers().forEach { workerEntity ->
            val workerEmails = mutableSetOf<String>()

            workerEmails += adminEmails
            workerEmails += getEmailsFromString(workerEntity.eMail)

            userService.getBossesByParent(workerEntity.parentId).forEach { bossEntity ->
                val bossEmail = getEmailsFromString(bossEntity.eMail)
                hmUserEmail[bossEntity.id] = bossEmail.toSet()

                workerEmails += bossEmail
            }

            hmUserEmail[workerEntity.id] = workerEmails
        }

        val objectEntities = objectService.getAllObjects()

        for (objectEntity in objectEntities) {
            val objectEmails = hmUserEmail[objectEntity.userId] ?: continue
            if (objectEmails.isEmpty()) {
                continue
            }

            val (userConfig, hmFullNames, hmShortNames) = userService.getUserConfig(conn, objectEntity.userId)

            val objectConfig = objectService.getObjectConfig(
                hmUserFullNames = hmFullNames,
                hmUserShortNames = hmShortNames,
                userConfig = userConfig,
                objectId = objectEntity.id,
            )
            val stateSensorId = objectConfig.hmSensorConfig[SensorConfig.SENSOR_STATE]?.values?.firstOrNull()?.id ?: continue

//--- вроде как не надо, номер контроллера уже есть в наименовании объекта
//            var rs = conn.executeQuery(
//                """
//                    SELECT serial_no
//                    FROM TS_device
//                    WHERE object_id = ${objectEntity.id}
//                    AND device_index = 0
//                """
//            )
//            val deviceSerialNo = if (rs.next()) {
//                rs.getString(1)
//            } else {
//                ""
//            }
//            rs.close()

            var rs = conn.executeQuery(
                """
                    SELECT last_state FROM TS_sensor WHERE id = $stateSensorId 
                """
            )
            val lastState = if (rs.next()) {
                rs.getInt(1)
            } else {
                0
            }
            rs.close()

            val objectState = ObjectState.getState(conn, objectConfig)
            val curStateTime = objectState.lastDateTime ?: continue
            val curStateValue = objectState.tmStateValue.values.firstOrNull() ?: continue
            val curDepth = objectState.tmDepthValue.values.firstOrNull() ?: continue

            if (curStateValue != lastState) {
                if (curStateValue in hsErrorStatusCodes) {
                    val dt = getDateTimeArray(userConfig.upZoneId, curStateTime)
                    val monthName = when (dt[1]) {
                        1 -> "января"
                        2 -> "февраля"
                        3 -> "марта"
                        4 -> "апреля"
                        5 -> "мая"
                        6 -> "июня"
                        7 -> "июля"
                        8 -> "августа"
                        9 -> "сентября"
                        10 -> "октября"
                        11 -> "ноября"
                        12 -> "декабря"
                        else -> "${dt[1]}"
                    }
                    val hourStr = "${if (dt[3] < 10) "0" else ""}${dt[3]}"
                    val minuteStr = "${if (dt[4] < 10) "0" else ""}${dt[4]}"
                    val dtStr = "${dt[2]} $monthName ${dt[0]} $hourStr:$minuteStr"
//                    val fullObjectName = "$deviceSerialNo : ${objectEntity.name} [${hmFullNames[objectEntity.userId]}]"
                    val fullObjectName = "${objectEntity.name} [${hmFullNames[objectEntity.userId]}]"
                    val statusDescr = SensorConfigState.hmStateInfo[curStateValue]?.descr
                    for (objectEmail in objectEmails) {
                        sendErrorStatus(
                            email = objectEmail,
                            subj = "$fullObjectName - $statusDescr",
                            body =
                            """
                                Добрый день!<br>
                                <br>
                                <b>$dtStr</b> (по вашему местному времени)<br>
                                на установке депарафинизации УДС-Техно 110:<br>
                                <b>$fullObjectName</b>,<br>
                                возникла проблема, требующая внимания - <b>$statusDescr (глубина: $curDepth м)</b>.<br>
                                <br>
                                Рекомендуем отправить операторов для осмотра скважины на месте.<br>
                                ---<br>
                                Техническая поддержка:<br> 
                                +7-927-034-00-70, (8552) 78-20-60<br>
                                support@tehsmart.online<br>                              
                            """.trimIndent(),
                        )
                    }
                }
                conn.executeUpdate(
                    """
                        UPDATE TS_sensor SET last_state = $curStateValue WHERE id = $stateSensorId
                    """
                )
                conn.commit()
            }
        }

        conn.close()
    }

    private fun getEmailsFromString(emails: String?) =
        emails?.split(' ')?.map { email ->
            email.trim()
        }?.filter { email ->
            email.isNotEmpty()
        } ?: emptyList()

    private fun sendErrorStatus(
        email: String,
        subj: String,
        body: String,
    ) {
        val props = System.getProperties()
        smtpOptionNames.forEachIndexed { index, s ->
            props[s] = smtpOptionValues[index]
        }

        val session = Session.getDefaultInstance(props, null)
        val transport = session.getTransport("smtp")
        transport.connect(smtpServer, smtpPort.toInt(), smtpLogin, smtpPassword)

        val msg: Message = MimeMessage(session)
        msg.sentDate = Date()
        msg.setFrom(InternetAddress(smtpLogin))
        msg.setRecipient(Message.RecipientType.TO, InternetAddress(email))
        msg.subject = subj
        msg.setContent(body, "text/html; charset=utf-8")
        msg.saveChanges()

        transport.sendMessage(msg, arrayOf(InternetAddress(email)))
        transport.close()
    }
}
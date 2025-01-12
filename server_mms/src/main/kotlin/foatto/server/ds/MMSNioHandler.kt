package foatto.server.ds

import foatto.server.entity.SensorEntity
import foatto.server.model.SensorConfigGeo
import foatto.server.sql.CoreAdvancedConnection
import kotlinx.datetime.TimeZone
import java.nio.channels.SocketChannel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

abstract class MMSNioHandler : AbstractTelematicNioHandler() {

    companion object {
        val timeZone: TimeZone = TimeZone.currentSystemDefault()
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override val configSessionLogPath: String = "mms_log_session"
    override val configJournalLogPath: String = "mms_log_journal"

    //--- серийный номер прибора
    protected var serialNo = ""

    //--- номер версии прошивки
    protected var fwVersion = ""

    //--- конфигурация устройства
    protected var deviceConfig: DeviceConfig? = null
    protected val sensorConfigs: MutableMap<Int, MutableList<SensorEntity>> = mutableMapOf()               // portNum to sensorEntities
    protected val sensorCalibrations: MutableMap<Int, MutableList<Pair<Double, Double>>> = mutableMapOf()  // sensorId to list of <sensorValue to dataValue> pairs

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun prepareErrorCommand(dataWorker: CoreNioWorker) {
        MMSTelematicFunction.writeError(
            conn = dataWorker.conn,
            dirSessionLog = dirSessionLog,
            timeZone = timeZone,
            deviceConfig = deviceConfig,
            fwVersion = fwVersion,
            begTime = begTime,
            address = selectionKey?.let { sk ->
                (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
            } ?: "(unknown remote address)",
            status = status,
            errorText = "Disconnect from device ID = $serialNo",
            dataCount = dataCount,
            firstPointTime = firstPointTime,
            lastPointTime = lastPointTime,
        )
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun loadDeviceConfig(conn: CoreAdvancedConnection): Boolean {
        DeviceConfig.getDeviceConfig(conn, serialNo)?.let { dc ->
            deviceConfig = dc
            status += " ID 2;"

            if (dc.objectId != 0) {
                loadSensorConfigs(conn, dc)
            }
            return true
        } ?: run {
            //--- неизвестный контроллер
            MMSTelematicFunction.writeError(
                conn = conn,
                dirSessionLog = dirSessionLog,
                timeZone = timeZone,
                deviceConfig = deviceConfig,
                fwVersion = fwVersion,
                begTime = begTime,
                address = selectionKey?.let { sk ->
                    (sk.channel() as SocketChannel).remoteAddress.toString() + " -> " + (sk.channel() as SocketChannel).localAddress.toString()
                } ?: "(unknown remote address)",
                status = status,
                errorText = "Unknown device ID = $serialNo",
                dataCount = dataCount,
                firstPointTime = firstPointTime,
                lastPointTime = lastPointTime,
            )
            CoreTelematicFunction.writeJournal(
                dirJournalLog = dirJournalLog,
                timeZone = timeZone,
                address = (selectionKey!!.channel() as SocketChannel).remoteAddress.toString() + " -> " + (selectionKey!!.channel() as SocketChannel).localAddress.toString(),
                errorText = "Unknown device ID = $serialNo",
            )
            return false
        }
    }

    private fun loadSensorConfigs(conn: CoreAdvancedConnection, dc: DeviceConfig) {
        sensorConfigs.clear()

        var rs = conn.executeQuery(
            """
                SELECT port_num , id , sensor_type ,
                    min_moving_time , min_parking_time , min_over_speed_time , is_absolute_run , speed_round_rule , run_koef ,
                        is_use_pos , is_use_speed , is_use_run ,
                    bound_value , active_value , beg_work_value ,
                    min_on_time , min_off_time ,
                    smooth_time , ignore_min_sensor , ignore_max_sensor , liquid_name , liquid_norm ,
                    analog_min_view , analog_max_view , analog_min_limit , analog_max_limit ,  
                    is_absolute_count , energo_phase , in_out_type ,
                    container_type , analog_using_min_len , analog_is_using_calc ,
                    analog_detect_inc , analog_detect_inc_min_diff , analog_detect_inc_min_len , analog_inc_add_time_before , analog_inc_add_time_after ,
                    analog_detect_dec , analog_detect_dec_min_diff , analog_detect_dec_min_len , analog_dec_add_time_before , analog_dec_add_time_after                            
                FROM MMS_sensor
                WHERE id <> 0 
                AND object_id = ${dc.objectId}                         
            """
        )
        while (rs.next()) {
            var pos = 1

            val portNum = rs.getInt(pos++)
            val sensorEntities = sensorConfigs.getOrPut(portNum) {
                mutableListOf()
            }
            sensorEntities += SensorEntity(
                id = rs.getInt(pos++),
                obj = null,
                name = null,
                group = null,
                descr = null,
                portNum = portNum,
                sensorType = rs.getInt(pos++),
                serialNo = null,
                usingStartDate = null,

                minMovingTime = rs.getInt(pos++),
                minParkingTime = rs.getInt(pos++),
                minOverSpeedTime = rs.getInt(pos++),
                isAbsoluteRun = rs.getInt(pos++) != 0,
                speedRoundRule = rs.getInt(pos++),
                runKoef = rs.getDouble(pos++),
                isUsePos = rs.getInt(pos++) != 0,
                isUseSpeed = rs.getInt(pos++) != 0,
                isUseRun = rs.getInt(pos++) != 0,

                boundValue = rs.getInt(pos++),
                activeValue = rs.getInt(pos++),
                begWorkValue = rs.getDouble(pos++),

                cmdOnId = null,
                cmdOffId = null,
                signalOn = null,
                signalOff = null,

                minOnTime = rs.getInt(pos++),
                minOffTime = rs.getInt(pos++),

                smoothTime = rs.getInt(pos++),
                minIgnore = rs.getDouble(pos++),
                maxIgnore = rs.getDouble(pos++),
                liquidName = rs.getString(pos++),
                liquidNorm = rs.getDouble(pos++),

                minView = rs.getDouble(pos++),
                maxView = rs.getDouble(pos++),
                minLimit = rs.getDouble(pos++),
                maxLimit = rs.getDouble(pos++),
                indicatorDelimiterCount = null,
                indicatorMultiplicator = null,

                isAbsoluteCount = rs.getInt(pos++) != 0,
                phase = rs.getInt(pos++),
                inOutType = rs.getInt(pos++),

                containerType = rs.getInt(pos++),
                usingMinLen = rs.getInt(pos++),
                isUsingCalc = rs.getInt(pos++) != 0,

                detectIncMinDiff = rs.getDouble(pos++),
                detectIncKoef = rs.getDouble(pos++),
                detectIncMinLen = rs.getInt(pos++),
                incAddTimeBefore = rs.getInt(pos++),
                incAddTimeAfter = rs.getInt(pos++),

                detectDecKoef = rs.getDouble(pos++),
                detectDecMinDiff = rs.getDouble(pos++),
                detectDecMinLen = rs.getInt(pos++),
                decAddTimeBefore = rs.getInt(pos++),
                decAddTimeAfter = rs.getInt(pos++),

                schemeX = null,
                schemeY = null,
                smoothMethod = null,
            )
        }
        rs.close()

        sensorCalibrations.clear()

        sensorConfigs.values.forEach { sensorEntities ->
            sensorEntities.forEach { sensorEntity ->
                val pairs = sensorCalibrations.getOrPut(sensorEntity.id) {
                    mutableListOf()
                }
                rs = conn.executeQuery(
                    """
                        SELECT value_sensor , value_data
                        FROM MMS_sensor_calibration
                        WHERE sensor_id = ${sensorEntity.id}
                        ORDER BY value_sensor 
                    """
                )
                while (rs.next()) {
                    var pos = 1

                    pairs += rs.getDouble(pos++) to rs.getDouble(pos++)
                }
                rs.close()
            }
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun roundSpeed(speed: Double): Int =
        when (deviceConfig!!.speedRoundRule) {
            SensorConfigGeo.SPEED_ROUND_RULE_LESS -> floor(speed).toInt()
            SensorConfigGeo.SPEED_ROUND_RULE_GREATER -> ceil(speed).toInt()
            SensorConfigGeo.SPEED_ROUND_RULE_STANDART -> round(speed).toInt()
            else -> round(speed).toInt()
        }

}

/*
    object_id           INT,    -- объект
    name                VARCHAR( 250 ), -- невидимое имя датчика, для опознавания генератором датчиков при заливке измерений
    group_name          VARCHAR( 250 ), -- видимое описание группы датчиков для логической связки разнотипных датчиков в пределах одного графика/отчёта
    descr               VARCHAR( 250 ), -- видимое описание датчика наладчиком или генератором датчиков при заливке измерений
    serial_no           VARCHAR( 250 ), -- серийный номер
    beg_ye              INT,    -- дата ввода в эксплуатацию
    beg_mo              INT,
    beg_da              INT,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "object_id")
    var obj: ObjectEntity?,

    val name: String?,       // inner/system sensor name for programmatically sensors adding

    @Column(name = "group_name")
    val group: String?,      // sensor group name for sensors logical linking and/or grouping

    val descr: String?,      // sensor visible description

    @Column(name = "serial_no")
    val serialNo: String?,

    @AttributeOverrides(
        AttributeOverride(name = "ye", column = Column(name = "beg_ye")),
        AttributeOverride(name = "mo", column = Column(name = "beg_mo")),
        AttributeOverride(name = "da", column = Column(name = "beg_da")),
    )
    @Embedded
    val usingStartDate: DateEntity?,


    cmd_on_id           INT,    -- команда на включение
    cmd_off_id          INT,    -- команда на отключение
    signal_on           VARCHAR( 250 ), -- сигналы, разрешающие включение
    signal_off          VARCHAR( 250 ), -- сигналы, разрешающие отключение

    @Column(name = "cmd_on_id")
    val cmdOnId: Int?,

    @Column(name = "cmd_off_id")
    val cmdOffId: Int?,

    @Column(name = "signal_on")
    val signalOn: String?,

    @Column(name = "signal_off")
    val signalOff: String?,


    analog_indicator_delimiter_count    INT,    -- кол-во делений на шкале индикатора
    analog_indicator_multiplicator      FLOAT8, -- множитель значений на шкале индикатора

    @Column(name = "analog_indicator_delimiter_count")
    val indicatorDelimiterCount: Int?,

    @Column(name = "analog_indicator_multiplicator")
    val indicatorMultiplicator: Double?,


    scheme_x            INT,
    scheme_y            INT,

    @Column(name = "scheme_x")
    val schemeX: Int?,

    @Column(name = "scheme_y")
    val schemeY: Int?,


    container_type              INT NOT NULL DEFAULT(1),    -- тип ёмкости
    analog_using_min_len        INT,    -- минимальная продолжительность расхода
    analog_is_using_calc        INT,    -- использовать ли расчётный расход топлива за период заправок/сливов
    analog_detect_inc           FLOAT8,  -- скорость увеличения уровня (топлива) для детектора заправки
    analog_detect_inc_min_diff  FLOAT8,  -- минимально учитываемый объём заправки
    analog_detect_inc_min_len   INT,    -- минимально учитываемая продолжительность заправки
    analog_inc_add_time_before  INT,    -- добавить время до заправки
    analog_inc_add_time_after   INT,    -- добавить время после заправки
    analog_detect_dec           FLOAT8,  -- скорость уменьшения уровня (топлива) для детектора слива
    analog_detect_dec_min_diff  FLOAT8,  -- минимально учитываемый объём слива
    analog_detect_dec_min_len   INT,    -- минимально учитываемая продолжительность слива
    analog_dec_add_time_before  INT,    -- добавить время до заправки
    analog_dec_add_time_after   INT,    -- добавить время после заправки
*/

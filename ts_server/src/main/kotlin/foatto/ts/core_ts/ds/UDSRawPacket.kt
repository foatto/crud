package foatto.ts.core_ts.ds

import com.fasterxml.jackson.annotation.JsonProperty
import foatto.core.util.YMDHMS_DateTime
import foatto.core.util.getDateTimeInt
import java.time.ZoneId

class UDSRawPacket(

    // Идентификатор (серийный №) установки УДС
    @JsonProperty("uds-id")
    val udsId: String,              // "1"

    // Текущее внутреннее время на установке
    val datetime: String,           // "2000-11-15 14:54:50"

    // Код текущего состояния установки УДС
    val state: String,              // "23"

    // Глубина (в метрах)
    val depth: String,              // "0"

    // Скорость спуска (метров/час)
    val speed: String,              // "0"

    // Нагрузка на привод (%)
    val load: String,               // "0"

    // Дата и время начала следующей чистки
    val ncc: String,                // "0001-01-01 00:00:00"

    // Глубина чистки (параметр настройки)
    val cds: String,                // "1000"

    // Период чистки (параметр настройки)
    val cps: String,                // "30"

    // Скорость чистки (параметр настройки)
    val css: String,                // "441"

    // Уровень сигнала сотовой связи
    @JsonProperty("linklev")
    val linkLevel: String,          // "22"

    // Счётчик количества перезагрузок модема
    val mrc: String,                // "4"

    // Ограничение нагрузки на привод (параметр настройки)
    val cls: String,                // "150"

    // Строка с содержимым результата AT-команды запроса баланса (если баланс не запрашивался, указывается “-1”)
    val balance: String? = "-1",    // "-1"

    // Глубина парковки скребка (параметр настройки)
    @JsonProperty("park_depth")
    val parkDepth: String,          // "98"

    // Количество попыток прохода препятствия (параметр настройки)
    @JsonProperty("pass_attempt")
    val passAttempt: String,        // "50"

    // Синхронизация с запуском ЭЦН  (параметр настройки)
    @JsonProperty("ecnstart")
    val ecnStart: String,           // "0"

    // Пауза между проходами препятствия (параметр настройки)
    @JsonProperty("pass_delay")
    val passDelay: String,          // "10"

    // Текущая темп. внутри станции УДС (29,3˚С) (реле №1)
    val temp1: String,              // "293"

    // Текущая темп. на улице (23,4 ˚С) (реле №2)
    val temp2: String,              // "234"

    // Уровень температуры внутри (параметр настройки)
    @JsonProperty("setpoint1")
    val setPoint1: String,          // "200"

    // Уровень температуры снаружи (параметр настройки)
    @JsonProperty("setpoint2")
    val setPoint2: String,          // "500"

    // Работает или не работает реле №1 (температура)
    @JsonProperty("relay1state")
    val relay1State: String,        // "False"

    // Работает или не работает реле №2 (температура)
    @JsonProperty("relay2state")
    val relay2State: String,        // "False"

    // Работает или не работает канал №1
    @JsonProperty("channel1error")
    val channel1Error: String,      // "True"

    // Работает или не работает канал №2
    @JsonProperty("channel2error")
    val channel2Error: String,      // "True"

    // Работает или не работает модуль ТРМ (общее управление температурными датчиками)
    @JsonProperty("trmfail")
    val trmFail: String,            // "True"

    // Версия прошивки станции УДС
    val version: String? = null,    // "1.8.1"
) {
    fun normalize(zoneId: ZoneId) = UDSDataPacket(
        state = state.toInt(),
        depth = depth.toDouble(),
        speed = speed.toDouble(),
        load = load.toDouble(),
        nextCleanindDateTime = getDateTimeInt(YMDHMS_DateTime(zoneId, ncc)),
        cleaningDepth = cds.toDouble(),
        cleaningPeriod = cps.toInt(),
        cleaningSpeed = css.toDouble(),
        signalLevel = linkLevel.toDouble(),
        mrc = mrc.toInt(),
        driveLoadRestrict = cls.toDouble(),
        balance = balance?.toDoubleOrNull() ?: -1.0,
        parkDepth = parkDepth.toDouble(),
        passAttempt = passAttempt.toInt(),
        ecnStart = ecnStart.toInt(),
        passDelay = passDelay.toInt(),
        temp1 = temp1.toDouble() / 10,
        temp2 = temp2.toDouble() / 10,
        setPoint1 = setPoint1.toDouble() / 10,
        setPoint2 = setPoint2.toDouble() / 10,
        relay1State = relay1State.lowercase().toBoolean(),
        relay2State = relay2State.lowercase().toBoolean(),
        channel1Error = channel1Error.lowercase().toBoolean(),
        channel2Error = channel2Error.lowercase().toBoolean(),
        trmFail = trmFail.lowercase().toBoolean(),
    )
}

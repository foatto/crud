package foatto.ts.core_ts.ds

class UDSRawPacket(

//--- Идентификационные параметры ------------------------------------------

    // Версия протокола. 1 - УДС-101, 2,3,x - для всех новых
    val ver: Int,

    // Идентификатор (серийный №) установки УДС
    val id: Int,

    // Версия прошивки станции УДС
    // sample: "1.8.1"
    val vers: String? = null,

    // Текущее внутреннее время на установке
    // единицы : Unix Time в секундах с 1/1/2000 0:0:0
    val timeSys: Int,

    // IMEI код модема
    val imei: String? = null,

//--- Основные/рабочие параметры ------------------------------------------

    // Код текущего состояния установки УДС
    val state: Short,

    // Глубина
    // единицы  : метры
    // дискрета : 1м
    val depth: Int,

    // Скорость спуска
    // единицы  : метр/час
    // дискрета : 1м/ч
    val speed: Int,

    // Нагрузка на привод
    // единицы  : %
    // дискрета : 1%
    val load: Int,

    // Дата и время начала следующей чистки
    // единицы  : Unix Time в секундах с 1/1/2000 0:0:0
    val cycTime: Int,

//--- Параметры настройки ------------------------------------------

    // Синхронизация с запуском ЭЦН (параметр настройки)
    // 0 - нет синхронизации
    // 1 - есть
    val cycPump: Byte,

    // Период чистки (параметр настройки)
    // единицы  : часы
    // дискрета : 1 час
    val cycPeriod: Short,

    // Глубина чистки (параметр настройки)
    // единицы  : метры
    // дискрета : 1м
    val cycDepth: Int,

    // Глубина парковки скребка (параметр настройки)
    // единицы  : метры
    // дискрета : 1м
    val cycPark: Int,

    // Скорость чистки (параметр настройки)
    // единицы  : метр/час
    // дискрета : 1м/ч
    val cycSpeed: Short,

    // Ограничение нагрузки на привод (параметр настройки)
    // единицы  : процент
    // дискрета : 1%
    val cycLoad: Short,

    // Пауза между проходами препятствия (параметр настройки)
    // единицы  : секунды
    // дискрета : 1сек
    val cycPause: Short,

    // Количество попыток прохода препятствия (параметр настройки)
    val cycTry: Short,

//--- Параметры модема/связи ------------------------------------------

    // Счётчик количества перезагрузок модема
    val mdmRC: Int,

    // Уровень сигнала сотовой связи
    // единицы  : % для данного модема от 100
    // дискрета : 1%
    val mdmCSQ: Short,

    // Строка с содержимым результата AT-команды запроса баланса
    // если баланс не запрашивался - параметр не передается
    val mdmMany: Int? = null,

//--- Параметры температуры ------------------------------------------

    // Текущая темп. внутри станции УДС (29,3С) (реле №1)
    // единицы  : градусы цельсия
    // дискрета : 0.1 градус
    val chT1cur: Int? = null,

    // Текущая темп. на улице (23,4 С) (реле №2)
    // единицы  : градусы цельсия
    // дискрета : 0.1 градус
    val chT2cur: Int? = null,

    // Уровень температуры внутри (параметр настройки)
    // единицы  : градусы цельсия
    // дискрета : 0.1 градус
    val chT1set: Int? = null,

    // Уровень температуры снаружи (параметр настройки)
    // единицы  : градусы цельсия
    // дискрета : 0.1 градус
    val chT2set: Int? = null,

    // Работает или не работает реле №1 (температура)
    val chT1on: Byte? = null,

    // Работает или не работает реле №2 (температура)
    val chT2on: Byte? = null,

    // Работает или не работает канал №1
    val chT1state: Byte? = null,

    // Работает или не работает канал №2
    val chT2state: Byte? = null,

    // Работает или не работает модуль ТРМ (общее управление температурными датчиками)
    val chTcmn: Byte? = null,

    ) {
    fun normalize() = UDSDataPacket(
        state = state.toInt(),
        depth = depth.toDouble(),
        speed = speed.toDouble(),
        load = load.toDouble(),
        nextCleanindDateTime = UDSHandler.UDS_RAW_PACKET_TIME_BASE + cycTime,

        ecnRunSync = cycPump.toInt() == 1,
        cleaningPeriod = cycPeriod.toInt(),
        cleaningDepth = cycDepth.toDouble(),
        parkDepth = cycPark.toDouble(),
        cleaningSpeed = cycSpeed.toDouble(),
        driveLoadRestrict = cycLoad.toDouble(),
        passDelay = cycPause.toInt(),
        passAttempt = cycTry.toInt(),

        modemRestartCount = mdmRC,
        signalLevel = mdmCSQ.toDouble(),
        balance = mdmMany?.toDouble() ?: -1.0,

        t1Current = chT1cur?.toDouble()?.div(10),
        t2Current = chT2cur?.toDouble()?.div(10),
        t1Setup = chT1set?.toDouble()?.div(10),
        t2Setup = chT2set?.toDouble()?.div(10),
        t1State = chT1on?.toInt() == 1,
        t2State = chT2on?.toInt() == 1,
        ch1State = chT1state?.toInt() == 1,
        ch2State = chT2state?.toInt() == 1,
        tpmState = chTcmn?.toInt() == 1,
    )
}

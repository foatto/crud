package foatto.ts.core_ts.ds.skk

class SKKPacket(

    // id измерения. в чистом виде это UnixTime создания этого измерения
    val id: Int,

    // тип пакета. ==1: заголовок измерения
    // тип пакета. ==2: данные измерения
    val type: Int,

    //--- для type == 1 ---

    // системное время передачи этого пакета (секунды от 01.01.2000 00:00:00)
    val sysTime: Int? = null,

    // UnixTime создания измерения (секунды от 01.01.2000 00:00:00)
    val onTime: Int? = null,

    // номер блока управления
    val bu: Int? = null,

    // версия ПО
    val vers: String? = null,

    // данные по модему
    val modem: SKKHeaderModemData? = null,

    // уставки по глубине
    val depth: SKKHeaderDepthData? = null,

    // уставки по скорости
    val speed: SKKHeaderSpeedData? = null,

    // уставки по нагрузке
    val force: SKKHeaderForceData? = null,

    // тип проволоки. float значение * 1000. т.е. проволока 1.8 мм передается как 1800
    val wire: Int? = null,

    // импульсов на оборот
    val encoder: Int? = null,

    // оборот ролика
    val roller: Int? = null,

    // месторождение
    val area: String? = null,

    // куст
    val bush: String? = null,

    // скважина
    val well: String? = null,

    // цех/подразделение
    val shop: String? = null,

    // бригада
    val crew: String? = null,

    // гос номер автомобиля
    val auto: String? = null,

    // ФИО ответственного
    val people: String? = null,

    //--- для type == 2 ---

    // очередные данные. количество в массиве - сколько влезет
    val data: List<SKKDataData>? = null,
)

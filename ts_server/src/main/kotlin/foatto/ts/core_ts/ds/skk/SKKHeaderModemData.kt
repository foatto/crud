package foatto.ts.core_ts.ds.skk

class SKKHeaderModemData(

    // imei модема
    val imei: String,

    // состояние модема
    val state: Int,

    // уровень сигнала модема (проценты от 100%)
    val csq: Int,

    // баланс модема (0 если нет данных)
    val many: Int,
)

package foatto.mms.core_mms.ds

import java.time.Instant

//--- JSON-class, do not rename fields!
// @Serializable - для перехода на kotlin-serilization
class PulsarData(
    //!!! при переходе на kotlin-serilization перестаёт работать сериализация Instant, заменить на String и его ручной разбор?
    val dateTime: Instant? = null,

    val deviceID: String? = null,
    val blockID: String? = null,

    val idx: Int? = null,
    val vals: Array<Map<String, Double>>? = null,
)


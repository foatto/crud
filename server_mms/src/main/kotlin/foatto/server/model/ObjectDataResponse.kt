package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class ObjectDataResponse(
    val errorMessage: String? = null,
    val sensors: List<ObjectDataResponseSensorInfo>? = null,
)

@Serializable
class ObjectDataResponseSensorInfo(
    val name: String?,
    val type: Int?,
    val data: List<Map<String, String>>,
)

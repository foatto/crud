package foatto.server.model

import kotlinx.serialization.Serializable

@Serializable
class ObjectDataRequest(
    val token: String,

    val name: String,
    val start: String? = null,
    val duration: Int? = null,
)
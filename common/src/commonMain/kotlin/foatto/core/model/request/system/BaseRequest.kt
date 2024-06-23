package foatto.core.model.request.system

import kotlinx.serialization.Serializable

@Serializable
sealed class BaseRequest(
    val url: String,
) {
    var sessionId: Long = 0
}
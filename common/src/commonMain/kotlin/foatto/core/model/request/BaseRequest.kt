package foatto.core.model.request

import kotlinx.serialization.Serializable

@Serializable
sealed class BaseRequest(
    val url: String,
) {
    var sessionId: Long = 0
}
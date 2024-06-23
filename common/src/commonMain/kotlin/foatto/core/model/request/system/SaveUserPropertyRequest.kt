package foatto.core.model.request.system

import kotlinx.serialization.Serializable

@Serializable
class SaveUserPropertyRequest(val name: String, val value: String) {
    var sessionId: Long = 0
}

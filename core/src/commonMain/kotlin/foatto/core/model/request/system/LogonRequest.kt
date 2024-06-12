package foatto.core.model.request.system

import foatto.core.model.ApiUrls
import kotlinx.serialization.Serializable

@Serializable
class LogonRequest(
    val login: String,
    val password: String,
) : BaseRequest(
    url = ApiUrls.LOGON,
)

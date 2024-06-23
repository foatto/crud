package foatto.core.model.request.system

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class LogoffRequest : BaseRequest(
    url = ApiUrl.LOGOFF,
)

package foatto.core.model.request

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class LogoffRequest : BaseRequest(
    url = ApiUrl.LOGOFF,
)

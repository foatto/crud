package foatto.core.model.request.system

import foatto.core.model.ApiUrls
import kotlinx.serialization.Serializable

@Serializable
class LogoffRequest : BaseRequest(
    url = ApiUrls.LOGOFF,
)

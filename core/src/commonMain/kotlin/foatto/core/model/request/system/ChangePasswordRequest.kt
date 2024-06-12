package foatto.core.model.request.system

import foatto.core.model.ApiUrls
import kotlinx.serialization.Serializable

@Serializable
class ChangePasswordRequest(
    val password: String,
) : BaseRequest(
    url = ApiUrls.CHANGE_PASSWORD,
)

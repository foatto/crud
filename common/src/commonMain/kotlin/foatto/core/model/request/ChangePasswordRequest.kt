package foatto.core.model.request

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class ChangePasswordRequest(
    val password: String,
) : BaseRequest(
    url = ApiUrl.CHANGE_PASSWORD,
)

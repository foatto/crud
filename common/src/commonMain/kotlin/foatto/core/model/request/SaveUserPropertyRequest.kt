package foatto.core.model.request

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class SaveUserPropertyRequest(
    val name: String,
    val value: String
) : BaseRequest(
    url = ApiUrl.SAVE_USER_PROPERTY,
)

package foatto.core.model.request

import foatto.core.ApiUrl
import kotlinx.serialization.Serializable

@Serializable
class GetShortFileLinkRequest(
    val copyRef: Long,
    val hour: Int,
) : BaseRequest(
    url = ApiUrl.GET_SHORT_FILE_LINK,
)

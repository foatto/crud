package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
class GetShortFileLinkResponse(
    override val responseCode: ResponseCode = ResponseCode.NOTHING,

    val url: String,
) : BaseResponse()
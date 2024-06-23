package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
class ChangePasswordResponse(
    override val responseCode: ResponseCode = ResponseCode.NOTHING,
) : BaseResponse()

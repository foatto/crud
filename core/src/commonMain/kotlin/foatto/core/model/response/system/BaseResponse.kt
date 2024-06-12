package foatto.core.model.response.system

import kotlinx.serialization.Serializable

@Serializable
sealed class BaseResponse {
    abstract val responseCode: ResponseCode
}
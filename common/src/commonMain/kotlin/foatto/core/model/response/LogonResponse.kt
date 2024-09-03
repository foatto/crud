package foatto.core.model.response

import foatto.core.model.AppUserConfig
import foatto.core.model.request.AppRequest
import kotlinx.serialization.Serializable

@Serializable
class LogonResponse(
    override val responseCode: ResponseCode,

    val appUserConfig: AppUserConfig? = null,
    val alMenuData: List<MenuData>? = null,
    val redirectOnLogon: AppRequest? = null,
) : BaseResponse()
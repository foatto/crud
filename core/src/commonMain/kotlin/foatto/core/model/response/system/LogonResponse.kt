package foatto.core.model.response.system

import foatto.core.model.AppUserConfig
import foatto.core.model.request.system.AppRequest
import kotlinx.serialization.Serializable

@Serializable
class LogonResponse(
    override val responseCode: ResponseCode,
    val appUserConfig: AppUserConfig? = null,
    val alMenuData: List<MenuData>? = null,
    val redirectOnLogon: AppRequest? = null,
) : BaseResponse()
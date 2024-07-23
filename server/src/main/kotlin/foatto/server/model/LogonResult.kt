package foatto.server.model

import foatto.core.model.AppUserConfig
import foatto.core.model.request.AppRequest
import foatto.core.model.response.MenuData
import foatto.core.model.response.ResponseCode

class LogonResult(
    val responseCode: ResponseCode,
    val appUserConfig: AppUserConfig? = null,
    val alMenuData: List<MenuData>? = null,
    val redirectOnLogon: AppRequest? = null,
)
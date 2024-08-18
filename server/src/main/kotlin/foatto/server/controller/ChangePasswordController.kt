package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.ChangePasswordRequest
import foatto.core.model.response.ChangePasswordResponse
import foatto.server.service.ChangePasswordService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ChangePasswordController(
    private val changePasswordService: ChangePasswordService,
) {

    @PostMapping(ApiUrl.CHANGE_PASSWORD)
    fun changePassword(
        @RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {

        changePasswordService.changePassword(
            sessionId = changePasswordRequest.sessionId,
            password = changePasswordRequest.password,
        )

        return ChangePasswordResponse()
    }
}
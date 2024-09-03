package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.SaveUserPropertyRequest
import foatto.core.model.response.SaveUserPropertyResponse
import foatto.server.service.UserPropertyService
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class UserPropertyController(
    private val userPropertyService: UserPropertyService,
) {

    @PostMapping(ApiUrl.SAVE_USER_PROPERTY)
    @Transactional
    fun saveUserProperty(
        @RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        return userPropertyService.saveUserProperty(
            sessionId = saveUserPropertyRequest.sessionId,
            name = saveUserPropertyRequest.name,
            value = saveUserPropertyRequest.value,
        )
    }

}
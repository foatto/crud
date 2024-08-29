package foatto.server.service

import foatto.core.model.response.ResponseCode
import foatto.core.model.response.SaveUserPropertyResponse
import foatto.core.util.getRandomInt
import foatto.server.SpringApp
import foatto.server.entity.UserPropertyEntity
import foatto.server.repository.UserPropertyRepository
import org.springframework.stereotype.Service

@Service
class UserPropertyService(
    private val userPropertyRepository: UserPropertyRepository,
) {

    fun saveUserProperty(
        sessionId: Long,
        name: String,
        value: String,
    ): SaveUserPropertyResponse {
        val sessionData = SpringApp.getSessionData(sessionId) ?: return SaveUserPropertyResponse(ResponseCode.LOGON_NEED)
        val userConfig = sessionData.serverUserConfig ?: return SaveUserPropertyResponse(ResponseCode.LOGON_NEED)

        val newUserPropertyEntity = userPropertyRepository.findByUserIdAndName(userConfig.id, name).firstOrNull()?.let { userPropertyEntity ->
            userPropertyEntity.value = value
            userPropertyEntity
        } ?: run {
            UserPropertyEntity(
                id = getNextId(),
                userId = userConfig.id,
                name = name,
                value = value,
            )
        }

        userPropertyRepository.saveAndFlush(newUserPropertyEntity)
        userConfig.userProperties[name] = value

        return SaveUserPropertyResponse(ResponseCode.OK)
    }

    private fun getNextId(): Int {
        var nextId: Int
        while (true) {
            nextId = getRandomInt()
            if (nextId == 0) {
                continue
            }
            if (userPropertyRepository.existsById(nextId)) {
                continue
            }
            return nextId
        }
    }

}
package foatto.server.service

import foatto.server.SpringApp
import foatto.server.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ChangePasswordService(
    private val userRepository: UserRepository,
) {

    fun changePassword(
        sessionId: Long,
        password: String,
    ) {
        SpringApp.getSessionData(sessionId)?.serverUserConfig?.let { serverUserConfig ->
            userRepository.findByIdOrNull(serverUserConfig.id)?.let { userEntity ->
                userEntity.password = password
                userRepository.saveAndFlush(userEntity)
            }
        }
    }
}
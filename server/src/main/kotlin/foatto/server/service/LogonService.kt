package foatto.server.service

import foatto.core.model.request.AppRequest
import foatto.core.model.response.ResponseCode
import foatto.server.OrgType
import foatto.server.SpringApp
import foatto.server.UserRelationEnum
import foatto.server.appRoleConfigs
import foatto.server.entity.UserEntity
import foatto.server.menuInit
import foatto.server.model.LogonResult
import foatto.server.model.ServerUserConfig
import foatto.server.model.SessionData
import foatto.server.repository.UserPropertyRepository
import foatto.server.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class LogonService(
    private val userRepository: UserRepository,
    private val userPropertyRepository: UserPropertyRepository,
) {

    fun logon(
        sessionId: Long,
        login: String,
        password: String,
    ): LogonResult =
        userRepository.findByLogin(login).firstOrNull()?.let { userEntity ->
            val responseCode = checkLogon(userEntity, password)
            if (responseCode == ResponseCode.LOGON_SUCCESS) {
                val serverUserConfig = ServerUserConfig(
                    id = userEntity.id,
                    currentUserName = userEntity.fullName ?: "(неизвестно)",    //return LogonResult(ResponseCode.LOGON_FAILED),
                    roles = userEntity.roles,
                    timeOffset = userEntity.timeOffset ?: (3 * 3600), //return LogonResult(ResponseCode.LOGON_FAILED),
                    fullNames = loadFullUserNames(),
                    shortNames = loadShortUserNames(),
                    relatedUserIds = loadRelatedUserIds(
                        userId = userEntity.id,
                        parentId = userEntity.parentId ?: 0,
                        orgType = userEntity.orgType ?: OrgType.ORG_TYPE_WORKER
                    ),
                    userProperties = userPropertyRepository.findByUserId(userEntity.id)
                        .filter { userPropertyEntity ->
                            !userPropertyEntity.name.isNullOrBlank() && !userPropertyEntity.value.isNullOrBlank()
                        }.associate { userPropertyEntity ->
                            (userPropertyEntity.name ?: "") to (userPropertyEntity.value ?: "")
                        }.toMutableMap(),
                )

                val sessionData = SpringApp.getSessionData(sessionId) ?: SessionData()
                SpringApp.putSessionData(sessionId, sessionData.copy(serverUserConfig = serverUserConfig))

                //!!! пока что ищем redirectOnLogon у любой своей роли :(
                //--- надо как-то отмечать "основную" роль
                var redirectOnLogon: AppRequest? = null
                for (role in userEntity.roles) {
                    appRoleConfigs[role]?.let { roleConfig ->
                        roleConfig.redirectOnLogon?.let { rol ->
                            redirectOnLogon = rol
                        }
                    }
                }

                LogonResult(
                    responseCode = responseCode,
                    appUserConfig = serverUserConfig.toAppUserConfig(),
                    alMenuData = menuInit(serverUserConfig),
                    redirectOnLogon = redirectOnLogon,
                )
            } else {
                LogonResult(responseCode)
            }
        } ?: LogonResult(ResponseCode.LOGON_FAILED)

    private fun checkLogon(userEntity: UserEntity, password: String): ResponseCode =
        if (userEntity.isDisabled == true) {
            ResponseCode.LOGON_ADMIN_BLOCKED
        } else if (userEntity.password != password) {
            ResponseCode.LOGON_FAILED
        } else {
            ResponseCode.LOGON_SUCCESS
        }

    private fun loadFullUserNames(): Map<Int, String> {
        val hmFullName = mutableMapOf<Int, String>()

        val userEntities = userRepository.findAll()
        userEntities.forEach { userEntity ->
            if (userEntity.id != 0) {
                hmFullName[userEntity.id] = userEntity.fullName ?: "(неизвестно)"
            }
        }
        return hmFullName
    }

    private fun loadShortUserNames(): Map<Int, String> {
        val hmShortName = mutableMapOf<Int, String>()

        val userEntities = userRepository.findAll()
        userEntities.forEach { userEntity ->
            if (userEntity.id != 0) {
                hmShortName[userEntity.id] = userEntity.shortName ?: "(неизвестно)"
            }
        }
        return hmShortName
    }

    fun loadRelatedUserIds(
        userId: Int,
        parentId: Int,
        orgType: Int,
    ): Map<Int, UserRelationEnum> {
        val relatedUserIds = mutableMapOf<Int, UserRelationEnum>()

        //--- список всех пользователей,
        //--- из которого путем последовательного исключения основных категорий пользователей
        //--- образуется список пользователей категории "все остальные"
        val hsOtherUsers = loadAllUserIds()

        //--- ничейное (userId == 0)
        relatedUserIds[0] = UserRelationEnum.NOBODY
        hsOtherUsers -= 0

        //--- свой userId
        relatedUserIds[userId] = UserRelationEnum.SELF
        hsOtherUsers -= userId

        //--- userId коллег одного уровня в одном подразделении
        val hsEqualUserIds = loadUserIds(parentId, orgType).toMutableSet()
        hsEqualUserIds.remove(userId)
        fillUserRelations(hsEqualUserIds, UserRelationEnum.EQUAL, relatedUserIds)
        hsOtherUsers -= hsEqualUserIds

        //--- userId начальников
        val hsBossUserIds = mutableSetOf<Int>()
        var pId = if (orgType == OrgType.ORG_TYPE_WORKER) {
            parentId
        } else if (parentId != 0) {
            getUserParentId(parentId)
        } else {
            0
        }
        hsBossUserIds += loadUserIds(pId, OrgType.ORG_TYPE_BOSS)
        while (pId != 0) {
            pId = getUserParentId(pId)
            hsBossUserIds += loadUserIds(pId, OrgType.ORG_TYPE_BOSS)
        }
        fillUserRelations(hsBossUserIds, UserRelationEnum.BOSS, relatedUserIds)
        hsOtherUsers -= hsBossUserIds

        //--- userId подчиненных
        if (orgType == OrgType.ORG_TYPE_BOSS) {
            val hsWorkerUserIds = mutableSetOf<Int>()
            //--- на своем уровне
            hsWorkerUserIds += loadUserIds(parentId, OrgType.ORG_TYPE_WORKER)
            //--- начальники подчиненных подразделений также являются прямыми подчиненными
            val alDivisionList = loadUserIds(parentId, OrgType.ORG_TYPE_DIVISION).toMutableList()
            //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
            var i = 0
            while (i < alDivisionList.size) {
                val bpId = alDivisionList[i]
                hsWorkerUserIds += loadUserIds(bpId, OrgType.ORG_TYPE_BOSS)
                hsWorkerUserIds += loadUserIds(bpId, OrgType.ORG_TYPE_WORKER)

                alDivisionList += loadUserIds(bpId, OrgType.ORG_TYPE_DIVISION)
                i++
            }
            fillUserRelations(hsWorkerUserIds, UserRelationEnum.WORKER, relatedUserIds)
            hsOtherUsers -= hsWorkerUserIds
        }

        //--- userId всех остальных
        fillUserRelations(hsOtherUsers, UserRelationEnum.OTHER, relatedUserIds)

        return relatedUserIds
    }

    private fun loadAllUserIds(): MutableSet<Int> =
        userRepository.findAll().filter { userEntity ->
            userEntity.orgType != OrgType.ORG_TYPE_DIVISION
        }.map { userEntity ->
            userEntity.id
        }.toMutableSet()

    private fun loadUserIds(parentId: Int, orgType: Int): Set<Int> =
        userRepository.findByParentIdAndOrgType(parentId, orgType).map { userEntity ->
            userEntity.id
        }.toMutableSet().apply {
            remove(0)
        }

    private fun getUserParentId(userId: Int): Int = userRepository.findByIdOrNull(userId)?.parentId ?: 0

    private fun fillUserRelations(
        hsUserIds: Set<Int>,
        userRelation: UserRelationEnum,
        relatedUserIds: MutableMap<Int, UserRelationEnum>
    ) {
        hsUserIds.forEach { userId ->
            relatedUserIds[userId] = userRelation
        }
    }

}
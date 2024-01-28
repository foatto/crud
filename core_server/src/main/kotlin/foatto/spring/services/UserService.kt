package foatto.spring.services

import foatto.core_server.app.server.OrgType
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.UserRelation
import foatto.core_server.app.server.cStandart
import foatto.core_server.app.server.data.DataString
import foatto.spring.controllers.CoreAppController
import foatto.spring.repositories.UserRepository
import foatto.sql.CoreAdvancedConnection
import foatto.util.encodePassword
import foatto.util.getCurrentTimeInt
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {

    fun getAllUsers() = userRepository.findAll()

    fun loadUserIdList(conn: CoreAdvancedConnection, parentId: Int, orgType: Int): Set<Int> {
        val hsUserId = mutableSetOf<Int>()

        val rs = conn.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = $parentId AND org_type = $orgType ")
        while (rs.next()) {
            hsUserId += rs.getInt(1)
        }
        rs.close()

        return hsUserId
    }

    fun getUserConfig(conn: CoreAdvancedConnection, userId: Int): Triple<UserConfig, Map<Int, String>, Map<Int, String>> {
        val (isAdmin, isCleanAdmin) = loadAdminRoles(conn, userId)
        lateinit var userConfig: UserConfig

        val rs = conn.executeQuery(" SELECT parent_id , org_type FROM SYSTEM_users WHERE id = $userId ")
        rs.next()
        userConfig = UserConfig(
            userId = userId,
            parentId = rs.getInt(1),
            orgType = rs.getInt(2),
            isAdmin = isAdmin,
            isCleanAdmin = isCleanAdmin,
            hmUserProperty = loadUserProperies(conn, userId),
        )
        rs.close()

        //--- вторичная загрузка данных
        val (hmFullName, hmShortName) = reloadUserNames()
        loadUserPermission(conn, userConfig)
        loadUserIDList(conn, userConfig)

        return Triple(userConfig, hmFullName, hmShortName)
    }

    fun saveUserProperty(conn: CoreAdvancedConnection, userId: Int?, userConfig: UserConfig?, upName: String, upValue: String) {
        val uId = userId ?: userConfig?.userId ?: return

        if (conn.executeUpdate(" UPDATE SYSTEM_user_property SET property_value = '$upValue' WHERE user_id = $uId AND property_name = '$upName' ") == 0) {
            conn.executeUpdate(" INSERT INTO SYSTEM_user_property ( user_id , property_name , property_value ) VALUES ( $uId , '$upName' , '$upValue' ) ")
        }

        userConfig?.hmUserProperty?.put(upName, upValue)
    }

    //--- раскраска фона имени пользователя в зависимости от времени последнего входа в систему
    fun getUserNameColor(isDisabled: Boolean, lastLogonTime: Int): Int {
        val curTime = getCurrentTimeInt()

        return if (isDisabled) {
            cStandart.TABLE_CELL_FORE_COLOR_DISABLED
        } else if (curTime - lastLogonTime > 7 * 24 * 60 * 60) {
            cStandart.TABLE_CELL_FORE_COLOR_CRITICAL
        } else if (curTime - lastLogonTime > 1 * 24 * 60 * 60) {
            cStandart.TABLE_CELL_FORE_COLOR_WARNING
        } else {
            cStandart.TABLE_CELL_FORE_COLOR_NORMAL
        }
    }

    fun checkAndSetNewPassword(conn: CoreAdvancedConnection, id: Int, pwd: DataString?) {
        var oldPassword = ""
        //--- запомнить старое значение пароля для возможной шифрации нового заданного пароля
        //--- на всякий случай проверка
        if (id != 0) {
            val rs = conn.executeQuery(" SELECT pwd FROM SYSTEM_users WHERE id = $id ")
            if (rs.next()) {
                oldPassword = rs.getString(1)
            }
            rs.close()
        }
        pwd?.let {
            val newPassword = pwd.text
            //--- если пароль не менялся, лишний раз перешифровывать не будем
            if (newPassword != oldPassword) {
                pwd.text = encodePassword(newPassword)
            }
        }
    }

    fun loadUserProperies(conn: CoreAdvancedConnection, userId: Int): MutableMap<String, String> {
        val hmUserProperty = mutableMapOf<String, String>()

        val rs = conn.executeQuery(" SELECT property_name , property_value FROM SYSTEM_user_property WHERE user_id = $userId ")
        while (rs.next()) {
            hmUserProperty[rs.getString(1).trim()] = rs.getString(2).trim()
        }
        rs.close()

        return hmUserProperty
    }

    private fun reloadUserNames(): Pair<Map<Int, String>, Map<Int, String>> {
        val hmFullName = mutableMapOf<Int, String>()
        val hmShortName = mutableMapOf<Int, String>()

        val userEntities = userRepository.findAll()
        userEntities.forEach { userEntity ->
            if (userEntity.id != 0) {
                hmFullName[userEntity.id] = userEntity.fullName
                hmShortName[userEntity.id] = userEntity.shortName
            }
        }

        return hmFullName to hmShortName
    }

    private fun loadUserPermission(conn: CoreAdvancedConnection, userConfig: UserConfig) {
        val userPermissionEnable = mutableMapOf<String, MutableSet<String>>()
        val userPermissionDisable = mutableMapOf<String, MutableSet<String>>()

        //--- загрузить права доступа для этого пользователя
        val rs = conn.executeQuery(
            """
                        SELECT SYSTEM_alias.name , SYSTEM_permission.name , SYSTEM_role.name 
                        FROM SYSTEM_alias , SYSTEM_permission , SYSTEM_role_permission , SYSTEM_user_role , SYSTEM_role 
                        WHERE SYSTEM_user_role.user_id = ${userConfig.userId} 
                            AND SYSTEM_role_permission.permission_value = 1 
                            AND SYSTEM_role_permission.role_id = SYSTEM_user_role.role_id 
                            AND SYSTEM_permission.id = SYSTEM_role_permission.permission_id 
                            AND SYSTEM_alias.id = SYSTEM_permission.class_id 
                            AND SYSTEM_role.id = SYSTEM_user_role.role_id
                    """
        )
        //--- чтобы отрицательные роли (с символом "!" в начале названия роли) были последними в списке - для удобства удаления.
        //--- СЮРПРИЗ: PostgreSQL под Ubuntu с кодировкой ru_ru.UTF8 игнорирует знаки препинания при сортировке,
        //--- т.е. образом строки с "!" в начале могут быть где угодно и данный метод отменяется
        //" ORDER BY SYSTEM_role.name DESC ")
        while (rs.next()) {
            val alias = rs.getString(1).trim()
            val permName = rs.getString(2).trim()
            val roleName = rs.getString(3).trim()

            if (roleName.isEmpty()) {
                continue
            }

            val hsPerm = if (roleName[0] == '!') {
                userPermissionDisable.getOrPut(alias) { mutableSetOf() }
            } else {
                userPermissionEnable.getOrPut(alias) { mutableSetOf() }
            }
            hsPerm += permName
        }
        rs.close()

        userConfig.userPermission.clear()
        //--- добавляем разрешительные права
        userConfig.userPermission.putAll(userPermissionEnable)
        //--- удаляем/вычитаем запретительные права
        for ((alias, hsPerm) in userPermissionDisable) {
            userConfig.userPermission[alias]?.minus(hsPerm)
        }
    }

    //--- загрузка userID других пользователей относительно положения с данным пользователем
    private fun loadUserIDList(conn: CoreAdvancedConnection, userConfig: UserConfig) {
        //--- список всех пользователей,
        //--- из которого путем последовательного исключения основных категорий пользователей
        //--- образуется список пользователей категории "все остальные"
        val hsOtherUsers = loadSubUserList(conn, 0)
        //--- добавить к списку пользователей псевдопользователя с userID == 0
        //--- (чтобы потом правильно обрабатывать (унаследованные) "ничьи" записи)
        hsOtherUsers.add(0)

        //--- ничейное (userId == 0)
        val hsNobodyUserIds = setOf(0)
        hsOtherUsers -= hsNobodyUserIds
        userConfig.hmRelationUser[UserRelation.NOBODY] = hsNobodyUserIds

        //--- свой userId
        val hsSelfUserIds = setOf(userConfig.userId)
        hsOtherUsers -= hsSelfUserIds
        userConfig.hmRelationUser[UserRelation.SELF] = hsSelfUserIds

        //--- userId коллег одного уровня в одном подразделении
        val hsEqualUserIds = loadUserIdList(conn, userConfig.parentId, userConfig.orgType)
        hsOtherUsers -= hsEqualUserIds
        userConfig.hmRelationUser[UserRelation.EQUAL] = hsEqualUserIds

        //--- userId начальников
        val hsBossUserIds = mutableSetOf<Int>()
        var pId = if (userConfig.orgType == OrgType.ORG_TYPE_WORKER) {
            userConfig.parentId
        } else if (userConfig.parentId != 0) {
            getUserParentId(conn, userConfig.parentId)
        } else {
            0
        }
        hsBossUserIds += loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        while (pId != 0) {
            pId = getUserParentId(conn, pId)
            hsBossUserIds += loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        }
        hsOtherUsers -= hsBossUserIds
        userConfig.hmRelationUser[UserRelation.BOSS] = hsBossUserIds

        //--- userId подчиненных
        val hsWorkerUserIds = mutableSetOf<Int>()
        if (userConfig.orgType == OrgType.ORG_TYPE_BOSS) {
            //--- на своем уровне
            hsWorkerUserIds += loadUserIdList(conn, userConfig.parentId, OrgType.ORG_TYPE_WORKER)
            //--- начальники подчиненных подразделений также являются прямыми подчиненными
            val alDivisionList = loadUserIdList(conn, userConfig.parentId, OrgType.ORG_TYPE_DIVISION).toMutableList()
            //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
            var i = 0
            while (i < alDivisionList.size) {
                val bpId = alDivisionList[i]
                hsWorkerUserIds += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_BOSS)
                hsWorkerUserIds += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_WORKER)

                alDivisionList += loadUserIdList(conn, bpId, OrgType.ORG_TYPE_DIVISION)
                i++
            }
        }
        hsOtherUsers -= hsWorkerUserIds
        userConfig.hmRelationUser[UserRelation.WORKER] = hsWorkerUserIds

        userConfig.hmRelationUser[UserRelation.OTHER] = hsOtherUsers
    }

    private fun loadSubUserList(conn: CoreAdvancedConnection, startPID: Int): MutableSet<Int> {
        val hsUser = mutableSetOf<Int>()

        val alDivisionList = mutableListOf(startPID)

        //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
        var i = 0
        while (i < alDivisionList.size) {
            val pID = alDivisionList[i]
            hsUser += loadUserIdList(conn, pID, OrgType.ORG_TYPE_BOSS)
            hsUser += loadUserIdList(conn, pID, OrgType.ORG_TYPE_WORKER)

            alDivisionList += loadUserIdList(conn, pID, OrgType.ORG_TYPE_DIVISION)
            i++
        }
        return hsUser
    }

    private fun getUserParentId(conn: CoreAdvancedConnection, userId: Int): Int {
        var parentId = 0

        val rs = conn.executeQuery(" SELECT parent_id FROM SYSTEM_users WHERE id = $userId ")
        if (rs.next()) {
            parentId = rs.getInt(1)
        }
        rs.close()

        return parentId
    }

    private fun loadAdminRoles(conn: CoreAdvancedConnection, userId: Int): Pair<Boolean, Boolean> {
        var isAdmin = false
        var roleCount = 0

        //--- загрузить список ролей пользователя
        val rs = conn.executeQuery(" SELECT role_id FROM SYSTEM_user_role WHERE user_id = $userId ")
        while (rs.next()) {
            val roleId = rs.getInt(1)
//                    if (roleId == ROLE_GUEST) {
//                        isGuest = true
//                    }
            if (roleId == CoreAppController.ROLE_ADMIN) {
                isAdmin = true
            }
            roleCount++
        }
        rs.close()

        return Pair(isAdmin, isAdmin && roleCount == 1)
    }

//    override fun getUserDTO(userId: Int): UserDTO {
//        val userEntity = userRepository.findByIdOrNull(userId) ?: "User not exist for user_id = $userId".let {
//            AdvancedLogger.error(it)
//            throw Exception(it)
//        }
//
//        return UserDTO(
//            id = userEntity.id,
//            parentId = userEntity.parentId,
//            userId = userEntity.userId,
//            isDisabled = userEntity.isDisabled != 0,
//            orgType = userEntity.orgType,
//            login = userEntity.login,
//            password = userEntity.password,
//            fullName = userEntity.fullName,
//            shortName = userEntity.shortName,
//            atCount = userEntity.atCount,
//            lastLoginDateTime = arrayOf(
//                userEntity.lastLoginDateTime.ye,
//                userEntity.lastLoginDateTime.mo,
//                userEntity.lastLoginDateTime.da,
//                userEntity.lastLoginDateTime.ho,
//                userEntity.lastLoginDateTime.mi,
//                0
//            ),
//            passwordLastChangeDate = arrayOf(
//                userEntity.passwordLastChangeDate.ye,
//                userEntity.passwordLastChangeDate.mo,
//                userEntity.passwordLastChangeDate.da,
//                0,
//                0,
//                0
//            ),
//            eMail = userEntity.eMail,
//            contactInfo = userEntity.contactInfo,
//            fileId = userEntity.fileId,
//            lastIP = userEntity.lastIP,
//        )
//    }
//

}
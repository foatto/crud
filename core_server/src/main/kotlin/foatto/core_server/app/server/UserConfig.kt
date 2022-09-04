package foatto.core_server.app.server

import foatto.core.app.UP_DECIMAL_DIVIDER
import foatto.core.app.UP_IS_USE_THOUSANDS_DIVIDER
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.core_server.app.iApplication
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId

class UserConfig private constructor(
    val userId: Int,
    val parentId: Int,
    val orgType: Int,
    //--- принадлежность пользователя к предопределённым ролям
    val isAdmin: Boolean,
    //--- позволяет отличить "чистого" админа от "частично админов" - монтажников/наладчиков и т.п.
    val isCleanAdmin: Boolean,
    val hmUserProperty: MutableMap<String, String>,
) : Cloneable {

    companion object {
        //--- предопреденные userID
        const val USER_GUEST = -1

        fun getConfig(application: iApplication, conn: CoreAdvancedConnection, userId: Int): UserConfig {
            val stm = conn.createStatement()

            val (isAdmin, isCleanAdmin) = application.loadAdminRoles(conn, userId)

            //--- первичная загрузка данных
            val rs = stm.executeQuery(" SELECT id , parent_id , org_type FROM SYSTEM_users WHERE id = $userId ")
            rs.next()
            val uc = UserConfig(
                userId = rs.getInt(1),
                parentId = rs.getInt(2),
                orgType = rs.getInt(3),
                isAdmin = isAdmin,
                isCleanAdmin = isCleanAdmin,
                hmUserProperty = application.loadUserProperies(conn, userId),
            )
            rs.close()

            //--- вторичная загрузка данных
            uc.loadUserPermission(stm)
            uc.loadUserIDList(application, conn)

            stm.close()
            return uc
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    var eMail: String = "" - not used

    //--- список прав доступа пользователя
    val userPermission = mutableMapOf<String, Set<String>>()

    //--- списки пользователей по отношениям
    private val hmRelationUser = mutableMapOf<String, Set<Int>>()

    val upTimeOffset: Int
        get() = hmUserProperty[UP_TIME_OFFSET]?.toIntOrNull() ?: 0
    val upZoneId: ZoneId
        get() = getZoneId(hmUserProperty[UP_TIME_OFFSET]?.toIntOrNull())

    val upIsUseThousandsDivider: Boolean
        get() = hmUserProperty[UP_IS_USE_THOUSANDS_DIVIDER]?.toBooleanStrictOrNull() ?: true

    val upDecimalDivider: Char
        get() = hmUserProperty[UP_DECIMAL_DIVIDER]?.first() ?: '.'

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getUserIDList(relation: String): Set<Int> = hmRelationUser[relation]!!

    fun getUserProperty(name: String): String? = hmUserProperty[name]

    //--- сохранение дополнительных параметров пользователя
    fun saveUserProperty(conn: CoreAdvancedConnection, upName: String, upValue: String) {
        val stm = conn.createStatement()

        if (stm.executeUpdate(" UPDATE SYSTEM_user_property SET property_value = '$upValue' WHERE user_id = $userId AND property_name = '$upName' ") == 0) {
            stm.executeUpdate(" INSERT INTO SYSTEM_user_property ( user_id , property_name , property_value ) VALUES ( $userId , '$upName' , '$upValue' ) ")
        }

        stm.close()

        hmUserProperty[upName] = upValue
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun loadUserPermission(stm: CoreAdvancedStatement) {
        val userPermissionEnable = mutableMapOf<String, MutableSet<String>>()
        val userPermissionDisable = mutableMapOf<String, MutableSet<String>>()

        //--- загрузить права доступа для этого пользователя
        val rs = stm.executeQuery(
            " SELECT SYSTEM_alias.name , SYSTEM_permission.name , SYSTEM_role.name " +
                " FROM SYSTEM_alias , SYSTEM_permission , SYSTEM_role_permission , SYSTEM_user_role , SYSTEM_role " +
                " WHERE SYSTEM_user_role.user_id = $userId " +
                " AND SYSTEM_role_permission.permission_value = 1 " +
                " AND SYSTEM_role_permission.role_id = SYSTEM_user_role.role_id " +
                " AND SYSTEM_permission.id = SYSTEM_role_permission.permission_id " +
                " AND SYSTEM_alias.id = SYSTEM_permission.class_id " +
                " AND SYSTEM_role.id = SYSTEM_user_role.role_id "
        )
        //--- чтобы отрицательные роли (с символом "!" в начале названия роли) были последними в списке - для удобства удаления.
        //--- СЮРПРИЗ: PostgreSQL под Ubuntu с кодировкой ru_ru.UTF8 игнорирует знаки препинания при сортировке,
        //--- т.е. образом строки с "!" в начале могут быть где угодно и данный метод отменяется
        //" ORDER BY SYSTEM_role.name DESC " )
        while (rs.next()) {
            val alias = rs.getString(1).trim()
            val permName = rs.getString(2).trim()
            val roleName = rs.getString(3).trim()

            if (roleName.isEmpty()) continue

            var hsPerm: MutableSet<String>?
            if (roleName[0] == '!') {
                hsPerm = userPermissionDisable[alias]
                if (hsPerm == null) {
                    hsPerm = mutableSetOf()
                    userPermissionDisable[alias] = hsPerm
                }
            } else {
                hsPerm = userPermissionEnable[alias]
                if (hsPerm == null) {
                    hsPerm = mutableSetOf()
                    userPermissionEnable[alias] = hsPerm
                }
            }
            hsPerm.add(permName)
        }
        rs.close()

        userPermission.clear()
        //--- добавляем разрешительные права
        userPermission.putAll(userPermissionEnable)
        //--- удаляем/вычитаем запретительные права
        for ((alias, hsPerm) in userPermissionDisable) userPermission[alias]?.minus(hsPerm)
    }

    //--- загрузка userID других пользователей относительно положения с данным пользователем
    private fun loadUserIDList(application: iApplication, conn: CoreAdvancedConnection) {
        //--- список всех пользователей,
        //--- из которого путем последовательного исключения основных категорий пользователей
        //--- образуется список пользователей категории "все остальные"
        val hsOtherUsers = loadSubUserList(application, conn, 0)
        //--- добавить к списку пользователей псевдопользователя с userID == 0
        //--- (чтобы потом правильно обрабатывать (унаследованные) "ничьи" записи)
        hsOtherUsers.add(0)

        //--- ничейное (userId == 0)
        val hsNobodyUserIds = setOf(0)
        hsOtherUsers -= hsNobodyUserIds
        hmRelationUser[UserRelation.NOBODY] = hsNobodyUserIds

        //--- свой userId
        val hsSelfUserIds = setOf(userId)
        hsOtherUsers -= hsSelfUserIds
        hmRelationUser[UserRelation.SELF] = hsSelfUserIds

        //--- userId коллег одного уровня в одном подразделении
        val hsEqualUserIds = application.loadUserIdList(conn, parentId, orgType)
        hsOtherUsers -= hsEqualUserIds
        hmRelationUser[UserRelation.EQUAL] = hsEqualUserIds

        //--- userId начальников
        val hsBossUserIds = mutableSetOf<Int>()
        var pId = if (orgType == OrgType.ORG_TYPE_WORKER) {
            parentId
        } else if (parentId != 0) {
            application.getUserParentId(conn, parentId)
        } else {
            0
        }
        hsBossUserIds += application.loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        while (pId != 0) {
            pId = application.getUserParentId(conn, pId)
            hsBossUserIds += application.loadUserIdList(conn, pId, OrgType.ORG_TYPE_BOSS)
        }
        hsOtherUsers -= hsBossUserIds
        hmRelationUser[UserRelation.BOSS] = hsBossUserIds

        //--- userId подчиненных
        val hsWorkerUserIds = mutableSetOf<Int>()
        if (orgType == OrgType.ORG_TYPE_BOSS) {
            //--- на своем уровне
            hsWorkerUserIds += application.loadUserIdList(conn, parentId, OrgType.ORG_TYPE_WORKER)
            //--- начальники подчиненных подразделений также являются прямыми подчиненными
            val alDivisionList = application.loadUserIdList(conn, parentId, OrgType.ORG_TYPE_DIVISION).toMutableList()
            //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
            var i = 0
            while (i < alDivisionList.size) {
                val bpId = alDivisionList[i]
                hsWorkerUserIds += application.loadUserIdList(conn, bpId, OrgType.ORG_TYPE_BOSS)
                hsWorkerUserIds += application.loadUserIdList(conn, bpId, OrgType.ORG_TYPE_WORKER)

                alDivisionList += application.loadUserIdList(conn, bpId, OrgType.ORG_TYPE_DIVISION)
                i++
            }
        }
        hsOtherUsers -= hsWorkerUserIds
        hmRelationUser[UserRelation.WORKER] = hsWorkerUserIds

        hmRelationUser[UserRelation.OTHER] = hsOtherUsers
    }

    private fun loadSubUserList(application: iApplication, conn: CoreAdvancedConnection, startPID: Int): MutableSet<Int> {
        val hsUser = mutableSetOf<Int>()

        val alDivisionList = mutableListOf(startPID)

        //--- именно через отдельный индекс, т.к. alDivisionList пополняется в процессе прохода
        var i = 0
        while (i < alDivisionList.size) {
            val pID = alDivisionList[i]
            hsUser += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_BOSS)
            hsUser += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_WORKER)

            alDivisionList += application.loadUserIdList(conn, pID, OrgType.ORG_TYPE_DIVISION)
            i++
        }
        return hsUser
    }

}

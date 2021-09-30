package foatto.core_server.app.server

import foatto.core.app.UP_DECIMAL_DIVIDER
import foatto.core.app.UP_IS_USE_THOUSANDS_DIVIDER
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedResultSet
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId

class UserConfig private constructor(
    rs: CoreAdvancedResultSet,
) : Cloneable {

    companion object {
        //--- предопреденные userID
        const val USER_GUEST = -1

        //--- предопределенные roleID
        val ROLE_GUEST = -1
        val ROLE_ADMIN = -2

        lateinit var hmUserFullNames: Map<Int, String>
        lateinit var hmUserShortNames: Map<Int, String>

        fun getConfig(conn: CoreAdvancedConnection): Map<Int, UserConfig> {
            val stm = conn.createStatement()

            //--- загружаем оющий для всех список полных и сокращённых имён пользователей
            loadUserName(stm)

            //--- первичная загрузка данных
            val hmResult = mutableMapOf<Int, UserConfig>()

            val rs = stm.executeQuery(" SELECT id , parent_id , org_type , e_mail FROM SYSTEM_users WHERE id <> 0 ")
            while (rs.next()) {
                val uc = UserConfig(rs)
                hmResult[uc.userId] = uc
            }
            rs.close()

            //--- вторичная загруза данных
            for (uc in hmResult.values) {
                uc.loadRole(stm)
                uc.loadUserPermission(stm)
                uc.loadUserIDList(stm)
                uc.loadUserProperty(stm)
            }

            stm.close()
            return hmResult
        }

        fun getConfig(conn: CoreAdvancedConnection, id: Int): UserConfig {
            val stm = conn.createStatement()

            //--- загружаем общий для всех список полных и сокращённых имён пользователей
            loadUserName(stm)

            //--- первичная загрузка данных
            val rs = stm.executeQuery(" SELECT id , parent_id , org_type , e_mail FROM SYSTEM_users WHERE id = $id ")
            rs.next()
            val uc = UserConfig(rs)
            rs.close()

            //--- вторичная загрузка данных
            uc.loadRole(stm)
            uc.loadUserPermission(stm)
            uc.loadUserIDList(stm)
            uc.loadUserProperty(stm)

            stm.close()
            return uc
        }

        private fun loadUserName(stm: CoreAdvancedStatement) {
            val hmFullName = mutableMapOf<Int, String>()
            val hmShortName = mutableMapOf<Int, String>()

            val rs = stm.executeQuery(" SELECT id , full_name , short_name FROM SYSTEM_users WHERE id <> 0 ")
            while (rs.next()) {
                val id = rs.getInt(1)
                hmFullName[id] = rs.getString(2).trim()
                hmShortName[id] = rs.getString(3).trim()
            }
            rs.close()

            hmUserFullNames = hmFullName.toMap()
            hmUserShortNames = hmShortName.toMap()
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var userId = 0
    private var parentID = 0
    var orgType = 0

    var eMail: String = ""

    //--- принадлежность пользователя к предопределённым ролям
    var isGuest = false
        private set
    var isAdmin = false
        private set

    //--- кол-во ролей у пользователя - позволяет отличить "чистого" админа от "частично админов" - монтажников/наладчиков и т.п.
    var roleCount = 0
        private set

    //--- список прав доступа пользователя
    val userPermission = mutableMapOf<String, Set<String>>()

    //--- списки пользователей по отношениям
    private val hmRelationUser = mutableMapOf<String, Set<Int>>()

    //--- список дополнительных параметров/св-в пользователя
    val hmUserProperty = mutableMapOf<String, String>()

    val upTimeOffset: Int
        get() = hmUserProperty[UP_TIME_OFFSET]?.toIntOrNull() ?: 0
    val upZoneId: ZoneId
        get() = getZoneId(hmUserProperty[UP_TIME_OFFSET]?.toIntOrNull())

    val upIsUseThousandsDivider: Boolean
        get() = hmUserProperty[UP_IS_USE_THOUSANDS_DIVIDER]?.toBooleanStrictOrNull() ?: true

    val upDecimalDivider: Char
        get() = hmUserProperty[UP_DECIMAL_DIVIDER]?.first() ?: '.'

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    init {
        userId = rs.getInt(1)
        parentID = rs.getInt(2)
        orgType = rs.getInt(3)
        eMail = rs.getString(4)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getUserIDList(relation: String): Set<Int> = hmRelationUser[relation]!!

    //--- загрузка дополнительных параметров пользователя
    private fun loadUserProperty(stm: CoreAdvancedStatement) {
        val rs = stm.executeQuery(" SELECT property_name , property_value FROM SYSTEM_user_property WHERE user_id = $userId ")
        while (rs.next()) setUserProperty(rs.getString(1), rs.getString(2))
        rs.close()
    }

    fun getUserProperty(name: String): String? = hmUserProperty[name]

    //--- сохранение дополнительных параметров пользователя
    fun saveUserProperty(conn: CoreAdvancedConnection, upName: String, upValue: String) {
        val stm = conn.createStatement()

        if (stm.executeUpdate(" UPDATE SYSTEM_user_property SET property_value = '$upValue' WHERE user_id = $userId AND property_name = '$upName' ") == 0) {
            stm.executeUpdate(" INSERT INTO SYSTEM_user_property ( user_id , property_name , property_value ) VALUES ( $userId , '$upName' , '$upValue' ) ")
        }

        setUserProperty(upName, upValue)
        stm.close()
    }

    private fun setUserProperty(name: String, value: String) {
        hmUserProperty[name] = value
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun loadFullSubUserList(conn: CoreAdvancedConnection, startPID: Int): Set<Int> {
        val stm = conn.createStatement()

        val hsUser = mutableSetOf<Int>()
        hsUser.add(startPID)
        val alDivisionList = mutableListOf<Int>()
        alDivisionList.add(startPID)
        //--- именно через .indices, т.к. alDivisionList пополняется в процессе прохода
        for (i in alDivisionList.indices) {
            val pID = alDivisionList[i]
            loadIDList(stm, pID, OrgType.ORG_TYPE_DIVISION, hsUser)
            loadIDList(stm, pID, OrgType.ORG_TYPE_BOSS, hsUser)
            loadIDList(stm, pID, OrgType.ORG_TYPE_WORKER, hsUser)
            loadDivisionList(stm, pID, alDivisionList)
        }

        stm.close()
        return hsUser
    }

    private fun loadRole(stm: CoreAdvancedStatement) {
        isGuest = false
        isAdmin = false
        roleCount = 0
        //--- загрузить список ролей пользователя
        val rs = stm.executeQuery(" SELECT role_id FROM SYSTEM_user_role WHERE user_id = $userId ")
        while (rs.next()) {
            val roleID = rs.getInt(1)
            if (roleID == ROLE_GUEST) isGuest = true
            if (roleID == ROLE_ADMIN) isAdmin = true
            roleCount++
        }
        rs.close()
    }

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
    private fun loadUserIDList(stm: CoreAdvancedStatement) {
        //--- список всех пользователей,
        //--- из которого путем последовательного исключения основных категорий пользователей
        //--- образуется список пользователей категории "все остальные"
        val hsOtherUser = loadSubUserList(stm, 0)
        //--- добавить к списку пользователей псевдопользователя с userID == 0
        //--- (чтобы потом правильно обрабатывать (унаследованные) "ничьи" записи)
        hsOtherUser.add(0)

        var hsUserID: MutableSet<Int>

        //--- ничейное (userID == 0)
        hsUserID = mutableSetOf()
        hsUserID.add(0)
        hsOtherUser.removeAll(hsUserID)
        hmRelationUser[UserRelation.NOBODY] = hsUserID

        //--- свой userID
        hsUserID = mutableSetOf()
        hsUserID.add(userId)
        hsOtherUser.removeAll(hsUserID)
        hmRelationUser[UserRelation.SELF] = hsUserID

        //--- userID коллег одного уровня в одном подразделении
        hsUserID = mutableSetOf()
        loadIDList(stm, parentID, orgType, hsUserID)
        hsOtherUser.removeAll(hsUserID)
        hmRelationUser[UserRelation.EQUAL] = hsUserID

        //--- userID начальников
        hsUserID = mutableSetOf()
        var pID = 0
        if (orgType == OrgType.ORG_TYPE_WORKER) pID = parentID
        else if (parentID != 0) pID = getParentID(stm, parentID)
        loadIDList(stm, pID, OrgType.ORG_TYPE_BOSS, hsUserID)
        while (pID != 0) {
            pID = getParentID(stm, pID)
            loadIDList(stm, pID, OrgType.ORG_TYPE_BOSS, hsUserID)
        }
        hsOtherUser.removeAll(hsUserID)
        hmRelationUser[UserRelation.BOSS] = hsUserID

        //--- userID подчиненных
        if (orgType == OrgType.ORG_TYPE_BOSS) {
            hsUserID = mutableSetOf()
            //--- на своем уровне
            loadIDList(stm, parentID, OrgType.ORG_TYPE_WORKER, hsUserID)
            //--- начальники подчиненных подразделений также являются прямыми подчиненными
            val alDivisionList = mutableListOf<Int>()
            loadDivisionList(stm, parentID, alDivisionList)
            //--- именно через .indices, т.к. alDivisionList пополняется в процессе прохода
            for (i in alDivisionList.indices) {
                val bpID = alDivisionList[i]
                loadIDList(stm, bpID, OrgType.ORG_TYPE_BOSS, hsUserID)
                loadIDList(stm, bpID, OrgType.ORG_TYPE_WORKER, hsUserID)
                loadDivisionList(stm, bpID, alDivisionList)
            }
            hsOtherUser.removeAll(hsUserID)
            hmRelationUser[UserRelation.WORKER] = hsUserID
        } else hmRelationUser[UserRelation.WORKER] = mutableSetOf()

        hmRelationUser[UserRelation.OTHER] = hsOtherUser
    }

    private fun loadSubUserList(stm: CoreAdvancedStatement, startPID: Int): MutableSet<Int> {
        val hsUser = mutableSetOf<Int>()
        val alDivisionList = mutableListOf<Int>()
        alDivisionList.add(startPID)
        //--- именно через .indices, т.к. alDivisionList пополняется в процессе прохода
        for (i in alDivisionList.indices) {
            val pID = alDivisionList[i]
            loadIDList(stm, pID, OrgType.ORG_TYPE_BOSS, hsUser)
            loadIDList(stm, pID, OrgType.ORG_TYPE_WORKER, hsUser)
            loadDivisionList(stm, pID, alDivisionList)
        }
        return hsUser
    }

    private fun loadIDList(stm: CoreAdvancedStatement, pID: Int, ot: Int, hsUserID: MutableSet<Int>) {
        val rs = stm.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = $pID AND org_type = $ot ")
        while (rs.next()) hsUserID.add(rs.getInt(1))
        rs.close()
    }

    private fun loadDivisionList(stm: CoreAdvancedStatement, pID: Int, alDivisionList: MutableList<Int>) {
        val rs = stm.executeQuery(" SELECT id FROM SYSTEM_users WHERE id <> 0 AND parent_id = $pID AND org_type = ${OrgType.ORG_TYPE_DIVISION} ")
        while (rs.next()) alDivisionList.add(rs.getInt(1))
        rs.close()
    }

    private fun getParentID(stm: CoreAdvancedStatement, uID: Int): Int {
        var pID = 0
        val rs = stm.executeQuery(" SELECT parent_id FROM SYSTEM_users WHERE id = $uID ")
        if (rs.next()) pID = rs.getInt(1)
        rs.close()
        return pID
    }

}

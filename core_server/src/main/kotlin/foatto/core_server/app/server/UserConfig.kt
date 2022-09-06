package foatto.core_server.app.server

import foatto.core.app.UP_DECIMAL_DIVIDER
import foatto.core.app.UP_IS_USE_THOUSANDS_DIVIDER
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.util.getZoneId
import java.time.ZoneId

class UserConfig(
    val userId: Int,
    val parentId: Int,
    val orgType: Int,
    //--- принадлежность пользователя к предопределённым ролям
    val isAdmin: Boolean,
    //--- позволяет отличить "чистого" админа от "частично админов" - монтажников/наладчиков и т.п.
    val isCleanAdmin: Boolean,
    val hmUserProperty: MutableMap<String, String>,
) {

    companion object {
        //--- предопреденные userID
        const val USER_GUEST = -1
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    var eMail: String = "" - not used

    //--- список прав доступа пользователя
    val userPermission = mutableMapOf<String, Set<String>>()

    //--- списки пользователей по отношениям
    val hmRelationUser = mutableMapOf<String, Set<Int>>()

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

}

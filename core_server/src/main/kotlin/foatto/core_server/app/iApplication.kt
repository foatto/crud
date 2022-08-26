package foatto.core_server.app

import foatto.sql.CoreAdvancedConnection
import foatto.sql.CoreAdvancedStatement

interface iApplication {

    val rootDirName: String
    val tempDirName: String

    val alClientAlias: Array<String>
    val alClientParentId: Array<String>
    val alClientRoleId: Array<String>

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    val hmAliasLogDir: MutableMap<String, String>

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun getFileList(conn: CoreAdvancedConnection, fileId: Int): List<Pair<Int, String>>
    fun saveFile(stm: CoreAdvancedStatement, fileId: Int, idFromClient: Int, fileName: String)
    fun deleteFile(stm: CoreAdvancedStatement, fileId: Int, id: Int? = null)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var hmUserFullNames: Map<Int, String>
    var hmUserShortNames: Map<Int, String>

    fun reloadUserNames(conn: CoreAdvancedConnection)
    fun loadUserProperies(conn: CoreAdvancedConnection, userId: Int): MutableMap<String, String>
    fun loadAdminRoles(conn: CoreAdvancedConnection, userId: Int): Pair<Boolean, Boolean>

//    fun getUserDTO(userId: Int): UserDTO

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        const val USER_CONFIG = "user_config"
    }
}
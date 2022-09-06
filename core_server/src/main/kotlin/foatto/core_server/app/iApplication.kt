package foatto.core_server.app

import foatto.core_server.app.server.UserConfig
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
    fun loadUserIdList(conn: CoreAdvancedConnection, parentId: Int, orgType: Int): Set<Int>
    fun getUserConfig(conn: CoreAdvancedConnection, userId: Int): UserConfig
    fun saveUserProperty(conn: CoreAdvancedConnection, userConfig: UserConfig, upName: String, upValue: String)

//    fun getUserDTO(userId: Int): UserDTO

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        const val USER_CONFIG = "user_config"
    }
}
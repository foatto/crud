package foatto.core_server.app

import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.core_server.app.server.data.DataString
import foatto.sql.CoreAdvancedConnection

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
    fun saveFile(conn: CoreAdvancedConnection, fileId: Int, idFromClient: Int, fileName: String)
    fun deleteFile(conn: CoreAdvancedConnection, fileId: Int, id: Int? = null)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    var hmUserFullNames: Map<Int, String>
    var hmUserShortNames: Map<Int, String>

    fun loadUserIdList(conn: CoreAdvancedConnection, parentId: Int, orgType: Int): Set<Int>
    fun getUserConfig(conn: CoreAdvancedConnection, userId: Int): UserConfig
    fun saveUserProperty(conn: CoreAdvancedConnection, userId: Int?, userConfig: UserConfig?, upName: String, upValue: String)
    fun getUserNameColor(isDisabled: Boolean, lastLogonTime: Int): Int
    fun checkAndSetNewPassword(conn: CoreAdvancedConnection, id: Int, pwd: DataString?)

    fun getAliasConfig(conn: CoreAdvancedConnection, aliasId: Int? = null, aliasName: String? = null): Map<String, AliasConfig>

//    fun getUserDTO(userId: Int): UserDTO

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        const val USER_CONFIG = "user_config"
    }
}
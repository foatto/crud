package foatto.core_server.app

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

    fun getFileList(stm: CoreAdvancedStatement, fileId: Int): List<Pair<Int, String>>
    fun saveFile(stm: CoreAdvancedStatement, fileId: Int, idFromClient: Int, fileName: String)
    fun deleteFile(stm: CoreAdvancedStatement, fileId: Int, id: Int? = null)

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    companion object {
        const val USER_CONFIG = "user_config"
    }
}
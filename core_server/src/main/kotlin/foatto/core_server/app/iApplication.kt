package foatto.core_server.app

interface iApplication {

    val rootDirName: String
    val tempDirName: String

    val alClientAlias: Array<String>
    val alClientParentId: Array<String>
    val alClientRoleId: Array<String>

    val hmAliasLogDir: MutableMap<String, String>

    companion object {
        const val USER_CONFIG = "user_config"
    }
}
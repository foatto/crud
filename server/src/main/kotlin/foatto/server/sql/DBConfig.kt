package foatto.server.sql

class DBConfig(
    val url: String,
    val login: String,
    val password: String,
) {

    companion object {
        private const val CONFIG_DB_URL_ = "db_url_"
        private const val CONFIG_DB_LOGIN_ = "db_login_"
        private const val CONFIG_DB_PASSWORD_ = "db_password_"

        fun loadConfig(hmConfig: Map<String, String>): List<DBConfig> {
            val alDBConfig = mutableListOf<DBConfig>()

            var index = 0
            while (true) {
                val dbUrl = hmConfig[CONFIG_DB_URL_ + index] ?: break

                alDBConfig.add(
                    DBConfig(
                        dbUrl,
                        hmConfig[CONFIG_DB_LOGIN_ + index]!!,
                        hmConfig[CONFIG_DB_PASSWORD_ + index]!!,
                    )
                )
                index++
            }

            return alDBConfig
        }
    }

}

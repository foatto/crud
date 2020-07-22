package foatto.sql

class DBConfig( val name: String, val url: String, val login: String, val password: String,
                val replName: String?, val replFilter: String?, val replPath: String ) {

    companion object {
        private val CONFIG_DB_NAME_ = "db_name_"
        private val CONFIG_DB_URL_ = "db_url_"
        private val CONFIG_DB_LOGIN_ = "db_login_"
        private val CONFIG_DB_PASSWORD_ = "db_password_"
        private val CONFIG_DB_REPLICATION_NAME_ = "db_replication_name_"
        private val CONFIG_DB_REPLICATION_FILTER_ = "db_replication_filter_"
        private val CONFIG_DB_REPLICATION_PATH_ = "db_replication_path_"

        fun loadConfig( hmConfig: Map<String, String>) : List<DBConfig> {
            val alDBConfig = mutableListOf<DBConfig>()

            var index = 0
            while( true ) {
                val dbName = hmConfig[ CONFIG_DB_NAME_ + index ] ?: break

                alDBConfig.add(
                    DBConfig(
                        dbName,
                        hmConfig[CONFIG_DB_URL_ + index]!!,
                        hmConfig[CONFIG_DB_LOGIN_ + index]!!,
                        hmConfig[CONFIG_DB_PASSWORD_ + index]!!,
                        hmConfig[CONFIG_DB_REPLICATION_NAME_ + index],
                        hmConfig[CONFIG_DB_REPLICATION_FILTER_ + index],
                        hmConfig[CONFIG_DB_REPLICATION_PATH_ + index]!!
                    )
                )
                index++
            }

            return alDBConfig
        }
    }

}

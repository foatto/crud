package foatto.server.model

class AppModuleConfig(
    val caption: String,
    val pageSize: Int? = null,
    val permissions: MutableMap<String, Permission>,
) {
    companion object {
        val DEFAULT_PAGE_SIZE: Int = 20
    }
}
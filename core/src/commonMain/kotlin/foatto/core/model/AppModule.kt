package foatto.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppModule(
    val appUrl: String,
    val formActionUrl: String,
) {
    NOTHING("", ""),

    USER(ApiUrls.USER, ApiUrls.USER_FORM_ACTION),

//--- MMS custom app modules

//--- TS custom app modules

}

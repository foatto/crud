package foatto.core.model.response

import foatto.core.model.AppAction
import kotlinx.serialization.Serializable

@Serializable
class TitleData(
    val action: AppAction?,
    val text: String,
    val isBold: Boolean,
)
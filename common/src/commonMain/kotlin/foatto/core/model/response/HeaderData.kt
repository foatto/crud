package foatto.core.model.response

import kotlinx.serialization.Serializable

@Serializable
class HeaderData(
    val titles: List<TitleData>,
    val rows: List<Pair<String, String>>,
)
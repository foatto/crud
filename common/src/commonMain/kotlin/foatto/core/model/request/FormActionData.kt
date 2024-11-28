package foatto.core.model.request

import kotlinx.serialization.Serializable

@Serializable
class FormActionData(
    //--- String, Number, String Key of Combo/Radio
    val stringValue: String = "",

    //--- Boolean
    val booleanValue: Boolean = false,

    //--- DateTime
    val dateTimeValue: Int? = null,

    //--- FILE
    val fileId: Int? = null,
    //-- use for passing String-to-String instead of Int/Long-to-String due to serialization problems in JSON (since field names will be obtained as numbers)
    val addFiles: Map<String, String>? = null,
    val fileRemovedIds: List<Int>? = null,
)

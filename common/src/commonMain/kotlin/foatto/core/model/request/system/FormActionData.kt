package foatto.core.model.request.system

import kotlinx.serialization.Serializable

@Serializable
class FormActionData(
    //--- String, Number, String Key of Combo/Radio
    val stringValue: String = "",

    //--- Boolean
    val booleanValue: Boolean = false,

    //--- DateTime
    val alDateTimeValue: List<String> = emptyList(),

    //--- FILE
    val fileId: Long? = null,
    //-- use for passing String-to-String instead of Int/Long-to-String due to serialization problems in JSON (since field names will be obtained as numbers)
    val addFiles: Map<String, String>? = null,
    val fileRemovedIds: List<Long>? = null,
)

package foatto.core.link

//--- all this cannot be translated into an enum class, because it is used precisely in a string form inside actionParam
object AppAction {
    const val LOGON = "logon"

    const val GRAPHIC = "graphic"
    const val XY = "xy"
//    const val VIDEO = "video"
    const val COMPOSITE = "composite"

    const val TABLE = "table"
    const val FORM = "form"
    const val FIND = "find"
    const val SAVE = "save"
    const val ARCHIVE = "archive"
    const val UNARCHIVE = "unarchive"
    const val DELETE = "delete"
}

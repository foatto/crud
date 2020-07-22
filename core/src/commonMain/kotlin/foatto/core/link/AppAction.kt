package foatto.core.link

//--- это всё нельзя перевести в enum class, потому что используется именно в строковом виде внутри actionParam
object AppAction {
    const val LOGON = "logon"

    const val GRAPHIC = "graphic"
    const val XY = "xy"
//    const val VIDEO = "video"

    const val TABLE = "table"
    const val FORM = "form"
    const val FIND = "find"
    const val SAVE = "save"
    const val ARCHIVE = "archive"
    const val UNARCHIVE = "unarchive"
    const val DELETE = "delete"
}

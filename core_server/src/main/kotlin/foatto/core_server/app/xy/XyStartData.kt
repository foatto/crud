package foatto.core_server.app.xy

class XyStartData {

    var alStartObjectData = mutableListOf<XyStartObjectData>()

    var rangeType = 0
    var begTime = 0
    var endTime = 0

    var shortTitle = ""
    var title = ""
}

class XyStartObjectData(
    val objectId: Int,
    var typeName: String = "",
    var isStart: Boolean = false,
    var isTimed: Boolean = false,
    var isReadOnly: Boolean = false
)
//{
//    constructor( aobjectId: Int ) {
//        objectId = aobjectId
//    }
//}

class XyStartObjectParsedData(val objectId: Int) {
    val hsType = mutableSetOf<String>()
    var begTime = 0
    var endTime = 0
}

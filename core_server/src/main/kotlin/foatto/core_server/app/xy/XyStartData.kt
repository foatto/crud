package foatto.core_server.app.xy

class XyStartData {

    var alStartObjectData = mutableListOf<XyStartObjectData>()

    var rangeType = 0
    var begTime = 0
    var endTime = 0

    var shortTitle = ""
    var sbTitle = StringBuilder()

    var parentObjectID = 0
    var parentObjectInfo = ""
}

class XyStartObjectData(val objectID: Int, var typeName: String = "", var isStart: Boolean = false, var isTimed: Boolean = false, var isReadOnly: Boolean = false)
//{
//    constructor( aObjectID: Int ) {
//        objectID = aObjectID
//    }
//}

class XyStartObjectParsedData(val objectID: Int) {
    val hsType = mutableSetOf<String>()
    var begTime = 0
    var endTime = 0
}

package foatto.core_server.app.xy

import foatto.core.link.ServerActionButton

class XyStartData(
    val alStartObjectData: MutableList<XyStartObjectData>,
    val rangeType: Int = 0, // для статических элементов неюзабельно
    val begTime: Int = 0,
    val endTime: Int = 0,
    var title: String = "",
    var tooltip: String = "",
    val alServerActionButton: MutableList<ServerActionButton> = mutableListOf(),
)

class XyStartObjectData(
    val objectId: Int,
    var typeName: String = "",
    var isStart: Boolean = false,
    var isTimed: Boolean = false,
    var isReadOnly: Boolean = false
)

class XyStartObjectParsedData(val objectId: Int) {
    val hsType = mutableSetOf<String>()
    var begTime = 0
    var endTime = 0
}

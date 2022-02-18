package foatto.core_server.app.composite.server

import foatto.core.link.XyServerActionButton

class CompositeStartData {
    var objectId = 0

    var rangeType = 0
    var begTime = 0
    var endTime = 0

    var shortTitle = ""
    var fullTitle = ""

    var xyStartDataId: String = ""
    var graphicStartDataId: String = ""

    var alServerActionButton = mutableListOf<XyServerActionButton>()
}

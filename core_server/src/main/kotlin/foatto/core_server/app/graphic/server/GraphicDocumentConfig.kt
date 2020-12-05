package foatto.core_server.app.graphic.server

open class GraphicDocumentConfig(
    val serverControlClassName: String
) {

    companion object {
        //--- there is no default configuration because the server class is different for everyone
        val hmConfig = mutableMapOf<String, GraphicDocumentConfig>()
    }
}

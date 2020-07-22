package foatto.core_server.app.graphic.server

open class GraphicDocumentConfig( val name: String, val serverControlClassName: String ) {

    companion object {
        //--- конфигурации по умолчанию не существует, т.к. серверный класс у всех разный
        val hmConfig = mutableMapOf<String, GraphicDocumentConfig>()
    }
}

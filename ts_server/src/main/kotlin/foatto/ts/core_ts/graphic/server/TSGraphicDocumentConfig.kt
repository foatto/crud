package foatto.ts.core_ts.graphic.server

import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.iGraphicHandler

class TSGraphicDocumentConfig(
    aServerControlClassName: String,
    val sensorType: Int,
    val graphicHandler: iGraphicHandler?
) : GraphicDocumentConfig(aServerControlClassName)
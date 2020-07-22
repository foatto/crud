package foatto.mms.core_mms.graphic.server

import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.mms.core_mms.graphic.server.graphic_handler.iGraphicHandler

class MMSGraphicDocumentConfig( aName: String, aServerControlClassName: String,
                                //String aClientControlClassName, String aClientModelClassName, String aClientViewClassName,
                                val sensorType: Int, val graphicHandler: iGraphicHandler? ) : GraphicDocumentConfig( aName, aServerControlClassName )
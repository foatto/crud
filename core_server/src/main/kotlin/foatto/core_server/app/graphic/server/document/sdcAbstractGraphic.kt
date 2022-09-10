package foatto.core_server.app.graphic.server.document

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.iApplication
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedConnection
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

abstract class sdcAbstractGraphic {

    companion object {

        val hmIndexColor = mapOf(
            GraphicColorIndex.FILL_NEUTRAL to 0xFF_E0_FF_FF.toInt(),
            GraphicColorIndex.FILL_NORMAL to 0xFF_E0_FF_E0.toInt(),
            GraphicColorIndex.FILL_WARNING to 0xFF_FF_FF_E0.toInt(),
            GraphicColorIndex.FILL_CRITICAL to 0xFF_FF_E0_E0.toInt(),

            GraphicColorIndex.BORDER_NEUTRAL to 0xFF_C0_FF_FF.toInt(),
            GraphicColorIndex.BORDER_NORMAL to 0xFF_B0_F0_B0.toInt(),
            GraphicColorIndex.BORDER_WARNING to 0xFF_E0_E0_C0.toInt(),
            GraphicColorIndex.BORDER_CRITICAL to 0xFF_FF_C0_C0.toInt(),

            GraphicColorIndex.TEXT_NEUTRAL to 0xFF_00_00_80.toInt(),
            GraphicColorIndex.TEXT_NORMAL to 0xFF_00_80_00.toInt(),
            GraphicColorIndex.TEXT_WARNING to 0xFF_80_80_00.toInt(),
            GraphicColorIndex.TEXT_CRITICAL to 0xFF_80_00_00.toInt(),

            GraphicColorIndex.POINT_NEUTRAL to 0xFF_C0_C0_FF.toInt(),
            GraphicColorIndex.POINT_NORMAL to 0xFF_C0_FF_C0.toInt(),
            GraphicColorIndex.POINT_BELOW to 0xFF_E0_E0_A0.toInt(),
            GraphicColorIndex.POINT_ABOVE to 0xFF_FF_C0_C0.toInt(),

            GraphicColorIndex.AXIS_0 to 0xFF_80_C0_80.toInt(),
            GraphicColorIndex.AXIS_1 to 0xFF_80_80_C0.toInt(),
            GraphicColorIndex.AXIS_2 to 0xFF_C0_80_80.toInt(),
            GraphicColorIndex.AXIS_3 to 0xFF_C0_80_C0.toInt(),

            GraphicColorIndex.LINE_LIMIT to 0xFF_FF_A0_A0.toInt(),

            GraphicColorIndex.LINE_NONE_0 to 0x00_80_80_80.toInt(),
            GraphicColorIndex.LINE_NORMAL_0 to 0xFF_00_E0_00.toInt(),
            GraphicColorIndex.LINE_BELOW_0 to 0xFF_00_60_E0.toInt(),
            GraphicColorIndex.LINE_ABOVE_0 to 0xFF_E0_60_00.toInt(),

            GraphicColorIndex.LINE_NONE_1 to 0x00_90_90_90.toInt(),
            GraphicColorIndex.LINE_NORMAL_1 to 0xFF_00_00_E0.toInt(),

            GraphicColorIndex.LINE_NONE_2 to 0x00_A0_A0_A0.toInt(),
            GraphicColorIndex.LINE_NORMAL_2 to 0xFF_E0_00_00.toInt(),

            GraphicColorIndex.LINE_NONE_3 to 0x00_B0_B0_B0.toInt(),
            GraphicColorIndex.LINE_NORMAL_3 to 0xFF_E0_00_E0.toInt(),
        )

        const val UP_GRAPHIC_VISIBLE = "graphic_visible_"

        //--- в худшем случае у нас как минимум 4 точки на мм ( 100 dpi ),
        //--- нет смысла выводить данные в каждый пиксель, достаточно в каждый мм
        const val DOT_PER_MM = 4
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var application: iApplication
    protected lateinit var conn: CoreAdvancedConnection
    protected lateinit var chmSession: ConcurrentHashMap<String, Any>
    protected lateinit var userConfig: UserConfig
    protected lateinit var documentTypeName: String

    protected lateinit var zoneId: ZoneId

    fun init(aApplication: iApplication, aConn: CoreAdvancedConnection, aChmSession: ConcurrentHashMap<String, Any>, aUserConfig: UserConfig, aDocumentTypeName: String) {
        application = aApplication
        conn = aConn
        chmSession = aChmSession
        //--- получить конфигурацию по подключенному пользователю
        userConfig = aUserConfig
        //--- наименование типа документа (алиас)
        documentTypeName = aDocumentTypeName

        zoneId = userConfig.upZoneId
    }

    open fun doGetCoords(startParamId: String): GraphicActionResponse {
        val begTime: Int
        val endTime: Int

        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + startParamId] as GraphicStartData

        if (sd.rangeType == 0) {
            begTime = sd.begTime
            endTime = sd.endTime
        } else {
            endTime = ZonedDateTime.now(zoneId).toEpochSecond().toInt()
            begTime = endTime - sd.rangeType
        }
        return GraphicActionResponse(begTime = begTime, endTime = endTime)
    }

    abstract fun doGetElements(graphicActionRequest: GraphicActionRequest): GraphicActionResponse

}


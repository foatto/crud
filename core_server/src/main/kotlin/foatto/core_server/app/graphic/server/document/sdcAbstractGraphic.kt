package foatto.core_server.app.graphic.server.document

import foatto.app.CoreSpringController
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.graphic.GraphicColorIndex
import foatto.core.util.getZoneId
import foatto.core_server.app.AppParameter
import foatto.core_server.app.graphic.server.GraphicStartData
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

abstract class sdcAbstractGraphic {

    companion object {

        val hmIndexColor = mutableMapOf<GraphicColorIndex, Int>()

        init {
            hmIndexColor[GraphicColorIndex.FILL_NEUTRAL] = 0xFF_E0_FF_FF.toInt()
            hmIndexColor[GraphicColorIndex.FILL_NORMAL] = 0xFF_E0_FF_E0.toInt()
            hmIndexColor[GraphicColorIndex.FILL_WARNING] = 0xFF_FF_FF_E0.toInt()
            hmIndexColor[GraphicColorIndex.FILL_CRITICAL] = 0xFF_FF_E0_E0.toInt()

            hmIndexColor[GraphicColorIndex.BORDER_NEUTRAL] = 0xFF_C0_FF_FF.toInt()
            hmIndexColor[GraphicColorIndex.BORDER_NORMAL] = 0xFF_B0_F0_B0.toInt()
            hmIndexColor[GraphicColorIndex.BORDER_WARNING] = 0xFF_E0_E0_C0.toInt()
            hmIndexColor[GraphicColorIndex.BORDER_CRITICAL] = 0xFF_FF_C0_C0.toInt()

            hmIndexColor[GraphicColorIndex.TEXT_NEUTRAL] = 0xFF_00_00_80.toInt()
            hmIndexColor[GraphicColorIndex.TEXT_NORMAL] = 0xFF_00_80_00.toInt()
            hmIndexColor[GraphicColorIndex.TEXT_WARNING] = 0xFF_80_80_00.toInt()
            hmIndexColor[GraphicColorIndex.TEXT_CRITICAL] = 0xFF_80_00_00.toInt()

            hmIndexColor[GraphicColorIndex.POINT_NEUTRAL] = 0xFF_C0_C0_FF.toInt()
            hmIndexColor[GraphicColorIndex.POINT_NORMAL] = 0xFF_C0_FF_C0.toInt()
            hmIndexColor[GraphicColorIndex.POINT_BELOW] = 0xFF_E0_E0_A0.toInt()
            hmIndexColor[GraphicColorIndex.POINT_ABOVE] = 0xFF_FF_C0_C0.toInt()

            hmIndexColor[GraphicColorIndex.AXIS_0] = 0xFF_80_C0_80.toInt()
            hmIndexColor[GraphicColorIndex.AXIS_1] = 0xFF_80_80_C0.toInt()
            hmIndexColor[GraphicColorIndex.AXIS_2] = 0xFF_C0_80_80.toInt()
            hmIndexColor[GraphicColorIndex.AXIS_3] = 0xFF_C0_80_C0.toInt()

            hmIndexColor[GraphicColorIndex.LINE_LIMIT] = 0xFF_FF_A0_A0.toInt()

            hmIndexColor[GraphicColorIndex.LINE_NONE_0] = 0xFF_80_80_80.toInt()
            hmIndexColor[GraphicColorIndex.LINE_NORMAL_0] = 0xFF_00_E0_00.toInt()
            hmIndexColor[GraphicColorIndex.LINE_BELOW_0] = 0xFF_00_60_E0.toInt()
            hmIndexColor[GraphicColorIndex.LINE_ABOVE_0] = 0xFF_E0_60_00.toInt()

            hmIndexColor[GraphicColorIndex.LINE_NONE_1] = 0xFF_90_90_90.toInt()
            hmIndexColor[GraphicColorIndex.LINE_NORMAL_1] = 0xFF_00_00_E0.toInt()

            hmIndexColor[GraphicColorIndex.LINE_NONE_2] = 0xFF_A0_A0_A0.toInt()
            hmIndexColor[GraphicColorIndex.LINE_NORMAL_2] = 0xFF_E0_00_00.toInt()

            hmIndexColor[GraphicColorIndex.LINE_NONE_3] = 0xFF_B0_B0_B0.toInt()
            hmIndexColor[GraphicColorIndex.LINE_NORMAL_3] = 0xFF_E0_00_E0.toInt()
        }

        const val UP_GRAPHIC_VISIBLE = "graphic_visible_"
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected lateinit var appController: CoreSpringController
    protected lateinit var stm: CoreAdvancedStatement
    protected lateinit var chmSession: ConcurrentHashMap<String, Any>
    protected lateinit var userConfig: UserConfig
    protected lateinit var documentTypeName: String

    protected lateinit var zoneId: ZoneId

    fun init(aAppController: CoreSpringController, aStm: CoreAdvancedStatement, aChmSession: ConcurrentHashMap<String, Any>, aUserConfig: UserConfig, aDocumentTypeName: String) {
        appController = aAppController
        stm = aStm
        chmSession = aChmSession
        //--- получить конфигурацию по подключенному пользователю
        userConfig = aUserConfig
        //--- наименование типа документа (алиас)
        documentTypeName = aDocumentTypeName

        zoneId = getZoneId(userConfig.getUserProperty(UP_TIME_OFFSET)?.toIntOrNull())
    }

    open fun doGetCoords(startParamID: String): GraphicActionResponse {
        val begTime: Int
        val endTime: Int

        val sd = chmSession[AppParameter.GRAPHIC_START_DATA + startParamID] as GraphicStartData

        if(sd.rangeType == 0) {
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


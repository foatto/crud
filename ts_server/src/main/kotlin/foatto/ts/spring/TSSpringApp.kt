package foatto.ts.spring

import foatto.core.link.XyDocumentClientType
import foatto.core.link.XyDocumentConfig
import foatto.core.link.XyElementClientType
import foatto.core.link.XyElementConfig
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.system.mUser
import foatto.spring.CoreSpringApp
import foatto.ts.core_ts.graphic.server.TSGraphicDocumentConfig
import foatto.ts.core_ts.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.ts.core_ts.sensor.config.SensorConfig
import foatto.ts.core_ts.xy.server.document.sdcTSState
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication
@EnableWebMvc
class TSSpringApp : CoreSpringApp() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<TSSpringApp>(*args)
        }

        init {
            listOf(
                "ts_graphic_depth" to SensorConfig.SENSOR_DEPTH,
                "ts_graphic_speed" to SensorConfig.SENSOR_SPEED,
                "ts_graphic_load" to SensorConfig.SENSOR_LOAD,

                "ts_graphic_temperature_in" to SensorConfig.SENSOR_TEMPERATURE_IN,
                "ts_graphic_temperature_out" to SensorConfig.SENSOR_TEMPERATURE_OUT,

            ).forEach { (name, sensorType) ->
                GraphicDocumentConfig.hmConfig[name] = TSGraphicDocumentConfig(
                    aServerControlClassName = "foatto.ts.core_ts.graphic.server.document.sdcAnalog",
                    sensorType = sensorType,
                    graphicHandler = AnalogGraphicHandler()
                )
            }
            GraphicDocumentConfig.hmConfig["ts_graphic_dsltt"] = TSGraphicDocumentConfig(
                aServerControlClassName = "foatto.ts.core_ts.graphic.server.document.sdcAnalogDSLTT",
                sensorType = 0, // не имеет значения
                graphicHandler = AnalogGraphicHandler()
            )

            mUser.alExtendChildData.add(ChildData("ts_object", true))
//            mUser.alExtendChildData.add(ChildData("ts_device_command_history"))

            mUser.alExtendDependData.add(DependData("TS_object", "user_id"))
//            mUser.alExtendDependData.add(DependData("TS_device_command_history", "user_id", DependData.SET, 0))
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    override fun addXyDocumentConfig() {
        super.addXyDocumentConfig()

        hmXyDocumentConfig["ts_state"] = XyDocumentConfig(
            name = "ts_state",
            descr = "Состояние объекта",
            serverClassName = "foatto.ts.core_ts.xy.server.document.sdcTSState",
            clientType = XyDocumentClientType.STATE,
            itScaleAlign = false,
            alElementConfig = initStateElementConfig(1, 1024 * 1024 * 1024).toList().toTypedArray()
        )
    }

    private fun initStateElementConfig(minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {

        val hmElementConfig = initXyElementConfig(level = 10, minScale = minScale, maxScale = maxScale)

        hmElementConfig[sdcTSState.TYPE_STATE_VALUE_BAR] = XyElementConfig(
            name = sdcTSState.TYPE_STATE_VALUE_BAR,
            clientType = XyElementClientType.POLY,
            layer = 10,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            itRotatable = false,
            itMoveable = false,
            itEditablePoint = false
        )

        hmElementConfig[sdcTSState.TYPE_STATE_AXIS] = XyElementConfig(
            name = sdcTSState.TYPE_STATE_AXIS,
            clientType = XyElementClientType.POLY,
            layer = 11,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            itRotatable = false,
            itMoveable = false,
            itEditablePoint = false
        )

        hmElementConfig[sdcTSState.TYPE_STATE_AXIS_POINTER] = XyElementConfig(
            name = sdcTSState.TYPE_STATE_AXIS_POINTER,
            clientType = XyElementClientType.MARKER,
            layer = 12,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            itRotatable = false,
            itMoveable = false,
            itEditablePoint = false
        )

        hmElementConfig[sdcTSState.TYPE_STATE_TEXT] = XyElementConfig(
            name = sdcTSState.TYPE_STATE_TEXT,
            clientType = XyElementClientType.TEXT,
            layer = 13,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            itRotatable = false,
            itMoveable = false,
            itEditablePoint = false
        )

        return hmElementConfig
    }
}
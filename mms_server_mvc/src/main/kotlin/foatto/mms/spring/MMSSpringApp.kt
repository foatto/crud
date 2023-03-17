package foatto.mms.spring

import foatto.core.link.XyDocumentClientType
import foatto.core.link.XyDocumentConfig
import foatto.core.link.XyElementClientType
import foatto.core.link.XyElementConfig
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.system.mUser
import foatto.mms.core_mms.graphic.server.MMSGraphicDocumentConfig
import foatto.mms.core_mms.graphic.server.graphic_handler.AnalogGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.sensor.config.SensorConfig
import foatto.mms.core_mms.xy.server.document.sdcMMSMap
import foatto.mms.core_mms.xy.server.document.sdcMMSState
import foatto.spring.CoreSpringApp
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication(
    scanBasePackages = ["foatto.spring", "foatto.mms.spring"],
)
@EnableWebMvc
class MMSSpringApp : CoreSpringApp() {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
        }

//-------------------------------------------------------------------------------------------------------------------------------

        //--- пока минимально используемый масштаб 1:16, что соответствует старому 1:2000 при 4 пикселях на мм
        //--- ограничение исходит из-за отсутствия битмапов выше 18-го уровня ( т.е. для 1:1000 ),
        //--- а 18-й уровень битмапов сооветствует масштабу 1:16
        val MAP_MIN_SCALE = 16

        //--- пока максимально используемый масштаб 1:512*1024, что соответствует старому 1:32_768_000 при 4 пикселях на мм
        val MAP_MAX_SCALE = 512 * 1024
        //--- "средний" масштаб для случаев,
        //--- когда "граничная" точка только одна и определить необходимый масштаб невозможно
        //private static final int MAP_AVG_SCALE = 512; - не видна пригодимость

        init {
            GraphicDocumentConfig.hmConfig["mms_graphic_liquid"] = MMSGraphicDocumentConfig(
                aServerControlClassName = "foatto.mms.core_mms.graphic.server.document.sdcLiquid",
                sensorType = SensorConfig.SENSOR_LIQUID_LEVEL,
                graphicHandler = LiquidGraphicHandler()
            )

            listOf(
                "mms_graphic_weight" to SensorConfig.SENSOR_WEIGHT,
                "mms_graphic_turn" to SensorConfig.SENSOR_TURN,
                "mms_graphic_pressure" to SensorConfig.SENSOR_PRESSURE,
                "mms_graphic_temperature" to SensorConfig.SENSOR_TEMPERATURE,
                "mms_graphic_voltage" to SensorConfig.SENSOR_VOLTAGE,
                "mms_graphic_power" to SensorConfig.SENSOR_POWER,
                "mms_graphic_density" to SensorConfig.SENSOR_DENSITY,
                "mms_graphic_mass_flow" to SensorConfig.SENSOR_MASS_FLOW,
                "mms_graphic_volume_flow" to SensorConfig.SENSOR_VOLUME_FLOW,

                "mms_graphic_energo_voltage" to SensorConfig.SENSOR_ENERGO_VOLTAGE,
                "mms_graphic_energo_current" to SensorConfig.SENSOR_ENERGO_CURRENT,
                "mms_graphic_energo_power_koef" to SensorConfig.SENSOR_ENERGO_POWER_KOEF,
                "mms_graphic_energo_power_active" to SensorConfig.SENSOR_ENERGO_POWER_ACTIVE,
                "mms_graphic_energo_power_reactive" to SensorConfig.SENSOR_ENERGO_POWER_REACTIVE,
                "mms_graphic_energo_power_full" to SensorConfig.SENSOR_ENERGO_POWER_FULL,
            ).forEach { (name, sensorType) ->
                GraphicDocumentConfig.hmConfig[name] = MMSGraphicDocumentConfig(
                    aServerControlClassName = "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    sensorType = sensorType,
                    graphicHandler = AnalogGraphicHandler()
                )
            }

            GraphicDocumentConfig.hmConfig["mms_graphic_speed"] = MMSGraphicDocumentConfig(
                aServerControlClassName = "foatto.mms.core_mms.graphic.server.document.sdcSpeed",
                sensorType = SensorConfig.SENSOR_GEO,
                graphicHandler = null
            )

            mUser.alExtendChildData.add(ChildData("mms_object", true))
            mUser.alExtendChildData.add(ChildData("mms_day_work"))
            mUser.alExtendChildData.add(ChildData("mms_shift_work"))
            mUser.alExtendChildData.add(ChildData("mms_department"))
            mUser.alExtendChildData.add(ChildData("mms_group"))
            mUser.alExtendChildData.add(ChildData("mms_work_shift"))
            mUser.alExtendChildData.add(ChildData("mms_waybill"))
            mUser.alExtendChildData.add(ChildData("mms_worker"))
            mUser.alExtendChildData.add(ChildData("mms_zone"))
            mUser.alExtendChildData.add(ChildData("mms_user_zone"))
            mUser.alExtendChildData.add(ChildData("mms_device"))
            mUser.alExtendChildData.add(ChildData("mms_device_command_history"))

            mUser.alExtendDependData.add(DependData("MMS_object", "user_id"))
            mUser.alExtendDependData.add(DependData("MMS_day_work", "user_id", DependData.DELETE))
            mUser.alExtendDependData.add(DependData("MMS_department", "user_id"))
            mUser.alExtendDependData.add(DependData("MMS_group", "user_id"))
            mUser.alExtendDependData.add(DependData("MMS_work_shift", "user_id"))  // удалить нельзя, есть зависимости в MMS_work_shift_data
            mUser.alExtendDependData.add(DependData("MMS_worker", "user_id"))      // удалить нельзя, есть зависимости в MMS_work_shift
            mUser.alExtendDependData.add(DependData("MMS_zone", "user_id"))      // удалить нельзя, есть зависимости в MMS_object_zone
            mUser.alExtendDependData.add(DependData("MMS_user_zone", "user_id", DependData.DELETE))
            mUser.alExtendDependData.add(DependData("MMS_device", "user_id", DependData.SET, 0))
            mUser.alExtendDependData.add(DependData("MMS_device_command_history", "user_id", DependData.SET, 0))
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

        hmXyDocumentConfig["mms_map"] = XyDocumentConfig(
            name = "mms_map",
            descr = "Карта",
            serverClassName = "foatto.mms.core_mms.xy.server.document.sdcMMSMap",
            clientType = XyDocumentClientType.MAP,
            isScaleAlign = true,
            alElementConfig = initMapElementConfig(MAP_MIN_SCALE, MAP_MAX_SCALE).toList().toTypedArray()
        )

        hmXyDocumentConfig["mms_state"] = XyDocumentConfig(
            name = "mms_state",
            descr = "Состояние объекта",
            serverClassName = "foatto.mms.core_mms.xy.server.document.sdcMMSState",
            clientType = XyDocumentClientType.STATE,
            isScaleAlign = false,
            alElementConfig = initStateElementConfig(1, 1024 * 1024 * 1024).toList().toTypedArray()
        )
    }

    private fun initMapElementConfig(minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {

        val hmElementConfig = initXyElementConfig(level = 10, minScale = minScale, maxScale = maxScale)

        //--- прикладные топо-элементы, программно добавляемые в картографию на серверной стороне

        hmElementConfig[sdcMMSMap.TYPE_OBJECT_TRACE] = XyElementConfig(
            name = sdcMMSMap.TYPE_OBJECT_TRACE,
            clientType = XyElementClientType.TRACE,
            layer = 11,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSMap.TYPE_OBJECT_PARKING] = XyElementConfig(
            name = sdcMMSMap.TYPE_OBJECT_PARKING,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 12,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSMap.TYPE_OBJECT_OVER_SPEED] = XyElementConfig(
            name = sdcMMSMap.TYPE_OBJECT_OVER_SPEED,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 13,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSMap.TYPE_OBJECT_TRACE_INFO] = XyElementConfig(
            name = sdcMMSMap.TYPE_OBJECT_TRACE_INFO,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 14,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSMap.TYPE_OBJECT_INFO] = XyElementConfig(
            name = sdcMMSMap.TYPE_OBJECT_INFO,
            clientType = XyElementClientType.MARKER,
            layer = 15,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        //--- прикладные топо-объекты, добавляемые пользователем вручную на карте

        hmElementConfig[sdcMMSMap.ELEMENT_TYPE_ZONE] = XyElementConfig(
            name = sdcMMSMap.ELEMENT_TYPE_ZONE,
            clientType = XyElementClientType.ZONE,
            layer = 10,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "Геозона",
            isRotatable = false,
            isMoveable = true,
            isEditablePoint = true
        )

        //        hmElementConfig.put(  sdcMMSMap.TYPE_ZONE_LINEAR, new XyElementConfig(
        //            sdcMMSMap.TYPE_ZONE_LINEAR, "foatto.core.app.xy.element.XyCorePoly",
        //            "foatto.app_client.xy.element.XyPoly", "foatto.app.xy.client.element.cemXyPoly",
        //            "Геозона", "Геозона", "Геозона линейная", 80, XyDocumentConfig.MIN_SCALE, XyDocumentConfig.MAX_SCALE  )  );

        return hmElementConfig
    }

    private fun initStateElementConfig(minScale: Int, maxScale: Int): MutableMap<String, XyElementConfig> {

        val hmElementConfig = initXyElementConfig(level = 10, minScale = minScale, maxScale = maxScale)

        hmElementConfig[sdcMMSState.TYPE_STATE_SUM_GROUP_BOX_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_SUM_GROUP_BOX_25D,
            clientType = XyElementClientType.POLY,
            layer = 11,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_SUM_GROUP_TEXT_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_SUM_GROUP_TEXT_25D,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 12,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_GROUP_BOX_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_GROUP_BOX_25D,
            clientType = XyElementClientType.POLY,
            layer = 12,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_GROUP_TEXT_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_GROUP_TEXT_25D,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 13,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_BOTTOM_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_BOTTOM_25D,
            clientType = XyElementClientType.MARKER,
            layer = 14,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_VOLUME_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_VOLUME_25D,
            clientType = XyElementClientType.POLY,
            layer = 15,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_LEVEL_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_LEVEL_25D,
            clientType = XyElementClientType.MARKER,
            layer = 16,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_TANK_WALL_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_TANK_WALL_25D,
            clientType = XyElementClientType.POLY,
            layer = 17,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_TANK_TOP_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_TANK_TOP_25D,
            clientType = XyElementClientType.MARKER,
            layer = 17,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_LL_TEXT_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_LL_TEXT_25D,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 17,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_W_FIGURE_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_W_FIGURE_25D,
            clientType = XyElementClientType.POLY,
            layer = 18,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_W_TEXT_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_W_TEXT_25D,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 18,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_S_FIGURE_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_S_FIGURE_25D,
            clientType = XyElementClientType.MARKER,
            layer = 19,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        hmElementConfig[sdcMMSState.TYPE_STATE_S_TEXT_25D] = XyElementConfig(
            name = sdcMMSState.TYPE_STATE_S_TEXT_25D,
            clientType = XyElementClientType.HTML_TEXT,
            layer = 19,
            scaleMin = minScale,
            scaleMax = maxScale,
            descrForAction = "",
            isRotatable = false,
            isMoveable = false,
            isEditablePoint = false
        )

        return hmElementConfig
    }

}


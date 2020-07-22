package foatto.mms

import foatto.app.CoreSpringApp
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
import foatto.mms.core_mms.graphic.server.graphic_handler.PowerGraphicHandler
import foatto.mms.core_mms.graphic.server.graphic_handler.LiquidGraphicHandler
import foatto.mms.core_mms.sensor.SensorConfig
import foatto.mms.core_mms.xy.server.document.sdcMMSMap
import foatto.mms.core_mms.xy.server.document.sdcMMSState
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication  // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
@EnableWebMvc
open class MMSSpringApp : CoreSpringApp() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<MMSSpringApp>(*args)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

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
            GraphicDocumentConfig.hmConfig[ "mms_graphic_liquid" ] = MMSGraphicDocumentConfig( "mms_graphic_liquid", "foatto.mms.core_mms.graphic.server.document.sdcLiquid",
                    /*null, null, null,*/ SensorConfig.SENSOR_LIQUID_LEVEL, LiquidGraphicHandler() )

            GraphicDocumentConfig.hmConfig[ "mms_graphic_weight" ] = MMSGraphicDocumentConfig( "mms_graphic_weight", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_WEIGHT, AnalogGraphicHandler() )
            GraphicDocumentConfig.hmConfig[ "mms_graphic_turn" ] = MMSGraphicDocumentConfig( "mms_graphic_turn", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_TURN, AnalogGraphicHandler() )
            GraphicDocumentConfig.hmConfig[ "mms_graphic_pressure" ] = MMSGraphicDocumentConfig( "mms_graphic_pressure", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_PRESSURE, AnalogGraphicHandler() )
            GraphicDocumentConfig.hmConfig[ "mms_graphic_temperature" ] = MMSGraphicDocumentConfig( "mms_graphic_temperature", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_TEMPERATURE, AnalogGraphicHandler() )
            GraphicDocumentConfig.hmConfig[ "mms_graphic_voltage" ] = MMSGraphicDocumentConfig( "mms_graphic_voltage", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_VOLTAGE, AnalogGraphicHandler() )

            GraphicDocumentConfig.hmConfig[ "mms_graphic_power" ] = MMSGraphicDocumentConfig( "mms_graphic_power", "foatto.mms.core_mms.graphic.server.document.sdcAnalog",
                    /*null, null, null,*/ SensorConfig.SENSOR_POWER, PowerGraphicHandler() )

            GraphicDocumentConfig.hmConfig[ "mms_graphic_speed" ] = MMSGraphicDocumentConfig( "mms_graphic_speed", "foatto.mms.core_mms.graphic.server.document.sdcSpeed",
                    /*null, null, null,*/ SensorConfig.SENSOR_GEO, null )

            mUser.alExtendChildData.add( ChildData( "mms_object", true ) )
            mUser.alExtendChildData.add( ChildData( "mms_day_work" ) )
            mUser.alExtendChildData.add( ChildData( "mms_downtime" ) )
            mUser.alExtendChildData.add( ChildData( "mms_shift_work" ) )
            mUser.alExtendChildData.add( ChildData( "mms_department" ) )
            mUser.alExtendChildData.add( ChildData( "mms_group" ) )
            mUser.alExtendChildData.add( ChildData( "mms_work_shift" ) )
            mUser.alExtendChildData.add( ChildData( "mms_waybill" ) )
            mUser.alExtendChildData.add( ChildData( "mms_worker" ) )
            mUser.alExtendChildData.add( ChildData( "mms_zone" ) )
            mUser.alExtendChildData.add( ChildData( "mms_user_zone" ) )
            mUser.alExtendChildData.add( ChildData( "mms_device_command_history" ) )

            mUser.alExtendDependData.add( DependData( "SYSTEM_new", "user_id", DependData.DELETE ) )
            mUser.alExtendDependData.add( DependData( "MMS_object", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "MMS_day_work", "user_id", DependData.DELETE ) )
            mUser.alExtendDependData.add( DependData( "MMS_downtime", "user_id", DependData.DELETE ) )
            mUser.alExtendDependData.add( DependData( "MMS_department", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "MMS_group", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "MMS_work_shift", "user_id" ) )  // удалить нельзя, есть зависимости в MMS_work_shift_data
            mUser.alExtendDependData.add( DependData( "MMS_worker", "user_id" ) )      // удалить нельзя, есть зависимости в MMS_work_shift
            mUser.alExtendDependData.add( DependData( "MMS_zone", "user_id" ) )      // удалить нельзя, есть зависимости в MMS_object_zone
            mUser.alExtendDependData.add( DependData( "MMS_user_zone", "user_id", DependData.DELETE ) )
            mUser.alExtendDependData.add( DependData( "MMS_device_command_history", "user_id", DependData.SET, 0 ) )
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

        CoreSpringApp.hmXyDocumentConfig[ "mms_map" ] = XyDocumentConfig(
            "mms_map", "Карта",
            "foatto.mms.core_mms.xy.server.document.sdcMMSMap",
            XyDocumentClientType.MAP,
//            "foatto.app_client.XyMapControl", "foatto.app.MapControl",
            true, initMapElementConfig( MAP_MIN_SCALE, MAP_MAX_SCALE ).toList() )

        CoreSpringApp.hmXyDocumentConfig[ "mms_state" ] = XyDocumentConfig(
            "mms_state", "Состояние объекта",
            "foatto.mms.core_mms.xy.server.document.sdcMMSState",
            XyDocumentClientType.STATE,
//            "foatto.app_client.XyStateControl", "foatto.app.StateControl",
            //--- масштабы пока заданы от балды
            false, initStateElementConfig( 1, 1024 * 1024 * 1024 ).toList() )
    }

    private fun initMapElementConfig( minScale: Int, maxScale: Int ): MutableMap<String, XyElementConfig> {

        val hmElementConfig = initXyElementConfig( 10, minScale, maxScale )

        //--- прикладные топо-элементы, программно добавляемые в картографию на серверной стороне

        hmElementConfig[ sdcMMSMap.TYPE_OBJECT_TRACE ] = XyElementConfig(
            sdcMMSMap.TYPE_OBJECT_TRACE,
            XyElementClientType.TRACE,
            //"foatto.app_client.xy.element.XyTrace", "foatto.app.xy.element.fxTrace",
            11, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSMap.TYPE_OBJECT_PARKING ] = XyElementConfig(
            sdcMMSMap.TYPE_OBJECT_PARKING,
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            12, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSMap.TYPE_OBJECT_OVER_SPEED ] = XyElementConfig(
            sdcMMSMap.TYPE_OBJECT_OVER_SPEED,
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            13, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSMap.TYPE_OBJECT_TRACE_INFO ] = XyElementConfig(
            sdcMMSMap.TYPE_OBJECT_TRACE_INFO,
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            14, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSMap.TYPE_OBJECT_INFO ] = XyElementConfig(
            sdcMMSMap.TYPE_OBJECT_INFO,
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            15, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        //--- прикладные топо-объекты, добавляемые пользователем вручную на карте

        hmElementConfig[ sdcMMSMap.ELEMENT_TYPE_ZONE ] = XyElementConfig(
            sdcMMSMap.ELEMENT_TYPE_ZONE,
            XyElementClientType.ZONE,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxZone",
            10, minScale, maxScale, "Геозона",
            itRotatable = false, itMoveable = true, itEditablePoint = true
        )

        //        hmElementConfig.put(  sdcMMSMap.TYPE_ZONE_LINEAR, new XyElementConfig(
        //            sdcMMSMap.TYPE_ZONE_LINEAR, "foatto.core.app.xy.element.XyCorePoly",
        //            "foatto.app_client.xy.element.XyPoly", "foatto.app.xy.client.element.cemXyPoly",
        //            "Геозона", "Геозона", "Геозона линейная", 80, XyDocumentConfig.MIN_SCALE, XyDocumentConfig.MAX_SCALE  )  );

        return hmElementConfig
    }

    private fun initStateElementConfig( minScale: Int, maxScale: Int ): MutableMap<String, XyElementConfig> {

        val hmElementConfig = initXyElementConfig( 10, minScale, maxScale )

        hmElementConfig[ sdcMMSState.TYPE_STATE_SUM_GROUP_BOX_25D ] = XyElementConfig(
            sdcMMSState.TYPE_STATE_SUM_GROUP_BOX_25D,
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            11, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_SUM_GROUP_TEXT_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_SUM_GROUP_TEXT_25D,
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            12, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_GROUP_BOX_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_GROUP_BOX_25D, 
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            12, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_GROUP_TEXT_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_GROUP_TEXT_25D, 
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            13, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_BOTTOM_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_BOTTOM_25D, 
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            14, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_VOLUME_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_VOLUME_25D, 
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            15, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_LEVEL_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_LEVEL_25D, 
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            16, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_TANK_WALL_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_TANK_WALL_25D, 
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            17, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_TANK_TOP_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_TANK_TOP_25D, 
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            17, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_LL_TEXT_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_LL_TEXT_25D, 
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            17, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_W_FIGURE_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_W_FIGURE_25D, 
            XyElementClientType.POLY,
            //"foatto.app_client.xy.element.XyPoly", "foatto.app.xy.element.fxPoly",
            18, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_W_TEXT_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_W_TEXT_25D, 
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            18, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_S_FIGURE_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_S_FIGURE_25D, 
            XyElementClientType.MARKER,
            //"foatto.app_client.xy.element.XyMarker", "foatto.app.xy.element.fxMarker",
            19, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        hmElementConfig[ sdcMMSState.TYPE_STATE_S_TEXT_25D ] = XyElementConfig( 
            sdcMMSState.TYPE_STATE_S_TEXT_25D, 
            XyElementClientType.TEXT,
            //"foatto.app_client.xy.element.XyText", "foatto.app.xy.element.fxText",
            19, minScale, maxScale, "",
            itRotatable = false, itMoveable = false, itEditablePoint = false )

        return hmElementConfig
    }

}


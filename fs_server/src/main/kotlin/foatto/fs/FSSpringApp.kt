package foatto.fs

import foatto.app.CoreSpringApp
import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.system.mUser
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication  // = @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan
@EnableWebMvc
open class FSSpringApp : CoreSpringApp() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<FSSpringApp>(*args)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//        //--- пока минимально используемый масштаб 1:16, что соответствует старому 1:2000 при 4 пикселях на мм
//        //--- ограничение исходит из-за отсутствия битмапов выше 18-го уровня ( т.е. для 1:1000 ),
//        //--- а 18-й уровень битмапов сооветствует масштабу 1:16
//        val MAP_MIN_SCALE = 16
//        //--- пока максимально используемый масштаб 1:512*1024, что соответствует старому 1:32_768_000 при 4 пикселях на мм
//        val MAP_MAX_SCALE = 512 * 1024
//        //--- "средний" масштаб для случаев,
//        //--- когда "граничная" точка только одна и определить необходимый масштаб невозможно
//        //private static final int MAP_AVG_SCALE = 512; - не видна пригодимость

        init {
            GraphicDocumentConfig.hmConfig[ "fs_graphic_ds_foton" ] = GraphicDocumentConfig( "fs_graphic_ds_foton", "foatto.core_server.app.graphic.server.document.sdcLogGraphic" )

            GraphicDocumentConfig.hmConfig[ "fs_graphic_measure" ] = GraphicDocumentConfig( "fs_graphic_measure", "foatto.fs.core_fs.graphic.server.document.sdcMeasure" )

            mUser.alExtendChildData.add( ChildData( "fs_object", true ) )
            mUser.alExtendChildData.add( ChildData( "fs_controller" ) )
            mUser.alExtendChildData.add( ChildData( "fs_device" ) )
            mUser.alExtendChildData.add( ChildData( "fs_measure" ) )

            mUser.alExtendDependData.add( DependData( "SYSTEM_new", "user_id", DependData.DELETE ) )
            mUser.alExtendDependData.add( DependData( "FS_object", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "FS_controller", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "FS_device", "user_id" ) )
            mUser.alExtendDependData.add( DependData( "FS_measure", "user_id" ) )
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

}


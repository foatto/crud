package foatto.ts.spring

import foatto.core_server.app.graphic.server.GraphicDocumentConfig
import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.system.mUser
import foatto.spring.CoreSpringApp
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
//             GraphicDocumentConfig ...

//            mUser.alExtendChildData.add(ChildData("...", true))
//            mUser.alExtendChildData.add(ChildData("..."))

//            mUser.alExtendDependData.add(DependData("SYSTEM_new", "user_id", DependData.DELETE))
//            mUser.alExtendDependData.add(DependData("...", "user_id"))
//            mUser.alExtendDependData.add(DependData("...", "user_id", DependData.DELETE))
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()
    }

}
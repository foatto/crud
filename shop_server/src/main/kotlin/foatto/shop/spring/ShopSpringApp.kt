package foatto.shop.spring

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
class ShopSpringApp : CoreSpringApp() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<ShopSpringApp>(*args)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        init {
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_day_run", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_waybill", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_fuel", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_department", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_group", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_driver", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_other_owner", null  )  );
            //
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_auto_status", null, true  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_user_zone", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_zone", null  )  );
            //        mUser.alExtendChildData.add(  new ChildData(  "ft_route", null  )  );

            mUser.alExtendDependData.add( DependData( "SYSTEM_new", "user_id", DependData.DELETE ) )
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_auto", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PM_auto_day_run", "user_id", DependData.DELETE  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_auto_fuel", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_auto_waybill", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_route", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_department", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_auto_group", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_auto_driver", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_zone", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_user_zone", "user_id"  )  );
            //        mUser.alExtendDependData.add(  new DependData(  "PLA_report", "user_id", DependData.DELETE  )  );
        }
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()
    }

}


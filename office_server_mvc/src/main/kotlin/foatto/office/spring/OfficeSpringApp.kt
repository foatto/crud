package foatto.office.spring

import foatto.core_server.app.server.ChildData
import foatto.core_server.app.server.DependData
import foatto.core_server.app.system.mUser
import foatto.spring.CoreSpringApp
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.web.servlet.config.annotation.EnableWebMvc

@SpringBootApplication(
    scanBasePackages = ["foatto.spring", "foatto.office.spring"],
)
@EnableWebMvc
class OfficeSpringApp : CoreSpringApp() {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<OfficeSpringApp>(*args)
        }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

        init {

//        mUser.alExtendChildData.add( ChildData( "office_reminder", true ) )
//
//        mUser.alExtendChildData.add( ChildData( "office_reminder_call", true ) )
//        mUser.alExtendChildData.add( ChildData( "office_reminder_meet" ) )
//        mUser.alExtendChildData.add( ChildData( "office_reminder_call_remember" ) )
//        mUser.alExtendChildData.add( ChildData( "office_reminder_input_call" ) )
//        mUser.alExtendChildData.add( ChildData( "office_reminder_meeting" ) )
//        mUser.alExtendChildData.add( ChildData( "office_reminder_other" ) )

        mUser.alExtendChildData.add( ChildData( "office_task_out", true ) )
        mUser.alExtendChildData.add( ChildData( "office_task_in" ) )
        mUser.alExtendChildData.add( ChildData( "office_task_out_archive" ) )
        mUser.alExtendChildData.add( ChildData( "office_task_in_archive" ) )

//        mUser.alExtendChildData.add( ChildData( "office_people", true ) )
//        mUser.alExtendChildData.add( ChildData( "office_client_not_need" ) )
//        mUser.alExtendChildData.add( ChildData( "office_client_in_work" ) )
//        mUser.alExtendChildData.add( ChildData( "office_client_out_work" ) )
//        mUser.alExtendChildData.add( ChildData( "office_client_work_history" ) )

//        mUser.alExtendChildData.add( ChildData( "office_meeting", true ) )
//        mUser.alExtendChildData.add( ChildData( "office_meeting_archive" ) )
        //mUser.alExtendChildData.add( new ChildData( "office_meeting_plan" ) ); - нет смысла без meeting-parent'a
        //mUser.alExtendChildData.add( new ChildData( "office_meeting_invite" ) ); - нет смысла без meeting-parent'a
        //mUser.alExtendChildData.add( new ChildData( "office_meeting_present" ) ); - нет смысла без meeting-parent'a
        //mUser.alExtendChildData.add( new ChildData( "office_meeting_speech" ) ); - нет смысла без meeting-parent'a
        //mUser.alExtendChildData.add( new ChildData( "office_meeting_result" ) ); - нет смысла без meeting-parent'a

        mUser.alExtendDependData.add( DependData( "SYSTEM_new", "user_id", DependData.DELETE ) )

//        mUser.alExtendDependData.add( DependData( "OFFICE_reminder", "user_id", DependData.DELETE ) )
        mUser.alExtendDependData.add( DependData( "OFFICE_task", "out_user_id" ) )
        mUser.alExtendDependData.add( DependData( "OFFICE_task", "in_user_id" ) )
        mUser.alExtendDependData.add( DependData( "OFFICE_task_thread", "user_id", DependData.SET, 0 ) )
        mUser.alExtendDependData.add( DependData( "OFFICE_task_day_state", "out_user_id", DependData.DELETE ) )
        mUser.alExtendDependData.add( DependData( "OFFICE_task_day_state", "in_user_id", DependData.DELETE ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_people", "user_id", DependData.SET, 0 ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_people", "manager_id", DependData.SET, 0 ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_client_work", "user_id", DependData.SET, 0 ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_meeting", "user_id", DependData.SET, 0 ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_meeting_plan", "speaker_id", DependData.SET, 0 ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_meeting_invite", "invite_id", DependData.DELETE ) )
//        mUser.alExtendDependData.add( DependData( "OFFICE_meeting_speech", "speaker_id", DependData.SET, 0 ) )

//        for( int i = 0; i < mMeetingResult.SPEAKER_COUNT; i++ )
//            mUser.alExtendDependData.add( new DependData( "OFFICE_meeting_result",
//                                        new StringBuilder( "speaker_id_" ).append( i ).toString(), DependData.SET, 0 ) );
        }

    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @EventListener(ApplicationReadyEvent::class)
    override fun init() {
        super.init()
    }

}


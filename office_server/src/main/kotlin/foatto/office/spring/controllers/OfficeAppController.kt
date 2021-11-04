package foatto.office.spring.controllers

import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.MenuData
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.spring.controllers.CoreAppController
import foatto.sql.CoreAdvancedStatement
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OfficeAppController : CoreAppController() {

    @PostMapping("/api/app")
    override fun app(
        @RequestBody
        appRequest: AppRequest
    ): AppResponse {
        return super.app(appRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

//    @PostMapping("/api/update")
//    override fun update(
//        @RequestBody
//        updateRequest: UpdateRequest
//    ): UpdateResponse {
//        return super.update( updateRequest )
//    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    //--- пропускаем логи по запуску модулей из 1С, отчётов и показов картографии
    override fun checkLogSkipAliasPrefix(alias: String): Boolean {
        return alias.startsWith("office_report_")
    }

    override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Поручения --------------------------------------------------------------------------------------------------------

        val alMenuTask = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuTask, "office_task_out", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuTask, "office_task_in", true)

        addSeparator(alMenuTask)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuTask, "office_task_out_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuTask, "office_task_in_archive", true)

        if (alMenuTask.size > 1) {
            alMenu.add(MenuData("", "Поручения", alMenuTask.toTypedArray()))
        }

        //--- напоминания -------------------------------------------------------------------------------------------------------

        val alMenuReminder = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_call", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_meet", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_call_remember", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_input_call", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_meeting", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_other", true)

        addSeparator(alMenuReminder)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder", true)

        addSeparator(alMenuReminder)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_call_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_meet_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_call_remember_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_input_call_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_meeting_archive", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_other_archive", true)

        addSeparator(alMenuReminder)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReminder, "office_reminder_archive", true)

        if (alMenuReminder.size > 3) {
            alMenu.add(MenuData("", "Напоминания", alMenuReminder.toTypedArray()))
        }

////--- Сопровождение клиентов -------------------------------------------------------------------------------------------
//
//        ArrayList<MenuData> alMenuClientWork = new ArrayList<>();
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuClientWork, "office_client_not_need", true );
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuClientWork, "office_client_in_work", true );
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuClientWork, "office_client_out_work", true );
//
//            addSeparator( alMenuClientWork );
//
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuClientWork, "office_business", true );
//
//            addSeparator( alMenuClientWork );
//
//            String workAlias = "office_client_in_work";
//            CoreAdvancedResultSet rs = dataWorker.alStm.get( 0 ).executeQuery(
//                " SELECT id , name FROM OFFICE_business ORDER BY name " );
//            while( rs.next() ) {
//                int id = rs.getInt( 1 );
//                String descr = id == 0 ? "(Направление не определено)" : rs.getString( 2 );
//
//                if( checkMenuPermission( hmAliasConfig, hmAliasPerm, workAlias ) )
//                    alMenuClientWork.add( new MenuData( new StringBuilder()
//                        .append( AppParameter.ALIAS ).append( '=' ).append( workAlias )
//                        .append( '&' ).append( AppParameter.ACTION ).append( '=' ).append( AppAction.TABLE )
//                        .append( '&' ).append( AppParameter.PARENT_ALIAS ).append( '=' ).append( "office_business" )
//                        .append( '&' ).append( AppParameter.PARENT_ID ).append( '=' ).append( id ),
//                        descr ) );
//            }
//            rs.close();
//
//        if( alMenuClientWork.size() > 2 )
//            alMenu.add( new MenuData( null, "Сопровождение клиентов", alMenuClientWork ) );
////--- Совещания --------------------------------------------------------------------------------------------------------
//
//        ArrayList<MenuData> alMenuMeeting = new ArrayList<>();
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuMeeting, "office_meeting", true );
//
//            addSeparator( alMenuMeeting );
//
//            addMenu( hmAliasConfig, hmAliasPerm, alMenuMeeting, "office_meeting_archive", true );
////            addMenu( hmAliasConfig, hmAliasPerm, alMenuMeeting, "office_city", true );
//
//        if( alMenuMeeting.size() > 1 )
//            alMenu.add( new MenuData( null, "Совещания", alMenuMeeting ) );
//

        //--- Отчёты --------------------------------------------------------------------------------------------------------

        val alMenuReport = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "office_report_reminder", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "office_report_task", false)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuReport, "office_report_task_day_state", false)
//        addMenu( hmAliasConfig, hmAliasPerm, alMenuReport, "office_report_client_work_state", false )

        if (alMenuReport.size > 0) {
            alMenu.add(MenuData("", "Отчёты", alMenuReport.toTypedArray()))
        }

        //--- Справочники --------------------------------------------------------------------------------------------------------

        val alMenuDir = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "office_people", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "office_company", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuDir, "office_city", true)

        if (alMenuDir.size > 0) {
            alMenu.add(MenuData("", "Справочники", alMenuDir.toTypedArray()))
        }

        //--- Система --------------------------------------------------------------------------------------------------------

        val alMenuSystem = mutableListOf<MenuData>()

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_property", false)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_alias", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_role", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_permission", true)
        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role_permission", true)

        addSeparator(alMenuSystem)

        addMenu(hmAliasConfig, hmAliasPerm, alMenuSystem, "system_log_user", true)

        if (alMenuSystem.size > 3) {
            alMenu.add(MenuData("", "Система", alMenuSystem.toTypedArray()))
        }

        //----------------------------------------------------------------------------------------------------------------------

        return alMenu
    }

}
package foatto.fs

import foatto.app.CoreSpringApp
import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core_server.app.server.AliasConfig
import foatto.core_server.app.server.UserConfig
import foatto.sql.CoreAdvancedStatement

import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URLConnection
import javax.servlet.http.HttpServletResponse

@RestController     // = @Controller + @ResponseBody
open class FSSpringController( aJdbcTemplate: JdbcTemplate) : CoreSpringController( aJdbcTemplate ) {

    @Value("\${expire_period}")
    val expirePeriod: Int = 0



    @PostMapping("/api/app")
    @Transactional
    override fun app(
        //authentication: Authentication,
        @RequestBody
        appRequest: AppRequest
        //@CookieValue("SESSION") sessionId: String
    ): AppResponse {
        return super.app( appRequest )
    }

    @PostMapping("/api/graphic")
    @Transactional
    override fun graphic(
        //authentication: Authentication,
        @RequestBody
        graphicActionRequest: GraphicActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): GraphicActionResponse {
         return super.graphic(graphicActionRequest)
    }

    @PostMapping("/api/xy")
    @Transactional
    override fun xy(
        //authentication: Authentication,
        @RequestBody
        xyActionRequest: XyActionRequest
        //@CookieValue("SESSION") sessionId: String
    ): XyActionResponse {
         return super.xy(xyActionRequest)
    }

    @GetMapping(value = ["/files/{dirName:.+}/{fileName:.+}"])
    fun downloadFile(response: HttpServletResponse, @PathVariable("dirName") dirName: String, @PathVariable("fileName") fileName: String ) {

        val file = File( "${CoreSpringApp.rootDirName}/files/$dirName/$fileName" )
        val mimeType = URLConnection.guessContentTypeFromName(file.name)

        response.contentType = mimeType
        response.setContentLength(file.length().toInt())
        response.outputStream.write( file.readBytes() )
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

    @PostMapping("/api/get_file")
    override fun getFile(
        @RequestBody
        getFileRequest: GetFileRequest
    ): GetFileResponse {
        return super.getFile(getFileRequest)
    }

    @PostMapping("/api/put_file")
    override fun putFile(
        @RequestBody
        putFileRequest: PutFileRequest
    ): PutFileResponse {
        return super.putFile(putFileRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/get_replication")
    override fun getReplication(
        @RequestBody
        getReplicationRequest: GetReplicationRequest
    ): GetReplicationResponse {
        return super.getReplication(getReplicationRequest)
    }

    @PostMapping("/api/put_replication")
    override fun putReplication(
        @RequestBody
        putReplicationRequest: PutReplicationRequest
    ): PutReplicationResponse {
        return super.putReplication(putReplicationRequest)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @PostMapping("/api/save_user_property")
    override fun saveUserProperty(
        @RequestBody
        saveUserPropertyRequest: SaveUserPropertyRequest
    ): SaveUserPropertyResponse {
        return super.saveUserProperty(saveUserPropertyRequest)
    }

    @PostMapping("/api/change_password")
    override fun changePassword(
        @RequestBody
        changePasswordRequest: ChangePasswordRequest
    ): ChangePasswordResponse {
        return super.changePassword(changePasswordRequest)
    }

    @PostMapping("/api/logoff")
    override fun logoff(
        @RequestBody
        logoffRequest: LogoffRequest
    ): LogoffResponse {
        return super.logoff(logoffRequest)
    }

    @PostMapping("/api/upload_form_file")
    override fun uploadFormFile(
        @RequestParam("form_file_ids")
        arrFormFileId: Array<String>,
        @RequestParam("form_file_blobs")
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {
        return super.uploadFormFile(arrFormFileId, arrFormFileBlob)
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

     //--- пропускаем логи по запуску модулей из 1С, отчётов, графиков, показов картографии
    override fun checkLogSkipAliasPrefix( alias: String ): Boolean {
        return alias.startsWith( "fs_report_" ) || alias.startsWith( "fs_graphic_" ) || alias.startsWith( "fs_show_" )
        //        return alias.startsWith(  "1c_"  ) ||
        //               alias.startsWith(  "ft_report_"  )
    }

   override fun menuInit(stm: CoreAdvancedStatement, hmAliasConfig: Map<String, AliasConfig>, userConfig: UserConfig): List<MenuData> {

        val alMenu = mutableListOf<MenuData>()
        val hmAliasPerm = userConfig.userPermission

        //--- Учёт -------------------------------------------------------------------------------------------------------

        val alMenuGeneral = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "fs_object", true )

        addSeparator( alMenuGeneral )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuGeneral, "fs_measure", true )

        if( alMenuGeneral.size > 1 ) alMenu.add( MenuData( "", "Учёт", alMenuGeneral ) )

        //--- Устройства ------------------------------------------------------------------------------------------------------

        val alMenuDevice = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "fs_controller", true )

        addSeparator( alMenuDevice )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "fs_device", true )

//        addSeparator( alMenuDevice )
//
//        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "fs_graphic_ds_foton", false )
//
//        addSeparator( alMenuDevice )
//
//        addMenu( hmAliasConfig, hmAliasPerm, alMenuDevice, "fs_log_ds_foton", true )

        if( alMenuDevice.size > 1 ) alMenu.add( MenuData( "", "Устройства", alMenuDevice ) )

        //--- Система --------------------------------------------------------------------------------------------------------

        val alMenuSystem = mutableListOf<MenuData>()

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_alias", true )

        addSeparator( alMenuSystem )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_user_role", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_permission", true )
        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_role_permission", true )

        addSeparator( alMenuSystem )

        addMenu( hmAliasConfig, hmAliasPerm, alMenuSystem, "system_log_user", true )

        if( alMenuSystem.size > 2 ) alMenu.add( MenuData( "", "Система", alMenuSystem ) )

        //----------------------------------------------------------------------------------------------------------------------

        return alMenu
    }

}
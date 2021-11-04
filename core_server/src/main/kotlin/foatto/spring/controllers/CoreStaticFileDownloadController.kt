package foatto.spring.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.net.URLConnection
import javax.servlet.http.HttpServletResponse

@RestController
class CoreStaticFileDownloadController {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        download(response, "${rootDirName}/web/index.html")
    }

    //--------------------------------------------------------------------------------

    @GetMapping(value = ["/files/{dirName}/{fileName}"])
    fun downloadFile(
        response: HttpServletResponse,
        @PathVariable("dirName")
        dirName: String,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/files/$dirName/$fileName")
    }

    //--------------------------------------------------------------------------------

    @GetMapping(value = ["/web/{fileName}"])
    fun downloadWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/$fileName")
    }

    @GetMapping(value = ["/web/images/{fileName}"])
    fun downloadWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/images/$fileName")
    }

    @GetMapping(value = ["/web/js/{fileName}"])
    fun downloadWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/js/$fileName")
    }

    @GetMapping(value = ["/web/lib/{fileName}"])
    fun downloadWebLib(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/web/lib/$fileName")
    }

    //--------------------------------------------------------------------------------

    @GetMapping(value = ["/reports/{fileName}"])
    fun downloadReports(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/reports/$fileName")
    }

    //--------------------------------------------------------------------------------

    @GetMapping(value = ["/map/{fileName}"])
    fun downloadMaps(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/map/$fileName")
    }

    //--------------------------------------------------------------------------------

    protected fun download(response: HttpServletResponse, path: String) {
        val file = File(path)
        val mimeType = URLConnection.guessContentTypeFromName(file.name)

        response.contentType = mimeType
        response.setContentLength(file.length().toInt())
        response.outputStream.write(file.readBytes())
    }

}

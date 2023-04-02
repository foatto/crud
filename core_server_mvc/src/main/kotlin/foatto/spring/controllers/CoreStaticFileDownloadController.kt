package foatto.spring.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class CoreStaticFileDownloadController {

    @Value("\${root_dir}")
    val rootDirName: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        CoreAppController.download(response, "${rootDirName}/web/index.html")
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/web/{fileName}"])
    fun downloadWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/web/$fileName")
    }

    @GetMapping(value = ["/web/images/{fileName}"])
    fun downloadWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/web/images/$fileName")
    }

    @GetMapping(value = ["/web/js/{fileName}"])
    fun downloadWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/web/js/$fileName")
    }

    @GetMapping(value = ["/web/lib/{fileName}"])
    fun downloadWebLib(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/web/lib/$fileName")
    }

    @GetMapping(value = ["/web/compose/{fileName}"])
    fun downloadWebCompose(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/web/compose/$fileName")
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/reports/{fileName}"])
    fun downloadReports(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/reports/$fileName")
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/map/{fileName}"])
    fun downloadMaps(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        CoreAppController.download(response, "${rootDirName}/map/$fileName")
    }
}

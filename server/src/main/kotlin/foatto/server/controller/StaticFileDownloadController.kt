package foatto.server.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class StaticFileDownloadController {

    companion object {
        const val LICENSE_QR_DIR = "license-qr"
    }

    @Value("\${root_dir}")
    val rootDirName: String = ""

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        FileController.download(response, "${rootDirName}/web/index.html")
    }

    @GetMapping(value = ["/{fileName}"])
    fun downloadWebFromRootUrl(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/web/$fileName")
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    @GetMapping(value = ["/web/{fileName}"])
    fun downloadWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/web/$fileName")
    }

    @GetMapping(value = ["/web/images/{fileName}"])
    fun downloadWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/web/images/$fileName")
    }

    @GetMapping(value = ["/web/js/{fileName}"])
    fun downloadWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/web/js/$fileName")
    }

    @GetMapping(value = ["/web/compose/{fileName}"])
    fun downloadWebCompose(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/web/compose/$fileName")
    }

    @GetMapping(value = ["/$LICENSE_QR_DIR/{fileName}"])
    fun downloadQR(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        FileController.download(response, "${rootDirName}/$LICENSE_QR_DIR/$fileName")
    }

//    @GetMapping(value = ["/reports/{fileName}"])
//    fun downloadReports(
//        response: HttpServletResponse,
//        @PathVariable("fileName")
//        fileName: String
//    ) {
//        AppController.download(response, "${rootDirName}/reports/$fileName")
//    }

//    @GetMapping(value = ["/map/{fileName}"])
//    fun downloadMaps(
//        response: HttpServletResponse,
//        @PathVariable("fileName")
//        fileName: String
//    ) {
//        AppController.download(response, "${rootDirName}/map/$fileName")
//    }

}
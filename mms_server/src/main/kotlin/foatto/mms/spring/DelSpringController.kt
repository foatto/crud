package foatto.mms.spring

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.URI
import java.net.URISyntaxException
import java.net.URLConnection
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class DelSpringController {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    //!!! временная шняга на время разработки del-web -------------------------------

    @RequestMapping(value = ["/json/tnoconnect", "/json/tnoserverinfo", "/json/tnoping"])
    @ResponseBody
    @Throws(URISyntaxException::class)
    fun proxyTNOConnect(
        @RequestBody
        body: String, method: HttpMethod, request: HttpServletRequest, response: HttpServletResponse
    ): ByteArray {
//println( "request = '$body' " )

        val thirdPartyApi = URI("http", null, "tcn-test.pla.ru", 17997, request.requestURI, request.queryString, null)

        val resp = RestTemplate().exchange(thirdPartyApi, method, HttpEntity(body), String::class.java)
//println( "response = '${resp.body}' " )

        //--- именно с таким явнозаданным извратом, иначе слетает кодировка
        return (resp.body ?: "").toByteArray(Charsets.UTF_8)
    }

    @GetMapping(value = ["/del_web/{fileName:.+}"])
    fun downloadDelWeb(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/$fileName")
    }

    @GetMapping(value = ["/del_web/fonts/{fileName:.+}"])
    fun downloadDelWebFonts(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/fonts/$fileName")
    }

    @GetMapping(value = ["/del_web/images/{fileName:.+}"])
    fun downloadDelWebImages(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/images/$fileName")
    }

    @GetMapping(value = ["/del_web/js/{fileName:.+}"])
    fun downloadDelWebJS(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/js/$fileName")
    }

    @GetMapping(value = ["/del_web/lib/{fileName:.+}"])
    fun downloadDelWebLib(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/lib/$fileName")
    }

    @GetMapping(value = ["/del_web/style/{fileName:.+}"])
    fun downloadDelWebStyle(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/del_web/style/$fileName")
    }

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    protected fun download(response: HttpServletResponse, path: String) {
        val file = File(path)
        val mimeType = URLConnection.guessContentTypeFromName(file.name)

        response.contentType = mimeType
        response.setContentLength(file.length().toInt())
        response.outputStream.write(file.readBytes())
    }

}
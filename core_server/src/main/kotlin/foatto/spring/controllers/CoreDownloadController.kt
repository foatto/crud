package foatto.spring.controllers

import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.net.URLConnection
import javax.servlet.http.HttpServletResponse

//--- добавлять у каждого наследника
//@RestController
abstract class CoreDownloadController {

    @Value("\${root_dir}")
    val rootDirName: String = ""

    protected fun download(response: HttpServletResponse, path: String) {
        val file = File(path)
        val mimeType = URLConnection.guessContentTypeFromName(file.name)

        response.contentType = mimeType
        response.setContentLength(file.length().toInt())
        response.outputStream.write(file.readBytes())
    }

}

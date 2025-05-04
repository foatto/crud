package foatto.server.controller

import foatto.core.ApiUrl
import foatto.core.model.request.GetShortFileLinkRequest
import foatto.core.model.response.GetShortFileLinkResponse
import foatto.core.model.response.form.FormFileUploadParams
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.server.SpringApp
import foatto.server.service.FileStoreService
import foatto.server.service.ReportService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.net.URLConnection

@RestController
class FileStoreController(
    private val fileStoreService: FileStoreService,
) {

    companion object {

        fun download(response: HttpServletResponse, path: String) {
            val file = File(path.replace("..", "").replace("//", ""))
            val mimeType = if (path.endsWith(".wasm", true)) {
                "application/wasm"
            } else {
                URLConnection.guessContentTypeFromName(file.name)
            }

            response.contentType = mimeType
            response.setContentLength(file.length().toInt())
            response.outputStream.write(file.readBytes())
        }
    }

    @Value("\${root_dir}")
    private val rootDirName: String = ""

    @Value("\${temp_dir}")
    private val tempDirName: String = ""

    @GetMapping(value = ["/"])
    fun downloadRoot(response: HttpServletResponse) {
        //!!! после удаления старой версии переименовать в index.html
        download(response, "${rootDirName}/web/index_2.html")
    }

    @PostMapping(ApiUrl.GET_SHORT_FILE_LINK)
    fun getShortFileLink(
        @RequestBody
        getShortFileLinkRequest: GetShortFileLinkRequest,
    ): GetShortFileLinkResponse =
        GetShortFileLinkResponse(url = fileStoreService.getShortFileLink(getShortFileLinkRequest.copyRef, getShortFileLinkRequest.hour))

    @PostMapping(ApiUrl.UPLOAD_FORM_FILE)
    fun uploadFormFile(
        @RequestParam(FormFileUploadParams.FORM_FILE_IDS)
        arrFormFileId: Array<String>, // со стороны web-клиента ограничение на передачу массива или только строк или только файлов
        @RequestParam(FormFileUploadParams.FORM_FILE_BLOBS)
        arrFormFileBlob: Array<MultipartFile>
    ): FormFileUploadResponse {

        arrFormFileId.forEachIndexed { i, id ->
            arrFormFileBlob[i].transferTo(File(tempDirName, id))
        }

        return FormFileUploadResponse()
    }

    @GetMapping(value = ["/${FileStoreService.FILE_URL_BASE}/{ref}"])
    fun downloadFile(
        response: HttpServletResponse,
        @PathVariable("ref")
        refString: String,
    ) {
        refString.toLongOrNull()?.let { ref ->
            fileStoreService.getFileStoreData(ref)?.let { fileStoreData ->
                SpringApp.minioProxy?.let { minioProxy ->
                    val mimeType = URLConnection.guessContentTypeFromName(fileStoreData.name)
                    val byteArray = minioProxy.loadFileAsByteArray(fileStoreData.dir)

                    response.contentType = mimeType
                    response.setContentLength(byteArray.size)
                    response.outputStream.write(byteArray)
                } ?: run {
                    download(response, "$rootDirName/${fileStoreService.getFilePath(fileStoreData.dir, fileStoreData.name)}")
                }
            } ?: run {
                response.status = 403   // forbidden
            }
        } ?: run {
            response.status = 403       // forbidden
        }
    }

    @GetMapping(value = ["/${ReportService.REPORT_FILES_BASE}/{fileName}"])
    fun downloadReports(
        response: HttpServletResponse,
        @PathVariable("fileName")
        fileName: String
    ) {
        download(response, "${rootDirName}/reports/$fileName")
    }

}
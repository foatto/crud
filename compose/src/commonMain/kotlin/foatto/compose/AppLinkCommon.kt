package foatto.compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.compose.control.model.form.cell.FormFileCellClient
import foatto.core.model.request.BaseRequest
import foatto.core.model.response.BaseResponse
import foatto.core.model.response.form.FormFileUploadResponse
import foatto.core.util.getRandomLong

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)
/*private*/ internal val sessionId = getRandomLong()

internal expect inline fun <reified IN : BaseRequest, reified OUT : BaseResponse> invokeRequest(
    requestData: IN,
    crossinline onSuccess: suspend (responseData: OUT) -> Unit,
)

internal expect fun invokeUploadFormFile(
    gridData: FormFileCellClient,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit = {},
)

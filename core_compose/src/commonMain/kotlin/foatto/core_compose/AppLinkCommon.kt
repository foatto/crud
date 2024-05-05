package foatto.core_compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.ChangePasswordResponse
import foatto.core.link.FormFileUploadResponse
import foatto.core.link.LogoffRequest
import foatto.core.link.LogoffResponse
import foatto.core.util.getRandomLong
import foatto.core_compose.controls.models.form.FormGridData

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)
/*private*/ internal val sessionId = getRandomLong()

internal expect fun invokeApp(
    request: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
)

/*

fun invokeGraphic(
    graphicActionRequest: GraphicActionRequest,
    success: (responseData: GraphicActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/graphic", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<GraphicActionResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        graphicActionRequest.sessionId = sessionId

        send(Json.encodeToString(graphicActionRequest))
    }
}

fun invokeXy(
    xyActionRequest: XyActionRequest,
    success: (responseData: XyActionResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/xy", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<XyActionResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        xyActionRequest.sessionId = sessionId

        send(Json.encodeToString(xyActionRequest))
    }
}

fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    success: (responseData: SaveUserPropertyResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/save_user_property", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<SaveUserPropertyResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        saveUserPropertyRequest.sessionId = sessionId

        send(Json.encodeToString(saveUserPropertyRequest))
    }
}

*/

internal expect fun invokeChangePassword(
    request: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
)

internal expect fun invokeLogoff(
    request: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
)

internal expect fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    success: (responseData: FormFileUploadResponse) -> Unit = {},
)

/*

fun invokeCustom(
    customRequest: CustomRequest,
    success: (responseData: CustomResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/custom", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<CustomResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        send(Json.encodeToString(customRequest))
    }
}

 */

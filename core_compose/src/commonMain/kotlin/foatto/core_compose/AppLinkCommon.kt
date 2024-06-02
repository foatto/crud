package foatto.core_compose

import com.darkrockstudios.libraries.mpfilepicker.PlatformFile
import foatto.core.app.chart.ChartActionRequest
import foatto.core.app.chart.ChartActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import foatto.core.util.getRandomLong
import foatto.core_compose.controls.models.form.FormGridData

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)
/*private*/ internal val sessionId = getRandomLong()

internal expect fun invokeApp(
    appRequest: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
)

internal expect fun invokeXy(
    xyActionRequest: XyActionRequest,
    onSuccess: (responseData: XyActionResponse) -> Unit,
)

internal expect fun invokeChart(
    chartActionRequest: ChartActionRequest,
    onSuccess: (responseData: ChartActionResponse) -> Unit,
)

internal expect fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    onSuccess: (responseData: SaveUserPropertyResponse) -> Unit,
)

/*

fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    onSuccess: (responseData: SaveUserPropertyResponse) -> Unit = {},
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
    changePasswordRequest: ChangePasswordRequest,
    onSuccess: (response: ChangePasswordResponse) -> Unit,
)

internal expect fun invokeLogoff(
    logoffRequest: LogoffRequest,
    onSuccess: (response: LogoffResponse) -> Unit,
)

internal expect fun invokeUploadFormFile(
    gridData: FormGridData,
    platformFiles: List<PlatformFile>,
    onSuccess: (responseData: FormFileUploadResponse) -> Unit = {},
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

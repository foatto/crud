package foatto.core_compose

import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.util.getRandomLong

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

//!!!
//@OptIn(DelicateCoroutinesApi::class)
//fun invokeChangePassword(
//    request: ChangePasswordRequest,
//    onSuccess: (response: ChangePasswordResponse) -> Unit = {},
//) {
//    GlobalScope.launch(applicationDispatcher) {
//        request.sessionId = sessionId
//
//        val response: ChangePasswordResponse = httpClient.post {
//            url("/api/change_password")
//            contentType(ContentType.Application.Json)
//            setBody(request)
//            timeout {
//                socketTimeoutMillis = 60_000
//            }
//        }.body()
//
//        onSuccess(response)
//    }
//}
//
//@OptIn(DelicateCoroutinesApi::class)
//fun invokeLogoff(
//    request: LogoffRequest,
//    onSuccess: (response: LogoffResponse) -> Unit = {},
//) {
//    GlobalScope.launch(applicationDispatcher) {
//        request.sessionId = sessionId
//
//        val response: LogoffResponse = httpClient.post {
//            url("/api/logoff")
//            contentType(ContentType.Application.Json)
//            setBody(request)
//            timeout {
//                socketTimeoutMillis = 60_000
//            }
//        }.body()
//
//        onSuccess(response)
//    }
//}

/*

fun invokeUploadFormFile(
    formData: org.w3c.xhr.FormData,
    success: (responseData: FormFileUploadResponse) -> Unit = {},
) {
    XMLHttpRequest().apply {
        open("POST", "/api/upload_form_file", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
                        val response = Json.decodeFromString<FormFileUploadResponse>(responseText)
                        success(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("contentType", "application/octet-stream")
        send(formData)
    }
}

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
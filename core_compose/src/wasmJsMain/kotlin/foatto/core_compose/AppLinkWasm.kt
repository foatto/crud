package foatto.core_compose

import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import kotlinx.browser.window
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.xhr.XMLHttpRequest

//WASM!!! временно, до появления всеобщего ktor-client (в т.ч. для wasm)

internal actual fun invokeApp(
    request: AppRequest,
    onSuccess: (response: AppResponse) -> Unit,
) {
    XMLHttpRequest().apply {
        open("POST", "/api/app", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println("responseText = ${responseText}")
                        val response = Json.decodeFromString<AppResponse>(responseText)
//println("response = ${response}")
                        onSuccess(response)
                    }
                } else {
                    httpDefaultErrorHandler(this)
                }
            }
        }
        setRequestHeader("Content-Type", "application/json")

        request.sessionId = sessionId
//println("requestData.toJson = ${appRequest.toJson()}")
        send(Json.encodeToString(request))
    }

}

private fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println("Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} ")
        else -> println("Неизвестная ошибка: $status")
    }
}

package foatto.core_web

import foatto.core.app.graphic.GraphicActionRequest
import foatto.core.app.graphic.GraphicActionResponse
import foatto.core.app.xy.XyActionRequest
import foatto.core.app.xy.XyActionResponse
import foatto.core.link.*
import kotlinx.browser.window
import org.w3c.xhr.XMLHttpRequest
import kotlin.random.Random

private val sessionId = Random.nextLong()

fun invokeApp(
    appRequest: AppRequest,
    success: (responseData: AppResponse) -> Unit,
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/app", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<AppResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        appRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${appRequest.toJson()}" )
        send(appRequest.toJson())
    }
}

fun invokeGraphic(
    graphicActionRequest: GraphicActionRequest,
    success: (responseData: GraphicActionResponse) -> Unit,
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/graphic", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<GraphicActionResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        graphicActionRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${graphicActionRequest.toJson()}" )
        send(graphicActionRequest.toJson())
    }
}

fun invokeXy(
    xyActionRequest: XyActionRequest,
    success: (responseData: XyActionResponse) -> Unit,
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/xy", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<XyActionResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        xyActionRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${graphicActionRequest.toJson()}" )
        send(xyActionRequest.toJson())
    }
}

fun invokeSaveUserProperty(
    saveUserPropertyRequest: SaveUserPropertyRequest,
    success: (responseData: SaveUserPropertyResponse) -> Unit = {},
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/save_user_property", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<SaveUserPropertyResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        saveUserPropertyRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${graphicActionRequest.toJson()}" )
        send(saveUserPropertyRequest.toJson())
    }
}

fun invokeChangePassword(
    changePasswordRequest: ChangePasswordRequest,
    success: (responseData: ChangePasswordResponse) -> Unit = {},
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/change_password", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<ChangePasswordResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        changePasswordRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${graphicActionRequest.toJson()}" )
        send(changePasswordRequest.toJson())
    }
}

fun invokeLogoff(
    logoffRequest: LogoffRequest,
    success: (responseData: LogoffResponse) -> Unit = {},
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/logoff", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<LogoffResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("Content-Type", "application/json")

        logoffRequest.sessionId = sessionId
        //--- печально, но, в отличие от JSON.parse, не работает
        //send(JSON.stringify(requestData))
//println( "requestData.toJson = ${graphicActionRequest.toJson()}" )
        send(logoffRequest.toJson())
    }
}

fun invokeUploadFormFile(
    formData: org.w3c.xhr.FormData,
    success: (responseData: FormFileUploadResponse) -> Unit = {},
    error: (xmlHttpRequest: XMLHttpRequest) -> Unit = {},
    finally: (() -> Unit) = {}
) {
    XMLHttpRequest().apply {
        open("POST", "/api/upload_form_file", true)
        onreadystatechange = {
            if (this.readyState == XMLHttpRequest.DONE) {
                if (this.status == 200.toShort()) {
                    if (responseText.isNotBlank()) {
//println( "responseText = ${responseText}" )
                        val response = JSON.parse<FormFileUploadResponse>(responseText)
                        success(response)
                    }
                }
                else {
//                    error(this)
                    httpDefaultErrorHandler( this )
                }
//                finally()
            }
        }
        setRequestHeader("contentType", "application/octet-stream")
        send(formData)
    }
}

//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

fun httpDefaultErrorHandler(xmlHttpRequest: XMLHttpRequest) {
    val status = xmlHttpRequest.status.toInt()
    when {
        status == 400 -> window.location.reload()
        status >= 500 -> println( "Ошибка сервера: $status \r\n Сообщение для разработчиков: ${xmlHttpRequest.responseText} " )
        else -> println("Неизвестная ошибка: $status")
    }
}

////-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//
//    suspend fun invokeGetFile( getFileRequest: GetFileRequest ): GetFileResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<GetFileResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/get_file" ) )
//            contentType( ContentType.Application.Json )
//            body = getFileRequest
//        }
//        client.close()
//
//        return response
//    }
//
//    suspend fun invokePutFile( putFileRequest: PutFileRequest ): PutFileResponse {
//
//        val client = HttpClient( CIO ) {
//            install( JsonFeature ) {
//                serializer = JacksonSerializer()
////                serializer = KotlinxSerializer()
//            }
//        }
//
//        val ( serverIP, serverPort ) = alServer[ serverNo ]
//        val response = client.post<PutFileResponse> {
//            url( URL( "http://$serverIP:$serverPort/api/put_file" ) )
//            contentType( ContentType.Application.Json )
//            body = putFileRequest
//        }
//        client.close()
//
//        return response
//    }

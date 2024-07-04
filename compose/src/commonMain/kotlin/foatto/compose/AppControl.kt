package foatto.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import foatto.compose.composable.LogonForm
import foatto.compose.control.AbstractControl
import foatto.compose.control.EmptyControl
import foatto.compose.control.FormControl
import foatto.compose.control.TableControl
import foatto.compose.model.MenuDataClient
import foatto.compose.utils.SETTINGS_LOGIN
import foatto.compose.utils.SETTINGS_PASSWORD
import foatto.compose.utils.encodePassword
import foatto.compose.utils.settings
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.request.system.AppRequest
import foatto.core.model.request.system.LogonRequest
import foatto.core.model.response.system.AppResponse
import foatto.core.model.response.system.LogonResponse
import foatto.core.model.response.system.MenuData
import foatto.core.model.response.system.ResponseCode

class AppControl(
    private val root: Root,
    private val action: AppAction,
    private val tabId: Int,
) {

    private var responseCode by mutableStateOf(ResponseCode.LOGON_NEED)
    private var curControl by mutableStateOf<AbstractControl>(EmptyControl())

    private var login by mutableStateOf("")
    private var password by mutableStateOf("")
    private var isRememberMe by mutableStateOf(true)

    private lateinit var prevRequest: AppRequest

    @Composable
    fun Body() {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (responseCode) {
                ResponseCode.LOGON_NEED, ResponseCode.LOGON_FAILED -> {
                    LogonForm(
                        modifier = Modifier.align(Alignment.Center),
                        responseCode = responseCode,
                        login = login,
                        password = password,
                        isRememberMe = isRememberMe,
                        onLoginInput = { value ->
                            login = value
                        },
                        onPasswordInput = { value ->
                            password = value
                        },
                        onIsRememberMeChange = { value ->
                            isRememberMe = value
                        },
                        logon = {
                            invokeLogonRequest(getLogonRequest())
                        }
                    )
                }

                ResponseCode.LOGON_SYSTEM_BLOCKED -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Пользователь заблокирован системой.",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ResponseCode.LOGON_ADMIN_BLOCKED -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Пользователь заблокирован администратором.",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }

                ResponseCode.MODULE_TABLE, ResponseCode.MODULE_FORM,
                    /* !!!ResponseCode.XY, ResponseCode.CHART, ResponseCode.COMPOSITE*/
                -> {
                    curControl.Body()
                }

                ResponseCode.LOGON_SUCCESS -> {}   // промежуточное состояние при автологоне

                else -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Unknown response code: $responseCode",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    fun start() {
        call(AppRequest(action = action))
    }

    fun call(appRequest: AppRequest) {
        val isAutoClose = appRequest.action.isAutoClose

        //--- пустое урло, ничего не делаем
        if (appRequest.action.module.isNullOrBlank()) {
            if (isAutoClose) {
                root.closeTabById(tabId)
            }
        }
        //--- файловое урло
        else if (appRequest.action.type == ActionType.FILE) {
            root.openTab(appRequest.action)
            if (isAutoClose) {
                root.closeTabById(tabId)
            }
        } else {
            root.setWait(true)

            invokeRequest(appRequest) { appResponse: AppResponse ->
                when (appResponse.responseCode) {
                    //--- если требуется вход - сохраним последний запрос
                    ResponseCode.LOGON_NEED -> {
                        prevRequest = appRequest

                        //--- попробуем автологон
                        val savedLogin = settings.getStringOrNull(SETTINGS_LOGIN)
                        val savedPassword = settings.getStringOrNull(SETTINGS_PASSWORD)
                        if (!savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()) {
                            invokeLogonRequest(
                                LogonRequest(
                                    login = savedLogin,
                                    password = savedPassword,
//                            fillSystemProperties(logonRequest.hmSystemProperties)
                                )
                            )
                        } else {
                            responseCode = appResponse.responseCode
//                            val element = document.getElementById("logon_0")
//                            if (element is HTMLElement) {
//                                element.focus()
//                            }
                        }
                    }

                    ResponseCode.MODULE_TABLE -> {
                        val tableControl = TableControl(root, this, appRequest.action, appResponse.table!!, tabId)
                        curControl = tableControl
                        responseCode = appResponse.responseCode

                        tableControl.start()
                    }

                    ResponseCode.MODULE_FORM -> {
                        val formControl = FormControl(root, this, appRequest.action, appResponse.form!!, tabId)
                        curControl = formControl
                        responseCode = appResponse.responseCode

                        formControl.start()
                    }

//!!!                    ResponseCode.XY -> {
//                        appResponse.xy?.let { xy ->
//                            val xyControl = when (xy.documentConfig.clientType) {
//                                XyDocumentClientType.MAP -> MapControl(root, this, xy, tabId)
//                                XyDocumentClientType.SCHEME -> SchemeControl(root, this, xy, tabId)
//                            }
//                            curControl = xyControl
//                            responseCode = appResponse.code
//
//                            xyControl.start()
//                        }
//                    }
//
//                    ResponseCode.CHART -> {
//                        val chartControl = ChartControl(root, this, appResponse.chart!!, tabId)
//                        curControl = chartControl
//                        responseCode = appResponse.code
//
//                        chartControl.start()
//                    }
//
//                    ResponseCode.COMPOSITE -> {
//                        val compositeControl = CompositeControl(root, this, appResponse.composite!!, tabId)
//                        curControl = compositeControl
//                        responseCode = appResponse.code
//
//                        compositeControl.start()
//                    }

////            ResponseCode.VIDEO_ONLINE, ResponseCode.VIDEO_ARCHIVE -> {
////                val vcstartParamId = bbIn.getShortString()
////                val vcStartTitle = bbIn.getShortString()
////
////                val videoControl = if( curResponseCode == ResponseCode.VIDEO_ONLINE.toInt() ) VideoOnlineControl()
////                else VideoArchiveControl()
////                addPanes( videoControl )
////
////                //--- init работает совместно с read
////                videoControl.init( appContainer, appLink, tab, this, vcstartParamId, vcStartTitle )
////            }

                    else -> {
                        responseCode = appResponse.responseCode
                    }
                }
                root.setWait(false)
            }
        }
    }

    private fun getLogonRequest(): LogonRequest {
        val encodedPassword = encodePassword(password)

        if (isRememberMe) {
            settings.putString(SETTINGS_LOGIN, login)
            settings.putString(SETTINGS_PASSWORD, encodedPassword)
        } else {
            settings.remove(SETTINGS_LOGIN)
            settings.remove(SETTINGS_PASSWORD)
        }

        return LogonRequest(
            login = login,
            password = encodedPassword,
//        fillSystemProperties(logonRequest.hmSystemProperties)
        )
    }

    private fun invokeLogonRequest(logonRequest: LogonRequest) {
        root.setWait(true)
        invokeRequest(logonRequest) { logonResponse: LogonResponse ->
            responseCode = logonResponse.responseCode
            if (logonResponse.responseCode == ResponseCode.LOGON_SUCCESS ||
                logonResponse.responseCode == ResponseCode.LOGON_SUCCESS_BUT_OLD
            ) {
//                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
//                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
                root.appUserConfig.value = logonResponse.appUserConfig!!
                root.setMenuBarData(
                    alMenuData = logonResponse.alMenuData!!.map { menuDataServer ->
                        mapMenuData(menuDataServer)
                    }.toMutableList(),
                    scaledScreenWidth = root.scaledScreenWidth
                )
                prevRequest = logonResponse.redirectOnLogon ?: prevRequest
                //--- перевызовем сервер с предыдущей (до-логинной) командой
                call(prevRequest)
            }
            root.setWait(false)
        }
    }

    private fun mapMenuData(menuDataServer: MenuData): MenuDataClient = MenuDataClient(
        caption = menuDataServer.caption,
        action = menuDataServer.action,
        alSubMenu = menuDataServer.alSubMenu?.map { subMenuDataServer ->
            mapMenuData(subMenuDataServer)
        },
        inNewTab = true,
    )

}
/*
    private fun doNextFocus(curIndex: Int) {
        val element = document.getElementById("logon_${curIndex + 1}")
        if (element is HTMLElement) {
            element.focus()
        }
    }

    private fun fillSystemProperties(hmSystemProperties: MutableMap<String, String>) {
        hmSystemProperties["k.b.w.devicePixelRatio"] = window.devicePixelRatio.toString()
        hmSystemProperties["k.b.w.appCodeName"] = window.navigator.appCodeName
        hmSystemProperties["k.b.w.appName"] = window.navigator.appName
        hmSystemProperties["k.b.w.appVersion"] = window.navigator.appVersion
        hmSystemProperties["k.b.w.language"] = window.navigator.language
        hmSystemProperties["k.b.w.platform"] = window.navigator.platform
        hmSystemProperties["k.b.w.product"] = window.navigator.product
        hmSystemProperties["k.b.w.productSub"] = window.navigator.productSub
        hmSystemProperties["k.b.w.userAgent"] = window.navigator.userAgent
        hmSystemProperties["k.b.w.vendor"] = window.navigator.vendor
        hmSystemProperties["k.b.w.width"] = window.screen.width.toString()
        hmSystemProperties["k.b.w.height"] = window.screen.height.toString()
        hmSystemProperties["k.b.w.availWidth"] = window.screen.availWidth.toString()
        hmSystemProperties["k.b.w.availHeight"] = window.screen.availHeight.toString()
    }

 */
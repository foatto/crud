package foatto.core_compose

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
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.LogonRequest
import foatto.core.link.MenuData
import foatto.core.link.ResponseCode
import foatto.core.link.XyDocumentClientType
import foatto.core_compose.composables.LogonForm
import foatto.core_compose.controls.AbstractControl
import foatto.core_compose.controls.EmptyControl
import foatto.core_compose.controls.FormControl
import foatto.core_compose.controls.MapControl
import foatto.core_compose.controls.SchemeControl
import foatto.core_compose.controls.getTableControl
import foatto.core_compose.di.SETTINGS_LOGIN
import foatto.core_compose.di.SETTINGS_PASSWORD
import foatto.core_compose.di.settings
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.utils.encodePassword

/*

для перекрытия в shop-client

var getTableControl: (
    root: Root,
    appControl: AppControl,
    appParam: String,
    tableResponse: TableResponse,
    tabId: Int,
) -> TableControl = { root: Root,
                      appControl: AppControl,
                      appParam: String,
                      tableResponse: TableResponse,
                      tabId: Int ->
    TableControl(root, appControl, appParam, tableResponse, tabId)
}

 */

class AppControl(
    private val root: Root,
    private val startAppParam: String,
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
                            logon()
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

                ResponseCode.TABLE, ResponseCode.FORM, ResponseCode.XY -> {
                    curControl.Body()
                }

                else -> {
                    Text(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Unknown response code: $responseCode",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold,
                    )
                }

                /*
                    REDIRECT - ?, используется ли

                    GRAPHIC -> CHART,
                    COMPOSITE -> DASHBOARD (?),
                 */
            }
        }
    }

    fun start() {
        call(AppRequest(action = startAppParam))
    }

    fun call(appRequest: AppRequest) {
        var isAutoClose = false

        //--- урло с автозакрытием текущей вкладки
        if ( /*!appRequest.action.isEmpty() && */ appRequest.action.startsWith("#")) {
            appRequest.action = appRequest.action.substring(1)
            isAutoClose = true
        }

        //--- пустое урло, ничего не делаем
        if (appRequest.action.isEmpty()) {
            if (isAutoClose) {
                root.closeTabById(tabId)
            }
        }
        //--- файловое урло
        else if (appRequest.action.startsWith("/")) {
            root.openTab(appRequest.action)
            if (isAutoClose) {
                root.closeTabById(tabId)
            }
        } else {
            root.setWait(true)

            invokeApp(appRequest) { appResponse: AppResponse ->
                when (appResponse.code) {
                    //--- если требуется вход - сохраним последний запрос
                    ResponseCode.LOGON_NEED -> {
                        prevRequest = appRequest

                        //--- попробуем автологон
                        val savedLogin = settings.getStringOrNull(SETTINGS_LOGIN)
                        val savedPassword = settings.getStringOrNull(SETTINGS_PASSWORD)
                        if (!savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()) {
                            val logonRequest = LogonRequest(savedLogin, savedPassword)
//                            fillSystemProperties(logonRequest.hmSystemProperties)

                            call(AppRequest(action = AppAction.LOGON, logon = logonRequest))
                        } else {
                            responseCode = appResponse.code
//                            val element = document.getElementById("logon_0")
//                            if (element is HTMLElement) {
//                                element.focus()
//                            }
                        }
                    }
                    //--- если вход успешен - повторим последний запрос
                    ResponseCode.LOGON_SUCCESS, ResponseCode.LOGON_SUCCESS_BUT_OLD -> {
//                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
//                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
//
                        root.currentUserName = appResponse.currentUserName
                        appResponse.hmUserProperty?.forEach { (key, value) ->
                            if (key == UP_TIME_OFFSET) {
                                //--- на сервере может лежать как в секундах, так и в миллисекундах (старый вариант)
                                val timeOffset = value.toInt()
                                //--- если смещение <= максимально возможного смещения в секундах (43 200 сек), значит оно задано в секундах (логично)
                                //--- в противном случае смещение задано в старом варианте - в миллисекундах
                                //--- (минимальное значение будет начинаться с 1 час * 60 * 60 * 1000 = 3 600 000 мс, что всяко не совпадает с верхней границей в 43 200 от предущего варианта)
                                root.timeOffset = if (timeOffset <= 12 * 60 * 60) {
                                    timeOffset
                                } else {
                                    timeOffset / 1000
                                }
                            }
                        }
                        root.setMenuBarData(
                            alMenuData = appResponse.alMenuData!!.map { menuDataServer ->
                                mapMenuData(menuDataServer)
                            },
                            scaledScreenWidth = root.scaledScreenWidth
                        )
                        //--- перевызовем сервер с предыдущей (до-логинной) командой
                        call(prevRequest)
                    }

                    ResponseCode.REDIRECT -> {
                        call(AppRequest(appResponse.redirect!!))
                    }

                    ResponseCode.TABLE -> {
                        val tableControl = getTableControl(root, this, appRequest.action, appResponse.table!!, tabId)
                        curControl = tableControl
                        responseCode = appResponse.code

                        tableControl.start()
                    }

                    ResponseCode.FORM -> {
                        val formControl = FormControl(root, this, appResponse.form!!, tabId)
                        curControl = formControl
                        responseCode = appResponse.code

                        formControl.start()
                    }

                    ResponseCode.XY -> {
                        appResponse.xy?.let { xy ->
                            val xyControl = when (xy.documentConfig.clientType) {
                                XyDocumentClientType.MAP -> MapControl(root, this, xy, tabId)
                                XyDocumentClientType.STATE -> SchemeControl(root, this, xy, tabId)
                            }
                            curControl = xyControl
                            responseCode = appResponse.code

                            xyControl.start()
                        }
                    }

//                    ResponseCode.GRAPHIC -> {
//                        val graphicControl = GraphicControl(root, this, appResponse.graphic!!, tabId)
//                        curControl.value = graphicControl
//                        responseCode.value = appResponse.code
//
//                        graphicControl.start()
//                    }
//
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
//                    ResponseCode.COMPOSITE -> {
//                        val compositeControl = getCompositeControl(root, this, appResponse.composite!!, tabId)
//                        curControl.value = compositeControl
//                        responseCode.value = appResponse.code
//
//                        compositeControl.start()
//                    }

                    else -> {
                        responseCode = appResponse.code
                    }
                }
                root.setWait(false)
            }
        }
    }

    private fun logon() {
        val encodedPassword = encodePassword(password)

        if (isRememberMe) {
            settings.putString(SETTINGS_LOGIN, login)
            settings.putString(SETTINGS_PASSWORD, encodedPassword)
        } else {
            settings.remove(SETTINGS_LOGIN)
            settings.remove(SETTINGS_PASSWORD)
        }

        val logonRequest = LogonRequest(login, encodedPassword)
//        fillSystemProperties(logonRequest.hmSystemProperties)

        call(AppRequest(action = AppAction.LOGON, logon = logonRequest))
    }

    private fun mapMenuData(menuDataServer: MenuData): MenuDataClient = MenuDataClient(
        url = menuDataServer.url,
        text = menuDataServer.text,
        alSubMenu = menuDataServer.alSubMenu?.map { subMenuDataServer ->
            mapMenuData(subMenuDataServer)
        },
        inNewWindow = true,
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
package foatto.core_compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.LogonRequest
import foatto.core.link.MenuData
import foatto.core.link.ResponseCode
import foatto.core_compose.composables.LogonForm
import foatto.core_compose.controls.AbstractControl
import foatto.core_compose.controls.EmptyControl
import foatto.core_compose.di.settings
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.utils.encodePassword

class AppControl(
    private val root: Root,
    private val startAppParam: String,
    private val tabId: Int,
) {

    companion object {
        private const val SETTINGS_LOGIN: String = "login"
        private const val SETTINGS_PASSWORD: String = "password"
    }

    private var responseCode by mutableStateOf(ResponseCode.LOGON_NEED)
    private val curControl by mutableStateOf<AbstractControl>(EmptyControl())

    private var login by mutableStateOf("")
    private var password by mutableStateOf("")
    private var isRememberMe by mutableStateOf(false)

    private lateinit var prevRequest: AppRequest

    @Composable
    fun Body() {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            when (responseCode) {
                ResponseCode.LOGON_NEED, ResponseCode.LOGON_FAILED -> {
                    LogonForm(
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
                        text = "Пользователь заблокирован системой.",
                    )
                }

                ResponseCode.LOGON_ADMIN_BLOCKED -> {
                    Text(
                        text = "Пользователь заблокирован администратором.",
                    )
                }

                ResponseCode.TABLE, ResponseCode.FORM -> {
                    curControl.Body()
                }

                else -> {
                    Text("Unknown response code: $responseCode")
                }

                /*
                    LOGON_SUCCESS,
                    LOGON_SUCCESS_BUT_OLD,

                    REDIRECT,

                    GRAPHIC,
                    XY,
                    COMPOSITE,
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
                            appResponse.alMenuData!!.map { menuDataServer ->
                                mapMenuData(menuDataServer)
                            }
                        )
                        //--- перевызовем сервер с предыдущей (до-логинной) командой
                        call(prevRequest)
                    }

                    ResponseCode.REDIRECT -> {
                        call(AppRequest(appResponse.redirect!!))
                    }

//                    ResponseCode.TABLE -> {
//                        val tableControl = getTableControl(root, this, appRequest.action, appResponse.table!!, tabId)
//                        curControl.value = tableControl
//                        responseCode.value = appResponse.code
//
//                        tableControl.start()
//                    }
//
//                    ResponseCode.FORM -> {
//                        val formControl = FormControl(root, this, appResponse.form!!, tabId)
//                        curControl.value = formControl
//                        responseCode.value = appResponse.code
//
//                        formControl.start()
//                    }
//
//                    ResponseCode.GRAPHIC -> {
//                        val graphicControl = GraphicControl(root, this, appResponse.graphic!!, tabId)
//                        curControl.value = graphicControl
//                        responseCode.value = appResponse.code
//
//                        graphicControl.start()
//                    }
//
//                    ResponseCode.XY -> {
//                        appResponse.xy?.let { xy ->
//                            val xyControl = when (xy.documentConfig.clientType) {
//                                XyDocumentClientType.MAP -> MapControl(root, this, xy, tabId)
//                                XyDocumentClientType.STATE -> StateControl(root, this, xy, tabId)
//                                else -> MapControl(root, this, xy, tabId)
//                            }
//                            curControl.value = xyControl
//                            responseCode.value = appResponse.code
//
//                            xyControl.start()
//                        }
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
        inNewWindow = false,
        isHover = mutableStateOf(false),
    )

}
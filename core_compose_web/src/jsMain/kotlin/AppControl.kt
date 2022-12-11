import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import foatto.core.link.ResponseCode
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

//--- AppControl style ---

var styleAppControlPaddingTop: () -> Double = { 0.0 }
var styleAppControlPaddingRight: () -> Double = { 0.0 }
var styleAppControlPaddingBottom: () -> Double = { 0.0 }
var styleAppControlPaddingLeft: () -> Double = { 0.0 }

var colorLogonBackAround: () -> String = { colorMainBack1 }
var colorLogonBackCenter: () -> String = { colorMainBack2 }
var colorLogonBorder: () -> CSSColorValue = { colorMainBorder() }
var colorLogonButtonBack: () -> String = { colorButtonBack() }
var colorLogonButtonText = COLOR_MAIN_TEXT
var colorLogonButtonBorder: () -> CSSColorValue = { colorButtonBorder() }

var styleLogonTopExpanderContent = "&nbsp;"
var styleLogonLogo = "logo.png"
var styleLogonLogoContent = ""

val styleLogonCellPadding = "${if (!styleIsNarrowScreen) "2.0rem" else "1.0rem"} 2.0rem"
val styleLogonLogoPadding = "0.4rem 0 1.0rem 0"
val styleLogonControlPadding = "0.4rem 0"
val styleLogonCheckBoxMargin = "0rem 0.5rem 0rem 0rem"
var styleLogonButtonPadding: () -> String = { "1.0rem ${if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48}rem" }
val styleLogonButtonMargin = "1.0rem 0 ${if (!styleIsNarrowScreen) "1.0rem" else "0"} 0"

fun styleLogonTextLen() = if (!styleIsNarrowScreen) 40 else scaledScreenWidth / 16
var styleLogonButtonText = "Вход"
var styleLogonButtonFontWeight = "normal"

//--- AppControl ---

class AppControl(
    val startAppParam: String,
    val tabId: Int,
) {

    private var responseCode = mutableStateOf(ResponseCode.LOGON_NEED)

    @Composable
    fun getBody() {

        Div(
            attrs = {
                style {
                    flexGrow(1)
                    flexShrink(1)
                    //!!! Непонятный баг со вложенностью контейнеров,
                    //--- при установке height="100%" уходит за экран нижняя панель с кнопками страниц в таблице или кнопками формы.
                    //--- Если определение height убрать, то пропадает прокрутка вообще.
                    //--- Работает, если установить высоту в диапазон от 0% до примерно 80%, оставлю 1% на всякий случай
                    //--- (неизвестно как потенциально сглючит 0% высоты в будущем)
                    height(1.percent)
                    paddingTop(styleAppControlPaddingTop().cssRem)
                    paddingRight(styleAppControlPaddingRight().cssRem)
                    paddingBottom(styleAppControlPaddingBottom().cssRem)
                    paddingLeft(styleAppControlPaddingLeft().cssRem)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                }
            }
        ) {
            when(responseCode.value) {
                ResponseCode.LOGON_NEED, ResponseCode.LOGON_FAILED -> {
                    Div(
                        attrs = {
                            style {
                                height(100.percent)
                                display(DisplayStyle.Grid)
                                gridTemplateRows("1fr auto 1fr")
                                gridTemplateColumns("1fr auto 1fr")
                                background(colorLogonBackAround())
                                backgroundSize("cover")             // for background-image
                            }
                        }
                    ) {
                        Div(
                            attrs = {
                                style {
                                    gridArea("1", "1", "2", "4")
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    alignItems(AlignItems.Center)
                                    justifyContent(JustifyContent.SpaceEvenly)
                                    textAlign("center")
                                }
                            }
                        ) {
//                    $styleLogonTopExpanderContent
                        }
                        Div(
                            attrs = {
                                style {
                                    gridArea("2", "2", "3", "3")
                                    paddingTop(styleLogonCellPaddingTop().cssRem)
                                    paddingRight(styleLogonCellPaddingRight().cssRem)
                                    paddingBottom(styleLogonCellPaddingBottom().cssRem)
                                    paddingLeft(styleLogonCellPaddingLeft().cssRem)
                                    border {
                                        color(colorLogonBorder())
                                        style(LineStyle.Solid)
                                        width(1.px)
                                    }
                                    borderRadius(styleFormBorderRadius.cssRem)
                                    background(colorLogonBackCenter())
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    alignItems(AlignItems.Center)

                                }
                            }
                        ) {
                            Div(
                                attrs = {
                                    style {
                                        paddingTop(styleLogonLogoPaddingTop().cssRem)
                                        paddingRight(styleLogonLogoPaddingRight().cssRem)
                                        paddingBottom(styleLogonLogoPaddingBottom().cssRem)
                                        paddingLeft(styleLogonLogoPaddingLeft().cssRem)
                                    }
                                }
                            ) {
//                        <img src="/web/images/$styleLogonLogo">
                            }
//                    $styleLogonLogoContent
                            if (responseCode.value == ResponseCode.LOGON_FAILED) {
                                Div(
                                    attrs = {
                                        style {
                                            alignSelf(AlignSelf.Center)
                                            fontSize(styleControlTextFontSize().cssRem)
//                "color" to "red"
                                        }
                                    }
                                ) {
//                        Неправильное имя или пароль
                                }
                            }
//                    <div v-bind:style="style_logon_div">
//                        <input type="text"
//                               v-model="login"
//                               v-on:keyup.enter.exact="doNextFocus(0)"
//                               v-bind:style="style_logon_input"
//                               id="logon_0"
//                               size="${styleLogonTextLen()}"
//                               placeholder="Имя"
//                        >
//                    </div>
//                    <div v-bind:style="style_logon_div">
//                        <input type="password"
//                               v-model="password"
//                               v-on:keyup.enter.exact="doNextFocus(1)"
//                               v-bind:style="style_logon_input"
//                               id="logon_1"
//                               size="${styleLogonTextLen()}"
//                               placeholder="Пароль"
//                        >
//                    </div>
//                    <div v-bind:style="[
//                            style_logon_div,
//                            {
//                                'align-self' : 'flex-start' ,
//                                'color' : '$COLOR_MAIN_TEXT',
//                                'display' : 'flex' ,
//                                'align-items' : 'center'
//                            }
//                        ]"
//                    >
//                        <input type="checkbox"
//                               v-model="isRememberMe"
//                               v-on:keyup.enter.exact="doNextFocus(2)"
//                               v-bind:style="style_logon_checkbox"
//                               id="logon_2"
//                        >
//                            Запомнить меня
//                        </input>
//                    </div>
//                    <div v-bind:style="style_logon_div">
//                        <button v-on:click="logon"
//                                v-bind:style="style_logon_button"
//                                id="logon_3"
//                        >
//                            $styleLogonButtonText
//                        </button>
//                    </div>
                        }
                        Div(
                            attrs = {
                                style {
                                    gridArea("3", "1", "4", "4")
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    alignItems(AlignItems.Center)
                                    justifyContent(JustifyContent.SpaceEvenly)
                                    textAlign("center")
                                }
                            }
                        ) {
//                    &nbsp;
                        }
                    }
                }
                ResponseCode.LOGON_SYSTEM_BLOCKED -> {
//                "Ошибка входа в систему", "Слишком много неудачных попыток входа. \nПользователь временно заблокирован. \nПопробуйте войти попозже."
                }
                ResponseCode.LOGON_ADMIN_BLOCKED -> {
//                "Ошибка входа в систему", "Пользователь заблокирован администратором."
                }
                ResponseCode.TABLE, ResponseCode.FORM, ResponseCode.GRAPHIC, ResponseCode.XY, ResponseCode.COMPOSITE -> {
//                       v-bind:is="curControl">
                }
                else -> {
//                <!-- Unknown response code: --> {{responseCode}}
                }

            }
        }
    }
}

//import foatto.core.app.UP_TIME_OFFSET
//import foatto.core.link.AppAction
//import foatto.core.link.AppRequest
//import foatto.core.link.AppResponse
//import foatto.core.link.CompositeResponse
//import foatto.core.link.LogonRequest
//import foatto.core.link.XyDocumentClientType
//import kotlinx.browser.document
//import kotlinx.browser.localStorage
//import kotlinx.browser.window
//import org.w3c.dom.HTMLElement
//import kotlin.js.json
//
//const val LOCAL_STORAGE_LOGIN = "login"
//const val LOCAL_STORAGE_PASSWORD = "password"
//
//var compositeResponseCodeControlFun: (compositeResponse: CompositeResponse, tabId: Int) -> VueComponentOptions = { _: CompositeResponse, _: Int -> vueComponentOptions() }
//
//
//    this.methods = json(
//        "doNextFocus" to { curIndex: Int ->
//            val element = document.getElementById("logon_${curIndex + 1}")
//            if (element is HTMLElement) {
//                element.focus()
//            }
//        },
//        "invoke" to { appRequest: AppRequest ->
//            var isAutoClose = false
//            //--- урло с автозакрытием текущей вкладки
//            if ( /*!appRequest.action.isEmpty() && */ appRequest.action.startsWith("#")) {
//                appRequest.action = appRequest.action.substring(1)
//                isAutoClose = true
//            }
//
//            //--- пустое урло, ничего не делаем
//            if (appRequest.action.isEmpty()) {
//                if (isAutoClose) {
//                    that().`$root`.closeTabById(tabId)
//                }
//            }
//            //--- файловое урло
//            else if (appRequest.action.startsWith("/")) {
//                that().`$root`.openTab(appRequest.action)
//                if (isAutoClose) {
//                    that().`$root`.closeTabById(tabId)
//                }
//            } else {
//                //--- для проброса this внутрь лямбд
//                val that = that()
//                that.`$root`.setWait(true)
//                invokeApp(appRequest, { appResponse: AppResponse ->
//                    //--- из-за особенности (ошибки?) сравнения enum-значений, одно из которых берётся из десериализации json-объекта,
//                    //--- используем сравнение .toString() значений
//                    when (appResponse.code.toString()) {
//                        //--- если требуется вход - сохраним последний запрос
//                        ResponseCode.LOGON_NEED.toString() -> {
//                            that.prevRequest = appRequest
//
//                            //--- попробуем автологон
//                            val savedLogin = localStorage.getItem(LOCAL_STORAGE_LOGIN)
//                            val savedPassword = localStorage.getItem(LOCAL_STORAGE_PASSWORD)
//                            if (!savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()) {
//                                val logonRequest = LogonRequest(savedLogin, savedPassword)
//                                fillSystemProperties(logonRequest.hmSystemProperties)
//
//                                that.invoke(AppRequest(action = AppAction.LOGON, logon = logonRequest))
//                            } else {
//                                that.responseCode = appResponse.code
//                                Vue.nextTick {
//                                    val element = document.getElementById("logon_0")
//                                    if (element is HTMLElement) {
//                                        element.focus()
//                                    }
//                                }
//                            }
//                        }
//                        //--- если вход успешен - повторим последний запрос
//                        ResponseCode.LOGON_SUCCESS.toString(), ResponseCode.LOGON_SUCCESS_BUT_OLD.toString() -> {
//                            //                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
//                            //                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
//                            //
//                            that.`$root`.currentUserName = appResponse.currentUserName
//                            appResponse.hmUserProperty!!.forEach {
//                                if (it.first == UP_TIME_OFFSET) {
//                                    //--- на сервере может лежать как в секундах, так и в миллисекундах (старый вариант)
//                                    val timeOffset = it.second.toInt()
//                                    //--- если смещение <= максимально возможного смещения в секундах (43 200 сек), значит оно задано в секундах (логично)
//                                    //--- в противном случае смещение задано в старом варианте - в миллисекундах
//                                    //--- (минимальное значение будет начинаться с 1 час * 60 * 60 * 1000 = 3 600 000 мс, что всяко не совпадает с верхней границей в 43 200 от предущего варианта)
//                                    that.`$root`.timeOffset = if (timeOffset <= 12 * 60 * 60) {
//                                        timeOffset
//                                    } else {
//                                        timeOffset / 1000
//                                    }
//                                }
//                            }
//
//                            that.`$parent`.setMenuBar(menuBar(appResponse.arrMenuData!!))
//
//                            //--- перевызовем сервер с предыдущей (до-логинной) командой
//                            val prevRequest = that.prevRequest.unsafeCast<AppRequest>()
//                            that.invoke(prevRequest)
//                        }
//                        ResponseCode.REDIRECT.toString() -> {
//                            that.invoke(AppRequest(appResponse.redirect!!))
//                        }
//                        ResponseCode.TABLE.toString() -> {
//                            that.curControl = tableControl(appRequest.action, appResponse.table!!, tabId)
//                            that.responseCode = appResponse.code
//                        }
//                        ResponseCode.FORM.toString() -> {
//                            that.curControl = formControl(appResponse.form!!, tabId)
//                            that.responseCode = appResponse.code
//                        }
//                        ResponseCode.GRAPHIC.toString() -> {
//                            that.curControl = graphicControl(appResponse.graphic!!, tabId)
//                            that.responseCode = appResponse.code
//                        }
//                        ResponseCode.XY.toString() -> {
//                            appResponse.xy?.let { xy ->
//                                that.curControl = when (xy.documentConfig.clientType.toString()) {
//                                    XyDocumentClientType.MAP.toString() -> mapControl(xy, tabId)
//                                    XyDocumentClientType.STATE.toString() -> stateControl(xy, tabId)
//                                    else -> mapControl(xy, tabId)
//                                }
//                            }
//                            that.responseCode = appResponse.code
//                        }
////!!!            ResponseCode.VIDEO_ONLINE, ResponseCode.VIDEO_ARCHIVE -> {
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
//                        ResponseCode.COMPOSITE.toString() -> {
//                            that.curControl = compositeResponseCodeControlFun(appResponse.composite!!, tabId)
//                            that.responseCode = appResponse.code
//                        }
//                        else -> {
//                            that.responseCode = appResponse.code
//                        }
//                    }
//                    that.`$root`.setWait(false)
//                })
//            }
//        },
//        "logon" to {
//            val login = that().login.unsafeCast<String>()
//            val password = that().password.unsafeCast<String>()
//            val isRememberMe = that().isRememberMe.unsafeCast<Boolean>()
//
//            val encodedPassword = encodePassword(password)
//
//            if (isRememberMe) {
//                localStorage.setItem(LOCAL_STORAGE_LOGIN, login)
//                localStorage.setItem(LOCAL_STORAGE_PASSWORD, encodedPassword)
//            } else {
//                localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
//                localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")
//            }
//
//            val logonRequest = LogonRequest(login, encodedPassword)
//            fillSystemProperties(logonRequest.hmSystemProperties)
//
//            that().invoke(AppRequest(action = AppAction.LOGON, logon = logonRequest))
//        }
//    )
//
//    this.mounted = {
//        that().invoke(AppRequest(action = startAppParam))
//    }
//
//    this.data = {
//        json(
//            "login" to "",
//            "password" to "",
//            "isRememberMe" to false,
//            "prevRequest" to null,
//            "curControl" to null,
//
//            "style_logon_div" to json(
//                "font-size" to styleControlTextFontSize(),
//                "padding" to styleLogonControlPadding
//            ),
//            "style_logon_input" to json(
//                "background" to COLOR_MAIN_BACK_0,
//                "border" to "1px solid ${colorMainBorder()}",
//                "border-radius" to styleInputBorderRadius,
//                "font-size" to styleControlTextFontSize(),
//                "padding" to styleCommonEditorPadding()
//            ),
//            "style_logon_checkbox" to json(
//                "appearance" to "none",
//                "width" to styleCheckBoxWidth,
//                "height" to styleCheckBoxHeight,
//                "border" to styleCheckBoxBorder(),
//                "border-radius" to styleInputBorderRadius,
//                "margin" to styleLogonCheckBoxMargin
//            ),
//            "style_logon_button" to json(
//                "background" to colorLogonButtonBack(),
//                "color" to colorLogonButtonText,
//                "border" to "1px solid ${colorLogonButtonBorder()}",
//                "border-radius" to styleButtonBorderRadius,
//                "font-size" to styleCommonButtonFontSize(),
//                "font-weight" to styleLogonButtonFontWeight,
//                "padding" to styleLogonButtonPadding(),
//                "margin" to styleLogonButtonMargin,
//                "cursor" to "pointer"
//            )
//        )
//    }
//}
//
//private fun fillSystemProperties(hmSystemProperties: MutableMap<String, String>) {
//    hmSystemProperties["k.b.w.devicePixelRatio"] = window.devicePixelRatio.toString()
//    hmSystemProperties["k.b.w.appCodeName"] = window.navigator.appCodeName
//    hmSystemProperties["k.b.w.appName"] = window.navigator.appName
//    hmSystemProperties["k.b.w.appVersion"] = window.navigator.appVersion
//    hmSystemProperties["k.b.w.language"] = window.navigator.language
//    hmSystemProperties["k.b.w.platform"] = window.navigator.platform
//    hmSystemProperties["k.b.w.product"] = window.navigator.product
//    hmSystemProperties["k.b.w.productSub"] = window.navigator.productSub
//    hmSystemProperties["k.b.w.userAgent"] = window.navigator.userAgent
//    hmSystemProperties["k.b.w.vendor"] = window.navigator.vendor
//    hmSystemProperties["k.b.w.width"] = window.screen.width.toString()
//    hmSystemProperties["k.b.w.height"] = window.screen.height.toString()
//    hmSystemProperties["k.b.w.availWidth"] = window.screen.availWidth.toString()
//    hmSystemProperties["k.b.w.availHeight"] = window.screen.availHeight.toString()
//}
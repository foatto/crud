package foatto.core_compose_web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.LogonRequest
import foatto.core.link.ResponseCode
import foatto.core_compose_web.control.EmptyControl
import foatto.core_compose_web.control.FormControl
import foatto.core_compose_web.control.TableControl
import foatto.core_compose_web.control.iControl
import foatto.core_compose_web.link.invokeApp
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.encodePassword
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.size
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.properties.appearance
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Text
import org.w3c.dom.HTMLElement

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

const val LOCAL_STORAGE_LOGIN: String = "login"
const val LOCAL_STORAGE_PASSWORD: String = "password"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

private val arrStyleAppControlPaddings: Array<CSSSize> = arrayOf(0.cssRem, 0.cssRem, 0.cssRem, 0.cssRem)

//--- LOGON ----------------------------------------------------------------------------------------------------------------------------------------------------

var getColorLogonBackAround: () -> CSSColorValue = { colorMainBack1 }
var getColorLogonBackCenter: () -> CSSColorValue = { colorMainBack2 }
var getColorLogonBorder: () -> CSSColorValue = { colorMainBorder }
var getColorLogonButtonBack: () -> CSSColorValue = { getColorButtonBack() }
var colorLogonButtonText: CSSColorValue = COLOR_MAIN_TEXT
var getColorLogonButtonBorder: () -> CSSColorValue = { getColorButtonBorder() }

var styleLogonTopExpanderContent: String = "&nbsp;"
var styleLogonLogo: String = "logo.png"
var styleLogonLogoContent: String = ""

private val arrStyleLogonCellPadding: Array<CSSSize> = arrayOf(
    (if (!styleIsNarrowScreen) 2.0 else 1.0).cssRem,
    2.0.cssRem,
    (if (!styleIsNarrowScreen) 2.0 else 1.0).cssRem,
    2.0.cssRem,
)
private val arrStyleLogonLogoPadding: Array<CSSSize> = arrayOf(0.4.cssRem, 0.cssRem, 1.0.cssRem, 0.cssRem)
private val arrStyleLogonControlPadding: Array<CSSSize> = arrayOf(0.4.cssRem, 0.cssRem, 0.4.cssRem, 0.cssRem)
private val arrStyleLogonCheckBoxMargin: Array<CSSSize> = arrayOf(0.cssRem, 0.5.cssRem, 0.cssRem, 0.cssRem)
var getStyleLogonButtonPaddings: () -> Array<CSSSize> = {
    arrayOf(
        1.0.cssRem,
        (if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48).cssRem,
        1.0.cssRem,
        (if (!styleIsNarrowScreen) 8 else scaledScreenWidth / 48).cssRem,
    )
}
private val arrStyleLogonButtonMargin: Array<CSSSize> = arrayOf(1.0.cssRem, 0.cssRem, (if (!styleIsNarrowScreen) 1.0 else 0.0).cssRem, 0.cssRem)

private fun getStyleLogonTextLen() = if (!styleIsNarrowScreen) {
    40
} else {
    scaledScreenWidth / 16
}

var styleLogonButtonText: String = "Вход"
var styleLogonButtonFontWeight: String = "normal"

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

//var compositeResponseCodeControlFun: (compositeResponse: CompositeResponse, tabId: Int) -> VueComponentOptions = { _: CompositeResponse, _: Int -> vueComponentOptions() }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

class AppControl(
    private val root: Root,
    private val startAppParam: String,
    private val tabId: Int,
) {

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private val responseCode = mutableStateOf(ResponseCode.LOGON_NEED)
    private val curControl = mutableStateOf<iControl>(EmptyControl())

    private val login = mutableStateOf("")
    private val password = mutableStateOf("")
    private val isRememberMe = mutableStateOf(false)

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private lateinit var prevRequest: AppRequest

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

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
                    setPaddings(arrStyleAppControlPaddings)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                }
            }
        ) {
            when (responseCode.value) {
                ResponseCode.LOGON_NEED, ResponseCode.LOGON_FAILED -> {
                    Div(
                        attrs = {
                            style {
                                height(100.percent)
                                display(DisplayStyle.Grid)
                                gridTemplateRows("1fr auto 1fr")
                                gridTemplateColumns("1fr auto 1fr")
                                backgroundColor(getColorLogonBackAround())
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
                            Text(styleLogonTopExpanderContent)
                        }
                        Div(
                            attrs = {
                                style {
                                    gridArea("2", "2", "3", "3")
                                    setPaddings(arrStyleLogonCellPadding)
                                    setBorder(color = getColorLogonBorder(), radius = styleFormBorderRadius)
                                    backgroundColor(getColorLogonBackCenter())
                                    display(DisplayStyle.Flex)
                                    flexDirection(FlexDirection.Column)
                                    alignItems(AlignItems.Center)
                                }
                            }
                        ) {
                            Div(
                                attrs = {
                                    style {
                                        setPaddings(arrStyleLogonLogoPadding)
                                    }
                                }
                            ) {
                                Img("/web/images/$styleLogonLogo")
                            }
                            Text(styleLogonLogoContent)
                            if (responseCode.value == ResponseCode.LOGON_FAILED) {
                                Div(
                                    attrs = {
                                        style {
                                            alignSelf(AlignSelf.Center)
                                            fontSize(styleControlTextFontSize)
                                            color(Color.red)
                                        }
                                    }
                                ) {
                                    Text("Неправильное имя или пароль")
                                }
                            }
                            Div(
                                attrs = {
                                    style {
                                        setPaddings(arrStyleLogonControlPadding)
                                        fontSize(styleControlTextFontSize)
                                    }
                                }
                            ) {
                                Input(InputType.Text) {
                                    style {
                                        backgroundColor(COLOR_MAIN_BACK_0)
                                        setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                                        fontSize(styleControlTextFontSize)
                                        padding(styleCommonEditorPadding)
                                    }
                                    id("logon_0")
                                    size(getStyleLogonTextLen())
                                    placeholder("Имя")
                                    value(login.value)
                                    onInput { event ->
                                        login.value = event.value
                                    }
                                    onKeyUp { event ->
                                        if (event.key == "Enter") {
                                            doNextFocus(0)
                                        }
                                    }
                                }
                            }
                            Div(
                                attrs = {
                                    style {
                                        setPaddings(arrStyleLogonControlPadding)
                                        fontSize(styleControlTextFontSize)
                                    }
                                }
                            ) {
                                Input(InputType.Password) {
                                    style {
                                        backgroundColor(COLOR_MAIN_BACK_0)
                                        setBorder(color = colorMainBorder, radius = styleInputBorderRadius)
                                        fontSize(styleControlTextFontSize)
                                        padding(styleCommonEditorPadding)
                                    }
                                    id("logon_1")
                                    size(getStyleLogonTextLen())
                                    placeholder("Пароль")
                                    value(password.value)
                                    onInput { event ->
                                        password.value = event.value
                                    }
                                    onKeyUp { event ->
                                        if (event.key == "Enter") {
                                            doNextFocus(1)
                                        }
                                    }
                                }
                            }
                            Div(
                                attrs = {
                                    style {
                                        setPaddings(arrStyleLogonControlPadding)
                                        fontSize(styleControlTextFontSize)
                                        alignSelf(AlignSelf.FlexStart)
                                        color(COLOR_MAIN_TEXT)
                                        display(DisplayStyle.Flex)
                                        alignItems(AlignItems.Center)
                                    }
                                }
                            ) {
                                Input(InputType.Checkbox) {
                                    style {
                                        appearance("none")
                                        width(styleCheckBoxWidth)
                                        height(styleCheckBoxHeight)
                                        setBorder(getStyleCheckBoxBorder())
                                        setMargins(arrStyleLogonCheckBoxMargin)
                                    }
                                    id("logon_2")
                                    checked(isRememberMe.value)
                                    //onInput { syntheticInputEvent -> - тоже можно
                                    onChange { syntheticChangeEvent ->
                                        isRememberMe.value = syntheticChangeEvent.value
                                    }
                                    onKeyUp { event ->
                                        if (event.key == "Enter") {
                                            doNextFocus(2)
                                        }
                                    }
                                }
                                Text("Запомнить меня")
                            }
                            Div(
                                attrs = {
                                    style {
                                        setPaddings(arrStyleLogonControlPadding)
                                        fontSize(styleControlTextFontSize)
                                    }
                                }
                            ) {
                                Button(
                                    attrs = {
                                        style {
                                            backgroundColor(getColorLogonButtonBack())
                                            color(colorLogonButtonText)
                                            setBorder(color = getColorLogonButtonBorder(), radius = styleButtonBorderRadius)
                                            fontSize(styleCommonButtonFontSize)
                                            fontWeight(styleLogonButtonFontWeight)
                                            setPaddings(getStyleLogonButtonPaddings())
                                            setMargins(arrStyleLogonButtonMargin)
                                            cursor("pointer")
                                        }
                                        id("logon_3")
                                        onClick {
                                            logon()
                                        }
                                    }
                                ) {
                                    Text(styleLogonButtonText)
                                }
                            }
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
                            Text("&nbsp;")
                        }
                    }
                }

                ResponseCode.LOGON_SYSTEM_BLOCKED -> {
//                "Ошибка входа в систему",
                    Text("Слишком много неудачных попыток входа. \nПользователь временно заблокирован. \nПопробуйте войти попозже.")
                }

                ResponseCode.LOGON_ADMIN_BLOCKED -> {
//                "Ошибка входа в систему",
                    Text("Пользователь заблокирован администратором.")
                }

                ResponseCode.TABLE, ResponseCode.FORM, ResponseCode.GRAPHIC, ResponseCode.XY, ResponseCode.COMPOSITE -> {
                    curControl.value.getBody()
                }

                else -> {
                    Text("Unknown response code: ${responseCode.value}")
                }

            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    fun start() {
        call(AppRequest(action = startAppParam))
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

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
                //--- из-за особенности (ошибки?) сравнения enum-значений, одно из которых берётся из десериализации json-объекта,
                //--- используем сравнение .toString() значений
                when (appResponse.code.toString()) {
                    //--- если требуется вход - сохраним последний запрос
                    ResponseCode.LOGON_NEED.toString() -> {
                        prevRequest = appRequest

                        //--- попробуем автологон
                        val savedLogin = localStorage.getItem(LOCAL_STORAGE_LOGIN)
                        val savedPassword = localStorage.getItem(LOCAL_STORAGE_PASSWORD)
                        if (!savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()) {
                            val logonRequest = LogonRequest(savedLogin, savedPassword)
                            fillSystemProperties(logonRequest.hmSystemProperties)

                            call(AppRequest(action = AppAction.LOGON, logon = logonRequest))
                        } else {
                            responseCode.value = appResponse.code
                            val element = document.getElementById("logon_0")
                            if (element is HTMLElement) {
                                element.focus()
                            }
                        }
                    }
                    //--- если вход успешен - повторим последний запрос
                    ResponseCode.LOGON_SUCCESS.toString(), ResponseCode.LOGON_SUCCESS_BUT_OLD.toString() -> {
//                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
//                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
//
                        root.currentUserName = appResponse.currentUserName
                        appResponse.hmUserProperty!!.forEach {
                            if (it.first == UP_TIME_OFFSET) {
                                //--- на сервере может лежать как в секундах, так и в миллисекундах (старый вариант)
                                val timeOffset = it.second.toInt()
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
                        root.setMenuBarData(appResponse.arrMenuData!!)
                        //--- перевызовем сервер с предыдущей (до-логинной) командой
                        call(prevRequest)
                    }

                    ResponseCode.REDIRECT.toString() -> {
                        call(AppRequest(appResponse.redirect!!))
                    }

                    ResponseCode.TABLE.toString() -> {
                        val tableControl = TableControl(root, this, appRequest.action, appResponse.table!!, tabId)
                        curControl.value = tableControl
                        responseCode.value = appResponse.code

                        tableControl.start()
                    }

                    ResponseCode.FORM.toString() -> {
                        val formControl = FormControl(root, this, appResponse.form!!, tabId)
                        curControl.value = formControl
                        responseCode.value = appResponse.code

                        formControl.start()
                    }

                    ResponseCode.GRAPHIC.toString() -> {
//                        curControl.value = graphicControl(appResponse.graphic!!, tabId)
                        responseCode.value = appResponse.code
                    }

                    ResponseCode.XY.toString() -> {
//                        appResponse.xy?.let { xy ->
//                            curControl.value = when (xy.documentConfig.clientType.toString()) {
//                                XyDocumentClientType.MAP.toString() -> mapControl(xy, tabId)
//                                XyDocumentClientType.STATE.toString() -> stateControl(xy, tabId)
//                                else -> mapControl(xy, tabId)
//                            }
//                        }
                        responseCode.value = appResponse.code
                    }
//!!!            ResponseCode.VIDEO_ONLINE, ResponseCode.VIDEO_ARCHIVE -> {
//                val vcstartParamId = bbIn.getShortString()
//                val vcStartTitle = bbIn.getShortString()
//
//                val videoControl = if( curResponseCode == ResponseCode.VIDEO_ONLINE.toInt() ) VideoOnlineControl()
//                else VideoArchiveControl()
//                addPanes( videoControl )
//
//                //--- init работает совместно с read
//                videoControl.init( appContainer, appLink, tab, this, vcstartParamId, vcStartTitle )
//            }
                    ResponseCode.COMPOSITE.toString() -> {
//                        curControl.value = compositeResponseCodeControlFun(appResponse.composite!!, tabId)
                        responseCode.value = appResponse.code
                    }

                    else -> {
                        responseCode.value = appResponse.code
                    }
                }
                root.setWait(false)
            }
        }
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

    private fun doNextFocus(curIndex: Int) {
        val element = document.getElementById("logon_${curIndex + 1}")
        if (element is HTMLElement) {
            element.focus()
        }
    }

    private fun logon() {
        val encodedPassword = encodePassword(password.value)

        if (isRememberMe.value) {
            localStorage.setItem(LOCAL_STORAGE_LOGIN, login.value)
            localStorage.setItem(LOCAL_STORAGE_PASSWORD, encodedPassword)
        } else {
            localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
            localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")
        }

        val logonRequest = LogonRequest(login.value, encodedPassword)
        fillSystemProperties(logonRequest.hmSystemProperties)

        call(AppRequest(action = AppAction.LOGON, logon = logonRequest))
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

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

}

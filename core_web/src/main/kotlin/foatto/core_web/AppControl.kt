package foatto.core_web

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.AppAction
import foatto.core.link.AppRequest
import foatto.core.link.AppResponse
import foatto.core.link.CompositeResponse
import foatto.core.link.LogonRequest
import foatto.core.link.ResponseCode
import foatto.core.link.XyDocumentClientType
import foatto.core_web.external.vue.Vue
import foatto.core_web.external.vue.VueComponentOptions
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.js.json

const val LOCAL_STORAGE_LOGIN = "login"
const val LOCAL_STORAGE_PASSWORD = "password"

var compositeResponseCodeControlFun: (compositeResponse: CompositeResponse, tabId: Int) -> VueComponentOptions = { _: CompositeResponse, _: Int -> vueComponentOptions() }

@Suppress("UnsafeCastFromDynamic")
fun appControl(startAppParam: String, tabId: Int) = vueComponentOptions().apply {

    this.template = """
        <div v-bind:style="style_app_control">
            <div v-if="responseCode == '${ResponseCode.LOGON_NEED}' || responseCode == '${ResponseCode.LOGON_FAILED}'"
                 v-bind:style="style_logon_grid"
            >
                <div v-bind:style="style_logon_top_expander">
                    $styleLogonTopExpanderContent
                </div>
                <div v-bind:style="style_logon_cell">
                    <div v-bind:style="style_logon_logo">
                        <img src="/web/images/$styleLogonLogo">
                    </div>
                    $styleLogonLogoContent                    
                    <div v-if="responseCode == '${ResponseCode.LOGON_FAILED}'"
                         v-bind:style="style_logon_error"
                    >
                        Неправильное имя или пароль
                    </div>
                    <div v-bind:style="style_logon_div">
                        <input type="text"
                               v-model="login" 
                               v-on:keyup.enter.exact="doNextFocus(0)"
                               v-bind:style="style_logon_input"
                               id="logon_0" 
                               size="${styleLogonTextLen()}" 
                               placeholder="Имя"
                        >
                    </div>
                    <div v-bind:style="style_logon_div">
                        <input type="password"
                               v-model="password" 
                               v-on:keyup.enter.exact="doNextFocus(1)"
                               v-bind:style="style_logon_input" 
                               id="logon_1" 
                               size="${styleLogonTextLen()}" 
                               placeholder="Пароль"
                        >
                    </div>
                    <div v-bind:style="[ 
                            style_logon_div, 
                            { 
                                'align-self' : 'flex-start' , 
                                'color' : '$COLOR_MAIN_TEXT',
                                'display' : 'flex' , 
                                'align-items' : 'center' 
                            } 
                        ]"
                    >
                        <input type="checkbox"
                               v-model="isRememberMe" 
                               v-on:keyup.enter.exact="doNextFocus(2)"
                               v-bind:style="style_logon_checkbox" 
                               id="logon_2" 
                        >
                            Запомнить меня
                        </input>
                    </div>
                    <div v-bind:style="style_logon_div">
                        <button v-on:click="logon"
                                v-bind:style="style_logon_button"
                                id="logon_3" 
                        >
                            $styleLogonButtonText
                        </button>
                    </div>
                </div>
                <div v-bind:style="style_logon_bottom_expander">
                    &nbsp;
                </div>
            </div>
            <div v-else-if="responseCode == '${ResponseCode.LOGON_SYSTEM_BLOCKED}'">
                "Ошибка входа в систему", "Слишком много неудачных попыток входа. \nПользователь временно заблокирован. \nПопробуйте войти попозже."
            </div>
            <div v-else-if="responseCode == '${ResponseCode.LOGON_ADMIN_BLOCKED}'">
                "Ошибка входа в систему", "Пользователь заблокирован администратором."
            </div>
            <component v-else-if="responseCode == '${ResponseCode.TABLE}' || 
                                  responseCode == '${ResponseCode.FORM}' ||
                                  responseCode == '${ResponseCode.GRAPHIC}' || 
                                  responseCode == '${ResponseCode.XY}' ||
                                  responseCode == '${ResponseCode.COMPOSITE}'"
                       v-bind:is="curControl">
            </component>
            <div v-else>
                <!-- Unknown response code: --> {{responseCode}}
            </div>
        </div>
    """
    this.methods = json(
        "doNextFocus" to { curIndex: Int ->
            val element = document.getElementById("logon_${curIndex + 1}")
            if (element is HTMLElement) {
                element.focus()
            }
        },
        "invoke" to { appRequest: AppRequest ->
            var isAutoClose = false
            //--- урло с автозакрытием текущей вкладки
            if ( /*!appRequest.action.isEmpty() && */ appRequest.action.startsWith("#")) {
                appRequest.action = appRequest.action.substring(1)
                isAutoClose = true
            }

            //--- пустое урло, ничего не делаем
            if (appRequest.action.isEmpty()) {
                if (isAutoClose) {
                    that().`$root`.closeTabById(tabId)
                }
            }
            //--- файловое урло
            else if (appRequest.action.startsWith("/")) {
                that().`$root`.openTab(appRequest.action)
                if (isAutoClose) {
                    that().`$root`.closeTabById(tabId)
                }
            } else {
                //--- для проброса this внутрь лямбд
                val that = that()
                that.`$root`.setWait(true)
                invokeApp(appRequest, { appResponse: AppResponse ->
                    //--- из-за особенности (ошибки?) сравнения enum-значений, одно из которых берётся из десериализации json-объекта,
                    //--- используем сравнение .toString() значений
                    when (appResponse.code.toString()) {
                        //--- если требуется вход - сохраним последний запрос
                        ResponseCode.LOGON_NEED.toString() -> {
                            that.prevRequest = appRequest

                            //--- попробуем автологон
                            val savedLogin = localStorage.getItem(LOCAL_STORAGE_LOGIN)
                            val savedPassword = localStorage.getItem(LOCAL_STORAGE_PASSWORD)
                            if (!savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty()) {
                                val logonRequest = LogonRequest(savedLogin, savedPassword)
                                fillSystemProperties(logonRequest.hmSystemProperties)

                                that.invoke(AppRequest(action = AppAction.LOGON, logon = logonRequest))
                            } else {
                                that.responseCode = appResponse.code
                                Vue.nextTick {
                                    val element = document.getElementById("logon_0")
                                    if (element is HTMLElement) {
                                        element.focus()
                                    }
                                }
                            }
                        }
                        //--- если вход успешен - повторим последний запрос
                        ResponseCode.LOGON_SUCCESS.toString(), ResponseCode.LOGON_SUCCESS_BUT_OLD.toString() -> {
                            //                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
                            //                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
                            //
                            that.`$root`.currentUserName = appResponse.currentUserName
                            appResponse.hmUserProperty!!.forEach {
                                if (it.first == UP_TIME_OFFSET) {
                                    //--- на сервере может лежать как в секундах, так и в миллисекундах (старый вариант)
                                    val timeOffset = it.second.toInt()
                                    //--- если смещение <= максимально возможного смещения в секундах (43 200 сек), значит оно задано в секундах (логично)
                                    //--- в противном случае смещение задано в старом варианте - в миллисекундах
                                    //--- (минимальное значение будет начинаться с 1 час * 60 * 60 * 1000 = 3 600 000 мс, что всяко не совпадает с верхней границей в 43 200 от предущего варианта)
                                    that.`$root`.timeOffset = if (timeOffset <= 12 * 60 * 60) {
                                        timeOffset
                                    } else {
                                        timeOffset / 1000
                                    }
                                }
                            }

                            that.`$parent`.setMenuBar(menuBar(appResponse.arrMenuData!!))

                            //--- перевызовем сервер с предыдущей (до-логинной) командой
                            val prevRequest = that.prevRequest.unsafeCast<AppRequest>()
                            that.invoke(prevRequest)
                        }
                        ResponseCode.REDIRECT.toString() -> {
                            that.invoke(AppRequest(appResponse.redirect!!))
                        }
                        ResponseCode.TABLE.toString() -> {
                            that.curControl = tableControl(appRequest.action, appResponse.table!!, tabId)
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.FORM.toString() -> {
                            that.curControl = formControl(appResponse.form!!, tabId)
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.GRAPHIC.toString() -> {
                            that.curControl = graphicControl(appResponse.graphic!!, tabId)
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.XY.toString() -> {
                            appResponse.xy?.let { xy ->
                                that.curControl = when (xy.documentConfig.clientType.toString()) {
                                    XyDocumentClientType.MAP.toString() -> mapControl(xy, tabId)
                                    XyDocumentClientType.STATE.toString() -> stateControl(xy, tabId)
                                    else -> mapControl(xy, tabId)
                                }
                            }
                            that.responseCode = appResponse.code
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
                            that.curControl = compositeResponseCodeControlFun(appResponse.composite!!, tabId)
                            that.responseCode = appResponse.code
                        }
                        else -> {
                            that.responseCode = appResponse.code
                        }
                    }
                    that.`$root`.setWait(false)
                })
            }
        },
        "logon" to {
            val login = that().login.unsafeCast<String>()
            val password = that().password.unsafeCast<String>()
            val isRememberMe = that().isRememberMe.unsafeCast<Boolean>()

            val encodedPassword = encodePassword(password)

            if (isRememberMe) {
                localStorage.setItem(LOCAL_STORAGE_LOGIN, login)
                localStorage.setItem(LOCAL_STORAGE_PASSWORD, encodedPassword)
            } else {
                localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
                localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")
            }

            val logonRequest = LogonRequest(login, encodedPassword)
            fillSystemProperties(logonRequest.hmSystemProperties)

            that().invoke(AppRequest(action = AppAction.LOGON, logon = logonRequest))
        }
    )

    this.mounted = {
        that().invoke(AppRequest(action = startAppParam))
    }

    this.data = {
        json(
            "responseCode" to "",
            "login" to "",
            "password" to "",
            "isRememberMe" to false,
            "prevRequest" to null,
            "curControl" to null,

            "style_app_control" to json(
                "flex-grow" to 1,
                "flex-shrink" to 1,
                //!!! Непонятный баг со вложенностью контейнеров,
                //--- при установке height="100%" уходит за экран нижняя панель с кнопками страниц в таблице или кнопками формы.
                //--- Если определение height убрать, то пропадает прокрутка вообще.
                //--- Работает, если установить высоту в диапазон от 0% до примерно 80%, оставлю 1% на всякий случай
                //--- (неизвестно как потенциально сглючит 0% высоты в будущем)
                "height" to "1%",
                "display" to "flex",
                "flex-direction" to "column",
                "padding" to styleAppControlPadding(),
            ),
            "style_logon_grid" to json(
                "height" to "100%",
                "display" to "grid",
                "grid-template-rows" to "1fr auto 1fr",
                "grid-template-columns" to "1fr auto 1fr",
                "background" to colorLogonBackAround(),
                "background-size" to "cover",   // for background-image
            ),
            "style_logon_top_expander" to json(
                "grid-area" to "1 / 1 / 2 / 4",
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "center",
                "justify-content" to "space-evenly",
                "text-align" to "center",
            ),
            "style_logon_cell" to json(
                "grid-area" to "2 / 2 / 3 / 3",
                "padding" to styleLogonCellPadding,
                "border" to "1px solid ${colorLogonBorder()}",
                "border-radius" to styleFormBorderRadius,
                "background" to colorLogonBackCenter(),
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "center"
            ),
            "style_logon_bottom_expander" to json(
                "grid-area" to "3 / 1 / 4 / 4",
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "center",
                "justify-content" to "space-evenly",
                "text-align" to "center",
            ),
            "style_logon_logo" to json(
                "padding" to styleLogonLogoPadding
            ),
            "style_logon_error" to json(
                "align-self" to "center",
                "font-size" to styleControlTextFontSize(),
                "color" to "red"
            ),
            "style_logon_div" to json(
                "font-size" to styleControlTextFontSize(),
                "padding" to styleLogonControlPadding
            ),
            "style_logon_input" to json(
                "background" to COLOR_MAIN_BACK_0,
                "border" to "1px solid ${colorMainBorder()}",
                "border-radius" to styleInputBorderRadius,
                "font-size" to styleControlTextFontSize(),
                "padding" to styleCommonEditorPadding()
            ),
            "style_logon_checkbox" to json(
                "appearance" to "none",
                "width" to styleCheckBoxWidth,
                "height" to styleCheckBoxHeight,
                "border" to styleCheckBoxBorder(),
                "border-radius" to styleInputBorderRadius,
                "margin" to styleLogonCheckBoxMargin
            ),
            "style_logon_button" to json(
                "background" to colorLogonButtonBack(),
                "color" to colorLogonButtonText,
                "border" to "1px solid ${colorLogonButtonBorder()}",
                "border-radius" to styleButtonBorderRadius,
                "font-size" to styleCommonButtonFontSize(),
                "font-weight" to styleLogonButtonFontWeight,
                "padding" to styleLogonButtonPadding(),
                "margin" to styleLogonButtonMargin,
                "cursor" to "pointer"
            )
        )
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
package foatto.core_web

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.*
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlin.js.json

const val LOCAL_STORAGE_LOGIN = "login"
const val LOCAL_STORAGE_PASSWORD = "password"

@Suppress("UnsafeCastFromDynamic")
fun appControl( startAppParam: String, tabIndex: Int ) = vueComponentOptions().apply {

    this.template = """
        <div v-bind:style="style_app_control">
            <div v-if="responseCode == '${ResponseCode.LOGON_NEED}' || responseCode == '${ResponseCode.LOGON_FAILED}'"
                 v-bind:style="style_logon_grid">

                <div v-bind:style="style_logon_top_expander">
                    &nbsp;
                </div>
                <div v-bind:style="style_logon_cell">
                    <div v-bind:style="style_logon_logo">
                        <img src="/web/images/logo.png">
                    </div>
                    <div v-if="responseCode == '${ResponseCode.LOGON_FAILED}'"
                         v-bind:style="style_logon_error">
                        Неправильное имя или пароль
                    </div>
                    <div v-bind:style="style_logon_div">
                        <input v-bind:style="style_logon_input" v-model="login" size="${styleLogonTextLen()}" type="text" placeholder="Имя">
                    </div>
                    <div v-bind:style="style_logon_div">
                        <input v-bind:style="style_logon_input" v-model="password" size="${styleLogonTextLen()}" type="password" placeholder="Пароль">
                    </div>
                    <div v-bind:style="[ style_logon_div, { 'align-self': 'flex-start' } ]">
                        <input v-bind:style="style_logon_checkbox" v-model="isRememberMe" type="checkbox">
                            Запомнить меня
                        </input>
                    </div>
                    <div v-bind:style="style_logon_div">
                        <button v-bind:style="style_logon_button"
                                v-on:click="logon">
                            Вход
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
            <component v-else-if="responseCode == '${ResponseCode.TABLE}' || responseCode == '${ResponseCode.FORM}' ||
                                  responseCode == '${ResponseCode.GRAPHIC}' || responseCode == '${ResponseCode.XY}'"
                       v-bind:is="curControl">
            </component>
            <div v-else>
                <!-- Unknown response code: --> {{responseCode}}
            </div>
        </div>
    """
    this.methods = json(
        "invoke" to { appRequest: AppRequest ->
            var isAutoClose = false
            //--- урло с автозакрытием текущей вкладки
            if( /*!appRequest.action.isEmpty() && */ appRequest.action.startsWith( "#" ) ) {
                appRequest.action = appRequest.action.substring( 1 )
                isAutoClose = true
            }

            //--- пустое урло, ничего не делаем
            if( appRequest.action.isEmpty() ) {
                if( isAutoClose ) that().`$root`.closeTab( tabIndex )
            }
            //--- файловое урло
            else if( appRequest.action.startsWith( "/" ) ) {
                that().`$root`.openTab( appRequest.action )
                if( isAutoClose ) that().`$root`.closeTab( tabIndex )
            }
            else {
                //--- для проброса this внутрь лямбд
                val that = that()
                that.`$root`.setWait( true )
                invokeApp( appRequest, { appResponse: AppResponse ->
                    //--- из-за особенности (ошибки?) сравнения enum-значений, одно из которых берётся из десериализации json-объекта,
                    //--- используем сравнение .toString() значений
                    when( appResponse.code.toString() ) {
                        //--- если требуется вход - сохраним последний запрос
                        ResponseCode.LOGON_NEED.toString() -> {
                            that.prevRequest = appRequest

                            //--- попробуем автологон
                            val localStorage = kotlin.browser.localStorage
                            val savedLogin = localStorage.getItem( LOCAL_STORAGE_LOGIN )
                            val savedPassword = localStorage.getItem( LOCAL_STORAGE_PASSWORD )
                            if( !savedLogin.isNullOrBlank() && !savedPassword.isNullOrEmpty() ) {
                                val logonRequest = LogonRequest( savedLogin, savedPassword )
                                fillSystemProperties( logonRequest.hmSystemProperties )

                                that.invoke( AppRequest( action = AppAction.LOGON, logon = logonRequest ) )
                            }
                            else that.responseCode = appResponse.code
                        }
                        //--- если вход успешен - повторим последний запрос
                        ResponseCode.LOGON_SUCCESS.toString(), ResponseCode.LOGON_SUCCESS_BUT_OLD.toString() -> {
    //                            if( appResponse.code == Code.LOGON_SUCCESS_BUT_OLD )
    //                                showWarning( "Система безопасности", "Срок действия пароля истек.\nПожалуйста, смените пароль." )
    //
                            appResponse.hmUserProperty!!.forEach {
                                if( it.first == UP_TIME_OFFSET ) {
                                    //--- на сервере может лежать как в секунад, так и в миллисекундах (старый вариант)
                                    val timeOffset = it.second.toInt()
                                    //--- если смещение <= максимально возможного смещения в секундах (43 200 сек), значит оно задано в секундах (логично)
                                    //--- в противном случае смещение задано в старом варианте - в миллисекундах
                                    //--- (минимальное значение будет начинаться с 1 час * 60 * 60 * 1000 = 3 600 000 мс, что всяко не совпадает с верхней границей в 43 200 от предущего варианта)
                                    that.`$root`.timeOffset = if(timeOffset <= 12 * 60 * 60) timeOffset else timeOffset / 1000
                                }
                            }

                            that.`$parent`.setMenuBar( menuBar( appResponse.alMenuData!! ) )

                            //--- перевызовем сервер с предыдущей (до-логинной) командой
                            val prevRequest = that.prevRequest.unsafeCast<AppRequest>()
                            that.invoke( prevRequest )
                        }
                        ResponseCode.REDIRECT.toString() -> {
                            that.invoke( AppRequest( appResponse.redirect!! ) )
                        }
                        ResponseCode.TABLE.toString() -> {
                            that.curControl = tableControl( appRequest.action, appResponse.table!!, tabIndex )
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.FORM.toString() -> {
                            that.curControl = formControl( appResponse.form!!, tabIndex )
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.GRAPHIC.toString() -> {
                            that.curControl = graphicControl( appResponse.graphic!!, tabIndex )
                            that.responseCode = appResponse.code
                        }
                        ResponseCode.XY.toString() -> {
                            that.curControl = when( appResponse.xy!!.documentConfig.clientType.toString() ) {
                                XyDocumentClientType.MAP.toString() -> mapControl( appResponse.xy, tabIndex )
                                XyDocumentClientType.STATE.toString() -> stateControl( appResponse.xy, tabIndex )
                                else -> mapControl( appResponse.xy, tabIndex )
                            }
                            that.responseCode = appResponse.code
                        }
//!!!            ResponseCode.VIDEO_ONLINE, ResponseCode.VIDEO_ARCHIVE -> {
//                val vcStartParamID = bbIn.getShortString()
//                val vcStartTitle = bbIn.getShortString()
//
//                val videoControl = if( curResponseCode == ResponseCode.VIDEO_ONLINE.toInt() ) VideoOnlineControl()
//                else VideoArchiveControl()
//                addPanes( videoControl )
//
//                //--- init работает совместно с read
//                videoControl.init( appContainer, appLink, tab, this, vcStartParamID, vcStartTitle )
//            }
                        else -> {
                            that.responseCode = appResponse.code
                        }
                    }
                    that.`$root`.setWait( false )
                } )
            }
        },
        "logon" to {
            val login = that().login.unsafeCast<String>()
            val password = that().password.unsafeCast<String>()
            val isRememberMe = that().isRememberMe.unsafeCast<Boolean>()

            val encodedPassword = encodePassword( password )

            val localStorage = kotlin.browser.localStorage
            if( isRememberMe ) {
                localStorage.setItem( LOCAL_STORAGE_LOGIN, login )
                localStorage.setItem( LOCAL_STORAGE_PASSWORD, encodedPassword )
            }
            else {
                localStorage.setItem( LOCAL_STORAGE_LOGIN, "" )
                localStorage.setItem( LOCAL_STORAGE_PASSWORD, "" )
            }

            val logonRequest = LogonRequest( login, encodedPassword )
            fillSystemProperties( logonRequest.hmSystemProperties )

            that().invoke( AppRequest( action = AppAction.LOGON, logon = logonRequest ) )
        }
    )

    this.mounted = {
        that().invoke( AppRequest( action = startAppParam ) )
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
                "flex-direction" to "column"
            ),
            "style_logon_grid" to json (
                "height" to "100%",
                "display" to "grid",
                "grid-template-rows" to "1fr auto 1fr",
                "grid-template-columns" to "1fr auto 1fr",
                "background" to COLOR_PANEL_BACK
            ),
            "style_logon_top_expander" to json (
                "grid-area" to "1 / 2 / 2 / 3"
            ),
            "style_logon_cell" to json (
                "grid-area" to "2 / 2 / 3 / 3",
                "padding" to styleLogonCellPadding(),
                "border" to "1px solid $COLOR_MENU_BORDER",
                "border-radius" to BORDER_RADIUS,
                "background" to COLOR_LOGON_BACK,
                "display" to "flex",
                "flex-direction" to "column",
                "align-items" to "center"
            ),
            "style_logon_bottom_expander" to json (
                "grid-area" to "3 / 2 / 4 / 3"
            ),
            "style_logon_logo" to json (
                "padding" to styleLogonLogoPadding()
            ),
            "style_logon_error" to json (
                "align-self" to "center",
                "font-size" to styleControlTextFontSize(),
                "color" to "red"
            ),
            "style_logon_div" to json (
                "font-size" to styleControlTextFontSize(),
                "padding" to styleLogonControlPadding()
            ),
            "style_logon_input" to json (
                "background" to COLOR_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleControlTextFontSize(),
                "padding" to styleCommonEditorPadding()
            ),
            "style_logon_checkbox" to json (
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "transform" to styleControlCheckBoxTransform(),
                "font-size" to styleControlTextFontSize(),
                "margin" to styleLogonCheckBoxMargin()
            ),
            "style_logon_button" to json (
                "background" to COLOR_BUTTON_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleCommonButtonFontSize(),
                "padding" to styleLogonButtonPadding(),
                "margin" to styleLogonButtonMargin(),
                "cursor" to "pointer"
            )
        )
    }
}

private fun fillSystemProperties( hmSystemProperties: MutableMap<String,String> ) {
    hmSystemProperties[ "k.b.w.devicePixelRatio" ] = kotlin.browser.window.devicePixelRatio.toString()
    hmSystemProperties[ "k.b.w.appCodeName" ] = kotlin.browser.window.navigator.appCodeName
    hmSystemProperties[ "k.b.w.appName" ] = kotlin.browser.window.navigator.appName
    hmSystemProperties[ "k.b.w.appVersion" ] = kotlin.browser.window.navigator.appVersion
    hmSystemProperties[ "k.b.w.language" ] = kotlin.browser.window.navigator.language
    hmSystemProperties[ "k.b.w.platform" ] = kotlin.browser.window.navigator.platform
    hmSystemProperties[ "k.b.w.product" ] = kotlin.browser.window.navigator.product
    hmSystemProperties[ "k.b.w.productSub" ] = kotlin.browser.window.navigator.productSub
    hmSystemProperties[ "k.b.w.userAgent" ] = kotlin.browser.window.navigator.userAgent
    hmSystemProperties[ "k.b.w.vendor" ] = kotlin.browser.window.navigator.vendor
    hmSystemProperties[ "k.b.w.width" ] = kotlin.browser.window.screen.width.toString()
    hmSystemProperties[ "k.b.w.height" ] = kotlin.browser.window.screen.height.toString()
    hmSystemProperties[ "k.b.w.availWidth" ] = kotlin.browser.window.screen.availWidth.toString()
    hmSystemProperties[ "k.b.w.availHeight" ] = kotlin.browser.window.screen.availHeight.toString()
}
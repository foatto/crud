package foatto.core_web

import foatto.core.app.UP_TIME_OFFSET
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.LogoffRequest
import foatto.core.link.MenuData
import foatto.core.link.SaveUserPropertyRequest
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.js.json

private const val CMD_SET_START_PAGE = "set_start_page"
private const val CMD_CLEAR_START_PAGE = "clear_start_page"
private const val CMD_CHANGE_PASSWORD = "change_password"
private const val CMD_LOGOFF = "logoff"
private const val CMD_TIME_OFFSET_PREFIX = "timeoffset_"

private val arrTZOffset = intArrayOf(
    0, 1 * 60 * 60, 2 * 60 * 60, 3 * 60 * 60, 4 * 60 * 60, 5 * 60 * 60, 6 * 60 * 60,
    7 * 60 * 60, 8 * 60 * 60, 9 * 60 * 60, 10 * 60 * 60, 11 * 60 * 60, 12 * 60 * 60
)

val arrRbmiTZ = arrayOf(
    "UTC+00:00", "UTC+01:00", "UTC+02:00", "UTC+03:00", "UTC+04:00", "UTC+05:00", "UTC+06:00",
    "UTC+07:00", "UTC+08:00", "UTC+09:00", "UTC+10:00", "UTC+11:00", "UTC+12:00"
)

@Suppress("UnsafeCastFromDynamic")
fun menuBar(arrMenuData: Array<MenuData>) = vueComponentOptions().apply {

    //--- убрать этот вложенный ужас, переделать на циклические шаблоны
    this.template = """
        <span>
            <img src="/web/images/ic_menu_black_48dp.png"
                 v-bind:style="style_menu_button"
                 v-on:click="isShowMainMenu=!isShowMainMenu"
                 title="Главное меню"
            >

            <div v-bind:style="style_menu_start" v-show="isShowMainMenu">

                <template v-for="menuData_0 in arrMenuData">
                    <template v-if="menuData_0.alSubMenu">
                        <details>
                            <summary ${generateSummaryTag(0)}>
                                {{menuData_0.text}}
                            </summary>

                            <template v-for="menuData_1 in menuData_0.alSubMenu">
                                <template v-if="menuData_1.alSubMenu">
                                    <details>
                                        <summary ${generateSummaryTag(1)}>
                                            {{menuData_1.text}}
                                        </summary>

                                        <template v-for="menuData_2 in menuData_1.alSubMenu">
                                            ${generateMenuItem(2)}
                                        </template>
                                        
                                    </details>
                                </template>
                                <template v-else>
                                    ${generateMenuItem(1)}
                                </template>
                            </template>

                        </details>
                    </template>
                    <template v-else>
                        ${generateMenuItem(0)}
                    </template>
                </template>

            </div>
        </span>
    """

    this.methods = json(
        "menuClick" to { url: String ->
            that().isShowMainMenu = false

            //--- when не подойдёт из-за сложных else в конце
            if (url == CMD_SET_START_PAGE) {
                val curAppParam = that().`$root`.curAppParam.unsafeCast<String>()
                localStorage.setItem(LOCAL_STORAGE_APP_PARAM, curAppParam)
            } else if (url == CMD_CLEAR_START_PAGE) {
                localStorage.removeItem(LOCAL_STORAGE_APP_PARAM)
            } else if (url == CMD_CHANGE_PASSWORD) {
                val password1 = window.prompt("Введите новый пароль")
                password1?.let {
                    val password2 = window.prompt("Введите ещё раз")
                    password2?.let {
                        //--- проверка нового пароля
                        if (password1 != password2) window.alert("Вы ввели разные пароли.\nПопробуйте ввести еще раз.")
                        else if (password1.length <= 3) window.alert("Слишком короткий пароль.\nПопробуйте ввести еще раз.")
                        else {
                            //--- проверку комплексности пароля пока пропустим. Не любят этого пользователи.
                            //if(  pwd.length() < 8  ) return false;
                            //if(  pwd.equals(  pwd.toUpperCase()  )  ) return false;
                            //if(  pwd.equals(  pwd.toLowerCase()  )  ) return false;
                            //boolean haveDigit = false;
                            //for(  int i = 0; i < 10; i++  )
                            //    if(  pwd.indexOf(  "" + i  ) >= 0  ) {
                            //        haveDigit = true;
                            //        break;
                            //    }
                            //return haveDigit;
                            invokeChangePassword(ChangePasswordRequest(encodePassword(password1)))
                            window.alert("Пароль успешно сменен.")
                        }
                    }
                }
            } else if (url == CMD_LOGOFF) {
                val localStorage = localStorage
                localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
                localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")

                invokeLogoff(LogoffRequest())
            } else if (url.startsWith(CMD_TIME_OFFSET_PREFIX)) {
                val newTimeOffsetInSec = url.substringAfterLast('_').toInt()

                that().`$root`.timeOffset = newTimeOffsetInSec
                //--- на сервере лежит в миллисекундах - избыточно, но менять уже не будем
                invokeSaveUserProperty(SaveUserPropertyRequest(UP_TIME_OFFSET, newTimeOffsetInSec.toString()))
            } else that().`$root`.openTab(url)
        },
    )

    this.mounted = {
        //--- добавить клиентское меню и преобразовать в локальную структуру
        that().arrMenuData = addClientMenu(arrMenuData)
    }

    this.data = {
        json(
            "arrMenuData" to "[]",
            "isShowMainMenu" to false,

            "style_menu_button" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "align-self" to "flex-start",
                "position" to "relative",
                "background" to COLOR_BUTTON_BACK,
                "border" to "1px solid $COLOR_BUTTON_BORDER",
                "border-radius" to BORDER_RADIUS,
                "padding" to styleIconButtonPadding(),
                "margin" to styleMenuIconButtonMargin(),
                "cursor" to "pointer"
            ),
            "style_menu_start" to json(
                "z-index" to "999",
                "position" to "absolute",
                "top" to styleMenuStartTop(),
                "width" to styleMenuWidth(),
                "height" to "80%",
                "border" to "1px solid $COLOR_MENU_BORDER",
                "border-radius" to BORDER_RADIUS,
                "font-size" to styleMenuFontSize(),
                "padding" to styleMenuStartPadding(),
                "background" to COLOR_MENU_BACK,
                "overflow" to "auto",
                "cursor" to "pointer"
            ),
            "style_menu_summary_0" to json(
                "padding" to styleMenuItemPadding_0(),
            ),
            "style_menu_summary_1" to json(
                "padding" to styleMenuItemPadding_1(),
            ),
            "style_menu_item_0" to json(
                "padding" to styleMenuItemPadding_0()
            ),
            "style_menu_item_1" to json(
                "padding" to styleMenuItemPadding_1()
            ),
            "style_menu_item_2" to json(
                "padding" to styleMenuItemPadding_2()
            ),
        )
    }
}

private fun addClientMenu(arrMenuData: Array<MenuData>): Array<MenuData> {
    val alSubMenu = mutableListOf<MenuData>()

//    miUserDoc = MenuItem( "Руководство пользователя" )
//    miUserDoc.onAction = this as EventHandler<ActionEvent>
//    menuClientStaticMenu.items.add( miUserDoc )

    alSubMenu.add(MenuData(CMD_SET_START_PAGE, "Установить вкладку как стартовую", null))
    alSubMenu.add(MenuData(CMD_CLEAR_START_PAGE, "Очистить установку стартовой", null))

    alSubMenu.add(MenuData("", ""))

    alSubMenu.add(MenuData(CMD_CHANGE_PASSWORD, "Сменить пароль", null))

    alSubMenu.add(MenuData("", ""))

    val alTimeOffsetMenu = mutableListOf<MenuData>()
    for (i in arrTZOffset.indices) {
        alTimeOffsetMenu.add(MenuData(CMD_TIME_OFFSET_PREFIX + arrTZOffset[i], arrRbmiTZ[i], null))
    }

    alSubMenu.add(MenuData("", "Часовой пояс", alTimeOffsetMenu.toTypedArray()))

    alSubMenu.add(MenuData("", ""))

    alSubMenu.add(MenuData(CMD_LOGOFF, "Выход из системы", null))

    alSubMenu.add(MenuData("", ""))

    alSubMenu.add(MenuData("", "outer width = ${window.outerWidth}", null))
    alSubMenu.add(MenuData("", "inner width = ${window.innerWidth}", null))
    alSubMenu.add(MenuData("", "device pixel ratio = ${window.devicePixelRatio}", null))
    alSubMenu.add(MenuData("", "touch screen = ${styleIsTouchScreen()}", null))

    val alMenuData = arrMenuData.toMutableList()
    alMenuData.add(MenuData("", "Дополнительно", alSubMenu.toTypedArray()))

    return alMenuData.toTypedArray()
}

private fun generateSummaryTag(summaryLevel: Int): String {
    val menuDataName = "menuData_$summaryLevel"
    return """
        v-bind:style="[ style_menu_summary_$summaryLevel,
                        { 'background-color' : ( $menuDataName.isHover? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_BACK' ) }
                      ]"
            v-on:mouseenter="$menuDataName.isHover = true"
            v-on:mouseleave="$menuDataName.isHover = false"
    """
}

private fun generateMenuItem(menuLevel: Int): String {
    val menuDataName = "menuData_$menuLevel"
    return """
        <div v-bind:style="[ style_menu_item_$menuLevel,
                        { 'background-color' : ( $menuDataName.isHover? '$COLOR_MENU_BACK_HOVER' : '$COLOR_MENU_BACK' ) },
                        { 'text-decoration' : ( $menuDataName.url || $menuDataName.text ? '' : 'line-through' ) },
                        { 'color' : ( $menuDataName.url || $menuDataName.text ? '$COLOR_TEXT' : '$COLOR_MENU_DELIMITER' ) }
                      ]"
            v-on:click="$menuDataName.url ? menuClick( $menuDataName.url ) : null"
            v-on:mouseenter="$menuDataName.text ? $menuDataName.isHover = true : $menuDataName.isHover = false"
            v-on:mouseleave="$menuDataName.isHover = false"
        >
            {{ $menuDataName.url ? $menuDataName.text : ( $menuDataName.text ? $menuDataName.text + " &gt;" : "$MENU_DELIMITER" ) }}
        </div>
    """
}

//    private fun passwordChangeDialog(): Optional<Pair<String, String>>? {
//        val dialog = Dialog<Pair<String, String>>()
//        dialog.title = "Смена пароля"
//        dialog.headerText = null
//        //dialog.setContentText(  null  );
//        dialog.initStyle( StageStyle.UTILITY )
//        dialog.initModality( Modality.APPLICATION_MODAL )
//
//        //!!! ни один из вариантов пока не работает
//        //            dialog.initOwner(  appStage  );
//        //            URL iconURL = getClass().getResource(  ICON_URL  );
//        //            if(  iconURL != null  ) (  ( Stage ) dialog.getDialogPane().getScene().getWindow()  ).getIcons().add(  new Image(  iconURL.toString()  )  );
//
//        // dialog.setGraphic(  null  );
//
//        val okButtonType = ButtonType( "OK", ButtonBar.ButtonData.OK_DONE )
//        dialog.dialogPane.buttonTypes.addAll( okButtonType, ButtonType.CANCEL )
//
//        val grid = GridPane()
//        grid.hgap = 16.0
//        grid.vgap = 16.0
//        //grid.setPadding( new Insets( 20, 150, 10, 10 ) );
//
//        val lblPassword1 = Label( "Новый пароль:" )
//        val txtPassword1 = PasswordField()
//        val lblPassword2 = Label( "Новый пароль ( ещё раз ):" )
//        val txtPassword2 = PasswordField()
//
//        lblPassword1.alignment = Pos.BASELINE_RIGHT
//        lblPassword2.alignment = Pos.BASELINE_RIGHT
//
//        txtPassword1.selectAll()
//        //txtPassword1.setOnAction(  (  event  ) -> txtPassword2.requestFocus()  );
//
//        val loginButton = dialog.dialogPane.lookupButton( okButtonType )
//        txtPassword1.textProperty().addListener { _, _, _ -> loginButton.isDisable = txtPassword1.text.trim().isEmpty() || txtPassword2.text.trim().isEmpty() }
//        txtPassword2.textProperty().addListener { _, _, _ -> loginButton.isDisable = txtPassword1.text.trim().isEmpty() || txtPassword2.text.trim().isEmpty() }
//
//        grid.add( lblPassword1, 0, 0 )
//        grid.add( txtPassword1, 1, 0 )
//        grid.add( lblPassword2, 0, 1 )
//        grid.add( txtPassword2, 1, 1 )
//
//        dialog.dialogPane.content = grid
//
//        //--- обязательно через Platform.runLater, а не напрямую, т.к. ИНОГДА не будет срабатывать
//        Platform.runLater { txtPassword1.requestFocus() }
//
//        dialog.setResultConverter { dialogButton ->
//            if( dialogButton == okButtonType ) Pair( txtPassword1.text, txtPassword2.text )
//            else null
//        }
//
//        return dialog.showAndWait()
//    }

package foatto.core_web

import foatto.core.link.ChangePasswordRequest
import foatto.core.link.LogoffRequest
import foatto.core.link.MenuData
import foatto.core_web.external.vue.that
import foatto.core_web.external.vue.vueComponentOptions
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlin.js.json

const val MENU_BAR_ID = "menu_bar"

private const val CMD_SET_START_PAGE = "set_start_page"
private const val CMD_CLEAR_START_PAGE = "clear_start_page"
private const val CMD_CHANGE_PASSWORD = "change_password"
private const val CMD_LOGOFF = "logoff"

@Suppress("UnsafeCastFromDynamic")
fun menuBar(arrMenuData: Array<MenuData>) = vueComponentOptions().apply {

    this.template = if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
        """
            <span>
                <img src="/web/images/ic_menu_black_48dp.png"
                     v-bind:style="style_menu_button"
                     v-on:click="${'$'}root.isShowMainMenu=!${'$'}root.isShowMainMenu"
                     title="Главное меню"
                >
    
                <div v-show="${'$'}root.isShowMainMenu"
                     v-bind:style="[style_menu_start, style_menu_is_hidden]" 
                >
                    ${menuGenerateBody(true, "arrMenuData", "menuClick", ".url")}
                </div>
            </span>
        """
    } else {
        """
            <div id="$MENU_BAR_ID" 
                 v-bind:style="style_menu_start"
            >
                $styleMainMenuTop
                ${menuGenerateBody(true, "arrMenuData", "menuClick", ".url")}
            </div>
        """
    }

    this.methods = json(
        "menuClick" to { url: String ->
            that().`$root`.isShowMainMenu = false

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
                        if (password1 != password2) {
                            window.alert("Вы ввели разные пароли.\nПопробуйте ввести ещё раз.")
                        } else if (password1.length <= 3) {
                            window.alert("Слишком короткий пароль.\nПопробуйте ввести ещё раз.")
                        } else {
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
                            window.alert("Пароль успешно сменён.")
                        }
                    }
                }
            } else if (url == CMD_LOGOFF) {
                val localStorage = localStorage
                localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
                localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")

                invokeLogoff(LogoffRequest())
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

            "style_menu_button" to json(
                "flex-grow" to 0,
                "flex-shrink" to 0,
                "align-self" to "flex-start",
                "position" to "relative",
                "background" to colorButtonBack(),
                "border" to "1px solid ${colorMainBorder()}",
                "border-radius" to styleFormBorderRadius,
                "padding" to styleIconButtonPadding(),
                "margin" to styleMenuIconButtonMargin(),
                "cursor" to "pointer"
            ),
            "style_menu_start" to json(
                "width" to styleMenuWidth(),
                "min-width" to styleMenuWidth(),
                "background" to colorMainMenuBack(),
                "color" to colorMenuTextDefault,
                "border" to "1px solid ${colorMenuBorder()}",
                "border-radius" to styleFormBorderRadius,
                "font-size" to styleMenuFontSize(0),
                "padding" to styleMenuStartPadding,
                "overflow" to "auto",
                "cursor" to "pointer",
            ),
            "style_menu_is_hidden" to json(
                "z-index" to Z_INDEX_MENU,
                "position" to "absolute",
                "top" to styleMenuStartTop(),
                "bottom" to if (styleIsNarrowScreen) "20%" else "10%", //"height" to "80%",
            ),
            "style_menu_summary_0" to json(
                "font-size" to styleMenuFontSize(0),
                "padding" to styleMenuItemPadding(0),
            ),
            "style_menu_summary_1" to json(
                "font-size" to styleMenuFontSize(1),
                "padding" to styleMenuItemPadding(1),
            ),
            "style_menu_item_0" to json(
                "font-size" to styleMenuFontSize(0),
                "padding" to styleMenuItemPadding(0)
            ),
            "style_menu_item_1" to json(
                "font-size" to styleMenuFontSize(1),
                "padding" to styleMenuItemPadding(1)
            ),
            "style_menu_item_2" to json(
                "font-size" to styleMenuFontSize(2),
                "padding" to styleMenuItemPadding(2)
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

    alSubMenu.add(MenuData(CMD_LOGOFF, "Выход из системы", null))

    alSubMenu.add(MenuData("", ""))

    alSubMenu.add(MenuData("", "outer width = ${window.outerWidth}", null))
    alSubMenu.add(MenuData("", "outer height = ${window.outerHeight}", null))
    alSubMenu.add(MenuData("", "inner width = ${window.innerWidth}", null))
    alSubMenu.add(MenuData("", "inner height = ${window.innerHeight}", null))
    alSubMenu.add(MenuData("", "device pixel ratio = ${window.devicePixelRatio}", null))
    alSubMenu.add(MenuData("", "touch screen = ${styleIsTouchScreen()}", null))

    val alMenuData = arrMenuData.toMutableList()
    alMenuData.add(MenuData("", "Дополнительно", alSubMenu.toTypedArray()))

    return alMenuData.toTypedArray()
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

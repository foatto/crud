package foatto.core_compose_web

import androidx.compose.runtime.Composable
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.LogoffRequest
import foatto.core.link.MenuData
import foatto.core_compose_web.link.invokeChangePassword
import foatto.core_compose_web.link.invokeLogoff
import foatto.core_compose_web.style.*
import foatto.core_compose_web.util.encodePassword
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.css.keywords.auto
import org.jetbrains.compose.web.css.properties.zIndex
import org.jetbrains.compose.web.dom.Details
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Summary
import org.jetbrains.compose.web.dom.Text

//--- Element Ids ----------------------------------------------------------------------------------------------------------------------------------------------

const val MENU_BAR_ID = "menu_bar"

//--- Client Commands ---

private const val CMD_SET_START_PAGE = "set_start_page"
private const val CMD_CLEAR_START_PAGE = "clear_start_page"
private const val CMD_CHANGE_PASSWORD = "change_password"
private const val CMD_LOGOFF = "logoff"

//--- Main Menu ---

var styleIsHiddenMenuBar: Boolean = true

//var styleMenuBar = ""
//var colorMenuCloserBack = colorMainBack1
//var colorMenuCloserButtonBack = COLOR_MAIN_TEXT
//var colorMenuCloserButtonText = COLOR_MAIN_BACK_0

//var styleMainMenuTop = ""
//var styleTopBar = ""

var getColorMainMenuBack: () -> CSSColorValue = { colorMainBack1 }      // может быть розрачным из-за фонового рисунка фона главного меню
var getColorPopupMenuBack: () -> CSSColorValue = { colorMainBack1 }    // обычно всегда имеет сплошной цвет
var colorMenuTextDefault: CSSColorValue = COLOR_MAIN_TEXT
var getColorMenuBorder: () -> CSSColorValue = { colorMainBorder }
var getColorMenuDelimiter: () -> CSSColorValue = { colorMainBack3 }

var getColorMenuBackHover0: () -> CSSColorValue = { colorCurrentAndHover }
var getColorMenuTextHover0: () -> CSSColorValue? = { null }

var getColorMenuBackHoverN: () -> CSSColorValue = { colorCurrentAndHover }
var getColorMenuTextHoverN: () -> CSSColorValue? = { null }

val MENU_DELIMITER: String = "-".repeat(60)

private val styleMenuStartTop = (if (styleIsNarrowScreen) {
    3.4
} else {
    if (screenDPR <= 1.0) {
        3.7
    } else {
        3.4
    }
}).cssRem

private val styleMenuIconButtonMargin: Array<CSSSize> = if (styleIsNarrowScreen) {
    arrayOf(0.px, menuTabPadMar, 0.px, 0.px)
} else {
    arrayOf(menuTabPadMar, menuTabPadMar, 0.px, 0.px)
}

//--- Main & Popup Menus ---

val arrStyleMenuStartPadding: Array<CSSSize> = arrayOf(1.0.cssRem, 1.0.cssRem, 1.0.cssRem, 1.0.cssRem)
private fun getStyleMenuItemTopBottomPad(level: Int) = if (styleIsNarrowScreen) {
    arrayOf(0.8, 0.6, 0.4)[level]
} else {
    arrayOf(0.8, 0.4, 0.2)[level]
}

private val arrStyleMenuItemSidePad = arrayOf(0.0, 1.0, 2.0)
val arrStyleMenuFontSize: Array<CSSSize> = arrayOf(1.0.cssRem, 0.9.cssRem, 0.8.cssRem)
private fun getStyleMenuItemPadding(level: Int): Array<CSSSize> = arrayOf(
    getStyleMenuItemTopBottomPad(level).cssRem,
    arrStyleMenuItemSidePad[level].cssRem,
    getStyleMenuItemTopBottomPad(level).cssRem,
    arrStyleMenuItemSidePad[level].cssRem,
)

fun StyleScope.setMenuWidth() {
    if (styleIsNarrowScreen) {
        width(85.percent)
        minWidth(85.percent)
    } else if (!styleIsHiddenMenuBar) {
        width(17.cssRem)
        minWidth(17.cssRem)
    } else {
        width(auto)
    }
}

class Menu(
    private val root: Root,
    private var arrMenuData: Array<MenuData>,
) : iClickableMenu {

    init {
        val alClientSubMenu = mutableListOf<MenuData>()

        //    miUserDoc = MenuItem( "Руководство пользователя" )
        //    miUserDoc.onAction = this as EventHandler<ActionEvent>
        //    menuClientStaticMenu.items.add( miUserDoc )

        alClientSubMenu.add(MenuData(url = "", text = "Пользователь: " + root.currentUserName, arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = CMD_CHANGE_PASSWORD, text = "Сменить пароль", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = CMD_LOGOFF, text = "Выход из системы", arrSubMenu = null))

        alClientSubMenu.add(MenuData(url = "", text = ""))

        alClientSubMenu.add(MenuData(url = CMD_SET_START_PAGE, text = "Установить вкладку как стартовую", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = CMD_CLEAR_START_PAGE, text = "Очистить установку стартовой", arrSubMenu = null))

        alClientSubMenu.add(MenuData(url = "", text = ""))

        alClientSubMenu.add(MenuData(url = "", text = "outer width = ${window.outerWidth}", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = "", text = "outer height = ${window.outerHeight}", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = "", text = "inner width = ${window.innerWidth}", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = "", text = "inner height = ${window.innerHeight}", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = "", text = "device pixel ratio = ${window.devicePixelRatio}", arrSubMenu = null))
        alClientSubMenu.add(MenuData(url = "", text = "touch screen = ${getStyleIsTouchScreen()}", arrSubMenu = null))

        val alMenuData = arrMenuData.toMutableList()
        alMenuData.add(MenuData(url = "", text = "Дополнительно", arrSubMenu = alClientSubMenu.toTypedArray()))

        arrMenuData = alMenuData.toTypedArray()
    }

    @Composable
    fun getBody() {
        if (styleIsNarrowScreen || styleIsHiddenMenuBar) {
            Span {
                Img(
                    src = "/web/images/ic_menu_black_48dp.png",
                    attrs = {
                        title("Главное меню")
                        style {
                            flexGrow(0)
                            flexShrink(0)
                            alignSelf(AlignSelf.FlexStart)
                            position(Position.Relative)
                            backgroundColor(getColorButtonBack())
                            setBorder(
                                color = colorMainBorder,
                                radius = styleFormBorderRadius,
                            )
                            padding(styleIconButtonPadding)
                            setMargins(styleMenuIconButtonMargin)
                            cursor("pointer")
                        }
                        onClick {
                            root.isShowMainMenu.value = !root.isShowMainMenu.value
                        }
                    }
                )
                if (root.isShowMainMenu.value) {
                    Div(
                        attrs = {
                            style {
                                setMenuWidth()
                                backgroundColor(getColorMainMenuBack())
                                color(colorMenuTextDefault)
                                setBorder(
                                    color = getColorMenuBorder(),
                                    radius = styleFormBorderRadius,
                                )
                                fontSize(arrStyleMenuFontSize[0])
                                setPaddings(arrStyleMenuStartPadding)
                                overflow("auto")
                                cursor("pointer")
                                zIndex(Z_INDEX_MENU)
                                position(Position.Absolute)
                                top(styleMenuStartTop)
                                bottom(if (styleIsNarrowScreen) 20.percent else 10.percent) //"height" to "80%",
                            }
                        }
                    ) {
                        generateMenuBody(this@Menu, true, arrMenuData, 0)
                    }
                }
            }
        } else {
            Div(
//                id="$MENU_BAR_ID"
                attrs = {
                    style {
                        setMenuWidth()
                        backgroundColor(getColorMainMenuBack())
                        color(colorMenuTextDefault)
                        setBorder(
                            color = getColorMenuBorder(),
                            radius = styleFormBorderRadius,
                        )
                        fontSize(arrStyleMenuFontSize[0])
                        setPaddings(arrStyleMenuStartPadding)
                        overflow("auto")
                        cursor("pointer")
                    }
                }
            ) {
//                    $styleMainMenuTop
                generateMenuBody(this@Menu, true, arrMenuData, 0)  // "menuClick", ".url"
            }
        }
    }

    companion object {

        @Composable
        fun generateMenuBody(
            clickableMenu: iClickableMenu,
            isMainMenu: Boolean,
            arrMenuData: Array<MenuData>,   // arrMenuDataName: String
            level: Int,
        ) {
            for (menuData in arrMenuData) { // menuData_0 in $arrMenuDataName
                menuData.arrSubMenu?.let { arrSubMenu ->
                    Details {
                        Summary(
                            attrs = {
                                style {
                                    fontSize(arrStyleMenuFontSize[level])
                                    setPaddings(getStyleMenuItemPadding(level))
                                    backgroundColor(
                                        if (menuData.isHover.value) {
//                                            if (level == 0) {         - проверить!!!
//                                                colorMenuBackHover0
//                                            } else {
//                                                colorMenuBackHoverN
//                                            }
                                            getColorMenuBackHover0()
                                        } else if (isMainMenu) {
                                            getColorMainMenuBack()
                                        } else {
                                            getColorPopupMenuBack()
                                        }
                                    )
//                                    if (level == 0) {                 - проверить!!!
//                                        colorMenuTextHover0
//                                    } else {
//                                        colorMenuTextHoverN
                                    /*}*/
                                    getColorMenuTextHover0()?.let { colorMenuTextHover ->
                                        color(
                                            if (menuData.isHover.value) {
                                                colorMenuTextHover
                                            } else {
                                                colorMenuTextDefault
                                            }
                                        )
                                    }
                                }
                                onMouseEnter {
                                    menuData.isHover.value = true
                                }
                                onMouseLeave {
                                    menuData.isHover.value = false
                                }
                            }
                        ) {
                            Text(menuData.text)
                        }
                        generateMenuBody(clickableMenu, isMainMenu, arrSubMenu, level + 1)
                    }
                } ?: run {
                    Div(
                        attrs = {
                            style {
                                fontSize(arrStyleMenuFontSize[level])
                                setPaddings(getStyleMenuItemPadding(level))
                                if (menuData.url.isEmpty() && menuData.text.isEmpty()) {
                                    textDecoration("line-through")
                                }
                                backgroundColor(
                                    if (menuData.isHover.value) {
//                                            if (level == 0) {         - проверить!!!
//                                                colorMenuBackHover0
//                                            } else {
//                                                colorMenuBackHoverN
//                                            }
                                        getColorMenuBackHoverN()
                                    } else if (isMainMenu) {
                                        getColorMainMenuBack()
                                    } else {
                                        getColorPopupMenuBack()
                                    }
                                )
                                color(
                                    if (menuData.url.isNotEmpty() || menuData.text.isNotEmpty()) {
                                        if (menuData.isHover.value) {
                                            getColorMenuTextHoverN()?.let {
                                                getColorMenuTextHoverN()
                                            } ?: colorMenuTextDefault
                                        } else {
                                            colorMenuTextDefault
                                        }
                                    } else {
                                        getColorMenuDelimiter()
                                    }
                                )
                            }
                            onClick {
                                clickableMenu.menuClick(menuData.url, menuData.inNewWindow)
                            }
                            onMouseEnter {
                                menuData.isHover.value = menuData.text.isNotEmpty()
                            }
                            onMouseLeave {
                                menuData.isHover.value = false
                            }
                        }
                    ) {
                        Text(
                            if (menuData.url.isNotEmpty()) {
                                menuData.text
                            } else if (menuData.text.isNotEmpty()) {
                                menuData.text + " >"
                            } else {
                                MENU_DELIMITER
                            }
                        )
                    }
                }
            }
        }
    }

    override fun menuClick(url: String, inNewWindow: Boolean) {
        root.isShowMainMenu.value = false

        when (url) {
            CMD_SET_START_PAGE -> {
                localStorage.setItem(LOCAL_STORAGE_APP_PARAM, root.curAppParam)
            }

            CMD_CLEAR_START_PAGE -> {
                localStorage.removeItem(LOCAL_STORAGE_APP_PARAM)
            }

            CMD_CHANGE_PASSWORD -> {
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
            }

            CMD_LOGOFF -> {
                val localStorage = localStorage
                localStorage.setItem(LOCAL_STORAGE_LOGIN, "")
                localStorage.setItem(LOCAL_STORAGE_PASSWORD, "")

                invokeLogoff(LogoffRequest())
            }

            else -> {
                root.openTab(url)
            }
        }
    }
}
/*

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
 */


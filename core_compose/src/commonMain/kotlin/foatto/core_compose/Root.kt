package foatto.core_compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import foatto.core.link.ChangePasswordRequest
import foatto.core.link.LogoffRequest
import foatto.core_compose.composables.MainMenu
import foatto.core_compose.composables.PasswordChangeDialog
import foatto.core_compose.composables.StardartDialog
import foatto.core_compose.composables.TabPanel
import foatto.core_compose.composables.getClientSubMenu
import foatto.core_compose.di.SETTINGS_APP_PARAM
import foatto.core_compose.di.SETTINGS_LOGIN
import foatto.core_compose.di.SETTINGS_PASSWORD
import foatto.core_compose.di.settings
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.models.TabInfo
import foatto.core_compose.utils.encodePassword
import foatto.core_compose.utils.getScaledScreenWidth
import foatto.core_compose.utils.openFileByUrl

/* !!! для перекрытия в MMSRoot:
    var getMenu: (
        root: Root,
        arrMenuData: Array<MenuDataClient>,
    ) -> Menu = { root: Root,
                  arrMenuData: Array<MenuDataClient> ->
        Menu(root, arrMenuData)
    }

    var getAppControl: (
        root: Root,
        startAppParam: String,
        tabId: Int,
    ) -> AppControl = { root: Root,
                        startAppParam: String,
                        tabId: Int ->
        AppControl(root, startAppParam, tabId)
    }
*/

open class Root {

    companion object {
        const val CMD_SET_START_PAGE: String = "set_start_page"
        const val CMD_CLEAR_START_PAGE: String = "clear_start_page"
        const val CMD_CHANGE_PASSWORD: String = "change_password"
        const val CMD_LOGOFF: String = "logoff"
    }

    var scaledScreenWidth: Int = 0
        private set
    var isWideScreen: Boolean by mutableStateOf(false)
        private set
    var scaleKoef: Float = 1.0f
        private set

    private val alMenuDataClient = mutableStateListOf<MenuDataClient>()
    private var isShowMainMenu by mutableStateOf(false)
    private var isTabPanelVisible by mutableStateOf(false)
    private var selectedTabIndex by mutableIntStateOf(0)
    private val alTabInfo = mutableStateListOf<TabInfo>()
    private val alControl = mutableStateListOf<AppControl>()
    private var waitCount by mutableIntStateOf(0)

    var currentUserName: String = ""
    var timeOffset: Int = 0
    var curAppParam: String = ""

    private var lastTabId: Int = 0

    private var dialogActionFun: () -> Unit = {}
    private var dialogQuestion by mutableStateOf("")
    private var showDialogCancel by mutableStateOf(false)
    private var showDialog by mutableStateOf(false)
    private val dialogButtonOkText by mutableStateOf("OK")
    private val dialogButtonCancelText by mutableStateOf("Отмена")

    private var showPasswordChangeDialog by mutableStateOf(false)

    @Composable
    fun Content() {
        val density = LocalDensity.current

        scaledScreenWidth = getScaledScreenWidth()
        //--- новый рекорд: Samsung A52, inner width = 509, outer width = 412
        isWideScreen = scaledScreenWidth >= 600
        //--- инициализируем ДО того, как будет использоваться в Map/Scheme/Chart/Dashboard
        scaleKoef = density.density

        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .fillMaxHeight(),
                ) {
                    Row {
                        Column {
                            IconButton(
                                onClick = {
                                    isShowMainMenu = !isShowMainMenu
                                }
                            ) {
                                Icon(
                                    imageVector = if (isShowMainMenu) {
                                        Icons.Default.Close
                                    } else {
                                        Icons.Default.Menu
                                    },
                                    contentDescription = null,
                                )
                            }
                            MainMenu(
                                isShowMainMenu = isShowMainMenu,
                                alMenuDataClient = alMenuDataClient,
                                closeMenu = {
                                    isShowMainMenu = false
                                },
                                menuClick = { url: String, inNewWindow: Boolean ->
                                    openTab(url)
                                }
                            )
                        }
                        TabPanel(
                            isTabPanelVisible = isTabPanelVisible,
                            selectedTabIndex = selectedTabIndex,
                            alTabInfo = alTabInfo,
                            onTabClick = { index ->
                                selectedTabIndex = index
                            },
                            onTabCloseClick = { index ->
                                closeTabByIndex(index)
                            },
                        )
                    }
                    alControl.getOrNull(selectedTabIndex)?.Body()
                }
                if (showPasswordChangeDialog) {
                    PasswordChangeDialog(
                        onOkClick = { newPassword ->
                            showPasswordChangeDialog = false
                            invokeChangePassword(ChangePasswordRequest(encodePassword(newPassword))) {
                                showAlert("Пароль успешно сменён.")
                            }
                        },
                        onCancelClick = {
                            showPasswordChangeDialog = false
                        }
                    )
                }
                if (waitCount > 0) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(color = Color.White.copy(alpha = 0.7f))
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(128.dp).align(Alignment.Center),
                        )
                    }
                }
                if (showDialog) {
                    StardartDialog(
                        question = dialogQuestion,
                        buttonOkText = dialogButtonOkText,
                        buttonCancelText = dialogButtonCancelText,
                        showCancelButton = showDialogCancel,
                        onOkClick = {
                            showDialog = false
                            dialogActionFun()
                        },
                        onCancelClick = { showDialog = false },
                    )
                }
            }
        }
    }

    fun start() {
        settings.getStringOrNull(SETTINGS_APP_PARAM)?.let { localStartAppParam ->
            addTabComp(localStartAppParam)
        }
    }

    fun openTab(newAppParam: String) {
        //--- файловое урло
        if (newAppParam[0] == '/') {
            openFileByUrl(newAppParam)
        } else when (newAppParam) {
            CMD_SET_START_PAGE -> {
                settings.putString(SETTINGS_APP_PARAM, curAppParam)
            }

            CMD_CLEAR_START_PAGE -> {
                settings.remove(SETTINGS_APP_PARAM)
            }

            CMD_CHANGE_PASSWORD -> {
                showPasswordChangeDialog = true
            }

            CMD_LOGOFF -> {
                settings.remove(SETTINGS_LOGIN)
                settings.remove(SETTINGS_PASSWORD)

                invokeLogoff(LogoffRequest()) {
                    //!!! переход на стартовую страницу
                }
            }

            else -> {
                addTabComp(newAppParam)
            }
        }
    }

    private fun addTabComp(appParam: String) {
        lastTabId++

        val appControl = AppControl(this, appParam, lastTabId)

        alTabInfo += TabInfo(lastTabId, listOf(), "")
        alControl += appControl

        selectedTabIndex = alTabInfo.lastIndex

        appControl.start()
    }

    fun setTabInfo(tabId: Int, tabText: String, tabToolTip: String) {
        alTabInfo.find { tabInfo ->
            tabInfo.id == tabId
        }?.let { tabInfo ->
            tabInfo.alText = tabText.split('\n').filter { tabWord ->
                tabWord.isNotBlank()
            }.map { tabWord ->
                tabWord
//                if (tabWord.length > getStyleTabComboTextLen()) {
//                    tabWord.substring(0, getStyleTabComboTextLen()) + "..."
//                } else {
//                    tabWord
//                }
            }
            tabInfo.tooltip = tabToolTip
        }
    }

    fun closeTabById(tabId: Int) {
        val indexForClose = alTabInfo.indexOfFirst { tabInfo ->
            tabInfo.id == tabId
        }
        closeTabByIndex(indexForClose)
    }

    fun closeTabByIndex(indexForClose: Int) {
        //--- for last tab removing case
        if (selectedTabIndex == alTabInfo.lastIndex) {
            selectedTabIndex--
        }

        alControl.removeAt(indexForClose)
        alTabInfo.removeAt(indexForClose)
    }

    fun setMenuBarData(alMenuData: List<MenuDataClient>, scaledScreenWidth: Int) {
        alMenuDataClient.clear()
        alMenuDataClient.addAll(alMenuData)
        alMenuDataClient.add(getClientSubMenu(currentUserName, scaledScreenWidth, scaleKoef))
        isTabPanelVisible = true
    }

    fun setWait(isWait: Boolean) {
        waitCount += (if (isWait) 1 else -1)
    }

    fun showAlert(message: String) {
        dialogActionFun = {}
        dialogQuestion = message
        showDialogCancel = false
        showDialog = true
    }

}

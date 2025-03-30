package foatto.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import foatto.compose.composable.MainMenu
import foatto.compose.composable.PasswordChangeDialog
import foatto.compose.composable.StardartDialog
import foatto.compose.composable.TabPanel
import foatto.compose.composable.getClientSubMenu
import foatto.compose.control.TableControl
import foatto.compose.model.MenuDataClient
import foatto.compose.model.TabInfo
import foatto.compose.utils.*
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.AppUserConfig
import foatto.core.model.request.ChangePasswordRequest
import foatto.core.model.request.LogoffRequest
import foatto.core.model.response.ChangePasswordResponse
import foatto.core.model.response.LogoffResponse
import kotlinx.coroutines.launch

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
        //--- новый рекорд: Samsung A52, inner width = 509, outer width = 412
        private const val WIDE_SCREEN_WIDTH = 600
    }

    var scaledWindowWidth: Int = 0
        private set
    var isWideScreen: Boolean by mutableStateOf(false)
        private set
    var scaleKoef: Float = 1.0f
        private set

    var appUserConfig: AppUserConfig by mutableStateOf(
        AppUserConfig(
            currentUserName = "",
            isAdmin = false,
            timeOffset = 0,
            userProperties = mutableMapOf(),
        )
    )

    private var isShowMainMenuButton: Boolean by mutableStateOf(false)
    private val alMenuDataClient = mutableStateListOf<MenuDataClient>()
    private var isShowMainMenu by mutableStateOf(false)

    private var isTabPanelVisible by mutableStateOf(false)
    private var selectedTabIndex by mutableIntStateOf(0)
    private val alTabInfo = mutableStateListOf<TabInfo>()

    private val alControl = mutableStateListOf<AppControl>()
    var selectorControl: TableControl? by mutableStateOf(null)

    private var waitCount by mutableIntStateOf(0)

    lateinit var curAction: AppAction

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
        setCustomColors()

        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()

        scaledWindowWidth = getScaledWindowWidth()
        isWideScreen = scaledWindowWidth >= WIDE_SCREEN_WIDTH
        //--- инициализируем ДО того, как будет использоваться в Map/Scheme/Chart/Dashboard
        scaleKoef = density.density

        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                ) {
                    Row {
                        Column {
                            if (isShowMainMenuButton) {
                                FilledIconButton(
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(0.dp),   // чтобы кнопка заполнила всё пространство
                                    colors = colorIconButton ?: IconButtonDefaults.iconButtonColors(),
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
                            }
                            MainMenu(
                                isShowMainMenu = isShowMainMenu,
                                alMenuDataClient = alMenuDataClient,
                                closeMenu = {
                                    isShowMainMenu = false
                                },
                                menuClick = { action: AppAction, inNewTab: Boolean ->
                                    coroutineScope.launch {
                                        openTab(action)
                                    }
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

                selectorControl?.Body()

                if (showPasswordChangeDialog) {
                    PasswordChangeDialog(
                        onOkClick = { newPassword ->
                            showPasswordChangeDialog = false
                            coroutineScope.launch {
                                invokeRequest(ChangePasswordRequest(encodePassword(newPassword))) { changePasswordResponse: ChangePasswordResponse ->
                                    showAlert("Пароль успешно сменён.")
                                }
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
                            strokeWidth = 8.dp,
                            color = colorWait ?: ProgressIndicatorDefaults.circularColor,
                            trackColor = colorWaitTrack ?: ProgressIndicatorDefaults.circularIndeterminateTrackColor,
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

    @Composable
    open fun setCustomColors() {
    }

    suspend fun start() {
        settings.getStringOrNull(SETTINGS_START_MODULE)?.let { startModule ->
            addTabComp(
                AppAction(
                    type = ActionType.MODULE_TABLE,
                    module = startModule,
                )
            )
        }
    }

    suspend fun openTab(action: AppAction) {
        when (action.type) {
            ActionType.FILE -> {
                action.url?.let { fileUrl ->
                    openFileByUrl(fileUrl)
                }
            }

            ActionType.SET_START_MODULE -> {
                action.module?.let { module ->
                    settings.putString(SETTINGS_START_MODULE, module)
                }
            }

            ActionType.CLEAR_START_MODULE -> {
                settings.remove(SETTINGS_START_MODULE)
            }

            ActionType.CHANGE_PASSWORD -> {
                showPasswordChangeDialog = true
            }

            ActionType.LOGOFF -> {
                settings.remove(SETTINGS_LOGIN)
                settings.remove(SETTINGS_PASSWORD)
                settings.remove(SETTINGS_LOGON_EXPIRE)

                invokeRequest(LogoffRequest()) { logoffResponse: LogoffResponse ->
                    isShowMainMenuButton = false

                    alMenuDataClient.clear()

                    isTabPanelVisible = false
                    selectedTabIndex = 0
                    alTabInfo.clear()
                    alControl.clear()

                    start()
                }
            }

            else -> {
                addTabComp(action)
            }
        }
    }

    private suspend fun addTabComp(action: AppAction) {
        lastTabId++

        val appControl = AppControl(this, action, lastTabId)

        alTabInfo += TabInfo(lastTabId, listOf())
        alControl += appControl

        selectedTabIndex = alTabInfo.lastIndex

        appControl.start()
    }

    fun setTabInfo(tabId: Int, tabText: String) {
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

    fun setMenuBarData(alMenuData: List<MenuDataClient>, scaledWindowWidth: Int) {
        isShowMainMenuButton = true

        alMenuDataClient.clear()
        alMenuDataClient.addAll(alMenuData)
        alMenuDataClient.add(getClientSubMenu(appUserConfig, scaledWindowWidth, scaleKoef))

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

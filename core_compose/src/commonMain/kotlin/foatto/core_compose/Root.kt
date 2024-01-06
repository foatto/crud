package foatto.core_compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import foatto.core_compose.models.MenuDataClient
import foatto.core_compose.models.TabInfo

open class Root {

    private var isWideScreen by mutableStateOf(false)

    private val alMenuDataClient = mutableStateListOf<MenuDataClient>()
    private var isShowMainMenu by mutableStateOf(false)
    private var isTabPanelVisible by mutableStateOf(false)
    private var selectedTabIndex by mutableIntStateOf(0)
    private val alTabInfo = mutableStateListOf<TabInfo>()
    private val alControl = mutableStateListOf<AppControl>()
    private var waitCount by mutableIntStateOf(0)

    var currentUserName: String = ""
    var timeOffset: Int = 0
//    var curAppParam: String = ""

    private var lastTabId: Int = 0

    @Composable
    fun Content() {
        //--- новый рекорд: Samsung A52, inner width = 509, outer width = 412
        isWideScreen = LocalConfiguration.current.screenWidthDp > 600

        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (isWideScreen) {
                        MainMenu(
                            isWideScreen = isWideScreen,
                            alMenuDataClient = alMenuDataClient,
                            isShowMainMenu = isShowMainMenu,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .fillMaxHeight(),
                    ) {
                        Row {
                            if (!isWideScreen) {
                                //!!! попробовать IconButton
                                Button(
                                    onClick = {
                                        isShowMainMenu = !isShowMainMenu
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                    )
                                }
                            }
                            TabPanel(
                                isTabPanelVisible = isTabPanelVisible,
                                selectedTabIndex = selectedTabIndex,
                                alTabInfo = alTabInfo,
                                onTabClick = { index ->
                                    selectedTabIndex = index
                                },
                                onTabCloseClick = { index ->
                                    //!!! close tab
                                },
                            )
                        }
                        alControl.getOrNull(selectedTabIndex)?.Body()
                    }
                }
                if (waitCount > 0) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                //!!! dialog panel?
            }
        }
    }

    fun start() {
        //!!! use local storage
//        val localStartAppParam = localStorage.getItem(LOCAL_STORAGE_APP_PARAM)
//        addTabComp(
//            if (localStartAppParam.isNullOrBlank()) {
//                (document.getElementById("startAppParam") as HTMLSpanElement).innerText
//            } else {
//                localStartAppParam
//            }
//        )
        addTabComp("alias=mms_day_work&action=table")
    }

    fun openTab(newAppParam: String) {
        //--- файловое урло
        if (newAppParam[0] == '/') {
            //!!! file/url open
            //window.open(newAppParam)
        } else {
            addTabComp(newAppParam)
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

    fun setMenuBarData(alMenuData: List<MenuDataClient>) {
        alMenuDataClient.clear()
        alMenuDataClient.addAll(alMenuData)
        isTabPanelVisible = true
    }

    fun setWait(isWait: Boolean) {
        waitCount += (if (isWait) 1 else -1)
    }
}


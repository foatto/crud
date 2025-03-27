package foatto.compose.composable

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import foatto.compose.model.MenuDataClient
import foatto.core.ActionType
import foatto.core.model.AppAction
import foatto.core.model.AppUserConfig

@Composable
internal fun MainMenu(
    isShowMainMenu: Boolean,
    alMenuDataClient: SnapshotStateList<MenuDataClient>,
    closeMenu: () -> Unit,
    menuClick: (action: AppAction, inNewTab: Boolean) -> Unit,
) {
    DropdownMenu(
        expanded = isShowMainMenu,
        onDismissRequest = closeMenu,
    ) {
        GenerateMenuBody(
            alMenuDataClient = alMenuDataClient,
            level = 0,
            closeMenu = closeMenu,
            menuClick = menuClick,
        )
    }
}

@Composable
internal fun GenerateMenuBody(
    alMenuDataClient: List<MenuDataClient>,
    level: Int,
    closeMenu: () -> Unit,
    menuClick: (action: AppAction, inNewTab: Boolean) -> Unit,
) {
    for (menuDataClient in alMenuDataClient) {
        menuDataClient.alSubMenu?.let { alSubMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = menuDataClient.caption,
                        fontWeight = FontWeight.Bold,
                    )
                },
                contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                onClick = {
                    menuDataClient.isExpanded.value = !menuDataClient.isExpanded.value
                },
            )
            if (menuDataClient.isExpanded.value) {
                GenerateMenuBody(
                    alMenuDataClient = alSubMenu,
                    level = level + 1,
                    menuClick = menuClick,
                    closeMenu = closeMenu,
                )
            }
        } ?: run {
            if (menuDataClient.action != null || menuDataClient.caption.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(menuDataClient.caption)
                    },
                    contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                    onClick = {
                        closeMenu()
                        menuDataClient.action?.let { menuAction ->
                            menuClick(menuAction, menuDataClient.inNewTab)
                        }
                    },
                )
            } else {
                HorizontalDivider(
                    modifier = Modifier.padding(start = getMenuPadding(level)),
                )
            }
        }
    }
}

private fun getMenuPadding(level: Int) = ((1 + level) * 16).dp

internal fun getClientSubMenu(
    appUserConfig: AppUserConfig,
    scaledWindowWidth: Int,
    scaleKoef: Float,
): MenuDataClient {
    val alClientSubMenu = mutableListOf<MenuDataClient>()

    alClientSubMenu += MenuDataClient(caption = "Пользователь: ${appUserConfig.currentUserName}")
    alClientSubMenu += MenuDataClient(caption = "Сменить пароль", action = AppAction(ActionType.CHANGE_PASSWORD))
    alClientSubMenu += MenuDataClient(caption = "Выход из системы", action = AppAction(ActionType.LOGOFF))

//    alClientSubMenu += MenuDataClient(caption = "")
//
//    alClientSubMenu += MenuDataClient(caption = "Установить вкладку как стартовую", action = AppAction(ActionType.SET_START_MODULE))
//    alClientSubMenu += MenuDataClient(caption = "Очистить установку стартовой", action = AppAction(ActionType.CLEAR_START_MODULE))

    if (appUserConfig.isAdmin) {
        alClientSubMenu += MenuDataClient(caption = "")

        alClientSubMenu += MenuDataClient(caption = "scaled window width = $scaledWindowWidth")
        alClientSubMenu += MenuDataClient(caption = "device pixel ratio = $scaleKoef")
//    alClientSubMenu.add(MenuDataClient(url = "", text = "touch screen = ${getStyleIsTouchScreen()}"))
    }

    return MenuDataClient(caption = "Дополнительно", alSubMenu = alClientSubMenu)
}
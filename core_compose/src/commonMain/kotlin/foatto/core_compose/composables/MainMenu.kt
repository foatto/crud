package foatto.core_compose.composables

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
import foatto.core_compose.Root
import foatto.core_compose.models.MenuDataClient

@Composable
fun MainMenu(
    isShowMainMenu: Boolean,
    alMenuDataClient: SnapshotStateList<MenuDataClient>,
    closeMenu: () -> Unit,
    menuClick: (url: String, inNewWindow: Boolean) -> Unit,
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
fun GenerateMenuBody(
    alMenuDataClient: List<MenuDataClient>,
    level: Int,
    closeMenu: () -> Unit,
    menuClick: (url: String, inNewWindow: Boolean) -> Unit,
) {
    for (menuDataClient in alMenuDataClient) {
        menuDataClient.alSubMenu?.let { alSubMenu ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = menuDataClient.text,
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
            if (menuDataClient.url.isNotEmpty() || menuDataClient.text.isNotEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(menuDataClient.text)
                    },
                    contentPadding = PaddingValues(start = getMenuPadding(level), end = 16.dp),
                    onClick = {
                        closeMenu()
                        if (menuDataClient.url.isNotEmpty()) {
                            menuClick(menuDataClient.url, menuDataClient.inNewWindow)
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

fun getClientSubMenu(
    currentUserName: String,
    scaledScreenWidth: Int,
    scaleKoef: Float,
): MenuDataClient {
    val alClientSubMenu = mutableListOf<MenuDataClient>()

    alClientSubMenu.add(MenuDataClient(url = "", text = "Пользователь: $currentUserName"))
    alClientSubMenu.add(MenuDataClient(url = Root.CMD_CHANGE_PASSWORD, text = "Сменить пароль"))
    alClientSubMenu.add(MenuDataClient(url = Root.CMD_LOGOFF, text = "Выход из системы"))

    alClientSubMenu.add(MenuDataClient(url = "", text = ""))

    alClientSubMenu.add(MenuDataClient(url = Root.CMD_SET_START_PAGE, text = "Установить вкладку как стартовую"))
    alClientSubMenu.add(MenuDataClient(url = Root.CMD_CLEAR_START_PAGE, text = "Очистить установку стартовой"))

    alClientSubMenu.add(MenuDataClient(url = "", text = ""))

    alClientSubMenu.add(MenuDataClient(url = "", text = "scaled screen width = $scaledScreenWidth"))
    alClientSubMenu.add(MenuDataClient(url = "", text = "device pixel ratio = $scaleKoef"))
//    alClientSubMenu.add(MenuDataClient(url = "", text = "touch screen = ${getStyleIsTouchScreen()}"))

    return MenuDataClient(url = "", text = "Дополнительно", alSubMenu = alClientSubMenu)
}
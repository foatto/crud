package foatto.core_compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import foatto.core_compose.models.MenuDataClient

@Composable
fun MainMenu(
    isWideScreen: Boolean,
    alMenuDataClient: SnapshotStateList<MenuDataClient>,
    isShowMainMenu: Boolean,
) {
    if (alMenuDataClient.isEmpty() || !isShowMainMenu) {
        return
    }

    Column(
        modifier = Modifier
            .width(20.dp)   //!!! подобрать точнее
            .fillMaxHeight(),
    ) {

    }
}

@Composable
fun generateMenuBody(
//    clickableMenu: iClickableMenu,
    isMainMenu: Boolean,
    alMenuDataClient: List<MenuDataClient>,
    level: Int,
) {
    for (menuDataClient in alMenuDataClient) {
        Column {
            menuDataClient.alSubMenu?.let { alSubMenu ->
//                            fontSize(arrStyleMenuFontSize[level])
//                            setPaddings(getStyleMenuItemPadding(level))
//                            backgroundColor(
//                                if (menuDataClient.isHover.value) {
//                                    if (level == 0) {
//                                        getColorMenuBackHover0()
//                                    } else {
//                                        getColorMenuBackHoverN()
//                                    }
//                                } else if (isMainMenu) {
//                                    getColorMainMenuBack()
//                                } else {
//                                    getColorPopupMenuBack()
//                                }
//                            )
//                            getColorMenuTextHover0()?.let { colorMenuTextHover ->
//                                color(
//                                    if (menuDataClient.isHover.value) {
//                                        colorMenuTextHover
//                                    } else {
//                                        colorMenuTextDefault
//                                    }
//                                )
//                            }
//                        }
//                        onMouseEnter {
//                            menuDataClient.isHover.value = true
//                        }
//                        onMouseLeave {
//                            menuDataClient.isHover.value = false
//                        }
//                    }
//                ) {
                Button(
                    onClick = {},   //!!!
                ) {
                    Text(
                        text = menuDataClient.text,
                    )
                }
                generateMenuBody(/*clickableMenu,*/ isMainMenu, alSubMenu, level + 1)
            } ?: run {
//                        fontSize(arrStyleMenuFontSize[level])
//                        setPaddings(getStyleMenuItemPadding(level))
//                        if (menuDataClient.url.isEmpty() && menuDataClient.text.isEmpty()) {
//                            textDecoration("line-through")
//                        }
//                        backgroundColor(
//                            if (menuDataClient.isHover.value) {
//                                if (level == 0) {
//                                    getColorMenuBackHover0()
//                                } else {
//                                    getColorMenuBackHoverN()
//                                }
//                                getColorMenuBackHoverN()
//                            } else if (isMainMenu) {
//                                getColorMainMenuBack()
//                            } else {
//                                getColorPopupMenuBack()
//                            }
//                        )
//                        color(
//                            if (menuDataClient.url.isNotEmpty() || menuDataClient.text.isNotEmpty()) {
//                                if (menuDataClient.isHover.value) {
//                                    getColorMenuTextHoverN()?.let {
//                                        getColorMenuTextHoverN()
//                                    } ?: colorMenuTextDefault
//                                } else {
//                                    colorMenuTextDefault
//                                }
//                            } else {
//                                getColorMenuDelimiter()
//                            }
//                        )
//                    }
                Button(
                    onClick = {},   //!!!
                ) {
                    if (menuDataClient.url.isNotEmpty()) {
                        Text(menuDataClient.text)
                    } else if (menuDataClient.text.isNotEmpty()) {
                        Text(menuDataClient.text + " >")
                    } else {
                        Divider()
                    }
                }
//                    onClick {
//                        menuDataClient.isHover.value = false
//                        clickableMenu.menuClick(menuDataClient.url, menuDataClient.inNewWindow)
//                    }
//                    onMouseEnter {
//                        menuDataClient.isHover.value = menuDataClient.text.isNotEmpty()
//                    }
//                    onMouseLeave {
//                        menuDataClient.isHover.value = false
//                    }
            }
        }
    }
}

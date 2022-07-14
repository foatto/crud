package foatto.core_web

fun menuGenerateBody(isMainMenu: Boolean, arrMenuDataName: String, clickFunctionName: String, menuDataPostFix: String): String {
    return """
        <template v-for="menuData_0 in $arrMenuDataName">
            <template v-if="menuData_0.arrSubMenu">
                <details>
                    <summary ${menuGenerateSummaryTag(isMainMenu, 0)}>
                        {{menuData_0.text}}
                    </summary>
                    <template v-for="menuData_1 in menuData_0.arrSubMenu">
                        <template v-if="menuData_1.arrSubMenu">
                            <details>
                                <summary ${menuGenerateSummaryTag(isMainMenu, 1)}>
                                    {{menuData_1.text}}
                                </summary>
                                <template v-for="menuData_2 in menuData_1.arrSubMenu">
                                    ${menuGenerateItem(isMainMenu, 2, clickFunctionName, menuDataPostFix)}
                                </template>
                            </details>
                        </template>
                        <template v-else>
                            ${menuGenerateItem(isMainMenu, 1, clickFunctionName, menuDataPostFix)}
                        </template>
                    </template>
                </details>
            </template>
            <template v-else>
                ${menuGenerateItem(isMainMenu, 0, clickFunctionName, menuDataPostFix)}
            </template>
        </template>
    """
}

private fun menuGenerateSummaryTag(isMainMenu: Boolean, summaryLevel: Int): String {
    val menuDataName = "menuData_$summaryLevel"
    var tag =
        """
            v-bind:style="[ 
                style_menu_summary_$summaryLevel
                , { 'background-color' : ( $menuDataName.itHover? '$colorMenuBackHover0' : '${if (isMainMenu) colorMainMenuBack() else colorPopupMenuBack()}' ) }
        """

    colorMenuTextHover0?.let {
        tag +=
            """
                , { 'color' : ( $menuDataName.itHover? '$colorMenuTextHover0' : '$colorMenuTextDefault}' ) }
            """
    }

    tag +=
        """
            ]"
            v-on:mouseenter="$menuDataName.itHover = true"
            v-on:mouseleave="$menuDataName.itHover = false"
        """

    return tag
}

private fun menuGenerateItem(isMainMenu: Boolean, menuLevel: Int, clickFunctionName: String, menuDataPostFix: String): String {
    val menuDataName = "menuData_$menuLevel"
    val tag =
        """
            <div v-bind:style="[ 
                style_menu_item_$menuLevel
                , { 'text-decoration' : ( $menuDataName.url || $menuDataName.text ? '' : 'line-through' ) }
                , { 'background-color' : ( $menuDataName.itHover? '$colorMenuBackHoverN' : '${if (isMainMenu) colorMainMenuBack() else colorPopupMenuBack()}' ) }
                , { 'color' : ( 
                        $menuDataName.url || $menuDataName.text 
                            ? ( $menuDataName.itHover 
                                ? '${colorMenuTextHoverN ?: colorMenuTextDefault}' 
                                : '$colorMenuTextDefault' 
                              )
                            : '${colorMenuDelimiter()}'
                    ) }
            ]"
            v-on:click="$menuDataName.url ? $clickFunctionName( $menuDataName$menuDataPostFix ) : null"
            v-on:mouseenter="$menuDataName.text ? $menuDataName.itHover = true : $menuDataName.itHover = false"
            v-on:mouseleave="$menuDataName.itHover = false"
        >
            {{ $menuDataName.url ? $menuDataName.text : ( $menuDataName.text ? $menuDataName.text + " &gt;" : "$MENU_DELIMITER" ) }}
        </div>
        """

    return tag
}

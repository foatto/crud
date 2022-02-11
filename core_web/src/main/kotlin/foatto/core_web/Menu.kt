package foatto.core_web

fun menuGenerateBody(arrMenuDataName: String, clickFunctionName: String, menuDataPostFix: String): String {
    return """
        <template v-for="menuData_0 in $arrMenuDataName">
            <template v-if="menuData_0.arrSubMenu">
                <details>
                    <summary ${menuGenerateSummaryTag(0)}>
                        {{menuData_0.text}}
                    </summary>
                    <template v-for="menuData_1 in menuData_0.arrSubMenu">
                        <template v-if="menuData_1.arrSubMenu">
                            <details>
                                <summary ${menuGenerateSummaryTag(1)}>
                                    {{menuData_1.text}}
                                </summary>
                                <template v-for="menuData_2 in menuData_1.arrSubMenu">
                                    ${menuGenerateItem(2, clickFunctionName, menuDataPostFix)}
                                </template>
                            </details>
                        </template>
                        <template v-else>
                            ${menuGenerateItem(1, clickFunctionName, menuDataPostFix)}
                        </template>
                    </template>
                </details>
            </template>
            <template v-else>
                ${menuGenerateItem(0, clickFunctionName, menuDataPostFix)}
            </template>
        </template>
    """
}

private fun menuGenerateSummaryTag(summaryLevel: Int): String {
    val menuDataName = "menuData_$summaryLevel"
    return """
        v-bind:style="[ style_menu_summary_$summaryLevel,
                        { 'background-color' : ( $menuDataName.itHover? '$colorCurrentAndHover' : '$colorMenuBack' ) }
                      ]"
            v-on:mouseenter="$menuDataName.itHover = true"
            v-on:mouseleave="$menuDataName.itHover = false"
    """
}

private fun menuGenerateItem(menuLevel: Int, clickFunctionName: String, menuDataPostFix: String): String {
    val menuDataName = "menuData_$menuLevel"
    return """
        <div v-bind:style="[ style_menu_item_$menuLevel,
                        { 'background-color' : ( $menuDataName.itHover? '$colorCurrentAndHover' : '$colorMenuBack' ) },
                        { 'text-decoration' : ( $menuDataName.url || $menuDataName.text ? '' : 'line-through' ) },
                        { 'color' : ( $menuDataName.url || $menuDataName.text ? '$COLOR_MAIN_TEXT' : '$colorMenuDelimiter' ) }
                      ]"
            v-on:click="$menuDataName.url ? $clickFunctionName( $menuDataName$menuDataPostFix ) : null"
            v-on:mouseenter="$menuDataName.text ? $menuDataName.itHover = true : $menuDataName.itHover = false"
            v-on:mouseleave="$menuDataName.itHover = false"
        >
            {{ $menuDataName.url ? $menuDataName.text : ( $menuDataName.text ? $menuDataName.text + " &gt;" : "$MENU_DELIMITER" ) }}
        </div>
    """
}

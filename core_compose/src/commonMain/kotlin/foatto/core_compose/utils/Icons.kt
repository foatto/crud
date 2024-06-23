package foatto.core_compose.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import foatto.core.app.*
import foatto.core_compose.getStyleOtherIconNameSuffix
import foatto.core_compose.getStyleToolbarIconNameSuffix

internal const val LOCAL_ICON_NAME_CLOSE = "close"

internal val hmNameToImageVector = mapOf(
    LOCAL_ICON_NAME_CLOSE to Icons.Default.Close,
)

internal val hmNameToUrl = mapOf(
    ICON_NAME_SELECT to "/web/images/ic_reply_${getStyleToolbarIconNameSuffix()}.png",

    ICON_NAME_ADD_FOLDER to "/web/images/ic_create_new_folder_${getStyleToolbarIconNameSuffix()}.png",
    ICON_NAME_ADD_ITEM to "/web/images/ic_add_${getStyleToolbarIconNameSuffix()}.png",

    //--- подразделение
    ICON_NAME_DIVISION to "/web/images/ic_folder_shared_${getStyleToolbarIconNameSuffix()}.png",
    //--- руководитель
    ICON_NAME_BOSS to "/web/images/ic_account_box_${getStyleToolbarIconNameSuffix()}.png",
    //--- работник
    ICON_NAME_WORKER to "/web/images/ic_account_circle_${getStyleToolbarIconNameSuffix()}.png",

    //--- подраздел
    ICON_NAME_FOLDER to "/web/images/ic_folder_open_${getStyleToolbarIconNameSuffix()}.png",

    //--- печать
    ICON_NAME_PRINT_TABLE to "/web/images/ic_print_${getStyleToolbarIconNameSuffix()}.png",

    ICON_NAME_ARCHIVE to "/web/images/ic_archive_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_DELETE to "/web/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_EXIT to "/web/images/ic_exit_to_app_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_FILE to "/web/images/ic_attachment_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_GRAPHIC to "/web/images/ic_timeline_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_MAP to "/web/images/ic_language_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_PRINT_FORM to "/web/images/ic_print_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_SAVE to "/web/images/ic_save_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_STATE to "/web/images/ic_router_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_UNARCHIVE to "/web/images/ic_unarchive_${getStyleOtherIconNameSuffix()}.png",
    ICON_NAME_VIDEO to "/web/images/ic_play_circle_outline_${getStyleOtherIconNameSuffix()}.png",
)

package foatto.core_compose.di

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import foatto.core.app.*
import foatto.core_compose.getStyleIconNameSuffix

internal const val LOCAL_ICON_NAME_CLOSE = "close"

internal val hmNameToImageVector = mapOf(
    LOCAL_ICON_NAME_CLOSE to Icons.Default.Close,
)

internal val hmNameToUrl = mapOf(
    ICON_NAME_SELECT to "/web/images/ic_reply_${getStyleIconNameSuffix()}.png",

    ICON_NAME_ADD_FOLDER to "/web/images/ic_create_new_folder_${getStyleIconNameSuffix()}.png",
    ICON_NAME_ADD_ITEM to "/web/images/ic_add_${getStyleIconNameSuffix()}.png",

    //--- подразделение
    ICON_NAME_DIVISION to "/web/images/ic_folder_shared_${getStyleIconNameSuffix()}.png",
    //--- руководитель
    ICON_NAME_BOSS to "/web/images/ic_account_box_${getStyleIconNameSuffix()}.png",
    //--- работник
    ICON_NAME_WORKER to "/web/images/ic_account_circle_${getStyleIconNameSuffix()}.png",

    //--- подраздел
    ICON_NAME_FOLDER to "/web/images/ic_folder_open_${getStyleIconNameSuffix()}.png",

    //--- печать
    ICON_NAME_PRINT_TABLE to "/web/images/ic_print_${getStyleIconNameSuffix()}.png",

    ICON_NAME_ARCHIVE to "/web/images/ic_archive_black_48dp.png",
    ICON_NAME_DELETE to "/web/images/ic_delete_forever_black_48dp.png",
    ICON_NAME_EXIT to "/web/images/ic_exit_to_app_black_48dp.png",
    ICON_NAME_FILE to "/web/images/ic_attachment_black_48dp.png",
    ICON_NAME_GRAPHIC to "/web/images/ic_timeline_black_48dp.png",
    ICON_NAME_MAP to "/web/images/ic_language_black_48dp.png",
    ICON_NAME_PRINT_FORM to "/web/images/ic_print_black_48dp.png",
    ICON_NAME_SAVE to "/web/images/ic_save_black_48dp.png",
    ICON_NAME_STATE to "/web/images/ic_router_black_48dp.png",
    ICON_NAME_UNARCHIVE to "/web/images/ic_unarchive_black_48dp.png",
    ICON_NAME_VIDEO to "/web/images/ic_play_circle_outline_black_48dp.png",

)


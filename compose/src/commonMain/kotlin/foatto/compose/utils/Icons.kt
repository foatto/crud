package foatto.compose.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import foatto.compose.getStyleOtherIconNameSuffix
import foatto.compose.getStyleToolbarIconNameSuffix
import foatto.core.IconName

//--- возможны дополнения в прикладных субмодулях
val hmNameToImageVector: MutableMap<String, ImageVector> = mutableMapOf(
    IconName.CLOSE to Icons.Default.Close,
)

//--- возможны дополнения в прикладных субмодулях
val hmNameToUrl: MutableMap<String, String> = mutableMapOf(
    IconName.SELECT to "/images/ic_reply_${getStyleToolbarIconNameSuffix()}.png",

    IconName.ADD_FOLDER to "/images/ic_create_new_folder_${getStyleToolbarIconNameSuffix()}.png",
    IconName.ADD_ITEM to "/images/ic_add_${getStyleToolbarIconNameSuffix()}.png",

    //--- подразделение
    IconName.DIVISION to "/images/ic_folder_shared_${getStyleToolbarIconNameSuffix()}.png",
    //--- руководитель
    IconName.BOSS to "/images/ic_account_box_${getStyleToolbarIconNameSuffix()}.png",
    //--- работник
    IconName.WORKER to "/images/ic_account_circle_${getStyleToolbarIconNameSuffix()}.png",

    //--- подраздел
    IconName.FOLDER to "/images/ic_folder_open_${getStyleToolbarIconNameSuffix()}.png",

    //--- печать
    IconName.PRINT_TABLE to "/images/ic_print_${getStyleToolbarIconNameSuffix()}.png",

    IconName.ARCHIVE to "/images/ic_archive_${getStyleOtherIconNameSuffix()}.png",
    IconName.COPY to "/images/ic_content_copy_${getStyleOtherIconNameSuffix()}.png",
    IconName.DELETE to "/images/ic_delete_forever_${getStyleOtherIconNameSuffix()}.png",
    IconName.EXIT to "/images/ic_exit_to_app_${getStyleOtherIconNameSuffix()}.png",
    IconName.FILE to "/images/ic_attachment_${getStyleOtherIconNameSuffix()}.png",
    IconName.CHART to "/images/ic_timeline_${getStyleOtherIconNameSuffix()}.png",
    IconName.MAP to "/images/ic_language_${getStyleOtherIconNameSuffix()}.png",
    IconName.PRINT_FORM to "/images/ic_print_${getStyleOtherIconNameSuffix()}.png",
    IconName.SAVE to "/images/ic_save_${getStyleOtherIconNameSuffix()}.png",
    IconName.SCHEME to "/images/ic_router_${getStyleOtherIconNameSuffix()}.png",
    IconName.SELECT to "/images/ic_reply_${getStyleOtherIconNameSuffix()}.png",
    IconName.UNARCHIVE to "/images/ic_unarchive_${getStyleOtherIconNameSuffix()}.png",
    IconName.VIDEO to "/images/ic_play_circle_outline_${getStyleOtherIconNameSuffix()}.png",
)

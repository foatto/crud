package foatto.server.model

class AppModuleConfig(
    val caption: String,
    val pageSize: Int? = null,
    val enabledAccessRoles: MutableSet<String>,
    val disabledAccessRoles: MutableSet<String>,
    val enabledFormAddRoles: MutableSet<String> = mutableSetOf(),
    val disabledFormAddRoles: MutableSet<String> = mutableSetOf(),
    val rowPermissions: MutableMap<String, Permission> = mutableMapOf(),
    val enabledReportUnlockRoles: MutableSet<String> = mutableSetOf(),
    val disabledReportUnlockRoles: MutableSet<String> = mutableSetOf(),
) {
    companion object {
        val DEFAULT_PAGE_SIZE: Int = 20
    }
}
/*
  authorization_need        INT,            -- флаг требования обязательной авторизации
  show_row_no               INT,            -- флаг показа столбца с номером строки
  show_user_column          INT,            -- флаг показа столбца с именем пользователя - владельцем записи
  newable                   INT,            -- флаг использования "новых/непрочитанных" записей
  new_auto_read             INT,            -- флаг автопрочитки новых записей в табличном режиме
  default_parent_user       INT,            -- флаг использования фильтра "от текущего пользователя по умолчанию"
 */
package foatto.core_server.app.server

class AliasConfig(
    val id: Int,
    val name: String,
    val controlClassName: String,
    val modelClassName: String,
    val descr: String,
    val isAuthorization: Boolean,     // класс требует обязательной аутентификации
    val isShowRowNo: Boolean,         // показывать ли столбец с номером строки в таблице
    val isShowUserColumn: Boolean,    // показывать ли столбец с именем пользователя
    val pageSize: Int,                // размер страницы при просмотре
    val isNewable: Boolean,           // есть понятие "новая/прочитанная запись"
    val isNewAutoRead: Boolean,       // автопрочитка новых записей при просмотре в таблице (без просмотра в форме)
    val isDefaultParentUser: Boolean, // если нет парента, то по умолчанию parent == user
)

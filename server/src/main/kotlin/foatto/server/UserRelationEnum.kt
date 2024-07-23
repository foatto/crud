package foatto.server

enum class UserRelationEnum {
    SELF,       // своя запись
    EQUAL,      // коллеги равного уровня
    BOSS,       // начальники
    WORKER,     // подчиненные
    OTHER,      // все остальные
    NOBODY,     // ничейные == общие, в т.ч. при отсутствии атрибута привязки к владельцу
}
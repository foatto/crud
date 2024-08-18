package foatto.server

enum class UserRelationEnum {
    SELF,       // своя запись
    EQUAL,      // коллеги равного уровня
    BOSS,       // начальники
    WORKER,     // подчиненные
    OTHER,      // все остальные
    NOBODY,     // ничейные == общие, т.е. при user_id == 0 или при отсутствии user_id
}
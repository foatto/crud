package foatto.core_server.app.server

object UserRelation {
    const val SELF = "self"                   // своя запись
    const val EQUAL = "equal"                 // коллеги равного уровня
    const val BOSS = "boss"                   // начальники
    const val WORKER = "worker"               // подчиненные
    const val OTHER = "other"                 // все остальные
    const val NOBODY = "nobody"               // ничейные == общие

    val arrNameDescr = arrayOf(
        Pair(SELF, SELF),
        Pair(EQUAL, EQUAL),
        Pair(BOSS, BOSS),
        Pair(WORKER, WORKER),
        Pair(OTHER, OTHER),
        Pair(NOBODY, NOBODY)
    )
}

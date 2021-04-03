package foatto.core_server.app.server

class DependData(val destTableName: String, val destFieldName: String, val type: Int, val valueForSet: Int) {

    companion object {
        const val CHECK = 0
        const val DELETE = 1
        const val SET = 2
    }

    constructor(aDestTableName: String, aDestFieldName: String) : this(aDestTableName, aDestFieldName, CHECK, 0)

    constructor(aDestTableName: String, aDestFieldName: String, aType: Int) : this(aDestTableName, aDestFieldName, aType, 0)

}

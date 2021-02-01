package foatto.core_server.app.server

import foatto.core.link.AppAction
import foatto.core_server.app.server.column.iColumn

class ChildData constructor(val group: String, val alias: Any, val column: iColumn?, val action: String, val isNewGroup: Boolean, val isDefaultOperation: Boolean) {

    constructor(aGroup: String, aAlias: Any, aColumn: iColumn, aAction: String) : this(aGroup, aAlias, aColumn, aAction, false, false)

    constructor(aGroup: String, aAlias: Any, aColumn: iColumn, aAction: String, aNewGroup: Boolean) : this(aGroup, aAlias, aColumn, aAction, aNewGroup, false)

    constructor(aAlias: Any) : this("", aAlias, null, AppAction.TABLE, false, false)

    constructor(aAlias: Any, aNewGroup: Boolean) : this("", aAlias, null, AppAction.TABLE, aNewGroup, false)

    constructor(aAlias: Any, aColumn: iColumn) : this("", aAlias, aColumn, AppAction.TABLE, false, false)

    constructor(aAlias: Any, aColumn: iColumn, aNewGroup: Boolean) : this("", aAlias, aColumn, AppAction.TABLE, aNewGroup, false)

    constructor(aAlias: Any, aColumn: iColumn, aNewGroup: Boolean, aDefaultOperation: Boolean) : this("", aAlias, aColumn, AppAction.TABLE, aNewGroup, aDefaultOperation)

    constructor(aAlias: Any, aColumn: iColumn, aAction: String) : this("", aAlias, aColumn, aAction, false, false)

    constructor(aAlias: Any, aColumn: iColumn, aAction: String, aNewGroup: Boolean) : this("", aAlias, aColumn, aAction, aNewGroup, false)

    constructor(aAlias: Any, aColumn: iColumn, aAction: String, aNewGroup: Boolean, aDefaultOperation: Boolean) : this("", aAlias, aColumn, aAction, aNewGroup, aDefaultOperation)

}

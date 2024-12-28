package foatto.server.util

import foatto.core.util.getRandomInt

fun getNextId(checkExistingId: (Int) -> Boolean): Int {
    var nextId: Int
    while (true) {
        nextId = getRandomInt()
        if (nextId == 0) {
            continue
        }
        if (checkExistingId(nextId)) {
            continue
        }
        return nextId
    }
}


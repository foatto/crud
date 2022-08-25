/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables.records


import foatto.jooq.core.tables.SystemReplicationSend

import org.jooq.Field
import org.jooq.Record3
import org.jooq.Row3
import org.jooq.impl.TableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemReplicationSendRecord() : TableRecordImpl<SystemReplicationSendRecord>(SystemReplicationSend.SYSTEM_REPLICATION_SEND), Record3<String?, String?, Long?> {

    var destName: String?
        set(value) = set(0, value)
        get() = get(0) as String?

    var sourName: String?
        set(value) = set(1, value)
        get() = get(1) as String?

    var timeKey: Long?
        set(value) = set(2, value)
        get() = get(2) as Long?

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row3<String?, String?, Long?> = super.fieldsRow() as Row3<String?, String?, Long?>
    override fun valuesRow(): Row3<String?, String?, Long?> = super.valuesRow() as Row3<String?, String?, Long?>
    override fun field1(): Field<String?> = SystemReplicationSend.SYSTEM_REPLICATION_SEND.DEST_NAME
    override fun field2(): Field<String?> = SystemReplicationSend.SYSTEM_REPLICATION_SEND.SOUR_NAME
    override fun field3(): Field<Long?> = SystemReplicationSend.SYSTEM_REPLICATION_SEND.TIME_KEY
    override fun component1(): String? = destName
    override fun component2(): String? = sourName
    override fun component3(): Long? = timeKey
    override fun value1(): String? = destName
    override fun value2(): String? = sourName
    override fun value3(): Long? = timeKey

    override fun value1(value: String?): SystemReplicationSendRecord {
        this.destName = value
        return this
    }

    override fun value2(value: String?): SystemReplicationSendRecord {
        this.sourName = value
        return this
    }

    override fun value3(value: Long?): SystemReplicationSendRecord {
        this.timeKey = value
        return this
    }

    override fun values(value1: String?, value2: String?, value3: Long?): SystemReplicationSendRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        return this
    }

    /**
     * Create a detached, initialised SystemReplicationSendRecord
     */
    constructor(destName: String? = null, sourName: String? = null, timeKey: Long? = null): this() {
        this.destName = destName
        this.sourName = sourName
        this.timeKey = timeKey
    }
}
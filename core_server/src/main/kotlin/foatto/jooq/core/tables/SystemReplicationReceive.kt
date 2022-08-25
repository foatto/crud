/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables


import foatto.jooq.core.Public
import foatto.jooq.core.indexes.SYSTEM_REPLICATION_RECEIVE_DEST
import foatto.jooq.core.indexes.SYSTEM_REPLICATION_RECEIVE_TIME_KEY
import foatto.jooq.core.tables.records.SystemReplicationReceiveRecord

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Index
import org.jooq.Name
import org.jooq.Record
import org.jooq.Row2
import org.jooq.Schema
import org.jooq.Table
import org.jooq.TableField
import org.jooq.TableOptions
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemReplicationReceive(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, SystemReplicationReceiveRecord>?,
    aliased: Table<SystemReplicationReceiveRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<SystemReplicationReceiveRecord>(
    alias,
    Public.PUBLIC,
    child,
    path,
    aliased,
    parameters,
    DSL.comment(""),
    TableOptions.table()
) {
    companion object {

        /**
         * The reference instance of <code>public.system_replication_receive</code>
         */
        val SYSTEM_REPLICATION_RECEIVE = SystemReplicationReceive()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<SystemReplicationReceiveRecord> = SystemReplicationReceiveRecord::class.java

    /**
     * The column <code>public.system_replication_receive.dest_name</code>.
     */
    val DEST_NAME: TableField<SystemReplicationReceiveRecord, String?> = createField(DSL.name("dest_name"), SQLDataType.VARCHAR(250), this, "")

    /**
     * The column <code>public.system_replication_receive.time_key</code>.
     */
    val TIME_KEY: TableField<SystemReplicationReceiveRecord, Long?> = createField(DSL.name("time_key"), SQLDataType.BIGINT, this, "")

    private constructor(alias: Name, aliased: Table<SystemReplicationReceiveRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<SystemReplicationReceiveRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>public.system_replication_receive</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>public.system_replication_receive</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>public.system_replication_receive</code> table reference
     */
    constructor(): this(DSL.name("system_replication_receive"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, SystemReplicationReceiveRecord>): this(Internal.createPathAlias(child, key), child, key, SYSTEM_REPLICATION_RECEIVE, null)
    override fun getSchema(): Schema = Public.PUBLIC
    override fun getIndexes(): List<Index> = listOf(SYSTEM_REPLICATION_RECEIVE_DEST, SYSTEM_REPLICATION_RECEIVE_TIME_KEY)
    override fun `as`(alias: String): SystemReplicationReceive = SystemReplicationReceive(DSL.name(alias), this)
    override fun `as`(alias: Name): SystemReplicationReceive = SystemReplicationReceive(alias, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): SystemReplicationReceive = SystemReplicationReceive(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): SystemReplicationReceive = SystemReplicationReceive(name, null)

    // -------------------------------------------------------------------------
    // Row2 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row2<String?, Long?> = super.fieldsRow() as Row2<String?, Long?>
}

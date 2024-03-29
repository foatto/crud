/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables


import foatto.jooq.core.Public
import foatto.jooq.core.indexes.SYSTEM_NEW_ROW
import foatto.jooq.core.indexes.SYSTEM_NEW_TABLE
import foatto.jooq.core.indexes.SYSTEM_NEW_USER
import foatto.jooq.core.tables.records.SystemNewRecord

import kotlin.collections.List

import org.jooq.Field
import org.jooq.ForeignKey
import org.jooq.Index
import org.jooq.Name
import org.jooq.Record
import org.jooq.Row3
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
open class SystemNew(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, SystemNewRecord>?,
    aliased: Table<SystemNewRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<SystemNewRecord>(
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
         * The reference instance of <code>public.system_new</code>
         */
        val SYSTEM_NEW = SystemNew()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<SystemNewRecord> = SystemNewRecord::class.java

    /**
     * The column <code>public.system_new.table_name</code>.
     */
    val TABLE_NAME: TableField<SystemNewRecord, String?> = createField(DSL.name("table_name"), SQLDataType.VARCHAR(250), this, "")

    /**
     * The column <code>public.system_new.row_id</code>.
     */
    val ROW_ID: TableField<SystemNewRecord, Int?> = createField(DSL.name("row_id"), SQLDataType.INTEGER, this, "")

    /**
     * The column <code>public.system_new.user_id</code>.
     */
    val USER_ID: TableField<SystemNewRecord, Int?> = createField(DSL.name("user_id"), SQLDataType.INTEGER, this, "")

    private constructor(alias: Name, aliased: Table<SystemNewRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<SystemNewRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>public.system_new</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>public.system_new</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>public.system_new</code> table reference
     */
    constructor(): this(DSL.name("system_new"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, SystemNewRecord>): this(Internal.createPathAlias(child, key), child, key, SYSTEM_NEW, null)
    override fun getSchema(): Schema = Public.PUBLIC
    override fun getIndexes(): List<Index> = listOf(SYSTEM_NEW_ROW, SYSTEM_NEW_TABLE, SYSTEM_NEW_USER)
    override fun `as`(alias: String): SystemNew = SystemNew(DSL.name(alias), this)
    override fun `as`(alias: Name): SystemNew = SystemNew(alias, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): SystemNew = SystemNew(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): SystemNew = SystemNew(name, null)

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row3<String?, Int?, Int?> = super.fieldsRow() as Row3<String?, Int?, Int?>
}

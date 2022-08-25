/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables


import foatto.jooq.core.Public
import foatto.jooq.core.indexes.SYSTEM_USER_ROLE_ROLE
import foatto.jooq.core.indexes.SYSTEM_USER_ROLE_USER
import foatto.jooq.core.keys.SYSTEM_USER_ROLE_PKEY
import foatto.jooq.core.tables.records.SystemUserRoleRecord

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
import org.jooq.UniqueKey
import org.jooq.impl.DSL
import org.jooq.impl.Internal
import org.jooq.impl.SQLDataType
import org.jooq.impl.TableImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemUserRole(
    alias: Name,
    child: Table<out Record>?,
    path: ForeignKey<out Record, SystemUserRoleRecord>?,
    aliased: Table<SystemUserRoleRecord>?,
    parameters: Array<Field<*>?>?
): TableImpl<SystemUserRoleRecord>(
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
         * The reference instance of <code>public.system_user_role</code>
         */
        val SYSTEM_USER_ROLE = SystemUserRole()
    }

    /**
     * The class holding records for this type
     */
    override fun getRecordType(): Class<SystemUserRoleRecord> = SystemUserRoleRecord::class.java

    /**
     * The column <code>public.system_user_role.id</code>.
     */
    val ID: TableField<SystemUserRoleRecord, Int?> = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "")

    /**
     * The column <code>public.system_user_role.role_id</code>.
     */
    val ROLE_ID: TableField<SystemUserRoleRecord, Int?> = createField(DSL.name("role_id"), SQLDataType.INTEGER, this, "")

    /**
     * The column <code>public.system_user_role.user_id</code>.
     */
    val USER_ID: TableField<SystemUserRoleRecord, Int?> = createField(DSL.name("user_id"), SQLDataType.INTEGER, this, "")

    private constructor(alias: Name, aliased: Table<SystemUserRoleRecord>?): this(alias, null, null, aliased, null)
    private constructor(alias: Name, aliased: Table<SystemUserRoleRecord>?, parameters: Array<Field<*>?>?): this(alias, null, null, aliased, parameters)

    /**
     * Create an aliased <code>public.system_user_role</code> table reference
     */
    constructor(alias: String): this(DSL.name(alias))

    /**
     * Create an aliased <code>public.system_user_role</code> table reference
     */
    constructor(alias: Name): this(alias, null)

    /**
     * Create a <code>public.system_user_role</code> table reference
     */
    constructor(): this(DSL.name("system_user_role"), null)

    constructor(child: Table<out Record>, key: ForeignKey<out Record, SystemUserRoleRecord>): this(Internal.createPathAlias(child, key), child, key, SYSTEM_USER_ROLE, null)
    override fun getSchema(): Schema = Public.PUBLIC
    override fun getIndexes(): List<Index> = listOf(SYSTEM_USER_ROLE_ROLE, SYSTEM_USER_ROLE_USER)
    override fun getPrimaryKey(): UniqueKey<SystemUserRoleRecord> = SYSTEM_USER_ROLE_PKEY
    override fun getKeys(): List<UniqueKey<SystemUserRoleRecord>> = listOf(SYSTEM_USER_ROLE_PKEY)
    override fun `as`(alias: String): SystemUserRole = SystemUserRole(DSL.name(alias), this)
    override fun `as`(alias: Name): SystemUserRole = SystemUserRole(alias, this)

    /**
     * Rename this table
     */
    override fun rename(name: String): SystemUserRole = SystemUserRole(DSL.name(name), null)

    /**
     * Rename this table
     */
    override fun rename(name: Name): SystemUserRole = SystemUserRole(name, null)

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------
    override fun fieldsRow(): Row3<Int?, Int?, Int?> = super.fieldsRow() as Row3<Int?, Int?, Int?>
}

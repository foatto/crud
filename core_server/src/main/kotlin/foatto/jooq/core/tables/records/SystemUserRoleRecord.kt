/*
 * This file is generated by jOOQ.
 */
package foatto.jooq.core.tables.records


import foatto.jooq.core.tables.SystemUserRole

import org.jooq.Field
import org.jooq.Record1
import org.jooq.Record3
import org.jooq.Row3
import org.jooq.impl.UpdatableRecordImpl


/**
 * This class is generated by jOOQ.
 */
@Suppress("UNCHECKED_CAST")
open class SystemUserRoleRecord() : UpdatableRecordImpl<SystemUserRoleRecord>(SystemUserRole.SYSTEM_USER_ROLE), Record3<Int?, Int?, Int?> {

    var id: Int?
        set(value) = set(0, value)
        get() = get(0) as Int?

    var roleId: Int?
        set(value) = set(1, value)
        get() = get(1) as Int?

    var userId: Int?
        set(value) = set(2, value)
        get() = get(2) as Int?

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    override fun key(): Record1<Int?> = super.key() as Record1<Int?>

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    override fun fieldsRow(): Row3<Int?, Int?, Int?> = super.fieldsRow() as Row3<Int?, Int?, Int?>
    override fun valuesRow(): Row3<Int?, Int?, Int?> = super.valuesRow() as Row3<Int?, Int?, Int?>
    override fun field1(): Field<Int?> = SystemUserRole.SYSTEM_USER_ROLE.ID
    override fun field2(): Field<Int?> = SystemUserRole.SYSTEM_USER_ROLE.ROLE_ID
    override fun field3(): Field<Int?> = SystemUserRole.SYSTEM_USER_ROLE.USER_ID
    override fun component1(): Int? = id
    override fun component2(): Int? = roleId
    override fun component3(): Int? = userId
    override fun value1(): Int? = id
    override fun value2(): Int? = roleId
    override fun value3(): Int? = userId

    override fun value1(value: Int?): SystemUserRoleRecord {
        this.id = value
        return this
    }

    override fun value2(value: Int?): SystemUserRoleRecord {
        this.roleId = value
        return this
    }

    override fun value3(value: Int?): SystemUserRoleRecord {
        this.userId = value
        return this
    }

    override fun values(value1: Int?, value2: Int?, value3: Int?): SystemUserRoleRecord {
        this.value1(value1)
        this.value2(value2)
        this.value3(value3)
        return this
    }

    /**
     * Create a detached, initialised SystemUserRoleRecord
     */
    constructor(id: Int? = null, roleId: Int? = null, userId: Int? = null): this() {
        this.id = id
        this.roleId = roleId
        this.userId = userId
    }
}

package foatto.server.repository

import foatto.server.entity.GroupEntity
import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ObjectRepository : JpaRepository<ObjectEntity, Int> {

    fun findByUserIdAndName(userId: Int, name: String): List<GroupEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            WHERE oe.department.id = ?1
        """
    )
    fun findByDepartmentId(departmentId: Int): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            WHERE oe.group.id = ?1
        """
    )
    fun findByGroupId(groupId: Int): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            WHERE oe.id <> 0
                AND oe.userId IN ?1
        """
    )
    fun findByUserIdIn(userIds: List<Int>, pageRequest: PageRequest): Page<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            WHERE oe.id <> 0
                AND oe.userId IN ?1
                AND (
                        LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.department.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.group.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<ObjectEntity>

}

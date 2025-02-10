package foatto.server.repository

import foatto.server.entity.ObjectEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ObjectRepository : JpaRepository<ObjectEntity, Int> {

    fun findByUserIdAndName(userId: Int, name: String): List<ObjectEntity>
    fun findByUserIdInAndName(userIds: List<Int>, name: String): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            LEFT JOIN oe.department de
            WHERE de.id = ?1
        """
    )
    fun findByDepartmentId(departmentId: Int): List<ObjectEntity>

    @Query(
        """
            SELECT oe
            FROM ObjectEntity oe
            LEFT JOIN oe.group ge
            WHERE ge.id = ?1
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
            LEFT JOIN oe.department de
            LEFT JOIN oe.group ge
            WHERE oe.id <> 0
                AND oe.userId IN ?1
                AND (
                        LOWER(oe.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(oe.model) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(de.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                     OR LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
                )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<ObjectEntity>

}

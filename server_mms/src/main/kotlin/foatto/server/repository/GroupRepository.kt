package foatto.server.repository

import foatto.server.entity.GroupEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GroupRepository : JpaRepository<GroupEntity, Int> {

    fun findByUserIdAndName(userId: Int, name: String): List<GroupEntity>

    @Query(
        """
            SELECT ge
            FROM GroupEntity ge
            WHERE ge.id <> 0
                AND ge.userId IN ?1
        """
    )
    fun findByUserIdIn(userIds: List<Int>, pageRequest: PageRequest): Page<GroupEntity>

    @Query(
        """
            SELECT ge
            FROM GroupEntity ge
            WHERE ge.id <> 0
                AND ge.userId IN ?1
                AND LOWER(ge.name) LIKE LOWER( CONCAT( '%', ?2, '%' ) )
        """
    )
    fun findByUserIdInAndFilter(userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<GroupEntity>
}
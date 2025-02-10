package foatto.server.repository

import foatto.server.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByLogin(login: String): List<UserEntity>
    fun findByLoginAndPassword(login: String, password: String): List<UserEntity>
    fun findByFullName(fullName: String): List<UserEntity>
    fun findByParentId(parentId: Int): List<UserEntity>
    fun findByParentIdAndOrgType(parentId: Int, orgType: Int): List<UserEntity>

    @Query(
        """
            SELECT ue
            FROM UserEntity ue
            WHERE ue.id <> 0
                AND ue.parentId = ?1
                AND ue.userId IN ?2
        """
    )
    fun findByParentIdAndUserIdIn(parentId: Int, userIds: List<Int>, pageRequest: PageRequest): Page<UserEntity>

    @Query(
        """
            SELECT ue
            FROM UserEntity ue
            WHERE ue.id <> 0
                AND ue.parentId = ?1
                AND ue.userId IN ?2
                AND (
                        LOWER(ue.login) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.fullName) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.eMail) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                     OR LOWER(ue.contactInfo) LIKE LOWER( CONCAT( '%', ?3, '%' ) )
                )
        """
    )
    fun findByParentIdAndUserIdInAndFilter(parentId: Int, userIds: List<Int>, findText: String, pageRequest: PageRequest): Page<UserEntity>
}
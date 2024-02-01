package foatto.spring.repositories

import foatto.spring.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Int> {
    fun findByOrgType(orgType: Int): List<UserEntity>
    fun findByOrgTypeAndParentId(orgType: Int, parentId: Int): List<UserEntity>
}

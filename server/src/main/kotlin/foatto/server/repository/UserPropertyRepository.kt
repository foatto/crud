package foatto.server.repository

import foatto.server.entity.UserPropertyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserPropertyRepository : JpaRepository<UserPropertyEntity, Int> {
    fun findByUserId(userId: Int): List<UserPropertyEntity>
    fun findByUserIdAndName(userId: Int, Name: String): List<UserPropertyEntity>
}
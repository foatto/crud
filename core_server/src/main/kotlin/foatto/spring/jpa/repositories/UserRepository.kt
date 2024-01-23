package foatto.spring.jpa.repositories

import foatto.spring.jpa.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, Int>

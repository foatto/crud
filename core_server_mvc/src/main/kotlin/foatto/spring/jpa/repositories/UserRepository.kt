package foatto.spring.jpa.repositories

import foatto.spring.jpa.entities.UserEntity
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<UserEntity, Int>

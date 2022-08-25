package foatto.jpa.repositories

import foatto.jpa.entities.UserEntity
import org.springframework.data.repository.CrudRepository

interface UserRepository : CrudRepository<UserEntity, Int>

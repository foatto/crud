package foatto.ts.spring.repositories

import foatto.ts.spring.entities.ObjectEntity
import org.springframework.data.repository.CrudRepository

interface ObjectRepository : CrudRepository<ObjectEntity, Int>
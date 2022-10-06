package foatto.mms.spring.repositories

import foatto.mms.spring.entities.ObjectEntity
import org.springframework.data.repository.CrudRepository

interface ObjectRepository : CrudRepository<ObjectEntity, Int>
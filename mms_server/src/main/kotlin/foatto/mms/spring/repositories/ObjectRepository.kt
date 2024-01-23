package foatto.mms.spring.repositories

import foatto.mms.spring.entities.ObjectEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ObjectRepository : JpaRepository<ObjectEntity, Int>
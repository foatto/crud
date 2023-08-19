package foatto.ts.spring.repositories

import foatto.ts.spring.entities.ObjectEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ObjectRepository : JpaRepository<ObjectEntity, Int>
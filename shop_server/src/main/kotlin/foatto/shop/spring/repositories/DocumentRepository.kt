package foatto.shop.spring.repositories

import foatto.shop.spring.entities.DocumentEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DocumentRepository : JpaRepository<DocumentEntity, Int>


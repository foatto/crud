package foatto.shop.spring.repositories

import foatto.shop.spring.entities.DocumentEntity
import org.springframework.data.repository.CrudRepository

interface DocumentRepository : CrudRepository<DocumentEntity, Int>


package foatto.shop.spring.repositories

import foatto.shop.spring.entities.CatalogEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CatalogRepository : JpaRepository<CatalogEntity, Int>

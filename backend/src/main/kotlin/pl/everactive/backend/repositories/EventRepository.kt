package pl.everactive.backend.repositories

import org.springframework.data.jpa.repository.JpaRepository
import pl.everactive.backend.entities.EventEntity

interface EventRepository : JpaRepository<EventEntity, Long>

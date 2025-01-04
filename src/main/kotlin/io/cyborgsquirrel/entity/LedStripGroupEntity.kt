package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable
//import jakarta.persistence.*

//@Entity
//@Table(name = "led_strip_group")
@Serdeable
@MappedEntity("led_strip_groups")
data class LedStripGroupEntity(
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id", updatable = false, nullable = false)
    @Id
    @GeneratedValue
    var id: Long = -1,

//    @OneToMany(mappedBy = "group")
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "group")
    var strips: Set<GroupMemberLedStripEntity> = setOf(),

//    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String? = null,

//    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null,
)
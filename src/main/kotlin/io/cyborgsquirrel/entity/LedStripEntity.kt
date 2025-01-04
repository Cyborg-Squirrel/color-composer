package io.cyborgsquirrel.entity

import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.serde.annotation.Serdeable
import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id

//import jakarta.persistence.*

//@Entity
//@Table(name = "led_strip")
@Serdeable
@MappedEntity("led_strips")
data class LedStripEntity(
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "id", updatable = false, nullable = false)
    @Id
    @GeneratedValue
    var id: Long = -1,

//    @ManyToOne
//    @Relation(value = Relation.Kind.MANY_TO_ONE)
    @Relation(value = Relation.Kind.MANY_TO_ONE)
    var client: LedStripClientEntity? = null,

//    @OneToMany(mappedBy = "led_strip_map")
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "strip")
    var groupMemberStrips: Set<GroupMemberLedStripEntity> = setOf(),

//    @Column(name = "uuid", nullable = false, unique = true)
    var uuid: String? = null,

//    @Column(name = "name", nullable = false, unique = true)
    var name: String? = null,

//    @Column(name = "length", nullable = false, unique = false)
    var length: Int? = null,
)
package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.entity.LedStripEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.util.*

@JdbcRepository(dialect = Dialect.H2)
interface H2LedStripRepository : CrudRepository<LedStripEntity, Long> {
    // TODO rename so this can be findByGroupMembers?
    @Join(value = "client", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    fun findByMembers(member: GroupMemberLedStripEntity): List<LedStripEntity>

    @Join(value = "client", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    fun findByMembersIn(members: Set<GroupMemberLedStripEntity>): List<LedStripEntity>

    @Join(value = "client", type = Join.Type.LEFT_FETCH)
    @Join(value = "effects", type = Join.Type.LEFT_FETCH)
    @Join(value = "members", type = Join.Type.LEFT_FETCH)
    fun findByUuid(uuid: String): Optional<LedStripEntity>
}
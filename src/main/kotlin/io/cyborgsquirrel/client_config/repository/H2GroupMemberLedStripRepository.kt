package io.cyborgsquirrel.client_config.repository

import io.cyborgsquirrel.entity.GroupMemberLedStripEntity
import io.cyborgsquirrel.entity.LedStripGroupEntity
import io.micronaut.data.annotation.Join
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository

@JdbcRepository(dialect = Dialect.H2)
interface H2GroupMemberLedStripRepository : CrudRepository<GroupMemberLedStripEntity, Long> {
    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    fun queryAll(): List<GroupMemberLedStripEntity>

    @Join(value = "group", type = Join.Type.LEFT_FETCH)
    @Join(value = "strip", type = Join.Type.LEFT_FETCH)
    fun findByGroup(group: LedStripGroupEntity): List<GroupMemberLedStripEntity>
}
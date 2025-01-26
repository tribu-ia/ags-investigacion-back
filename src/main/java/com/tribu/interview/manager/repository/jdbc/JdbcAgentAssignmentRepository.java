package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.repository.mapper.AgentAssignmentRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcAgentAssignmentRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AgentAssignmentRowMapper rowMapper = new AgentAssignmentRowMapper();

    private static final String SELECT_BASE = """
        SELECT 
            aa.id as id,
            aa.investigador_id as researcher_id,
            i.name as researcher_name,
            i.email as researcher_email,
            aa.agent_id as agent_id,
            a.name as agent_name,
            aa.status as status,
            aa.assigned_at as assigned_at
        FROM agent_assignments aa
        JOIN investigadores i ON aa.investigador_id = i.id
        JOIN ai_agents a ON aa.agent_id = a.id
    """;

    public Optional<AgentAssignment> findById(String id) {
        String sql = SELECT_BASE + " WHERE aa.id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        List<AgentAssignment> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<AgentAssignment> findActiveAssignmentByAgentId(String agentId) {
        String sql = SELECT_BASE + " WHERE aa.agent_id = :agentId AND aa.status = 'active'";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("agentId", agentId);

        List<AgentAssignment> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AgentAssignment> findByResearcherId(String researcherId) {
        String sql = SELECT_BASE + " WHERE aa.investigador_id = :researcherId";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("researcherId", researcherId);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public AgentAssignment save(AgentAssignment assignment) {
        if (assignment.getId() == null) {
            return insert(assignment);
        }
        return update(assignment);
    }

    private AgentAssignment insert(AgentAssignment assignment) {
        String sql = """
            INSERT INTO agent_assignments (id, investigador_id, agent_id, status, assigned_at)
            VALUES (:id, :researcherId, :agentId, :status, :assignedAt)
        """;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(assignment)
            .addValue("id", id);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        
        assignment.setId(id);
        return assignment;
    }

    private AgentAssignment update(AgentAssignment assignment) {
        String sql = """
            UPDATE agent_assignments 
            SET investigador_id = :researcherId,
                agent_id = :agentId,
                status = :status,
                assigned_at = :assignedAt
            WHERE id = :id
        """;

        MapSqlParameterSource params = createParameterSource(assignment)
            .addValue("id", assignment.getId());

        jdbcTemplate.update(sql, params);
        return assignment;
    }

    private MapSqlParameterSource createParameterSource(AgentAssignment assignment) {
        return new MapSqlParameterSource()
            .addValue("researcherId", assignment.getResearcher().getId())
            .addValue("agentId", assignment.getAgent().getId())
            .addValue("status", assignment.getStatus())
            .addValue("assignedAt", assignment.getAssignedAt());
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM agent_assignments WHERE status = :status";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("status", status);

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }
} 
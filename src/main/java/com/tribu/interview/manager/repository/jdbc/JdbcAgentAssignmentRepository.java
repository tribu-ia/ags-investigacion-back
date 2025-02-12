package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.dto.enums.ResearcherTypeEnum;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.repository.mapper.AgentAssignmentRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
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
            a.slug as agent_slug,
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
            INSERT INTO agent_assignments (id, investigador_id, agent_id, status, assigned_at, role)
            VALUES (:id, :researcherId, :agentId, :status, :assignedAt, :role)
        """;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParams(assignment, id);

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
                role = :role,
                assigned_at = :assignedAt
            WHERE id = :id
        """;

        MapSqlParameterSource params = createParams(assignment, assignment.getId());

        jdbcTemplate.update(sql, params);
        return assignment;
    }

    private MapSqlParameterSource createParams(AgentAssignment assignment, String id) {
        return new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("researcherId", assignment.getResearcher().getId())
            .addValue("agentId", assignment.getAgent().getId())
            .addValue("status", assignment.getStatus())
            .addValue("role", assignment.getRole())
            .addValue("assignedAt", assignment.getAssignedAt());
    }

    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM agent_assignments WHERE status = :status";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("status", status);

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    public Optional<AgentAssignment> findActiveAssignmentByResearcherId(String researcherId) {
        String sql = """
            SELECT 
                aa.id,
                aa.status,
                aa.assigned_at,
                aa.role,
                i.id as researcher_id,
                i.name as researcher_name,
                i.email as researcher_email,
                i.avatar_url as researcher_avatar_url,
                i.repository_url as researcher_repository_url,
                i.linkedin_profile as researcher_linkedin_url,
                ag.id as agent_id,
                ag.name as agent_name,            
                ag.industry as agent_industry,
                ag.category as agent_category,       
                ag.short_description as agent_description
            FROM agent_assignments aa
            INNER JOIN investigadores i ON aa.investigador_id = i.id
            INNER JOIN ai_agents ag ON aa.agent_id = ag.id
            WHERE i.id = :researcherId
            AND aa.status = 'active'
            ORDER BY aa.assigned_at DESC
            LIMIT 1
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("researcherId", researcherId);
        
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
                // Construir el investigador
                Researcher researcher = Researcher.builder()
                    .id(rs.getString("researcher_id"))
                    .name(rs.getString("researcher_name"))
                    .email(rs.getString("researcher_email"))
                    .avatarUrl(rs.getString("researcher_avatar_url"))
                    .repositoryUrl(rs.getString("researcher_repository_url"))
                    .linkedinProfile(rs.getString("researcher_linkedin_url"))
                    .build();

                // Construir el agente
                AIAgent agent = AIAgent.builder()
                    .id(rs.getString("agent_id"))
                    .name(rs.getString("agent_name"))
                        .industry(rs.getString("agent_industry"))
                        .category(rs.getString("agent_category"))
                    .shortDescription(rs.getString("agent_description"))
                    .build();

                // Construir la asignación
                return AgentAssignment.builder()
                    .id(rs.getString("id"))
                    .researcher(researcher)
                    .agent(agent)
                    .status(rs.getString("status"))
                    .role(rs.getString("role"))
                    .assignedAt(rs.getTimestamp("assigned_at").toLocalDateTime())
                    .build();
            }));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean existsByResearcherIdAndAgentId(String researcherId, String agentId) {
        String sql = """
            SELECT COUNT(*)
            FROM agent_assignments
            WHERE investigador_id = :researcherId
            AND agent_id = :agentId
            AND status = 'active'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("researcherId", researcherId)
            .addValue("agentId", agentId);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByResearcherIdAndAgentIdWhitContributorRole(String researcherId) {
        String role = ResearcherTypeEnum.CONTRIBUTOR.name();

        String sql = """
            SELECT COUNT(*)
            FROM agent_assignments
            WHERE investigador_id = :researcherId
            AND status = 'active'
            AND role = :role
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("researcherId", researcherId)
                .addValue("role", role);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }


    public boolean existsByAgentIdAndRole(String agentId, String role) {
        String sql = """
            SELECT COUNT(*)
            FROM agent_assignments
            WHERE agent_id = :agentId
            AND role = :role
            AND status = 'active'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("agentId", agentId)
            .addValue("role", role);

        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    private AgentAssignment mapToAgentAssignment(ResultSet rs, int rowNum) throws SQLException {
        return AgentAssignment.builder()
            .id(rs.getString("id"))
            .researcher(mapToResearcher(rs))
            .agent(mapToAgent(rs))
            .status(rs.getString("status"))
            .role(rs.getString("role"))
            .assignedAt(rs.getTimestamp("assigned_at").toLocalDateTime())
            .build();
    }

    private Researcher mapToResearcher(ResultSet rs) throws SQLException {
        return Researcher.builder()
            .id(rs.getString("researcher_id"))
            .name(rs.getString("researcher_name"))
            .email(rs.getString("researcher_email"))
            .avatarUrl(rs.getString("researcher_avatar_url"))
            .repositoryUrl(rs.getString("researcher_repository_url"))
            .linkedinProfile(rs.getString("researcher_linkedin_url"))
            .build();
    }

    private AIAgent mapToAgent(ResultSet rs) throws SQLException {
        return AIAgent.builder()
            .id(rs.getString("agent_id"))
            .name(rs.getString("agent_name"))
            .industry(rs.getString("agent_industry"))
            .category(rs.getString("agent_category"))
            .shortDescription(rs.getString("agent_description"))
            .build();
    }
} 
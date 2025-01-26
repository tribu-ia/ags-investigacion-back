package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.model.AgentDocumentation;
import com.tribu.interview.manager.repository.mapper.AgentDocumentationRowMapper;
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
public class JdbcAgentDocumentationRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AgentDocumentationRowMapper rowMapper = new AgentDocumentationRowMapper();

    private static final String SELECT_BASE = """
        SELECT d.id, d.agent_id, d.title, d.content, d.url, d.type
        FROM agent_documentation d
    """;

    public Optional<AgentDocumentation> findById(String id) {
        String sql = SELECT_BASE + " WHERE d.id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        List<AgentDocumentation> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AgentDocumentation> findByAgentId(String agentId) {
        String sql = SELECT_BASE + " WHERE d.agent_id = :agentId";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("agentId", agentId);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public AgentDocumentation save(AgentDocumentation documentation) {
        if (documentation.getId() == null) {
            return insert(documentation);
        }
        return update(documentation);
    }

    private AgentDocumentation insert(AgentDocumentation documentation) {
        String sql = """
            INSERT INTO agent_documentation (id, agent_id, title, content, url, type)
            VALUES (:id, :agentId, :title, :content, :url, :type)
        """;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(documentation)
            .addValue("id", id);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        
        documentation.setId(id);
        return documentation;
    }

    private AgentDocumentation update(AgentDocumentation documentation) {
        String sql = """
            UPDATE agent_documentation 
            SET agent_id = :agentId,
                title = :title,
                content = :content,
                url = :url,
                type = :type
            WHERE id = :id
        """;

        MapSqlParameterSource params = createParameterSource(documentation)
            .addValue("id", documentation.getId());

        jdbcTemplate.update(sql, params);
        return documentation;
    }

    private MapSqlParameterSource createParameterSource(AgentDocumentation documentation) {
        return new MapSqlParameterSource()
            .addValue("agentId", documentation.getAgentId())
            .addValue("title", documentation.getTitle())
            .addValue("content", documentation.getContent())
            .addValue("url", documentation.getUrl())
            .addValue("type", documentation.getType());
    }

    public long countDocumentedAgents() {
        String sql = "SELECT COUNT(DISTINCT agent_id) FROM agent_documentation";
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    }
} 
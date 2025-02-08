package com.tribu.interview.manager.repository.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tribu.interview.manager.model.AgentDocumentation;
import com.tribu.interview.manager.service.impl.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcAgentDocumentationRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    public Optional<AgentDocumentation> findByAssignmentId(String assignmentId) {
        String sql = """
            SELECT *
            FROM agent_documentation
            WHERE assignment_id = :assignmentId
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("assignmentId", assignmentId);
            
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, this::mapToDocumentation));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    public AgentDocumentation save(AgentDocumentation doc) {
        if (doc.getId() == null) {
            return insert(doc);
        }
        return update(doc);
    }
    
    private AgentDocumentation insert(AgentDocumentation doc) {
        String sql = """
            INSERT INTO agent_documentation (
                id, assignment_id, markdown_content, documentation_date, status
            ) VALUES (
                :id, :assignmentId, :markdownContent, :documentationDate, :status
            )
        """;
        
        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParams(doc, id);
        
        jdbcTemplate.update(sql, params);
        doc.setId(id);
        return doc;
    }
    
    private AgentDocumentation update(AgentDocumentation doc) {
        String sql = """
            UPDATE agent_documentation 
            SET 
                markdown_content = :markdownContent,
                status = :status
            WHERE id = :id
        """;
        
        MapSqlParameterSource params = createParams(doc, doc.getId());
        
        int updated = jdbcTemplate.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Documentation not found: " + doc.getId());
        }
        
        return doc;
    }
    
    private MapSqlParameterSource createParams(AgentDocumentation doc, String id) {
        return new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("assignmentId", doc.getAssignmentId())
            .addValue("markdownContent", doc.getMarkdownContent())
            .addValue("documentationDate", doc.getDocumentationDate())
            .addValue("status", doc.getStatus());
    }
    
    private AgentDocumentation mapToDocumentation(ResultSet rs, int rowNum) throws SQLException {
        return AgentDocumentation.builder()
            .id(rs.getString("id"))
            .assignmentId(rs.getString("assignment_id"))
            .markdownContent(rs.getString("markdown_content"))
            .documentationDate(rs.getTimestamp("documentation_date").toLocalDateTime())
            .status(rs.getString("status"))
            .build();
    }
} 
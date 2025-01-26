package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.model.AgentDocumentation;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgentDocumentationRowMapper implements RowMapper<AgentDocumentation> {
    
    @Override
    public AgentDocumentation mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AgentDocumentation.builder()
            .id(rs.getString("id"))
            .agentId(rs.getString("agent_id"))
            .title(rs.getString("title"))
            .content(rs.getString("content"))
            .url(rs.getString("url"))
            .type(rs.getString("type"))
            .build();
    }
} 
package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.AgentAssignment;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgentAssignmentRowMapper implements RowMapper<AgentAssignment> {
    
    @Override
    public AgentAssignment mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AgentAssignment.builder()
            .id(rs.getString("id"))
            .status(rs.getString("status"))
            .assignedAt(rs.getTimestamp("assigned_at").toLocalDateTime())
                .agent(AIAgent.builder()
                        .name(rs.getString("agent_name"))
                        .slug(rs.getString("agent_slug"))
                        .build())
            .build();
    }
} 
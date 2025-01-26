package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.model.AIAgent;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PresentationRowMapper implements RowMapper<Presentation> {
    @Override
    public Presentation mapRow(ResultSet rs, int rowNum) throws SQLException {
        Researcher researcher = Researcher.builder()
            .name(rs.getString("researcher_name"))
            .avatarUrl(rs.getString("researcher_avatar_url"))
            .repositoryUrl(rs.getString("researcher_repository_url"))
            .linkedinProfile(rs.getString("researcher_linkedin_url"))
            .build();

        AIAgent agent = AIAgent.builder()
            .name(rs.getString("agent_name"))
            .build();

        AgentAssignment assignment = AgentAssignment.builder()
            .researcher(researcher)
            .agent(agent)
            .build();

        return Presentation.builder()
            .id(rs.getString("id"))
            .assignment(assignment)
            .presentationDate(rs.getTimestamp("presentation_date").toLocalDateTime())
            .status(rs.getString("status"))
            .build();
    }
} 
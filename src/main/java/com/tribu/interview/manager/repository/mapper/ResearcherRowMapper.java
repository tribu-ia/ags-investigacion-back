package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.model.Researcher;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ResearcherRowMapper implements RowMapper<Researcher> {
    
    @Override
    public Researcher mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Researcher.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .email(rs.getString("email"))
            .phone(rs.getString("phone"))
            .githubUsername(rs.getString("github_username"))
            .avatarUrl(rs.getString("avatar_url"))
            .repositoryUrl(rs.getString("repository_url"))
            .linkedinProfile(rs.getString("linkedin_profile"))
                .currentRol(rs.getString("current_rol"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .build();
    }
} 
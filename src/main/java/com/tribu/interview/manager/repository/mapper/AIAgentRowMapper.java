package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.model.AIAgent;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AIAgentRowMapper implements RowMapper<AIAgent> {
    
    @Override
    public AIAgent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AIAgent.builder()
            .id(rs.getString("id"))
            .name(rs.getString("name"))
            .createdBy(rs.getString("created_by"))
            .website(rs.getString("website"))
            .access(rs.getString("access"))
            .pricingModel(rs.getString("pricing_model"))
            .category(rs.getString("category"))
            .industry(rs.getString("industry"))
            .shortDescription(rs.getString("short_description"))
            .longDescription(rs.getString("long_description"))
            .keyFeatures(rs.getString("key_features"))
            .useCases(rs.getString("use_cases"))
            .tags(rs.getString("tags"))
            .logo(rs.getString("logo"))
            .logoFileName(rs.getString("logo_file_name"))
            .image(rs.getString("image"))
            .imageFileName(rs.getString("image_file_name"))
            .video(rs.getString("video"))
            .upvotes(rs.getInt("upvotes"))
            .approved(rs.getBoolean("approved"))
            .createdAt(rs.getObject("created_at", LocalDateTime.class))
            .slug(rs.getString("slug"))
            .version(rs.getString("version"))
            .featured(rs.getBoolean("featured"))
            .assignmentStatus(rs.getString("assignment_status"))
            .assignedAt(rs.getObject("assigned_at", LocalDateTime.class))
            .assignedToName(rs.getString("assigned_to_name"))
            .assignedToEmail(rs.getString("assigned_to_email"))
            .build();
    }
} 
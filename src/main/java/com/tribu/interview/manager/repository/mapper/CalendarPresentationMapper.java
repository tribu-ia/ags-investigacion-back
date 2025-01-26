package com.tribu.interview.manager.repository.mapper;

import com.tribu.interview.manager.dto.CalendarPresentationDto;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CalendarPresentationMapper implements RowMapper<CalendarPresentationDto> {
    @Override
    public CalendarPresentationDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return CalendarPresentationDto.builder()
            .id(rs.getString("id"))
            .name(rs.getString("researcher_name"))
            .avatarUrl(rs.getString("researcher_avatar_url"))
            .repositoryUrl(rs.getString("researcher_repository_url"))
            .linkedinUrl(rs.getString("researcher_linkedin_url"))
            .role(rs.getString("status"))
            .presentation(rs.getString("agent_name"))
            .presentationDateTime(rs.getTimestamp("presentation_date") != null ? 
                rs.getTimestamp("presentation_date").toLocalDateTime() : null)
            .build();
    }
} 
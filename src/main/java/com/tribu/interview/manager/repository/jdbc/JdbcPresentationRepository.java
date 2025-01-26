package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.dto.CalendarPresentationDto;
import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.repository.mapper.CalendarPresentationMapper;
import com.tribu.interview.manager.repository.mapper.PresentationRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcPresentationRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PresentationRowMapper rowMapper = new PresentationRowMapper();

    private static final String SELECT_BASE = """
        SELECT 
            p.id,
            i.name as researcher_name,
            i.avatar_url as researcher_avatar_url,
            i.repository_url as researcher_repository_url,
            i.linkedin_url as researcher_linkedin_url,
            a.name as agent_name,
            p.presentation_date,
            p.status
        FROM presentations p
        JOIN agent_assignments aa ON p.assignment_id = aa.id
        JOIN investigadores i ON aa.investigador_id = i.id
        JOIN ai_agents a ON aa.agent_id = a.id
    """;

    public List<CalendarPresentationDto> findPresentationsForRange(LocalDateTime startDate,
                                                                   LocalDateTime endDate) {
        String sql = """
            SELECT 
                p.id,
                i.name,
                i.avatar_url,
                i.repository_url,
                i.linkedin_profile,
                p.presentation_date,
                i.current_rol,
                ag.name as agent_name
            FROM presentations p
            INNER JOIN agent_assignments aa ON p.assignment_id = aa.id
            INNER JOIN investigadores i ON aa.investigador_id = i.id
            INNER JOIN ai_agents ag ON aa.agent_id = ag.id
            WHERE p.presentation_date BETWEEN :startDate AND :endDate
            ORDER BY p.presentation_date ASC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);
            
        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
            CalendarPresentationDto.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .avatarUrl(rs.getString("avatar_url"))
                .repositoryUrl(rs.getString("repository_url"))
                .linkedinUrl(rs.getString("linkedin_profile"))
                .role(rs.getString("current_rol"))
                .presentation(rs.getString("agent_name"))
                .presentationDateTime(rs.getTimestamp("presentation_date").toLocalDateTime())
                .build()
        );
    }

    public Optional<LocalDateTime> findLatestPresentationDate() {
        String sql = "SELECT MAX(presentation_date) FROM presentations";
        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), LocalDateTime.class));
    }

    public Presentation save(Presentation presentation) {
        if (presentation.getId() == null) {
            return insert(presentation);
        }
        return update(presentation);
    }

    private Presentation insert(Presentation presentation) {
        String sql = """
            INSERT INTO presentations (id, assignment_id, video_url, presentation_week, presentation_date,
                                    upload_date, votes_count, is_winner, status)
            VALUES (:id, :assignmentId, :videoUrl, :presentationWeek, :presentationDate,
                   :uploadDate, :votesCount, :isWinner, :status)
        """;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(presentation)
            .addValue("id", id);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        
        presentation.setId(id);
        return presentation;
    }

    private Presentation update(Presentation presentation) {
        String sql = """
            UPDATE presentations 
            SET video_url = :videoUrl,
                presentation_week = :presentationWeek,
                presentation_date = :presentationDate,
                upload_date = :uploadDate,
                votes_count = :votesCount,
                is_winner = :isWinner,
                status = :status
            WHERE id = :id
        """;

        MapSqlParameterSource params = createParameterSource(presentation)
            .addValue("id", presentation.getId());

        jdbcTemplate.update(sql, params);
        return presentation;
    }

    private MapSqlParameterSource createParameterSource(Presentation presentation) {
        return new MapSqlParameterSource()
            .addValue("id", presentation.getId())
            .addValue("assignmentId", presentation.getAssignment().getId())
            .addValue("videoUrl", presentation.getVideoUrl())
            .addValue("presentationWeek", presentation.getPresentationWeek())
            .addValue("presentationDate", presentation.getPresentationDate())
            .addValue("uploadDate", presentation.getUploadDate())
            .addValue("votesCount", presentation.getVotesCount())
            .addValue("isWinner", presentation.getIsWinner())
            .addValue("status", presentation.getStatus());
    }

} 
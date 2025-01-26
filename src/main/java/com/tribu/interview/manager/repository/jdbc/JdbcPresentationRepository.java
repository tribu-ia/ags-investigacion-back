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

    public Optional<Presentation> findById(String id) {
        String sql = SELECT_BASE + " WHERE p.id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        List<Presentation> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<Presentation> findByStatus(PresentationStatusEnum status) {
        String sql = SELECT_BASE + " WHERE p.status = :status";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("status", status.name());

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findByPresentationWeek(int week) {
        String sql = SELECT_BASE + """
            WHERE p.presentation_week = :week
            ORDER BY p.presentation_date ASC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("week", week);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findByPresentationWeekAndStatus(int week, PresentationStatusEnum status) {
        String sql = SELECT_BASE + " WHERE p.presentation_week = :week AND p.status = :status";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("week", week)
            .addValue("status", status.name());

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findUpcomingPresentations(LocalDateTime now) {
        String sql = SELECT_BASE + " WHERE p.presentation_date >= :now ORDER BY p.presentation_date";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("now", now);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findByUploadDateBetweenOrderByVotesCountDesc(LocalDateTime start, LocalDateTime end) {
        String sql = SELECT_BASE + """
            WHERE p.upload_date BETWEEN :start AND :end 
            ORDER BY p.votes_count DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("start", start)
            .addValue("end", end);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findByPresentationDateBetweenOrderByVotesCountDesc(
            LocalDateTime startDate, LocalDateTime endDate) {
        String sql = SELECT_BASE + """
            WHERE p.presentation_date BETWEEN :startDate AND :endDate
            ORDER BY p.votes_count DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findWinnersByPresentationDateBetween(
            LocalDateTime startDate, LocalDateTime endDate) {
        String sql = SELECT_BASE + """
            WHERE p.presentation_date BETWEEN :startDate AND :endDate
            AND p.is_winner = true
            ORDER BY p.presentation_date DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, rowMapper);
    }

    public List<Presentation> findByPresentationDateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = SELECT_BASE + """
            WHERE p.presentation_date BETWEEN :startDate AND :endDate
            ORDER BY p.presentation_date ASC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, rowMapper);
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
            VALUES (:id, :assignmentId,  :videoUrl, :presentationWeek, :presentationDate,
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
            .addValue("presentationDate", presentation.getPresentationDate())
            .addValue("status", presentation.getStatus());
    }

    public long countByPresentationWeek(int week) {
        String sql = "SELECT COUNT(*) FROM presentations WHERE presentation_week = :week";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("week", week);

        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    public Optional<LocalDateTime> findLatestPresentationDate() {
        String sql = """
            SELECT MAX(presentation_date)
            FROM presentations
        """;
        
        LocalDateTime latestDate = jdbcTemplate.queryForObject(
            sql, new MapSqlParameterSource(), LocalDateTime.class);
            
        return Optional.ofNullable(latestDate);
    }

    public long countPresentationsForWeek(LocalDate date) {
        String sql = """
            SELECT COUNT(*) 
            FROM presentations 
            WHERE DATE_TRUNC('week', presentation_date) = DATE_TRUNC('week', :date::date)
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("date", date);
            
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }

    public List<CalendarPresentationDto> findPresentationsForCalendar(LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT 
                p.id,
                i.nombre as researcher_name,
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
            WHERE p.presentation_date BETWEEN :startDate AND :endDate
            ORDER BY p.presentation_date ASC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("startDate", startDate)
            .addValue("endDate", endDate);

        return jdbcTemplate.query(sql, params, new CalendarPresentationMapper());
    }

    public List<CalendarPresentationDto> findUpcomingPresentations() {
        String sql = """
            SELECT p.id, p.presentation_date, p.status,
                   i.name, i.avatar_url, i.repository_url, i.linkedin_profile,
                   aa.agent_id, ag.name as agent_name
            FROM presentations p
            INNER JOIN agent_assignments aa ON p.assignment_id = aa.id
            INNER JOIN investigadores i ON aa.investigador_id = i.id
            INNER JOIN ai_agents ag ON aa.agent_id = ag.id
            WHERE p.presentation_date >= CURRENT_DATE
            ORDER BY p.presentation_date ASC
            """;
            
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            CalendarPresentationDto.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .avatarUrl(rs.getString("avatar_url"))
                .repositoryUrl(rs.getString("repository_url"))
                .linkedinUrl(rs.getString("linkedin_profile"))
                .role(rs.getString("status"))
                .presentation(rs.getString("agent_name"))
                .presentationDateTime(rs.getTimestamp("presentation_date").toLocalDateTime())
                .build()
        );
    }
} 
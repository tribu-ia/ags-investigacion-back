package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.model.PresentationVideo;
import com.tribu.interview.manager.model.PresentationVote;
import com.tribu.interview.manager.service.impl.ResourceNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Transactional
@RequiredArgsConstructor
@Slf4j
public class JdbcPresentationVideoRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PresentationVideo save(PresentationVideo video) {
        if (video.getId() == null) {
            return insert(video);
        }
        return update(video);
    }
    
    private PresentationVideo insert(PresentationVideo video) {
        String sql = """
            INSERT INTO presentation_videos (
                id, assignment_id, title, description, youtube_url, uploaded_at,
                voting_start_date, voting_end_date, votes_count, status
            ) VALUES (
                :id, :assignmentId, :title, :description, :youtubeUrl, :uploadedAt,
                :votingStartDate, :votingEndDate, :votesCount, :status
            )
        """;
        
        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(video)
            .addValue("id", id);
            
        jdbcTemplate.update(sql, params);
        video.setId(id);
        return video;
    }
    
    private MapSqlParameterSource createParameterSource(PresentationVideo video) {
        return new MapSqlParameterSource()
            .addValue("assignmentId", video.getAssignmentId())
            .addValue("title", video.getTitle())
            .addValue("description", video.getDescription())
            .addValue("youtubeUrl", video.getYoutubeUrl())
            .addValue("uploadedAt", video.getUploadedAt())
            .addValue("votingStartDate", video.getVotingStartDate())
            .addValue("votingEndDate", video.getVotingEndDate())
            .addValue("votesCount", video.getVotesCount())
            .addValue("status", video.getStatus());
    }
    
    private PresentationVideo mapToPresentationVideo(ResultSet rs) throws SQLException {
        return PresentationVideo.builder()
            .id(rs.getString("id"))
            .assignmentId(rs.getString("assignment_id"))
            .title(rs.getString("title"))
            .description(rs.getString("description"))
            .youtubeUrl(rs.getString("youtube_url"))
            .uploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime())
            .votingStartDate(getLocalDateTimeOrNull(rs, "voting_start_date"))
            .votingEndDate(getLocalDateTimeOrNull(rs, "voting_end_date"))
            .votesCount(rs.getInt("votes_count"))
            .status(rs.getString("status"))
            .build();
    }

    public List<PresentationVideo> findAllByUploadedAtAfter(LocalDateTime date) {
        String sql = """
            SELECT 
                pv.*,
                aa.id as assignment_id,
                i.id as researcher_id,
                i.name as researcher_name,
                a.id as agent_id,
                a.name as agent_name
            FROM presentation_videos pv
            JOIN agent_assignments aa ON pv.assignment_id = aa.id
            JOIN investigadores i ON aa.investigador_id = i.id
            JOIN ai_agents a ON aa.agent_id = a.id
            WHERE pv.uploaded_at >= :date
            ORDER BY pv.uploaded_at DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("date", date);
            
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapToPresentationVideo(rs));
    }

    public Optional<PresentationVideo> findById(String id) {
        String sql = """
            SELECT 
                pv.*,
                aa.investigador_id,
                aa.agent_id,
                i.name as researcher_name,
                a.name as agent_name
            FROM presentation_videos pv
            JOIN agent_assignments aa ON pv.assignment_id = aa.id
            JOIN investigadores i ON aa.investigador_id = i.id
            JOIN ai_agents a ON aa.agent_id = a.id
            WHERE pv.id = :id
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);
            
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, 
                (rs, rowNum) -> mapToPresentationVideo(rs)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public boolean hasVoted(String videoId, String voterId) {
        String sql = """
            SELECT COUNT(*)
            FROM presentation_votes
            WHERE video_id = :videoId 
            AND voter_id = :voterId
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("videoId", videoId)
            .addValue("voterId", voterId);
            
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public void saveVote(PresentationVote vote) {
        String sql = """
            INSERT INTO presentation_votes (
                id, 
                video_id, 
                voter_id, 
                voted_at
            ) VALUES (
                :id, 
                :videoId, 
                :voterId, 
                :votedAt
            )
        """;
        
        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("videoId", vote.getVideoId())
            .addValue("voterId", vote.getVoterId())
            .addValue("votedAt", vote.getVotedAt());
            
        jdbcTemplate.update(sql, params);
    }

    public List<PresentationVideo> findAllInVotingPeriod(int currentChallengeMonth) {
        String sql = """
            SELECT 
                pv.*,
                aa.id as assignment_id,
                i.id as researcher_id,
                i.name as researcher_name,
                a.id as agent_id,
                a.name as agent_name
            FROM presentation_videos pv
            JOIN agent_assignments aa ON pv.assignment_id = aa.id
            JOIN presentations p ON p.assignment_id = aa.id
            JOIN investigadores i ON aa.investigador_id = i.id
            JOIN ai_agents a ON aa.agent_id = a.id
            WHERE p.show_order = :currentMonth
            ORDER BY pv.uploaded_at DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("currentMonth", currentChallengeMonth);
            
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapToPresentationVideo(rs));
    }

    private PresentationVideo update(PresentationVideo video) {
        String sql = """
            UPDATE presentation_videos 
            SET 
                title = :title,
                description = :description,
                youtube_url = :youtubeUrl,
                voting_start_date = :votingStartDate,
                voting_end_date = :votingEndDate,
                votes_count = :votesCount,
                status = :status
            WHERE id = :id
        """;
        
        MapSqlParameterSource params = createParameterSource(video)
            .addValue("id", video.getId());
        
        int updated = jdbcTemplate.update(sql, params);
        if (updated == 0) {
            throw new ResourceNotFoundException("Video not found with id: " + video.getId());
        }
        
        return video;
    }

    private LocalDateTime getLocalDateTimeOrNull(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    public boolean existsByAssignmentId(String assignmentId) {
        String sql = """
            SELECT COUNT(*)
            FROM presentation_videos
            WHERE assignment_id = :assignmentId
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("assignmentId", assignmentId);
            
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public List<PresentationVideo> findAllByMonth(int month) {
        String sql = """
            SELECT 
                pv.*,
                aa.id as assignment_id,
                i.id as researcher_id,
                i.name as researcher_name,
                a.id as agent_id,
                a.name as agent_name
            FROM presentation_videos pv
            JOIN agent_assignments aa ON pv.assignment_id = aa.id
            JOIN investigadores i ON aa.investigador_id = i.id
            JOIN ai_agents a ON aa.agent_id = a.id
            WHERE EXTRACT(MONTH FROM aa.assigned_at) = :month
            ORDER BY pv.uploaded_at DESC
        """;
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("month", month);
            
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapToPresentationVideo(rs));
    }

} 
package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.dto.ChallengeStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class JdbcConfigParamsRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    public ChallengeStatusResponse getChallengeStatus() {
        String sql = """
            SELECT 
                current_month_for_challenge,
                is_week_of_upload,
                is_week_of_voting
            FROM config_params 
            WHERE id = '1'
        """;
        
        try {
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), 
                (rs, rowNum) -> ChallengeStatusResponse.builder()
                    .currentMonth(rs.getInt("current_month_for_challenge"))
                    .isWeekOfUpload(rs.getBoolean("is_week_of_upload"))
                    .isWeekOfVoting(rs.getBoolean("is_week_of_voting"))
                    .build()
            );
        } catch (EmptyResultDataAccessException e) {
            log.error("No se encontró configuración del challenge");
            throw new IllegalStateException("No challenge configuration found");
        }
    }
} 
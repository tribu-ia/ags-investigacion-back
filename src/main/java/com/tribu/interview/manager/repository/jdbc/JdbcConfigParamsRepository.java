package com.tribu.interview.manager.repository.jdbc;

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
    
    public Integer getCurrentMonthForChallenge() {
        String sql = """
            SELECT current_month_for_challenge 
            FROM config_params 
            WHERE id = '1'
        """;
        
        try {
            return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Integer.class);
        } catch (EmptyResultDataAccessException e) {
            log.error("No se encontró configuración del mes actual");
            throw new IllegalStateException("No current month configuration found");
        }
    }
} 
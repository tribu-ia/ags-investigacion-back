package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.repository.mapper.ResearcherRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcResearcherRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ResearcherRowMapper rowMapper = new ResearcherRowMapper();

    private static final String SELECT_BASE = """
        SELECT id, name, email, phone, github_username, avatar_url, 
               repository_url, linkedin_profile, current_rol
        FROM investigadores
    """;

    private static final String INSERT_SQL = """
        INSERT INTO investigadores (
            id, name, email, phone, github_username, 
            avatar_url, repository_url, linkedin_profile, current_rol
        ) VALUES (
            :id, :name, :email, :phone, :githubUsername,
            :avatarUrl, :repositoryUrl, :linkedinProfile, :currentRol
        )
    """;

    private static final String UPDATE_SQL = """
        UPDATE investigadores 
        SET name = :name,
            email = :email,
            phone = :phone,
            github_username = :githubUsername,
            avatar_url = :avatarUrl,
            repository_url = :repositoryUrl,
            linkedin_profile = :linkedinProfile,
            current_rol = :currentRol
        WHERE id = :id
    """;

    public Optional<Researcher> findById(String id) {
        String sql = SELECT_BASE + " WHERE id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        List<Researcher> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<Researcher> findByEmail(String email) {
        String sql = """
            SELECT 
                id,
                name,
                email,
                phone,
                github_username,
                avatar_url,
                repository_url,
                linkedin_profile,
                current_rol,
                created_at
            FROM investigadores
            WHERE email = :email
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("email", email);

        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                Researcher.builder()
                    .id(rs.getString("id"))
                    .name(rs.getString("name"))
                    .email(rs.getString("email"))
                    .phone(rs.getString("phone"))
                    .githubUsername(rs.getString("github_username"))
                    .avatarUrl(rs.getString("avatar_url"))
                    .repositoryUrl(rs.getString("repository_url"))
                    .linkedinProfile(rs.getString("linkedin_profile"))
                    .currentRol(rs.getString("current_rol"))
                    .createdAt(LocalDateTime.parse(rs.getString("created_at").replace(" ", "T")))
                    .build()
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<Researcher> findAll() {
        return jdbcTemplate.query(SELECT_BASE, rowMapper);
    }

    public Researcher save(Researcher researcher) {
        if (researcher.getId() == null) {
            return insert(researcher);
        }
        return update(researcher);
    }

    private Researcher insert(Researcher researcher) {
        String sql = INSERT_SQL;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(researcher)
            .addValue("id", id);

        jdbcTemplate.update(sql, params);
        researcher.setId(id);
        return researcher;
    }

    private Researcher update(Researcher researcher) {
        String sql = UPDATE_SQL;

        MapSqlParameterSource params = createParameterSource(researcher)
            .addValue("id", researcher.getId());

        jdbcTemplate.update(sql, params);
        return researcher;
    }

    private MapSqlParameterSource createParameterSource(Researcher researcher) {
        return new MapSqlParameterSource()
            .addValue("name", researcher.getName())
            .addValue("email", researcher.getEmail())
            .addValue("phone", researcher.getPhone())
            .addValue("githubUsername", researcher.getGithubUsername())
            .addValue("avatarUrl", researcher.getAvatarUrl())
            .addValue("repositoryUrl", researcher.getRepositoryUrl())
            .addValue("linkedinProfile", researcher.getLinkedinProfile())
                .addValue("currentRol", researcher.getCurrentRol())
                .addValue("phone", researcher.getPhone());

    }

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM investigadores WHERE email = :email";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("email", email);

        return jdbcTemplate.queryForObject(sql, params, Integer.class) > 0;
    }
} 
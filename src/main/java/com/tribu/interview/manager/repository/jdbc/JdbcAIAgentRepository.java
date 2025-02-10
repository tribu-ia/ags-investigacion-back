package com.tribu.interview.manager.repository.jdbc;

import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.repository.mapper.AIAgentRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class JdbcAIAgentRepository {
    
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AIAgentRowMapper rowMapper = new AIAgentRowMapper();

    private static final String SELECT_BASE = """
        SELECT a.id, a.name, a.created_by, a.website, a.access, a.pricing_model,
               a.category, a.industry, a.short_description, a.long_description,
               a.key_features, a.use_cases, a.tags, a.logo, a.logo_file_name,
               a.image, a.image_file_name, a.video, a.upvotes, a.approved,
               a.created_at, a.slug, a.version, a.featured,
               aa.status as assignment_status,
               aa.assigned_at,
               r.name as assigned_to_name,
               r.email as assigned_to_email,
               r.role as assigned_role,
               (SELECT COUNT(*) FROM agent_assignments aa2 
                WHERE aa2.agent_id = a.id 
                AND aa2.status = 'active') as total_contributors
        FROM ai_agents a
        LEFT JOIN agent_assignments aa ON a.id = aa.agent_id 
            AND aa.status = 'active' 
            AND EXISTS (
                SELECT 1 FROM investigadores i 
                WHERE i.id = aa.investigador_id 
                AND i.role = 'PRIMARY'
            )
        LEFT JOIN investigadores r ON aa.investigador_id = r.id
    """;

    public Optional<AIAgent> findById(String id) {
        String sql = SELECT_BASE + " WHERE a.id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id);

        List<AIAgent> results = jdbcTemplate.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<AIAgent> saveAll(List<AIAgent> agents) {
        List<AIAgent> savedAgents = new ArrayList<>();
        for (AIAgent agent : agents) {
            savedAgents.add(save(agent));
        }
        return savedAgents;
    }

    public List<String> findDistinctCategories() {
        String sql = "SELECT DISTINCT category FROM ai_agents WHERE category IS NOT NULL ORDER BY category";
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
    }

    public List<String> findDistinctIndustries() {
        String sql = "SELECT DISTINCT industry FROM ai_agents WHERE industry IS NOT NULL ORDER BY industry";
        return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
    }

    public List<AIAgent> findAllWithFilters(String category, String industry, String search) {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();

        if (StringUtils.hasText(category)) {
            conditions.add("a.category = :category");
            params.addValue("category", category);
        }

        if (StringUtils.hasText(industry)) {
            conditions.add("a.industry = :industry");
            params.addValue("industry", industry);
        }

        if (StringUtils.hasText(search)) {
            conditions.add("(LOWER(a.name) LIKE LOWER(:search) OR LOWER(a.short_description) LIKE LOWER(:search))");
            params.addValue("search", "%" + search + "%");
        }

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        sql.append(" ORDER BY a.created_at DESC");
        return jdbcTemplate.query(sql.toString(), params, rowMapper);
    }

    public long count() {
        String sql = "SELECT COUNT(*) FROM ai_agents";
        return jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
    }

    public AIAgent save(AIAgent agent) {
        if (agent.getId() == null) {
            return insert(agent);
        }
        return update(agent);
    }

    private AIAgent insert(AIAgent agent) {
        String sql = """
            INSERT INTO ai_agents (id, name, created_by, website, access, pricing_model,
                                 category, industry, short_description, long_description,
                                 key_features, use_cases, tags, logo, logo_file_name,
                                 image, image_file_name, video, upvotes, approved,
                                 created_at, slug, version, featured)
            VALUES (:id, :name, :createdBy, :website, :access, :pricingModel,
                   :category, :industry, :shortDescription, :longDescription,
                   :keyFeatures, :useCases, :tags, :logo, :logoFileName,
                   :image, :imageFileName, :video, :upvotes, :approved,
                   :createdAt, :slug, :version, :featured)
        """;

        String id = UUID.randomUUID().toString();
        MapSqlParameterSource params = createParameterSource(agent)
            .addValue("id", id);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder);
        
        agent.setId(id);
        return agent;
    }

    private AIAgent update(AIAgent agent) {
        String sql = """
            UPDATE ai_agents 
            SET name = :name,
                created_by = :createdBy,
                website = :website,
                access = :access,
                pricing_model = :pricingModel,
                category = :category,
                industry = :industry,
                short_description = :shortDescription,
                long_description = :longDescription,
                key_features = :keyFeatures,
                use_cases = :useCases,
                tags = :tags,
                logo = :logo,
                logo_file_name = :logoFileName,
                image = :image,
                image_file_name = :imageFileName,
                video = :video,
                upvotes = :upvotes,
                approved = :approved,
                created_at = :createdAt,
                slug = :slug,
                version = :version,
                featured = :featured
            WHERE id = :id
        """;

        MapSqlParameterSource params = createParameterSource(agent)
            .addValue("id", agent.getId());

        jdbcTemplate.update(sql, params);
        return agent;
    }

    private MapSqlParameterSource createParameterSource(AIAgent agent) {
        return new MapSqlParameterSource()
            .addValue("name", agent.getName())
            .addValue("createdBy", agent.getCreatedBy())
            .addValue("website", agent.getWebsite())
            .addValue("access", agent.getAccess())
            .addValue("pricingModel", agent.getPricingModel())
            .addValue("category", agent.getCategory())
            .addValue("industry", agent.getIndustry())
            .addValue("shortDescription", agent.getShortDescription())
            .addValue("longDescription", agent.getLongDescription())
            .addValue("keyFeatures", agent.getKeyFeatures())
            .addValue("useCases", agent.getUseCases())
            .addValue("tags", agent.getTags())
            .addValue("logo", agent.getLogo())
            .addValue("logoFileName", agent.getLogoFileName())
            .addValue("image", agent.getImage())
            .addValue("imageFileName", agent.getImageFileName())
            .addValue("video", agent.getVideo())
            .addValue("upvotes", agent.getUpvotes())
            .addValue("approved", agent.getApproved())
            .addValue("createdAt", agent.getCreatedAt())
            .addValue("slug", agent.getSlug())
            .addValue("version", agent.getVersion())
            .addValue("featured", agent.getFeatured());
    }
} 
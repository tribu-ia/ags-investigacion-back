package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.AIAgent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIAgentRepository extends JpaRepository<AIAgent, String>, JpaSpecificationExecutor<AIAgent> {
    @Query("SELECT DISTINCT a.category FROM AIAgent a WHERE a.category IS NOT NULL AND a.category != ''")
    List<String> findDistinctCategories();

    @Query("SELECT DISTINCT a.industry FROM AIAgent a WHERE a.industry IS NOT NULL AND a.industry != ''")
    List<String> findDistinctIndustries();

    @Query("SELECT a FROM AIAgent a WHERE " +
           "(:category IS NULL OR a.category = :category) AND " +
           "(:industry IS NULL OR a.industry = :industry) AND " +
           "(:search IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.shortDescription) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AIAgent> findWithFilters(
        @Param("category") String category,
        @Param("industry") String industry,
        @Param("search") String search,
        Pageable pageable
    );
} 
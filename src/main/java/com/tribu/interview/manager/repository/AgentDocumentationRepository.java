package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.AgentDocumentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AgentDocumentationRepository extends JpaRepository<AgentDocumentation, String> {
    @Query("SELECT COUNT(ad) FROM AgentDocumentation ad")
    long countDocumentedAgents();
} 
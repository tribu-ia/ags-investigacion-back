package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.AIAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentAssignmentRepository extends JpaRepository<AgentAssignment, String> {

    Optional<AgentAssignment> findActiveAssignmentByAgentId(String agentId);
    long countByStatus(String status);
    
    @Query("SELECT a FROM AgentAssignment a WHERE a.agent.id = :agentId AND a.status = 'active'")
    Optional<AgentAssignment> findByAgentIdAndStatusActive(@Param("agentId") String agentId);
} 
package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResearcherRepository extends JpaRepository<Researcher, String> {
    boolean existsByEmail(String email);
    Optional<Researcher> findByEmail(String email);
} 
package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.Researcher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvestigadorRepository extends JpaRepository<Researcher, String> {
    Optional<Researcher> findByEmail(String email);
    boolean existsByEmail(String email);
} 
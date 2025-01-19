package com.tribu.interview.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "agent_documentation")
public class AgentDocumentation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "assignment_id")
    private AgentAssignment assignment;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "research_summary", columnDefinition = "TEXT")
    private String researchSummary;

    @Column(name = "documentation_date")
    private LocalDateTime documentationDate;

    private String status;

    @PrePersist
    protected void onCreate() {
        documentationDate = LocalDateTime.now();
        if (status == null) {
            status = "completed";
        }
    }
} 
package com.tribu.interview.manager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "agent_assignments",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"agent_id"}, name = "unique_active_assignment"),
           @UniqueConstraint(columnNames = {"agent_id", "investigador_id"}, name = "unique_assignment_pair")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "investigador_id", nullable = false)
    private Researcher researcher;

    @ManyToOne
    @JoinColumn(name = "agent_id", nullable = false)
    private AIAgent agent;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    private String status;

    @OneToOne(mappedBy = "assignment")
    private Presentation presentation;

    @PrePersist
    protected void onCreate() {
        assignedAt = LocalDateTime.now();
        if (status == null) {
            status = "active";
        }
    }
} 
package com.tribu.interview.manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAssignment {
    private String id;
    private Researcher researcher;
    private AIAgent agent;
    private String status;
    private LocalDateTime assignedAt;
} 
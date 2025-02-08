package com.tribu.interview.manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDocumentation {
    private String id;
    private String assignmentId;
    private String markdownContent;
    private LocalDateTime documentationDate;
    private String status; // DRAFT, COMPLETED
}
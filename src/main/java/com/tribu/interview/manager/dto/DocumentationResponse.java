package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationResponse {
    private String id;
    private String assignmentId;
    private String investigadorId;
    private LocalDateTime documentationDate;
    private String status;
    private String markdownContent;
    private String recommendations;
    private String researchSummary;
    private Map<String, Object> additionalData;
    private Boolean isEditable;
}


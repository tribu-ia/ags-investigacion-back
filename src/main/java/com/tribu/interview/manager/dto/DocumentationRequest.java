package com.tribu.interview.manager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationRequest {
    @NotNull
    private String assignmentId;
    private String markdownContent;
    private String recommendations;
    private String researchSummary;
    private Map<String, Object> additionalData;
} 
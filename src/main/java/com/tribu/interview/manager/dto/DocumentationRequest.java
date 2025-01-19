package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentationRequest {
    private String investigadorId;
    private String findings;
    private String recommendations;
    private String researchSummary;
} 
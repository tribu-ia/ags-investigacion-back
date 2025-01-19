package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentationData {
    private String agentId;
    private String researcherId;
    private String status;
    private String findings;
    private String recommendations;
    private String researchSummary;
}
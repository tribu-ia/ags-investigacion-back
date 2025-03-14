package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResearcherResponseDto {
    private String id;
    private String name;
    private String category;
    private String industry;
    private String shortDescription;
    private String longDescription;

    private String role;
    private String assignmentId;
    private String status;
}
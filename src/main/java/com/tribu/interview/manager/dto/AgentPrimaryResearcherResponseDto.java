package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPrimaryResearcherResponseDto {
    private String assignmentId;
    // Presentation info
    private String presentationDate;
    private String presentationTime;
    private String status;
    private String presentationWeek;

    // Agent info
    private String agentName;
    private String agentDescription;
    private String agentCategory;
    private String agentIndustry;

}
package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResearcherDetailDto {
    private String name;
    private String email;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinProfile;
    private String role;
    private String githubUsername;
    
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

    // Assignment
    private String assignmentId;

    // PresentationVideo
    private Integer showOrder;

} 
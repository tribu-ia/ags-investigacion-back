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
    private String currentRole;
    private String githubUsername;
    
    // Presentation info
    private String presentationDate;
    private String presentationTime;
    private String agentName;
    private String status;
    private String presentationWeek;
} 
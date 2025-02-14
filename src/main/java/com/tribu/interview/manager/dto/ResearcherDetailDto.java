package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResearcherDetailDto {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinProfile;
    private String currentRole;
    private String githubUsername;
    private List<AgentPrimaryResearcherResponseDto> primaryResearches;
    private List<AgentResearcherResponseDto> contributorsResearches;
    // PresentationVideo
    private Integer showOrder;

} 
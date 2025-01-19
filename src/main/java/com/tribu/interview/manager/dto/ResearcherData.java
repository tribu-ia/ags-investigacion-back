package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResearcherData {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String githubUsername;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinProfile;
    private String agentId;
    private String status;
}
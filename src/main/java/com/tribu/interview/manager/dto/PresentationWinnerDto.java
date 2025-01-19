package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresentationWinnerDto {
    private String id;
    private String investigadorName;
    private String avatarUrl;
    private String agentName;
    private String presentationDate;
    private Integer votes;
    private String role;
    private String repositoryUrl;
    private String linkedinProfile;
}
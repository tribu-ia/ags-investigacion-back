package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CalendarPresentationDto {
    private String id;
    private String name;            // researcher_name
    private String avatarUrl;       // researcher_avatar_url
    private String repositoryUrl;   // researcher_repository_url
    private String linkedinUrl;     // researcher_linkedin_url
    private String role;            // status
    private String presentation;    // agent_name
    private LocalDateTime presentationDateTime;
} 
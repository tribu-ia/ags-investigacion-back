package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UpcomingPresentationResponse {
    private String id;
    private String investigadorName;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinProfile;
    private String role;
    private String agentName;
    private LocalDateTime presentationDate;
    private Integer weeklyVotes;
    private Boolean isWeeklyWinner;
    private Integer monthlyVotes;
    private Boolean isMonthlyWinner;
} 
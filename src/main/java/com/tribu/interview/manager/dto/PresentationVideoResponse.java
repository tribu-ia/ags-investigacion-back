package com.tribu.interview.manager.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationVideoResponse {
    private String id;
    private String assignmentId;
    private String title;
    private String description;
    private String youtubeUrl;
    private LocalDateTime uploadedAt;
    private String status;
    private VotingPeriod votingPeriod;
    private Integer votesCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VotingPeriod {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private boolean isVotingOpen;
    }
} 
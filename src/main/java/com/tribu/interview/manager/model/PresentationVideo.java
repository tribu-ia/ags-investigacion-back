package com.tribu.interview.manager.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationVideo {
    private String id;
    private String assignmentId;
    private String title;
    private String description;
    private String youtubeUrl;
    private LocalDateTime uploadedAt;
    private LocalDateTime votingStartDate;
    private LocalDateTime votingEndDate;
    private Integer votesCount;
    private String status;
    private Integer showOrder;
} 
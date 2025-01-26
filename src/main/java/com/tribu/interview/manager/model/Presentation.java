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
public class Presentation {
    private String id;
    private AgentAssignment assignment;
    private String videoUrl;
    private Integer presentationWeek;
    private LocalDateTime presentationDate;
    private LocalDateTime uploadDate;
    private Integer votesCount;
    private Boolean isWinner;
    private String status;
}


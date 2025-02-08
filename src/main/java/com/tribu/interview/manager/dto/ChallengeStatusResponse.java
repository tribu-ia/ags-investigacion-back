package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeStatusResponse {
    private Integer currentMonth;
    private Boolean isWeekOfUpload;
    private Boolean isWeekOfVoting;
} 
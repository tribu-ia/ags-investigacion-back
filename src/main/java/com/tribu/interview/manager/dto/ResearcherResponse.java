package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResearcherResponse {
    private boolean success;
    private String message;
    private ResearcherData data;
    private String errorType;
    private String errorCode;
    private LocalDateTime presentationDateTime;

}

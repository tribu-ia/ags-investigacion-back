package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WeekPresentationsResponse {
    private String weekStart;
    private String weekEnd;
    private List<PresentationViewDto> presentations;
} 
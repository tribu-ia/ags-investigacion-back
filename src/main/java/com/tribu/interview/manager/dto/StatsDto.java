package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatsDto {
    private Long total_agents;
    private Long documented_agents;
    private Long active_investigators;
}
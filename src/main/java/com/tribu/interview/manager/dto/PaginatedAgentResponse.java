package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedAgentResponse {
    private List<AgentWithAssignmentDto> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
} 
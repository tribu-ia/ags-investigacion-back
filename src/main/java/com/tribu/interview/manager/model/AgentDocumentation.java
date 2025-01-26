package com.tribu.interview.manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDocumentation {
    private String id;
    private String agentId;
    private String title;
    private String content;
    private String url;
    private String type;
} 
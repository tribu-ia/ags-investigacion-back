package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentationRequest {
    private String title;
    private String content;
    private String url;
    private String type;
} 
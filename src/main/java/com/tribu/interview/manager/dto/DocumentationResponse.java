package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentationResponse {
    private boolean success;
    private String message;
    private DocumentationData data;
}


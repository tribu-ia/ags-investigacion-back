package com.tribu.interview.manager.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveMarkdownRequest {
    @NotNull
    private String assignmentId;
    @NotNull
    private String markdownContent;
} 
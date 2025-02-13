package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitDocumentationDto {
    private String githubPullRequest;
}
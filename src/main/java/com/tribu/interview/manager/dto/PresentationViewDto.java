package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresentationViewDto {
    private String id;
    private String name;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinUrl;
    private String role;
    private String presentation;
    private String date;
    private String time;
} 
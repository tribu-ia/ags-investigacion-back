package com.tribu.interview.manager.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherUpdateDto {
    private String currentRole;
    private String githubUsername;
    private String linkedinProfile;
    private String phone;
} 
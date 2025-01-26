package com.tribu.interview.manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Researcher {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String githubUsername;
    private String avatarUrl;
    private String repositoryUrl;
    private String linkedinProfile;
    private LocalDateTime createdAt;
} 
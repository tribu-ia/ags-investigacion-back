package com.tribu.interview.manager.model;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
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
    private String role;
} 
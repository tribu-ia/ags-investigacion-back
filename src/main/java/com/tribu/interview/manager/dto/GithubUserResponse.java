package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GithubUserResponse {
    private String avatarUrl;
    private String htmlUrl;
    private String name;
    private String login;
    private String bio;
    private String location;
    private String company;
} 
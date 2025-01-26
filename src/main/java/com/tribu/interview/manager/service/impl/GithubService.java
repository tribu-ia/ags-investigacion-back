package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.GithubUserResponse;
import com.tribu.interview.manager.service.IGithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService implements IGithubService {
    private final RestTemplate restTemplate;
    
    @Value("${github.api.token}")
    private String githubToken;

    @Override
    public Optional<GithubUserResponse> fetchUserData(String username) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "token " + githubToken);
            headers.set("Accept", "application/vnd.github+json");
            headers.set("X-GitHub-Api-Version", "2022-11-28");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            var response = restTemplate.exchange(
                "https://api.github.com/users/" + username,
                HttpMethod.GET,
                entity,
                GithubUserResponse.class
            );
            
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.error("Error fetching GitHub data for user {}: {}", username, e.getMessage());
            return Optional.empty();
        }
    }
} 
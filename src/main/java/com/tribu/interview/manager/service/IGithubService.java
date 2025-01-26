package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.GithubUserResponse;
import java.util.Optional;

public interface IGithubService {
    Optional<GithubUserResponse> fetchUserData(String username);
} 
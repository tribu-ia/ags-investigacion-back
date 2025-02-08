package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.GithubUserResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface IGithubService {
    Optional<GithubUserResponse> fetchUserData(String username);

    String uploadDocumentation(
            String folderPath,
            String documentName,
            String markdownContent,
            List<MultipartFile> documents);
}
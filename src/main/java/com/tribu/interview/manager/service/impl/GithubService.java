package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.GithubUserResponse;
import com.tribu.interview.manager.service.IGithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;
import java.util.HashMap;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubService implements IGithubService {
    private final RestTemplate restTemplate;
    
    @Value("${github.api.token}")
    private String githubToken;
    
    @Value("${github.repository.owner}")
    private String repositoryOwner;
    
    @Value("${github.repository.name}")
    private String repositoryName;

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

    public String uploadDocumentation(
            String folderPath, 
            String documentName,
            String markdownContent,
            String researcherName,
            List<MultipartFile> documents) {
        try {
            // Formatear el nombre del investigador y crear la ruta completa
            String formattedResearcherName = sanitizeFileName(researcherName);
            String completeFolderPath = folderPath + "/" + formattedResearcherName;

            // 1. Crear el archivo markdown principal
            String markdownPath = completeFolderPath + "/" + documentName;
            String encodedMarkdown = Base64.getEncoder().encodeToString(markdownContent.getBytes(StandardCharsets.UTF_8));
            createOrUpdateFile(markdownPath, "Add documentation for " + completeFolderPath, encodedMarkdown);
            
            // 2. Subir cada documento adjunto
            for (MultipartFile document : documents) {
                String filePath = completeFolderPath + "/" + sanitizeFileName(document.getOriginalFilename());
                byte[] content = document.getBytes();
                String base64Content = Base64.getEncoder().encodeToString(content);
                
                createOrUpdateFile(filePath, 
                    "Add supporting document: " + document.getOriginalFilename(), 
                    base64Content);
            }
            
            return String.format("https://github.com/%s/%s/tree/main/%s", 
                repositoryOwner, repositoryName, completeFolderPath);
                
        } catch (Exception e) {
            log.error("Error uploading to GitHub: {}", e.getMessage());
            throw new RuntimeException("Failed to upload documentation to GitHub", e);
        }
    }
    
    private void createOrUpdateFile(String path, String commitMessage, String base64Content) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s",
            repositoryOwner, repositoryName, path);
            
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + githubToken);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        
        Map<String, String> body = new HashMap<>();
        body.put("message", commitMessage);
        body.put("content", base64Content);
        body.put("branch", "main");
        
        try {
            // Verificar si el archivo existe
            ResponseEntity<Map> existingFile = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            
            if (existingFile.getBody() != null) {
                body.put("sha", (String) existingFile.getBody().get("sha"));
            }
        } catch (HttpClientErrorException.NotFound ignored) {
            // El archivo no existe, no necesitamos SHA
        }
        
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);
        } catch (HttpClientErrorException e) {
            log.error("GitHub API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
    
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "document.txt";
        return fileName.trim()
            .toLowerCase()
            .replaceAll("[^a-z0-9.-]", "-")
            .replaceAll("-+", "-");
    }
} 
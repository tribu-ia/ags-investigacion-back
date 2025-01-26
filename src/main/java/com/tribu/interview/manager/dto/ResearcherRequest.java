package com.tribu.interview.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearcherRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "GitHub username is required")
    @JsonProperty("github_username")
    private String githubUsername;

    @NotBlank(message = "LinkedIn profile is required")
    @JsonProperty("linkedin_profile")
    private String linkedinProfile;

    @NotBlank(message = "Agent ID is required")
    @JsonProperty("agent")
    private String agentId;
} 
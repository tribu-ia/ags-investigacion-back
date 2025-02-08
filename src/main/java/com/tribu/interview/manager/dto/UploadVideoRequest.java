package com.tribu.interview.manager.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadVideoRequest {
    @NotNull
    private String assignmentId;
    
    @NotNull
    @Size(min = 3, max = 100)
    private String title;
    
    @Size(max = 250)
    private String description;
    
    @NotNull
    @Pattern(regexp = "^(https?://)?(www\\.)?youtube\\.com/.*$", 
            message = "Must be a valid YouTube URL")
    private String youtubeUrl;
} 
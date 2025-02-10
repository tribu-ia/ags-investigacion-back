package com.tribu.interview.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentWithAssignmentDto {
    private String id;
    private String name;
    private String createdBy;
    private String website;
    private String access;
    private String pricingModel;
    private String category;
    private String industry;
    private String shortDescription;
    private String longDescription;
    private String keyFeatures;
    private String useCases;
    private String tags;
    private String logo;
    private String logoFileName;
    private String image;
    private String imageFileName;
    private String video;
    private Integer upvotes;
    private Boolean approved;
    private LocalDateTime createdAt;
    private String slug;
    private String version;
    private Boolean featured;
    private Boolean hasPrimaryResearcher;
    private AssignmentInfoDto assignmentInfo;
    private Integer totalContributors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentInfoDto {
        private String assignedTo;
        private String assignedEmail;
        private LocalDateTime assignedAt;
        private String assignedRole;
    }
} 
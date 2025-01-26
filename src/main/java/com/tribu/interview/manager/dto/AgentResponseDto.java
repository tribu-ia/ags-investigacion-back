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
public class AgentResponseDto {
    private String id;
    private String name;
    private String category;
    private String industry;
    private String shortDescription;
    private String longDescription;
    private String keyFeatures;
    private String useCases;
    private String tags;
    private String logo;
    private String image;
    private String video;
    private String website;
    private String access;
    private String pricingModel;
    private String createdBy;
    private LocalDateTime createdAt;
    private Integer upvotes;
    private Boolean approved;
    private Boolean featured;
}
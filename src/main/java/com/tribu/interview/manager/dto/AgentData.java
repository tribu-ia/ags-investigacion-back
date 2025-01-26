package com.tribu.interview.manager.dto;

import lombok.Data;
import java.util.List;

@Data
public class AgentData {
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
    private List<String> upvoters;
    private Boolean approved;
    private String createdAt;
    private String slug;
    private String version;
    private Boolean featured;
} 
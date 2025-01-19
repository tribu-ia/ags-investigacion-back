package com.tribu.interview.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_agents")
public class AIAgent {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_by")
    private String createdBy;

    private String website;
    private String access;

    @Column(name = "pricing_model")
    private String pricingModel;

    private String category;
    private String industry;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "long_description", columnDefinition = "TEXT")
    private String longDescription;

    @Column(name = "key_features", columnDefinition = "TEXT")
    private String keyFeatures;

    @Column(name = "use_cases", columnDefinition = "TEXT")
    private String useCases;

    @Column(columnDefinition = "TEXT")
    private String tags;

    private String logo;

    @Column(name = "logo_file_name")
    private String logoFileName;

    private String image;

    @Column(name = "image_file_name")
    private String imageFileName;

    private String video;
    private Integer upvotes;

    @Column(columnDefinition = "jsonb")
    private String upvoters;

    private Boolean approved;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String slug;
    private String version;
    private Boolean featured;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
} 
package com.tribu.interview.manager.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PresentationVote {
    private String id;
    private String videoId;  // Cambiado de presentationVideoId a videoId para consistencia
    private String voterId; // ID del investigador que vota
    private LocalDateTime votedAt;
} 
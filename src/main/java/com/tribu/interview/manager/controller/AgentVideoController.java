package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.PresentationVideoResponse;
import com.tribu.interview.manager.dto.UploadVideoRequest;
import com.tribu.interview.manager.dto.ChallengeStatusResponse;
import com.tribu.interview.manager.service.impl.PresentationVideoService;
import com.tribu.interview.manager.service.impl.ConfigParamsService;
import com.tribu.interview.manager.repository.jdbc.JdbcConfigParamsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agent-videos")
public class AgentVideoController {
    
    private final PresentationVideoService videoService;
    private final ConfigParamsService configParamsService;

    @PostMapping("/upload")
    public ResponseEntity<PresentationVideoResponse> uploadVideo(
            @Valid @RequestBody UploadVideoRequest request) {
        return ResponseEntity.ok(videoService.uploadVideo(request));
    }

    @PostMapping("/{videoId}/vote")
    public ResponseEntity<PresentationVideoResponse> voteForVideo(
            @PathVariable String videoId,
            @RequestParam String voterId) {
        return ResponseEntity.ok(videoService.registerVote(videoId, voterId));
    }

    @GetMapping("/voting-period")
    public ResponseEntity<List<PresentationVideoResponse>> getVideosInVotingPeriod() {
        return ResponseEntity.ok(videoService.getVideosInVotingPeriod());
    }

    @GetMapping("/challenge-status")
    public ResponseEntity<ChallengeStatusResponse> getChallengeStatus() {
        return ResponseEntity.ok(configParamsService.getChallengeStatus());
    }
} 
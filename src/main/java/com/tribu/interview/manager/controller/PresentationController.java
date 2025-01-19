package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.UpcomingPresentationResponse;
import com.tribu.interview.manager.dto.WinnersResponse;
import com.tribu.interview.manager.service.PresentationService;
import com.tribu.interview.manager.service.VotingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presentations")
@RequiredArgsConstructor
public class PresentationController {
    
    private final VotingService votingService;
    private final PresentationService presentationService;
    
    @GetMapping("/upcoming")
    public ResponseEntity<List<UpcomingPresentationResponse>> getUpcomingPresentations() {
        return ResponseEntity.ok(presentationService.getUpcomingPresentations());
    }
    
    @PostMapping("/{presentationId}/vote")
    public ResponseEntity<Void> voteForPresentation(
            @PathVariable String presentationId,
            @RequestHeader("X-User-Email") String userEmail) {
        votingService.processVote(presentationId,"", userEmail);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/winners")
    public ResponseEntity<WinnersResponse> getWinners(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(presentationService.getWinners(month, year));
    }
} 
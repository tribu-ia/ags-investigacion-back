package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.service.impl.ResearcherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/researchers")
public class ResearcherController {
    private final ResearcherService researcherService;

    @PostMapping
    public ResponseEntity<ResearcherResponse> createResearcher(
            @Valid @RequestBody ResearcherRequest request) {
        return ResponseEntity.ok(researcherService.createResearcher(request));
    }

    @GetMapping("/details")
    public ResponseEntity<ResearcherDetailDto> getResearcherDetails(@RequestParam String email) {
        return ResponseEntity.ok(researcherService.getResearcherDetailsByEmail(email));
    }

    @PutMapping("/{email}/profile")
    public ResponseEntity<ResearcherDetailDto> updateResearcherProfile(
            @PathVariable String email,
            @RequestBody ResearcherUpdateDto updateDto) {
        return ResponseEntity.ok(researcherService.updateResearcherProfile(email, updateDto));
    }
} 
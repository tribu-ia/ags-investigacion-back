package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.ResearcherRequest;
import com.tribu.interview.manager.dto.ResearcherResponse;
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

} 
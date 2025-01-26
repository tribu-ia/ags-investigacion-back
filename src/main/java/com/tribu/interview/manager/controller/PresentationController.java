package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.WeekPresentationsResponse;
import com.tribu.interview.manager.service.IPresentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/presentations")
@RequiredArgsConstructor
public class PresentationController {
    private final IPresentationService presentationService;

    @GetMapping("/current-week")
    public ResponseEntity<WeekPresentationsResponse> getCurrentWeekPresentations() {
        return ResponseEntity.ok(presentationService.getCurrentWeekPresentations());
    }
} 
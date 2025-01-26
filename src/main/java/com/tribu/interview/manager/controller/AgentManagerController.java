package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.service.impl.AgentManagerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agents")
public class AgentManagerController {
    private final AgentManagerService agentManagerService;

    @PostMapping("/upload")
    public ResponseEntity<List<AIAgent>> uploadAgents(@Valid @RequestBody AgentUploadRequest payload) {
        return ResponseEntity.ok(agentManagerService.processJsonData(payload));
    }

    @GetMapping("/metadata")
    public ResponseEntity<MetadataResponse> getAgentsMetadata() {
        return ResponseEntity.ok(agentManagerService.getMetadata());
    }

    @GetMapping
    public ResponseEntity<PaginatedAgentResponse> getAgents(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(agentManagerService.getAgents(page, pageSize, category, industry, search));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsDto> getStats() {
        return ResponseEntity.ok(agentManagerService.getStats());
    }

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(HealthResponse.builder()
            .status("OK")
            .build());
    }
} 
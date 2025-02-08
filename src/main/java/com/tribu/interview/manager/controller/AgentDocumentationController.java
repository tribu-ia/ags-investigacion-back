package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.FinalizeDocumentationRequest;
import com.tribu.interview.manager.dto.SaveMarkdownRequest;
import com.tribu.interview.manager.model.AgentDocumentation;
import com.tribu.interview.manager.service.impl.AgentDocumentationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/agent-documentation")
public class AgentDocumentationController {
    
    private final AgentDocumentationService documentationService;
    
    @PostMapping("/markdown")
    public ResponseEntity<AgentDocumentation> saveMarkdown(@RequestBody SaveMarkdownRequest request) {
        return ResponseEntity.ok(documentationService.saveMarkdown(request));
    }
    
    @PostMapping("/finalize")
    public ResponseEntity<AgentDocumentation> finalizeDocumentation(
            @RequestPart("documents") List<MultipartFile> documents,
            @NotNull() @RequestPart("assignmentId") String assignmentId) {
        return ResponseEntity.ok(documentationService.finalizeDocumentation(FinalizeDocumentationRequest.builder()
                        .assignmentId(assignmentId)
                        .documents(documents)
                .build()));
    }
    
    @GetMapping("/{assignmentId}")
    public ResponseEntity<AgentDocumentation> getDocumentation(
            @PathVariable String assignmentId) {
        return ResponseEntity.ok(documentationService.getDocumentation(assignmentId));
    }
} 
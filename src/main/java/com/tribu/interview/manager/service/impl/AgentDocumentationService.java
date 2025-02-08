package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.model.AgentDocumentation;
import com.tribu.interview.manager.dto.SaveMarkdownRequest;
import com.tribu.interview.manager.dto.FinalizeDocumentationRequest;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentDocumentationRepository;
import com.tribu.interview.manager.service.IGithubService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentDocumentationService {
    
    private final JdbcAgentDocumentationRepository documentationRepository;
    private final IGithubService githubService;
    
    public AgentDocumentation saveMarkdown(SaveMarkdownRequest request) {
        AgentDocumentation documentation = documentationRepository
            .findByAssignmentId(request.getAssignmentId())
            .orElse(AgentDocumentation.builder()
                .assignmentId(request.getAssignmentId())
                .status("DRAFT")
                .documentationDate(LocalDateTime.now())
                .build());
        
        documentation.setMarkdownContent(request.getMarkdownContent());
        
        return documentationRepository.save(documentation);
    }
    
    public AgentDocumentation finalizeDocumentation(FinalizeDocumentationRequest request) {
        // Obtener la documentación existente
        AgentDocumentation documentation = documentationRepository
            .findByAssignmentId(request.getAssignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Documentation not found"));
            
        // TODO: Implementar la carga a GitHub
        // Por ahora solo mockearemos la funcionalidad
        String githubUrl = mockGithubUpload(documentation, request.getDocuments());
        
        // Actualizar estado
        documentation.setStatus("COMPLETED");
        
        return documentationRepository.save(documentation);
    }
    
    private String mockGithubUpload(AgentDocumentation documentation, List<MultipartFile> documents) {
        // Mock de la funcionalidad de GitHub
        // Aquí iría la lógica real usando githubService
        log.info("Mock: Subiendo {} documentos a GitHub para la documentación {}", 
            documents.size(), documentation.getId());
        return "https://github.com/org/repo/docs/" + documentation.getId();
    }
    
    public AgentDocumentation getDocumentation(String assignmentId) {
        return documentationRepository.findByAssignmentId(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Documentation not found"));
    }
} 
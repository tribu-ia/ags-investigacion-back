package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.AgentDocumentation;
import com.tribu.interview.manager.dto.SaveMarkdownRequest;
import com.tribu.interview.manager.dto.FinalizeDocumentationRequest;
import com.tribu.interview.manager.repository.jdbc.JdbcAIAgentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentDocumentationRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentDocumentationService {
    
    private final JdbcAgentDocumentationRepository documentationRepository;
    private final JdbcAgentAssignmentRepository assignmentRepository;
    private final JdbcAIAgentRepository agentRepository;
    private final GithubService githubService;
    
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
            
        // Obtener la asignación y el agente
        AgentAssignment assignment = assignmentRepository
            .findById(request.getAssignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        // Crear estructura de carpetas usando el slug del agente
        String folderPath = assignment.getAgent().getSlug();
        String documentName = "documentacion.md"; // o podría ser {agentSlug}.md
        
        // Subir documentación a GitHub
        String githubUrl = githubService.uploadDocumentation(
            folderPath,
            documentName,
            documentation.getMarkdownContent(),
            request.getDocuments()
        );
        
        // Actualizar estado y guardar
        documentation.setStatus("COMPLETED");
        AgentDocumentation savedDoc = documentationRepository.save(documentation);
        
        log.info("Documentation finalized and uploaded to GitHub: {}", githubUrl);
        
        return savedDoc;
    }
    
    public AgentDocumentation getDocumentation(String assignmentId) {
        return documentationRepository.findByAssignmentId(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Documentation not found"));
    }
} 
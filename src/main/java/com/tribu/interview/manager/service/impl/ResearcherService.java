package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.repository.jdbc.JdbcAIAgentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentAssignmentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcResearcherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearcherService implements IResearcherService {
    private final JdbcResearcherRepository researcherRepository;
    private final GithubService githubService;
    private final PresentationService presentationService;
    private final JdbcAgentAssignmentRepository assignmentRepository;
    private final JdbcAIAgentRepository aiAgentRepository;

    @Override
    @Transactional
    public ResearcherResponse createResearcher(ResearcherRequest request) {
        log.info("Creating new researcher with email: {}", request.getEmail());
        
        validateRequest(request);
        GithubUserResponse githubData = fetchGithubData(request.getGithubUsername());
        
        // Create researcher
        Researcher researcher = createResearcherEntity(request, githubData);
        researcher = researcherRepository.save(researcher);
        log.info("Researcher created with ID: {}", researcher.getId());

        // Create assignment and schedule presentation
        AgentAssignment assignment = createAgentAssignment(researcher, request.getAgentId());
        log.info("Assignment created for researcher: {} and agent: {}", 
            researcher.getId(), request.getAgentId());

        Presentation presentation = presentationService.createPresentation(assignment);
        log.info("Presentation scheduled for week: {}", presentation);
        
        return buildSuccessResponse(researcher, request.getAgentId());
    }

    private void validateRequest(ResearcherRequest request) {
        validateUniqueEmail(request.getEmail());
        validateAgentAvailability(request.getAgentId());
        validateAgentExists(request.getAgentId());
    }

    private void validateUniqueEmail(String email) {
        if (researcherRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "An account with this email already exists");
        }
    }

    private void validateAgentExists(String agentId) {
        if (aiAgentRepository.findById(agentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Agent not found");
        }
    }

    private void validateAgentAvailability(String agentId) {
        Optional<AgentAssignment> existingAssignment = 
            assignmentRepository.findActiveAssignmentByAgentId(agentId);
        
        if (existingAssignment.isPresent()) {
            Researcher currentResearcher = existingAssignment.get().getResearcher();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                String.format("Agent is already assigned to %s", currentResearcher.getName()));
        }
    }

    private GithubUserResponse fetchGithubData(String githubUsername) {
        return githubService.fetchUserData(githubUsername)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "No se pudo verificar el usuario de GitHub, por favor verifique que el usuario exista y que tenga acceso a la API de GitHub"));
    }

    private Researcher createResearcherEntity(ResearcherRequest request, GithubUserResponse githubData) {
        return Researcher.builder()
            .name(request.getName())
            .email(request.getEmail())
            .phone(request.getPhone())
            .githubUsername(request.getGithubUsername())
            .avatarUrl(githubData.getAvatarUrl())
            .repositoryUrl(githubData.getHtmlUrl())
            .linkedinProfile(request.getLinkedinProfile())
            .build();
    }

    private AgentAssignment createAgentAssignment(Researcher researcher, String agentId) {
        AIAgent agent = aiAgentRepository.findById(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        return assignmentRepository.save(AgentAssignment.builder()
            .researcher(researcher)
            .agent(agent)
            .status("active")
            .assignedAt(LocalDateTime.now())
            .build());
    }

    private ResearcherResponse buildSuccessResponse(Researcher researcher, String agentId) {
        return ResearcherResponse.builder()
            .success(true)
            .message("Successfully created account and assigned agent")
            .data(ResearcherData.builder()
                .id(researcher.getId())
                .name(researcher.getName())
                .email(researcher.getEmail())
                .phone(researcher.getPhone())
                .githubUsername(researcher.getGithubUsername())
                .avatarUrl(researcher.getAvatarUrl())
                .repositoryUrl(researcher.getRepositoryUrl())
                .linkedinProfile(researcher.getLinkedinProfile())
                .agentId(agentId)
                .status("assigned")
                .build())
            .build();
    }
} 
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.repository.AIAgentRepository;
import com.tribu.interview.manager.repository.AgentAssignmentRepository;
import com.tribu.interview.manager.repository.PresentationRepository;
import com.tribu.interview.manager.repository.ResearcherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearcherService implements IResearcherService {
    private final ResearcherRepository researcherRepository;
    private final GithubService githubService;
    private final PresentationSchedulerService presentationSchedulerService;
    private final AgentAssignmentRepository assignmentRepository;
    private final AIAgentRepository aiAgentRepository;

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
        
        // Schedule presentation
        Presentation presentation = presentationSchedulerService.schedulePresentation(assignment);
        log.info("Presentation scheduled for: {}", presentation.getPresentationDate());
        
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
        if (!aiAgentRepository.existsById(agentId)) {
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
                "Could not verify GitHub user"));
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
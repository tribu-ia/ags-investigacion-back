package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.Researcher;
import com.tribu.interview.manager.repository.jdbc.JdbcAIAgentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentAssignmentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcPresentationRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcResearcherRepository;
import com.tribu.interview.manager.service.IGithubService;
import com.tribu.interview.manager.service.IResearcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResearcherService implements IResearcherService {
    private final JdbcResearcherRepository researcherRepository;
    private final IGithubService githubService;
    private final PresentationService presentationService;
    private final JdbcAgentAssignmentRepository assignmentRepository;
    private final JdbcAIAgentRepository aiAgentRepository;
    private final JdbcPresentationRepository presentationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

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
        
        return buildSuccessResponse(researcher,
                request.getAgentId(), presentation);
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

    private ResearcherResponse buildSuccessResponse(Researcher researcher,
                                                    String agentId,
                                                    Presentation presentation) {
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
                .presentationDateTime(presentation.getPresentationDate())
            .build();
    }

    public ResearcherDetailDto getResearcherDetailsByEmail(String email) {
        Researcher researcher = researcherRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investigador no encontrado"));

        Optional<Presentation> presentation = presentationRepository.findCurrentPresentationByResearcherId(researcher.getId());
        Optional<AgentAssignment> assignment = assignmentRepository.findActiveAssignmentByResearcherId(researcher.getId());

        return buildResearcherDetailDto(researcher, presentation.orElse(null), assignment.orElse(null));
    }

    @Transactional
    public ResearcherDetailDto updateResearcherProfile(String email, ResearcherUpdateDto updateDto) {
        Researcher researcher = researcherRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Investigador no encontrado"));

        // Si el username de GitHub cambió, actualizar la información desde GitHub
        if (!researcher.getGithubUsername().equals(updateDto.getGithubUsername())) {
            GithubUserResponse githubInfo = githubService.fetchUserData(updateDto.getGithubUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "No se pudo verificar el usuario de GitHub"));
            
            researcher.setAvatarUrl(githubInfo.getAvatarUrl());
            researcher.setRepositoryUrl(githubInfo.getHtmlUrl());
            researcher.setGithubUsername(updateDto.getGithubUsername());
        }

        researcher.setCurrentRole(updateDto.getCurrentRole());
        researcher.setLinkedinProfile(updateDto.getLinkedinProfile());

        researcher = researcherRepository.save(researcher);
        Optional<Presentation> presentation = presentationRepository.findCurrentPresentationByResearcherId(researcher.getId());
        Optional<AgentAssignment> assignment = assignmentRepository.findActiveAssignmentByResearcherId(researcher.getId());

        return buildResearcherDetailDto(researcher, presentation.orElse(null), assignment.orElse(null));
    }

    private ResearcherDetailDto buildResearcherDetailDto(Researcher researcher,
                                                         Presentation presentation,
                                                         AgentAssignment assignment) {
        ResearcherDetailDto.ResearcherDetailDtoBuilder builder = ResearcherDetailDto.builder()
            .name(researcher.getName())
            .email(researcher.getEmail())
            .avatarUrl(researcher.getAvatarUrl())
            .repositoryUrl(researcher.getRepositoryUrl())
            .linkedinProfile(researcher.getLinkedinProfile())
            .currentRole(researcher.getCurrentRole())
            .githubUsername(researcher.getGithubUsername());

        if (presentation != null) {
            builder
                .presentationDate(presentation.getPresentationDate().format(DATE_FORMATTER))
                .presentationTime(presentation.getPresentationDate().format(TIME_FORMATTER))
                .status(presentation.getStatus())
                .presentationWeek(String.valueOf(presentation.getPresentationWeek()));
        }

        if (assignment != null) {
            int showOrder = 0;
            if(presentation != null && presentation.getPresentationVideo() != null){
                showOrder = presentation.getPresentationVideo().getShowOrder();
            }

            builder.agentName(assignment.getAgent().getName());
            builder.agentDescription(assignment.getAgent().getShortDescription());
            builder.agentCategory(assignment.getAgent().getCategory());
            builder.agentIndustry(assignment.getAgent().getIndustry());
            builder.assignmentId(assignment.getId());
            builder.showOrder(showOrder);
        }

        return builder.build();
    }
} 
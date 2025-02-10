package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.dto.enums.ResearcherTypeEnum;
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
import org.springframework.dao.DataIntegrityViolationException;

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

        validateAgentExists(request.getAgentId());
        GithubUserResponse githubData = fetchGithubData(request.getGithubUsername());

        // Obtener o crear investigador
        Researcher researcher = researcherRepository.findByEmail(request.getEmail())
            .map(existing -> updateExistingResearcher(existing, request, githubData))
            .orElseGet(() -> createAndSaveNewResearcher(request, githubData));

        // Create assignment and schedule presentation
        AgentAssignment assignment = createAgentAssignment(researcher, request.getAgentId());
        log.info("Assignment created for researcher: {} and agent: {}",
            researcher.getId(), request.getAgentId());

        Presentation presentation = null;
        if (request.getRole().equalsIgnoreCase(ResearcherTypeEnum.PRIMARY.name())){
            presentation = presentationService.createPresentation(assignment);
            log.info("Presentation scheduled for week: {}", presentation);
        }
        
        return buildSuccessResponse(researcher,
                request.getAgentId(), presentation, researcher.getRole());
    }

    private Researcher updateExistingResearcher(Researcher existing, ResearcherRequest request, GithubUserResponse githubData) {
        // Actualizar solo si hay cambios en la información
        if (!existing.getGithubUsername().equals(request.getGithubUsername())) {
            existing.setGithubUsername(request.getGithubUsername());
            existing.setAvatarUrl(githubData.getAvatarUrl());
            existing.setRepositoryUrl(githubData.getHtmlUrl());
        }
        if (!existing.getPhone().equals(request.getPhone())) {
            existing.setPhone(request.getPhone());
        }
        if (!existing.getLinkedinProfile().equals(request.getLinkedinProfile())) {
            existing.setLinkedinProfile(request.getLinkedinProfile());
        }
        
        return researcherRepository.save(existing);
    }

    private Researcher createAndSaveNewResearcher(ResearcherRequest request, GithubUserResponse githubData) {
        Researcher researcher = createResearcherEntity(request, githubData);
        researcher = researcherRepository.save(researcher);
        log.info("Researcher created with ID: {}", researcher.getId());
        return researcher;
    }


    private void validateAgentExists(String agentId) {
        if (aiAgentRepository.findById(agentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Agent not found");
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
            .role(request.getRole())
            .createdAt(LocalDateTime.now())
            .build();
    }

    private AgentAssignment createAgentAssignment(Researcher researcher, String agentId) {
        AIAgent agent = aiAgentRepository.findById(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found"));

        // Primero verificamos si ya existe la asignación
        if (assignmentRepository.existsByResearcherIdAndAgentId(researcher.getId(), agentId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, 
                "Ya tienes una asignación activa para este agente"
            );
        }

        try {
            return assignmentRepository.save(AgentAssignment.builder()
                .researcher(researcher)
                .agent(agent)
                .status("active")
                .assignedAt(LocalDateTime.now())
                .build());
        } catch (DataIntegrityViolationException e) {
            // Por si ocurre una condición de carrera
            if (e.getMessage() != null && e.getMessage().contains("unique_assignment_pair")) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT, 
                    "Ya tienes una asignación activa para este agente"
                );
            }
            throw e;
        }
    }

    private ResearcherResponse buildSuccessResponse(Researcher researcher,
                                                    String agentId,
                                                    Presentation presentation,
                                                    String role) {
        return ResearcherResponse.builder()
            .success(true)
            .message("Successfully created account and assigned agent")
                .role(role)
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
                .role(researcher.getRole())
                .build())
            .presentationDateTime(presentation != null ? presentation.getPresentationDate() : null)
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

        researcher.setRole(updateDto.getCurrentRole());
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
            .role(researcher.getRole())
            .githubUsername(researcher.getGithubUsername());

        if (presentation != null) {
            builder
                .presentationDate(presentation.getPresentationDate().format(DATE_FORMATTER))
                .presentationTime(presentation.getPresentationDate().format(TIME_FORMATTER))
                .status(presentation.getStatus())
                .presentationWeek(String.valueOf(presentation.getPresentationWeek()));
            builder.showOrder(presentation.getShowOrder());
        }

        if (assignment != null) {

            builder.agentName(assignment.getAgent().getName());
            builder.agentDescription(assignment.getAgent().getShortDescription());
            builder.agentCategory(assignment.getAgent().getCategory());
            builder.agentIndustry(assignment.getAgent().getIndustry());
            builder.assignmentId(assignment.getId());

        }

        return builder.build();
    }
} 
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.jdbc.JdbcAIAgentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentAssignmentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentDocumentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentManagerService implements IAgentManagerService {
    private final JdbcAIAgentRepository aiAgentRepository;
    private final JdbcAgentDocumentationRepository documentationRepository;
    private final JdbcAgentAssignmentRepository assignmentRepository;
    
    @Override
    public List<AIAgent> processJsonData(AgentUploadRequest payload) {
        List<AIAgent> agents = payload.getData().get(0).getJson().getData().stream()
            .filter(this::isValidDocument)
            .map(this::mapToAIAgent)
            .toList();
        
        log.info("Processing {} valid agents", agents.size());
        return aiAgentRepository.saveAll(agents);
    }

    @Override
    public MetadataResponse getMetadata() {
        return MetadataResponse.builder()
            .categories(aiAgentRepository.findDistinctCategories())
            .industries(aiAgentRepository.findDistinctIndustries())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedAgentResponse getAgents(int page, int pageSize, String category, String industry, String search) {
        List<AIAgent> agents = aiAgentRepository.findAllWithFilters(category, industry, search);

        return PaginatedAgentResponse.builder()
            .content(agents.stream()
                .map(this::mapToAgentWithAssignmentDto)
                .collect(Collectors.toList()))
            .totalElements((long) agents.size())
            .totalPages((int) Math.ceil((double) agents.size() / pageSize))
            .currentPage(page)
            .build();
    }

    private AgentWithAssignmentDto mapToAgentWithAssignmentDto(AIAgent agent) {
        boolean isAssigned = agent.getAssignmentStatus() != null && agent.getAssignmentStatus().equals("active");
        
        AgentWithAssignmentDto.AssignmentInfoDto assignmentInfo = isAssigned ? 
            AgentWithAssignmentDto.AssignmentInfoDto.builder()
                .assignedTo(agent.getAssignedToName())
                .assignedEmail(agent.getAssignedToEmail())
                .assignedAt(agent.getAssignedAt())
                .build() : null;

        return AgentWithAssignmentDto.builder()
            .id(agent.getId())
            .name(agent.getName())
            .createdBy(agent.getCreatedBy())
            .website(agent.getWebsite())
            .access(agent.getAccess())
            .pricingModel(agent.getPricingModel())
            .category(agent.getCategory())
            .industry(agent.getIndustry())
            .shortDescription(agent.getShortDescription())
            .longDescription(agent.getLongDescription())
            .keyFeatures(agent.getKeyFeatures())
            .useCases(agent.getUseCases())
            .tags(agent.getTags())
            .logo(agent.getLogo())
            .logoFileName(agent.getLogoFileName())
            .image(agent.getImage())
            .imageFileName(agent.getImageFileName())
            .video(agent.getVideo())
            .upvotes(agent.getUpvotes())
            .approved(agent.getApproved())
            .createdAt(agent.getCreatedAt())
            .slug(agent.getSlug())
            .version(agent.getVersion())
            .featured(agent.getFeatured())
            .isAssigned(isAssigned)
            .assignmentInfo(assignmentInfo)
            .build();
    }

    @Override
    @Transactional
    public DocumentationResponse completeAgentDocumentation(String agentId, DocumentationRequest request) {
        AgentAssignment assignment = assignmentRepository.findActiveAssignmentByAgentId(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active assignment found for this agent"));

        AgentDocumentation documentation = AgentDocumentation.builder()
            .agentId(agentId)
            .title(request.getTitle())
            .content(request.getContent())
            .url(request.getUrl())
            .type(request.getType())
            .build();

        documentation = documentationRepository.save(documentation);

        // Update assignment status
        assignment.setStatus("completed");
        assignmentRepository.save(assignment);

        return DocumentationResponse.builder()
            .success(true)
            .message("Agent documentation successfully registered")
            .data(DocumentationData.builder()
                .agentId(agentId)
                .researcherId(assignment.getResearcher().getId())
                .status("completed")
                .title(request.getTitle())
                .content(request.getContent())
                .url(request.getUrl())
                .type(request.getType())
                .build())
            .build();
    }

    private boolean isValidDocument(AgentData item) {
        return Optional.ofNullable(item)
            .filter(i -> StringUtils.hasText(i.getName()))
            .filter(i -> StringUtils.hasText(i.getCategory()))
            .filter(i -> StringUtils.hasText(i.getIndustry()))
            .filter(i -> StringUtils.hasText(i.getShortDescription()))
            .isPresent();
    }

    private AIAgent mapToAIAgent(AgentData item) {
        return AIAgent.builder()
            .id(item.getId())
            .name(item.getName())
            .createdBy(Optional.ofNullable(item.getCreatedBy()).orElse(""))
            .website(Optional.ofNullable(item.getWebsite()).orElse(""))
            .access(Optional.ofNullable(item.getAccess()).orElse(""))
            .pricingModel(Optional.ofNullable(item.getPricingModel()).orElse(""))
            .category(item.getCategory())
            .industry(item.getIndustry())
            .shortDescription(item.getShortDescription())
            .longDescription(Optional.ofNullable(item.getLongDescription()).orElse(""))
            .keyFeatures(Optional.ofNullable(item.getKeyFeatures()).orElse(""))
            .useCases(Optional.ofNullable(item.getUseCases()).orElse(""))
            .tags(Optional.ofNullable(item.getTags()).orElse(""))
            .logo(Optional.ofNullable(item.getLogo()).orElse(""))
            .logoFileName(Optional.ofNullable(item.getLogoFileName()).orElse(""))
            .image(Optional.ofNullable(item.getImage()).orElse(""))
            .imageFileName(Optional.ofNullable(item.getImageFileName()).orElse(""))
            .video(Optional.ofNullable(item.getVideo()).orElse(""))
            .upvotes(Optional.ofNullable(item.getUpvotes()).orElse(0))
            .approved(Optional.ofNullable(item.getApproved()).orElse(false))
            .createdAt(LocalDateTime.now())
            .slug(Optional.ofNullable(item.getSlug()).orElse(""))
            .version(Optional.ofNullable(item.getVersion()).orElse(""))
            .featured(Optional.ofNullable(item.getFeatured()).orElse(false))
            .build();
    }

    public StatsDto getStats() {
        return StatsDto.builder()
                .active_investigators(assignmentRepository.countByStatus("active"))
                .documented_agents(documentationRepository.countDocumentedAgents())
                .total_agents(aiAgentRepository.count())
                .build();
    }
} 
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.*;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentManagerService implements IAgentManagerService {
    private final AIAgentRepository aiAgentRepository;
    private final AgentDocumentationRepository documentationRepository;
    private final AgentAssignmentRepository assignmentRepository;
    
    @Override
    public List<AIAgent> processJsonData(AgentUploadRequest payload) {
        List<AIAgent> agents = payload.getData().getJson().getData().stream()
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
    public PaginatedResponse<AIAgent> getAgents(int page, int pageSize, String category, 
            String industry, String search) {
        Specification<AIAgent> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            Expression<String> nameExpression = root.get("name").as(String.class);
            Expression<String> descriptionExpression = root.get("shortDescription").as(String.class);
            
            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            
            if (StringUtils.hasText(industry)) {
                predicates.add(cb.equal(root.get("industry"), industry));
            }
            
            if (StringUtils.hasText(search)) {
                String searchLower = search.toLowerCase();
                predicates.add(cb.or(
                    cb.like(cb.lower(nameExpression), "%" + searchLower + "%"),
                    cb.like(cb.lower(descriptionExpression), "%" + searchLower + "%")
                ));
            }
            
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
        
        PageRequest pageRequest = PageRequest.of(page - 1, pageSize);
        Page<AIAgent> agentPage = aiAgentRepository.findAll(spec, pageRequest);
        
        return PaginatedResponse.of(
            agentPage.getContent(),
            agentPage.getTotalElements(),
            page,
            pageSize
        );
    }

    @Override
    @Transactional
    public DocumentationResponse completeAgentDocumentation(String agentId, 
            DocumentationRequest request) {
        AgentAssignment assignment = assignmentRepository.findActiveAssignmentByAgentId(agentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "No active assignment found for this agent"));

        AgentDocumentation documentation = AgentDocumentation.builder()
            .assignment(assignment)
            .findings(request.getFindings())
            .recommendations(request.getRecommendations())
            .researchSummary(request.getResearchSummary())
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
                .findings(request.getFindings())
                .recommendations(request.getRecommendations())
                .researchSummary(request.getResearchSummary())
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
            .createdBy(item.getCreatedBy())
            .website(item.getWebsite())
            .access(item.getAccess())
            .pricingModel(item.getPricingModel())
            .category(item.getCategory())
            .industry(item.getIndustry())
            .shortDescription(item.getShortDescription())
            .longDescription(item.getLongDescription())
            .keyFeatures(item.getKeyFeatures())
            .useCases(item.getUseCases())
            .tags(item.getTags())
            .logo(item.getLogo())
            .logoFileName(item.getLogoFileName())
            .image(item.getImage())
            .imageFileName(item.getImageFileName())
            .video(item.getVideo())
            .upvotes(item.getUpvotes())
            .upvoters(item.getUpvoters().toString())
            .approved(item.getApproved())
            .createdAt(LocalDateTime.now())
            .slug(item.getSlug())
            .version(item.getVersion())
            .featured(item.getFeatured())
            .build();
    }

    public StatsDto getStats() {
        return StatsDto.builder()
                .active_investigators(assignmentRepository.countByStatus("active"))
                .documented_agents(documentationRepository.countDocumentedAgents())
                .total_agents((long) aiAgentRepository.findAll().size())
                .build();
    }
} 
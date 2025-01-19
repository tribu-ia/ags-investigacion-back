package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import java.util.List;

public interface IAgentManagerService {
    List<AIAgent> processJsonData(AgentUploadRequest jsonItems);
    MetadataResponse getMetadata();
    PaginatedResponse<AIAgent> getAgents(int page, int pageSize, String category, String industry, String search);
    DocumentationResponse completeAgentDocumentation(String agentId, DocumentationRequest documentationData);
} 
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import java.util.List;

public interface IAgentManagerService {
    List<AIAgent> processJsonData(AgentUploadRequest jsonItems);
    MetadataResponse getMetadata();


    /**
     * Esto sirve para que las personas sean mas pros
     * @param page
     * @param pageSize
     * @param category
     * @param industry
     * @param search
     * @return
     */
    PaginatedAgentResponse getAgents(int page, int pageSize, String category, String industry, String search);

    List<AgentResearcherResponseDto> getActiveAgents(String state, String email);
}
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.*;
import com.tribu.interview.manager.model.AIAgent;
import java.util.List;

public interface IAgentManagerService {
    List<AIAgent> processJsonData(AgentUploadRequest jsonItems);
    MetadataResponse getMetadata();
    PaginatedAgentResponse getAgents(int page, int pageSize, String category, String industry, String search);

}
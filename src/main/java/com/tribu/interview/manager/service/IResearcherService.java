package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.ResearcherRequest;
import com.tribu.interview.manager.dto.ResearcherResponse;

public interface IResearcherService {
    ResearcherResponse createResearcher(ResearcherRequest request);
} 
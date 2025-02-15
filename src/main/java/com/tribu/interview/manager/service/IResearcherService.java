package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.ResearcherDetailDto;
import com.tribu.interview.manager.dto.ResearcherRequest;
import com.tribu.interview.manager.dto.ResearcherResponse;
import com.tribu.interview.manager.dto.SimpleResearcherRequest;
import com.tribu.interview.manager.model.Researcher;
import jakarta.validation.Valid;

public interface IResearcherService {
    ResearcherResponse createResearcher(ResearcherRequest request);

    Researcher getResearcher(String email);

    ResearcherResponse createNewAgentAssignmentRequest(@Valid SimpleResearcherRequest simpleResearcherRequest);
} 
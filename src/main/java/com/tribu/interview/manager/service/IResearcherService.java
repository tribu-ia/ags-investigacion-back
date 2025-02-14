package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.ResearcherDetailDto;
import com.tribu.interview.manager.dto.ResearcherRequest;
import com.tribu.interview.manager.dto.ResearcherResponse;
import com.tribu.interview.manager.model.Researcher;

public interface IResearcherService {
    ResearcherResponse createResearcher(ResearcherRequest request);

    Researcher getResearcher(String email);
} 
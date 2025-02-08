package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.ChallengeStatusResponse;
import com.tribu.interview.manager.repository.jdbc.JdbcConfigParamsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigParamsService {
    
    private final JdbcConfigParamsRepository configParamsRepository;
    
    public ChallengeStatusResponse
    getChallengeStatus() {
        return configParamsRepository.getChallengeStatus();
    }
    
    public Integer getCurrentMonthForChallenge() {
        return configParamsRepository.getChallengeStatus().getCurrentMonth();
    }
} 
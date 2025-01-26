package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.CalendarPresentationDto;
import com.tribu.interview.manager.dto.WeekPresentationsResponse;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.AgentAssignment;
import java.util.List;

public interface IPresentationService {
    Presentation createPresentation(AgentAssignment assignment);
    WeekPresentationsResponse getCurrentWeekPresentations();
} 
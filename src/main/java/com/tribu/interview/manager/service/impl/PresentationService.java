package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.CalendarPresentationDto;
import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.jdbc.JdbcPresentationRepository;
import com.tribu.interview.manager.service.IPresentationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationService implements IPresentationService {
    private final JdbcPresentationRepository presentationRepository;

    @Transactional
    public Presentation createPresentation(AgentAssignment assignment) {

        // Calculate presentation date based on week
        LocalDateTime presentationDate = calculatePresentationDate();

        Presentation presentation = Presentation.builder()
            .assignment(assignment)
            .presentationWeek(0)
            .presentationDate(presentationDate)
            .status(PresentationStatusEnum.PENDING.toString())
            .votesCount(0)
            .isWinner(false)
            .build();

        return presentationRepository.save(presentation);
    }

    public List<CalendarPresentationDto> getCurrentWeekPresentations() {
        return presentationRepository.findUpcomingPresentations();
    }

    private LocalDateTime calculatePresentationDate() {
        // Obtener la última fecha de presentación programada o usar la fecha base si no hay presentaciones
        LocalDateTime baseDate = presentationRepository.findLatestPresentationDate()
            .orElse(LocalDateTime.of(2025, 1, 21, 18, 0));
        
        // Verificar si hay espacio en la semana actual
        long presentationsCount = presentationRepository.countPresentationsForWeek(
            baseDate.toLocalDate());
            
        // Si ya hay 5 presentaciones, avanzar a la siguiente semana
        if (presentationsCount >= 5) {
            baseDate = baseDate.plusWeeks(1);
        }
        
        return baseDate;
    }


} 
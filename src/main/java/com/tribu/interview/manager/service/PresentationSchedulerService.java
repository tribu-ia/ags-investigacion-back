package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.repository.PresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationSchedulerService {
    private final PresentationRepository presentationRepository;
    private static final int MAX_WEEKLY_PRESENTATIONS = 6;
    private static final int PRESENTATION_HOUR = 18; // 6 PM
    private static final int PRESENTATION_MINUTE = 0;

    @Transactional
    public Presentation schedulePresentation(AgentAssignment assignment) {
        LocalDateTime nextAvailableSlot = findNextAvailableSlot();
        
        Presentation presentation = Presentation.builder()
            .assignment(assignment)
            .presentationDate(nextAvailableSlot)
            .status(PresentationStatusEnum.SCHEDULED)
            .weeklyVotes(0)
            .monthlyVotes(0)
            .weeklyWinner(false)
            .monthlyWinner(false)
            .build();
        
        return presentationRepository.save(presentation);
    }

    private LocalDateTime findNextAvailableSlot() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextTuesday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            .withHour(PRESENTATION_HOUR)
            .withMinute(PRESENTATION_MINUTE)
            .withSecond(0)
            .withNano(0);

        // Si es después de la hora de presentación del martes, ir al próximo martes
        if (now.isAfter(nextTuesday)) {
            nextTuesday = nextTuesday.plusWeeks(1);
        }

        // Buscar una semana con espacio disponible
        while (true) {
            LocalDateTime weekStart = nextTuesday;
            LocalDateTime weekEnd = weekStart.plusWeeks(1);

            long presentationsInWeek = presentationRepository.countByPresentationDateBetween(
                weekStart, weekEnd);

            if (presentationsInWeek < MAX_WEEKLY_PRESENTATIONS) {
                return nextTuesday;
            }
            
            nextTuesday = nextTuesday.plusWeeks(1);
        }
    }

    public List<Presentation> getCurrentWeekPresentations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentTuesday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
            .withHour(PRESENTATION_HOUR)
            .withMinute(PRESENTATION_MINUTE);
        
        // Si es después del martes, obtener las presentaciones de la próxima semana
        if (now.isAfter(currentTuesday)) {
            LocalDateTime nextTuesday = currentTuesday.plusWeeks(1);
            return presentationRepository.findByPresentationDateBetweenOrderByPresentationDate(
                nextTuesday, nextTuesday.plusWeeks(1));
        }
        
        // Si es antes o durante el martes, obtener las presentaciones de esta semana
        return presentationRepository.findByPresentationDateBetweenOrderByPresentationDate(
            currentTuesday, currentTuesday.plusWeeks(1));
    }

    public boolean canSchedulePresentation(LocalDateTime date) {
        LocalDateTime weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY))
            .withHour(PRESENTATION_HOUR)
            .withMinute(PRESENTATION_MINUTE);
        LocalDateTime weekEnd = weekStart.plusWeeks(1);

        long presentationsInWeek = presentationRepository.countByPresentationDateBetween(
            weekStart, weekEnd);

        return presentationsInWeek < MAX_WEEKLY_PRESENTATIONS;
    }

    @Transactional
    public void registerVote(String presentationId, String userEmail) {
        Presentation presentation = presentationRepository.findById(presentationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Presentation not found"));

        validateVotingPeriod(presentation.getPresentationDate());
        
        // Incrementar votos
        presentation.incrementWeeklyVotes();
        presentation.incrementMonthlyVotes();
        
        presentationRepository.save(presentation);
    }

    private void validateVotingPeriod(LocalDateTime presentationDate) {
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isBefore(presentationDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Voting is not open yet");
        }

        LocalDateTime votingEnd = presentationDate
            .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            .withHour(23)
            .withMinute(59);

        if (now.isAfter(votingEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Voting period has ended");
        }
    }
} 
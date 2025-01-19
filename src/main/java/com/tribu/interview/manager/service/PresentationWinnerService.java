package com.tribu.interview.manager.service;

import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.repository.PresentationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresentationWinnerService {
    private final PresentationRepository presentationRepository;

    @Transactional
    public void processWeeklyWinners(LocalDateTime weekStart) {
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        List<Presentation> presentations = presentationRepository
            .findByPresentationDateBetweenOrderByWeeklyVotesDesc(weekStart, weekEnd);
        
        if (!presentations.isEmpty()) {
            Presentation winner = presentations.get(0);
            winner.setWeeklyWinner(true);
            presentationRepository.save(winner);
            log.info("Weekly winner processed for week starting {}: {}", 
                weekStart, winner.getAssignment().getResearcher().getName());
        }
    }

    @Transactional
    public void processMonthlyWinners(int month, int year) {
        List<Presentation> presentations = presentationRepository
            .findByMonthOfYearAndYearOrderByMonthlyVotesDesc(month, year);
        
        if (!presentations.isEmpty()) {
            Presentation winner = presentations.get(0);
            winner.setMonthlyWinner(true);
            presentationRepository.save(winner);
            log.info("Monthly winner processed for {}/{}: {}", 
                month, year, winner.getAssignment().getResearcher().getName());
        }
    }

    // Método para procesar automáticamente los ganadores
    @Scheduled(cron = "0 0 0 * * MON") // Cada lunes a medianoche
    public void processWeeklyWinnersAutomatically() {
        LocalDateTime previousWeekStart = LocalDateTime.now()
            .minusWeeks(1)
            .with(TemporalAdjusters.previous(DayOfWeek.TUESDAY))
            .withHour(0)
            .withMinute(0)
            .withSecond(0);
        
        processWeeklyWinners(previousWeekStart);
    }

    // Método para procesar ganadores mensuales
    @Scheduled(cron = "0 0 1 1 * *") // Primer día de cada mes a medianoche
    public void processMonthlyWinnersAutomatically() {
        LocalDateTime previousMonth = LocalDateTime.now().minusMonths(1);
        processMonthlyWinners(
            previousMonth.getMonthValue(),
            previousMonth.getYear()
        );
    }
} 
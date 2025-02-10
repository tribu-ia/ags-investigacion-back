package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.dto.CalendarPresentationDto;
import com.tribu.interview.manager.dto.WeekPresentationsResponse;
import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.jdbc.JdbcPresentationRepository;
import com.tribu.interview.manager.service.IPresentationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = "presentations")
public class PresentationService implements IPresentationService {
    private final JdbcPresentationRepository presentationRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy");
    private static final int MAX_PRESENTATIONS_PER_DATE = 5;

    @Transactional
    public Presentation createPresentation(AgentAssignment assignment) {
        LocalDateTime presentationDate = calculateNextAvailablePresentationDate();

        Presentation presentation = Presentation.builder()
            .assignment(assignment)
            .presentationWeek(0)
            .presentationDate(presentationDate)
            .status(PresentationStatusEnum.SCHEDULED.toString())
            .votesCount(0)
            .isWinner(false)
            .build();

        return presentationRepository.save(presentation);
    }

    @Retryable(
            value = {CannotGetJdbcConnectionException.class},
            backoff = @Backoff(delay = 1000)
    )
    @Cacheable(key = "#startDate.toString() + '-' + #endDate.toString()")
    public WeekPresentationsResponse getCurrentWeekPresentations() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetPresentationDate = getTargetPresentationDate(now);

        List<CalendarPresentationDto> presentations = presentationRepository.findPresentationsForRange(
            targetPresentationDate.withHour(0).withMinute(0).withSecond(0),
            targetPresentationDate.withHour(23).withMinute(59).withSecond(59)
        );

        LocalDateTime weekStartTuesday = targetPresentationDate.with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));
        
        return WeekPresentationsResponse.builder()
            .weekStart(weekStartTuesday.format(DATE_FORMATTER))
            .weekEnd(weekStartTuesday.plusDays(7).format(DATE_FORMATTER))
            .presentations(presentations)
            .build();
    }

    public LocalDateTime getTargetPresentationDate(LocalDateTime currentDate) {
        LocalDateTime currentWeekTuesday = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));

        if (currentDate.getDayOfWeek().getValue() > DayOfWeek.TUESDAY.getValue()) {
            return currentWeekTuesday.plusDays(7).withHour(18).withMinute(0).withSecond(0);
        } else if (currentDate.getDayOfWeek().getValue() == DayOfWeek.TUESDAY.getValue()) {
            return currentWeekTuesday.withHour(18).withMinute(0).withSecond(0);
        } else {
            return currentWeekTuesday.plusDays(7).withHour(18).withMinute(0).withSecond(0);
        }
    }

    private LocalDateTime calculateNextAvailablePresentationDate() {
        LocalDateTime latestDate = presentationRepository.findLatestPresentationDate()
            .orElse(LocalDateTime.of(2025, 1, 21, 18, 0));

        long presentationsCount = countPresentationsForDate(latestDate);
        
        if (presentationsCount >= MAX_PRESENTATIONS_PER_DATE) {
            return getTargetPresentationDate(latestDate.plusDays(1));
        }
        
        return latestDate;
    }

    private long countPresentationsForDate(LocalDateTime date) {
        LocalDateTime startOfDay = date.withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = date.withHour(23).withMinute(59).withSecond(59);
        
        List<CalendarPresentationDto> presentations = presentationRepository.findPresentationsForRange(startOfDay, endOfDay);
        return presentations.size();
    }

    @Recover
    public WeekPresentationsResponse recover(CannotGetJdbcConnectionException e) {
        log.error("All retries failed for database connection when fetching presentations", e);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStartTuesday = now.with(TemporalAdjusters.previous(DayOfWeek.TUESDAY));
        
        return WeekPresentationsResponse.builder()
            .weekStart(weekStartTuesday.format(DATE_FORMATTER))
            .weekEnd(weekStartTuesday.plusDays(6).format(DATE_FORMATTER))
            .presentations(Collections.emptyList())
            .build();
    }

    @Scheduled(fixedRate = 300000) // 5 minutos
    public void refreshCache() {
        LocalDateTime now = LocalDateTime.now();
        try {
            getCurrentWeekPresentations();
            log.debug("Cache refreshed successfully");
        } catch (Exception e) {
            log.error("Error refreshing cache", e);
        }
    }
} 
package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.PresentationWinnerDto;
import com.tribu.interview.manager.dto.UpcomingPresentationResponse;
import com.tribu.interview.manager.dto.WinnersResponse;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.InvestigadorRepository;
import com.tribu.interview.manager.repository.PresentationRepository;
import com.tribu.interview.manager.repository.VoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.util.Comparator;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationService {
    private final PresentationRepository presentationRepository;
    private final VoteRepository voteRepository;
    private final InvestigadorRepository investigadorRepository;

    public List<UpcomingPresentationResponse> getUpcomingPresentations() {
        LocalDateTime now = LocalDateTime.now();
        return presentationRepository.findUpcomingPresentations(now)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Scheduled(cron = "0 0 0 * * MON") // Cada lunes a medianoche
    @Transactional
    public void calculateWeeklyWinners() {
        LocalDateTime lastWeekStart = LocalDateTime.now().minusWeeks(1)
            .with(DayOfWeek.MONDAY).withHour(0).withMinute(0);
        LocalDateTime lastWeekEnd = lastWeekStart.plusDays(7);

        List<Presentation> weekPresentations = presentationRepository
            .findByPresentationDateBetween(lastWeekStart, lastWeekEnd);

        if (weekPresentations.isEmpty()) {
            log.info("No hay presentaciones para evaluar esta semana");
            return;
        }

        Presentation weeklyWinner = weekPresentations.stream()
            .max(Comparator.comparing(Presentation::getWeeklyVotes))
            .orElseThrow();

        weeklyWinner.setWeeklyWinner(true);
        presentationRepository.save(weeklyWinner);

        log.info("Ganador semanal calculado: {} con {} votos", 
            weeklyWinner.getAssignment().getResearcher().getName(),
            weeklyWinner.getWeeklyVotes());
    }

    @Scheduled(cron = "0 0 0 1 * *") // Primer día de cada mes
    @Transactional
    public void calculateMonthlyWinner() {
        LocalDateTime lastMonthStart = LocalDateTime.now()
            .minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime lastMonthEnd = lastMonthStart.plusMonths(1);

        List<Presentation> weeklyWinners = presentationRepository
            .findByWeeklyWinnerTrueAndPresentationDateBetween(lastMonthStart, lastMonthEnd);

        if (weeklyWinners.isEmpty()) {
            log.info("No hay ganadores semanales para evaluar este mes");
            return;
        }

        Presentation monthlyWinner = weeklyWinners.stream()
            .max(Comparator.comparing(Presentation::getMonthlyVotes))
            .orElseThrow();

        monthlyWinner.setMonthlyWinner(true);
        presentationRepository.save(monthlyWinner);

        log.info("Ganador mensual calculado: {} con {} votos", 
            monthlyWinner.getAssignment().getResearcher().getName(),
            monthlyWinner.getMonthlyVotes());
    }

    public WinnersResponse getWinners(Integer month, Integer year) {
        LocalDateTime now = LocalDateTime.now();
        month = month != null ? month : now.getMonthValue();
        year = year != null ? year : now.getYear();

        LocalDateTime monthStart = LocalDateTime.of(year, month, 1, 0, 0);
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        List<Presentation> weeklyWinners = presentationRepository
            .findByWeeklyWinnerTrueAndPresentationDateBetween(monthStart, monthEnd);

        List<Presentation> monthlyWinners = presentationRepository
            .findByPresentationDateBetween(monthStart, monthEnd)
            .stream()
            .filter(p -> Boolean.TRUE.equals(p.getMonthlyWinner()))
            .toList();

        return WinnersResponse.builder()
            .weeklyWinners(weeklyWinners.stream()
                .map(this::mapToPresentationWinnerDto)
                .toList())
            .monthlyWinner(monthlyWinners.isEmpty() ? null : 
                mapToPresentationWinnerDto(monthlyWinners.get(0)))
            .build();
    }

    private UpcomingPresentationResponse mapToResponse(Presentation presentation) {
        AgentAssignment assignment = presentation.getAssignment();
        Researcher researcher = assignment.getResearcher();
        AIAgent agent = assignment.getAgent();

        return UpcomingPresentationResponse.builder()
            .id(presentation.getId())
            .investigadorName(researcher.getName())
            .avatarUrl(researcher.getAvatarUrl())
            .repositoryUrl(researcher.getRepositoryUrl())
            .linkedinProfile(researcher.getLinkedinProfile())
            .role("investigador.getRole()")
            .agentName(agent.getName())
            .presentationDate(presentation.getPresentationDate())
            .weeklyVotes(presentation.getWeeklyVotes())
            .isWeeklyWinner(presentation.getWeeklyWinner())
            .monthlyVotes(presentation.getMonthlyVotes())
            .isMonthlyWinner(presentation.getMonthlyWinner())
            .build();
    }

    private PresentationWinnerDto mapToPresentationWinnerDto(Presentation presentation) {
        return PresentationWinnerDto.builder()
            .id(presentation.getId())
            .investigadorName(presentation.getAssignment().getResearcher().getName())
            .avatarUrl(presentation.getAssignment().getResearcher().getAvatarUrl())
            .agentName(presentation.getAssignment().getAgent().getName())
            .presentationDate(presentation.getPresentationDate().toString())
            .votes(presentation.getWeeklyWinner() ? 
                presentation.getWeeklyVotes() : presentation.getMonthlyVotes())
            .role("presentation.getAssignment().getInvestigador().getRole()")
            .repositoryUrl(presentation.getAssignment().getResearcher().getRepositoryUrl())
            .linkedinProfile(presentation.getAssignment().getResearcher().getLinkedinProfile())
            .build();
    }
} 
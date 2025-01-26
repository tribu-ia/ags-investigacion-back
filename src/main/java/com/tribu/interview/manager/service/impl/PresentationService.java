package com.tribu.interview.manager.service;

import com.tribu.interview.manager.dto.PresentationWinnerDto;
import com.tribu.interview.manager.dto.UpcomingPresentationResponse;
import com.tribu.interview.manager.dto.WinnersResponse;
import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import com.tribu.interview.manager.model.*;
import com.tribu.interview.manager.repository.jdbc.JdbcPresentationRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcVoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.util.Comparator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.temporal.WeekFields;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PresentationService implements IPresentationService {
    private final JdbcPresentationRepository presentationRepository;
    private final JdbcVoteRepository voteRepository;
    private static final int MAX_VOTES_PER_USER = 3;

    @Transactional
    public Presentation uploadVideo(String presentationId, String videoUrl) {
        Presentation presentation = presentationRepository.findById(presentationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Presentation not found"));

        presentation.setVideoUrl(videoUrl);
        presentation.setStatus(PresentationStatusEnum.VIDEO_UPLOADED);
        return presentationRepository.save(presentation);
    }

    @Transactional
    public Presentation createPresentation(AgentAssignment assignment) {

        // Calculate presentation date based on week
        LocalDateTime presentationDate = calculatePresentationDate();

        Presentation presentation = Presentation.builder()
            .assignment(assignment)
            //.presentationWeek(week)
            .presentationDate(presentationDate)
            .status(PresentationStatusEnum.PENDING)
            .votesCount(0)
            .isWinner(false)
            .build();

        return presentationRepository.save(presentation);
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

    public List<Presentation> getPresentationsForVoting() {
        return presentationRepository.findByStatus(PresentationStatusEnum.VOTING_OPEN);
    }

    public List<Presentation> getPresentationsForWeek(int week) {
        return presentationRepository.findByPresentationWeek(week);
    }

    private int getCurrentWeek() {
        return LocalDateTime.now().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()) % 4 + 1;
    }

    @Transactional
    public void openVotingForWeek(int week) {
        List<Presentation> presentations = presentationRepository.findByPresentationWeekAndStatus(
            week, PresentationStatusEnum.VIDEO_UPLOADED);
        
        for (Presentation presentation : presentations) {
            presentation.setStatus(PresentationStatusEnum.VOTING_OPEN);
            presentationRepository.save(presentation);
        }
    }

    @Transactional
    public void closeVoting(int week) {
        List<Presentation> presentations = presentationRepository.findByPresentationWeekAndStatus(
            week, PresentationStatusEnum.VOTING_OPEN);
        
        for (Presentation presentation : presentations) {
            presentation.setStatus(PresentationStatusEnum.COMPLETED);
            presentationRepository.save(presentation);
        }
    }

    public List<Presentation> getUpcomingPresentations() {
        return presentationRepository.findUpcomingPresentations(LocalDateTime.now());
    }

    @Transactional
    public void processVote(String presentationId, String userId) {
        if (!isVotingPeriod()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Voting is only allowed during week 5");
        }

        // Verificar límite de votos del usuario
        long userVoteCount = voteRepository.countByUser(User.builder().id(userId).build());
        if (userVoteCount >= MAX_VOTES_PER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Maximum votes limit reached for this voting period");
        }

        Presentation presentation = presentationRepository.findById(presentationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Presentation not found"));

        if (presentation.getStatus() != PresentationStatusEnum.VOTING_OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "This presentation is not open for voting");
        }

        // Verificar si el usuario ya votó por esta presentación
        if (voteRepository.existsByUserAndPresentation(
                User.builder().id(userId).build(), 
                presentationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "You have already voted for this presentation");
        }

        // Registrar voto
        Vote vote = Vote.builder()
            .user(User.builder().id(userId).build())
            .presentation(presentation)
            .voteDate(LocalDateTime.now())
            .build();

        voteRepository.save(vote);

        // Actualizar conteo de votos en la presentación
        presentation.setVotesCount(presentation.getVotesCount() + 1);
        presentationRepository.save(presentation);
    }

    private boolean isVotingPeriod() {
        int currentWeek = getCurrentWeek();
        return currentWeek == 5;
    }
} 
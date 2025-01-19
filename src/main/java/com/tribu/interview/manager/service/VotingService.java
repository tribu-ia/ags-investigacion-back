package com.tribu.interview.manager.service;

import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.Vote;
import com.tribu.interview.manager.model.VoteTypeEnum;
import com.tribu.interview.manager.repository.AgentAssignmentRepository;
import com.tribu.interview.manager.repository.PresentationRepository;
import com.tribu.interview.manager.repository.VoteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import com.tribu.interview.manager.model.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class VotingService {
    private final PresentationRepository presentationRepository;
    private final VoteRepository voteRepository;
    private final AgentAssignmentRepository assignmentRepository;
    private final UserService userService;

    @Transactional
    public void processVote(String presentationId, String userId, String userEmail) {
        // Obtener o crear usuario
        User user = userService.getOrCreateUser(userId, userEmail);
        
        Presentation presentation = presentationRepository.findById(presentationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                "Presentation not found"));
        
        LocalDateTime presentationDate = presentation.getPresentationDate();
        int week = presentationDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        int month = presentationDate.getMonthValue();
        int year = presentationDate.getYear();

        // Validar período de votación
        validateVotingPeriod(presentationDate);
        
        // Verificar si el usuario ya votó esta semana
        if (voteRepository.existsByUserAndPresentationWeekAndPresentationMonthAndPresentationYear(
                user, week, month, year)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "You have already voted this week");
        }
        
        // Registrar voto
        Vote vote = Vote.builder()
            .user(user)
            .presentation(presentation)
            .presentationWeek(week)
            .presentationMonth(month)
            .presentationYear(year)
            .voteDate(LocalDateTime.now())
            .build();
        
        voteRepository.save(vote);
        
        // Incrementar votos en la presentación
        presentation.incrementWeeklyVotes();
        presentation.incrementMonthlyVotes();
        presentationRepository.save(presentation);
        
        log.info("Vote registered for presentation {} by user {}", presentationId, userEmail);
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

    private void updateVoteCounters(Presentation presentation, VoteTypeEnum voteType) {
        if (voteType == VoteTypeEnum.WEEKLY) {
            presentation.setWeeklyVotes(presentation.getWeeklyVotes() + 1);
        } else {
            presentation.setMonthlyVotes(presentation.getMonthlyVotes() + 1);
        }
        presentationRepository.save(presentation);
    }

    private void updateAssignmentVotes(AgentAssignment assignment, VoteTypeEnum voteType) {
        // Aquí puedes actualizar cualquier contador de votos que necesites en AgentAssignment
        assignmentRepository.save(assignment);
    }
} 
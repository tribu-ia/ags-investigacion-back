package com.tribu.interview.manager.service.impl;

import com.tribu.interview.manager.model.AgentAssignment;
import com.tribu.interview.manager.model.PresentationVideo;
import com.tribu.interview.manager.model.PresentationVote;
import com.tribu.interview.manager.repository.jdbc.JdbcPresentationVideoRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcAgentAssignmentRepository;
import com.tribu.interview.manager.repository.jdbc.JdbcConfigParamsRepository;
import com.tribu.interview.manager.dto.UploadVideoRequest;
import com.tribu.interview.manager.dto.PresentationVideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PresentationVideoService {
    
    private final JdbcPresentationVideoRepository videoRepository;
    private final JdbcAgentAssignmentRepository assignmentRepository;
    private final JdbcConfigParamsRepository configParamsRepository;
    
    public PresentationVideoResponse uploadVideo(UploadVideoRequest request) {
        // Validar que la asignación existe y está activa
        AgentAssignment assignment = assignmentRepository.findById(request.getAssignmentId())
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
            
        // Validar que no existe un video previo para esta asignación
        if (videoRepository.existsByAssignmentId(assignment.getId())) {
            throw new IllegalStateException(
                "A video has already been uploaded for this assignment. Only one video per assignment is allowed."
            );
        }
            
        validateAssignmentEligibility(assignment);
        
        // Crear y guardar el video
        PresentationVideo video = PresentationVideo.builder()
            .assignmentId(assignment.getId())
            .title(request.getTitle())
            .description(request.getDescription())
            .youtubeUrl(request.getYoutubeUrl())
            .uploadedAt(LocalDateTime.now())
            .status("UPLOADED")
            .votesCount(0)
            .build();
            
        PresentationVideo savedVideo = videoRepository.save(video);
        
        return mapToVideoResponse(savedVideo);
    }
    
    private void validateAssignmentEligibility(AgentAssignment assignment) {
        // Obtener el mes actual del challenge desde la configuración
        int currentChallengeMonth = configParamsRepository.getCurrentMonthForChallenge();
        
        // Obtener el mes de la asignación
        int assignmentMonth = assignment.getAssignedAt().getMonthValue();
        
        // Validar que la asignación corresponde al mes actual del challenge
        if (assignmentMonth != currentChallengeMonth) {
            throw new IllegalStateException(
                "Videos can only be uploaded for assignments from the current challenge month (" + 
                currentChallengeMonth + ")"
            );
        }
        
        // Validar que la asignación está activa
        if (!"active".equals(assignment.getStatus())) {
            throw new IllegalStateException("Videos can only be uploaded for active assignments");
        }
        
        // Validar que estamos en el período de carga de videos (primera semana después de la asignación)
        LocalDate uploadDeadline = assignment.getAssignedAt().toLocalDate().plusWeeks(1);
        if (LocalDate.now().isAfter(uploadDeadline)) {
            throw new IllegalStateException("Video upload period has ended");
        }
    }
    
    private PresentationVideoResponse mapToVideoResponse(PresentationVideo video) {
        return PresentationVideoResponse.builder()
            .id(video.getId())
            .assignmentId(video.getAssignmentId())
                .title(video.getTitle())
                .description(video.getDescription())
            .youtubeUrl(video.getYoutubeUrl())
            .uploadedAt(video.getUploadedAt())
            .status(video.getStatus())
            .votingPeriod(PresentationVideoResponse.VotingPeriod.builder()
                .startDate(video.getVotingStartDate())
                .endDate(video.getVotingEndDate())
                .isVotingOpen("VOTING_OPEN".equals(video.getStatus()))
                .build())
            .votesCount(video.getVotesCount())
            .build();
    }


    public PresentationVideoResponse registerVote(String videoId, String voterId) {
        // Validar que el video existe
        PresentationVideo video = videoRepository.findById(videoId)
            .orElseThrow(() -> new ResourceNotFoundException("Video not found"));
            
        // Validar período de votación
        validateVotingPeriod(video);
        
        // Validar que el investigador no haya votado antes
        if (videoRepository.hasVoted(videoId, voterId)) {
            throw new IllegalStateException("Researcher has already voted for this video");
        }
        
        // Registrar el voto
        PresentationVote vote = PresentationVote.builder()
            .videoId(videoId)
            .voterId(voterId)
            .votedAt(LocalDateTime.now())
            .build();
            
        videoRepository.saveVote(vote);
        
        // Actualizar contador de votos
        video.setVotesCount(video.getVotesCount() + 1);
        return mapToVideoResponse(videoRepository.save(video));
    }

    public List<PresentationVideoResponse> getVideosInVotingPeriod() {
        // Obtener el mes actual del challenge desde la configuración
        int currentChallengeMonth = configParamsRepository.getCurrentMonthForChallenge();
        
        return videoRepository.findAllInVotingPeriod(currentChallengeMonth)
            .stream()
            .map(this::mapToVideoResponse)
            .collect(Collectors.toList());
    }

    public List<PresentationVideoResponse> getCurrentMonthVideos() {
        int currentChallengeMonth = configParamsRepository.getCurrentMonthForChallenge();
        
        return videoRepository.findAllByMonth(currentChallengeMonth)
            .stream()
            .map(this::mapToVideoResponse)
            .collect(Collectors.toList());
    }

    private void validateVotingPeriod(PresentationVideo video) {
        LocalDateTime now = LocalDateTime.now();
        if (!"VOTING_OPEN".equals(video.getStatus()) ||
            now.isBefore(video.getVotingStartDate()) ||
            now.isAfter(video.getVotingEndDate())) {
            throw new IllegalStateException("Video is not in voting period");
        }
    }
} 
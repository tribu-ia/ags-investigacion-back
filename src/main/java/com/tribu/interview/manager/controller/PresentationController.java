package com.tribu.interview.manager.controller;

import com.tribu.interview.manager.dto.PresentationViewDto;
import com.tribu.interview.manager.dto.WeekPresentationsResponse;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.service.IPresentationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import com.tribu.interview.manager.dto.CalendarPresentationDto;

@RestController
@RequestMapping("/presentations")
@RequiredArgsConstructor
public class PresentationController {
    private final IPresentationService presentationService;

    @GetMapping("/current-week")
    public ResponseEntity<WeekPresentationsResponse> getCurrentWeekPresentations() {
        List<CalendarPresentationDto> presentations = presentationService.getCurrentWeekPresentations();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.TUESDAY));
        
        return ResponseEntity.ok(WeekPresentationsResponse.builder()
            .weekStart(weekStart.format(DateTimeFormatter.ISO_DATE))
            .weekEnd(weekStart.plusDays(6).format(DateTimeFormatter.ISO_DATE))
            .presentations(presentations.stream()
                .map(p -> PresentationViewDto.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .avatarUrl(p.getAvatarUrl())
                    .repositoryUrl(p.getRepositoryUrl())
                    .linkedinUrl(p.getLinkedinUrl())
                    .role(p.getRole())
                    .presentation(p.getPresentation())
                    .date(p.getPresentationDateTime().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")))
                    .time(p.getPresentationDateTime().format(DateTimeFormatter.ofPattern("hh:mm a")))
                    .build())
                .toList())
            .build());
    }

} 
package com.tribu.interview.manager.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class WinnersResponse {
    private List<PresentationWinnerDto> weeklyWinners;
    private PresentationWinnerDto monthlyWinner;
}


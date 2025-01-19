package com.tribu.interview.manager.model;

import com.tribu.interview.manager.dto.enums.PresentationStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "presentations")
public class Presentation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "assignment_id")
    private AgentAssignment assignment;

    @Column(name = "presentation_date")
    private LocalDateTime presentationDate;
    
    @Enumerated(EnumType.STRING)
    private PresentationStatusEnum status;
    
    @Column(name = "weekly_votes")
    private Integer weeklyVotes;
    
    @Column(name = "monthly_votes")
    private Integer monthlyVotes;
    
    @Column(name = "weekly_winner")
    private Boolean weeklyWinner;
    
    @Column(name = "monthly_winner")
    private Boolean monthlyWinner;
    
    @Column(name = "presentation_week")
    private Integer weekOfYear;
    
    @Column(name = "presentation_month")
    private Integer monthOfYear;
    
    @Column(name = "presentation_year")
    private Integer year;

    @PrePersist
    protected void onCreate() {
        if (weeklyVotes == null) weeklyVotes = 0;
        if (monthlyVotes == null) monthlyVotes = 0;
        if (weeklyWinner == null) weeklyWinner = false;
        if (monthlyWinner == null) monthlyWinner = false;
        
        // Establecer los campos de fecha basados en presentationDate
        weekOfYear = presentationDate.get(WeekFields.ISO.weekOfWeekBasedYear());
        monthOfYear = presentationDate.getMonthValue();
        year = presentationDate.getYear();
    }

    // Método para incrementar votos
    public void incrementWeeklyVotes() {
        if (this.weeklyVotes == null) {
            this.weeklyVotes = 0;
        }
        this.weeklyVotes++;
    }

    public void incrementMonthlyVotes() {
        if (this.monthlyVotes == null) {
            this.monthlyVotes = 0;
        }
        this.monthlyVotes++;
    }
}


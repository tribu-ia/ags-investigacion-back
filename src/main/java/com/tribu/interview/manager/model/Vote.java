package com.tribu.interview.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes", 
       uniqueConstraints = {
           @UniqueConstraint(
               columnNames = {"user_id", "presentation_week", "presentation_month", "presentation_year"}, 
               name = "unique_user_weekly_vote"
           )
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "presentation_id")
    private Presentation presentation;

    @Column(name = "presentation_week")
    private Integer presentationWeek;

    @Column(name = "presentation_month")
    private Integer presentationMonth;

    @Column(name = "presentation_year")
    private Integer presentationYear;

    @Column(name = "vote_date")
    private LocalDateTime voteDate;
}

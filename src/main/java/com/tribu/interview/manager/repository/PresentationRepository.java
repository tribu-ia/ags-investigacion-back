package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.Presentation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PresentationRepository extends JpaRepository<Presentation, String> {
    
    @Query("SELECT p FROM Presentation p WHERE p.presentationDate >= :now ORDER BY p.presentationDate")
    List<Presentation> findUpcomingPresentations(LocalDateTime now);
    
    List<Presentation> findByPresentationDateBetween(LocalDateTime start, LocalDateTime end);
    
    List<Presentation> findByWeeklyWinnerTrueAndPresentationDateBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT p FROM Presentation p WHERE " +
           "FUNCTION('WEEK', p.presentationDate) = :week AND " +
           "MONTH(p.presentationDate) = :month AND " +
           "YEAR(p.presentationDate) = :year " +
           "ORDER BY p.monthlyVotes DESC")
    List<Presentation> findByWeekAndMonthAndYear(
        @Param("week") int week,
        @Param("month") int month,
        @Param("year") int year
    );
    
    @Query("SELECT COUNT(p) FROM Presentation p WHERE p.monthlyWinner = true AND p.monthOfYear = :month AND p.year = :year")
    long countMonthlyWinnersByMonthAndYear(int month, int year);

    List<Presentation> findByPresentationDateBetweenOrderByPresentationDate(
        LocalDateTime start, 
        LocalDateTime end
    );
    
    List<Presentation> findByPresentationDateBetweenOrderByWeeklyVotesDesc(
        LocalDateTime start, 
        LocalDateTime end
    );
    
    List<Presentation> findByMonthOfYearAndYearOrderByMonthlyVotesDesc(
        Integer month, 
        Integer year
    );
    
    long countByPresentationDateBetween(
        LocalDateTime start, 
        LocalDateTime end
    );
    
    @Query("SELECT p FROM Presentation p WHERE p.weekOfYear = :week AND p.year = :year")
    List<Presentation> findByWeekAndYear(Integer week, Integer year);
} 
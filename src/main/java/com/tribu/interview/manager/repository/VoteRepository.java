package com.tribu.interview.manager.repository;

import com.tribu.interview.manager.model.Vote;
import com.tribu.interview.manager.model.Presentation;
import com.tribu.interview.manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Vote, String> {
    
    boolean existsByUserAndPresentationWeekAndPresentationMonthAndPresentationYear(
        User user, 
        Integer week, 
        Integer month, 
        Integer year
    );

    List<Vote> findByPresentation(Presentation presentation);
} 
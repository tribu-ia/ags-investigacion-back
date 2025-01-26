package com.tribu.interview.manager.dto.enums;

public enum PresentationStatusEnum {
    PENDING,
    SCHEDULED,// Initial state
    VIDEO_UPLOADED,      // Video has been uploaded
    VOTING_OPEN,        // Presentation is open for voting
    COMPLETED           // Voting period ended
}
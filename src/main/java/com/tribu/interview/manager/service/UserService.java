package com.tribu.interview.manager.service;

import com.tribu.interview.manager.model.User;
import com.tribu.interview.manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(String userId, String email) {
        return userRepository.findById(userId)
            .orElseGet(() -> {
                log.info("Creando nuevo usuario con ID: {}", userId);
                User newUser = User.builder()
                    .id(userId)
                    .email(email)
                    .build();
                
                return userRepository.save(newUser);
            });
    }
} 
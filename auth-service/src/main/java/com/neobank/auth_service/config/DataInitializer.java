package com.neobank.auth_service.config;

import com.neobank.auth_service.entity.User;
import com.neobank.auth_service.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername("customer")) {
            return;
        }
        User user = new User();
        user.setCustomerId(generateCustomerId());
        user.setUsername("customer");
        user.setEmail("customer@singko.com");
        user.setPassword(passwordEncoder.encode("Customer@123"));
        userRepository.save(user);
    }

    private String generateCustomerId() {
        return "CUS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}

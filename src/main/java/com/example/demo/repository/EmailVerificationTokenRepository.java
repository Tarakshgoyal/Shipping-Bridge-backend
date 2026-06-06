package com.example.demo.repository;

import com.example.demo.entity.EmailVerificationToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

	Optional<EmailVerificationToken> findByToken(String token);
}

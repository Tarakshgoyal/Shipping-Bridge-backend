package com.example.demo.service;

import com.example.demo.config.EmailProperties;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.RegisterResponse;
import com.example.demo.dto.VerifyEmailResponse;
import com.example.demo.entity.EmailVerificationToken;
import com.example.demo.entity.UserAccount;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.integration.email.EmailSender;
import com.example.demo.repository.EmailVerificationTokenRepository;
import com.example.demo.repository.UserAccountRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final UserAccountRepository userAccountRepository;
	private final EmailVerificationTokenRepository tokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final EmailSender emailSender;
	private final EmailProperties emailProperties;

	public AuthService(
			UserAccountRepository userAccountRepository,
			EmailVerificationTokenRepository tokenRepository,
			PasswordEncoder passwordEncoder,
			EmailSender emailSender,
			EmailProperties emailProperties) {
		this.userAccountRepository = userAccountRepository;
		this.tokenRepository = tokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.emailSender = emailSender;
		this.emailProperties = emailProperties;
	}

	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase();
		String username = request.username().trim();
		if (userAccountRepository.existsByUsername(username)) {
			throw new ConflictException("Username is already registered");
		}
		if (userAccountRepository.existsByEmail(email)) {
			throw new ConflictException("Email is already registered");
		}

		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setEmail(email);
		account.setPasswordHash(passwordEncoder.encode(request.password()));
		account.setEmailVerified(false);
		UserAccount saved = userAccountRepository.save(account);

		EmailVerificationToken verificationToken = new EmailVerificationToken();
		verificationToken.setUserAccount(saved);
		verificationToken.setToken(newToken());
		verificationToken.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
		tokenRepository.save(verificationToken);

		emailSender.sendVerificationEmail(email, username, verificationLink(verificationToken.getToken()));
		return new RegisterResponse(saved.getId(), saved.getUsername(), saved.getEmail(), "PENDING_EMAIL_VERIFICATION");
	}

	@Transactional
	public VerifyEmailResponse verifyEmail(String token) {
		EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
				.orElseThrow(() -> new BadRequestException("Verification token is invalid"));
		if (verificationToken.getConsumedAt() != null) {
			throw new BadRequestException("Verification token has already been used");
		}
		if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
			throw new BadRequestException("Verification token has expired");
		}

		UserAccount account = verificationToken.getUserAccount();
		account.setEmailVerified(true);
		verificationToken.setConsumedAt(Instant.now());
		userAccountRepository.save(account);
		tokenRepository.save(verificationToken);
		return new VerifyEmailResponse(account.getEmail(), true, "VERIFIED");
	}

	private String verificationLink(String token) {
		return emailProperties.getVerificationBaseUrl()
				+ "?token="
				+ URLEncoder.encode(token, StandardCharsets.UTF_8);
	}

	private static String newToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}

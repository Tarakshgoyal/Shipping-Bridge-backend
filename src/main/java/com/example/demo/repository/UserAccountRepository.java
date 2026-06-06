package com.example.demo.repository;

import com.example.demo.entity.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	Optional<UserAccount> findByUsernameOrEmail(String username, String email);
}

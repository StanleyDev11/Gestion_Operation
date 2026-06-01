package com.ace3i.connect;

import com.ace3i.connect.entity.AppUser;
import com.ace3i.connect.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
public class Ace3iConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(Ace3iConnectApplication.class, args);
	}

	@Bean
	CommandLineRunner init(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.count() == 0) {
				AppUser admin = new AppUser();
				admin.setUsername("admin");
				admin.setPassword(passwordEncoder.encode("admin123"));
				admin.setFullName("Administrateur Système");
				admin.setRole("ROLE_ADMIN");
				userRepository.save(admin);
				
				AppUser tech = new AppUser();
				tech.setUsername("tech");
				tech.setPassword(passwordEncoder.encode("tech123"));
				tech.setFullName("Technicien ACE3i");
				tech.setRole("ROLE_TECHNICIAN");
				userRepository.save(tech);
			}
		};
	}
}

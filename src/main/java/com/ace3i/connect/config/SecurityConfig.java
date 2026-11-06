package com.ace3i.connect.config;

import com.ace3i.connect.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/settings/gitlab/**"))
            .authorizeHttpRequests((requests) -> requests
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                .requestMatchers("/client/**").hasAnyRole("CLIENT", "ADMIN")
                .requestMatchers("/tickets/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .requestMatchers("/admin/**", "/dashboard/**", "/settings/**", "/actuator/**").hasRole("ADMIN")
                .requestMatchers("/missions/**", "/feature-tracking/**").hasAnyRole("ADMIN", "TECHNICIAN")
                .anyRequest().authenticated()
            )
            .formLogin((form) -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout((logout) -> logout.permitAll())
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository userRepository) {
        return username -> userRepository.findByUsername(username)
                .map(u -> User.builder()
                        .username(u.getUsername())
                        .password(u.getPassword())
                        .roles(u.getRole().replace("ROLE_", ""))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

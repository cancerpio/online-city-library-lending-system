package com.library.config;

import com.library.auth.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 此專案為純 JWT/Bearer 放在 Authorization header，不依賴cookies / session，不處理csrf
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // 註冊用API，對外開放
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                
                // 需要JWT驗證身份的End point
                .requestMatchers(HttpMethod.POST, "/api/books").hasRole("Librarian")
                .requestMatchers(HttpMethod.PATCH, "/api/books/**").hasRole("Librarian")
                .requestMatchers(HttpMethod.DELETE, "/api/books/copies/**").hasRole("Librarian")
                .requestMatchers("/api/books/search").hasAnyRole("Member", "Librarian")
                .requestMatchers("/api/loans/borrow", "/api/loans/return").hasAnyRole("Member", "Librarian")
                

                .requestMatchers(HttpMethod.GET, "/api/branches").hasAnyRole("Member", "Librarian")
                .requestMatchers(HttpMethod.POST, "/api/branches").hasRole("Librarian")
                .requestMatchers(HttpMethod.PUT, "/api/branches/**").hasRole("Librarian")
                .requestMatchers(HttpMethod.DELETE, "/api/branches/**").hasRole("Librarian")

                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

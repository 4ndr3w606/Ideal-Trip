package com.web.spring.ideal_trip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Bean del encoder en una clase separada para romper el ciclo:
 * SecurityConfig → CustomOAuth2UserService → PasswordEncoder.
 *
 * Si PasswordEncoder estuviera dentro de SecurityConfig, Spring no podría
 * construir CustomOAuth2UserService antes que SecurityConfig (porque el
 * encoder es bean de SecurityConfig, que aún no terminó de inicializarse).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
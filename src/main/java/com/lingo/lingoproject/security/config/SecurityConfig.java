package com.lingo.lingoproject.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.exception.ExceptionHandlerFilter;
import com.lingo.lingoproject.repository.BlockedUserRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.form.CustomAuthenticationFilter;
import com.lingo.lingoproject.security.form.CustomAuthenticationManager;
import com.lingo.lingoproject.security.form.CustomAuthenticationProvider;
import com.lingo.lingoproject.security.form.CustomUserDetailService;
import com.lingo.lingoproject.security.jwt.JwtAuthenticationFilter;
import com.lingo.lingoproject.security.jwt.JwtUtil;
import com.lingo.lingoproject.utils.RedisUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final ObjectMapper objectMapper;
  private final JwtUtil jwtUtil;
  private final RedisUtils redisUtils;
  private final UserRepository userRepository;
  private final BlockedUserRepository blockedUserRepository;
  private final CustomUserDetailService customUserDetailService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/photographers/**").hasRole("PHOTOGRAPHER")
                .anyRequest().authenticated())
        .formLogin(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, redisUtils, userRepository, blockedUserRepository), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new CustomAuthenticationFilter(customAuthenticationManager(), objectMapper, authenticationConverter()), JwtAuthenticationFilter.class)
        .addFilterBefore(new ExceptionHandlerFilter(), CustomAuthenticationFilter.class)
        .sessionManagement(session -> session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        ));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    //config.setAllowCredentials(true);
    config.addAllowedOrigin("*");
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public CustomAuthenticationManager customAuthenticationManager() {
    return new CustomAuthenticationManager((CustomAuthenticationProvider) authenticationProvider());
  }

  @Bean
  public AuthenticationProvider authenticationProvider(){
    return new CustomAuthenticationProvider(customUserDetailService, passwordEncoder());
  }

  @Bean
  public AuthenticationConverter authenticationConverter(){
    return new BasicAuthenticationConverter();
  }
}

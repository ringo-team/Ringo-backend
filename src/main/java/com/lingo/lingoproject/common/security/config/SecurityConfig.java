package com.lingo.lingoproject.common.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.common.exception.ExceptionHandlerFilter;
import com.lingo.lingoproject.db.repository.BlockedUserRepository;
import com.lingo.lingoproject.db.repository.UserRepository;
import com.lingo.lingoproject.common.security.form.CustomAuthenticationFilter;
import com.lingo.lingoproject.common.security.form.CustomAuthenticationManager;
import com.lingo.lingoproject.common.security.form.CustomAuthenticationProvider;
import com.lingo.lingoproject.common.security.form.CustomUserDetailService;
import com.lingo.lingoproject.common.security.jwt.JwtAuthenticationFilter;
import com.lingo.lingoproject.common.security.jwt.JwtUtil;
import com.lingo.lingoproject.common.utils.RedisUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
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

  private final String[] whiteList = {
      "/signup/**", "/users/access", "/health",
      "/swagger-ui/**", "v3/api-docs/**",
      "/ws", "/stomp/**", "/stomp-test.html",
      "/actuator/**",
      "/profiles", "/feeds"
  };

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }


  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
      RedisTemplate<String, Object> redisTemplate) throws Exception {
    http
        .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers(whiteList).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/photographers/**").hasRole("PHOTOGRAPHER")
                .anyRequest().authenticated())
        .formLogin(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, redisTemplate, userRepository, blockedUserRepository), UsernamePasswordAuthenticationFilter.class)
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
    config.addAllowedOrigin("*"); // https://ringo.linkgo.com 도 추가
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public CustomAuthenticationManager customAuthenticationManager() {
    return new CustomAuthenticationManager(authenticationProvider());
  }

  @Bean
  public CustomAuthenticationProvider authenticationProvider(){
    return new CustomAuthenticationProvider(customUserDetailService, passwordEncoder());
  }

  @Bean
  public AuthenticationConverter authenticationConverter(){
    return new BasicAuthenticationConverter();
  }
}

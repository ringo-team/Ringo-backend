package com.lingo.lingoproject.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.security.response.LogoutResponseDto;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder bCryptPasswordEncoder() {
    return new BCryptPasswordEncoder();
  }

  private final String[] whiteList = {
      "/login/**",
      "/login",
      "/signUp/**",
      "/signUp",
      "/google/callback/**",
      "/kakao/callback/**",
      "/image/**",
      "/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors((cors) -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(authorize ->
            authorize
                .requestMatchers(whiteList).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/photo/**").hasRole("PHOTOGRAPHER")
                .anyRequest().authenticated())
        .formLogin(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(
            SessionCreationPolicy.STATELESS
        ))
        .logout((logout) -> logout
            .logoutUrl("/logout")
            .logoutSuccessHandler((request, response, authentication) -> {
              String jsonResponse = new ObjectMapper().writeValueAsString(
                  new LogoutResponseDto(HttpStatus.OK, "로그아웃 되었습니다.", null));
              // refreshtoken 삭제
              // HTTP 상태 코드 200 OK, JSON 형식 리턴
              response.setStatus(HttpStatus.OK.value());
              response.setContentType("application/json;charset=UTF-8"); //응답 데이터 타입 지정
              response.getWriter().write(jsonResponse); //응답 데이터 출력
              response.getWriter().flush(); //즉시 응답(더 빠름)
            })
            .invalidateHttpSession(true));
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    config.setAllowCredentials(true);
    config.addAllowedOrigin("http://localhost:3000");
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}

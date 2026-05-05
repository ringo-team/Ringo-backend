package com.lingo.lingoproject.shared.exception;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Slf4j
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    }catch (RingoException e){
      log.error("step=필터_예외, uri={}, method={}, message={}", request.getRequestURI(), request.getMethod(), e.getMessage(), e);
      response.setStatus(e.getHttpStatus().value());
      response.setContentType("application/json; charset=UTF-8");
      response.getWriter().write(
          ErrorResponse.of(LocalDateTime.now().toString(), e.getHttpStatus().value(), e.getMessage())
              .convertToJson()
      );
    }
  }
}

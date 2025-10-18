package com.lingo.lingoproject.exception;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    try {
      filterChain.doFilter(request, response);
    }catch (RingoException e){
      response.setStatus(e.getStatus().value());
      response.setContentType("application/json; charset=UTF-8");
      response.getWriter().write(
          ErrorResponse.of(LocalDateTime.now().toString(), e.getStatus().value(), e.getMessage())
              .convertToJson()
      );
    }
  }
}

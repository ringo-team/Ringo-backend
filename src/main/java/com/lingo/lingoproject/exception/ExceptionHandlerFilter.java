package com.lingo.lingoproject.exception;
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
      log.error("필터에서 오류가 발생하였습니다: {}, method: {}, message: {}", request.getRequestURI(), request.getMethod(), e.getMessage(), e);
      response.setStatus(e.getStatus().value());
      response.setContentType("application/json; charset=UTF-8");
      response.getWriter().write(
          ErrorResponse.of(LocalDateTime.now().toString(), e.getStatus().value(), e.getMessage())
              .convertToJson()
      );
    }
  }
}

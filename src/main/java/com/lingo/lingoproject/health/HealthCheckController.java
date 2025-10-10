package com.lingo.lingoproject.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "서비스 상태 확인 API")
public class HealthCheckController {

  @GetMapping("/health")
  @Operation(summary = "헬스 체크", description = "서비스의 상태를 확인합니다.")
  @ApiResponse(responseCode = "200", description = "서비스가 정상적으로 동작 중입니다.")
  public String health() {
    return "OK";
  }
}

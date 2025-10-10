package com.lingo.lingoproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ServletComponentScan
@EnableCaching
public class LingoProjectApplication {

  public static void main(String[] args) {
    SpringApplication.run(LingoProjectApplication.class, args);
  }

}

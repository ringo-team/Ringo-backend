package com.lingo.lingoproject.shared.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
  @GetMapping("/stomp")
  public String stomp() {
    return "stomp-test.html";
  }
}

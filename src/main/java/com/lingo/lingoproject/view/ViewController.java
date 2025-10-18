package com.lingo.lingoproject.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
  @GetMapping("/stomp")
  public String stomp() {
    return "stomp-test.html";
  }
}

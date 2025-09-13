package com.lingo.lingoproject.security.form;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationManager implements AuthenticationManager {

  private final AuthenticationProvider authenticationProvider;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return authenticationProvider.authenticate(authentication);
  }
}

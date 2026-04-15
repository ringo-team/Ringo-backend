package com.lingo.lingoproject.common.security.form;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

@RequiredArgsConstructor
public class CustomAuthenticationManager implements AuthenticationManager {

  private final CustomAuthenticationProvider customAuthenticationProvider;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    return customAuthenticationProvider.authenticate(authentication);
  }
}

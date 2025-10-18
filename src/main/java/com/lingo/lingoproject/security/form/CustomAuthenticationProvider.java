package com.lingo.lingoproject.security.form;

import com.lingo.lingoproject.exception.RingoException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

  private final CustomUserDetailService customUserDetailService;
  private final PasswordEncoder passwordEncoder;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    UserDetails user = customUserDetailService.loadUserByUsername(authentication.getName());
    if(!passwordEncoder.matches(authentication.getCredentials().toString(), user.getPassword())){
      throw new RingoException("패스워드가 옳지 않습니다.", HttpStatus.FORBIDDEN);
    }
    return  new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities());
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return authentication.equals(UsernamePasswordAuthenticationToken.class);
  }
}

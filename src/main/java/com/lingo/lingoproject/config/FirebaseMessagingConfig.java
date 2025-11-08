package com.lingo.lingoproject.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseMessagingConfig {
  //@PostConstruct
  public void refreshFcmCredentialsAndInitializeApp() throws IOException {
    GoogleCredentials googleCredentials = GoogleCredentials
        .fromStream(new ClassPathResource("firebase/").getInputStream())
        .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
    googleCredentials.refresh();
    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(googleCredentials)
        .build();
    FirebaseApp.initializeApp(options);
  }
}

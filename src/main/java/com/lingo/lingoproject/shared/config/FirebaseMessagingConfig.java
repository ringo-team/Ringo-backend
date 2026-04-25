package com.lingo.lingoproject.shared.config;

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
  @PostConstruct
  public void refreshFcmCredentialsAndInitializeApp() throws IOException {

    FirebaseApp firebaseApp = null;
    List<FirebaseApp> firebaseApps = FirebaseApp.getApps();

    if (firebaseApps != null && !firebaseApps.isEmpty()){
      for (FirebaseApp app : firebaseApps) {
        if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)){
          firebaseApp = app;
          return;
        }
      }
    }

    GoogleCredentials googleCredentials = GoogleCredentials
        .fromStream(new ClassPathResource("firebase/ringo-cloud-messaging-project-firebase-adminsdk-fbsvc-4bbbe99bb2.json").getInputStream())
        .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
    googleCredentials.refresh();
    FirebaseOptions options = FirebaseOptions.builder()
        .setCredentials(googleCredentials)
        .build();
    firebaseApp = FirebaseApp.initializeApp(options);
  }
}

package com.lingo.lingoproject.shared.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class FirebaseMessagingConfig {

  @Value("${firebase.key.url}")
  private String FIREBASE_KEY_PATH;

  @PostConstruct
  public void refreshFcmCredentialsAndInitializeApp() throws IOException {

    FirebaseApp firebaseApp = null;
    List<FirebaseApp> firebaseApps = FirebaseApp.getApps();

    if (firebaseApps != null && !firebaseApps.isEmpty()){
      for (FirebaseApp app : firebaseApps) {
        if (app.getName().equals(FirebaseApp.DEFAULT_APP_NAME)){
          firebaseApp = app;
          log.info("step=FIREBASE_이미_초기화됨, appName={}", app.getName());
          return;
        }
      }
    }

    try {
      GoogleCredentials googleCredentials = GoogleCredentials
          .fromStream(getFirebaseCredentials())
          .createScoped(List.of("https://www.googleapis.com/auth/firebase.messaging"));
      log.info("step=FIREBASE_자격증명_갱신_성공, accessToken_존재={}", googleCredentials.getAccessToken() != null);
      FirebaseOptions options = FirebaseOptions.builder()
          .setCredentials(googleCredentials)
          .build();
      FirebaseApp.initializeApp(options);
      log.info("step=FIREBASE_초기화_완료");
    } catch (Exception e) {
      log.error("step=FIREBASE_초기화_실패", e);
      throw e;
    }
  }

  private InputStream getFirebaseCredentials() throws IOException {
    if (FIREBASE_KEY_PATH != null && !FIREBASE_KEY_PATH.isBlank()) {
      // prod: EFS 등 파일 경로
      return new FileInputStream(FIREBASE_KEY_PATH);
    }
    // dev: classpath 리소스
    return new ClassPathResource(
        "firebase/ringo-bdd26-firebase-adminsdk-fbsvc-107cc408f2.json")
        .getInputStream();
  }
}

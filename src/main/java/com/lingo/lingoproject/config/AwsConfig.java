package com.lingo.lingoproject.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {
  @Value("${aws.credentials.access-key}")
  private String accessKey;

  @Value("${aws.credentials.secret-key}")
  private String secretKey;

  @Value("${aws.region.static}")
  private String region;

  @Bean
  public AmazonS3Client amazonS3Client(){
    BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return (AmazonS3Client) AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }

  @Bean
  public AmazonRekognition amazonRekognition(){

    BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return AmazonRekognitionClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}

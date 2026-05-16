package com.lingo.lingoproject.shared.utils;

public class RedisKey {
  public static final String 일일_설문_기반_추천_캐시_레디스_키 = "recommend-for-daily-survey::";
  public static final String 누적_설문_기반_추천_프로필_캐시_키 = "recommend-for-cumulative-survey::";
  public static final String FCM_재시도_레디스_키           = "fcm::retry-queue";
  public static final String 접속_유저_레디스_키             = "connect-app::*";
  public static final String 활성_유저_레디스_키             = "redis::active::ids";
  public static final String 디스코드_재시도_레디스_키         = "discord::retry-queue";


}

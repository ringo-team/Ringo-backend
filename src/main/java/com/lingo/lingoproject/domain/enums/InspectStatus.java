package com.lingo.lingoproject.domain.enums;

public enum InspectStatus {
  PENDING("얼굴 미노출, 과도한 노출 검수중입니다."),
  PASS("해당 프로필이 검수에 통과하였습니다.");

  private String description;

  InspectStatus(String description){
    this.description = description;
  }

  public String getValue(){
    return description;
  }
}

package com.lingo.lingoproject.shared.domain.model;

public enum PlaceType {
  RINGO("링고픽"),
  NIGHT("야경이 매력적인 장소"),
  ALLEY("골목길에 숨겨진 공간"),
  COZY_CAFE("인스타 감성 카페"),
  HOTPLACE("웨이팅이 끊이지 않는 핫플"),
  ACTIVITY("액티비티 데이트에 어울리는 공간"),
  ARTISTIC("예술적 감각이 돋보이는 공간"),
  EXOTIC("이국적인 분위기가 매력적인 공간"),
  EXPERIENCE("다양한 체험이 가능한 공간"),
  IMMERSIVE("한 가지 경험에 몰입하기 좋은 공간"),
  HANOK("한옥 감성을 느낄 수 있는 공간"),
  DESSERT("디저트가 매력적인 공간"),
  RETRO("레트로한 분위기가 살아있는 공간"),
  ROOFTOP("루프탑이 매력적인 공간"),
  WALKABLE("산책하기 좋은 공간"),
  CLEAN("깔끔하고 정돈된 공간"),
  QUIET("조용히 대화 나누기 좋은 공간"),
  MODERN("모던한 분위기가 매력적인 공간"),
  WIDE("개방감이 느껴지는 공간");

  private final String description;

  PlaceType(String description){
    this.description = description;
  }

  public String getDescription(){
    return this.description;
  }
}

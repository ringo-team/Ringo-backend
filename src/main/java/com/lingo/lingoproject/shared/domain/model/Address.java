package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address {

  @Column(length = 10)
  private String province;
  @Column(length = 10)
  private String city;

  protected Address() {}

  public Address(String province, String city) {
    if (province == null || province.isBlank()) {
      throw new IllegalArgumentException("province는 필수입니다.");
    }
    if (city == null || city.isBlank()) {
      throw new IllegalArgumentException("city는 필수입니다.");
    }
    this.province = province;
    this.city = city;
  }

  public String getProvince() { return province; }
  public String getCity()     { return city; }
}

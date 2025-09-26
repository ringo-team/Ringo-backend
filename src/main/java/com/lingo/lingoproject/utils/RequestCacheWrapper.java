package com.lingo.lingoproject.utils;

import org.springframework.stereotype.Component;

@Component
public class RequestCacheWrapper {
  String content;
  public void setContent(String request){
    this.content = request;
  }
  @Override
  public String toString(){
    return this.content;
  }
}

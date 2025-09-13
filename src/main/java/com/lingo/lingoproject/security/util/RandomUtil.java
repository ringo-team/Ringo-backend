package com.lingo.lingoproject.security.util;

import java.util.Random;
import org.springframework.stereotype.Component;

@Component
public class RandomUtil {
  public int getRandomNumber(){
    Random random = new Random();
    long seed = System.currentTimeMillis();
    random.setSeed(seed);
    return random.nextInt()%100000;
  }
}

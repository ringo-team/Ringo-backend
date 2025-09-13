package com.lingo.lingoproject.utils;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest
public class testUUID {
  @Test
  void testUUID(){
    UUID uuid = UUID.randomUUID();
    System.out.println(uuid);
    String uuidString = uuid.toString();
    System.out.println(uuidString);
    Assertions.assertEquals(uuidString,uuid.toString());
    Assertions.assertEquals(UUID.fromString(uuidString), uuid);
  }
}

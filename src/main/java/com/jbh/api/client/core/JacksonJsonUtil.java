package com.jbh.api.client.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class JacksonJsonUtil {
  private static final ObjectMapper mapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new ParameterNamesModule());

  public static <T> T fromJson(String json, Class<T> clazz) throws Exception {
    return mapper.readValue(json, clazz);
  }

  public static String toJson(Object obj) throws Exception {
    return mapper.writeValueAsString(obj);
  }
}

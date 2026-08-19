/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.common.pagenation;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.global.exception.CustomException;
import com.skulikelion.festival.global.exception.GlobalErrorCode;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class CursorCodec {

  private final ObjectMapper objectMapper;

  public <T> String encode(T cursor) {
    String json = objectMapper.writeValueAsString(cursor);
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  public <T> T decode(String cursor, Class<T> clazz) {
    if (cursor == null) {
      return null;
    }
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(cursor);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, clazz);
    } catch (Exception e) {
      throw new CustomException(GlobalErrorCode.INVALID_INPUT_VALUE);
    }
  }
}

/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.converter;

import java.util.List;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Converter
public class IntegerListJsonConverter implements AttributeConverter<List<Integer>, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<Integer>> INTEGER_LIST_TYPE = new TypeReference<>() {};

  @Override
  public String convertToDatabaseColumn(List<Integer> attribute) {
    if (attribute == null) {
      return null;
    }

    try {
      return OBJECT_MAPPER.writeValueAsString(attribute);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("부스 번호 목록 JSON 변환에 실패했습니다.", e);
    }
  }

  @Override
  public List<Integer> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }

    try {
      return OBJECT_MAPPER.readValue(dbData, INTEGER_LIST_TYPE);
    } catch (JacksonException e) {
      throw new IllegalArgumentException("부스 번호 목록 역직렬화에 실패했습니다.", e);
    }
  }
}

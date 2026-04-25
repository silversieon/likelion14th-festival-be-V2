/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.enums;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "시간 타입")
public enum TimeType {
  DAY("낮"),
  NIGHT("밤"),
  ALL("낮&밤");

  private final String description;

  public static List<TimeType> forQuery(TimeType type) {
    return switch (type) {
      case DAY -> List.of(DAY, ALL);
      case NIGHT -> List.of(NIGHT, ALL);
      case ALL -> List.of(ALL);
    };
  }
}

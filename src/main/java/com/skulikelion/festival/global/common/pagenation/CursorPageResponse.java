/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.common.pagenation;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class CursorPageResponse<T> {

  private List<T> items;

  private String nextCursor;

  private Boolean hasNext;

  private Integer size;

  public static <T> CursorPageResponse<T> of(
      List<T> items, String nextCursor, Boolean hasNext, Integer size) {
    return CursorPageResponse.<T>builder()
        .items(items)
        .nextCursor(nextCursor)
        .hasNext(hasNext)
        .size(size)
        .build();
  }
}

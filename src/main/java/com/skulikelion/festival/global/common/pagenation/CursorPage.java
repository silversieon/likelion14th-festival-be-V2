/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.common.pagenation;

import java.util.List;

public record CursorPage<T>(List<T> content, boolean hasNext) {

  public static <T> CursorPage<T> of(List<T> results, int size) {
    boolean hasNext = results.size() > size;

    List<T> content = hasNext ? results.subList(0, size) : results;

    return new CursorPage<>(content, hasNext);
  }
}

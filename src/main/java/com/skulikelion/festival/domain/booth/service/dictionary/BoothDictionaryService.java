/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.dictionary;

public interface BoothDictionaryService {

  // BoothMenuDictionary에 반영하는 메서드
  // 트랜잭션 전파 수준 새로 생성하는 것으로

  /**
   * @param nameKo
   * @param nameEn
   * @param nameZh
   */
  void saveBoothMenuDictionary(String nameKo, String nameEn, String nameZh);
}

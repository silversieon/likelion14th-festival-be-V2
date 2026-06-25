/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.dictionary;

public interface BoothDictionaryService {

  /**
   * @param nameKo
   * @param nameEn
   * @param nameZh
   */
  void saveBoothMenuDictionary(String nameKo, String nameEn, String nameZh);
}

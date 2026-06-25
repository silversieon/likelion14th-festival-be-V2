/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.dictionary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.entity.BoothMenuDictionary;
import com.skulikelion.festival.domain.booth.repository.BoothMenuDictionaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoothDictionaryServiceImpl implements BoothDictionaryService {

  private final BoothMenuDictionaryRepository boothMenuDictionaryRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveBoothMenuDictionary(String nameKo, String nameEn, String nameZh) {
    boothMenuDictionaryRepository.save(new BoothMenuDictionary(nameKo, nameEn, nameZh));
  }
}

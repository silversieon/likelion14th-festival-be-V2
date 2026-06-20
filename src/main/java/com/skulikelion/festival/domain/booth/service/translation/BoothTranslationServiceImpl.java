/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.translation;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuTranslationResponse;
import com.skulikelion.festival.domain.booth.entity.BoothMenuDictionary;
import com.skulikelion.festival.domain.booth.repository.BoothMenuDictionaryRepository;
import com.skulikelion.festival.global.ai.AnthropicClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BoothTranslationServiceImpl implements BoothTranslationService {
  private final BoothMenuDictionaryRepository boothMenuDictionaryRepository;
  private final BoothTranslationPromptBuilder translationPromptBuilder;
  private final AnthropicClient anthropicClient;

  @Override
  @Transactional(readOnly = true)
  public BoothMenuTranslationResponse translateBoothMenu(BoothMenuTranslationRequest request) {
    String nameKo = request.nameKo();

    Optional<BoothMenuDictionary> cacheMenu = boothMenuDictionaryRepository.findById(nameKo);
    boolean isCacheHit = cacheMenu.isPresent();

    String systemPrompt = translationPromptBuilder.getMenuSystemPrompt();
    String userPrompt = translationPromptBuilder.buildMenuTranslationPrompt(request, isCacheHit);

    String aiResponse = anthropicClient.call(systemPrompt, userPrompt);

    // 캐시 히트 여부에 따라 응답 생성 방식 다르게
    if (isCacheHit) {

    } else {
      // 캐시 미스 발생에 따른 BoothMenuDictionary에 추가 반영하는 함수 호출

    }
    return null;
  }
}

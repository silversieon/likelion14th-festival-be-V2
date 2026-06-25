/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.translation;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuTranslationResponse;
import com.skulikelion.festival.domain.booth.entity.BoothMenuDictionary;
import com.skulikelion.festival.domain.booth.repository.BoothMenuDictionaryRepository;
import com.skulikelion.festival.domain.booth.service.dictionary.BoothDictionaryService;
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
  private final ObjectMapper objectMapper;
  private final BoothDictionaryService boothDictionaryService;

  @Override
  public BoothMenuTranslationResponse translateBoothMenu(BoothMenuTranslationRequest request) {
    String nameKo = request.nameKo();

    Optional<BoothMenuDictionary> cacheMenu = boothMenuDictionaryRepository.findById(nameKo);
    boolean isCacheHit = cacheMenu.isPresent();

    String systemPrompt = translationPromptBuilder.getMenuSystemPrompt();
    String userPrompt = translationPromptBuilder.buildMenuTranslationPrompt(request, isCacheHit);

    String aiResponse = anthropicClient.call(systemPrompt, userPrompt);
    log.debug(aiResponse);

    try {
      BoothMenuTranslationResponse translationResponse =
          objectMapper.readValue(aiResponse, BoothMenuTranslationResponse.class);

      if (isCacheHit) {
        BoothMenuDictionary dictionary = cacheMenu.get();
        return new BoothMenuTranslationResponse(
            nameKo,
            dictionary.getNameEn(),
            dictionary.getNameZh(),
            request.descriptionKo(),
            translationResponse.descriptionEn(),
            translationResponse.descriptionZh());
      }

      boothDictionaryService.saveBoothMenuDictionary(
          nameKo, translationResponse.nameEn(), translationResponse.nameZh());

      return new BoothMenuTranslationResponse(
          nameKo,
          translationResponse.nameEn(),
          translationResponse.nameZh(),
          request.descriptionKo(),
          translationResponse.descriptionEn(),
          translationResponse.descriptionZh());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}

/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.translation;

import org.springframework.stereotype.Component;

import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuTranslationRequest;

@Component
public class BoothTranslationPromptBuilder {

  private static final String MENU_SYSTEM_PROMPT =
      """
            You are a Korean food menu translator.

            Rules:
            - Respond ONLY with JSON format, no explanation
            - English: Use commonly known romanization or natural English expression
            - Chinese: Use Simplified Chinese
            - For description: Do NOT translate word-for-word.
                               Capture the overall feel and nuance of the food naturally.
            """;

  public String getMenuSystemPrompt() {
    return MENU_SYSTEM_PROMPT;
  }

  public String buildMenuTranslationPrompt(
      BoothMenuTranslationRequest request, boolean isCacheHit) {
    String nameKo = request.nameKo();
    String descriptionKo = request.descriptionKo();

    if (isCacheHit) {
      return """
                    Translate the given Korean food description into English and Chinese.
                    Use the menu name as context for better translation quality.

                    Response format:
                    {
                        "descriptionEn": "...",
                        "descriptionZh": "..."
                    }

                    Korean menu name (for context): %s
                    Korean description: %s
                    """
          .formatted(nameKo, descriptionKo);
    } else {
      return """
                    Translate the given Korean menu name and description into English and Chinese.

                    Response format:
                    {
                        "nameEn": "...",
                        "nameZh": "...",
                        "descriptionEn": "...",
                        "descriptionZh": "..."
                    }

                    Korean menu name: %s
                    Korean description: %s
                    """
          .formatted(nameKo, descriptionKo);
    }
  }
}

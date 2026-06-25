/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.global.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skulikelion.festival.global.config.property.AiProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnthropicClient {

  private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";
  private static final String ANTHROPIC_VERSION = "2023-06-01";
  private final ObjectMapper objectMapper;

  private final AiProperties aiProperties;
  private final RestTemplate restTemplate;

  public String call(String systemPrompt, String userPrompt) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("x-api-key", aiProperties.getApiKey());
      headers.set("anthropic-version", ANTHROPIC_VERSION);

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", aiProperties.getModel());
      body.put("max_tokens", 1024);
      body.put("system", systemPrompt);
      body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

      HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
      ResponseEntity<String> response =
          restTemplate.postForEntity(ANTHROPIC_URL, request, String.class);

      JsonNode root = objectMapper.readTree(response.getBody());

      JsonNode usage = root.path("usage");
      log.debug(
          "[AI] 토큰 사용량 - input: {}, output: {}",
          usage.path("input_tokens").asInt(),
          usage.path("output_tokens").asInt());

      String responseContent = root.path("content").get(0).path("text").asText();
      return stripMarkdownCodeBlock(responseContent);
    } catch (Exception e) {
      log.error("[AI] Claude API 호출 실패: {}", e.getMessage());
      return null;
    }
  }

  private String stripMarkdownCodeBlock(String text) {
    String stripped = text.strip();
    if (!stripped.startsWith("```")) return stripped;

    int firstNewline = stripped.indexOf('\n');
    if (firstNewline != -1) {
      stripped = stripped.substring(firstNewline + 1);
    }
    if (stripped.endsWith("```")) {
      stripped = stripped.substring(0, stripped.lastIndexOf("```")).strip();
    }
    return stripped;
  }
}

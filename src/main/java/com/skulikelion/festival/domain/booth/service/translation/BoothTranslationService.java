/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.service.translation;

import com.skulikelion.festival.domain.booth.dto.request.menu.BoothMenuTranslationRequest;
import com.skulikelion.festival.domain.booth.dto.response.menu.BoothMenuTranslationResponse;

public interface BoothTranslationService {

  /**
   * [ 부스 메뉴 번역 ] 한국어 메뉴명, 메뉴 설명을 인자로 받아 영어, 중국어 메뉴명, 설명 반환
   *
   * @param request 한국어 메뉴명, 메뉴 설명 dto
   * @return 번역된 메뉴명, 설명 dto
   */
  BoothMenuTranslationResponse translateBoothMenu(BoothMenuTranslationRequest request);
}

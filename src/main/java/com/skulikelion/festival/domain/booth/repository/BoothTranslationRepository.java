/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothTranslation;
import com.skulikelion.festival.global.enums.Language;

public interface BoothTranslationRepository extends JpaRepository<BoothTranslation, Long> {

  Optional<BoothTranslation> findByBoothIdAndLanguage(Long boothId, Language language);

  void deleteByBoothId(Long boothId);
}

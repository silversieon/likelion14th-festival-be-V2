/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.BoothMenuDictionary;

public interface BoothMenuDictionaryRepository extends JpaRepository<BoothMenuDictionary, String> {}

/* 
 * Copyright (c) SKU LIKELION 
 */
package com.skulikelion.festival.domain.booth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.skulikelion.festival.domain.booth.entity.Booth;

public interface BoothRepository extends JpaRepository<Booth, Long> {

  Optional<Booth> findByName(String name);

  List<Booth> findAllByServiceAgreementAndIdIn(Boolean serviceAgreement, List<Long> ids);

  Page<Booth> findByIdGreaterThan(Long cursor, Pageable pageable);

  Page<Booth> findByServiceAgreementTrue(Pageable pageable);

  Page<Booth> findByServiceAgreementTrueAndIdGreaterThan(Long id, Pageable pageable);
}

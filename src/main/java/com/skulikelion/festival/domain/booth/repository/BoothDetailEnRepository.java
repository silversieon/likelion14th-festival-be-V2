package com.skulikelion.festival.domain.booth.repository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailEn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoothDetailEnRepository extends JpaRepository<BoothDetailEn, Long> {

  List<BoothDetailEn> findByBoothFacultyEnContaining(String faculty);

  Optional<BoothDetailEn> findByBoothId(Long id);
}

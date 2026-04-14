package com.skulikelion.festival.domain.booth.repository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailKo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoothDetailKoRepository extends JpaRepository<BoothDetailKo, Long> {

  List<BoothDetailKo> findByBoothFacultyKoContaining(String faculty);

  Optional<BoothDetailKo> findByBoothId(Long id);
}

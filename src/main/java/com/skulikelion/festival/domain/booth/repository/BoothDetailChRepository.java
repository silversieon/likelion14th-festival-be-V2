package com.skulikelion.festival.domain.booth.repository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailCh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoothDetailChRepository extends JpaRepository<BoothDetailCh, Long> {

  List<BoothDetailCh> findByBoothFacultyChContaining(String faculty);

  Optional<BoothDetailCh> findByBoothId(Long id);
}

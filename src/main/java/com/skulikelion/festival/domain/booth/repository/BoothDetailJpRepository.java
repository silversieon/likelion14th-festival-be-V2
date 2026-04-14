package com.skulikelion.festival.domain.booth.repository;

import com.skulikelion.festival.domain.booth.entity.BoothDetailJp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoothDetailJpRepository extends JpaRepository<BoothDetailJp, Long> {

  List<BoothDetailJp> findByBoothFacultyJpContaining(String faculty);

  Optional<BoothDetailJp> findByBoothId(Long id);
}

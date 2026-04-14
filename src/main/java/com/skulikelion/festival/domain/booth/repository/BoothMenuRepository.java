package com.skulikelion.festival.domain.booth.repository;

import com.skulikelion.festival.domain.booth.entity.BoothMenu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoothMenuRepository extends JpaRepository<BoothMenu, Long> {

  List<BoothMenu> findByBoothId(Long id);

  Optional<BoothMenu> findByBoothIdAndMenuKo(Long id, String menuKo);

  boolean existsByBoothIdAndMenuKo(Long id, String menuKo);
}
